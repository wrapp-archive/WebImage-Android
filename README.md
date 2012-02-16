WebImage for Android
====================

WebImage is an Android library which asynchronously downloads images from the
internet and caches them for quick access. It has the following features:

- Images are downloaded in a background thread pool, keeping the GUI thread
  responsive. The size of the thread pool is based on the device's network
  speed, with more threads for faster connection types.
- Separate background thread for loading cached images from disk, which
  reduces I/O bottlenecks on most Android phones.
- Images are cached to SD card, or if unavailable, to the app's internal
  storage cache.
- Images saved in the cache are periodically checked to see if they have
  expired and must be re-downloaded. This query only fetches HTTP headers and
  is done in a separate low-priority thread.
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


Installation
------------

To use WebImage in your Android application, simply download the latest
[WebImage.jar](https://github.com/wrapp/WebImage-Android/downloads) file and
copy it to your project's "libs" directory. You may need to configure your IDE
to include jarfiles from this directory if you have not already done so.

Your project will need to declare the following permissions in the
AndroidManifest file:

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <!-- Only required if you want to dynamically change thread pool size -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

To actually download images, use the included `WebImageView` class, which is a
subclass of `ImageView` and has a method named `setImageUrl()`. This class can
be used either from XML layout files or in your classes. Subclassing this
class is a good way to provide extra functionality, such as rescaling the
download images or showing progress spinners while the image is loading.

Although you can subclass `WebImageView` to provide post-processing on the
image (which is done outside of the GUI thread), you can also fetch raw
bitmaps asynchronously via `WebImage.load()`. This approach is not usually
recommended, but may be preferable in some cases.


Configuration
-------------

During your application's initialization, you can configure WebImage's
caching, threading, and logging behavior. WebImage tries to use sane defaults,
so in most cases you should not need to change these behaviors, but if you do,
you must do so before attempting to fetch any images.

By default, WebImage is designed to work best in an environment where a large
number of images are being requested from the internet, like in a ListView
where each item is an image from the web. If your application only needs to
fetch a single image every now and then, you should probably set the maximum
number of worker threads to 1.

For more details, check out the source code, which is also bundled in the
distribution jarfile. WebImage also has includes [a demo application]
(https://github.com/wrapp/WebImage-Android/tree/master/examples)
which shows how the library can be used in your project.


Licensing
---------

WebImage is licensed under the MIT license. See the file LICENSE.txt for more
details.

