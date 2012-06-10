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

package com.wrapp.android.webimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * ImageView successor class which can load images asynchronously from the web. This class
 * is safe to use in ListAdapters or views which may trigger many simultaneous requests.
 */
public class WebImageView extends ImageView implements ImageRequest.Listener {
  private Listener listener;
  // Save both a Drawable and int here. If the user wants to pass a resource ID, we can load
  // this lazily and save a bit of memory.
  private Drawable errorImage;
  private int errorImageResId;
  private Drawable placeholderImage;
  private int placeholderImageResId;
  
  private String pendingImageUrl;
  private String loadedImageUrl;

  public interface Listener {
    public void onImageLoadStarted();
    public void onImageLoadComplete();
    public void onImageLoadError();
    public void onImageLoadCancelled();
  }

  public WebImageView(Context context) {
    super(context);
  }

  public WebImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public WebImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  /**
   * Set a listener to be informed about events from this view. If this is not set, then no events are sent.
   * @param listener Listener
   */
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  /**
   * Load an image asynchronously from the web
   * @param imageUrl Image URL to download image from
   */
  public final void setImageUrl(String imageUrl) {
    setImageUrl(imageUrl, null, 0, 0);
  }
  
  /**
   * Load an image asynchronously from the web
   * @param imageUrl Image URL to download image from
   * @param options Options to use when loading the image. See the documentation for {@link BitmapFactory.Options}
   * for more details. Can be null.
   * @param errorImageResId Resource ID  to be displayed in case the image could not be loaded. If 0, no new image
   * will be displayed on error.
   * @param placeholderImageResId Resource ID to set for placeholder image while image is loading.
   */
  public void setImageUrl(String imageUrl, BitmapFactory.Options options, int errorImageResId, int placeholderImageResId) {
    setImageUrl(new ImageRequest(imageUrl, new StandardBitmapLoader(options)), errorImageResId, placeholderImageResId);
  }


  protected void setImageUrl(ImageRequest request, int errorImageResId, int placeholderImageResId) {
    if(request.imageUrl == null) {
      return;
    } else if (request.imageUrl.equals(loadedImageUrl)) {
      // This url is already loaded
      return;
    }

    this.errorImageResId = errorImageResId;
    if(this.placeholderImageResId > 0) {
      setImageResource(this.placeholderImageResId);
    }
    else if(this.placeholderImage != null) {
      setImageDrawable(this.placeholderImage);
    }
    else if(placeholderImageResId > 0) {
      setImageResource(placeholderImageResId);
    }
    if(this.listener != null) {
      listener.onImageLoadStarted();
    }
    
    loadedImageUrl = null;
    pendingImageUrl = request.imageUrl;
    
    WebImage.load(getContext(), request, this);
  }

  /**
   * This method is called when the drawable has been downloaded (or retreived from cache) and is
   * ready to be displayed. If you override this class, then you should not call this method via
   * super.onBitmapLoaded(). Instead, handle the drawable as necessary (ie, resizing or other
   * transformations), and then call postToGuiThread() to display the image from the correct thread.
   *
   * If you only need a callback to be notified about the drawable being loaded to update other
   * GUI elements and whatnot, then you should override onImageLoaded() instead.
   *
   * @param response Request response
   */
  @Override
  public void onBitmapLoaded(ImageRequest request, Bitmap bitmap) {
    if(request.imageUrl.equals(pendingImageUrl)) {
      loadedImageUrl = pendingImageUrl;
      pendingImageUrl = null;
      
      setImageBitmap(bitmap);
      if (listener != null) {
        listener.onImageLoadComplete();
      }
    } else {
      LogWrapper.logMessage("WebImageView dropping image: " + request.imageUrl + ", waiting for: " + pendingImageUrl);
      
      if(listener != null) {
        listener.onImageLoadCancelled();
      }
    }
  }

  /**
   * This method is called if the drawable could not be loaded for any reason. If you need a callback
   * to react to these events, you should override onImageError() instead.
   * @param message Error message (non-localized)
   */
  @Override
  public void onBitmapLoadError(String message) {
    LogWrapper.logMessage(message);
    // In case of error, lazily load the drawable here
    if (errorImageResId > 0) {
      errorImage = getResources().getDrawable(errorImageResId);
    }
    if (errorImage != null) {
      setImageDrawable(errorImage);
    }
    if(listener != null) {
      listener.onImageLoadError();
    }
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    
    if(visibility == VISIBLE) {
      setImageUrl(pendingImageUrl);
    }
  }

  /**
   * Called when the URL which the caller asked to load was cancelled. This can happen for a number
   * of reasons, including the activity being closed or scrolling rapidly in a ListView. For this
   * reason it is recommended not to do so much work in this method.
   */
  @Override
  public void onBitmapLoadCancelled() {
    if(listener != null) {
      listener.onImageLoadCancelled();
    }
  }

  public void setErrorImageResId(int errorImageResId) {
    this.errorImageResId = errorImageResId;
  }

  public void setErrorImage(Drawable errorImage) {
    this.errorImage = errorImage;
  }

  public void setPlaceholderImage(Drawable placeholderImage) {
    this.placeholderImage = placeholderImage;
  }

  public void setPlaceholderImageResId(int placeholderImageResId) {
    this.placeholderImageResId = placeholderImageResId;
  }
}
