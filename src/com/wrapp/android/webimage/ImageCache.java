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
import android.graphics.Bitmap;

import java.io.File;
import java.lang.ref.WeakReference;
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
  private static final String DEFAULT_CACHE_SUBDIRECTORY_NAME = "images";

  private static File cacheDirectory;
  private static Map<String, WeakReference<Bitmap>> memoryCache = new HashMap<String, WeakReference<Bitmap>>();
  private static long cacheRecheckAgeInMs = CACHE_RECHECK_AGE_IN_MS;

  public static boolean isImageCached(Context context, String imageKey) {
    final File cacheFile = new File(getCacheDirectory(context), imageKey);
    return cacheFile.exists();
  }

  public static long getCacheRecheckAgeInMs() {
    return cacheRecheckAgeInMs;
  }

  public static void setCacheRecheckAgeInMs(long cacheRecheckAgeInMs) {
    ImageCache.cacheRecheckAgeInMs = cacheRecheckAgeInMs;
  }

  public static Bitmap loadImageFromMemoryCache(final String imageKey) {
    synchronized(memoryCache) {
      if(memoryCache.containsKey(imageKey)) {
        // Apparently Android's SoftReference can sometimes free objects too early, see:
        // http://groups.google.com/group/android-developers/browse_thread/thread/ebabb0dadf38acc1
        // If that happens then it's no big deal, as this class will simply re-load the image
        // from file, but if that is the case then we should be polite and remove the imageKey
        // from the cache to reflect the actual caching state of this image.
        final Bitmap bitmap = memoryCache.get(imageKey).get();
        if(bitmap == null) {
          memoryCache.remove(imageKey);
        }
        return bitmap;
      }
      else {
        return null;
      }
    }
  }

  private static void saveImageInMemoryCache(String imageKey, final Bitmap bitmap) {
    synchronized(memoryCache) {
      memoryCache.put(imageKey, new WeakReference<Bitmap>(bitmap));
    }
  }

  public static File getCacheDirectory(final Context context) {
    if(cacheDirectory == null) {
      setCacheDirectory(context, DEFAULT_CACHE_SUBDIRECTORY_NAME);
    }
    return cacheDirectory;
  }

  public static void setCacheDirectory(Context context, String subdirectoryName) {
    // Final destination is Android/data/com.packagename/cache/subdirectory
    final File androidDirectory = new File(android.os.Environment.getExternalStorageDirectory(), "Android");
    if(!androidDirectory.exists()) {
      androidDirectory.mkdir();
    }

    final File dataDirectory = new File(androidDirectory, "data");
    if(!dataDirectory.exists()) {
      dataDirectory.mkdir();
    }

    final File packageDirectory = new File(dataDirectory, context.getPackageName());
    if(!packageDirectory.exists()) {
      packageDirectory.mkdir();
    }

    final File packageCacheDirectory = new File(packageDirectory, "cache");
    if(!packageCacheDirectory.exists()) {
      packageCacheDirectory.mkdir();
    }

    cacheDirectory = new File(packageCacheDirectory, subdirectoryName);
    if(!cacheDirectory.exists()) {
      cacheDirectory.mkdir();
    }

    LogWrapper.logMessage("Cache directory is " + cacheDirectory.toString());

    // WebImage versions prior to 1.2.2 stored images in /mnt/sdcard/data/packageName. If images are found
    // there, we should migrate them to the correct location. Unfortunately, WebImage 1.1.2 and below also
    // used the location /mnt/sdcard/data/images if no packageName was supplied. Since this isn't very
    // specific, we don't bother to remove those images, as they may belong to other applications.
    final File oldDataDirectory = new File(android.os.Environment.getExternalStorageDirectory(), "data");
    final File oldPackageDirectory = new File(oldDataDirectory, context.getPackageName());
    final File oldCacheDirectory = new File(oldPackageDirectory, subdirectoryName);
    if(oldCacheDirectory.exists()) {
      if(cacheDirectory.delete()) {
        if(!oldCacheDirectory.renameTo(cacheDirectory)) {
          LogWrapper.logMessage("Could not migrate old cache directory from " + oldCacheDirectory.toString());
          cacheDirectory.mkdir();
        }
        else {
          LogWrapper.logMessage("Finished migrating <1.2.2 cache directory");
        }
      }
    }
    else {
      // WebImage versions prior to 1.6.0 stored the subdirectory directly under the package name, avoiding
      // the intermediate cache directory. Migrate these images if this is the case.
      if(!subdirectoryName.equals("cache")) {
        final File oldSubdirectory = new File(packageDirectory, subdirectoryName);
        if(oldSubdirectory.exists()) {
          if(cacheDirectory.delete()) {
            if(!oldSubdirectory.renameTo(cacheDirectory)) {
              LogWrapper.logMessage("Could not migrate old cache directory from " + oldSubdirectory.toString());
              cacheDirectory.mkdir();
            }
            else {
              LogWrapper.logMessage("Finished migrating <1.6.0 cache directory");
            }
          }
        }
      }
    }
  }

  public static void clearImageFromCaches(final Context context, final URL imageUrl) {
    String imageKey = getCacheKeyForUrl(imageUrl);
    synchronized(memoryCache) {
      if(memoryCache.containsKey(imageKey)) {
        memoryCache.remove(imageKey);
      }
    }

    final File cacheFile = new File(getCacheDirectory(context), imageKey);
    if(cacheFile.exists()) {
      if(!cacheFile.delete()) {
        LogWrapper.logMessage("Could not remove cached version of image " + imageUrl);
      }
    }
  }

  /**
   * Clear expired images in the file cache to save disk space. This method will remove all
   * images older than {@link #CACHE_EXPIRATION_AGE_IN_SEC} seconds.
   * @param context Context used for getting app's package name
   */
  public static void clearOldCacheFiles(final Context context) {
    clearOldCacheFiles(context, CACHE_EXPIRATION_AGE_IN_SEC);
  }

  /**
   * Clear all images older than a given amount of seconds.
   * @param context Context used for getting app's package name
   * @param cacheAgeInSec Image expiration limit, in seconds
   */
  public static void clearOldCacheFiles(final Context context, long cacheAgeInSec) {
    final long cacheAgeInMs = cacheAgeInSec * 1000;
    Date now = new Date();
    String[] cacheFiles = getCacheDirectory(context).list();
    if(cacheFiles != null) {
      for(String child : cacheFiles) {
        File childFile = new File(getCacheDirectory(context), child);
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
    if(memoryCache != null) {
      synchronized(memoryCache) {
        LogWrapper.logMessage("Emptying in-memory cache");
        for(String key : memoryCache.keySet()) {
          WeakReference reference = memoryCache.get(key);
          if(reference != null) {
            reference.clear();
          }
        }
        memoryCache.clear();
      }
    }
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
  public static String getCacheKeyForUrl(URL url) {
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
