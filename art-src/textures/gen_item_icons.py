"""Writes the Pontilha recipes for the flat 16x16 inventory icons of the cables and the Fluid Tank.

Run:  python3 gen_item_icons.py   then   pontilha run item_icon_<name>.recipe.json 0.15
Each recipe builds the icon layer by layer in the open window (shape, shading, core, outline).

Cables are drawn as one diagonal segment (bottom-left to top-right) with a connector flange at each
end. Positions use u = x - y (along the cable) and v = x + y - 15 (across it, negative = top-left,
the lit side).
"""
import json
from pathlib import Path

HERE = Path(__file__).parent
OUTLINE = "#16181c"
FILL_LEVELS = 8  # one per row of glass, so 1/32 of a tank shows as one row

CABLES = {
    # name: casing ramp (light..dark), core (base, bright, glint), flange ramp (light..dark), ring
    "item_cable": dict(casing=["#7d8796", "#4d5562", "#3f4753", "#2b313b"],
                       core=["#db7b2a", "#f6b847", "#ffe6a0", "#9c4a22"],
                       flange=["#b4bdc9", "#8c96a5", "#687180"], ring="#db7b2a"),
    "fluid_cable": dict(casing=["#7d8796", "#4d5562", "#3f4753", "#2b313b"],
                        core=["#2f6fd0", "#4fb0f0", "#a4e4ff", "#1f3e8c"],
                        flange=["#b4bdc9", "#8c96a5", "#687180"], ring="#2f6fd0"),
    "voltaic_cable": dict(casing=["#7d8796", "#4d5562", "#3f4753", "#2b313b"],
                          core=["#1fa9d4", "#59dcff", "#c6f6ff", "#0f6683"],
                          flange=["#d6864a", "#a9592a", "#73361b"], ring="#59dcff"),
}


def cable_pixels(spec):
    """A 7-wide casing around a 3-wide glowing core, metal end caps at both ends."""
    body, core, glints, flange, ring = [], [], [], [], []
    for y in range(16):
        for x in range(16):
            u, v = x - y, x + y - 15
            if abs(u) <= 6 and abs(v) <= 3:
                if abs(v) <= 1:
                    tone = {-1: 1, 0: 0, 1: 3}[v]
                    core.append([x, y, spec["core"][tone]])
                    if v == -1 and u in (-4, 2):
                        glints.append([x, y, spec["core"][2]])
                else:
                    shade = {-3: 0, -2: 1, 2: 2, 3: 3}[v]
                    if abs(u) == 3:  # armour joints
                        shade = min(shade + 1, 3)
                    body.append([x, y, spec["casing"][shade]])
            elif 7 <= abs(u) <= 9 and abs(v) <= 3:
                if abs(u) == 7 and abs(v) <= 3:
                    ring.append([x, y, spec["ring"]])
                else:
                    shade = 0 if v <= -2 else (1 if v <= 1 else 2)
                    flange.append([x, y, spec["flange"][shade]])
    return body, core, glints, flange, ring


def cable_recipe(name, spec):
    body, core, glints, flange, ring = cable_pixels(spec)
    return {
        "name": f"Ícone {name}",
        "description": f"Ícone 16x16 de inventário do {name} (gerado por gen_item_icons.py).",
        "steps": [
            {"id": "doc.new", "params": {"width": 16, "height": 16, "name": name, "discard": True}},
            {"id": "layer.setProps", "params": {"name": "cabo"}},
            {"id": "draw.pixels", "params": {"pixels": body}},
            {"id": "draw.pixels", "params": {"pixels": flange}},
            {"id": "draw.pixels", "params": {"pixels": core}},
            {"id": "draw.pixels", "params": {"pixels": ring}},
            {"id": "draw.pixels", "params": {"pixels": glints}},
            {"id": "effect.outline", "params": {"color": OUTLINE}},
        ],
    }


