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

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.wrapp.android.webimage.WebImageView;

public class WebImageListAdapter extends BaseAdapter {
  private static final boolean USE_NUMBER_IMAGES = true;
  private static final boolean USE_AWESOME_IMAGES = true;
  private static final int NUM_IMAGES = 100;
  private static final int IMAGE_SIZE = 100;

  private WebImageView.Listener listener;
  private boolean shouldCacheImagesInMemory = true;

  public WebImageListAdapter(WebImageView.Listener listener) {
    this.listener = listener;
  }

  public int getCount() {
    return NUM_IMAGES;
  }

  private String getImageUrl(int i) {
    if(USE_NUMBER_IMAGES) {
      // Numbers with random backgrounds. More useful for testing correct listview behavior
      return "http://static.nikreiman.com/numbers/" + i + ".png";
    }
    else {
      if(USE_AWESOME_IMAGES) {
        // Unicorns!
        return "http://unicornify.appspot.com/avatar/" + i + "?s=" + IMAGE_SIZE;
      }
      else {
        // Boring identicons
        return "http://www.gravatar.com/avatar/" + i + "?s=" + IMAGE_SIZE + "&d=identicon";
      }
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

    containerView.setImageUrl(getImageUrl(i), listener, shouldCacheImagesInMemory);
    containerView.setImageText("Image #" + i);
    return containerView;
  }

  public boolean getShouldCacheImagesInMemory() {
    return shouldCacheImagesInMemory;
  }

  public void setShouldCacheImagesInMemory(boolean shouldCacheImagesInMemory) {
    this.shouldCacheImagesInMemory = shouldCacheImagesInMemory;
  }
}
