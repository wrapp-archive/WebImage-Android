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

import java.net.URL;

/**
 * Endpoint class for all main library tasks.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class WebImage {
  /**
   * Load an image from URL to the given listener. This is a non-blocking call which is run in
   * a background thread. It is safe to call this method multiple times; duplicate requests will
   * be ignored.
   * @param imageUrl URL to load the image from
   * @param listener Object which will be notified when the request is complete
   * @param cacheInMemory If true, then keep a copy of the drawable in memory for quick access.
   * Be careful about using this flag; although WebImage tries to minimize memory consumption and
   * uses soft references, it is easy to exceed the device's VM limits and get OutOfMemory
   * exceptions.
   */
  public static void load(URL imageUrl, ImageRequest.Listener listener, boolean cacheInMemory) {
    ImageLoader.load(imageUrl, listener, cacheInMemory);
  }

  /**
   * Cancel all pending requests. The parent activity should call this method when it is about
   * to be stopped or paused, or else you will waste resources by running in the background.
   */
  public static void cancelAllRequests() {
    ImageLoader.cancelAllRequests();
  }

  /**
   * Empty the first-try memory cache. The parent activity should call this method when it receives
   * low memory warnings.
   */
  public static void clearMemoryCaches() {
    ImageCache.clearMemoryCaches();
  }

  /**
   * Remove old files from the file cache. The parent application should call this method once during
   * initialization to prevent the file cache from growing too large.
   */
  public static void clearOldCacheFiles() {
    ImageCache.clearOldCacheFiles();
  }

  /**
   * Remove cached files older than this many seconds from the file cache. Call with 0 to remove all
   * files in the cache.
   * @param cacheAgeInSec Maximum age of file, in seconds
   */
  public static void clearOldCacheFiles(long cacheAgeInSec) {
    ImageCache.clearOldCacheFiles(cacheAgeInSec);
  }

  /**
   * By default, the WebImage library is silent and will not produce any output to the console. During
   * debugging you may wish to call this method in your app's initialization method to see debugging
   * output to the logcat.
   * @param tag Android logging tag to use
   * @param level Android log level to use
   */
  public static void enableLogging(String tag, int level) {
    LogWrapper.enableLogging(tag, level);
  }

  /**
   * Override the location used to save images on the SD card. By default, this is on the SD card under
   * /data/images. Calling this method will save images to /data/com.example.yourapp/subdirectoryName.
   * @param packageName Package name to use, you probably want Context.getPackageName()
   * @param subdirectoryName Subdirectory name to use
   */
  public static void setCacheDirectory(String packageName, String subdirectoryName) {
    ImageCache.setCacheDirectory(packageName, subdirectoryName);
  }
}
