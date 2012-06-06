package com.wrapp.android.webimage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import com.wrapp.android.webimage.ImageLoader.FileLoadListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class FileLoadTask implements Runnable {
  private FileLoadListener listener;
  
  private ImageRequest request;
  
  public FileLoadTask(ImageRequest request, FileLoadListener listener) {
    this.listener = listener;
    this.request = request;
  }

  @Override
  public void run() {
    Bitmap bitmap = loadBitmap();
    
   if (bitmap != null) {
      // This should probably be called on UI thread here instead
      RequestResponse response = new RequestResponse(bitmap, request);
      listener.onComplete(response);
    } else {
      // Nothing
      LogWrapper.logMessage("FileLoadTask: No bitmap :(");
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
          CheckTimestampThread.getInstance().addTask(request);
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
