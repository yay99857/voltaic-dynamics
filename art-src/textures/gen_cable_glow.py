"""Glow layer of the Voltaic Cable: only the blue energy channel of voltaic_cable.png, a little brighter, everything
else transparent. The lit cable models draw it over the cable at full brightness while energy flows. Run from the
repo root:

    python art-src/textures/gen_cable_glow.py
"""
import colorsys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
BLOCK = ROOT / "src/main/resources/assets/voltaicdynamics/textures/block"


def main():
    src = Image.open(BLOCK / "voltaic_cable.png").convert("RGBA")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if 0.5 < h < 0.6 and s > 0.5:  # the cyan-blue channel
                l = min(1.0, l * 1.12)
                nr, ng, nb = colorsys.hls_to_rgb(h, l, s)
                out.putpixel((x, y), (round(nr * 255), round(ng * 255), round(nb * 255), 255))
    out.save(BLOCK / "voltaic_cable_glow.png")
    print("wrote voltaic_cable_glow.png")


if __name__ == "__main__":
    main()
