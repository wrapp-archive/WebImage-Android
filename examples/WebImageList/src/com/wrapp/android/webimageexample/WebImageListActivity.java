package com.wrapp.android.webimageexample;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.wrapp.android.webimage.WebImage;

public class WebImageListActivity extends ListActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.web_image_activity);

    WebImage.clearOldCacheFiles();
    WebImage.enableLogging("WebImageExample", Log.DEBUG);

    WebImageListAdapter listAdapter = new WebImageListAdapter();
    setListAdapter(listAdapter);
    listAdapter.notifyDataSetChanged();
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
}
