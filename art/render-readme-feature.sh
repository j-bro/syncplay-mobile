#!/usr/bin/env bash
# Insert original screenshot pixels into the generated frame artwork.
set -euo pipefail

art_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
render_tmp="$(mktemp -d "${TMPDIR:-/tmp}/synkplay-hero.XXXXXX")"
trap 'rm -rf -- "$render_tmp"' EXIT

magick -size 1774x887 xc:black -fill white \
  -draw 'roundrectangle 845,139 1587,581 16,16' -fill black \
  -draw 'roundrectangle 1550,359 1741,791 28,28' "$render_tmp/desktop-mask.png"
magick -size 1774x887 xc:black -fill white \
  -draw 'roundrectangle 551,618 1166,819 14,14' "$render_tmp/landscape-mask.png"
magick -size 1774x887 xc:black -fill white \
  -draw 'roundrectangle 1559,369 1733,781 18,18' "$render_tmp/portrait-mask.png"

cp "$art_dir/readme-feature-frame.webp" "$render_tmp/banner.webp"

insert_screen() {
  local image_name="$1" box_size="$2" box_position="$3" mask_name="$4"
  # Contain the whole screenshot. Black padding keeps the original aspect ratio.
  magick "$art_dir/screenshots/$image_name" -filter Lanczos -resize "$box_size" \
    -background black -gravity center -extent "$box_size" "$render_tmp/screen.png"
  magick -size 1774x887 xc:none "$render_tmp/screen.png" -gravity northwest \
    -geometry "$box_position" -compose Over -composite "$render_tmp/placed.png"
  magick "$render_tmp/placed.png" "$render_tmp/$mask_name-mask.png" \
    -alpha off -compose CopyOpacity -composite "$render_tmp/clipped.png"
  magick "$render_tmp/banner.webp" "$render_tmp/clipped.png" -compose Over \
    -composite "$render_tmp/banner.png"
  # Lossless intermediates avoid repeated compression of artwork and text.
  magick "$render_tmp/banner.png" -define webp:lossless=true "$render_tmp/banner.webp"
}

insert_screen macos.png 743x443 +845+139 desktop
insert_screen android-landscape.png 616x202 +551+618 landscape
insert_screen android-portrait.png 175x413 +1559+369 portrait

magick "$render_tmp/banner.png" -filter Lanczos -resize 1600x800 \
  -colorspace sRGB -strip -quality 92 -define webp:method=6 "$art_dir/readme-feature.webp"
