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

import android.graphics.Bitmap;

public final class ImageRequest {
  public String imageKey;
  public String imageUrl;
  public BitmapLoader bitmapLoader;
  public boolean forceDownload = false;

  public interface Listener {
    public void onBitmapLoaded(ImageRequest request, Bitmap bitmap);
    public void onBitmapLoadError(String message);
    public void onBitmapLoadCancelled();
  }
  
  public ImageRequest(String imageUrl) {
    this.imageKey = ImageCache.getCacheKeyForUrl(imageUrl);
    this.imageUrl = imageUrl;
    bitmapLoader = new StandardBitmapLoader();
  }

  public ImageRequest(String imageUrl, BitmapLoader bitmapLoader) {
    this.imageKey = ImageCache.getCacheKeyForUrl(imageUrl);
    this.imageUrl = imageUrl;
    this.bitmapLoader = bitmapLoader;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ImageRequest)) {
      return false;
    }
    ImageRequest other = (ImageRequest) obj;
    if (imageUrl == null) {
      if (other.imageUrl != null) {
        return false;
      }
    } else if (!imageUrl.equals(other.imageUrl)) {
      return false;
    }
    return true;
  }
}
