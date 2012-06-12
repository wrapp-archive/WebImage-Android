package com.wrapp.android.webimagelist;

import android.content.Context;
import android.graphics.BitmapFactory.Options;
import android.util.AttributeSet;

import com.wrapp.android.webimage.ImageRequest;
import com.wrapp.android.webimage.ScaledBitmapLoader;
import com.wrapp.android.webimage.WebImageView;

public class ScaleWebImageView extends WebImageView {
  public ScaleWebImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public ScaleWebImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ScaleWebImageView(Context context) {
    super(context);
  }

  @Override
  public void setImageUrl(final String imageUrl, Options options, final int errorImageResId, final int placeholderImageResId) {
    if (imageUrl == null) {
      return;
    }
    
    int width = getWidth();
    int height = getHeight();
    
    if (width == 0 && height == 0) {
      // We have probably not been layout yet, post
      post(new Runnable() {
        @Override
        public void run() {
          int width = getWidth();
          int height = getHeight();
          
          setImageUrl(new ImageRequest(imageUrl, new ScaledBitmapLoader(width, height)), errorImageResId, placeholderImageResId);
        }
      });
    } else {
      setImageUrl(new ImageRequest(imageUrl, new ScaledBitmapLoader(width, height)), errorImageResId, placeholderImageResId);
    }
  }
}
