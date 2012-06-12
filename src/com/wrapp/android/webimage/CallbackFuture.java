package com.wrapp.android.webimage;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.os.Handler;
import android.util.Log;

public class CallbackFuture<T> extends FutureTask<T> {
  public interface Callback<T> {
    void onSuccess(ImageRequest request, T value);
    void onFailure(ImageRequest request, Throwable t);
  }
  
  private ImageRequest request;
  private Callback<T> callback;
  private Handler handler;
  
  public CallbackFuture(Callable<T> callable, ImageRequest request, Callback<T> callback, Handler handler) {
    super(callable);
    
    this.request = request;
    this.callback = callback;
    this.handler = handler;
  }
  
  public CallbackFuture(Runnable runnable, T result, ImageRequest request, Callback<T> callback, Handler handler) {
    super(runnable, result);
    
    this.request = request;
    this.callback = callback;
    this.handler = handler;
  }

  @Override
  protected void done() {
    // Don't call callback if we were cancelled
    if (!isCancelled()) {
      try {
        final T value = get();
        handler.post(new Runnable() {
          @Override
          public void run() {
            callback.onSuccess(request, value);
          }
        });
      } catch (ExecutionException e) {
        final Throwable t = e.getCause();
        
        handler.post(new Runnable() {
          @Override
          public void run() {
            callback.onFailure(request, t);
          }
        });
      } catch (InterruptedException e) {
        // Should not be able to happend
        LogWrapper.logException("get() was interrupted", e);
        
        throw new RuntimeException(e);
      }
    }
  }
}
