package com.wrapp.android.webimage;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ScaledBitmapLoader implements BitmapLoader {
  private int reqWidth;
  private int reqHeight;

  public ScaledBitmapLoader(int reqWidth, int reqHeight) {
    this.reqWidth = reqWidth;
    this.reqHeight = reqHeight;
  }

  @Override
  public Bitmap load(File file) {
    BitmapFactory.Options options = new BitmapFactory.Options();

    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getPath(), options);

    options.inSampleSize = calculateInSampleSize(options);

    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(file.getPath(), options);
  }

  // Borrowed from http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
  private int calculateInSampleSize(BitmapFactory.Options options) {
    // Raw height and width of image
    int height = options.outHeight;
    int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
      if (width > height) {
        inSampleSize = Math.round((float) height / (float) reqHeight);
      } else {
        inSampleSize = Math.round((float) width / (float) reqWidth);
      }
    }

    return inSampleSize;
  }
}
