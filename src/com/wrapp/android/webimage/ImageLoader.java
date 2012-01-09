/*
 * Copyright (c) 2011 Bohemian Wrappsody AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.wrapp.android.webimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class ImageLoader {
  // Number of threads which download and load images simultaneously. From my testing, 2 is the best
  // value here. Although most servers function quite efficiently with up to 4 simultaneous requests,
  // lower performance phones will start to choke when up to 4 threads are delivering drawables.
  private static final int NUM_WORKERS = 2;

  // Static singleton instance
  private static ImageLoader staticInstance;

  // Instance variables
  private final Queue<ImageRequest> pendingRequests;
  private final Worker[] workerPool;

  private static class Worker extends Thread {
    public ImageRequest.Listener currentListener = null;

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
      // Pop the first element from the pending request queue
      ImageRequest request = requestQueue.poll();

      // Go through the list of pending requests, pruning duplicate requests and using the latest URL
      // requested by a particular listener. It is quite common that a listener will request multiple
      // URL's, especially when a ListView is scrolling quickly.
      Iterator requestIterator = requestQueue.iterator();
      while(requestIterator.hasNext()) {
        ImageRequest checkRequest = (ImageRequest)requestIterator.next();
        if(request.listener.equals(checkRequest.listener)) {
          if(request.imageUrl.equals(checkRequest.imageUrl)) {
            // Ignore duplicate requests. This is common when doing view recycling in list adapters.
            request.listener.onBitmapLoadCancelled();
            requestIterator.remove();
          }
          else {
            // If this request in the queue was made by the same listener but is for a new URL,
            // then use that request instead and remove it from the queue.
            request.listener.onBitmapLoadCancelled();
            request = checkRequest;
            requestIterator.remove();
          }
        }
      }

      // Finally, look at the other worker threads to see what they are processing. If one of them
      // is processing the same listener as this request, then invalidate that listener so the correct
      // image will be delivered by this worker instead.
      for(Worker worker : getInstance().workerPool) {
        if(worker.getId() != getId()) {
          if(worker.currentListener == request.listener) {
            worker.currentListener = null;
          }
        }
      }

      return request;
    }

    private void processRequest(ImageRequest request) {
      try {
        currentListener = request.listener;
        Bitmap bitmap = ImageCache.loadImage(request);

        // While the drawable was loading, another thread may have invalidated our listener. If so,
        // then return right away, but only after informing the (real) listener that this request
        // has been cancelled.
        if(currentListener == null) {
          request.listener.onBitmapLoadCancelled();
          return;
        }

        if(bitmap == null) {
          request.listener.onBitmapLoadError("Failed to load image");
        }
        else {
          // When this request has completed successfully, check the pending requests queue again
          // to see if this same listener has made a request for a different image. This is quite
          // common in list adpaters when the user is scrolling quickly. In this case, we return
          // early without notifying the listener, but at least the image will be cached.
          final Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
          synchronized(requestQueue) {
            for(ImageRequest checkRequest : requestQueue) {
              if(request.listener.equals(checkRequest.listener) &&
                !request.imageUrl.equals(checkRequest.imageUrl)) {
                request.listener.onBitmapLoadCancelled();
                return;
              }
            }
          }
          request.listener.onBitmapLoaded(bitmap);
          currentListener = null;
        }
      }
      catch(Exception e) {
        // Catch any other random exceptions which may be thrown when loading the image. Although
        // the ImageLoader and ImageCache classes do rigorous try/catch checking, it doesn't hurt
        // to have a last line of defence.
        request.listener.onBitmapLoadError(e.getMessage());
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
    pendingRequests = new LinkedList<ImageRequest>();
    workerPool = new Worker[NUM_WORKERS];
    for(int i = 0; i < NUM_WORKERS; i++) {
      workerPool[i] = new Worker();
      workerPool[i].start();
    }
  }

  public static void load(URL imageUrl, ImageRequest.Listener listener) {
    load(imageUrl, listener, false, null);
  }

  public static void load(URL imageUrl, ImageRequest.Listener listener, boolean cacheInMemory, BitmapFactory.Options options) {
    Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
    synchronized(requestQueue) {
      requestQueue.add(new ImageRequest(imageUrl, listener, cacheInMemory, options));
      requestQueue.notify();
    }
  }

  public static void cancelAllRequests() {
    final Queue<ImageRequest> requestQueue = getInstance().pendingRequests;
    synchronized(requestQueue) {
      for(ImageRequest request : requestQueue) {
        request.listener.onBitmapLoadCancelled();
      }
      requestQueue.clear();
    }
  }
}
