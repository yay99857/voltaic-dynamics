"""Corta art-src/textures/voltaic-textures.png (exportada do Pontilha em escala 1) nos PNGs do mod."""
import json, os
from PIL import Image
here = os.path.dirname(os.path.abspath(__file__))
root = os.path.dirname(os.path.dirname(here))
tex = os.path.join(root, 'src/main/resources/assets/voltaicdynamics/textures')
ITEMS = {'voltaic_ingot', 'voltaic_dust', 'galvanic_alloy', 'galvanic_dust', 'thermopile_core', 'pocket_accumulator'}
sheet = Image.open(os.path.join(here, 'voltaic-textures.png')).convert('RGBA')
for name, x, y in json.load(open(os.path.join(here, 'voltaic-textures.recipe.json')))['layout']:
    tile = sheet.crop((x, y, x + 16, y + 16))
    kind = 'item' if name in ITEMS else 'block'
    if kind == 'block' and not name.startswith('voltaic_cable'):  # o cabo só usa a cruz central
        assert tile.getextrema()[3][0] == 255, f'{name}: bloco com pixel transparente'
    tile.save(os.path.join(tex, kind, name + '.png'))
    print(kind, name)
