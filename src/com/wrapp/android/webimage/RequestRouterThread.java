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

public class RequestRouterThread extends TaskQueueThread {
  public RequestRouterThread() {
    super("RequestRouter");
  }

  @Override
  protected Bitmap processRequest(ImageRequest request) {
    // Check possible locations for the image from fastest to slowest, sending the
    // request to the respective thread, with the exception of memory cache hits
    // which are handled immediately.
    final Bitmap bitmap = ImageCache.loadImageFromMemoryCache(request.imageKey);
    if(bitmap != null) {
      LogWrapper.logMessage("Found image " + request.imageUrl + " in memory cache");
      return bitmap;
    }
    else if(ImageCache.isImageCached(request.context, request.imageKey)) {
      LogWrapper.logMessage("Found image " + request.imageUrl + " in file cache");
      FileLoaderThread.getInstance().addTask(request);
    }
    else {
      DownloadThread.getInstance().addTask(request);
    }

    return null;
  }
}
