/*
 * Copyright (c) 2012 Bohemian Wrappsody AB
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

import java.util.*;

public abstract class TaskQueueThread extends Thread {
  private static final long SHUTDOWN_TIMEOUT_IN_MS = 100;
  private final Queue<ImageRequest> pendingRequests;
  private boolean isRunning;

  protected abstract Bitmap processRequest(ImageRequest request);
  protected abstract void onRequestComplete(RequestResponse response);
  protected abstract void onRequestCancelled(ImageRequest request);

  public TaskQueueThread(final String taskName) {
    super(taskName);
    pendingRequests = new LinkedList<ImageRequest>();
  }

  @Override
  public void run() {
    LogWrapper.logMessage("Starting up task " + getName());
    ImageRequest request;
    isRunning = true;
    while(isRunning) {
      synchronized(pendingRequests) {
        while(pendingRequests.isEmpty() && isRunning) {
          try {
            pendingRequests.wait();
          }
          catch(InterruptedException e) {
            isRunning = false;
            break;
          }
        }

        try {
          request = getNextRequest(pendingRequests);
        }
        catch(Exception e) {
          continue;
        }
      }

      try {
        if(request != null && request.listener != null) {
          try {
            Bitmap bitmap = processRequest(request);
            synchronized(pendingRequests) {
              if(isRequestStillValid(request, pendingRequests)) {
                if(bitmap != null) {
                  onRequestComplete(new RequestResponse(bitmap, request));
                }
              }
              else {
                LogWrapper.logMessage("Bitmap request is no longer valid: " + request.imageUrl);
                onRequestCancelled(request);
              }
            }
          }
          catch(Exception e) {
            request.listener.onBitmapLoadError(e.getMessage());
          }
        }
      }
      catch(Exception e) {
        LogWrapper.logException(e);
      }
    }

    LogWrapper.logMessage("Shutting down task " + getName());
  }

  @Override
  public void interrupt() {
    super.interrupt();
    isRunning = false;
  }

  public void addTask(ImageRequest request) {
    synchronized(pendingRequests) {
      pendingRequests.add(request);
      pendingRequests.notifyAll();
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

    return request;
  }

  private boolean isRequestStillValid(ImageRequest finishedRequest, Queue<ImageRequest> requestQueue) {
    for(ImageRequest checkRequest : requestQueue) {
      if(finishedRequest.listener.equals(checkRequest.listener) &&
        !finishedRequest.imageUrl.equals(checkRequest.imageUrl)) {
        return false;
      }
    }
    return true;
  }

  public void cancelAllRequests() {
    synchronized(pendingRequests) {
      for(ImageRequest request : pendingRequests) {
        request.listener.onBitmapLoadCancelled();
        request.listener = null;
      }
      pendingRequests.clear();
    }
  }

  public void shutdown() {
    try {
      interrupt();
      join(SHUTDOWN_TIMEOUT_IN_MS);
    }
    catch(InterruptedException e) {
      LogWrapper.logException(e);
    }
  }
}
