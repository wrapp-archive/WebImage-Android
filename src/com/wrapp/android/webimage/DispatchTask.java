package com.wrapp.android.webimage;

import java.util.concurrent.Callable;

import android.content.Context;

public class DispatchTask implements Callable<DispatchTask.NextTask> {
  public enum NextTask {
    FILE,
    WEB
  }
  
  private Context context;
  private ImageRequest request;

  public DispatchTask(Context context, ImageRequest request) {
    this.context = context;
    this.request = request;
  }

  @Override
  public NextTask call() throws Exception {
    if (ImageCache.isImageCached(context, request.imageKey)) {
      return NextTask.FILE;
    } else {
      return NextTask.WEB;
    }
  }

}
