<p align="center">
  <img src="art/readme-feature.webp" width="960" alt="Synkplay — Your video. Everyone in sync. Watch together on phones and computers.">
</p>

<p align="center">
  <b>Android · iOS · Windows · macOS · Linux</b><br>
  No sign-up. No ads. Open source.
</p>

<p align="center">
  <a href="#download">Download</a> &nbsp;·&nbsp;
  <a href="#start-a-room">Start a room</a> &nbsp;·&nbsp;
  <a href="#screenshots">Screenshots</a> &nbsp;·&nbsp;
  <a href="#help">Help</a> &nbsp;·&nbsp;
  <a href="docs/DEVELOPING.md">Build</a>
</p>

Play, pause and seek together. Synkplay is an independent [Syncplay](https://syncplay.pl/) client,
compatible with the official desktop app.

> [!NOTE]
> Everyone opens the same video from their own file or link. Synkplay syncs playback; it does not send video or share your screen.

## Download

<!-- Rows have the same purpose on both platforms: store, direct file, other source. -->
<table align="center">
  <tr>
    <th align="center">Android</th>
    <th align="center">iPhone &amp; iPad</th>
  </tr>
  <tr>
    <td align="center">
      <a href="https://play.google.com/store/apps/details?id=com.yuroyami.syncplay">
        <img src="art/badges/google-play.png" width="140" height="42" alt="Install from Google Play">
      </a><br><sub>Store install</sub>
    </td>
    <td align="center">
      <a href="https://apps.apple.com/app/id6760187432">
        <img src="art/badges/app-store.png" width="140" height="42" alt="Install from the App Store">
      </a><br><sub>Store install</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/yuroyami/syncplay-mobile/releases/latest">
        <img src="art/badges/get-it-on-github.png" width="140" height="42" alt="Get the Android APK from GitHub Releases">
      </a><br><sub>Full Android APK</sub>
    </td>
    <td align="center">
      <a href="https://github.com/yuroyami/syncplay-mobile/releases/latest">
        <img src="art/badges/Download_Blue.png" width="140" height="42" alt="Get the IPA from GitHub Releases for sideloading">
      </a><br><sub>IPA for sideloading</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://apt.izzysoft.de/fdroid/index/apk/com.reddnek.syncplay">
        <img src="art/badges/IzzyOnDroid.png" width="140" height="42" alt="Get the ExoPlayer-only Android app from IzzyOnDroid">
      </a><br><sub>ExoPlayer only</sub>
    </td>
    <td align="center">
      <a href="https://celloserenity.github.io/altdirect/?url=https://raw.githubusercontent.com/yuroyami/syncplay-mobile/refs/heads/master/altstore_yuroyami.json">
        <img src="art/badges/AltSource_Blue.png" width="140" height="42" alt="Add the Synkplay source to AltStore">
      </a><br><sub>Add to AltStore</sub>
    </td>
  </tr>
</table>

**Android:** 8.0+ · **Latest IPA:** iOS 14.1+ · Store versions may arrive later.

**On a computer?** [Run Synkplay from source](docs/DEVELOPING.md#desktop), or install [official Syncplay](https://syncplay.pl/download/) to join the same rooms.

<details>
<summary><b>Which download should I choose?</b></summary>

- **Android:** choose Google Play for a store install, or the latest full GitHub APK for mpv, ExoPlayer and KitePlayer. The universal APK covers all supported CPU types.
- **IzzyOnDroid:** a smaller, ExoPlayer-only app. It installs alongside the full app and has separate settings.
- **iPhone / iPad:** install from the App Store. For an IPA, [set up AltStore Classic](https://faq.altstore.io/), then use **Add to AltStore** above.

</details>

## Start a room

1. **Meet in the same room.** Use the same server, port and room name. The default public server is fine.
2. **Open your video.** Each person loads their file or video link.
3. **Mark yourself ready.** When everyone is ready, press play.

You can share an invite from inside the room. Friends need Synkplay installed to open its invite links.

**Watching alone?** Tap the logo on Home, then **Watch alone**. No server needed; your playback position is saved.

## What you can do

| Want to… | Use… |
|:---|:---|
| Pause or skip together | Shared playback controls |
| Talk during the video | Text, emoji and GIF chat |
| Queue the next episode | Shared playlists with auto-advance |
| Adjust for a different intro | Your own time offset |
| Use your subtitle files | External tracks on supported players |
| Host your own room | **Host mine** on Home |

<details>
<summary><b>More controls, when you need them</b></summary>

Themes, gesture controls, reduced motion, shared-playlist undo, and audio/subtitle language preferences.
Picture-in-Picture is available on Android and with AVPlayer or VLCKit on iOS.
Managed rooms let a controller decide playback. Filename and file-size sharing can be hidden or hashed.
Encrypted connections are available when the server supports them.

</details>

## Screenshots

<p align="center">
  <a href="art/screenshots/android-portrait.png"><img src="art/screenshots/android-portrait.webp" width="180" alt="Android home in portrait: room details, server and video engine choices"></a>
  &nbsp;&nbsp;
  <a href="art/screenshots/android-landscape.png"><img src="art/screenshots/android-landscape.webp" width="500" alt="Android room in landscape: video, playback controls, chat and user info"></a>
</p>

<p align="center"><sub>Android home (portrait) · Android room (landscape)</sub></p>

<p align="center">
  <a href="art/screenshots/macos.png"><img src="art/screenshots/macos.webp" width="720" alt="macOS room: video, chat, playback controls and player settings"></a>
</p>

<p align="center"><sub>macOS room · Select any screenshot to see it full size.</sub></p>

## Help

<details>
<summary><b>Can my friends use Syncplay on PC?</b></summary>

Yes. Join the same server, port and room, then open the same video. You can mix Synkplay and the official Syncplay desktop client.

</details>

<details>
<summary><b>A video or subtitle will not play correctly.</b></summary>

Leave the room and choose a different player on Home before joining again.
The defaults are **mpv** on full Android builds and **VLCKit** on iOS.
KitePlayer is experimental on mobile. AVPlayer cannot load external subtitle files.
[Compare the players](docs/DEVELOPING.md#player-capabilities).

</details>

<details>
<summary><b>Why do I have two Android apps?</b></summary>

The IzzyOnDroid app and the full GitHub / Google Play app use different app IDs, so they can coexist.
One does not update the other. Each keeps its own settings.

</details>

**Still stuck?** [Report a bug](https://github.com/yuroyami/syncplay-mobile/issues/new?template=bug_report.yml) · [Ask a question](https://github.com/yuroyami/syncplay-mobile/discussions) · [Request a feature](https://github.com/yuroyami/syncplay-mobile/issues/new?template=feature_request.yml)

## Make it better

Small fixes help. A clearer translation, a reproducible bug report, or a tested patch is welcome.

**[Translate on Weblate](https://hosted.weblate.org/engage/syncplay-mobile/)** &nbsp;·&nbsp;
**[Build & test](docs/DEVELOPING.md)** &nbsp;·&nbsp;
**[Support development](https://ko-fi.com/sidenaakram)**

<details>
<summary><b>Thanks to the people behind it</b></summary>

[Syncplay](https://syncplay.pl/) and [Et0h](https://github.com/Et0h) for the original project and help;
[soredake](https://github.com/soredake) for testing;
[Osanosa](https://github.com/Osanosa) for contributions;
[Zhaodaidai](https://github.com/Zhaodaidai), [DoncanC](https://github.com/DoncanC), [ivy-reps](https://github.com/ivy-reps), and everyone translating on Weblate.

</details>

---

<p align="center">
  Built by <a href="https://github.com/yuroyami">yuroyami</a> with Kotlin &amp; Compose Multiplatform.<br>
  <a href="CHANGELOG.md">Release notes</a> &nbsp;·&nbsp;
  <a href="PRIVACY_POLICY.md">Privacy</a> &nbsp;·&nbsp;
  <a href="LICENSE">AGPL-3.0 license</a>
</p>
