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

import java.io.File;
import java.util.Date;

public class CheckTimestampThread extends TaskQueueThread {
  private static CheckTimestampThread staticInstance;

  public CheckTimestampThread() {
    super("CheckTimestamp");
    setPriority(Thread.MIN_PRIORITY);
  }

  public static CheckTimestampThread getInstance() {
    if(staticInstance == null) {
      staticInstance = new CheckTimestampThread();
    }
    return staticInstance;
  }

  @Override
  protected Bitmap processRequest(ImageRequest request) {
    LogWrapper.logMessage("Requesting timestamp for " + request.imageUrl);
    Date expirationDate = ImageDownloader.getServerTimestamp(request.imageUrl);
    Date now = new Date();
    if(expirationDate.after(now)) {
      LogWrapper.logMessage("Cached version of " + request.imageUrl.toString() + " is still current, updating timestamp");
      File cacheFile = new File(ImageCache.getCacheDirectory(request.context), request.imageKey);
      if(!cacheFile.setLastModified(now.getTime())) {
        LogWrapper.logMessage("Can't update timestamp!");
        // Ugh, it seems that in some cases this call will always return false and refuse to update the timestamp
        // For more info, see: http://code.google.com/p/android/issues/detail?id=18624
        // In these cases, we manually re-write the file to disk. Yes, that sucks, but it's better than loosing
        // the ability to do any intelligent file caching at all.
        // TODO: saveImageInFileCache(imageKey, bitmap);
      }
    }
    else {
      LogWrapper.logMessage("Cached version of " + request.imageUrl.toString() + " found, but has expired.");
      DownloadThread.getInstance().addTask(request);
    }
    return null;
  }
}
