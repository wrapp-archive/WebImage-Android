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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Build;

public class ImageDownloader {
  private static final int CONNECTION_TIMEOUT_IN_MS = 10 * 1000;
  private static final int DEFAULT_BUFFER_SIZE = 8192;
  private static String userAgent = null;

  public static boolean loadImage(Context context, String imageKey, URL imageUrl) throws IOException {
    HttpClient client = createHttpClient(getUserAgent());
    
    try {
      HttpResponse response = client.execute(new HttpGet(imageUrl.toString()));
      
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        throw new IOException("Entity is null");
      }
      
      ReadableByteChannel fin = Channels.newChannel(new BufferedInputStream(entity.getContent()));
      
      File cacheFile = File.createTempFile("image-", "tmp", ImageCache.getCacheDirectory(context));
      FileChannel fout = new FileOutputStream(cacheFile).getChannel();
      
      try {
        ByteBuffer buff = ByteBuffer.allocate(4096);
        while (fin.read(buff) != -1 || buff.position() > 0) {
            buff.flip();
            fout.write(buff);
            buff.compact();
        }
      } finally {
        // Ensure we close streams in case we get interrupted
        fin.close();
        fout.close();
        entity.consumeContent();
      }

      LogWrapper.logMessage("Downloaded image " + imageUrl + " to file cache");
      File outputFile = new File(ImageCache.getCacheDirectory(context), imageKey);
      cacheFile.renameTo(outputFile);
      
      return true;
    } finally {
      if (client instanceof AndroidHttpClient) {
        AndroidHttpClient androidClient = (AndroidHttpClient) client;
        androidClient.close();
      }
    }
  }
  
  public static Date getServerTimestamp(URL imageUrl) throws IOException {
    HttpClient client = createHttpClient(getUserAgent());
    
    try {
      String url = imageUrl.toString();
      LogWrapper.logMessage("Requesting image " + url);
      
      HttpResponse response = client.execute(new HttpHead(url));
      
      Header[] header = response.getHeaders("Expires");
      
      if(header != null && header.length > 0) {
        Date expirationDate = parseServerDateHeader(header[0]);
        LogWrapper.logMessage("Image at " + imageUrl.toString() + " expires on " + expirationDate.toString());
        
        return expirationDate;
      } else {
        // Could not find any date
        return new Date();
      }
    } finally {
      if (client instanceof AndroidHttpClient) {
        AndroidHttpClient androidClient = (AndroidHttpClient) client;
        androidClient.close();
      }
    }
  }

  private static HttpClient createHttpClient(String userAgent) {
    HttpClient client = getHttpClient();

    HttpParams params = client.getParams();
    HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT_IN_MS);
    HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT_IN_MS);
    HttpConnectionParams.setSocketBufferSize(params, DEFAULT_BUFFER_SIZE);
    HttpClientParams.setRedirecting(params, true);
    
    return client;
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
