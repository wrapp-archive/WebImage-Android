package com.wrapp.android.webimage;

import android.graphics.drawable.Drawable;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class ImageLoader {
  private static final int NUM_WORKERS = 2;
  private static ImageLoader staticInstance;
  private final Queue<ImageRequest> pendingRequests;


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
              // Log, but otherwise ignore. Not a big deal.
              LogWrapper.logException(e);
            }
          }

          request = getNextRequest(requestQueue);
        }

        processRequest(request);
      }
    }

    private ImageRequest getNextRequest(Queue<ImageRequest> requestQueue) {
      ImageRequest request = requestQueue.poll();
      Iterator requestIterator = requestQueue.iterator();
      while(requestIterator.hasNext()) {
        ImageRequest checkRequest = (ImageRequest)requestIterator.next();
        if(request.listener.equals(checkRequest.listener)) {
          if(request.imageUrl.equals(checkRequest.imageUrl)) {
            // Ignore duplicate requests. This is common when doing view recycling in list adapters.
            requestIterator.remove();
          }
          else {
            // If this request in the queue was made by the same listener but is for a new URL,
            // then use that request instead and remove it from the queue.
            request = checkRequest;
            requestIterator.remove();
          }
        }
      }
      return request;
    }

    private void processRequest(ImageRequest request) {
      Drawable drawable = ImageCache.loadImage(request);
      // When this request is finished, check the pending requests queue again to see if this
      // same listener has made a request for a different image. This is quite common in list
      // adpaters when the user is scrolling quickly. In this case, we return early without
      // notifying the listener, but at least the image will be cached to disk/memory.
      final Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
      synchronized(requestQueue) {
        for(ImageRequest checkRequest : requestQueue) {
          if(request.listener.equals(checkRequest.listener) &&
            !request.imageUrl.equals(checkRequest.listener)) {
            return;
          }
        }
      }
      request.listener.onDrawableLoaded(drawable);
    }
  }


  private static ImageLoader getInstance() {
    if(staticInstance == null) {
      staticInstance = new ImageLoader();
    }
    return staticInstance;
  }

  private ImageLoader() {
    pendingRequests = new LinkedList<ImageRequest>();
    final Worker[] workerPool = new Worker[NUM_WORKERS];
    for(int i = 0; i < NUM_WORKERS; i++) {
      workerPool[i] = new Worker();
      workerPool[i].start();
    }
  }

  public static void load(URL imageUrl, ImageRequest.Listener listener, boolean cacheInMemory) {
    Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
    synchronized(requestQueue) {
      requestQueue.add(new ImageRequest(imageUrl, listener, cacheInMemory));
      requestQueue.notify();
    }
  }

  public static void cancelAllRequests() {
    final Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
    synchronized(requestQueue) {
      requestQueue.clear();
    }
  }
}
