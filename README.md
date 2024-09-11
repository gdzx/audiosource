# Audio Source

Audio Source forwards Android microphone audio input to the PulseAudio daemon
through ADB, so you can use your Android device as a USB microphone.

![screenshot](assets/screenshot.png)

## Requirements

- Device with at least Android 4.0 (API level 14), but fully tested only on
  Android 10 (API level 29) so your mileage may vary.
- GNU/Linux machine with:
  - Android SDK Platform Tools (requires `adb` in `PATH`).
  - PulseAudio (requires `pactl` in `PATH`).
  - Python 3 (requires `python3` in `PATH`).

## Usage

1. Install the Audio Source APK by following the [build
   instructions](#build-and-install), or from the
   [releases](https://github.com/gdzx/audiosource/releases).
2. Enable *Android Debug Bridge* (ADB) from the *Developer options* and connect
   the device to your computer.
3. Into the linux Clone the repo using terminal with git clone https://github.com/gdzx/audiosource
4. cd in the directory cd /path/to/the/audiocource or cd audiosource
5. Run `./audiosource run` to start Audio Source and forward the audio
   automatically. (You may have to grant the permission to record audio in
   Android.)
6. Run `./audiosource volume LEVEL`, to set the PulseAudio source volume to
   LEVEL, for instance `200%` (you will likely need to set the volume higher
   than 100%).

## Build and install

Run `./gradlew tasks` to list the available commands.

### Debug

```shell
$ ./audiosource build
$ ./audiosource install
```

### Release

1. Generate a Java KeyStore:

   ```shell
   $ keytool -keystore /home/user/android.jks -genkey -alias release \
          -keyalg RSA -keysize 2048 -validity 30000
   ```

2. Create `keystore.properties` in the project root directory containing:

   ```
   storeFile=/home/user/android.jks
   storePassword=STORE_PASS
   keyAlias=release
   keyPassword=KEY_PASS
   ```

3. Build and install:

   ```shell
   $ export AUDIOSOURCE_PROFILE=release
   $ ./audiosource build
   $ ./audiosource install
   ```

## Acknowledgement

[sndcpy](https://github.com/rom1v/sndcpy) for the initial implementation of
audio playback forwarding.

## License

This project is licensed under the MIT license ([LICENSE](LICENSE) or
http://opensource.org/licenses/MIT).
