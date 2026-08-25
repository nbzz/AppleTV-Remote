"""Generates tvOS-shaped app icons: one geometry source, two outputs."""
import math, os

W, H = 400, 240
CX, CY = W / 2, H / 2

# ---------------------------------------------------------------- primitives

def circle(cx, cy, r):
    return (f"M{cx-r:.1f},{cy:.1f}a{r:.1f},{r:.1f} 0 1,0 {2*r:.1f},0"
            f"a{r:.1f},{r:.1f} 0 1,0 {-2*r:.1f},0z")

def ring(cx, cy, outer, inner):
    """Donut: outer circle wound against an inner one, for even-odd fill."""
    return circle(cx, cy, outer) + circle(cx, cy, inner)

def rrect(x, y, w, h, r):
    return (f"M{x+r:.1f},{y:.1f}h{w-2*r:.1f}a{r:.1f},{r:.1f} 0 0 1 {r:.1f},{r:.1f}"
            f"v{h-2*r:.1f}a{r:.1f},{r:.1f} 0 0 1 {-r:.1f},{r:.1f}h{-(w-2*r):.1f}"
            f"a{r:.1f},{r:.1f} 0 0 1 {-r:.1f},{-r:.1f}v{-(h-2*r):.1f}"
            f"a{r:.1f},{r:.1f} 0 0 1 {r:.1f},{-r:.1f}z")

def frame(x, y, w, h, r, t):
    """Rounded outline: a rect with a smaller one punched out of it."""
    return rrect(x, y, w, h, r) + rrect(x+t, y+t, w-2*t, h-2*t, max(r-t, 1))

def poly(points):
    d = f"M{points[0][0]:.1f},{points[0][1]:.1f}"
    for px, py in points[1:]:
        d += f"L{px:.1f},{py:.1f}"
    return d + "z"

def arc_band(cx, cy, r, t, a0, a1):
    """Sweep of a ring between two angles, thickness t. Angles in degrees."""
    ro, ri = r + t / 2, r - t / 2
    a0r, a1r = math.radians(a0), math.radians(a1)
    large = 1 if abs(a1 - a0) > 180 else 0
    x0o, y0o = cx + ro*math.cos(a0r), cy + ro*math.sin(a0r)
    x1o, y1o = cx + ro*math.cos(a1r), cy + ro*math.sin(a1r)
    x1i, y1i = cx + ri*math.cos(a1r), cy + ri*math.sin(a1r)
    x0i, y0i = cx + ri*math.cos(a0r), cy + ri*math.sin(a0r)
    return (f"M{x0o:.1f},{y0o:.1f}A{ro:.1f},{ro:.1f} 0 {large} 1 {x1o:.1f},{y1o:.1f}"
            f"L{x1i:.1f},{y1i:.1f}A{ri:.1f},{ri:.1f} 0 {large} 0 {x0i:.1f},{y0i:.1f}z")

def bar(cx, cy, r0, r1, width, deg):
    """Rectangle running outward from a centre along an angle."""
    h = width / 2
    pts = [(r0, -h), (r1, -h), (r1, h), (r0, h)]
    a = math.radians(deg)
    return poly([(cx + x*math.cos(a) - y*math.sin(a),
                  cy + x*math.sin(a) + y*math.cos(a)) for x, y in pts])

def gear(cx, cy, r_out, r_in, teeth, hole):
    pts = []
    for i in range(teeth * 2):
        a = math.pi * i / teeth
        r = r_out if i % 2 == 0 else r_in
        # Narrow at the tip, wide at the root, or the teeth read as a star.
        spread = 0.105 if i % 2 == 0 else 0.235
        for da in (-spread, spread):
            pts.append((cx + r*math.cos(a+da), cy + r*math.sin(a+da)))
    return poly(pts) + circle(cx, cy, hole)

def rot(points, cx, cy, deg):
    a = math.radians(deg)
    return [((x-cx)*math.cos(a) - (y-cy)*math.sin(a) + cx,
             (x-cx)*math.sin(a) + (y-cy)*math.cos(a) + cy) for x, y in points]

# -------------------------------------------------------------------- icons
# Each: gradient pair, then white glyph paths. "eo" marks even-odd fills.

def tv():
    return [frame(116, 54, 168, 116, 18, 14), rrect(176, 180, 48, 13, 6)]

def music():
    stem_l = rrect(178, 74, 11, 96, 5)
    stem_r = rrect(256, 60, 11, 96, 5)
    beam = poly([(178, 74), (267, 60), (267, 84), (178, 98)])
    return [circle(166, 170, 23), circle(244, 156, 23), stem_l, stem_r, beam]

def podcasts():
    return [circle(200, 158, 17),
            arc_band(200, 158, 46, 15, 205, 335),
            arc_band(200, 158, 80, 15, 210, 330),
            arc_band(200, 158, 114, 15, 215, 325)]

def photos():
    return [frame(120, 58, 160, 124, 18, 12),
            circle(163, 96, 15),
            poly([(133, 168), (186, 108), (226, 152), (246, 130), (267, 168)])]

