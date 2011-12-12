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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ImageCache {
  private static final long ONE_DAY_IN_SEC = 24 * 60 * 60;
  private static final long CACHE_RECHECK_AGE_IN_SEC = ONE_DAY_IN_SEC;
  private static final long CACHE_RECHECK_AGE_IN_MS = CACHE_RECHECK_AGE_IN_SEC * 1000;
  private static final long CACHE_EXPIRATION_AGE_IN_SEC = ONE_DAY_IN_SEC * 30;
  private static final long CACHE_EXPIRATION_AGE_IN_MS = CACHE_EXPIRATION_AGE_IN_SEC * 1000;
  private static final String DEFAULT_CACHE_DIRECTORY_NAME = "images";

  private static File cacheDirectory;
  private static Map<String, SoftReference<Drawable>> drawableCache = new HashMap<String, SoftReference<Drawable>>();

  public static boolean isImageCached(URL imageUrl) {
    final String imageKey = getKeyForUrl(imageUrl);
    final File cacheFile = new File(getCacheDirectory(), imageKey);
    return cacheFile.exists();
  }

  public static Drawable loadImage(ImageRequest request) {
    final String imageKey = getKeyForUrl(request.imageUrl);
    LogWrapper.logMessage("Loading image: " + request.imageUrl);

    Drawable drawable = loadImageFromMemoryCache(imageKey);
    if(drawable != null) {
      LogWrapper.logMessage("Found image " + request.imageUrl + " in memory cache");
      return drawable;
    }

    drawable = loadImageFromFileCache(imageKey, request.imageUrl);
    if(drawable != null) {
      LogWrapper.logMessage("Found image " + request.imageUrl + " in file cache");
      return drawable;
    }

    drawable = ImageDownloader.loadImage(imageKey, request.imageUrl);
    if(drawable != null) {
      saveImageInFileCache(imageKey, drawable);
      if(request.cacheInMemory) {
        saveImageInMemoryCache(imageKey, drawable);
      }
      return drawable;
    }

    LogWrapper.logMessage("Could not load drawable, returning null");
    return drawable;
  }

  private static Drawable loadImageFromMemoryCache(final String imageKey) {
    if(drawableCache.containsKey(imageKey)) {
      // Apparently Android's SoftReference can sometimes free objects too early, see:
      // http://groups.google.com/group/android-developers/browse_thread/thread/ebabb0dadf38acc1
      // If that happens then it's no big deal, as this class will simply re-load the image
      // from file, but if that is the case then we should be polite and remove the imageKey
      // from the cache to reflect the actual caching state of this image.
      final Drawable drawable = drawableCache.get(imageKey).get();
      if(drawable == null) {
        drawableCache.remove(imageKey);
      }
      return drawable;
    }
    else {
      return null;
    }
  }

  private static Drawable loadImageFromFileCache(final String imageKey, final URL imageUrl) {
    Drawable drawable = null;

    File cacheFile = new File(getCacheDirectory(), imageKey);
    if(cacheFile.exists()) {
      try {
        Date now = new Date();
        long fileAgeInMs = now.getTime() - cacheFile.lastModified();
        if(fileAgeInMs > CACHE_RECHECK_AGE_IN_MS) {
          Date expirationDate = ImageDownloader.getServerTimestamp(imageUrl);
          if(expirationDate.after(now)) {
            drawable = Drawable.createFromStream(new FileInputStream(cacheFile), imageKey);
            LogWrapper.logMessage("Cached version of " + imageUrl.toString() + " is still current, updating timestamp");
            if(!cacheFile.setLastModified(now.getTime())) {
              // Ugh, it seems that in some cases this call will always return false and refuse to update the timestamp
              // For more info, see: http://code.google.com/p/android/issues/detail?id=18624
              // In these cases, we manually re-write the file to disk. Yes, that sucks, but it's better than loosing
              // the ability to do any intelligent file caching at all.
              saveImageInFileCache(imageKey, drawable);
            }
          }
          else {
            LogWrapper.logMessage("Cached version of " + imageUrl.toString() + " found, but has expired.");
          }
        }
        else {
          drawable = Drawable.createFromStream(new FileInputStream(cacheFile), imageKey);
          if(drawable == null) {
            throw new Exception("Could not create drawable from image: " + imageUrl.toString());
          }
        }
      }
      catch(Exception e) {
        LogWrapper.logException(e);
      }
    }

    return drawable;
  }

  private static void saveImageInMemoryCache(String imageKey, final Drawable drawable) {
    drawableCache.put(imageKey, new SoftReference<Drawable>(drawable));
  }

  private static void saveImageInFileCache(String imageKey, final Drawable drawable) {
    OutputStream outputStream = null;

    try {
      File cacheFile = new File(getCacheDirectory(), imageKey);
      BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
      outputStream = new FileOutputStream(cacheFile);
      bitmapDrawable.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputStream);
      LogWrapper.logMessage("Saved image " + imageKey + " to file cache");
      outputStream.flush();
      outputStream.close();
    }
    catch(IOException e) {
      LogWrapper.logException(e);
    }
    finally {
      try {
        if(outputStream != null) {
          outputStream.close();
        }
      }
      catch(IOException e) {
        LogWrapper.logException(e);
      }
    }
  }


  private static File getCacheDirectory() {
    if(cacheDirectory == null) {
      //noinspection NullableProblems
      setCacheDirectory(null, DEFAULT_CACHE_DIRECTORY_NAME);
    }
    return cacheDirectory;
  }

  public static void setCacheDirectory(String packageName, String subdirectoryName) {
    File dataDirectory = new File(android.os.Environment.getExternalStorageDirectory(), "data");
    if(!dataDirectory.exists()) {
      dataDirectory.mkdir();
    }

    File packageDirectory;
    if(packageName != null) {
      packageDirectory = new File(dataDirectory, packageName);
      if(!packageDirectory.exists()) {
        packageDirectory.mkdir();
      }
    }
    else {
      packageDirectory = dataDirectory;
    }

    cacheDirectory = new File(packageDirectory, subdirectoryName);
    if(!cacheDirectory.exists()) {
      cacheDirectory.mkdir();
    }
  }

  /**
   * Clear expired images in the file cache to save disk space. This method will remove all
   * images older than {@link #CACHE_EXPIRATION_AGE_IN_SEC} seconds.
   */
  public static void clearOldCacheFiles() {
    clearOldCacheFiles(CACHE_EXPIRATION_AGE_IN_SEC);
  }

  /**
   * Clear all images older than a given amount of seconds.
   * @param cacheAgeInSec Image expiration limit, in seconds
   */
  public static void clearOldCacheFiles(long cacheAgeInSec) {
    final long cacheAgeInMs = cacheAgeInSec * 1000;
    Date now = new Date();
    String[] cacheFiles = getCacheDirectory().list();
    if(cacheFiles != null) {
      for(String child : cacheFiles) {
        File childFile = new File(getCacheDirectory(), child);
        if(childFile.isFile()) {
          long fileAgeInMs = now.getTime() - childFile.lastModified();
          if(fileAgeInMs > cacheAgeInMs) {
            LogWrapper.logMessage("Deleting image '" + child + "' from cache");
            childFile.delete();
          }
        }
      }
    }
  }

  /**
   * Remove all images from the fast in-memory cache. This should be called to free up memory
   * when receiving onLowMemory warnings or when the activity knows it has no use for the items
   * in the memory cache anymore.
   */
  public static void clearMemoryCaches() {
    LogWrapper.logMessage("Emptying in-memory drawable cache");
    for(String key : drawableCache.keySet()) {
      SoftReference reference = drawableCache.get(key);
      reference.clear();
    }
    drawableCache.clear();
    System.gc();
  }


  private static final char[] HEX_CHARACTERS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  /**
   * Calculate a hash key for the given URL, which is used to create safe filenames and
   * key strings. Internally, this method uses MD5, as that is available on Android 2.1
   * devices (unlike base64, for example).
   * @param url Image URL
   * @return Hash for image URL
   */
  public static String getKeyForUrl(URL url) {
    String result = "";

    try {
      String urlString = url.toString();
      MessageDigest digest = MessageDigest.getInstance("MD5");
      digest.update(urlString.getBytes(), 0, urlString.length());
      byte[] resultBytes = digest.digest();
      StringBuilder hexStringBuilder = new StringBuilder(2 * resultBytes.length);
      for(final byte b : resultBytes) {
        hexStringBuilder.append(HEX_CHARACTERS[(b & 0xf0) >> 4]).append(HEX_CHARACTERS[b & 0x0f]);
      }
      result = hexStringBuilder.toString();
    }
    catch(NoSuchAlgorithmException e) {
      LogWrapper.logException(e);
    }

    return result;
  }
}
