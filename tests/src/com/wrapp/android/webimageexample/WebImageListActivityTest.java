package com.wrapp.android.webimageexample;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.wrapp.android.webimageexample.WebImageListActivityTest \
 * com.wrapp.android.webimageexample.tests/android.test.InstrumentationTestRunner
 */
public class WebImageListActivityTest extends ActivityInstrumentationTestCase2<WebImageListActivity> {

    public WebImageListActivityTest() {
        super("com.wrapp.android.webimageexample", WebImageListActivity.class);
    }

  public void testName() throws Exception {
    assertEquals(1, 1);
  }
}
