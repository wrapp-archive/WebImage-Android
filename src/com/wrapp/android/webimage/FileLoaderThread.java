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
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

public class FileLoaderThread extends TaskQueueThread {
  static FileLoaderThread staticInstance;

  public static FileLoaderThread getInstance() {
    if(staticInstance == null) {
      staticInstance = new FileLoaderThread();
    }
    return staticInstance;
  }

  private FileLoaderThread() {
    super("FileLoader");
    // Set to be slightly below normal priority so that the GUI thread runs a bit snappier
    setPriority(Thread.NORM_PRIORITY - 1);
  }

  @Override
  protected Bitmap processRequest(ImageRequest request) {
    Bitmap bitmap = null;

    FileInputStream inputStream = null;
    File cacheFile = new File(ImageCache.getCacheDirectory(request.context), request.imageKey);
    if(cacheFile.exists()) {
      try {
        Date now = new Date();
        long fileAgeInMs = now.getTime() - cacheFile.lastModified();
        if(fileAgeInMs > ImageCache.getCacheRecheckAgeInMs()) {
          CheckTimestampThread.getInstance().addTask(request);
        }

        LogWrapper.logMessage("Loading image " + request.imageUrl + " from file cache");
        inputStream = new FileInputStream(cacheFile);
        bitmap = BitmapFactory.decodeFileDescriptor(inputStream.getFD(), null, request.loadOptions);
        if(bitmap == null) {
          throw new Exception("Could not create bitmap from image " + request.imageUrl.toString());
        }
      }
      catch(Exception e) {
        LogWrapper.logException(e);
      }
      finally {
        if(inputStream != null) {
          try {
            inputStream.close();
          }
          catch(IOException e) {
            LogWrapper.logException(e);
          }
        }
      }
    }

    return bitmap;
  }

  @Override
  protected void onRequestComplete(RequestResponse response) {
    response.originalRequest.listener.onBitmapLoaded(response);
  }

  @Override
  protected void onRequestCancelled(ImageRequest request) {
    request.listener.onBitmapLoadCancelled();
  }
}
