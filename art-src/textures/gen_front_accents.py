"""Pontilha recipes that paint each machine front's window bevel in the machine's accent colour (the same colour
as its side bars, see gen_machine_sides.py): the dark top-left edge in the accent shadow, the light bottom-right
edge in the accent light. Only pixels of the bevel colours are touched, so the window art is kept.

Open textures/block/<machine>_front[_on].png in Pontilha, then  pontilha run <front>.accent.recipe.json
Animated fronts are vertical strips; every 16x16 tile gets the same edge.
"""
import json
from pathlib import Path

BEVEL_DARK, BEVEL_LIGHT = "#3d4450", "#687180"
ACCENTS = {
    "combustion": ["#9c2a2a", "#e0573f"],
    "electric_furnace": ["#a9592a", "#d6864a"],
    "alloy_smelter": ["#9c8418", "#e8cf3a"],
    "pulverizer": ["#2f7d4a", "#6fbf5a"],
    "fluid_generator": ["#57307f", "#c98be0"],
}
FRONTS = {  # file -> (machine, tiles)
    "combustion_front": ("combustion", 1), "combustion_front_on": ("combustion", 1),
    "electric_furnace_front": ("electric_furnace", 1), "electric_furnace_front_on": ("electric_furnace", 1),
    "alloy_smelter_front": ("alloy_smelter", 1), "alloy_smelter_front_on": ("alloy_smelter", 1),
    "pulverizer_front": ("pulverizer", 1), "pulverizer_front_on": ("pulverizer", 2),
    "fluid_generator_front": ("fluid_generator", 1), "fluid_generator_front_on": ("fluid_generator", 1),
}


def edges(tile):
    oy = 16 * tile
    dark = [[x, 4 + oy] for x in range(4, 12)] + [[c, y + oy] for y in range(5, 12) for c in (4, 11)]
    light = [[12, y + oy] for y in range(4, 13)] + [[x, 12 + oy] for x in range(4, 12)]
    return dark, light


if __name__ == "__main__":
    for name, (machine, tiles) in FRONTS.items():
        shadow, light = ACCENTS[machine]
        steps = []
        for tile in range(tiles):
            dark_px, light_px = edges(tile)
            steps.append({"id": "draw.pixels", "params": {"pixels": dark_px, "color": shadow, "onlyOn": BEVEL_DARK}})
            steps.append({"id": "draw.pixels", "params": {"pixels": light_px, "color": light, "onlyOn": BEVEL_LIGHT}})
        recipe = {"name": f"{name}.accent", "description": f"Moldura da janela de {name} na cor da máquina "
                  "(gen_front_accents.py).", "steps": steps}
        (Path(__file__).parent / f"{name}.accent.recipe.json").write_text(json.dumps(recipe, indent=1) + "\n")
        print(name)
