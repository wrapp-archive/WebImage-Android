package com.wrapp.android.webimage;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  private ExecutorService fileLoaderExecutor;
  private ExecutorService checkTimestampExecutor;
  private AdaptingThreadPoolExecutor downloadExecutor;
  
  private PendingRequests pendingRequests;

  public static ImageLoader getInstance(Context context) {
    if (instance == null) {
      instance = new ImageLoader(context);
    }

    return instance;
  }

  public ImageLoader(Context context) {
    this.context = context.getApplicationContext();
    handler = new Handler();

    pendingRequests = new PendingRequests();
    
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
    return downloadExecutor;
  }
  
  void checkTimeStamp(ImageRequest request) {
    checkTimestampExecutor.submit(new CheckTimeStampTask(context, request));
  }
  
  void forceUpdateImage(URL url, BitmapFactory.Options options) {
    // TODO: Actually let this update the view
    ImageRequest request = new ImageRequest(url, options);
    request.forceDownload = true;
    
    downloadExecutor.submit(new DownloadTask(context, request));
  }
  
  private void createExecutors() {
    fileLoaderExecutor = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.NORM_PRIORITY - 1));
    checkTimestampExecutor = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.MIN_PRIORITY));
    downloadExecutor = new AdaptingThreadPoolExecutor(context);
  }
  
  private void cancelAllRequestsInternal() {
    pendingRequests.clear();
  }
  
  private void shutdownInternal() {
    LogWrapper.logMessage("Shutting down");
    try {
      downloadExecutor.shutdownNow();
      checkTimestampExecutor.shutdownNow();
      fileLoaderExecutor.shutdownNow();
      
      pendingRequests.clear();
      
      downloadExecutor.awaitTermination(SHUTDOWN_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Ignore
    }
    
    // TODO: What are we supposed to do?
    createExecutors();
  }

  private void load(URL imageUrl, ImageRequest.Listener listener, BitmapFactory.Options options) {
    ImageRequest request = new ImageRequest(imageUrl, options);
    
    if (!pendingRequests.addRequest(request, listener)) {
      // We need to add a task
      CallbackFuture task;
      if (ImageCache.isImageCached(context, request.imageKey)) {
        task = loadFromDisk(request);
      } else {
        task = loadFromUrl(request);
      }
      
      pendingRequests.addTask(request, listener, task);
    }
  }
  
  private CallbackFuture loadFromDisk(ImageRequest request) {
    CallbackFuture task = new CallbackFuture(new FileLoadTask(context, request), request, completionListener, handler);
    fileLoaderExecutor.submit(task);
    
    return task;
  }
  
  private CallbackFuture loadFromUrl(ImageRequest request) {
    CallbackFuture task = new CallbackFuture(new DownloadTask(context, request), request, completionListener, handler);
    downloadExecutor.submit(task);
    
    return task;
  }

  private CallbackFuture.Listener completionListener = new CallbackFuture.Listener() {
    @Override
    public void onComplete(ImageRequest request) {
      if (!pendingRequests.isPending(request)) {
        // No longer pending
        LogWrapper.logMessage("Request no longer pending, dropping: " + request.imageUrl);
        
        return;
      }
      
      try {
        Bitmap b = pendingRequests.getResult(request);

        if (b != null) {
          // Image was fetched from disk
          for (ImageRequest.Listener listener : pendingRequests.getListeners(request)) {
            listener.onBitmapLoaded(request, b);
          }
          
          pendingRequests.removeRequest(request);
        } else {
          // Image was downloaded to disk, fetch it
          // Only the future will change
          CallbackFuture future = loadFromDisk(request);
          pendingRequests.swapFuture(request, future);
        }
      } catch (ExecutionException e) {
        // Request failed for some reason
        Throwable original = e.getCause();
        Log.e("WebImage", "Failed to fetch image: " + request.imageUrl, original);

        for (ImageRequest.Listener listener : pendingRequests.getListeners(request)) {
          listener.onBitmapLoadError(original.getMessage());
        }
        
        pendingRequests.removeRequest(request);
      } catch (InterruptedException e) {
        // In case we get interrupted while getting the value,
        // shouldn't be able to happen as we only call it after completion
        throw new RuntimeException(e);
      }
    }
  };
}
