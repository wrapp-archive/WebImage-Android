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
import android.graphics.BitmapFactory;

import java.net.URL;

public final class ImageRequest {
  public Context context;
  public String imageKey;
  public URL imageUrl;
  public Listener listener;
  public boolean cacheInMemory;
  public BitmapFactory.Options loadOptions;

  public interface Listener {
    public void onBitmapLoaded(final RequestResponse requestResponse);
    public void onBitmapLoadError(String message);
    public void onBitmapLoadCancelled();
  }

  public ImageRequest(final Context context, URL imageUrl, Listener listener, boolean cacheInMemory, BitmapFactory.Options options) {
    this.context = context;
    this.imageKey = ImageCache.getCacheKeyForUrl(imageUrl);
    this.imageUrl = imageUrl;
    this.listener = listener;
    this.cacheInMemory = cacheInMemory;
    this.loadOptions = options;
  }
}
