package com.wrapp.android.webimage;

import java.io.IOException;
import java.util.concurrent.Callable;

import android.content.Context;

public class DownloadTask implements Callable<Void> {
  private Context context;
  private ImageRequest request;

  public DownloadTask(Context context, ImageRequest request) {
    this.context = context;
    this.request = request;
  }
  
  @Override
  public Void call() throws Exception {
    if (ImageCache.isImageCached(context, request.imageKey) && !request.forceDownload) {
      return null;
    } else if (ImageDownloader.loadImage(context, request.imageKey, request.imageUrl)) {
      return null;
    } else {
      // Image was neither cached nor did we manage to download it
      throw new IOException("Could not download image");
    }
  }
}
