package com.wrapp.android.webimage;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public class StandardBitmapLoader implements BitmapLoader {
  private BitmapFactory.Options options;
  
  public StandardBitmapLoader() {
  }
  
  public StandardBitmapLoader(Options options) {
    this.options = options;
  }

  @Override
  public Bitmap load(File file) {
    return BitmapFactory.decodeFile(file.getPath(), options); 
  }
}
