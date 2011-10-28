package com.wrapp.android.webimage;

import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

public class ImageLoader {
  private static final int NUM_WORKERS = 2;

  private static ImageLoader staticInstance;
  private final Queue<ImageRequest> pendingRequests = new LinkedList<ImageRequest>();
  private final Worker[] workerPool = new Worker[NUM_WORKERS];


  private static class Worker extends Thread {
    @Override
    public void run() {
      final Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
      ImageRequest request;
      while(true) {
        synchronized(requestQueue) {
          while(requestQueue.isEmpty()) {
            try {
              requestQueue.wait();
            }
            catch(InterruptedException e) {
              e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
          }

          request = requestQueue.poll();
        }

        processRequest(request);
      }
    }

    private void processRequest(ImageRequest request) {
      request.listener.onDrawableLoaded(ImageCache.loadImage(request));
    }
  }


  private static ImageLoader getInstance() {
    if(staticInstance == null) {
      staticInstance = new ImageLoader();
    }
    return staticInstance;
  }

  private ImageLoader() {
    for(int i = 0; i < NUM_WORKERS; i++) {
      workerPool[i] = new Worker();
      workerPool[i].start();
    }
  }

  public static void load(URL imageUrl, ImageRequest.Listener listener, boolean cacheInMemory) {
    Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
    synchronized(requestQueue) {
      for(ImageRequest request : requestQueue) {
        if(request.listener.equals(listener)) {
          if(request.imageUrl.equals(imageUrl)) {
            return;
          }
          else {
            // TODO: Check for same request but with different URL
          }
        }
      }

      // TODO: Check running tasks for duplicate jobs
    }

    synchronized(requestQueue) {
      requestQueue.add(new ImageRequest(imageUrl, listener, cacheInMemory));
      // TODO: Use notifyAll() instead?
      requestQueue.notify();
    }
  }

  public static void enableLogging(String tag, int level) {
    LogWrapper.tag = tag;
    LogWrapper.level = level;
  }
}
