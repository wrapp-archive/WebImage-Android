package com.wrapp.android.webimage;

import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;

public class ImageLoader {
  private static final long SHUTDOWN_TIMEOUT_IN_MS = 100;
  
  private static ImageLoader instance;

  private Context context;
  private Handler handler;

  // Worker threads for different tasks, ordered from fast -> slow
  private ExecutorService fileLoader;
  private ExecutorService checkTimestamp;
  private ExecutorService download;
  
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
  
  private void createExecutors() {
    fileLoader = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.NORM_PRIORITY - 1));
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
      task.future.cancel(false);
      
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
    Future<?> future = fileLoader.submit(new FileLoadTask(request, fileLoadListener));
    pending.put(request.listener, new PendingTask(request.imageUrl, future));
  }
  
  private void loadFromUrl(ImageRequest request) {
    Future<?> future = download.submit(new DownloadTask(request, downloadListener));
    pending.put(request.listener, new PendingTask(request.imageUrl, future));
  }
  
  private boolean requestValid(ImageRequest request) {
    PendingTask task = pending.get(request.listener);
    
    return task != null && task.url.equals(request.imageUrl);
  }
  
  public interface FileLoadListener {
    void onComplete(RequestResponse response);
  }
  
  public interface DownloadListener {
    void onComplete(ImageRequest request);
  }
  
  private FileLoadListener fileLoadListener = new FileLoadListener() {
    @Override
    public void onComplete(RequestResponse response) {
      ImageRequest request = response.originalRequest;
      
      // Are we still pending?
      if (requestValid(request)) {
        request.listener.onBitmapLoaded(response);
        pending.remove(request.listener);
      } else {
        // The listener wants something else, ignore
        LogWrapper.logMessage("Request no longer pending, dropping: " + request.imageUrl);
      }
    }
  };
  
  private DownloadListener downloadListener = new DownloadListener() {
    @Override
    public void onComplete(ImageRequest request) {
      // Are we still pending?
      if (requestValid(request)) {
        loadFromDisk(request);
      } else {
        // The listener wants something else, ignore
        LogWrapper.logMessage("Request no longer pending, dropping: " + request.imageUrl);
      }
    }
  };

  private static class PendingTask {
    public URL url;
    public Future<?> future;
    
    public PendingTask(URL url, Future<?> future) {
      this.url = url;
      this.future = future;
    }
  }
}
