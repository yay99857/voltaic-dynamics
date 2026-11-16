"""Pontilha recipes for the Pulverizer front: the machine frame of the Electric Furnace front with two
counter-rotating toothed rollers in the window. The 'on' texture has 2 frames (rollers turning) and a cyan light.

Run:  python3 gen_pulverizer_front.py, then open the base PNG in Pontilha and
      pontilha run pulverizer_front.recipe.json / pulverizer_front_on.recipe.json
"""
import json
from pathlib import Path

HERE = Path(__file__).parent
WINDOW = "#13161b"                           # the black window, as on the other machine fronts
TOOTH = ["#b4bdc9", "#8c96a5"]                # roller columns, lit from the left
GROOVE = ["#4d5562", "#3d4450"]
STATUS = (10, 11)


def rollers(phase):
    """Two 2-px rollers in the 6x6 window (x5..10, y5..10), black around them; teeth one row out of phase."""
    pixels = [[x, y, WINDOW] for y in range(5, 11) for x in range(5, 11)]
    for y in range(6, 10):
        for i, x in enumerate((6, 7)):             # left roller turns down
            groove = (y - phase) % 2 == 0
            pixels.append([x, y, (GROOVE if groove else TOOTH)[i]])
        for i, x in enumerate((8, 9)):             # right roller turns up
            groove = (y + phase) % 2 == 1
            pixels.append([x, y, (GROOVE if groove else TOOTH)[i]])
    # roller ends, curving away into the dark
    for x, c in ((6, "#3d4450"), (7, "#2b313b"), (8, "#3d4450"), (9, "#2b313b")):
        pixels += [[x, 5, c], [x, 10, c]]
    return pixels


def recipe(name, frames, status):
    steps = [{"id": "layer.setProps", "params": {"name": "frente"}}]
    for f in range(frames):
        if f > 0:
            steps.append({"id": "frame.new", "params": {"copy": True, "duration": 100}})
        steps.append({"id": "draw.pixels", "params": {"pixels": rollers(f), "frame": f + 1}})
        steps.append({"id": "draw.pixels", "params": {"pixels": [[*STATUS, status]], "frame": f + 1}})
    return {"name": name, "description": "Frente da Pulverizer sobre a moldura da Electric Furnace "
            "(abra textures/block/electric_furnace_front.png antes). Gerado por gen_pulverizer_front.py.",
            "steps": steps}


if __name__ == "__main__":
    for name, frames, status in (("pulverizer_front", 1, "#5a2027"), ("pulverizer_front_on", 2, "#59dcff")):
        (HERE / f"{name}.recipe.json").write_text(json.dumps(recipe(name, frames, status), indent=1) + "\n")
        print(name)
