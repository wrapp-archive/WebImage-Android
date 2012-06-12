package com.wrapp.android.webimage;

import java.io.File;

import android.graphics.Bitmap;

public interface BitmapLoader {
  Bitmap load(File file);
}
