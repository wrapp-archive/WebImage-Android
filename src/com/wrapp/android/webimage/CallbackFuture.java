package com.wrapp.android.webimage;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import android.os.Handler;

public class CallbackFuture<T> extends FutureTask<T> {
  public interface Listener<T> {
    void onComplete(ImageRequest request, Future<T> future);
  }
  
  private ImageRequest request;
  private Listener<T> listener;
  private Handler handler;
  
  public CallbackFuture(Callable<T> callback, ImageRequest request, Listener<T> listener, Handler handler) {
    super(callback);
    
    this.request = request;
    this.listener = listener;
    this.handler = handler;
  }
  
  public CallbackFuture(Runnable runnable, T result, ImageRequest request, Listener<T> listener, Handler handler) {
    super(runnable, result);
    
    this.request = request;
    this.listener = listener;
    this.handler = handler;
  }

  @Override
  protected void done() {
    // Don't call callback if we were cancelled
    if (!isCancelled()) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          listener.onComplete(request, CallbackFuture.this);
        }
      });
    }
  }
}
