package com.wrapp.android.webimage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.wrapp.android.webimage.DispatchTask.NextTask;

public class ImageLoader {
  private static final long SHUTDOWN_TIMEOUT_IN_MS = 100;
  
  private Context context;
  private Handler handler;

  // Worker threads for different tasks, ordered from fast -> slow
  private ExecutorService dispatchExecutor;
  private ExecutorService fileLoaderExecutor;
  private ExecutorService checkTimestampExecutor;
  private AdaptingThreadPoolExecutor downloadExecutor;
  
  private PendingRequests pendingRequests;

  public ImageLoader(Context context) {
    this.context = context.getApplicationContext();
    handler = new Handler();

    pendingRequests = new PendingRequests();
    
    createExecutors();
  }

  AdaptingThreadPoolExecutor getDownloadExecutor() {
    return downloadExecutor;
  }
  
  void checkTimeStamp(ImageRequest request) {
    checkTimestampExecutor.submit(new CheckTimeStampTask(context, request));
  }
  
  void forceUpdateImage(ImageRequest request) {
    // TODO: Actually let this update the view
    ImageRequest newRequest = new ImageRequest(request.imageUrl, request.bitmapLoader);
    newRequest.forceDownload = true;
    
    downloadExecutor.submit(new DownloadTask(context, newRequest));
  }
  
  private void createExecutors() {
    dispatchExecutor = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.NORM_PRIORITY - 1));
    fileLoaderExecutor = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.NORM_PRIORITY - 1));
    checkTimestampExecutor = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.MIN_PRIORITY));
    downloadExecutor = new AdaptingThreadPoolExecutor(context);
  }
  
  public void cancelAllRequests() {
    pendingRequests.clear();
  }
  
  public void shutdown() {
    LogWrapper.logMessage("Shutting down");
    try {
      dispatchExecutor.shutdownNow();
      fileLoaderExecutor.shutdownNow();
      checkTimestampExecutor.shutdownNow();
      downloadExecutor.shutdownNow();
      
      pendingRequests.clear();
      
      downloadExecutor.awaitTermination(SHUTDOWN_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Ignore
    }
    
    // TODO: What are we supposed to do?
    createExecutors();
  }

  public void load(ImageRequest request, ImageRequest.Listener listener) {
    if (!pendingRequests.addRequest(request, listener)) {
      // Start with the dispatch task who checks to see if
      // the image is cached and then dispatches it
      CallbackFuture<?> task = dispatchRequest(request);
      
      pendingRequests.addTask(request, listener, task);
    }
  }
  
  private CallbackFuture<?> dispatchRequest(ImageRequest request) {
    CallbackFuture<DispatchTask.NextTask> task =
        new CallbackFuture<DispatchTask.NextTask>(new DispatchTask(context, request), request, dispatchCallback, handler);
    dispatchExecutor.submit(task);
    
    return task;
  }

  private CallbackFuture<?> loadFromUrl(ImageRequest request) {
    CallbackFuture<Void> task = new CallbackFuture<Void>(new DownloadTask(context, request), request, downloadCallback, handler);
    downloadExecutor.submit(task);
    
    return task;
  }
  
  private CallbackFuture<?> loadFromDisk(ImageRequest request) {
    CallbackFuture<Bitmap> task = new CallbackFuture<Bitmap>(new FileLoadTask(context, request), request, fileCallback, handler);
    fileLoaderExecutor.submit(task);
    
    return task;
  }
  
  private BaseCallback<DispatchTask.NextTask> dispatchCallback = new BaseCallback<DispatchTask.NextTask>() {
    @Override
    public void requestComplete(ImageRequest request, NextTask task) {
      CallbackFuture<?> newFuture = null;
      
      switch (task) {
        case FILE:
          newFuture = loadFromDisk(request);
          break;
        case WEB:
          newFuture = loadFromUrl(request);
          break;
      }
      
      pendingRequests.swapFuture(request,  newFuture);
    }
  };
  
  private BaseCallback<Void> downloadCallback = new BaseCallback<Void>() {
    @Override
    public void requestComplete(ImageRequest request, Void value) {
      // Download completed, load from disk
      CallbackFuture<?> newFuture = loadFromDisk(request);
      pendingRequests.swapFuture(request, newFuture);
    }
  };
  
  private BaseCallback<Bitmap> fileCallback = new BaseCallback<Bitmap>() {
    @Override
    public void requestComplete(ImageRequest request, Bitmap b) {
      // Bitmap was loaded from disk
      for (ImageRequest.Listener listener : pendingRequests.getListeners(request)) {
        listener.onBitmapLoaded(request, b);
      }
      
      // We are done
      pendingRequests.removeRequest(request);
    }
  };
  
  private abstract class BaseCallback<T> implements CallbackFuture.Callback<T> {
    @Override
    public void onSuccess(ImageRequest request, T value) {
      if (!checkPending(request)) {
        return;
      }
      
      requestComplete(request, value);
    }

    @Override
    public void onFailure(ImageRequest request, Throwable t) {
      if (!checkPending(request)) {
        return;
      }
      
      LogWrapper.logException("Failed to fetch image: " + request.imageUrl, t);

      for (ImageRequest.Listener listener : pendingRequests.getListeners(request)) {
        listener.onBitmapLoadError(t.getMessage());
      }
      
      pendingRequests.removeRequest(request);
    }
    
    private boolean checkPending(ImageRequest request) {
      // Check if we are still interested in this request
      if (!pendingRequests.isPending(request)) {
        // No longer pending
        LogWrapper.logMessage("Request no longer pending, dropping: " + request.imageUrl);
        
        return false;
      } else {
        return true;
      }
    }

    public abstract void requestComplete(ImageRequest request, T value);
  }
}
