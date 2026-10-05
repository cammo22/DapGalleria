#!/usr/bin/env python3
"""Genera l'icona di DapGalleria.

Un'unica geometria produce:
  - app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml  (icona adattiva Android)
  - docs/icon.svg e docs/icon.png                                                   (README / GitHub)

Uso: python3 tools/make_icon.py   (serve `pip install cairosvg` solo per il PNG)
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
DOCS = ROOT / "docs"

# --- palette ---------------------------------------------------------------
BG = [(0.0, "#FF8A5B"), (0.5, "#FF3D81"), (1.0, "#7B3FF2")]
SKY = [(0.0, "#FFE7F0"), (1.0, "#E6DAFF")]
VIOLET, PINK, SUN, RED, GREEN = "#7B3FF2", "#FF3D81", "#FFB020", "#FF4D5E", "#2EE59D"

# --- geometria (viewport 108x108, zona sicura = cerchio di 66 al centro) ----
CARD = dict(x=35, y=28, w=38, h=50, r=5)
PHOTO = "M40.5,31 H67.5 A2.5,2.5 0 0 1 70,33.5 V60.5 A2.5,2.5 0 0 1 67.5,63 H40.5 A2.5,2.5 0 0 1 38,60.5 V33.5 A2.5,2.5 0 0 1 40.5,31 Z"
MOUNTAIN_BIG = "M38,60.5 L48.5,47.5 L59,63 H40.5 A2.5,2.5 0 0 1 38,60.5 Z"
MOUNTAIN_SMALL = "M50.5,63 L61,50.5 L70,60.5 A2.5,2.5 0 0 1 67.5,63 Z"
SUN_C = (63.5, 38, 3.4)
BADGE_Y, BADGE_R, BADGE_X_DEL, BADGE_X_KEEP = 70.5, 3.4, 46, 62
X_MARK = "M44.8,69.3 L47.2,71.7 M47.2,69.3 L44.8,71.7"
CHECK_MARK = "M60.6,70.6 L61.7,71.7 L63.6,69.4"
PIVOT = (54, 53)
ROT_FRONT, ROT_BACK = 8, -13


def card_path(x, y, w, h, r):
    return (f"M{x + r},{y} H{x + w - r} A{r},{r} 0 0 1 {x + w},{y + r} V{y + h - r} "
            f"A{r},{r} 0 0 1 {x + w - r},{y + h} H{x + r} A{r},{r} 0 0 1 {x},{y + h - r} "
            f"V{y + r} A{r},{r} 0 0 1 {x + r},{y} Z")


def circle_path(cx, cy, r):
    return f"M{cx - r},{cy} A{r},{r} 0 1 1 {cx + r},{cy} A{r},{r} 0 1 1 {cx - r},{cy} Z"


BACK = dict(CARD, x=CARD["x"] - 3, y=CARD["y"] - 2)
CARD_D = card_path(**CARD)
BACK_D = card_path(**BACK)
SHADOW_D = card_path(CARD["x"], CARD["y"] + 1.4, CARD["w"], CARD["h"], CARD["r"])


# --- SVG ------------------------------------------------------------------
def svg_stops(stops):
    return "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in stops)


def svg_art():
    cx, cy = PIVOT
    return f"""
  <g transform="rotate({ROT_BACK} {cx} {cy})"><path d="{BACK_D}" fill="#fff" fill-opacity="0.55"/></g>
  <g transform="rotate({ROT_FRONT} {cx} {cy})">
    <path d="{SHADOW_D}" fill="#000" fill-opacity="0.16"/>
    <path d="{CARD_D}" fill="#fff"/>
    <path d="{PHOTO}" fill="url(#sky)"/>
    <circle cx="{SUN_C[0]}" cy="{SUN_C[1]}" r="{SUN_C[2]}" fill="{SUN}"/>
    <path d="{MOUNTAIN_BIG}" fill="{VIOLET}"/>
    <path d="{MOUNTAIN_SMALL}" fill="{PINK}"/>
    <circle cx="{BADGE_X_DEL}" cy="{BADGE_Y}" r="{BADGE_R}" fill="{RED}"/>
    <circle cx="{BADGE_X_KEEP}" cy="{BADGE_Y}" r="{BADGE_R}" fill="{GREEN}"/>
    <path d="{X_MARK}" stroke="#fff" stroke-width="1.1" stroke-linecap="round" fill="none"/>
    <path d="{CHECK_MARK}" stroke="#fff" stroke-width="1.1" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
  </g>"""


def svg_full():
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">{svg_stops(BG)}</linearGradient>
    <linearGradient id="sky" x1="0" y1="31" x2="0" y2="63" gradientUnits="userSpaceOnUse">{svg_stops(SKY)}</linearGradient>
    <clipPath id="mask"><rect width="108" height="108" rx="25" ry="25"/></clipPath>
  </defs>
  <g clip-path="url(#mask)">
    <rect width="108" height="108" fill="url(#bg)"/>{svg_art()}
  </g>
</svg>
"""


# --- Android vector drawable ---------------------------------------------------
AAPT = 'xmlns:aapt="http://schemas.android.com/aapt"'
HEAD = f'<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    {AAPT}\n    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n'


