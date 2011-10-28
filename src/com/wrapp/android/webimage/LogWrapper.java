package com.wrapp.android.webimage;

import android.util.Log;

public class LogWrapper {
  public static int level;
  public static String tag;

  public static final void logMessage(String message) {
    if(tag != null) {
      Log.println(level, tag, message);
    }
  }

  public static final void logException(Exception exception) {
    if(tag != null) {
      StackTraceElement[] stackTraceElements = exception.getStackTrace();
      for(int i = 0; i < stackTraceElements.length; i++) {
        StackTraceElement element = stackTraceElements[i];
        if(element.getClassName().contains(ImageLoader.class.getSimpleName())) {
          String message = exception.getClass().getSimpleName() + " at " + element.getFileName() + ":" +
            element.getLineNumber() + ": " + exception.getMessage();
          Log.println(Log.ERROR, tag, message);
          break;
        }
      }
    }
  }
}