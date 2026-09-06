# Changelog

Written for people who use the app. The full engineering history is in the commit log.

## 0.24.0

### The new look

- The whole app was redrawn: one set of controls, five text sizes, a palette that comes from the theme, and frosted panels over the video where the device can draw them.
- Home is one form: who you are, which server, which player, join. It lays itself out for a phone, a phone on its side, a tablet or a desktop window.
- The room has a rail for its actions, a status line, and controls that hide themselves while the video plays and come back on a tap.
- Settings are a console: search, grouped rows, values in the unit people read, colours edited in place, and each player's settings inside the player category.
- Themes preview as a miniature of the app, and the theme creator shows what a seed colour will do before you save.
- Motion can be reduced from settings. A screen reader hears who is ready, connection changes and new messages.
- Watching alone is a plain choice on the home screen, not something hidden inside About.

### Players

- KitePlayer, a new engine built for this app on top of FFmpeg, is on Android, iOS and desktop as an experimental choice: chapters, external and styled subtitles, pitch-preserved speed from 0.25x to 4x, and links over https.
- Three engines per platform: ExoPlayer, mpv and KitePlayer on Android; the system player, VLCKit and KitePlayer on iOS. VLC on Android and mpv on iOS are gone. VLCKit is the iOS default.
- Volume is one ladder: the device's own level up to 100, then the engine's gain past it where the engine has any.
- Every seek takes one path. Double taps add up to one jump, and a long press shows where it will land before it commits.
- Watching alone, the app remembers where a file was left and offers to pick it up.
- Seeking with KitePlayer lands on the exact frame in one step, instead of showing two pictures a blink apart, and is about twice as fast as before. Files carrying tens of thousands of subtitle lines no longer make it re-read the whole list every time a line changes.

### Sync and playback

- Fixed the phantom pause: an engine stopping on its own no longer pauses the whole room.
- The room shows when it is waiting for the video instead of looking frozen, and says who it is waiting for before playback starts.
- Audio and subtitle picks survive a reload on every engine, and follow your preferred languages.
- Tracks marked as accessibility captions, audio description or forced now say so in the picker.
- The end of a file moves the playlist on by one, not once per person watching.
- A file that is still opening no longer drags the whole room back to the start.
- Loading your file after the room has already started now syncs you to the room, instead of leaving you paused at the start.
- A per-user time offset lets two different rips of the same film be watched together.
- The three drift thresholds (rewind, slowdown, fast-forward) are settings now.
- A phone coming back from the background follows the room instead of dragging it back.

### Room and chat

- Chat colours follow the theme, so they stay readable on a light one.
- Chat timestamps follow your device's clock format.
- Slash commands in the chat box: /ready, /room, /seek, /op, /users and /help.
- Volume and brightness are reachable without a swipe.
- You can mute someone, and peer image links stay hidden until you tap them.
- The user list has two views, compact and detailed, with each person's file, its length, its size and whether it matches yours.
- A playlist change can be taken back.
- Picture in picture morphs out of the video instead of appearing from nowhere.
- The room can follow the device's rotation, or stay landscape. Your choice, in settings.
- Android: lock screen and headset controls work in the room and follow the same readiness rules as the play button.
- A television keeps the room's controls inside the visible area.
- The locked screen tells you how to unlock it, instead of leaving you guessing.
- The room shows whether your connection is encrypted.
- Panels look right on a light theme, not bruised at the bottom and edgeless at the top.
- Buffering no longer shoves the play button around. The button rounds into a circle and its colours move until the player catches up.
- The custom skip button sits between the two jump buttons.
- Chat that fades in while the controls are hidden now shows in the top middle, under the notices, and is never smaller than a notice. It used to sit tiny at the left edge.
- Subtitle search downloads work again, and a failed search says why.
- A bug report sent from the app says which engine, which build and which device.

### Connection

- Encrypted connections now check the server's certificate against the name you dialled.
- You can require encryption. A server that offers none is refused before anything is sent.
- A dropped connection reconnects with a growing wait instead of hammering the server.
- A handshake that never finishes gives up instead of hanging.
- Leaving the server address empty joins the official server, as it always looked like it would.
- The app's own services can no longer be pushed onto plain HTTP by the network you are on.
- A pasted link is checked against the host it really names, not the text in front of it.

### Hosting

- Hosting lives on the home screen now, under the server choice, with the address first.
- The hosting screen and its notification speak the app's language.
- A silent client is dropped after the timeout the server already advertised.
- An operator password works only for the room it belongs to.
- Creating a managed room really does put the password on your clipboard, which it has claimed to do for a long time.

### Language

- Change the app's language from settings, on every platform, with no restart.
- All seven translations are complete: Arabic, German, Spanish, French, Polish, Russian and Chinese.
- Audio and subtitle language names are shown in your own language.

### Elsewhere

- Invite links: share a room with a link, and open one to join.
- About lists what the app is built from, and can check for a newer release.
- Settings can be exported to a file and imported back.
- A damaged settings file no longer crashes the app at launch; it starts on defaults and tells you.
- Service keys are never written to the log, so a log you share carries none.
- The GIF service can be told to stop recognising you between sessions.
- Startup no longer reads preferences on the drawing thread, and neither does logging.
- Android downloads are two files now: the full universal APK and the smaller exo-only one. The per-CPU-type files are gone; Google Play serves each phone only what it needs.
- The app also runs on desktop (Windows, macOS, Linux) with KitePlayer. Installers are not part of this release yet.
