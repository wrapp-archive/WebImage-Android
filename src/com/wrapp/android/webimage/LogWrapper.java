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

import android.util.Log;

public class LogWrapper {
  public static int level;
  public static String tag;

  public static void enableLogging(String tag, int level) {
    LogWrapper.tag = tag;
    LogWrapper.level = level;
  }

  public static final void logMessage(String message) {
    // Log.println() will throw if the message is null or empty, so better to do the check here
    if(tag != null && message != null && message.length() > 0) {
      Log.println(level, tag, message);
    }
  }
  


  public static void logException(Throwable t) {
    logException("Exception thrown", t);
  }
  
  public static void logException(String message, Throwable t) {
    if(tag != null) {
      Log.e(tag, message, t);
    }
  }
}