package com.wrapp.android.webimage;

import android.graphics.drawable.Drawable;

import java.net.URL;

public final class ImageRequest {
  public URL imageUrl;
  public Listener listener;
  public boolean cacheInMemory;

  public interface Listener {
    public void onDrawableLoaded(final Drawable drawable);
  }

  public ImageRequest(URL imageUrl, Listener listener, boolean cacheInMemory) {
    this.imageUrl = imageUrl;
    this.listener = listener;
    this.cacheInMemory = cacheInMemory;
  }
}
