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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

public class ImageDownloader {
  private static final int CONNECTION_TIMEOUT_IN_MS = 10 * 1000;
  private static final int DEFAULT_BUFFER_SIZE = 8192;
  private static String userAgent = null;

  public static boolean loadImage(final Context context, final String imageKey, final URL imageUrl) {
    AndroidHttpClient httpClient = null;
    HttpEntity responseEntity = null;
    InputStream contentInputStream = null;

    try {
      final String imageUrlString = imageUrl.toString();
      if(imageUrlString == null || imageUrlString.length() == 0) {
        throw new Exception("Passed empty URL");
      }
      LogWrapper.logMessage("Requesting image '" + imageUrlString + "'");
      httpClient = AndroidHttpClient.newInstance(getUserAgent());
      final HttpParams httpParams = httpClient.getParams();
      httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, CONNECTION_TIMEOUT_IN_MS);
      httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT_IN_MS);
      httpParams.setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
      final HttpGet httpGet = new HttpGet(imageUrlString);
      final HttpResponse response = httpClient.execute(httpGet);

      responseEntity = response.getEntity();
      if(responseEntity == null) {
        throw new Exception("No response entity for image: " + imageUrl.toString());
      }
      contentInputStream = responseEntity.getContent();
      if(contentInputStream == null) {
        throw new Exception("No content stream for image: " + imageUrl.toString());
      }

      Bitmap bitmap = BitmapFactory.decodeStream(contentInputStream);
      ImageCache.saveImageInFileCache(context, imageKey, bitmap);
      LogWrapper.logMessage("Downloaded image: " + imageUrl.toString());
      bitmap.recycle();
    }
    catch(IOException e) {
      LogWrapper.logException(e);
      return false;
    }
    catch(Exception e) {
      LogWrapper.logException(e);
      return false;
    }
    finally {
      try {
        if(contentInputStream != null) {
          contentInputStream.close();
        }
        if(responseEntity != null) {
          responseEntity.consumeContent();
        }
      }
      catch(IOException e) {
        LogWrapper.logException(e);
      }
    }

    return true;
  }

  public static Date getServerTimestamp(final URL imageUrl) {
    Date expirationDate = new Date();

    try {
      final String imageUrlString = imageUrl.toString();
      if(imageUrlString == null || imageUrlString.length() == 0) {
        throw new Exception("Passed empty URL");
      }
      LogWrapper.logMessage("Requesting image '" + imageUrlString + "'");
      final HttpClient httpClient = new DefaultHttpClient();
      final HttpParams httpParams = httpClient.getParams();
      httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, CONNECTION_TIMEOUT_IN_MS);
      httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT_IN_MS);
      httpParams.setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
      final HttpHead httpHead = new HttpHead(imageUrlString);
      final HttpResponse response = httpClient.execute(httpHead);

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
}
