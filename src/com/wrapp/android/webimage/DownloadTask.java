package com.wrapp.android.webimage;

import java.io.IOException;
import java.util.concurrent.Callable;

import android.graphics.Bitmap;

public class DownloadTask implements Callable<Bitmap> {
  private ImageRequest request;

  public DownloadTask(ImageRequest request) {
    this.request = request;
  }
  
  @Override
  public Bitmap call() throws Exception {
    if (ImageCache.isImageCached(request.context, request.imageKey) && !request.forceDownload) {
      return null;
    } else if (ImageDownloader.loadImage(request.context, request.imageKey, request.imageUrl)) {
      return null;
    } else {
      // Image was neither cached nor did we manage to download it
      throw new IOException("Could not download image");
    }
  }
}
