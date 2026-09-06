# Prebuilt libraries

## libffmpeg_media3exo_1.8.0.aar

The FFmpeg audio renderer extension for Media3 ExoPlayer. It gives ExoPlayer the audio
decoders Android itself does not ship, which is what lets the ExoPlayer engine play files
whose sound is E-AC-3, TrueHD, FLAC, Opus, Vorbis or ALAC.

**What it is.** The `androidx.media3.decoder.ffmpeg` extension from the Media3 source tree,
built for four ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) against FFmpeg's libavcodec 60.
Media3 does not publish this extension to Maven: the project ships the Java side and asks you
to build the native half yourself, which is why it is a file here rather than a coordinate.

**Version.** Named for the Media3 release it was built against, 1.8.0. The Media3 dependency
in the version catalog can move ahead of it without a rebuild: the extension's own interface
has been stable across the 1.x line, which is why the file name says so.

**Licence.** The Java side is Apache 2.0, from the Android Open Source Project. The bundled
FFmpeg is used under the LGPL 2.1 or later, which is FFmpeg's default: the build enables only
the audio decoders listed above and does not enable GPL components. FFmpeg's own source is at
<https://ffmpeg.org>, and the corresponding source for this build is FFmpeg's release tarball
for the version named in the extension's build script, unmodified.

**Build inputs.** Read back out of the shipped binary, so this describes the file that is here
rather than what was meant to be built.

| | |
|---|---|
| Media3 tag | `1.8.0` |
| libavcodec | 60.3.100 (FFmpeg 6.0 line) |
| Decoders enabled | `vorbis opus flac alac eac3 truehd`, plus FFmpeg's built-in PCM set |
| ABIs | arm64-v8a, armeabi-v7a, x86, x86_64 |
| Toolchain | NDK r26 clang 18.0.1 (build 12027248, based on r522817) |

The configure line itself is not in the binary: `build_ffmpeg.sh` does not compile
`--enable-*` flags into a configuration string the way a full FFmpeg build does. The decoder
list above is the argument it was given, and it is what the script turns into those flags.

**How to rebuild it.** Follow the extension's own instructions in the Media3 source tree, at
`libraries/decoder_ffmpeg/README.md`. In short:

```bash
git clone --depth 1 --branch 1.8.0 https://github.com/androidx/media.git
cd media/libraries/decoder_ffmpeg/src/main/jni
git clone --depth 1 --branch n6.0 https://git.ffmpeg.org/ffmpeg.git ffmpeg
./build_ffmpeg.sh "$(pwd)" "$ANDROID_NDK_HOME" linux-x86_64 21 vorbis opus flac alac eac3 truehd
cd ../../../.. && ./gradlew :lib-decoder-ffmpeg:assembleRelease
```

The result is the same AAR.

**Integrity.**

```
sha256  eb7c57daaeed34e27b87120c2e595959656118ac182b9e380b3fff940cc8d834
```

Check it with `shasum -a 256 shared/libs/libffmpeg_media3exo_1.8.0.aar`.

**Note.** This is one of the FFmpeg copies inside an Android build. mpv carries its own, and
so does KiteCodec. They are separate builds with separate licences; see the licences screen in
the app's About page for what each one is offered under.