def tank_recipe():
    metal_light, metal, metal_dark = "#b4bdc9", "#8c96a5", "#4d5562"
    trim, trim_light, trim_dark = "#db7b2a", "#f6b847", "#9c4a22"
    glass, glass_glint = "#1f242c", "#d8f0ff"
    frame, caps, inside = [], [], []
    for x in range(3, 13):
        # caps, rounded at the outer corners
        if x not in (3, 12):
            caps.append([x, 1, metal_light])
            caps.append([x, 14, metal_dark])
        caps.append([x, 2, metal_light if x < 11 else metal])
        caps.append([x, 3, trim_light if x < 6 else (trim if x < 11 else trim_dark)])
        caps.append([x, 12, trim if x < 11 else trim_dark])
        caps.append([x, 13, metal if x < 11 else metal_dark])
    for y in range(4, 12):
        frame.append([3, y, metal])
        frame.append([12, y, metal_dark])
        for x in range(4, 12):
            inside.append([x, y, glass])
    # empty glass: a reflection down the left side, brighter at the top
    glints = [[5, y, glass_glint if y <= 5 else "#687180"] for y in range(4, 11)] + [[6, 4, "#4d5562"]]

    steps = [
        {"id": "doc.new", "params": {"width": 16, "height": 16, "name": "fluid_tank", "discard": True}},
        {"id": "layer.setProps", "params": {"name": "tanque"}},
        {"id": "draw.pixels", "params": {"pixels": caps}},
        {"id": "draw.pixels", "params": {"pixels": frame}},
        {"id": "draw.pixels", "params": {"pixels": inside}},
        {"id": "draw.pixels", "params": {"pixels": glints}},
        {"id": "effect.outline", "params": {"color": OUTLINE}},
    ]
    # Fluid overlays, one layer per fill level. Grey, because the game multiplies them by the
    # fluid's colour: white = the fluid colour itself (surface and reflection), darker = shade.
    for level in range(1, FILL_LEVELS + 1):
        rows = level * 8 // FILL_LEVELS
        top = 12 - rows
        pixels = []
        for y in range(top, 12):
            for x in range(4, 12):
                if y == top:
                    tone = "#ffffff"
                elif x == 5:
                    tone = "#ffffff"
                elif x >= 10:
                    tone = "#8a8a8a"
                else:
                    tone = "#c4c4c4"
                pixels.append([x, y, tone])
        steps += [
            {"id": "layer.new", "params": {"name": f"fluido_{level}"}},
            {"id": "draw.pixels", "params": {"pixels": pixels}},
            {"id": "layer.setProps", "params": {"layer": f"fluido_{level}", "visible": level == FILL_LEVELS}},
        ]
    return {
        "name": "Ícone fluid_tank",
        "description": "Ícone 16x16 do Fluid Tank: camada tanque (vazio) e fluido_1..8 (níveis, "
                       "tingidos pela cor do fluido no jogo). Gerado por gen_item_icons.py.",
        "steps": steps,
    }


def wrench_recipe():
    """Diagonal open-end wrench: steel head top-right with the jaw opening at the tip, Voltaic cyan grip."""
    steel = {-4: "#d8dee6", -3: "#d8dee6", -2: "#b4bdc9", -1: "#b4bdc9", 0: "#8c96a5", 1: "#687180", 2: "#4d5562",
             3: "#3f4753", 4: "#3f4753"}
    grip = {-1: "#59dcff", 0: "#1fa9d4", 1: "#0f6683"}
    handle, head, grips, glints = [], [], [], []
    for y in range(16):
        for x in range(16):
            u, v = x - y, x + y - 15
            if 2 <= u <= 9 and abs(v) <= 4:
                if u >= 5 and abs(v) <= 2:
                    continue  # the jaw opening
                if (u == 2 or u == 9) and abs(v) == 4:
                    continue  # round off the corners
                head.append([x, y, steel[v]])
            elif -9 <= u <= 2 and abs(v) <= 1:
                if u <= -3:
                    grips.append([x, y, grip[v]])
                else:
                    handle.append([x, y, steel[v]])
    glints = [[x, y, "#ffffff"] for y in range(16) for x in range(16) if (x - y, x + y - 15) in ((3, -3), (7, -4), (-7, -1))]
    return {
        "name": "Ícone voltaic_wrench",
        "description": "Ícone 16x16 da Chave Voltaica (gerado por gen_item_icons.py).",
        "steps": [
            {"id": "doc.new", "params": {"width": 16, "height": 16, "name": "voltaic_wrench", "discard": True}},
            {"id": "layer.setProps", "params": {"name": "chave"}},
            {"id": "draw.pixels", "params": {"pixels": handle}},
            {"id": "draw.pixels", "params": {"pixels": head}},
            {"id": "draw.pixels", "params": {"pixels": grips}},
            {"id": "draw.pixels", "params": {"pixels": glints}},
            {"id": "effect.outline", "params": {"color": OUTLINE}},
        ],
    }


if __name__ == "__main__":
    recipes = {name: cable_recipe(name, spec) for name, spec in CABLES.items()}
    recipes["fluid_tank"] = tank_recipe()
    recipes["voltaic_wrench"] = wrench_recipe()
    for name, recipe in recipes.items():
        path = HERE / f"item_icon_{name}.recipe.json"
        path.write_text(json.dumps(recipe, indent=1) + "\n")
        print(path)
