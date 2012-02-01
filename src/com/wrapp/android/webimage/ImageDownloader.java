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
import android.net.http.AndroidHttpClient;
import android.os.Build;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDownloader {
  private static final int CONNECTION_TIMEOUT_IN_MS = 10 * 1000;
  private static final int DEFAULT_BUFFER_SIZE = 8192;
  private static final int MAX_REDIRECT_COUNT = 4;
  private static String userAgent = null;

  public static boolean loadImage(final Context context, final String imageKey, final URL imageUrl) {
    return loadImage(context, imageKey, imageUrl, 0);
  }

  private static boolean loadImage(final Context context, final String imageKey, final URL imageUrl, int redirectCount) {
    if(redirectCount > MAX_REDIRECT_COUNT) {
      LogWrapper.logMessage("Too many redirects!");
      return false;
    }

    HttpClient httpClient = null;
    HttpEntity responseEntity = null;
    BufferedInputStream bufferedInputStream = null;
    BufferedOutputStream bufferedOutputStream = null;

    try {
      final String imageUrlString = imageUrl.toString();
      if(imageUrlString == null || imageUrlString.length() == 0) {
        throw new Exception("Passed empty URL");
      }
      LogWrapper.logMessage("Requesting image '" + imageUrlString + "'");
      httpClient = getHttpClient();
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
      final StatusLine statusLine = response.getStatusLine();
      final int statusCode = statusLine.getStatusCode();
      switch(statusCode) {
        case HttpStatus.SC_OK:
          break;
        case HttpStatus.SC_MOVED_TEMPORARILY:
        case HttpStatus.SC_MOVED_PERMANENTLY:
        case HttpStatus.SC_SEE_OTHER:
          final String location = response.getFirstHeader("Location").getValue();
          LogWrapper.logMessage("Image redirected to: " + location);
          // Force close the connection now, otherwise we risk leaking too many open HTTP connections
          responseEntity.consumeContent();
          responseEntity = null;
          if(httpClient instanceof AndroidHttpClient) {
            ((AndroidHttpClient)httpClient).close();
          }
          httpClient = null;
          return loadImage(context, imageKey, new URL(location), redirectCount + 1);
        default:
          LogWrapper.logMessage("Could not download image, got status code " + statusCode);
          return false;
      }

      bufferedInputStream = new BufferedInputStream(responseEntity.getContent());
      File cacheFile = new File(ImageCache.getCacheDirectory(context), imageKey);
      bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(cacheFile));

      long contentSize = responseEntity.getContentLength();
      long totalBytesRead = 0;
      try {
        int bytesRead;
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        do {
          bytesRead = bufferedInputStream.read(buffer, 0, DEFAULT_BUFFER_SIZE);
          if(bytesRead > 0) {
            bufferedOutputStream.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
          }
        } while(bytesRead > 0);
      }
      catch(IOException e) {
        LogWrapper.logException(e);
        return false;
      }

      if(totalBytesRead != contentSize) {
        LogWrapper.logMessage("Short read! Expected " + contentSize + "b, got " + totalBytesRead);
        return false;
      }
      LogWrapper.logMessage("Saved image " + imageKey + " to file cache");
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
        if(bufferedInputStream != null) {
          bufferedInputStream.close();
        }
        if(bufferedOutputStream != null) {
          bufferedOutputStream.flush();
          bufferedOutputStream.close();
        }
        if(responseEntity != null) {
          responseEntity.consumeContent();
        }
      }
      catch(IOException e) {
        LogWrapper.logException(e);
      }
      if(httpClient != null) {
        try {
          if(httpClient instanceof AndroidHttpClient) {
            ((AndroidHttpClient)httpClient).close();
          }
        }
        catch(Exception e) {
          // Ignore
        }
      }
    }

    return true;
  }

  public static Date getServerTimestamp(final URL imageUrl) {
    Date expirationDate = new Date();
    HttpClient httpClient = null;

    try {
      final String imageUrlString = imageUrl.toString();
      if(imageUrlString == null || imageUrlString.length() == 0) {
        throw new Exception("Passed empty URL");
      }
      LogWrapper.logMessage("Requesting image '" + imageUrlString + "'");
      httpClient = getHttpClient();
      final HttpParams httpParams = httpClient.getParams();
      httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, CONNECTION_TIMEOUT_IN_MS);
      httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT_IN_MS);
      httpParams.setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
      final HttpHead httpHead = new HttpHead(imageUrlString);
      final HttpResponse response = httpClient.execute(httpHead);

      Header[] header = response.getHeaders("Expires");
      if(header != null && header.length > 0) {
        expirationDate = parseServerDateHeader(header[0]);
        LogWrapper.logMessage("Image at " + imageUrl.toString() + " expires on " + expirationDate.toString());
      }
    }
    catch(Exception e) {
      LogWrapper.logException(e);
    }
    finally {
      if(httpClient != null) {
        try {
          if(httpClient instanceof AndroidHttpClient) {
            ((AndroidHttpClient)httpClient).close();
          }
        }
        catch(Exception e) {
          // Ignore
        }
      }
    }

    return expirationDate;
  }

  // AndroidHttpClient was introduced in API Level 8, but many 2.1 phones actually have it. Why this is,
  // I have no idea. However, on these devices it is much safer to use the default HTTP client instead
  // rather than risk throwing an exception when the class (or one of its methods) is not found.
  private static HttpClient getHttpClient() {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
      return new DefaultHttpClient();
    }
    else {
      return AndroidHttpClient.newInstance(getUserAgent());
    }
  }

  private static Date parseServerDateHeader(Header serverDateHeader) {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
      try {
        return dateFormat.parse(serverDateHeader.getValue());
      }
      catch(ParseException e) {
        return new Date();
      }
    }
    else {
      return new Date(AndroidHttpClient.parseDate(serverDateHeader.getValue()));
    }
  }

  private static String getUserAgent() {
    if(userAgent == null) {
      final String USER_AGENT_TEMPLATE = "WebImage-Android (%s %s; %s API%s; %s)";
      userAgent = String.format(USER_AGENT_TEMPLATE, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT, Locale.getDefault().toString());
    }
    return userAgent;
  }
}
