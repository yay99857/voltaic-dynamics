# ⚡ Voltaic Dynamics

Energy and technology mod for **Minecraft 1.21.1 (NeoForge)**, built for **Bubie Pack** (and any 1.21.1 NeoForge pack). Everything speaks Forge Energy (FE), so it works with **Mekanism**, **Applied Energistics 2**, **Powah**, **Extreme Reactors** and any other FE mod.

---

## 🚀 Installation (Bubie Pack or any 1.21.1 pack)

1. Build the jar (`./gradlew build`). It lands in `build/libs/voltaicdynamics-<version>.jar`.
2. Open your instance folder (CurseForge, Prism Launcher or Modrinth).
3. Drop the jar into `mods/` and launch. The mod has its own creative tab; recipes show up in JEI/EMI.

Server owners can tune every rate and capacity in `world/serverconfig/voltaicdynamics-server.toml` (synced to players).

---

## 🔋 Content

### 1. 🌡️ Thermoelectric Generator
* **Passive generation** from the Seebeck effect: it needs a hot side and a cold side.
* **Heat:** Lava `+20`, Fire / Campfire `+15`, Magma Block `+12`.
* **Cold:** Blue Ice `+25`, Packed Ice `+16`, Powder Snow `+14`, Ice `+10`, Snow Block `+8`, Snow `+6`, Water `+5`, snowy biome `+8`.
* **Output:** `(heat × cold) / 15` FE/t, capped at **100 FE/t** (configurable).
* **Wear:** about once an hour per block, a source degrades: lava cools to obsidian, magma turns to netherrack, ice steps down and melts, snow disappears. Set `wearChance = 0` for the old infinite behaviour.
* 50,000 FE buffer, pushes to neighbours. Right-click shows the current rate and buffer.

### 2. 🔥 Combustion Generator
* Burns any furnace fuel for **40 FE/t**, 100,000 FE buffer, 400 FE/t output.
* **Lava buckets** leave the empty bucket in the slot, like a vanilla furnace.
* **Pauses when full** instead of wasting fuel, and only starts a new item when there is room.
* Pipes and hoppers can insert fuel but cannot pull it out (they can take the empty bucket).
* GUI with burn meter and an energy gauge whose tooltip shows the real generation rate.

### 3. 🔥 Electric Furnace (new)
* Smelts **any vanilla furnace recipe** using **20 FE/t**, **5 s per item** (twice as fast as a furnace).
* Without power it pauses and keeps its progress. Taking the output gives the recipe's XP.
* Pipes insert into the input and extract only from the output. Comparator reads the input stack.

### 4. 🔋 Accumulators
| | Capacity | I/O |
|---|---|---|
| Basic | 1,000,000 FE | 2,000 FE/t |
| Advanced | 8,000,000 FE | 10,000 FE/t |

* **Keep their charge when broken**: the item shows a charge bar and restores the energy when placed again. Charged cells can also be charged or emptied in another cell's slots.
* Charge slot and discharge slot for items. Pipes only take items out once they are full (charge slot) or empty (discharge slot).
* **Push only into consumers** (machines and cables), never into other batteries, so cells next to each other no longer bounce energy back and forth.
* **Comparator output** proportional to the stored energy.

### 5. 🔌 Voltaic Cables
* Connected cables form one **network** that routes energy in the same tick, with no per-cable buffer and no per-cable ticking.
* **2,000 FE/t per connection** (configurable).
* **Routing:** machines first, split evenly; batteries get what is left, and only when the energy comes from a generator. Energy never goes back to the block that sent it, and batteries never charge each other through cables.
* Pulls from generators that do not push on their own.
* Visual connection to any block with the NeoForge `Capabilities.EnergyStorage.BLOCK` capability.
* 8 cables per craft (Voltaic Ingots + Redstone).

### 6. 🔋 Pocket Accumulator
* 100,000 FE personal bank with a charge bar.
* While in your inventory it charges the items **in both hands and the armor you wear**, up to 500 FE/t in total.
* Can be charged or emptied in an Accumulator.
* **🔧 Voltaic Wrench** (iron + Voltaic Ingot): right-click a machine to **turn it** (its side configuration turns with it);
  right-click, sneaking or not, an **item or fluid cable arm** to cycle *deliver* → *pull* → *off*;
  **sneak + right-click** any other Voltaic block to **pick it up** straight into your inventory, energy and fluid included.
  It is in `c:tools/wrench`, so other mods treat it as a wrench and other mods' wrenches configure our cables.

