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
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.net.URL;

/**
 * ImageView successor class which can load images asynchronously from the web. This class
 * is safe to use in ListAdapters or views which may trigger many simultaneous requests.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class WebImageView extends ImageView implements ImageRequest.Listener {
  Handler uiHandler;
  private Drawable errorImage;
  private Listener listener;

  public interface Listener {
    public void onImageLoadStarted();
    public void onImageLoadComplete();
    public void onImageLoadError();
    public void onImageLoadCancelled();
  }

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

  /**
   * Set a listener to be informed about events from this view. If this is not set, then no events are sent.
   * @param listener Listener
   */
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  /**
   * Load an image asynchronously from the web
   * @param imageUrl Image URL to download image from. By default, this image will be cached to
   * disk (ie, SD card), but not in memory.
   */
  public void setImageUrl(URL imageUrl) {
    //noinspection NullableProblems
    setImageUrl(imageUrl, false, null, null);
  }

  /**
   * Load an image asynchronously from the web
   * @param imageUrl Image URL to download image from
   * @param cacheInMemory True to keep the downloaded drawable in the memory cache. Set to true for faster
   * access, but be careful about using this flag, as it can consume a lot of memory. This is recommended
   * only for activities which re-use the same images frequently.
   * @param options Options to use when loading the image. See the documentation for {@link BitmapFactory.Options}
   * for more details. Can be null.
   * @param errorImage Drawable to be displayed in case the image could not be loaded.
   */
  public void setImageUrl(URL imageUrl, boolean cacheInMemory, BitmapFactory.Options options, Drawable errorImage) {
    this.errorImage = errorImage;
    if(this.listener != null) {
      listener.onImageLoadStarted();
    }
    ImageLoader.load(imageUrl, this, cacheInMemory, options);
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
   * @param bitmap Bitmap returned from web/cache
   */
  public void onBitmapLoaded(final Bitmap bitmap) {
    postToGuiThread(new Runnable() {
      public void run() {
        setImageBitmap(bitmap);
      }
    });
    if(listener != null) {
      listener.onImageLoadComplete();
    }
  }

  /**
   * This method is called if the drawable could not be loaded for any reason. If you need a callback
   * to react to these events, you should override onImageError() instead.
   * @param message Error message (non-localized)
   */
  public void onBitmapLoadError(String message) {
    LogWrapper.logMessage(message);
    postToGuiThread(new Runnable() {
      public void run() {
        if(errorImage != null) {
          setImageDrawable(errorImage);
        }
      }
    });
    if(listener != null) {
      listener.onImageLoadError();
    }
  }

  /**
   * Called when the URL which the caller asked to load was cancelled. This can happen for a number
   * of reasons, including the activity being closed or scrolling rapidly in a ListView. For this
   * reason it is recommended not to do so much work in this method.
   */
  public void onBitmapLoadCancelled() {
    if(listener != null) {
      listener.onImageLoadCancelled();
    }
  }

  /**
   * Post a message to the GUI thread. This should be used for updating the component from
   * background tasks.
   * @param runnable Runnable task
   */
  public final void postToGuiThread(Runnable runnable) {
    uiHandler.post(runnable);
  }
}
