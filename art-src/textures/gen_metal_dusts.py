"""Pontilha recipes for the iron, copper and gold dust icons: the Voltaic Dust sprite recoloured.

Open textures/item/voltaic_dust.png in Pontilha, then  pontilha run <metal>_dust.recipe.json
"""
import json
from pathlib import Path

VOLTAIC = ["#5a1414", "#8c2626", "#b8423a", "#d4664e", "#ea8f6c", "#ffc49e"]  # dark -> light
METALS = {
    "iron": ["#4a4440", "#6b635c", "#8f877e", "#b3aba1", "#d4cdc4", "#f2ede6"],
    "copper": ["#5a2a12", "#8a4419", "#b8622a", "#d9823c", "#eda45e", "#ffd0a0"],
    "gold": ["#6b5a10", "#9c8418", "#c9ad25", "#e8cf3a", "#f8e86a", "#fffbc4"],
}

if __name__ == "__main__":
    for metal, ramp in METALS.items():
        steps = [{"id": "color.replaceAll", "params": {"from": a, "to": b}} for a, b in zip(VOLTAIC, ramp)]
        recipe = {"name": f"{metal}_dust", "description": f"{metal} dust: Voltaic Dust recoloured (gen_metal_dusts.py).",
                  "steps": steps}
        (Path(__file__).parent / f"{metal}_dust.recipe.json").write_text(json.dumps(recipe, indent=1) + "\n")
        print(metal)
