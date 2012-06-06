package com.wrapp.android.webimage;

import java.io.File;
import java.util.Date;

public class CheckTimeStampTask implements Runnable {
  private ImageRequest request;

  public CheckTimeStampTask(ImageRequest request) {
    this.request = request;
  }

  @Override
  public void run() {
    LogWrapper.logMessage("Requesting timestamp for " + request.imageUrl);
    File cacheFile = new File(ImageCache.getCacheDirectory(request.context), request.imageKey);
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
    }
    else {
      LogWrapper.logMessage("Cached version of " + request.imageUrl.toString() + " found, but has expired.");
      cacheFile.delete();
      
      ImageLoader.getInstance(request.context).forceUpdateImage(request.imageUrl, request.loadOptions);
    }
  }
}
