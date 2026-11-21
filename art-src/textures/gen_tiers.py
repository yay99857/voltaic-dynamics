"""Textures of the higher tiers: Elite, Ultimate and Creative Accumulators and Fluid Tanks, and the Tier Installers.

Each one is an existing texture with its warm accent (the copper or gold trim) recoloured into the tier's ramp,
keeping the shading: the accent's lightness picks the shade. Run from the repo root:

    python art-src/textures/gen_tiers.py
"""
import colorsys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "src/main/resources/assets/voltaicdynamics/textures"

# dark, mid, light
RAMPS = {
    "advanced": ["#8a5d12", "#c4911d", "#eec142"],
    "elite": ["#4e2380", "#8448cc", "#c592f7"],
    "ultimate": ["#6a1020", "#b3263a", "#f2626c"],
    "creative": ["#6e1466", "#cf34b9", "#ff9cf1"],
}


def rgb(hex_color):
    return tuple(int(hex_color[i:i + 2], 16) for i in (1, 3, 5))


def is_accent(r, g, b):
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    return s > 0.35 and l > 0.12 and (h < 0.17 or h > 0.95)


def ramp_at(ramp, t):
    """t in 0..1 across dark → mid → light."""
    stops = [rgb(c) for c in ramp]
    if t <= 0.5:
        a, b, u = stops[0], stops[1], t / 0.5
    else:
        a, b, u = stops[1], stops[2], (t - 0.5) / 0.5
    return tuple(round(a[i] + (b[i] - a[i]) * u) for i in range(3))


def recolor(src, dst, ramp):
    im = Image.open(TEX / src).convert("RGBA")
    px = im.load()
    accents = [(x, y) for y in range(im.height) for x in range(im.width)
               if px[x, y][3] > 0 and is_accent(*px[x, y][:3])]
    if not accents:
        raise SystemExit(f"{src}: no accent pixels")
    light = {p: colorsys.rgb_to_hls(*(c / 255 for c in px[p][:3]))[1] for p in accents}
    lo, hi = min(light.values()), max(light.values())
    for p in accents:
        t = 0.5 if hi == lo else (light[p] - lo) / (hi - lo)
        px[p] = ramp_at(ramp, t) + (px[p][3],)
    im.save(TEX / dst)
    print("wrote", dst)


def main():
    for tier in ("elite", "ultimate", "creative"):
        for face in ("side", "top"):
            recolor(f"block/advanced_accumulator_{face}.png", f"block/{tier}_accumulator_{face}.png", RAMPS[tier])
    for tier in ("advanced", "elite", "ultimate", "creative"):
        for face in ("side", "top"):
            recolor(f"block/fluid_tank_{face}.png", f"block/{tier}_fluid_tank_{face}.png", RAMPS[tier])
        recolor("item/fluid_tank.png", f"item/{tier}_fluid_tank.png", RAMPS[tier])
    for tier in ("advanced", "elite", "ultimate"):
        recolor("item/speed_upgrade.png", f"item/{tier}_tier_installer.png", RAMPS[tier])


if __name__ == "__main__":
    main()
