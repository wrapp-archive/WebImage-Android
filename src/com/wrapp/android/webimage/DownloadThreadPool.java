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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

public class DownloadThreadPool {
  // These don't seem to be declared in the Android SDK. Or did I just not look hard enough?
  private static final int CONNECTION_TYPE_MOBILE = 0;
  private static final int CONNECTION_TYPE_WIFI = 1;
  private static final int CONNECTION_TYPE_ETHERNET = 9;
  private static final int DEFAULT_MAX_THREADS = 4;

  static DownloadThreadPool staticInstance;
  private static int numThreads = DEFAULT_MAX_THREADS;
  private DownloadThread[] downloadThreads;
  private int numActiveThreads = 0;
  private int currentThread = 0;

  public static class ConnectivityChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      DownloadThreadPool.resizeThreadPool(context);
    }

    public static IntentFilter getIntentFilter() {
      return new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    }
  }

  public static DownloadThreadPool getInstance() {
    if(staticInstance == null) {
      staticInstance = new DownloadThreadPool();
    }
    return staticInstance;
  }

  public static void setMaxThreads(int value) {
    numThreads = value;
  }

  private DownloadThreadPool() {
    downloadThreads = new DownloadThread[numThreads];
    for(int i = 0; i < numThreads; i++) {
      downloadThreads[i] = new DownloadThread();
    }
  }

  public void addTask(ImageRequest request) {
    DownloadThreadPool downloadThreadPool = getInstance();
    // If there is only one thread in the worker pool, issue the request right away
    if(downloadThreadPool.numActiveThreads == 1) {
      downloadThreads[0].addTask(request);
    }
    else {
      // Rotate to the next thread
      downloadThreadPool.currentThread++;
      if(downloadThreadPool.currentThread >= downloadThreadPool.numActiveThreads) {
        downloadThreadPool.currentThread = 0;
      }
      downloadThreads[downloadThreadPool.currentThread].addTask(request);
    }
  }

  public void start(Context context) {
    for(int i = 0; i < numThreads; i++) {
      downloadThreads[i].start();
    }
    numActiveThreads = getBestThreadPoolSize(context);
  }

  public static void resizeThreadPool(Context context) {
    final DownloadThreadPool downloadThreadPool = getInstance();
    // Check if the thread pool has not been initialized
    if(downloadThreadPool.numActiveThreads == 0) {
      return;
    }

    downloadThreadPool.currentThread = 0;
    downloadThreadPool.numActiveThreads = downloadThreadPool.getBestThreadPoolSize(context);
  }

  private int getBestThreadPoolSize(final Context context) {
    // Android 2.1 devices are never going to be that fast even in the very best case, so only use
    // a single downloader thread for them
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
      return 1;
    }

    try {
      // Try to see network information to determine the ideal thread pool size.
      ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
      if(networkInfo != null) {
        final int type = networkInfo.getType();
        switch(type) {
          case CONNECTION_TYPE_MOBILE:
            // Connection subtype will return integer respective for 1G, 2G, 3G
            // For 3G connections and better, we should use up to half the max pool size.
            if(networkInfo.getSubtype() >= 3) {
              return numThreads / 2;
            }
            // For all other cases, just use one thread. EDGE/2G is slow pretty much everywhere.
            else {
              return 1;
            }
          // For WIFI, use the entire available thread pool
          case CONNECTION_TYPE_WIFI:
            return numThreads;
          // Yeah, this looks weird, but there are Android devices which support this (like Android-x86).
          case CONNECTION_TYPE_ETHERNET:
            return numThreads;
        }
      }
    }
    catch(SecurityException e) {
      LogWrapper.logMessage("Could not determine network type, need permission for ACCESS_NETWORK_STATE in app's manifest");
    }
    catch(Exception e) {
      LogWrapper.logMessage("Could not determine network connection type");
    }

    // If we couldn't figure out a good pool size, then just use one thread to be safe.
    return 1;
  }

  public void cancelAllRequests() {
    for(int i = 0; i < numThreads; i++) {
      downloadThreads[i].cancelAllRequests();
    }
  }

  public void shutdown() {
    for(int i = 0; i < numThreads; i++) {
      downloadThreads[i].shutdown();
    }
  }
}
