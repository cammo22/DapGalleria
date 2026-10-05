#!/usr/bin/env python3
"""Genera docs/banner.png (1280x640) per README e anteprima social di GitHub. Richiede cairosvg."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import make_icon as icon  # noqa: E402

W, H = 1280, 640
FONT = "DejaVu Sans, Liberation Sans, sans-serif"


def pill(x, y, text, color):
    w = 24 + len(text) * 13.5
    return (f'<rect x="{x}" y="{y}" width="{w}" height="46" rx="23" fill="{color}" fill-opacity="0.14" stroke="{color}" stroke-opacity="0.55" stroke-width="2"/>'
            f'<text x="{x + w / 2}" y="{y + 31}" text-anchor="middle" font-family="{FONT}" font-size="21" font-weight="bold" fill="{color}">{text}</text>'), w


def build():
    pills, x = [], 110
    for text, color in [("FOTO", "#FF4D8D"), ("VIDEO", "#8B5CF6"), ("SWIPE", "#2EE59D")]:
        svg, w = pill(x, 516, text, color)
        pills.append(svg)
        x += w + 16
    bg_stops = icon.svg_stops(icon.BG)
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">
  <defs>
    <linearGradient id="page" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#17101F"/><stop offset="1" stop-color="#0B0B10"/></linearGradient>
    <radialGradient id="glowPink"><stop offset="0" stop-color="#FF3D81" stop-opacity="0.38"/><stop offset="1" stop-color="#FF3D81" stop-opacity="0"/></radialGradient>
    <radialGradient id="glowViolet"><stop offset="0" stop-color="#7B3FF2" stop-opacity="0.42"/><stop offset="1" stop-color="#7B3FF2" stop-opacity="0"/></radialGradient>
    <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">{bg_stops}</linearGradient>
    <linearGradient id="sky" x1="0" y1="31" x2="0" y2="63" gradientUnits="userSpaceOnUse">{icon.svg_stops(icon.SKY)}</linearGradient>
    <linearGradient id="title" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#FF6B8F"/><stop offset="1" stop-color="#A580FF"/></linearGradient>
    <clipPath id="mask"><rect width="108" height="108" rx="25" ry="25"/></clipPath>
  </defs>
  <rect width="{W}" height="{H}" fill="url(#page)"/>
  <circle cx="1000" cy="330" r="470" fill="url(#glowViolet)"/>
  <circle cx="260" cy="120" r="380" fill="url(#glowPink)"/>

  <!-- icona -->
  <g transform="translate(110 78) scale(2.1)">
    <g clip-path="url(#mask)"><rect width="108" height="108" fill="url(#bg)"/>{icon.svg_art()}</g>
  </g>

  <text x="104" y="420" font-family="{FONT}" font-size="104" font-weight="bold" fill="url(#title)">DapGalleria</text>
  <text x="110" y="478" font-family="{FONT}" font-size="36" fill="#C9C9D8">Scorri, tieni, elimina.</text>
  {''.join(pills)}

  <!-- carte -->
  <g transform="translate(642 -50) scale(7.2)">{icon.svg_art()}</g>
  <g transform="translate(916 190) rotate(-14)">
    <rect x="-8" y="-46" width="196" height="64" rx="14" fill="none" stroke="#2EE59D" stroke-width="7"/>
    <text x="90" y="3" text-anchor="middle" font-family="{FONT}" font-size="44" font-weight="bold" fill="#2EE59D" letter-spacing="3">TIENI</text>
  </g>
  <g transform="translate(1000 548) rotate(12)">
    <rect x="-8" y="-46" width="236" height="64" rx="14" fill="none" stroke="#FF4D5E" stroke-width="7"/>
    <text x="110" y="3" text-anchor="middle" font-family="{FONT}" font-size="44" font-weight="bold" fill="#FF4D5E" letter-spacing="3">ELIMINA</text>
  </g>
</svg>
"""


if __name__ == "__main__":
    import cairosvg

    out = icon.DOCS / "banner.png"
    cairosvg.svg2png(bytestring=build().encode(), write_to=str(out), output_width=W, output_height=H)
    print("Banner generato:", out)
