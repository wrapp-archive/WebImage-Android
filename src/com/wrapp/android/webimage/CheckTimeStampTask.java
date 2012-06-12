package com.wrapp.android.webimage;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.util.Log;

public class CheckTimeStampTask implements Runnable {
  private Context context;
  private ImageRequest request;

  public CheckTimeStampTask(Context context, ImageRequest request) {
    this.context = context;
    this.request = request;
  }

  @Override
  public void run() {
    try {
      LogWrapper.logMessage("Requesting timestamp for " + request.imageUrl);
      File cacheFile = new File(ImageCache.getCacheDirectory(context), request.imageKey);
      Date expirationDate = ImageDownloader.getServerTimestamp(request.imageUrl);
      Date now = new Date();
      if(expirationDate.after(now)) {
        LogWrapper.logMessage("Cached version of " + request.imageUrl.toString() + " is still current, updating timestamp");
        if(!cacheFile.setLastModified(now.getTime())) {
          LogWrapper.logMessage("Can't update timestamp!");
          // TODO: It seems that in some cases this call will always return false and refuse to update the timestamp
          // For more info, see: http://code.google.com/p/android/issues/detail?id=18624
          // This occurs on other devices, including my Galaxy Nexus. Not sure how many others have this bug.
        }
      } else {
        LogWrapper.logMessage("Cached version of " + request.imageUrl.toString() + " found, but has expired.");
        cacheFile.delete();
        
        WebImage.getLoader(context).forceUpdateImage(request);
      }
    } catch (IOException e) {
      Log.e("WebImage", "Could not check timestamp", e);
    }
  }
}
