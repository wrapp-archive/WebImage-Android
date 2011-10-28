package com.wrapp.android.webimage;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.net.URL;

@SuppressWarnings({"UnusedDeclaration"})
public class WebImageView extends ImageView implements ImageRequest.Listener {
  Handler uiHandler;

  public WebImageView(Context context) {
    super(context);
    initialize();
  }

  public WebImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public WebImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initialize();
  }

  private void initialize() {
    uiHandler = new Handler();
  }

  public void setImageUrl(URL imageUrl) {
    ImageLoader.load(imageUrl, this, false);
  }

  public void setImageUrl(URL imageUrl, boolean cacheInMemory) {
    ImageLoader.load(imageUrl, this, cacheInMemory);
  }

  public void onDrawableLoaded(final Drawable drawable) {
    uiHandler.post(new Runnable() {
      public void run() {
        setImageDrawable(drawable);
      }
    });
  }
}
