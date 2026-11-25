# Voltaic Dynamics

**Compact Forge Energy tech for NeoForge 26.3 (2.x) and 1.21.1 (1.x): generators, machines with upgrades, batteries and cables for energy, items and fluids.**

*🇧🇷 Versão em português mais abaixo.*

Everything speaks Forge Energy (FE), so Voltaic Dynamics plugs into Mekanism, Applied Energistics 2, Powah, Extreme
Reactors and any other FE mod. Built for Bubie Pack, balanced to sit next to Thermal and Mekanism without making them
pointless.

## ⚡ Power
- **Thermoelectric Generator:** free power from heat next to cold (lava beside ice, packed ice, snow...), slowly wearing its blocks.
- **Combustion Generator:** any furnace fuel, 40 FE/t.
- **Fluid Generator:** lava or any fuel a pack adds to the `voltaicdynamics:fluid_fuels` data map, fed by pipes.
- **Accumulators** from 1M to 128M FE (Basic, Advanced, Elite, Ultimate) that keep their charge when broken, a **Creative Accumulator**, and a **Pocket Accumulator** that charges what you hold and wear.

## ⚙️ Machines
- **Electric Furnace**, **Alloy Smelter** (`voltaicdynamics:alloying`) and **Pulverizer** (`voltaicdynamics:crushing`, ores to 2 dusts).
- **Speed and Efficiency Upgrades**, two slots per machine.
- **Tier Installers** raise a machine in place to process 2, 4 or 8 items at once (like Mekanism's factories).
- Mekanism-style **side configuration** with auto-eject on every face, comparator output, XP.

## 📦 Logistics
- **Voltaic Cables** for energy (they glow while energy flows), **Item and Fluid Cables** with pull / deliver / off connectors and no items in transit.
- **Fluid Tanks** from 32,000 to 256,000 mB that keep their fluid when broken and show it on their icon, and a **Creative Fluid Tank**.
- **Voltaic Wrench:** turn machines, configure cable arms, pick blocks up with their contents (`c:tools/wrench`).

## 🧩 Compatibility
- JEI and EMI recipe pages, Jade tooltips, `c:` tags for every material (works with AlmostUnified).
- Recipe types and fuels are data-driven: packs add their own through datapacks.
- Screens are laid out in JSON (`assets/voltaicdynamics/screens`), so resource packs can rearrange them.

---

## 🇧🇷 Em português

**Tecnologia com Forge Energy para NeoForge 26.3 (2.x) e 1.21.1 (1.x): geradores, máquinas com melhorias, baterias e cabos de energia, itens e fluidos.**

Tudo usa Forge Energy (FE), então funciona com Mekanism, Applied Energistics 2, Powah, Extreme Reactors e qualquer mod
de FE. Feito para o Bubie Pack e equilibrado para conviver com a Thermal e o Mekanism.

- **Energia:** Gerador Termoelétrico (calor perto de frio), Gerador a Combustão (qualquer combustível de fornalha),
  Gerador a Fluido (lava ou combustíveis do data map `voltaicdynamics:fluid_fuels`), Acumuladores de 1M a 128M FE,
  o Acumulador Criativo e o Acumulador de Bolso.
- **Máquinas:** Fornalha Elétrica, Fundidora de Ligas e Pulverizador, com Melhorias de Velocidade e Eficiência,
  Instaladores de Nível (2, 4 ou 8 itens ao mesmo tempo) e configuração de lados no estilo do Mekanism.
- **Logística:** Cabos Voltaicos que brilham com energia, Cabos de Itens e de Fluidos sem itens em trânsito, Tanques de
  Fluidos de 32.000 a 256.000 mB, o Tanque Criativo e a Chave Voltaica.
- **Compatibilidade:** JEI, EMI, Jade, tags `c:`, receitas e combustíveis por datapack, telas em JSON.

---

## Publishing checklist (not part of the page)
CurseForge project id: **1809423** (created 2026-11-16, unlisted, MIT, Energy / Energy, Fluid, and Item Transport;
awaiting moderation). Manage it at https://authors.curseforge.com/#/projects/1809423.
First file: "Voltaic Dynamics 1.5.0", fileID **9642198** (Release; 1.21.1, NeoForge, Java 21, client + server).
1.6.0: fileID **9658155** (sent 2026-11-17).
1.7.0: fileID **9731481** (sent 2026-11-21; Modrinth version Y6NkQbZO).
2.0.0 (Minecraft 26.3, branch `mc/26.3`): game version 26.3, NeoForge, Java 25.

1. `./gradlew build` and `./gradlew runGameTestServer` (all tests pass).
2. 1.x: `tools/prepare-pack-server.sh && ./gradlew runPackServer`: the server reaches "Done" with the whole Bubie Pack.
   2.x: fill `runs/compat/mods` with the 26.3 jars and run `./gradlew runCompatGameTestServer`.
3. Bump `mod_version` in `gradle.properties` and the changelog in `README.md`.
4. Upload `build/libs/voltaicdynamics-<version>.jar` (CurseForge / Modrinth), loader NeoForge, game version 1.21.1 (1.x) or 26.3 (2.x),
   environment client + server, license MIT. Logo: `src/main/resources/voltaicdynamics_logo.png`.
5. Add the jar to Bubie Pack's `pack/manifest.json` (projectID 1809423, fileID of the uploaded jar).
