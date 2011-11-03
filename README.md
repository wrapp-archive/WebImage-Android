WebImage for Android
====================

WebImage is an Android library which asynchronously downloads images from the
internet and caches them for quick access. It has the following features:

- Images are downloaded in a background thread pool, keeping the GUI thread
  responsive.
- Images are cached to SD card, and if desired, also in a fast-access
  in-memory cache.
- If the user removes the SD card, the library will still work (albeit,
  without being able to save images to the file cache).
- Image cache removes old images, and can fetch HTTP headers to check if an
  image is expired and needs to be re-downloaded.
- Good performance, even on older Android 2.1 devices.
- Compatible with API level 7 and up.
- No additional library or framework dependencies!
- Builds quickly and easily with ant, or simply drop the tiny jarfile to your
  project's libs directory.


Project History
---------------

WebImage was written for [Wrapp](http://www.wrapp.com) for use in our [Android
client](http://market.android.com/details?id=com.wrapp.android) in the summer
of 2011 by Nik Reiman. We needed a library capable of handling a large number
of asynchronous image downloads from the web without effecting the GUI, and
with the exception of [droid-fu](https://github.com/kaeppler/droid-fu) the
only code to do this exists in the form of StackOverflow answers.

This seemed to be a common wheel which many Android developers continue to
re-invent, so we broke off some of our image handling code into a separate,
modular library. Hopefully other developers will find it useful in their
projects.


Usage
-----

To use WebImage in your Android application, simply download the latest
[WebImage.jar](https://github.com/wrapp/WebImage-Android/downloads) file and
copy it to your project's "libs" directory. You may need to configure your IDE
to include jarfiles from this directory if you have not already done so.

Your project will need to declare the following permissions in the
AndroidManifest file:

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

To actually download images, use the included `WebImageView` class, which is a
subclass of `ImageView` and has a method named `setImageUrl()`. This class can
be used either from XML layout files or in your classes.

Although you can override this class to provide post-processing on the
drawable (from outside of the GUI thread, even), you can also fetch raw
drawables asynchronously via `WebImage.load()`.

During your application's initialization, you can configure WebImage's caching
and logging behavior. Your individual activities can also tell the library to
free memory, cancel pending requests, and so on. All of these tasks are
detailed in the top-level [WebImage class](https://github.com/wrapp/WebImage-Android/blob/master/src/com/wrapp/android/webimage/WebImage.java).

For more details, check out the source code, which is also bundled in the
distribution jarfile. WebImage also has includes a [some example
projects](https://github.com/wrapp/WebImage-Android/tree/master/examples)
which show how the library is most effectively used.


Licensing
---------

WebImage is licensed under the MIT license. See the file LICENSE.txt for more
details.

