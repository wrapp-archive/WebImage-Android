Migrating From WebImage-1.x to 2.x
==================================

The API in WebImage-2.x has been significantly changed, and if you were using
a 1.x version of the library, you will most likely have to make some changes
in your code. Sorry!

Major changes since 1.x include:

- Elimination of the in-memory cache. Although this was very convenient, it is
  also the source of many OutOfMemory exceptions, and is not significantly
  faster than loading images from disk. After much careful consideration, I
  decided to remove this feature to prevent potential accidental misuse.
- Refactored and simplified calling API, particularly to the `WebImageView`
  class. This is probably the most noticeable change which will break things
  in your code.
- You must pass a context into most operations. Rather than setting the cache
  directory during initalization, your app's package name is automatically
  used.
- Now `Bitmap` is fetched instead of `Drawable`. This allows for a number of
  memory optimizations, and also for rescaling/downsampling images on the fly.

