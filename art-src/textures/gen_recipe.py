"""Gera voltaic-textures.recipe.json: todas as texturas 16x16 do Voltaic Dynamics numa folha.

Rode `python3 art-src/textures/gen_recipe.py` e depois `pontilha run art-src/textures/voltaic-textures.recipe.json 0.2`
com o Pontilha aberto. Cada tile fica em (coluna*18, linha*18); `slice.py` corta a folha nos PNGs do mod.
Estilo (estudo de Mekanism, Powah, Ender IO e AE2): carcaça de aço com chanfro (luz em cima/esquerda),
painel rebaixado, cor saturada só no que funciona (LED, janela, núcleo), tier pela cor da moldura
(cobre = básico, ouro = avançado) e ciano como a cor de energia do mod.
"""
import json, os

STEEL = dict(k='#13161b', d='#1f242c', e='#2b313b', m='#3d4450', n='#4d5562', l='#687180', h='#8c96a5', w='#b4bdc9')
CYAN = {'1': '#0a2f3c', '2': '#0f6683', '3': '#1fa9d4', '4': '#59dcff', '5': '#c6f6ff'}
COPPER = dict(q='#3b1a10', r='#73361b', s='#a9592a', t='#d6864a', u='#f2bd87')
GOLD = dict(G='#553609', H='#8a5d12', I='#c4911d', J='#eec142', K='#fff0a6')
FIRE = dict(A='#3a0c06', B='#8a2509', D='#d0521a', E='#ff9330', F='#ffdd70')
COLD = dict(P='#1b3a74', Q='#3576c8', S='#96d2ff')
LED_OFF = dict(x='#5a2027')
BASE = {**STEEL, **CYAN, **COPPER, **GOLD, **FIRE, **COLD, **LED_OFF}

# Moldura de máquina: borda k, chanfro (h claro em cima/esquerda, e escuro embaixo/direita),
# painel rebaixado 8x8 em (4,4). "@" marca o miolo, preenchido por cada máquina.
FRAME = [
    'kkkkkkkkkkkkkkkk',
    'khhhhhhhhhhhhhnk',
    'khwnnnnnnnnnwnek',
    'khnddddddddddnek',
    'khnd@@@@@@@@lnek',
    'khnd@@@@@@@@lnek',
    'khnd@@@@@@@@lnek',
    'khnd@@@@@@@@lnek',
    'khnd@@@@@@@@lnek',
    'khnd@@@@@@@@lnek',
    'khnd@@@@@@@@lnek',
    'khnd@@@@@@@@lnek',
    'khndlllllllllnek',
    'khwnnnnnnnnnlnek',
    'kneeeeeeeeeeeeek',
    'kkkkkkkkkkkkkkkk',
]
TIER = {  # moldura dos acumuladores: o chanfro troca de aço para a cor do tier
    'basic': dict(h=COPPER['t'], n=COPPER['s'], e=COPPER['r'], w=COPPER['u']),
    'advanced': dict(h=GOLD['J'], n=GOLD['I'], e=GOLD['H'], w=GOLD['K']),
}

def machine(core):
    assert len(core) == 8 and all(len(r) == 8 for r in core), core
    rows, i = [], 0
    for r in FRAME:
        if '@' in r:
            r = r.replace('@' * 8, core[i]); i += 1
        rows.append(r)
    return rows

TILES = {}
TILES['machine_side'] = machine([  # medidor de energia entre dois condutores de cobre
    'mstmmstm', 'mstkkstm', 'mst23stm', 'mst23stm', 'mst34stm', 'mst34stm', 'mstkkstm', 'mstmmstm'])
TILES['machine_top'] = machine([  # grade de ventilação
    'dkkddkkd', 'mllmmllm', 'mmmmmmmm', 'dkkddkkd', 'mllmmllm', 'mmmmmmmm', 'dkkddkkd', 'mllmmllm'])
TILES['machine_bottom'] = machine([  # chapa com reforço em X
    'nmmmmmmn', 'mnmmmmnm', 'mmnmmnmm', 'mmmddmmm', 'mmmddmmm', 'mmnmmnmm', 'mnmmmmnm', 'nmmmmmmn'])
