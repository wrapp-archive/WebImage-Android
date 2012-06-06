package com.wrapp.android.webimage;

import com.wrapp.android.webimage.ImageLoader.DownloadListener;

public class DownloadTask implements Runnable {
  private DownloadListener listener;

  private ImageRequest request;

  public DownloadTask(ImageRequest request, DownloadListener listener) {
    this.request = request;
    this.listener = listener;
  }

  @Override
  public void run() {
    if (ImageCache.isImageCached(request.context, request.imageKey) && !request.forceDownload) {
      listener.onComplete(request);
    } else if (ImageDownloader.loadImage(request.context, request.imageKey, request.imageUrl)) {
      listener.onComplete(request);
    }
  }
}
