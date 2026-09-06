# App screenshots

Real captures from September 6, 2026. The PNG originals are untouched; the README loads smaller WebP copies and links to the originals.

| View | Original PNG | README WebP | WebP bytes |
|---|---|---|---:|
| ASUS Android home, portrait | [1080 × 2400](android-portrait.png) | [720 × 1600](android-portrait.webp) | 33,614 |
| ASUS Android room, landscape | [2400 × 1080](android-landscape.png) | [1920 × 864](android-landscape.webp) | 93,292 |
| macOS room, captured by the owner | [3420 × 1962](macos.png) | [1920 × 1101](macos.webp) | 116,408 |

The Android shots came directly from the physical ASUS phone. The macOS original is the owner's attached window capture. No screen content was generated or retouched.

To refresh the display copies, run from the repository root with ImageMagick:

```sh
magick art/screenshots/android-portrait.png -resize 720x1600 -strip -quality 90 -define webp:method=6 art/screenshots/android-portrait.webp
magick art/screenshots/android-landscape.png -resize 1920x864 -strip -quality 90 -define webp:method=6 art/screenshots/android-landscape.webp
magick art/screenshots/macos.png -resize 1920x -strip -quality 90 -define webp:method=6 art/screenshots/macos.webp
```

These exports preserve the full image and its proportions, with the macOS height rounded to the nearest pixel. Together they load 243,314 bytes.

The Play Store copies in `fastlane/metadata/android/en-US/images/phoneScreenshots/` are 24-bit RGB PNGs without transparency. Plain `#141119` padding gives portrait 1350 × 2400 (9:16) and landscape 2400 × 1350 (16:9). Every original pixel remains, centered without cropping or scaling.

```sh
magick art/screenshots/android-portrait.png -background '#141119' -gravity center -extent 1350x2400 -alpha remove -alpha off PNG24:fastlane/metadata/android/en-US/images/phoneScreenshots/android-portrait.png
magick art/screenshots/android-landscape.png -background '#141119' -gravity center -extent 2400x1350 -alpha remove -alpha off PNG24:fastlane/metadata/android/en-US/images/phoneScreenshots/android-landscape.png
```

The retired screenshots were also removed from the AltStore feed. Its [`screenshots` field is optional](https://faq.altstore.io/developers/make-a-source); add fresh iOS captures there when available.
