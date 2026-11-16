"""Pontilha recipes for the Fluid Generator front: the machine frame (open combustion_front.png first) with a glass
fuel capsule in the window: dim lava when off, glowing lava when on. The bevel is then painted purple by
gen_front_accents.py.
"""
import json
from pathlib import Path

WINDOW = "#13161b"


def window(lit):
    px = [[x, y, WINDOW] for y in range(5, 11) for x in range(5, 11)]
    for y in range(5, 11):
        px += [[6, y, "#687180"], [9, y, "#3d4450"]]             # glass walls, lit from the left
    for y in range(5, 11):
        for x in (7, 8):
            if lit:
                c = "#f6b847" if y == 6 else ("#db7b2a" if y < 9 else "#9c4a22")
                if y == 5:
                    c = "#2b313b"
            else:
                c = "#2b313b" if y < 9 else "#73361b"
            px.append([x, y, c])
    if lit:
        px += [[7, 8, "#ffe6a0"], [8, 7, "#ffe6a0"]]             # bubbles
    px.append([10, 11, "#59dcff" if lit else "#5a2027"])        # status light
    return px


if __name__ == "__main__":
    for name, lit in (("fluid_generator_front", False), ("fluid_generator_front_on", True)):
        recipe = {"name": name, "description": "Frente do Fluid Generator (gen_fluid_generator_front.py); abra "
                  "combustion_front.png antes.",
                  "steps": [{"id": "draw.pixels", "params": {"pixels": window(lit)}}]}
        (Path(__file__).parent / f"{name}.recipe.json").write_text(json.dumps(recipe, indent=1) + "\n")
        print(name)