TILES['thermoelectric_side'] = machine([  # lado frio | termopilha | lado quente
    'QSQkkDFD', 'QSQstDFD', 'PQP34BDB', 'QSQstDFD', 'PQP34BDB', 'QSQstDFD', 'PQP34BDB', 'QSQkkDFD'])
TILES['combustion_front'] = machine([  # fornalha apagada, LED desligado
    'mmmmmmmm', 'mkkkkkkm', 'mkdkkdkm', 'mkdkkdkm', 'mnnnnnnm', 'mmmmmmmm', 'mkkmmkkm', 'mmmmmmxm'])
TILES['combustion_front_on'] = machine([  # fogo atrás da grelha, LED aceso
    'mmmmmmmm', 'mBDEEDBm', 'mEdFFdEm', 'mDdEEdDm', 'mBBBBBBm', 'mmmmmmmm', 'mkkmmkkm', 'mmmmmm4m'])
TILES['electric_furnace_front'] = machine([  # três resistências, frias
    'mmmmmmmm', 'mkkkkkkm', 'mksrrskm', 'mkkkkkkm', 'mksrrskm', 'mkkkkkkm', 'mksrrskm', 'mnnnnnxm'])
TILES['electric_furnace_front_on'] = machine([  # resistências incandescentes
    'mmmmmmmm', 'mAAAAAAm', 'mAEFFEAm', 'mABBBBAm', 'mAEFFEAm', 'mABBBBAm', 'mAEFFEAm', 'mBBBBB4m'])
CELL = ['kkkkkkkk', 'k234432k', 'kkkkkkkk', 'k234432k', 'kkkkkkkk', 'k234432k', 'kkkkkkkk', 'k234432k']
TERMINAL = ['mmmmmmmm', 'mmkkkkmm', 'mk2442km', 'mk4554km', 'mk4554km', 'mk2442km', 'mmkkkkmm', 'mmmmmmmm']
for tier in ('basic', 'advanced'):
    TILES[f'{tier}_accumulator_side'] = (machine(CELL), TIER[tier])
    TILES[f'{tier}_accumulator_top'] = (machine(TERMINAL), TIER[tier])
# Cabo: braços (tiras 6 px) com isolamento de aço e condutor ciano; centro = ponta com anel de cobre.
ARM_V = 'dl34nd'
CAP = ['deeeed', 'esttse', 'et45te', 'et54te', 'esttse', 'deeeed']
cable = [['.'] * 16 for _ in range(16)]
for y in range(16):
    for x in range(16):
        inV, inH = 5 <= x <= 10, 5 <= y <= 10
        if inV and inH: cable[y][x] = CAP[y - 5][x - 5]
        elif inV: cable[y][x] = 'rsttsr'[x - 5] if y in (2, 13) else ARM_V[x - 5]
        elif inH: cable[y][x] = 'rsttsr'[y - 5] if x in (2, 13) else ARM_V[y - 5]
TILES['voltaic_cable'] = [''.join(r) for r in cable]
TILES['voltaic_cable_core'] = TILES['voltaic_cable']

# Lingotes e pós no padrão do pack (All the Ores, Mekanism, Ender IO): lingote diagonal do vanilla e monte de pó,
# sem contorno preto (a borda é o tom mais escuro da própria cor), 6 tons por material, luz de cima à esquerda.
# A forma é a convenção do vanilla; o sombreado é gerado aqui.
VOLTAIC_RAMP = ['#5a1414', '#8c2626', '#b8423a', '#d4664e', '#ea8f6c', '#ffc49e']   # vermelho-salmão (cobre + redstone)
GALVANIC_RAMP = ['#5c3008', '#94560e', '#c7831a', '#e6a92c', '#f6cf5a', '#fff0a8']  # âmbar (voltaico + ouro)
def ramp_legend(ramp): return {str(i): c for i, c in enumerate(ramp)}

def _side(p, a, b):  # >0 à esquerda de a->b (y para baixo)
    return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