def xml_gradient(x1, y1, x2, y2, stops, indent):
    pad = " " * indent
    items = "".join(f'{pad}        <item android:offset="{o}" android:color="{c}" />\n' for o, c in stops)
    return (f'{pad}<aapt:attr name="android:fillColor">\n{pad}    <gradient\n{pad}        android:startX="{x1}" android:startY="{y1}"\n'
            f'{pad}        android:endX="{x2}" android:endY="{y2}"\n{pad}        android:type="linear">\n{items}{pad}    </gradient>\n{pad}</aapt:attr>\n')


def xml_background():
    return (HEAD + '    <path android:pathData="M0,0h108v108h-108z">\n' + xml_gradient(0, 0, 108, 108, BG, 8) + "    </path>\n</vector>\n")


def xml_foreground():
    cx, cy = PIVOT
    out = HEAD
    out += f'    <group android:rotation="{ROT_BACK}" android:pivotX="{cx}" android:pivotY="{cy}">\n'
    out += f'        <path android:fillColor="#FFFFFF" android:fillAlpha="0.55" android:pathData="{BACK_D}" />\n    </group>\n'
    out += f'    <group android:rotation="{ROT_FRONT}" android:pivotX="{cx}" android:pivotY="{cy}">\n'
    out += f'        <path android:fillColor="#000000" android:fillAlpha="0.16" android:pathData="{SHADOW_D}" />\n'
    out += f'        <path android:fillColor="#FFFFFF" android:pathData="{CARD_D}" />\n'
    out += f'        <path android:pathData="{PHOTO}">\n' + xml_gradient(0, 31, 0, 63, SKY, 12) + '        </path>\n'
    out += f'        <path android:fillColor="{SUN}" android:pathData="{circle_path(*SUN_C)}" />\n'
    out += f'        <path android:fillColor="{VIOLET}" android:pathData="{MOUNTAIN_BIG}" />\n'
    out += f'        <path android:fillColor="{PINK}" android:pathData="{MOUNTAIN_SMALL}" />\n'
    out += f'        <path android:fillColor="{RED}" android:pathData="{circle_path(BADGE_X_DEL, BADGE_Y, BADGE_R)}" />\n'
    out += f'        <path android:fillColor="{GREEN}" android:pathData="{circle_path(BADGE_X_KEEP, BADGE_Y, BADGE_R)}" />\n'
    out += f'        <path android:pathData="{X_MARK}" android:strokeColor="#FFFFFF" android:strokeWidth="1.1" android:strokeLineCap="round" />\n'
    out += f'        <path android:pathData="{CHECK_MARK}" android:strokeColor="#FFFFFF" android:strokeWidth="1.1" android:strokeLineCap="round" android:strokeLineJoin="round" />\n'
    out += "    </group>\n</vector>\n"
    return out


def xml_monochrome():
    cx, cy = PIVOT
    out = HEAD
    out += f'    <group android:rotation="{ROT_BACK}" android:pivotX="{cx}" android:pivotY="{cy}">\n'
    out += f'        <path android:fillColor="#000000" android:fillAlpha="0.5" android:pathData="{BACK_D}" />\n    </group>\n'
    out += f'    <group android:rotation="{ROT_FRONT}" android:pivotX="{cx}" android:pivotY="{cy}">\n'
    # scheda con foro per la foto (even-odd), poi montagne e sole pieni
    out += f'        <path android:fillColor="#000000" android:fillType="evenOdd" android:pathData="{CARD_D} {PHOTO}" />\n'
    out += f'        <path android:fillColor="#000000" android:pathData="{MOUNTAIN_BIG}" />\n'
    out += f'        <path android:fillColor="#000000" android:pathData="{MOUNTAIN_SMALL}" />\n'
    out += f'        <path android:fillColor="#000000" android:pathData="{circle_path(*SUN_C)}" />\n'
    out += f'        <path android:fillColor="#000000" android:pathData="{circle_path(BADGE_X_DEL, BADGE_Y, BADGE_R)}" />\n'
    out += f'        <path android:fillColor="#000000" android:pathData="{circle_path(BADGE_X_KEEP, BADGE_Y, BADGE_R)}" />\n'
    out += "    </group>\n</vector>\n"
    return out


ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""


def main():
    (RES / "drawable").mkdir(parents=True, exist_ok=True)
    (RES / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
    DOCS.mkdir(exist_ok=True)
    (RES / "drawable/ic_launcher_background.xml").write_text(xml_background())
    (RES / "drawable/ic_launcher_foreground.xml").write_text(xml_foreground())
    (RES / "drawable/ic_launcher_monochrome.xml").write_text(xml_monochrome())
    (RES / "mipmap-anydpi-v26/ic_launcher.xml").write_text(ADAPTIVE)
    (RES / "mipmap-anydpi-v26/ic_launcher_round.xml").write_text(ADAPTIVE)
    (DOCS / "icon.svg").write_text(svg_full())
    try:
        import cairosvg
        cairosvg.svg2png(bytestring=svg_full().encode(), write_to=str(DOCS / "icon.png"), output_width=512, output_height=512)
    except ImportError:
        print("cairosvg non installato: PNG saltato")
    print("Icona generata")


if __name__ == "__main__":
    main()
