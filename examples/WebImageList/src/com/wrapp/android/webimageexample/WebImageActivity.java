package com.wrapp.android.webimageexample;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import com.wrapp.android.webimage.WebImage;

public class WebImageActivity extends ListActivity {
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
}
