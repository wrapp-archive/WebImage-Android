package com.wrapp.android.webimageexample;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class WebImageListAdapter extends BaseAdapter {
  private static final int NUM_IMAGES = 50;

  public int getCount() {
    return NUM_IMAGES;
  }

  private String getImageUrl(int i) {
    // Unicorns!
    return "http://unicornify.appspot.com/avatar/" + i + "?s=100";
  }

  public Object getItem(int i) {
    return getImageUrl(i);
  }

  public long getItemId(int i) {
    return i;
  }

  public View getView(int i, View convertView, ViewGroup parentViewGroup) {
    LinearLayout webImageContainerView;
    if(convertView != null) {
      webImageContainerView = (LinearLayout)convertView;
    }
    else {
      final Context context = parentViewGroup.getContext();
      LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      webImageContainerView = (LinearLayout)layoutInflater.inflate(R.layout.web_image_container_view, null);
    }

    WebImageView webImageView = (WebImageView)webImageContainerView.findViewById(R.id.WebImageView);
    webImageView.setImageUrl(getImageUrl(i));
    return webImageContainerView;
  }
}
