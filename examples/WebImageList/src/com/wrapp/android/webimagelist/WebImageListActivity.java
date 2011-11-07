package com.wrapp.android.webimagelist;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import com.wrapp.android.webimage.WebImage;
import com.wrapp.android.webimage.WebImageView;

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

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.web_image_activity);

    // Remove all images from the cache when starting up. Real apps probably should call this
    // method with a non-zero argument (in seconds), or without any argument to use the default
    // value.
    WebImage.clearOldCacheFiles(0);

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

  /**
   * If your activity plans on loading a lot of images, you should call WebImage.cancelAllRequests()
   * before going into the background. Otherwise, you risk wasting CPU time and bandwidth.
   */
  @Override
  protected void onPause() {
    super.onPause();
    WebImage.cancelAllRequests();
  }

  /**
   * When the low memory warning is received, tell the WebImage class to free all memory caches. I'm
   * a bit suspicious of Android's memory management techniques, since I never see this method get
   * called even right before out of memory exceptions are thrown. Anyways, it's good practice to call
   * this method periodically to free up space to keep your app running fast.
   */
  @Override
  public void onLowMemory() {
    super.onLowMemory();
    WebImage.clearMemoryCaches();
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
        WebImage.cancelAllRequests();
        WebImage.clearMemoryCaches();
        WebImage.clearOldCacheFiles(0);
        refresh();
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
}