def settings():
    return [gear(200, 120, 84, 60, 8, 26)]

def appstore():
    body = rrect(140, 104, 120, 92, 18)
    handle = arc_band(200, 104, 34, 13, 180, 360)
    return [body, handle]

def search():
    return [ring(188, 104, 54, 36) + bar(188, 104, 48, 116, 24, 45)]

def computers():
    return [frame(126, 56, 148, 100, 12, 12), rrect(102, 166, 196, 16, 8)]

def arcade():
    body = rrect(112, 74, 176, 100, 34)
    dpad = (rrect(140, 114, 46, 14, 7) + rrect(156, 98, 14, 46, 7))
    return [body + dpad + circle(238, 108, 12) + circle(262, 138, 12)]

def fitness():
    heart = ("M200,178C200,178 138,140 138,104C138,84 154,70 172,70"
             "C185,70 195,78 200,88C205,78 215,70 228,70C246,70 262,84 262,104"
             "C262,140 200,178 200,178z")
    pulse = poly([(150, 112), (178, 112), (188, 92), (202, 134), (214, 108),
                  (226, 108), (226, 122), (206, 122), (196, 148), (182, 106),
                  (176, 126), (150, 126)])
    return [heart + pulse]

def facetime():
    body = rrect(118, 78, 132, 84, 18)
    lens = poly([(262, 108), (300, 84), (300, 156), (262, 132)])
    return [body, lens]

def sing():
    stand = rrect(194, 168, 12, 26, 6) + rrect(164, 190, 72, 12, 6)
    return [rrect(178, 46, 44, 92, 22), arc_band(200, 116, 42, 13, 0, 180), stand]

def generic():
    out = []
    for r in range(3):
        for c in range(3):
            out.append(rrect(134 + c*50, 54 + r*50, 38, 38, 11))
    return ["".join(out)]

ICONS = {
    "tv":        (("#4C4C52", "#1A1A1D"), tv(),        True),
    "music":     (("#FB5C74", "#D62E62"), music(),     False),
    "podcasts":  (("#A85CF9", "#6E2BE0"), podcasts(),  False),
    "photos":    (("#3FC8F5", "#1E63D6"), photos(),    True),
    "settings":  (("#909399", "#484B53"), settings(),  True),
    "appstore":  (("#33A7FF", "#0A5FDC"), appstore(),  False),
    "search":    (("#6E7BF2", "#3239B8"), search(),    True),
    "computers": (("#7E8B9A", "#3B4854"), computers(), True),
    "arcade":    (("#FF8A3D", "#FF2D8F"), arcade(),    True),
    "fitness":   (("#3A3A3F", "#0E0E10"), fitness(),   True),
    "facetime":  (("#3BD962", "#12A046"), facetime(),  False),
    "sing":      (("#FF4FA3", "#A62BD6"), sing(),      False),
    "generic":   (("#78818E", "#3A424C"), generic(),   False),
}

# ------------------------------------------------------------------ outputs

def svg(name):
    (c0, c1), paths, eo = ICONS[name]
    rule = ' fill-rule="evenodd"' if eo else ""
    body = "".join(f'<path d="{p}" fill="#FFFFFF"{rule}/>' for p in paths)
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">'
            f'<defs><linearGradient id="g" x1="0" y1="0" x2="{W}" y2="{H}" '
            f'gradientUnits="userSpaceOnUse">'
            f'<stop offset="0" stop-color="{c0}"/><stop offset="1" stop-color="{c1}"/>'
            f'</linearGradient></defs>'
            f'<rect width="{W}" height="{H}" fill="url(#g)"/>{body}</svg>')

def vector(name):
    (c0, c1), paths, eo = ICONS[name]
    rule = '\n        android:fillType="evenOdd"' if eo else ""
    glyphs = "".join(
        f'\n    <path{rule}\n        android:fillColor="#FFFFFFFF"'
        f'\n        android:pathData="{p}" />' for p in paths)
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/generate_app_icons.py - do not edit by hand. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="200dp"
    android:height="120dp"
    android:viewportWidth="{W}"
    android:viewportHeight="{H}">
    <path android:pathData="M0,0h{W}v{H}h-{W}z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="{W}"
                android:endY="{H}">
                <item android:offset="0" android:color="{c0}" />
                <item android:offset="1" android:color="{c1}" />
            </gradient>
        </aapt:attr>
    </path>{glyphs}
</vector>
'''

if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        sys.exit("usage: generate_app_icons.py <res/drawable dir> [preview dir]\n"
                 "       previews are SVG and must not be written into res/")

    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name in ICONS:
        open(f"{out}/ic_app_{name}.xml", "w").write(vector(name))
    print(f"{len(ICONS)} vectors -> {out}")

    # SVGs render the same geometry for review. They are kept out of the
    # drawable directory, where aapt would choke on them.
    if len(sys.argv) > 2:
        preview = sys.argv[2]
        os.makedirs(preview, exist_ok=True)
        for name in ICONS:
            open(f"{preview}/{name}.svg", "w").write(svg(name))
        print(f"{len(ICONS)} previews -> {preview}")
