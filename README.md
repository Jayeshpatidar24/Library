# Jayesh Library — Android Studio project

This is an Android Studio source project (not a prebuilt APK).

## Build an installable APK
1. Install Android Studio on a computer.
2. Open this `JayeshLibrary` folder in Android Studio.
3. Let Gradle sync finish; use JDK 17 if prompted.
4. Choose **Build → Build APK(s)**.
5. Find the debug APK under `app/build/outputs/apk/debug/app-debug.apk`, then copy it to your Android phone and install it. You may need to allow installs from that file manager.

## Included in this starter build
- Dark/light theme toggle
- Import one or multiple local files using Android's file picker
- Persistent personal bookshelf, search, and format shelves
- PDF page-by-page reader
- Basic EPUB HTML chapter rendering
- CBZ/ZIP image-comic reader
- TXT/MD reader with font size controls
- Files remain local; no upload/account backend

## Important limitations in this first build
- This source project has not been compiled/tested into an APK in this environment.
- Android's native PDF renderer supports PDF only. EPUB rendering is a basic first pass and may not preserve all chapter order, images, CSS, or navigation.
- CBR/RAR, DRM-protected books, and several uncommon formats are not supported yet.
- Book cover thumbnails are generated as format-based cover tiles, not extracted real covers yet.
- PDF has previous/next controls but no direct page-number jump or zoom slider yet.
- This is a foundation to iterate on, not a production-ready release.
