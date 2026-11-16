# ⚡ Voltaic Dynamics

Energy and technology mod for **Minecraft 1.21.1 (NeoForge)**, built for **All The Mods 11**. Everything speaks Forge Energy (FE), so it works with **Mekanism**, **Applied Energistics 2**, **Powah**, **Extreme Reactors** and any other FE mod.

---

## 🚀 Installation (All The Mods 11)

1. Build the jar (`./gradlew build`). It lands in `build/libs/voltaicdynamics-<version>.jar`.
2. Open your **All The Mods 11** instance folder (CurseForge, Prism Launcher or Modrinth).
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

### 4. 🔋 Energy Cells
| | Capacity | I/O |
|---|---|---|
| Basic | 1,000,000 FE | 2,000 FE/t |
| Advanced | 8,000,000 FE | 10,000 FE/t |

* **Keep their charge when broken**: the item shows a charge bar and restores the energy when placed again. Charged cells can also be charged or emptied in another cell's slots.
* Charge slot and discharge slot for items. Pipes only take items out once they are full (charge slot) or empty (discharge slot).
* **Push only into consumers** (machines and cables), never into other batteries, so cells next to each other no longer bounce energy back and forth.
* **Comparator output** proportional to the stored energy.

### 5. 🔌 Energy Cables
* Connected cables form one **network** that routes energy in the same tick, with no per-cable buffer and no per-cable ticking.
* **2,000 FE/t per connection** (configurable).
* **Routing:** machines first, split evenly; batteries get what is left, and only when the energy comes from a generator. Energy never goes back to the block that sent it, and batteries never charge each other through cables.
* Pulls from generators that do not push on their own.
* Visual connection to any block with the NeoForge `Capabilities.EnergyStorage.BLOCK` capability.
* 8 cables per craft (Voltaic Ingots + Redstone).

### 6. 🔋 Pocket Capacitor
* 100,000 FE personal bank with a charge bar.
* While in your inventory it charges the items **in both hands and the armor you wear**, up to 500 FE/t in total.
* Can be charged or emptied in an Energy Cell.

### 7. ⚙️ Materials
* **Voltaic Dust:** `Iron Ingot + Copper Ingot + Redstone` → 2. Smelt (200 ticks) or blast (100 ticks) into **Voltaic Ingots**.
* **Conductive Dust:** `Voltaic Ingot + Gold Ingot + Redstone` → 2. Smelt or blast into **Conductive Alloy**.
* Ingots come **only** from smelting the dust: there is no direct shapeless recipe competing for the same ingredients.
* Recipes accept any mod's iron, copper, gold and redstone through the common `c:` tags.
* Tags: `c:ingots/voltaic`, `c:ingots/conductive_alloy`, `c:dusts/voltaic`, `c:dusts/conductive_alloy` (inside `c:ingots` and `c:dusts`).
* **Thermopile Core:** component for the Thermoelectric Generator.

---

## 🛠️ Development

```bash
./gradlew build              # build the mod jar
./gradlew runClient          # dev client
./gradlew runData            # regenerate tags, loot tables and recipes (src/generated/resources)
./gradlew runGameTestServer  # run the in-world tests; exits non-zero if one fails
```

* **Data is generated:** tags, loot tables and recipes come from `src/main/java/com/voltaicdynamics/datagen`. Edit the providers and run `runData`; don't hand-edit `src/generated`.
* **Game tests** (`gametest/VoltaicGameTests.java`) check mining tags, cells keeping energy, no battery ping-pong, cable delivery, the lava bucket, the Electric Furnace, the Thermoelectric Generator and the recipe set.
* **Art sources** for the Electric Furnace textures are in `art-src/textures/*.pontilha` (Pontilha).
* Versions live in `gradle.properties`; `neoforge.mods.toml` is filled from it at build time.

---

## 📜 Changelog

### 1.1.0
* Fixed: machines and cells dropped nothing when mined (missing `mineable/pickaxe` tag).
* Fixed: duplicate recipes. Iron + copper + redstone made both dust and ingots, and the same happened for conductive alloy.
* Fixed: lava buckets were destroyed by the Combustion Generator.
* Fixed: Energy Cells lost their energy when broken.
* Fixed: cells and cables bounced energy back and forth between batteries.
* Fixed: pipes could steal fuel from the Combustion Generator.
* New cable network: one network per connected group, cached capabilities, no per-cable ticking.
* New: Electric Furnace, server config, comparator output, Pocket Capacitor charges offhand and armor, Brazilian Portuguese translation, Thermoelectric source wear.
* Item energy is stored in the `voltaicdynamics:energy` data component. Pocket Capacitors from 1.0.0 keep their charge (converted the next time they are in a player's inventory).
* Upgrading a world: the up to 4,000 FE buffered inside each 1.0.0 cable is dropped, since cables no longer store energy.
