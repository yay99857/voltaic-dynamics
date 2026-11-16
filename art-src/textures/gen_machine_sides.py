"""Pontilha recipes for the per-machine side textures: machine_side.png with its copper bars recoloured to the
machine's accent colour, so machines tell apart at a glance in the inventory (the side is the biggest face of
the item icon). The Electric Furnace keeps the copper bars.

Open textures/block/machine_side.png in Pontilha, then  pontilha run <machine>_side.recipe.json
"""
import json
from pathlib import Path

COPPER = ["#a9592a", "#d6864a"]  # bar shadow, bar light
ACCENTS = {
    "combustion_generator": ["#9c2a2a", "#e0573f"],  # ember red
    "alloy_smelter": ["#9c8418", "#e8cf3a"],         # galvanic gold
    "pulverizer": ["#2f7d4a", "#6fbf5a"],            # mineral green
}

if __name__ == "__main__":
    for machine, ramp in ACCENTS.items():
        steps = [{"id": "color.replaceAll", "params": {"from": a, "to": b}} for a, b in zip(COPPER, ramp)]
        recipe = {"name": f"{machine}_side", "description": f"Lateral do {machine}: machine_side com as barras "
                  "na cor da máquina (gen_machine_sides.py).", "steps": steps}
        (Path(__file__).parent / f"{machine}_side.recipe.json").write_text(json.dumps(recipe, indent=1) + "\n")
        print(machine)
