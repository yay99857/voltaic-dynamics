# ⚡ Voltaic Dynamics

High-performance energy and technology mod built for **Minecraft 1.21.1 (NeoForge)**, tailored specifically for **All The Mods 11** and fully cross-compatible with **Mekanism**, **Applied Energistics 2**, **Powah**, and **Extreme Reactors**.

---

## 🚀 Installation Guide for All The Mods 11

1. The compiled `.jar` artifact is located at:
   ```
   voltaic-dynamics/build/libs/voltaicdynamics-1.0.0.jar
   ```
2. Open your **All The Mods 11** instance folder (via CurseForge, Prism Launcher, or Modrinth).
3. Navigate to the `mods/` directory.
4. Drop `voltaicdynamics-1.0.0.jar` into `mods/`.
5. Launch the game! The mod registers its own creative tab, full recipes in JEI/EMI, and native FE capabilities.

---

## 🔋 Mechanics & Core Content

### 1. 🌡️ Thermoelectric Generator
* **Passive Generation:** Exploits the Seebeck thermoelectric effect to generate continuous Forge Energy (FE) without consuming fuel.
* **Environmental Gradient System:** Evaluates all 6 adjacent faces:
  * **Heat Sources:** Lava (`+20`), Fire / Campfire (`+15`), Magma Block (`+12`).
  * **Cold Sources:** Blue Ice (`+25`), Packed Ice (`+16`), Regular Ice (`+10`), Snow Block (`+6`), Water (`+5`).
  * **Equation:** `(Heat Score * Cold Score) / 15`, producing up to **100 FE/t** passively in early-game setups.
* **Internal Buffer:** 50,000 FE with auto-push to adjacent cables and energy acceptors.
* **Status Inspection:** Right-click to view real-time generation rate and stored energy.

### 2. 🔥 Combustion Generator & Control Panel (HUD/GUI)
* **Solid Fuel Power:** Burns standard solid fuels (coal, charcoal, wood logs, blaze rods, coal blocks) producing **40 FE/t**.
* **Internal Buffer:** 100,000 FE with 400 FE/t output rate.
* **Interactive HUD/Screen:**
  * Clean, industrial 176x166 control panel.
  * Dedicated fuel slot with ghost coal icon and quick shift-click support.
  * Animated flame combustion meter.
  * Dynamic electric cyan energy gauge with interactive hover tooltip displaying live FE storage and generation rate.
* **World Feedback:** Illuminates surrounding blocks (light level 13), emits flame and smoke particles when burning.

### 3. 🔋 Energy Cells (Batteries)
* **Basic Energy Cell:**
  * **Capacity:** `1,000,000 FE` (1M FE).
  * **I/O Rate:** `2,000 FE/t`.
  * **Interface:** Dedicated GUI with live energy gauge, item **Charge Slot** (charges items/tools) and item **Discharge Slot** (empties batteries into the cell).
  * **Auto-Push:** Automatically sends energy to connected cables and machines.
* **Advanced Energy Cell:**
  * **Capacity:** `8,000,000 FE` (8M FE).
  * **I/O Rate:** `10,000 FE/t`.
  * **Interface:** Heavy-duty gold/amber reinforced interface and chassis for mid-to-late game power storage.

### 4. 🔌 Energy Cables (Universal Compatibility)
* **Dynamic Multipart Network:** 3D connected pipes that visually attach to any adjacent machine, battery, or cable.
* **Cross-Mod Compatibility:** Fully compatible with **Mekanism**, **Powah**, **Thermal Series**, **AE2 Energy Acceptor**, **Extreme Reactors**, and any mod using NeoForge `Capabilities.EnergyStorage.BLOCK`.
* **Transfer Rate:** `2,000 FE/t` per connection with balanced round-robin distribution among receivers.
* **Crafting Yield:** 8 cables per craft using `Voltaic Ingots` and `Redstone`.

### 5. 🔋 Pocket Capacitor
* **Personal Energy Bank:** Stores up to **100,000 FE**.
* **Auto-Recharge:** While carried in the inventory, automatically transfers energy to powered tools, armor, and equipment held in the main hand.
* **Dynamic Durability Bar:** Displays current charge level with an electric cyan bar.

### 6. ⚙️ Materials, Dusts & Ore Processing Pipeline (ATM Standard)
* **Voltaic Ingot:** Canonical Minecraft ingot geometry forged with bronze-iron alloy and a pulsating cyan plasma channel.
* **Conductive Alloy:** High-contrast electro-refined gold ingot embedded with conductive Redstone filaments.
* **Voltaic Dust:** Metallic dust blend crafted via `1x Iron + 1x Copper + 1x Redstone` ➔ `2x Voltaic Dust`.
* **Conductive Dust:** Precious alloy blend crafted via `1x Voltaic Ingot + 1x Gold Ingot + 1x Redstone` ➔ `2x Conductive Dust`.
* **Furnace & Blast Furnace Support:** Dusts can be smelted into ingots in standard furnaces (200 ticks) or blast furnaces (100 ticks).
* **Thermopile Core:** High-precision thermoelectric module with machined copper coils and bipolar Peltier junctions.
* **Global Common Tags (`#c:ingots`, `#c:dusts`):** Seamless integration with Mekanism Crusher, Thermal Pulverizer, and AE2 patterns.

---

## 🛠️ Build Commands

```bash
# Build the mod jar:
./gradlew build

# Launch local development test client:
./gradlew runClient
```
