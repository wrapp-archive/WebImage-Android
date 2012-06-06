package com.wrapp.android.webimage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class FileLoadTask implements Callable<Bitmap> {
  private ImageRequest request;
  
  public FileLoadTask(ImageRequest request) {
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
  
  private Bitmap loadBitmap() {
    Bitmap bitmap = null;

    FileInputStream inputStream = null;
    File cacheFile = new File(ImageCache.getCacheDirectory(request.context), request.imageKey);
    if(cacheFile.exists()) {
      try {
        Date now = new Date();
        long fileAgeInMs = now.getTime() - cacheFile.lastModified();
        if(fileAgeInMs > ImageCache.getCacheRecheckAgeInMs()) {
          ImageLoader.getInstance(request.context).checkTimeStamp(request);
        }

        LogWrapper.logMessage("Loading image " + request.imageUrl + " from file cache");
        inputStream = new FileInputStream(cacheFile);
        bitmap = BitmapFactory.decodeFileDescriptor(inputStream.getFD(), null, request.loadOptions);
        if(bitmap == null) {
          throw new Exception("Could not create bitmap from image " + request.imageUrl.toString());
        }
      }
      catch(Exception e) {
        LogWrapper.logException(e);
      }
      finally {
        if(inputStream != null) {
          try {
            inputStream.close();
          }
          catch(IOException e) {
            LogWrapper.logException(e);
          }
        }
      }
    }

    return bitmap;
  }
}