def ingot_rows():
    L, T, R, N = (0, 6), (11, 2), (15, 6), (4, 9)        # cantos da face de cima
    D = 4                                                 # altura das faces laterais
    def face(x, y):
        c = (x + 0.5, y + 0.5)
        top = _side(c, L, T) >= 0 and _side(c, T, R) >= 0 and _side(c, R, N) >= 0 and _side(c, N, L) >= 0
        if top: return 'top'
        if 0 <= c[0] <= N[0] + 0.5 and _side(c, N, L) < 0 and _side(c, (N[0], N[1] + D), (L[0], L[1] + D)) > 0: return 'left'
        if N[0] - 0.5 <= c[0] <= R[0] + 1 and _side(c, R, N) < 0 and _side(c, (R[0], R[1] + D), (N[0], N[1] + D)) > 0: return 'right'
        return None
    F = [[face(x, y) for x in range(16)] for y in range(16)]
    inside = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and F[y][x] is not None
    rows = []
    for y in range(16):
        r = ''
        for x in range(16):
            f = F[y][x]
            if f is None: r += '.'; continue
            if f == 'top':
                v = 4
                if F[y + 1][x] in ('left', 'right') or (y + 1 < 16 and F[y + 1][x] is None): v = 5   # aresta da frente iluminada
                if not inside(x, y - 1) or not inside(x - 1, y): v = 3                                 # borda de cima
            elif f == 'left':
                v = 3 if x < N[0] else 4
                if not inside(x, y + 1): v = 1
                elif not inside(x - 1, y): v = 2
            else:
                v = 2
                if x == N[0]: v = 3
                if not inside(x, y + 1) or not inside(x + 1, y): v = 0
                elif ((x * 7 + y * 3) % 5) == 0: v = 1                                                  # textura de metal
            r += str(v)
        rows.append(r)
    return rows

DUST_MASK = ['................', '................', '................', '.......##.......', '......####......',
             '.....######.....', '....########....', '...##########...', '..############..', '..############..',
             '..############..', '...##########...', '....########....', '......####......', '................',
             '................']
def dust_rows():
    inside = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and DUST_MASK[y][x] == '#'
    rows = []
    for y in range(16):
        r = ''
        for x in range(16):
            if not inside(x, y): r += '.'; continue
            if not inside(x + 1, y) or not inside(x, y + 1): r += '0'; continue      # borda de baixo/direita
            if not inside(x - 1, y) or not inside(x, y - 1): r += '1'; continue      # borda de cima/esquerda
            lum = 5.1 - 0.13 * (x - 2) - 0.2 * (y - 3)                             # luz de cima à esquerda
            h = ((x * 73856093) ^ (y * 19349663) ^ 0x2545f491) & 0xff
            lum += 0.55 if (x + y) % 2 == 0 else -0.45                             # grãos quase em xadrez
            lum += (h % 5 - 2) * 0.38                                              # e em grupinhos
            r += str(max(2, min(5, int(round(lum)))))
        rows.append(r)
    return rows

def alloy_rows():
    """Liga em cubo isométrico (12 px, inclinação 2:1): topo claro, face esquerda média, face direita escura,
    aresta central clara e um raio gravado na frente. 0 = mais escuro ... 5 = brilho."""
    g = [['.'] * 16 for _ in range(16)]
    for c in range(2, 14):
        k = (c - 2) // 2 if c <= 7 else (13 - c) // 2       # 0, 1, 2 do canto para o meio
        top, mid = 4 - k, 5 + k                              # borda de cima e aresta topo/lado
        for y in range(top, mid): g[y][c] = '5' if y == top and c <= 7 else '4'
        for y in range(mid, mid + 6):
            last = y == mid + 5
            if c <= 7: g[y][c] = '2' if last else ('4' if c == 7 else '3')
            else: g[y][c] = '0' if last or c == 13 else ('1' if c == 8 else '2')
    for x, y in ((5, 7), (4, 8), (5, 8), (4, 9)): g[y][x] = '1'   # raio gravado na face da frente
    return [''.join(r) for r in g]

