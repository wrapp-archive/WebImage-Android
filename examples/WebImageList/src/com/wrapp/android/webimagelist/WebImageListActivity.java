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

public class WebImageListActivity extends ListActivity implements WebImageListAdapter.Listener {
  private static final long SHOW_PROGRESS_DELAY_IN_MS = 100;
  private Handler uiHandler;
  private Integer numTasks = 0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.web_image_activity);

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

  @Override
  protected void onPause() {
    super.onPause();
    WebImage.cancelAllRequests();
  }

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
