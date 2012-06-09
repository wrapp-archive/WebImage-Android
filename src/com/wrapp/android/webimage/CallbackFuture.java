package com.wrapp.android.webimage;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import android.graphics.Bitmap;
import android.os.Handler;

public class CallbackFuture extends FutureTask<Bitmap> {
  public interface Listener {
    void onComplete(ImageRequest request);
  }
  
  private ImageRequest request;
  private Listener listener;
  private Handler handler;
  
  public CallbackFuture(Callable<Bitmap> callback, ImageRequest request, Listener listener, Handler handler) {
    super(callback);
    
    this.request = request;
    this.listener = listener;
    this.handler = handler;
  }
  
  public CallbackFuture(Runnable runnable, Bitmap result, ImageRequest request, Listener listener, Handler handler) {
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
          listener.onComplete(request);
        }
      });
    }
  }
}