INGOT = ingot_rows()
DUST = dust_rows()
TILES['voltaic_ingot'] = (INGOT, ramp_legend(VOLTAIC_RAMP))
TILES['galvanic_alloy'] = (alloy_rows(), ramp_legend(GALVANIC_RAMP))
TILES['voltaic_dust'] = (DUST, ramp_legend(VOLTAIC_RAMP))
TILES['galvanic_dust'] = (DUST, ramp_legend(GALVANIC_RAMP))
TILES['thermopile_core'] = ([
    '................', '................', '................', '...kkkkkkkkkk...',
    '..khhhhhhhhhhk..', '..kQSttttttEDk..', '..kPQddddddDBk..', '..kQSttttttEDk..',
    '..kPQddddddDBk..', '..kQSttttttEDk..', '..kPQddddddDBk..', '..keeeeeeeeeek..',
    '...kkkkkkkkkk...', '....s......s....', '....r......r....', '................'], {})
TILES['pocket_accumulator'] = ([
    '................', '......kkkk......', '......ktsk......', '....kkkkkkkk....',
    '....khllllnk....', '....khkkkknk....', '....khk45knk....', '....khk34knk....',
    '....khk34knk....', '....khk23knk....', '....khk23knk....', '....khkkkknk....',
    '....khnnnnek....', '....ktssssrk....', '....kkkkkkkk....', '................'], {})

LAYOUT = [  # (nome, coluna, linha, camada)
    ('machine_side', 0, 0), ('machine_top', 1, 0), ('machine_bottom', 2, 0), ('thermoelectric_side', 3, 0),
    ('combustion_front', 4, 0), ('combustion_front_on', 5, 0),
    ('electric_furnace_front', 0, 1), ('electric_furnace_front_on', 1, 1), ('basic_accumulator_side', 2, 1),
    ('basic_accumulator_top', 3, 1), ('advanced_accumulator_side', 4, 1), ('advanced_accumulator_top', 5, 1),
    ('voltaic_cable', 0, 2), ('voltaic_cable_core', 1, 2), ('voltaic_ingot', 2, 2), ('voltaic_dust', 3, 2),
    ('galvanic_alloy', 4, 2), ('galvanic_dust', 5, 2), ('thermopile_core', 0, 3), ('pocket_accumulator', 1, 3),
]
ITEMS = {'voltaic_ingot', 'voltaic_dust', 'galvanic_alloy', 'galvanic_dust', 'thermopile_core', 'pocket_accumulator'}

def legend_for(rows, over):
    used = {c for r in rows for c in r if c not in '. '}
    leg = {c: over.get(c, BASE.get(c)) for c in sorted(used)}
    missing = [c for c, v in leg.items() if v is None]
    assert not missing, missing
    return leg

steps = [
    {'id': 'doc.new', 'params': {'width': 6 * 18 - 2, 'height': 4 * 18 - 2, 'name': 'voltaic-textures'}},
    {'id': 'palette.set', 'params': {'colors': sorted(set(BASE.values()))}},
    {'id': 'layer.setProps', 'params': {'name': 'maquinas'}},
]
made_items_layer = False
for name, col, row in LAYOUT:
    t = TILES[name]
    rows, over = (t if isinstance(t, tuple) else (t, {}))
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), (name, [len(r) for r in rows])
    if name in ITEMS and not made_items_layer:
        steps.append({'id': 'layer.new', 'params': {'name': 'itens'}}); made_items_layer = True
    steps.append({'id': 'draw.pattern', 'params': {'x': col * 18, 'y': row * 18, 'rows': rows, 'legend': legend_for(rows, over)}})

here = os.path.dirname(os.path.abspath(__file__))
json.dump({'name': 'Voltaic Dynamics textures', 'layout': [[n, c * 18, r * 18] for n, c, r in LAYOUT], 'steps': steps},
          open(os.path.join(here, 'voltaic-textures.recipe.json'), 'w'), indent=1)
print(len(steps), 'passos,', len(LAYOUT), 'tiles')
