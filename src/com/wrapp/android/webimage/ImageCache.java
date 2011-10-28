package com.wrapp.android.webimage;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ImageCache {
  private static final long CACHE_RECHECK_AGE_IN_SEC = 24 * 60 * 60; // One day
  private static final long CACHE_EXPIRATION_AGE_IN_SEC = CACHE_RECHECK_AGE_IN_SEC * 30; // One month
  private static final String DEFAULT_CACHE_DIRECTORY_NAME = "images";

  private static File cacheDirectory;
  private static Map<String, Drawable> drawableCache = new HashMap<String, Drawable>();

  public static Drawable loadImage(ImageRequest request) {
    final String imageKey = getKeyForUrl(request.imageUrl);
    LogWrapper.logMessage("Loading image: " + request.imageUrl);

    Drawable drawable = loadImageFromMemoryCache(imageKey);
    if(drawable != null) {
      LogWrapper.logMessage("Found image " + request.imageUrl + " in first-try memory cache, " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());
      return drawable;
    }

    drawable = loadImageFromFileCache(imageKey, request.imageUrl);
    if(drawable != null) {
      LogWrapper.logMessage("Found image " + request.imageUrl + " in file cache, " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());
      return drawable;
    }

    drawable = ImageDownloader.loadImage(imageKey, request.imageUrl);
    if(drawable != null) {
      LogWrapper.logMessage("Downloaded image " + request.imageUrl + " from network");
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
      return drawableCache.get(imageKey);
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
        if(fileAgeInMs > CACHE_RECHECK_AGE_IN_SEC * 1000) {
          Date expirationDate = ImageDownloader.getServerTimestamp(imageUrl);
          if(expirationDate.after(now)) {
            LogWrapper.logMessage("Cached version of '" + imageUrl.toString() + "' is still current, updating timestamp");
            cacheFile.setLastModified(now.getTime());
            drawable = Drawable.createFromStream(new FileInputStream(cacheFile), imageKey);
          }
          else {
            LogWrapper.logMessage("Cached version of '" + imageUrl.toString() + "' found, but is expired.");
          }
        }
        else {
          drawable = Drawable.createFromStream(new FileInputStream(cacheFile), imageKey);
          if(drawable == null) {
            throw new Exception("Could not create drawable from image '" + imageUrl.toString() + "'");
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
    drawableCache.put(imageKey, drawable);
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
      assert outputStream != null;
      try {
        outputStream.close();
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
   *
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

  public static void clearMemoryCaches() {
    LogWrapper.logMessage("Emptying in-memory drawable cache");
    for(String key : drawableCache.keySet()) {
      Drawable drawable = drawableCache.get(key);
      drawable = null;
    }
    drawableCache.clear();
    System.gc();
  }


  private static final char[] HEX_CHARACTERS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

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
      // Erm, not much to do here LogWrapper.logException(e);
    }

    return result;
  }
}
