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
   * This method is called when the drawable has been downloaded (or retreived from cache) and is
   * ready to be displayed. If you override this class, then you should not call this method via
   * super.onDrawableLoaded(). Instead, handle the drawable as necessary (ie, resizing or other
   * transformations), and then call postToGuiThread() to display the image from the correct thread.
   * @param drawable Drawable returned from web/cache
   */
  public void onDrawableLoaded(final Drawable drawable) {
    postToGuiThread(new Runnable() {
      public void run() {
        setImageDrawable(drawable);
      }
    });
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
