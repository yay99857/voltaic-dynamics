package com.voltaicdynamics.core.energy;

/**
 * A block entity that stores energy and pushes it out by itself, such as an Accumulator. Buffers never push
 * into each other ({@code SideConfigHelper.ejectEnergy} skips them), so two touching buffers do not bounce
 * energy back and forth. Anything else that accepts energy, including other mods' cables, is fed.
 */
public interface EnergyBuffer {
}
