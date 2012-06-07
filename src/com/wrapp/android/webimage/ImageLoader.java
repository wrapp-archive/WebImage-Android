package com.wrapp.android.webimage;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
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
  
  private WeakHashMap<ImageRequest.Listener, ImageRequest> pendingListeners;
  private WeakHashMap<ImageRequest, PendingTask> pendingTasks;

  public static ImageLoader getInstance(Context context) {
    if (instance == null) {
      instance = new ImageLoader(context);
    }

    return instance;
  }

  public ImageLoader(Context context) {
    this.context = context.getApplicationContext();
    handler = new Handler();

    pendingListeners = new WeakHashMap<ImageRequest.Listener, ImageRequest>();
    pendingTasks = new WeakHashMap<ImageRequest, ImageLoader.PendingTask>();
    
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
    checkTimestamp.submit(new CheckTimeStampTask(context, request));
  }
  
  void forceUpdateImage(URL url, BitmapFactory.Options options) {
    // TODO: Actually let this update the view
    ImageRequest request = new ImageRequest(url, options);
    request.forceDownload = true;
    
    download.submit(new DownloadTask(context, request));
  }
  
  private void createExecutors() {
    fileLoader = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.NORM_PRIORITY - 1));
    checkTimestamp = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.MIN_PRIORITY));
    download = new AdaptingThreadPoolExecutor(context);
  }
  
  private void cancelAllRequestsInternal() {
    for (Map.Entry<ImageRequest, PendingTask> entry : pendingTasks.entrySet()) {
      entry.getValue().future.cancel(false);
    }

    // TODO: We should call the correct callbacks
    pendingTasks.clear();
    pendingListeners.clear();
  }
  
  private void shutdownInternal() {
    LogWrapper.logMessage("Shutting down");
    try {
      download.shutdownNow();
      checkTimestamp.shutdownNow();
      fileLoader.shutdownNow();
      
      pendingTasks.clear();
      pendingListeners.clear();
      
      download.awaitTermination(SHUTDOWN_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Ignore
    }
    
    // TODO: What are we supposed to do?
    createExecutors();
  }

  private void load(URL imageUrl, ImageRequest.Listener listener, BitmapFactory.Options options) {
    ImageRequest request = new ImageRequest(imageUrl, options);
    
    ImageRequest pendingRequest = pendingListeners.get(listener);
    if (request.equals(pendingRequest)) {
      // We are already pending for this request, do nothing
      return;
    } else if (pendingRequest != null) {
      // This listener is pending for another request, remove us from it
      LogWrapper.logMessage("Already pending, old: " + pendingRequest.imageUrl + ", new: " + request.imageUrl);
      pendingListeners.remove(listener);
      
     if (!pendingTasks.get(pendingRequest).removeListener(listener)) {
       // We were the only ones waiting for this task, remove it
       pendingTasks.remove(pendingRequest);
     }
    }
    
    PendingTask pendingTask = pendingTasks.get(request);
    if (pendingTask == null) {
      // No other listeners are pending for this request, create it
      CallbackTask task;
      if (ImageCache.isImageCached(context, request.imageKey)) {
        task = loadFromDisk(request);
      } else {
        task = loadFromUrl(request);
      }
      
      pendingTask = new PendingTask(task);
      pendingTasks.put(request, pendingTask);
    } else {
      LogWrapper.logMessage("Reusing existing request: " + request.imageUrl);
    }
    
    pendingTask.addListener(listener);
    pendingListeners.put(listener, request);
  }
  
  private CallbackTask loadFromDisk(ImageRequest request) {
    CallbackTask task = new CallbackTask(new FileLoadTask(context, request), request, completionListener, handler);
    fileLoader.submit(task);
    
    return task;
  }
  
  private CallbackTask loadFromUrl(ImageRequest request) {
    CallbackTask task = new CallbackTask(new DownloadTask(context, request), request, completionListener, handler);
    download.submit(task);
    
    return task;
  }

  private CallbackTask.Listener completionListener = new CallbackTask.Listener() {
    @Override
    public void onComplete(ImageRequest request) {
      PendingTask task = pendingTasks.get(request);
      if (task == null) {
        // No longer pending
        LogWrapper.logMessage("Request no longer pending, dropping: " + request.imageUrl);
        
        return;
      }
      
      try {
        Bitmap b = task.future.get();

        if (b != null) {
          // Image was fetched from disk
          for (ImageRequest.Listener listener : task.getListeners()) {
            listener.onBitmapLoaded(request, b);
            pendingListeners.remove(listener);
          }
          
          pendingTasks.remove(request);
        } else {
          // Image was downloaded to disk, fetch it
          // Only the future will change
          CallbackTask future = loadFromDisk(request);
          task.future = future;
        }
      } catch (ExecutionException e) {
        // Request failed for some reason
        Throwable original = e.getCause();
        Log.e("WebImage", "Failed to fetch image: " + request.imageUrl, original);

        for (ImageRequest.Listener listener : task.getListeners()) {
          listener.onBitmapLoadError(original.getMessage());
          pendingListeners.remove(listener);
        }

        pendingTasks.remove(request);
      } catch (InterruptedException e) {
        // In case we get interrupted while getting the value,
        // shouldn't be able to happen as we only call it after completion
        throw new RuntimeException(e);
      }
    }
  };

  private static class PendingTask {
    public Future<Bitmap> future;
    
    private Set<ImageRequest.Listener> listeners;
    
    public PendingTask(Future<Bitmap> future) {
      this.future = future;
      
      listeners = Collections.newSetFromMap(new WeakHashMap<ImageRequest.Listener, Boolean>());
    }
    
    public void addListener(ImageRequest.Listener listener) {
      listeners.add(listener);
    }
    
    /**
     * Remove a listener
     * @return true if this task is still pending
     */
    public boolean removeListener(ImageRequest.Listener listener) {
      if (!listeners.remove(listener)) {
        throw new IllegalStateException("Request was not pending for this task");
      }
      
      if (listeners.isEmpty()) {
        // No other listeners, cancel this task
        // It doesn't matter if successfully cancel it, if we don't
        // the it will be ignored in the callback anyways
        future.cancel(false);
        
        listener.onBitmapLoadCancelled();
        
        return false;
      } else {
        // Task is still pending for others
        listener.onBitmapLoadCancelled();
        
        return true;
      }
    }
    
    public Set<ImageRequest.Listener> getListeners() {
      return listeners;
    }
  }
}
