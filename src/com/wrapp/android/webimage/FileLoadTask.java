package com.wrapp.android.webimage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;

import android.content.Context;
import android.graphics.Bitmap;

public class FileLoadTask implements Callable<Bitmap> {
  private Context context;
  private ImageRequest request;
  
  public FileLoadTask(Context context, ImageRequest request) {
    this.context = context;
    this.request = request;
  }

  @Override
  public Bitmap call() throws Exception {
    Bitmap b = loadBitmap();
    
    if (b != null) {
      return b;
    } else {
      throw new IOException("Could not load bitmap from disk");
    }
  }
  
  private Bitmap loadBitmap() throws IOException {
    File cacheFile = new File(ImageCache.getCacheDirectory(context), request.imageKey);
    if (!cacheFile.exists()) {
      throw new FileNotFoundException(cacheFile + " was not found");
    }

    // Check if the image has expired
    Date now = new Date();
    long fileAgeInMs = now.getTime() - cacheFile.lastModified();
    if (fileAgeInMs > ImageCache.getCacheRecheckAgeInMs()) {
      ImageLoader.getInstance(context).checkTimeStamp(request);
    }
    
    LogWrapper.logMessage("Loading image " + request.imageUrl + " from file cache");
    return request.bitmapLoader.load(cacheFile);
  }
}
