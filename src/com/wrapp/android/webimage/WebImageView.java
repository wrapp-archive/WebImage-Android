/*
 * Copyright (c) 2011 Bohemian Wrappsody, AB
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

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  /**
   * Load an image asynchronously from the web
   * @param imageUrl Image URL to download image from. By default, this image will be cached to
   * disk (ie, SD card), but not in memory.
   */
  public void setImageUrl(URL imageUrl) {
    ImageLoader.load(imageUrl, this, false);
  }

  /**
   * Load an image asynchronously from the web
   * @param imageUrl Image URL to download image from
   * @param cacheInMemory True to keep the downloaded drawable in the memory cache. Set to true for faster
   * access, but be careful about using this flag, as it can consume a lot of memory. This is recommended
   * only for activities which re-use the same images frequently.
   */
  public void setImageUrl(URL imageUrl, boolean cacheInMemory) {
    ImageLoader.load(imageUrl, this, cacheInMemory);
  }

  /**
   * Load an image asynchronously from the web
   * @param imageUrl Image URL to download image from
   * @param cacheInMemory True to keep the downloaded drawable in the memory cache. Set to true for faster
   * access, but be careful about using this flag, as it can consume a lot of memory. This is recommended
   * only for activities which re-use the same images frequently.
   * @param errorImage Drawable to be displayed in case the image could not be loaded.
   */
  public void setImageUrl(URL imageUrl, boolean cacheInMemory, Drawable errorImage) {
    this.errorImage = errorImage;
    ImageLoader.load(imageUrl, this, cacheInMemory);
  }

  /**
   * This method is called when the drawable has been downloaded (or retreived from cache) and is
   * ready to be displayed. If you override this class, then you should not call this method via
   * super.onDrawableLoaded(). Instead, handle the drawable as necessary (ie, resizing or other
   * transformations), and then call postToGuiThread() to display the image from the correct thread.
   *
   * If you only need a callback to be notified about the drawable being loaded to update other
   * GUI elements and whatnot, then you should override onImageLoaded() instead.
   *
   * @param drawable Drawable returned from web/cache
   */
  public void onDrawableLoaded(final Drawable drawable) {
    postToGuiThread(new Runnable() {
      public void run() {
        setImageDrawable(drawable);
        onImageLoaded();
      }
    });
  }

  /**
   * This method is called if the drawable could not be loaded for any reason. If you need a callback
   * to react to these events, you should override onImageError() instead.
   * @param message Error message (non-localized)
   */
  public void onDrawableError(String message) {
    LogWrapper.logMessage(message);
    postToGuiThread(new Runnable() {
      public void run() {
        onImageError();
      }
    });
  }

  /**
   * Called when the drawable request was cancelled. This may happen for a number of reasons, including
   * clearing the request queue or attempting to load an image multiple times, as sometimes happens
   * when recycling views in list adapters. Override this method to get a callback in case you want to
   * know when this happens, which can be useful for synchronizing progress spinners or other things
   * which rely on knowing the number of actively running background tasks.
   *
   * This method is called from a background thread, so if you attempt to directly update the GUI from
   * it you will get an exception. You've been warned!
   */
  public void onDrawableLoadCancelled() {}

  /**
   * Override this method to perform additional work after the image has been loaded. Note that
   * this call runs from the GUI thread, so if you have a lot of work to do (such as image resizing
   * or other processing), you are better off overriding onDrawableLoaded() instead.
   */
  public void onImageLoaded() {}

  /**
   * Override this method to perform additional work if there was an error loading the image. If an
   * error image drawable was set in the call to setImageUrl(), then that will be displayed here.
   * Note that this method is called from the GUI thread, so you should avoid doing too much work
   * here.
   */
  public void onImageError() {
    if(this.errorImage != null) {
      setImageDrawable(errorImage);
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
