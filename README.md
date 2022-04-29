# Xodus

**Xodus** is a small casual game based on stories of people evacuating from the war.

![960x540](https://user-images.githubusercontent.com/89737218/165823298-ac353ae3-d0ef-4a3c-9bb7-67a6bb7fe8f1.jpg)

https://user-images.githubusercontent.com/89737218/165823337-0669a28b-4005-4c90-a1e3-43e43e1c82f5.mp4

**Game objective:** keep the balls alive and running. Avoid collisions and the Shredder line.
You can replace the track sections on the field and choose moving direction on the track forks.
You are playing against the Chaos: after you end your turn, the Chaos makes its move, sometimes for worse,
sometimes for better.

You can adjust the field size, the number of Chaos mover per turn, the Shredder line speed, and whether the killed
balls respawn or not.

The field can be scrolled and zoomed, by pinch-zooms, long touches and (Ctrl-)mouse wheel on desktops.

There is no game timer and no hall of fame. There are also no sound, no ads and no in-game purchases.
The game is completely free and will remain so. It does not use Internet connection and does not require
any device permissions.

The game is auto-saved after each turn.

## Download

The game is provided in two options:

- **Desktop Java**. [Download Xodus.jar](https://github.com/andrzej-nov/Xodus/releases/download/v1.1/Xodus.jar).
  Run it with `java -jar Xodus.jar` command line, or in most cases just double-click the Xodus.jar 
  file. It has been tested with Java 18 Runtime, should also work with prior versions up to Java 8.
    - **On MacOS** you will get a warning about unidentified developer. Start the Xodus.jar 
      using Finder context menu instead of Launchpad,
      [as explained here](https://www.bemidjistate.edu/offices/its/knowledge-base/how-to-open-an-app-from-an-unidentified-developer-and-exempt-it-from-gatekeeper/)
      .

- **Android**. [Get it on Google Play](https://play.google.com/store/apps/details?id=com.andrzejn.xodus)
  (recommended, but not available yet because of the Google Play approval lag) or
  [download the Xodus.apk](https://github.com/andrzej-nov/Xodus/releases/download/v1.1/Xodus.apk)
  here for manual install (it might be sometimes also a newer version than on the Google Play). It has been tested
  on Android 8.0 and 10.0, should also work on any Android version starting from 4.4 and later.

There is no iOS build because I do not have tools to test and deploy it to the AppStore. If somebody completes the iOS
module (see below), I will add it here.

## Donation

If you like the game and want to support the author, you may donate arbitrary amount via following
link: https://pay.fondy.eu/s/Eb30H8espN (processed by the [Fondy.eu](https://fondy.io/) payment system).

## Development

The game is provided under the [Creative Commons Attribution license](https://creativecommons.org/licenses/by/4.0/).
Please feel free to reuse, extend, derive, improve etc. as long as you keep a reference to the original and mention me,
Andrzej Novosiolov, as the original author.

The game has been implemented using following tools and libraries:

- [IntelliJ IDEA 2022.1 (Community Edition)](https://www.jetbrains.com/idea/download/)
- [Android Studio 2021.1.1 Patch 2](https://developer.android.com/studio) (for the Android emulator)
- [Gradle 7.0.4](https://gradle.org/)
- [Kotlin 1.6.20](https://kotlinlang.org/)
- [libGDX 1.10.0](https://libgdx.com/)
- [libKTX 1.10.0-rc2](https://libktx.github.io/)
- [ShapeDrawer 2.5.0](https://github.com/earlygrey/shapedrawer#shape-drawer)
- [Universal Tween Engine 6.3.3](https://github.com/AurelienRibon/universal-tween-engine)
- Free icons from https://www.flaticon.com/

The `ios` module is present in the project and compiling, but I did not tested it because I do not have Apple devices
and tools for that. If you make it work, I would gratefully accept the pull request.
