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

package com.wrapp.android.webimagelist;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;
import com.wrapp.android.webimage.*;

public class WebImageListActivity extends ListActivity implements WebImageView.Listener {
  // Don't show the progress spinner right away, because when scrolling rapidly through the list
  // of images, there will get a bunch of callbacks which may cause the progress spinner to flicker
  // as it is rapidly shown and hidden. Imposing a small delay here will show the spinner only when
  // images take more than a few milliseconds to load, ie, from the network and not from disk/memory.
  private static final long SHOW_PROGRESS_DELAY_IN_MS = 100;

  // Handler used to post to the GUI thread. This is important, because WebImageView.Listener
  // callbacks are posted to the background thread, so if we want to update the GUI it must
  // be with a handler. If no handler is used, there will be a bunch of "Only the original thread
  // that created a view hierarchy can touch its views" exceptions.
  private Handler uiHandler;

  // Runnable which is called in case of error, image cancelled, or image loaded
  private Runnable stopTaskRunnable;

  // Counter to keep track of number of running image tasks. Note that this must be an Integer
  // rather than an int so that it can be synchronized and thus more threadsafe.
  private Integer numTasks = 0;

  // Used when toggling cache recheck time from menu
  private long defaultCacheRecheckAgeInMs = ImageCache.getCacheRecheckAgeInMs();
  private BroadcastReceiver connectivityChangeReceiver;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.web_image_activity);

    // Remove all images from the cache when starting up
    WebImage.clearOldCacheFiles(this);
    // Calling the above method with an extra argument of 0 will force all images to be removed from the cache
    // WebImage.clearOldCacheFiles(this, 0);

    // Turn on logging so we can see what is going on.
    WebImage.enableLogging("WebImageList", Log.DEBUG);

    // Create handler, runnable used when stopping tasks.
    uiHandler = new Handler();
    stopTaskRunnable = new Runnable() {
      public void run() {
        onTaskStopped();
      }
    };

    // Create a list adapter and attach it to the ListView
    WebImageListAdapter listAdapter = new WebImageListAdapter(this);
    setListAdapter(listAdapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    connectivityChangeReceiver = new DownloadThreadPool.ConnectivityChangeReceiver();
    registerReceiver(connectivityChangeReceiver, DownloadThreadPool.ConnectivityChangeReceiver.getIntentFilter());
  }

  /**
   * If your activity plans on loading a lot of images, you should call WebImage.cancelAllRequests()
   * before going into the background. Otherwise, you risk wasting CPU time and bandwidth.
   */
  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(connectivityChangeReceiver);
    WebImage.cancelAllRequests(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    LogWrapper.logMessage("Stopping activity");
    WebImage.shutdown(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater menuInflater = new MenuInflater(this);
    menuInflater.inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case R.id.MainMenuRefreshItem:
        refresh();
        break;
      case R.id.MainMenuClearCachesItem:
        WebImage.cancelAllRequests(this);
        WebImage.clearOldCacheFiles(this, 0);
        Toast toast = Toast.makeText(this, "Caches cleared", Toast.LENGTH_SHORT);
        toast.show();
        refresh();
        break;
      case R.id.MainMenuForceTimestampChecks:
        toggleForceTimestampChecks();
        break;
      case R.id.MainMenuShowMemoryUse:
        showMemoryUsageToast();
        break;
      case R.id.MainMenuRestrictMemoryUse:
        toggleRestrictMemoryUsage();
        break;
      default:
        refresh();
        break;
    }

    return true;
  }

  private void refresh() {
    final WebImageListAdapter listAdapter = (WebImageListAdapter)getListAdapter();
    listAdapter.notifyDataSetChanged();
  }

  public void onImageLoadStarted() {
    uiHandler.postDelayed(new Runnable() {
      public void run() {
        onTaskStarted();
      }
    }, SHOW_PROGRESS_DELAY_IN_MS);
  }

  // Start and stop the progress spinner in the activity's top bar

  public void onImageLoadComplete() {
    uiHandler.post(stopTaskRunnable);
  }

  public void onImageLoadError() {
    uiHandler.post(stopTaskRunnable);
  }

  public void onImageLoadCancelled() {
    uiHandler.post(stopTaskRunnable);
  }

  private void onTaskStarted() {
    synchronized(numTasks) {
      if(numTasks == 0) {
        setProgressBarIndeterminateVisibility(true);
      }
      numTasks++;
    }
  }

  private void onTaskStopped() {
    synchronized(numTasks) {
      numTasks--;
      if(numTasks == 0) {
        setProgressBarIndeterminateVisibility(false);
      }
    }
  }

  private void toggleRestrictMemoryUsage() {
    final WebImageListAdapter webImageListAdapter = (WebImageListAdapter)getListAdapter();
    final boolean shouldRestrictMemoryUsage = !webImageListAdapter.getShouldRestrictMemoryUsage();
    webImageListAdapter.setShouldRestrictMemoryUsage(shouldRestrictMemoryUsage);
    WebImage.cancelAllRequests(this);
    WebImage.clearOldCacheFiles(this, 0);
    final String toastMessage = "Restrict memory usage: " + (shouldRestrictMemoryUsage ? "enabled" : "disabled");
    Toast toast = Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT);
    toast.show();
    refresh();
  }

  private void toggleForceTimestampChecks() {
    String toastMessage = "Force timestamp checks: ";
    if(ImageCache.getCacheRecheckAgeInMs() == 1) {
      ImageCache.setCacheRecheckAgeInMs(defaultCacheRecheckAgeInMs);
      toastMessage += "disabled";
    }
    else {
      ImageCache.setCacheRecheckAgeInMs(1);
      toastMessage += "enabled";
    }
    Toast toast = Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT);
    toast.show();
    refresh();
  }

  public void showMemoryUsageToast() {
    final int heapKbAllocated = (int)(Debug.getNativeHeapAllocatedSize() / 1024);
    final int heapKbTotal = (int)(Debug.getNativeHeapSize() / 1024);
    final int heapPercent = (int)(100.0f * heapKbAllocated / heapKbTotal);
    ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
    final int memoryClass = activityManager.getMemoryClass();
    final String toastMessage = "Heap: " + heapKbAllocated + "K used (" + heapPercent + "%)\nMemory class: " + memoryClass;
    Toast toast = Toast.makeText(this, toastMessage, Toast.LENGTH_LONG);
    toast.show();
  }
}
