package com.wrapp.android.webimage;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.wrapp.android.webimage.DispatchTask.NextTask;

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
  private ExecutorService dispatchExecutor;
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
    dispatchExecutor = Executors.newSingleThreadExecutor(new PriorityThreadFactory(Thread.NORM_PRIORITY - 1));
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

  private void load(URL imageUrl, ImageRequest.Listener listener, BitmapFactory.Options options) {
    ImageRequest request = new ImageRequest(imageUrl, options);
    
    if (!pendingRequests.addRequest(request, listener)) {
      // Start with the dispatch task who checks to see if
      // the image is cached and then dispatches it
      CallbackFuture<?> task = dispatchRequest(request);
      
      pendingRequests.addTask(request, listener, task);
    }
  }
  
  private CallbackFuture<?> dispatchRequest(ImageRequest request) {
    CallbackFuture<DispatchTask.NextTask> task =
        new CallbackFuture<DispatchTask.NextTask>(new DispatchTask(context, request), request, dispatchListener, handler);
    dispatchExecutor.submit(task);
    
    return task;
  }

  private CallbackFuture<?> loadFromUrl(ImageRequest request) {
    CallbackFuture<Void> task = new CallbackFuture<Void>(new DownloadTask(context, request), request, downloadListener, handler);
    downloadExecutor.submit(task);
    
    return task;
  }
  
  private CallbackFuture<?> loadFromDisk(ImageRequest request) {
    CallbackFuture<Bitmap> task = new CallbackFuture<Bitmap>(new FileLoadTask(context, request), request, fileListener, handler);
    fileLoaderExecutor.submit(task);
    
    return task;
  }
  
  private BaseListener<DispatchTask.NextTask> dispatchListener = new BaseListener<DispatchTask.NextTask>() {
    @Override
    public void requestComplete(ImageRequest request, Future<NextTask> future) throws ExecutionException, InterruptedException {
      CallbackFuture<?> newFuture = null;
      
      switch (future.get()) {
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
  
  private BaseListener<Void> downloadListener = new BaseListener<Void>() {
    @Override
    public void requestComplete(ImageRequest request, Future<Void> future) throws ExecutionException, InterruptedException {
      // Download completed, load from disk
      future.get();
      
      CallbackFuture<?> newFuture = loadFromDisk(request);
      pendingRequests.swapFuture(request, newFuture);
    }
  };
  
  private BaseListener<Bitmap> fileListener = new BaseListener<Bitmap>() {
    @Override
    public void requestComplete(ImageRequest request, Future<Bitmap> future) throws ExecutionException, InterruptedException {
      // Bitmap was loaded from disk
      Bitmap b = future.get();
      
      for (ImageRequest.Listener listener : pendingRequests.getListeners(request)) {
        listener.onBitmapLoaded(request, b);
      }
      
      // We are done
      pendingRequests.removeRequest(request);
    }
  };
  
  private abstract class BaseListener<T> implements CallbackFuture.Listener<T> {
    @Override
    public final void onComplete(ImageRequest request, Future<T> future) {
      // Check if we are still interested in this request
      if (!pendingRequests.isPending(request)) {
        // No longer pending
        LogWrapper.logMessage("Request no longer pending, dropping: " + request.imageUrl);
        
        return;
      }
      
      try {
        requestComplete(request, future);
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
    
    public abstract void requestComplete(ImageRequest request, Future<T> future) throws ExecutionException, InterruptedException;
  }
}
