package com.wrapp.android.webimage;

import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

public class ImageLoader {
  private static final long SHUTDOWN_TIMEOUT_IN_MS = 100;
  
  private static ImageLoader instance;

  private Context context;
  private Handler handler;

  // Worker threads for different tasks, ordered from fast -> slow
  private ExecutorService fileLoader;
  private ExecutorService checkTimestamp;
  private AdaptingThreadPoolExecutor download;
  
  private WeakHashMap<ImageRequest.Listener, PendingTask> pending;

  public static ImageLoader getInstance(Context context) {
    if (instance == null) {
      instance = new ImageLoader(context);
    }

    return instance;
  }

  public ImageLoader(Context context) {
    this.context = context.getApplicationContext();
    handler = new Handler();

    pending = new WeakHashMap<ImageRequest.Listener, PendingTask>();
    
    createExecutors();
  }

  public static void load(Context context, URL imageUrl, ImageRequest.Listener listener, BitmapFactory.Options options) {
    getInstance(context).load(imageUrl, listener, options);
  }

  public static void cancelAllRequests() {
    getInstance(null).cancelAllRequestsInternal();
  }

  public static void shutdown() {
    getInstance(null).shutdownInternal();
  }
  
  AdaptingThreadPoolExecutor getDownloadExecutor() {
    return download;
  }
  
  void checkTimeStamp(ImageRequest request) {
    checkTimestamp.submit(new CheckTimeStampTask(request));
  }
  
  void forceUpdateImage(URL url, BitmapFactory.Options options) {
    // TODO: Actually let this update the view
    ImageRequest request = new ImageRequest(context, url, null, options);
    request.forceDownload = true;
    
    download.submit(new DownloadTask(request));
  }
  
  private void createExecutors() {
    fileLoader = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.NORM_PRIORITY - 1));
    checkTimestamp = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.MIN_PRIORITY));
    download = new AdaptingThreadPoolExecutor(context);
  }
  
  private void cancelAllRequestsInternal() {
    for (Map.Entry<ImageRequest.Listener, PendingTask> entry : pending.entrySet()) {
      entry.getValue().future.cancel(false);
    }
    
    pending.clear();
  }
  
  private void shutdownInternal() {
    LogWrapper.logMessage("Shutting down");
    try {
      download.shutdownNow();
      checkTimestamp.shutdownNow();
      fileLoader.shutdownNow();
      
      pending.clear();
      
      download.awaitTermination(SHUTDOWN_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Ignore
    }
    
    // TODO: What are we supposed to do?
    createExecutors();
  }

  private void load(URL imageUrl, ImageRequest.Listener listener, BitmapFactory.Options options) {
    ImageRequest request = new ImageRequest(context, imageUrl, listener, options);
    
    PendingTask task = pending.get(listener);
    if (task != null && !task.url.equals(request.imageUrl)) {
      // Listener is pending with another url, cancel it
      LogWrapper.logMessage("Already pending, old: " + task.url + ", new: " + request.imageUrl);
      if (task.future.cancel(false)) {
        // Notify listener if we managed to cancel it, otherwise it
        // will be notified in the callback
        request.listener.onBitmapLoadCancelled();
      }
      
      pending.remove(listener);
    } else if (task != null && task.url.equals(request.imageUrl)) {
      // Request already exists, ignore new request
      return;
    }
    
    if (ImageCache.isImageCached(request.context, request.imageKey)) {
      loadFromDisk(request);
    } else {
      loadFromUrl(request);
    }
  }
  
  private void loadFromDisk(ImageRequest request) {
    CallbackTask task = new CallbackTask(new FileLoadTask(request), request, completionListener, handler);
    fileLoader.submit(task);
    pending.put(request.listener, new PendingTask(request.imageUrl, task));
  }
  
  private void loadFromUrl(ImageRequest request) {
    CallbackTask task = new CallbackTask(new DownloadTask(request), request, completionListener, handler);
    download.submit(task);
    pending.put(request.listener, new PendingTask(request.imageUrl, task));
  }
  
  private boolean requestValid(ImageRequest request) {
    PendingTask task = pending.get(request.listener);
    
    return task != null && task.url.equals(request.imageUrl);
  }
  
  private CallbackTask.Listener completionListener = new CallbackTask.Listener() {
    @Override
    public void onComplete(ImageRequest request) {
      if (!requestValid(request)) {
        // The listener wants something else, ignore
        request.listener.onBitmapLoadCancelled();
        LogWrapper.logMessage("Request no longer pending, dropping: " + request.imageUrl);
        return;
      }
      
      PendingTask task = pending.get(request.listener);

      try {
        Bitmap b = task.future.get();

        if (b != null) {
          // Image was fetched from disk
          request.listener.onBitmapLoaded(request, b);
          pending.remove(request.listener);
        } else {
          // Image was downloaded to disk, fetch it
          loadFromDisk(request);
        }
      } catch (ExecutionException e) {
        // Request failed for some reason
        Throwable original = e.getCause();
        Log.e("WebImage", "Failed to fetch image", original);

        request.listener.onBitmapLoadError(original.getMessage());
        pending.remove(request.listener);
      } catch (InterruptedException e) {
        // In case we get interrupted while getting the value,
        // shouldn't be able to happen as we only call it after completion
      }
    }
  };

  private static class PendingTask {
    public URL url;
    public Future<Bitmap> future;
    
    public PendingTask(URL url, Future<Bitmap> future) {
      this.url = url;
      this.future = future;
    }
  }
}
