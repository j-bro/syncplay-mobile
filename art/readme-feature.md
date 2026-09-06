# README feature graphic

[readme-feature.webp](readme-feature.webp) is the 1600 × 800 README banner (107,450 bytes).
Its three screens show the actual Android portrait, Android landscape and macOS captures
in [screenshots/](screenshots/README.md). The macOS image was supplied by the maintainer.

## Rebuild

```sh
bash art/render-readme-feature.sh
```

Requires ImageMagick. The script inserts the original PNG captures into
[readme-feature-frame.webp](readme-feature-frame.webp), scales them proportionally, adds black
padding where needed, clips them inside the bezels, then exports WebP at quality 92.
The full screenshots remain available separately in the README.

The surrounding frame artwork was revised with the **built-in image-generation tool** on
2026-09-07. Its generated screen contents were cleared; the final screens are composed from
the original captures by the script, so no generated interface labels or controls are used.
The separate [Play Store artwork](play-store/README.md) uses a simpler design.

## Frame revision prompt

Reference 1 was the previous README banner. References 2–4 were
`android-portrait.png`, `android-landscape.png` and `macos.png`, respectively.
The tool output supplied the surrounding frame layout; the rebuild step above supplies the
final screen pixels.

Use case: compositing.
Edit target: reference image 1, the existing Synkplay README banner.
Supporting inserts: reference image 2 is the real Android portrait Home screenshot; reference image 3 is the real Android landscape room screenshot; reference image 4 is the user's real macOS desktop room screenshot.
Change only the screen contents and the minimum frame geometry needed to fit these real screenshots. Preserve the existing banner background, logo, exact typography, all copy, colors, framing style, lighting and composition. Keep the result a 2:1 landscape banner. The left side must remain visually unchanged: Synkplay; Your video. Everyone in sync.; Watch together on phones and computers.
Compositing assignment: put reference image 4 into the largest landscape frame at upper right, reference image 3 into the small landscape frame below it, and reference image 2 into the portrait phone on the far right. Remove every trace of the previous paper-boat and moon illustration from inside the screens. Fit each entire actual screenshot without stretching. The desktop screenshot includes the real macOS title bar; retain it. Adjust the phone portrait frame to a taller/narrower proportion so the full Home screenshot fits. Adjust the small landscape frame to the actual wide phone ratio. Keep the three screens and connecting line on the right side, away from the copy.
Critical invariants: These are real app screenshots, not prompts to redraw the interface. Faithfully composite their actual image content including its real video frame and exact app controls. Do not invent, retouch, replace, omit or restyle app controls, labels or typography. Do not turn the screenshots into illustrations. No fake UI, no replacement media artwork, no additional text or device frames. Preserve screenshot detail as well as the target resolution allows. Output only the revised finished banner.