### 7. 🔥 Alloy Smelter (new)
* Three input slots and one output, **30 FE/t**, same rules as the Electric Furnace (pauses without power, keeps progress, XP, comparator).
* Recipe type `voltaicdynamics:alloying` (any slot order, sized ingredients); packs and other mods can add their own:
  | Inputs | Result | Time |
  |---|---|---|
  | Iron Ingot + Copper Ingot + Redstone | 2 Voltaic Ingot | 5 s |
  | Iron Dust + Copper Dust + Redstone *(when a mod adds the dusts)* | 3 Voltaic Ingot | 4 s |
  | 2 Voltaic Dust | 3 Voltaic Ingot | 3 s |
  | Voltaic Ingot + Gold Ingot + Redstone | 2 Galvanic Alloy | 6 s |
  | Voltaic Dust + Gold Dust + Redstone *(when a mod adds gold dust)* | 3 Galvanic Alloy | 5 s |
  | 2 Galvanic Dust | 3 Galvanic Alloy | 3 s |
* Crafted from iron, copper, a furnace, a Voltaic Ingot and redstone. It needs **no Galvanic Alloy**, the thing it makes.
* Pipes never put the same item in two input slots, so a stream of iron cannot block the recipe.
* JEI and EMI category with the "+" transfer, Jade progress. `timeMultiplier` in the config speeds every recipe up or down.

### 8. 🛢️ Fluid Tank (new)
* **32,000 mB** of one fluid (configurable). Keeps the fluid when broken; the item shows it and works as a fluid container.
* Right-click with a bucket, or put containers in the GUI's top slot (filled ones are emptied, empty ones are filled).
* The fluid level shows through the glass; comparator reads the fill.

### 9. 📦 Item Cable and 💧 Fluid Cable (new)
* Look and work like Mekanism transporters / AE2 cables: thin core, arms to other cables, a **connector plate** on every machine.
* **Right-click an arm with the Voltaic Wrench** (or any `c:tools/wrench`), or **sneak + right-click it with an empty hand**: *deliver* → *pull* (orange plate) → *off*. Off between two cables splits the network.
* Pulling connectors move **16 items every 0.5 s** or **1,000 mB/t** into the delivering connectors, **round-robin**.
* Machines and hoppers can also push straight into a connector (auto-eject works with cables).
* **No lag by design:** no items in transit (nothing to tick, sync or render), cables never tick, one network tick per group,
  cached capabilities, bounded slot scans and staggered item operations. All rates are in the server config.

### 10. 🧭 Side configuration (new, every machine with a GUI)
* A tab on the right of the GUI, like Mekanism's: one page per type (**items, fluids, energy**) and the six faces as an unfolded cube
  (top / left · front · right / bottom · back).
* Left-click a face for the next mode, right-click for the previous: **input** (red), **output** (blue), **input + output** (purple), **off** (grey).
  An off face exposes nothing, so cables and pipes do not even connect to it.
* **Auto-eject** pushes items, fluid or energy out of the output faces on its own.
* Electric Furnace, Alloy Smelter, Combustion Generator, Accumulators and Fluid Tank. Faces are named from the front, so the setup
  survives turning the machine; blocks without a front (Accumulators, Tank) use compass directions.

### 11. ⚙️ Materials
* **Voltaic Ingot:** in the Alloy Smelter from ingots or dusts (above), or by hand: `Iron Dust + Copper Dust + Redstone` → 2 Voltaic Dust,
  smelted or blasted into ingots. Packs **without** an ore-processing mod also get `Iron Ingot + Copper Ingot + Redstone` → 1 Voltaic Dust,
  so the mod can always be started; that recipe only loads when iron or copper dust does not exist.
