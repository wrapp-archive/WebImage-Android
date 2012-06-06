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

import android.content.Context;
import android.graphics.BitmapFactory;

import java.net.URL;

public class ImageLoaderOld {
  // Static singleton instance
  private static ImageLoaderOld staticInstance;

  // Worker threads for different tasks, ordered from fast -> slow
  private RequestRouterThread requestRouterThread;
  private FileLoaderThread fileLoaderThread;
  private CheckTimestampThread checkTimestampThread;
  private DownloadThreadPool downloadThreadPool;

  public static ImageLoaderOld getInstance(Context context) {
    if(staticInstance == null) {
      staticInstance = new ImageLoaderOld(context);
    }
    return staticInstance;
  }

  private ImageLoaderOld(final Context context) {
    fileLoaderThread = FileLoaderThread.getInstance();
    fileLoaderThread.start();
    checkTimestampThread = CheckTimestampThread.getInstance();
    checkTimestampThread.start();
    downloadThreadPool = DownloadThreadPool.getInstance();
    downloadThreadPool.start(context);
    requestRouterThread = RequestRouterThread.getInstance();
    requestRouterThread.start();
  }

  public static void load(final Context context, URL imageUrl, ImageRequest.Listener listener, BitmapFactory.Options options) {
    final ImageLoaderOld instance = getInstance(context);
    instance.requestRouterThread.addTask(new ImageRequest(context, imageUrl, listener, options));
  }

  public static void cancelAllRequests() {
    final ImageLoaderOld imageLoader = getInstance(null);
    imageLoader.requestRouterThread.cancelAllRequests();
    imageLoader.fileLoaderThread.cancelAllRequests();
    imageLoader.checkTimestampThread.cancelAllRequests();
    imageLoader.downloadThreadPool.cancelAllRequests();
  }

  public static void shutdown() {
    LogWrapper.logMessage("Shutting down");
    final ImageLoaderOld imageLoader = getInstance(null);
    imageLoader.requestRouterThread.shutdown();
    RequestRouterThread.staticInstance = null;
    imageLoader.fileLoaderThread.shutdown();
    FileLoaderThread.staticInstance = null;
    imageLoader.checkTimestampThread.shutdown();
    CheckTimestampThread.staticInstance = null;
    imageLoader.downloadThreadPool.shutdown();
    DownloadThreadPool.staticInstance = null;
    staticInstance = null;
  }
}
