package com.wrapp.android.webimage;

import java.net.URL;

public class WebImage {
  public static void load(URL imageUrl, ImageRequest.Listener listener, boolean cacheInMemory) {
    ImageLoader.load(imageUrl, listener, cacheInMemory);
  }

  public static void cancelAllRequests() {
    ImageLoader.cancelAllRequests();
  }

  public static void clearMemoryCaches() {
    ImageCache.clearMemoryCaches();
  }

  public static void clearOldCacheFiles() {
    ImageCache.clearOldCacheFiles();
  }

  public static void clearOldCacheFiles(long cacheAgeInSec) {
    ImageCache.clearOldCacheFiles(cacheAgeInSec);
  }

  public static void enableLogging(String tag, int level) {
    LogWrapper.enableLogging(tag, level);
  }
}
