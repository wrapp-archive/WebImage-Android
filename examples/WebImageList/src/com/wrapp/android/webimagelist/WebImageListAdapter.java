package com.wrapp.android.webimagelist;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.wrapp.android.webimage.WebImageView;

public class WebImageListAdapter extends BaseAdapter {
  private static final boolean USE_AWESOME_IMAGES = true;
  private static final int NUM_IMAGES = 100;
  private static final int IMAGE_SIZE = 100;
  private WebImageView.Listener listener;

  public WebImageListAdapter(WebImageView.Listener listener) {
    this.listener = listener;
  }

  public int getCount() {
    return NUM_IMAGES;
  }

  private String getImageUrl(int i) {
    if(USE_AWESOME_IMAGES) {
      // Unicorns!
      return "http://unicornify.appspot.com/avatar/" + i + "?s=" + IMAGE_SIZE;
    }
    else {
      // Boring identicons
      return "http://www.gravatar.com/avatar/" + i + "?s=" + IMAGE_SIZE + "&d=identicon";
    }
  }

  public Object getItem(int i) {
    return getImageUrl(i);
  }

  public long getItemId(int i) {
    return i;
  }

  public View getView(int i, View convertView, ViewGroup parentViewGroup) {
    WebImageContainerView containerView;
    if(convertView != null) {
      containerView = (WebImageContainerView)convertView;
    }
    else {
      containerView = new WebImageContainerView(parentViewGroup.getContext());
    }

    containerView.setImageUrl(getImageUrl(i), listener);
    containerView.setImageText("Image #" + i);
    return containerView;
  }
}