* **Galvanic Alloy:** in the Alloy Smelter, or by hand: `Voltaic Ingot + Gold Ingot + Redstone` → 2 Galvanic Dust, smelted or blasted.
  Tier component of the Advanced Accumulator and the Electric Furnace (an alloy like Mekanism's, not an ingot).
* Recipes accept any mod's iron, copper, gold and redstone through the common `c:` tags.
* Tags: `c:ingots/voltaic`, `c:dusts/voltaic`, `c:dusts/galvanic_alloy` (inside `c:ingots` and `c:dusts`) and
  `voltaicdynamics:alloys/galvanic`. The alloy keeps its own tag so it never stands in for another mod's alloy.
* **Thermopile Core:** component for the Thermoelectric Generator.

---

## 🛠️ Development

```bash
./gradlew build              # build the mod jar
./gradlew runClient          # dev client
./gradlew runData            # regenerate tags, loot tables and recipes (src/generated/resources)
./gradlew runGameTestServer  # run the in-world tests; exits non-zero if one fails
```

* **Code is split into modules** under `src/main/java/com/voltaicdynamics`, each with its own registries, capabilities and creative-tab entries in a `Voltaic<Module>` class. Modules depend only on `core`, so any of them can become its own mod later:

  | Package | Contents | Possible future mod |
  |---|---|---|
  | `core` | materials, data components, creative tab, energy storage, side configuration, base menu/screen | Voltaic Core |
  | `generators` | Thermoelectric and Combustion Generators | Voltaic Generators |
  | `machines` | Electric Furnace, Alloy Smelter, alloying recipe type | Voltaic Dynamics |
  | `power` | Accumulators, Voltaic Cables | Voltaic Dynamics |
  | `logistics` | item and fluid cables, Fluid Tank | Voltaic Logistics |
  | `gadgets` | Pocket Accumulator | Voltaic Gadgets |

  `client/ClientModEvents`, `compat`, `datagen` and `gametest` still cover every module.
* **Data is generated:** tags, loot tables and recipes come from `src/main/java/com/voltaicdynamics/datagen`. Edit the providers and run `runData`; don't hand-edit `src/generated`.
* **Game tests** live in `gametest/`, one class per module (32 tests): `CoreGameTests` (mining tags, deferred saves, menu sync, side configuration and auto-eject), `PowerGameTests` (cells keeping energy, no battery ping-pong, cable delivery), `GeneratorGameTests` (lava bucket, fuel extraction, Thermoelectric Generator), `GadgetGameTests` (Voltaic Wrench: turning, picking up, cable arms), `MachineGameTests` (Electric Furnace, recipe cache, recipe set, Alloy Smelter) `LogisticsGameTests` (item and fluid cables, Fluid Tank) and `CompatGameTests` (other mods' blocks: Accumulator and Combustion Generator on Mekanism's Universal Cable, Fluid Cable pulling from and Fluid Tank ejecting into a Mekanism Fluid Tank, vanilla hoppers feeding and emptying the Electric Furnace). Shared helpers are in `GameTestSupport`.
* **Art sources** for the machine fronts, GUIs, tank and cable textures are in `art-src/textures/*.pontilha` (Pontilha).
* Versions live in `gradle.properties`; `neoforge.mods.toml` is filled from it at build time.

---

## 📜 Changelog

### 1.4.0
* New: flat inventory icons for the Fluid Tank and the Voltaic, Item and Fluid Cables.
* New: the Fluid Tank icon shows the fluid it holds, in the fluid's colour, at one of 8 levels; the Jade icon of a placed tank follows its contents live.
* Faster: machines cache their recipe and only search again when the inputs or the loaded recipes change.
* Faster: block entities mark themselves changed once per tick instead of on every energy or item transfer.
* Fixed: machine GUIs now sync full 32-bit values, so energy above 32,767 FE, long fuel burn times and long recipes show correctly.
* New: Voltaic Wrench: turns machines, cycles cable arms, picks Voltaic blocks up with their contents; tagged `c:tools/wrench`.
* Fixed: Accumulators now work on other mods' cable networks (Mekanism's Universal Cable): they used to skip those cables as if they were batteries, and once fed would hand the energy straight back instead of charging.
* Internal: code split into modules (`core`, `generators`, `machines`, `power`, `logistics`, `gadgets`) that only depend on `core`; game tests split per module.

### 1.3.0
* New: Alloy Smelter with the `voltaicdynamics:alloying` recipe type (JEI, EMI, Jade).
* New: Fluid Tank, Item Cable and Fluid Cable (lag-free networks, pull/deliver/off connectors).
* New: Mekanism-style side configuration with auto-eject on every machine GUI.
* Fixed: without an ore-processing mod there was no way to make Voltaic Ingots; ingots now work through the Alloy Smelter and a fallback dust recipe.

### 1.1.0
* Fixed: machines and cells dropped nothing when mined (missing `mineable/pickaxe` tag).
* Fixed: duplicate recipes. Iron + copper + redstone made both dust and ingots, and the same happened for galvanic alloy.
* Fixed: lava buckets were destroyed by the Combustion Generator.
* Fixed: Accumulators lost their energy when broken.
* Fixed: cells and cables bounced energy back and forth between batteries.
* Fixed: pipes could steal fuel from the Combustion Generator.
* New cable network: one network per connected group, cached capabilities, no per-cable ticking.
* New: Electric Furnace, server config, comparator output, Pocket Accumulator charges offhand and armor, Brazilian Portuguese translation, Thermoelectric source wear.
* Item energy is stored in the `voltaicdynamics:energy` data component. Pocket Accumulators from 1.0.0 keep their charge (converted the next time they are in a player's inventory).
* Upgrading a world: the up to 4,000 FE buffered inside each 1.0.0 cable is dropped, since cables no longer store energy.
