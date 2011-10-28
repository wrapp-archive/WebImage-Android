package com.wrapp.android.webimage;

import android.graphics.drawable.Drawable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDownloader {
  public static Drawable loadImage(final String imageKey, final URL imageUrl) {
    Drawable drawable = null;
    InputStream contentInputStream = null;

    try {
      HttpResponse response = getHttpResponseForImage(imageUrl);
      HttpEntity responseEntity = response.getEntity();
      if(responseEntity == null) {
        throw new Exception("No response entity for image: " + imageUrl.toString());
      }
      contentInputStream = responseEntity.getContent();
      if(contentInputStream == null) {
        throw new Exception("No content stream for image: " + imageUrl.toString());
      }

      drawable = Drawable.createFromStream(contentInputStream, imageKey);
      LogWrapper.logMessage("Downloaded image " + imageUrl.toString());
    }
    catch(IOException e) {
      LogWrapper.logException(e);
    }
    catch(Exception e) {
      LogWrapper.logException(e);
    }
    finally {
      assert contentInputStream != null;
      try {
        contentInputStream.close();
      }
      catch(IOException e) {
        LogWrapper.logException(e);
      }
    }

    return drawable;
  }

  public static Date getServerTimestamp(final URL imageUrl) {
    Date expirationDate = new Date();

    try {
      HttpResponse response = getHttpResponseForImage(imageUrl);
      Header[] header = response.getHeaders("Expires");
      if(header != null && header.length > 0) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        expirationDate = dateFormat.parse(header[0].getValue());
        LogWrapper.logMessage("Image at " + imageUrl.toString() + " expires on " + expirationDate.toString());
      }
    }
    catch(Exception e) {
      LogWrapper.logException(e);
    }

    return expirationDate;
  }

  private static HttpResponse getHttpResponseForImage(URL url) throws Exception {
    String imageUrl = url.toString();
    if(imageUrl == null || imageUrl.length() == 0) {
      throw new Exception("Passed empty URL");
    }
    LogWrapper.logMessage("Requesting image '" + imageUrl + "'");
    HttpClient httpClient = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet(imageUrl);
    HttpResponse response = httpClient.execute(httpGet);
    return response;
  }
}
