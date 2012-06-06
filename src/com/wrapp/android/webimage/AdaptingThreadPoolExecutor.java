package com.wrapp.android.webimage;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

class AdaptingThreadPoolExecutor extends ThreadPoolExecutor {
  private static final int DEFAULT_MAX_THREADS = 4;
  
  private Context context;
  private ConnectivityChangeReceiver connectivityReceiver;
  
  private static int maxThreads = DEFAULT_MAX_THREADS;
  
  static void setMaxThreads(int value) {
    // This is pretty hackish but has to be done to fit the
    // current interface of WebImage.
    maxThreads = value;
  }

  public AdaptingThreadPoolExecutor(Context context) {
    super(DEFAULT_MAX_THREADS, DEFAULT_MAX_THREADS, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory(Thread.MIN_PRIORITY));
    
    this.context = context;
    
    connectivityReceiver = new ConnectivityChangeReceiver();
    context.registerReceiver(connectivityReceiver,  new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    
    resizeThreadPool();
  }

  @Override
  protected void terminated() {
    super.terminated();
    
    context.unregisterReceiver(connectivityReceiver);
  }

  public void resizeThreadPool() {
    int bestSize = getBestThreadPoolSize();
    
    LogWrapper.logMessage("Using " + bestSize + " threads for download");
    
    setCorePoolSize(bestSize);
    setMaximumPoolSize(bestSize);
  }
  
  private int getBestThreadPoolSize() {
    // Android 2.1 devices are never going to be that fast even in the very best case, so only use
    // a single downloader thread for them
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
      return 1;
    }

    try {
      // Try to see network information to determine the ideal thread pool size.
      ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
      if(networkInfo != null) {
        final int type = networkInfo.getType();
        switch(type) {
          case ConnectivityManager.TYPE_MOBILE:
            // Connection subtype will return integer respective for 1G, 2G, 3G
            // For 3G connections and better, we should use up to half the max pool size.
            if(networkInfo.getSubtype() >= 3) {
              return maxThreads / 2;
            }
            // For all other cases, just use one thread. EDGE/2G is slow pretty much everywhere.
            else {
              return 1;
            }
          // For WIFI, use the entire available thread pool
          case ConnectivityManager.TYPE_WIFI:
            return maxThreads;
          // Yeah, this looks weird, but there are Android devices which support this (like Android-x86).
          case ConnectivityManager.TYPE_ETHERNET:
            return maxThreads;
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
  
  private class ConnectivityChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      resizeThreadPool();
    }
  }
}
