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

public class ImageLoader {
  // Static singleton instance
  private static ImageLoader staticInstance;

  // Worker threads for different tasks, ordered from fast -> slow
  private RequestRouterThread requestRouterThread;
  private FileLoaderThread fileLoaderThread;
  private CheckTimestampThread checkTimestampThread;
  private DownloadThread downloadThread;

  public static ImageLoader getInstance() {
    if(staticInstance == null) {
      staticInstance = new ImageLoader();
    }
    return staticInstance;
  }

  private ImageLoader() {
    fileLoaderThread = FileLoaderThread.getInstance();
    fileLoaderThread.start();
    checkTimestampThread = CheckTimestampThread.getInstance();
    checkTimestampThread.start();
    downloadThread = DownloadThread.getInstance();
    downloadThread.start();
    requestRouterThread = RequestRouterThread.getInstance();
    requestRouterThread.start();
  }

  public static void load(final Context context, URL imageUrl, ImageRequest.Listener listener, BitmapFactory.Options options) {
    final ImageLoader instance = getInstance();
    instance.requestRouterThread.addTask(new ImageRequest(context, imageUrl, listener, options));
  }

  public static void cancelAllRequests() {
    final ImageLoader imageLoader = getInstance();
    imageLoader.requestRouterThread.cancelAllRequests();
    imageLoader.fileLoaderThread.cancelAllRequests();
    imageLoader.checkTimestampThread.cancelAllRequests();
    imageLoader.downloadThread.cancelAllRequests();
  }

  public static void shutdown() {
    LogWrapper.logMessage("Shutting down");
    final ImageLoader imageLoader = getInstance();
    imageLoader.requestRouterThread.shutdown();
    RequestRouterThread.staticInstance = null;
    imageLoader.fileLoaderThread.shutdown();
    FileLoaderThread.staticInstance = null;
    imageLoader.checkTimestampThread.shutdown();
    CheckTimestampThread.staticInstance = null;
    imageLoader.downloadThread.shutdown();
    DownloadThread.staticInstance = null;
    staticInstance = null;
  }
}
