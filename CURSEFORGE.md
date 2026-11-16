# Voltaic Dynamics

**Compact Forge Energy tech for NeoForge 1.21.1: generators, machines with upgrades, batteries and lag-free cables.**

*🇧🇷 Versão em português mais abaixo.*

Everything speaks Forge Energy (FE), so Voltaic Dynamics plugs into Mekanism, Applied Energistics 2, Powah, Extreme
Reactors and any other FE mod. Built for Bubie Pack, balanced to sit next to Thermal and Mekanism without making them
pointless.

## ⚡ Power
- **Thermoelectric Generator:** free power from heat next to cold (lava beside ice, packed ice, snow...), slowly wearing its blocks.
- **Combustion Generator:** any furnace fuel, 40 FE/t.
- **Fluid Generator:** lava or any fuel a pack adds to the `voltaicdynamics:fluid_fuels` data map, fed by pipes.
- **Accumulators** (1M and 8M FE) that keep their charge when broken, and a **Pocket Accumulator** that charges what you hold and wear.

## ⚙️ Machines
- **Electric Furnace**, **Alloy Smelter** (`voltaicdynamics:alloying`) and **Pulverizer** (`voltaicdynamics:crushing`, ores to 2 dusts).
- **Speed and Efficiency Upgrades**, two slots per machine.
- Mekanism-style **side configuration** with auto-eject on every face, comparator output, XP.

## 📦 Logistics
- **Voltaic Cables** for energy, **Item and Fluid Cables** with pull / deliver / off connectors and no items in transit (no lag).
- **Fluid Tank** that keeps its fluid when broken and shows it on its icon.
- **Voltaic Wrench:** turn machines, configure cable arms, pick blocks up with their contents (`c:tools/wrench`).

## 🧩 Compatibility
- JEI and EMI recipe pages, Jade tooltips, `c:` tags for every material (works with AlmostUnified).
- Recipe types and fuels are data-driven: packs add their own through datapacks.
- Screens are laid out in JSON (`assets/voltaicdynamics/screens`), so resource packs can rearrange them.

---

## 🇧🇷 Em português

**Tecnologia com Forge Energy para NeoForge 1.21.1: geradores, máquinas com melhorias, baterias e cabos sem lag.**

Tudo usa Forge Energy (FE), então funciona com Mekanism, Applied Energistics 2, Powah, Extreme Reactors e qualquer mod
de FE. Feito para o Bubie Pack e equilibrado para conviver com a Thermal e o Mekanism.

- **Energia:** Gerador Termoelétrico (calor perto de frio), Gerador a Combustão (qualquer combustível de fornalha),
  Gerador a Fluido (lava ou combustíveis do data map `voltaicdynamics:fluid_fuels`), Acumuladores de 1M e 8M FE e o
  Acumulador de Bolso.
- **Máquinas:** Fornalha Elétrica, Fundidora de Ligas e Pulverizador, com Melhorias de Velocidade e Eficiência e
  configuração de lados no estilo do Mekanism.
- **Logística:** Cabos Voltaicos, Cabos de Itens e de Fluidos sem itens em trânsito, Tanque de Fluidos e a Chave Voltaica.
- **Compatibilidade:** JEI, EMI, Jade, tags `c:`, receitas e combustíveis por datapack, telas em JSON.

---

## Publishing checklist (not part of the page)
CurseForge project id: **1809423** (created 2026-11-16, unlisted, MIT, Energy / Energy, Fluid, and Item Transport;
awaiting moderation). Manage it at https://authors.curseforge.com/#/projects/1809423.

1. `./gradlew build` and `./gradlew runGameTestServer` (all tests pass).
2. `tools/prepare-pack-server.sh && ./gradlew runPackServer`: the server reaches "Done" with the whole Bubie Pack.
3. Bump `mod_version` in `gradle.properties` and the changelog in `README.md`.
4. Upload `build/libs/voltaicdynamics-<version>.jar` (CurseForge / Modrinth), loader NeoForge, game version 1.21.1,
   environment client + server, license MIT. Logo: `src/main/resources/voltaicdynamics_logo.png`.
5. Add the jar to Bubie Pack's `pack/manifest.json` (projectID 1809423, fileID of the uploaded jar).
