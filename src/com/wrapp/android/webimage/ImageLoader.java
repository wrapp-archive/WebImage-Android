package com.wrapp.android.webimage;

import android.graphics.drawable.Drawable;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class ImageLoader {
  private static final int NUM_WORKERS = 1;

  private static ImageLoader staticInstance;
  private final Queue<ImageRequest> pendingRequests = new LinkedList<ImageRequest>();
  private final Worker[] workerPool = new Worker[NUM_WORKERS];
  private final ImageRequest[] runningRequests = new ImageRequest[NUM_WORKERS];


  private static class Worker extends Thread {
    private int index;

    public Worker(int index) {
      this.index = index;
    }

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
              // Log, but otherwise ignore. Not a big deal.
              LogWrapper.logException(e);
            }
          }

          request = requestQueue.poll();
          Iterator requestIterator = requestQueue.iterator();
          while(requestIterator.hasNext()) {
            ImageRequest checkRequest = (ImageRequest)requestIterator.next();
            if(request.listener.equals(checkRequest.listener)) {
              if(request.imageUrl.equals(checkRequest.imageUrl)) {
                // Ignore duplicate requests. This is common when doing view recycling in list adapters
                requestIterator.remove();
              }
              else {
                // If the listener is the same but the request is for a new URL, remove the previous
                // request from the pending queue
                request = checkRequest;
                requestIterator.remove();
              }
            }
          }
        }

        processRequest(request);
      }
    }

    private void processRequest(ImageRequest request) {
      final ImageRequest[] runningRequests = getInstance().runningRequests;
      Drawable drawable;

      synchronized(runningRequests) {
        runningRequests[index] = request;
        drawable = ImageCache.loadImage(request);
      }
      synchronized(runningRequests) {
        if(request != null) {
          request.listener.onDrawableLoaded(drawable);
          runningRequests[index] = null;
        }
        else {
          LogWrapper.logMessage("Interrupted, returning");
        }
      }
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
      workerPool[i] = new Worker(i);
      workerPool[i].start();
    }
  }

  public static void load(URL imageUrl, ImageRequest.Listener listener, boolean cacheInMemory) {
    // TODO: This might be too much work in the GUI thread. Move to a worker?

    Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
/*
      synchronized(requestQueue) {
      Iterator requestIterator = requestQueue.iterator();
      while(requestIterator.hasNext()) {
        ImageRequest request = (ImageRequest)requestIterator.next();
        if(request.listener.equals(listener)) {
          if(request.imageUrl.equals(imageUrl)) {
            // Ignore duplicate requests. This is common when doing view recycling in list adapters
            return;
          }
          else {
            // If the listener is the same but the request is for a new URL, remove the previous
            // request from the pending queue
            requestIterator.remove();
          }
        }
      }
      */

/*
      final ImageRequest[] runningRequests = getInstance().runningRequests;
      synchronized(runningRequests) {
        for(int i = 0; i < runningRequests.length; i++) {
          ImageRequest request = runningRequests[i];
          if(request != null) {
            if(request.listener.equals(listener)) {
              if(request.imageUrl.equals(imageUrl)) {
                // Ignore duplicate requests. This is common when doing view recycling in list adapters
                return;
              }
              else {
                // Null out the running request in this index. The job will continue running, but when
                // it returns it will skip notifying the listener and start processing the next job in
                // the pending request queue.
                runningRequests[i] = null;
              }
            }
          }
        }
      }
    }
*/

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
