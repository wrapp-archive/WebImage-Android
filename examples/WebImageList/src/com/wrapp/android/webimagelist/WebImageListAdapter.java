package com.wrapp.android.webimagelist;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class WebImageListAdapter extends BaseAdapter {
  private static final boolean USE_AWESOME_IMAGES = true;
  private static final int NUM_IMAGES = 100;
  private static final int IMAGE_SIZE = 100;
  private Integer numTasks = 0;
  private ProgressController progressController;

  public interface ProgressController {
    public void taskStarted();
    public void allTasksStopped();
  }

  public WebImageListAdapter(ProgressController progressController) {
    this.progressController = progressController;
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

    onTaskStarted();
    containerView.setImageUrl(getImageUrl(i), new ProgressWebImageView.Listener() {
      public void onImageLoadComplete() {
        onTaskStopped();
      }
    });
    containerView.setImageText("Image #" + i);
    return containerView;
  }

  private void onTaskStarted() {
    synchronized(numTasks) {
      if(numTasks == 0) {
        progressController.taskStarted();
      }
      numTasks++;
    }
  }

  private void onTaskStopped() {
    synchronized(numTasks) {
      numTasks--;
      if(numTasks == 0) {
        progressController.allTasksStopped();
      }
    }
  }
}
