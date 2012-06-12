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

/** Endpoint class for all main library tasks. */
public class WebImage {
  private static ImageLoader imageLoader;
  
  public static ImageLoader getLoader(Context context) {
    if (imageLoader == null) {
      imageLoader = new ImageLoader(context);
    }
    
    return imageLoader;
  }
  
  // Loading Images ////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Load an image from URL to the given listener. This is a non-blocking call which is run in
   * a background thread. It is safe to call this method multiple times; duplicate requests will
   * be ignored.
   * @param context Context used for getting app's package name
   * @param imageUrl URL to load the image from
   * @param listener Object which will be notified when the request is complete
   */
  public static void load(Context context, String imageUrl, ImageRequest.Listener listener) {
    load(context, new ImageRequest(imageUrl), listener);
  }

  /**
   * Load an image from URL to the given listener. This is a non-blocking call which is run in
   * a background thread. It is safe to call this method multiple times; duplicate requests will
   * be ignored.
   * @param context Context used for getting app's package name
   * @param imageUrl URL to load the image from
   * @param listener Object which will be notified when the request is complete
   * @param options Options to use when loading the image. See the documentation for {@link BitmapFactory.Options}
   * for more details. Can be null.
   */
  public static void load(Context context, String imageUrl, ImageRequest.Listener listener, BitmapFactory.Options options) {
    load(context, new ImageRequest(imageUrl, new StandardBitmapLoader(options)), listener);
  }
  
  /**
   * Load an image from URL to the given listener. This is a non-blocking call which is run in
   * a background thread. It is safe to call this method multiple times; duplicate requests will
   * be ignored.
   * @param context Context used for getting app's package name
   * @param request Image request
   * @param listener Object which will be notified when the request is complete
   */
  public static void load(Context context, ImageRequest request, ImageRequest.Listener listener) {
    getLoader(context).load(request, listener);
  }

  // Image Cache Operations ////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Check to see if an image has already been saved in the file cache. This can be useful when
   * you want to display an animation or other GUI notification in case the image has to be
   * fetched from the net.
   * @param context Context used for getting app's package name
   * @param imageUrl URL to check
   * @return True if the image is in the file cache, false otherwise
   */
  public static boolean isImageCached(Context context, String imageUrl) {
    return ImageCache.isImageCached(context, ImageCache.getCacheKeyForUrl(imageUrl));
  }

  /**
   * Remove old files from the file cache. The parent application should call this method once during
   * initialization to prevent the file cache from growing too large.
   * @param context Context used for getting app's package name
   */
  public static void clearOldCacheFiles(Context context) {
    ImageCache.clearOldCacheFiles(context);
  }

  /**
   * Remove cached files older than this many seconds from the file cache. Call with 0 to remove all
   * files in the cache.
   * @param context Context used for getting app's package name
   * @param cacheAgeInSec Maximum age of file, in seconds
   */
  public static void clearOldCacheFiles(Context context, long cacheAgeInSec) {
    ImageCache.clearOldCacheFiles(context, cacheAgeInSec);
  }

  /**
   * Remove a single image from the disk cache
   * @param context Context used for getting app's package name
   * @param imageUrl Image URL to remove
   */
  public static void clearImageFromCaches(Context context, String imageUrl) {
    ImageCache.clearImageFromCaches(context, imageUrl);
  }

  // Configuration /////////////////////////////////////////////////////////////////////////////////////////////////////

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
   * Set the maximum number of threads to be used for downloading images. The actual number of download
   * threads varies depending on the phone's network connection. Smaller apps which only load a few
   * images may want to set this value to 1.
   * Note that this does not effect the total number of threads started by WebImage; there will still
   * be other background threads for reading cached images, checking timestamps, etc.
   * @param value Number of threads
   */
  public static void setMaxDownloadThreads(Context context, int value) {
    AdaptingThreadPoolExecutor.setMaxThreads(value);
  }

  // Thread Control Operations /////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Cancel all pending requests. The parent activity should call this method when it is about
   * to be stopped or paused, or else you will waste resources by running in the background.
   */
  public static void cancelAllRequests(Context context) {
    getLoader(context).cancelAllRequests();
  }

  /**
   * Call this method to manually force a resize check for the download thread pool size. Normally
   * the preferred way of doing this is to instead use the BroadcastReceiver provided by the
   * DownloadThreadPool class (see the example app for a demonstration of this).
   * However, you may also wish to manually call this, for instance when your application is resumed
   * and any network changes may not have been caught by your app.
   * @param context Activity's context
   */
  public static void onNetworkStatusChanged(Context context) {
    getLoader(context).getDownloadExecutor().resizeThreadPool();
  }

  /**
   * Stop all background threads. Call this when your application quits. Can also be called when
   * the app is paused to free up additional resources. Note that the next request to load an
   * image will re-inialize the thread pool.
   */
  public static void shutdown(Context context) {
    getLoader(context).shutdown();
  }
}
