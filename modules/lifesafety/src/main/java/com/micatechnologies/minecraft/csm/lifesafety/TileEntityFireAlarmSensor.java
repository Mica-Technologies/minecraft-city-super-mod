package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tile entity for fire alarm activator blocks (pull stations, detectors, sprinklers) that stores
 * the linked fire alarm control panel position for alarm activation, and — for sprinklers — the
 * water this head has discharged, so the panel can take it back on reset.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class TileEntityFireAlarmSensor extends AbstractTileEntity {

  /** Outcome of pointing a linker at this device. */
  public enum LinkResult {
    /** The device had no panel and now has one. */
    LINKED,
    /** The device was moved from one panel to a different one. */
    RELINKED,
    /** The device was already linked to this same panel; nothing changed. */
    ALREADY_LINKED
  }

  // Short-form key holds the linked panel position as a single 3-int IntArray {x, y, z}.
  // The legacy triple of per-axis ints is still accepted on read for backwards compat.
  private static final String linkedPanelKey = "lp";
  private static final String legacyLinkedPanelPosXKey = "lpX";
  private static final String legacyLinkedPanelPosYKey = "lpY";
  private static final String legacyLinkedPanelPosZKey = "lpZ";
  /** Water this head has discharged, as a flat IntArray of x, y, z triples. */
  private static final String dischargedWaterKey = "wtr";
  /** The device's fire alarm zone, 1-8; absent (zone 0, general) for every device before #335. */
  private static final String zoneKey = "zn";
  /**
   * The home system a residential alarm is interconnected with, when it has no panel: the position
   * of the alarm the system was started from, as {x, y, z}. Only a key, never looked up.
   */
  private static final String interconnectKey = "ig";

  /**
   * Y sentinel meaning "not linked to any panel". A real link can never use it: the world floor is
   * y=0, so nothing legitimate is ever placed at -500.
   */
  private static final int UNLINKED_Y = -500;

  /**
   * Ceiling on tracked discharge positions. A sprinkler over a fire that keeps burning re-fires
   * every scan, and without a bound the list would grow for as long as the fire lasts. Reaching
   * the cap only means the oldest puddle stops being cleaned up automatically.
   */
  private static final int MAX_TRACKED_WATER = 256;

  private int linkedPanelX;
  private int linkedPanelY = UNLINKED_Y;
  private int linkedPanelZ;
  private final List<BlockPos> dischargedWater = new ArrayList<>();
  private int zone;
  private BlockPos interconnectGroup;

  /**
   * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    boolean loaded = false;
    if (compound.hasKey(linkedPanelKey)) {
      int[] pos = compound.getIntArray(linkedPanelKey);
      if (pos.length == 3) {
        linkedPanelX = pos[0];
        linkedPanelY = pos[1];
        linkedPanelZ = pos[2];
        loaded = true;
      }
    } else if (compound.hasKey(legacyLinkedPanelPosXKey) &&
        compound.hasKey(legacyLinkedPanelPosYKey) &&
        compound.hasKey(legacyLinkedPanelPosZKey)) {
      linkedPanelX = compound.getInteger(legacyLinkedPanelPosXKey);
      linkedPanelY = compound.getInteger(legacyLinkedPanelPosYKey);
      linkedPanelZ = compound.getInteger(legacyLinkedPanelPosZKey);
      loaded = true;
    }
    if (!loaded) {
      linkedPanelY = UNLINKED_Y;
    }

    dischargedWater.clear();
    int[] water = compound.getIntArray(dischargedWaterKey);
    for (int i = 0; i + 2 < water.length; i += 3) {
      dischargedWater.add(new BlockPos(water[i], water[i + 1], water[i + 2]));
    }

    zone = FireAlarmZones.clamp(compound.getInteger(zoneKey));
    int[] group = compound.getIntArray(interconnectKey);
    interconnectGroup = group.length == 3 ? new BlockPos(group[0], group[1], group[2]) : null;

    // Strip legacy long-form keys so the next save produces only short-form output
    compound.removeTag(legacyLinkedPanelPosXKey);
    compound.removeTag(legacyLinkedPanelPosYKey);
    compound.removeTag(legacyLinkedPanelPosZKey);
  }

  /**
   * Returns the NBT tag compound with the tile entity's NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setIntArray(linkedPanelKey,
        new int[] {linkedPanelX, linkedPanelY, linkedPanelZ});
    if (!dischargedWater.isEmpty()) {
      int[] water = new int[dischargedWater.size() * 3];
      for (int i = 0; i < dischargedWater.size(); i++) {
        BlockPos pos = dischargedWater.get(i);
        water[i * 3] = pos.getX();
        water[i * 3 + 1] = pos.getY();
        water[i * 3 + 2] = pos.getZ();
      }
      compound.setIntArray(dischargedWaterKey, water);
    }
    if (zone != FireAlarmZones.GENERAL) {
      compound.setInteger(zoneKey, zone);
    }
    if (interconnectGroup != null) {
      compound.setIntArray(interconnectKey, new int[] {interconnectGroup.getX(),
          interconnectGroup.getY(), interconnectGroup.getZ()});
    }
    return compound;
  }

  /** Joins the scheduled looks if this is a detector or a panel follower (#308). */
  @Override
  public void onLoad() {
    super.onLoad();
    FireAlarmDeviceSampler.add(world, pos);
    if (interconnectGroup != null) {
      ResidentialInterconnect.join(world, interconnectGroup, pos);
    }
  }

  @Override
  public void invalidate() {
    super.invalidate();
    FireAlarmDeviceSampler.remove(world, pos);
    if (interconnectGroup != null) {
      ResidentialInterconnect.leave(world, interconnectGroup, pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    FireAlarmDeviceSampler.remove(world, pos);
    if (interconnectGroup != null) {
      ResidentialInterconnect.leave(world, interconnectGroup, pos);
    }
  }

  /**
   * The device's fire alarm zone (#335): 0, the general zone, unless a linker set one.
   *
   * @return the zone, 0-8
   *
   * @since 2026.11
   */
  public int getZone() {
    return zone;
  }

  /**
   * Sets the device's fire alarm zone, as the linker stamps it.
   *
   * @param zone the zone, 0-8
   *
   * @return {@code true} if it changed
   *
   * @since 2026.11
   */
  public boolean setZone(int zone) {
    int z = FireAlarmZones.clamp(zone);
    if (z == this.zone) {
      return false;
    }
    this.zone = z;
    markDirty();
    return true;
  }

  /**
   * The home system this residential alarm is interconnected with when it has no panel, or
   * {@code null}.
   *
   * @return the system's key position, or {@code null}
   *
   * @since 2026.11
   */
  public BlockPos getInterconnectGroup() {
    return interconnectGroup;
  }

  /**
   * Puts this alarm on a home system's interconnect, or takes it off with {@code null}.
   *
   * @param group the system's key position, or {@code null}
   *
   * @return {@code true} if it changed
   *
   * @since 2026.11
   */
  public boolean setInterconnectGroup(BlockPos group) {
    BlockPos g = group == null ? null : group.toImmutable();
    if (g == null ? interconnectGroup == null : g.equals(interconnectGroup)) {
      return false;
    }
    if (interconnectGroup != null && world != null) {
      ResidentialInterconnect.leave(world, interconnectGroup, pos);
    }
    interconnectGroup = g;
    if (g != null && world != null) {
      ResidentialInterconnect.join(world, g, pos);
    }
    markDirty();
    return true;
  }

  /**
   * Whether this device is linked to a control panel.
   *
   * @return {@code true} if a panel position is stored
   */
  public boolean hasLinkedPanel() {
    return linkedPanelY != UNLINKED_Y;
  }

  /**
   * The position of the panel this device reports to, or {@code null} when it is not linked.
   * <p>
   * Previously this returned a {@link BlockPos} built from the unlinked sentinel, leaving every
   * caller to look up a tile entity at y=-500 and infer "not linked" from the miss.
   *
   * @param world the device's world (unused; kept for call-site compatibility)
   *
   * @return the linked panel position, or {@code null} if unlinked
   */
  public BlockPos getLinkedPanelPos(World world) {
    return hasLinkedPanel() ? new BlockPos(linkedPanelX, linkedPanelY, linkedPanelZ) : null;
  }

  /**
   * Points this device at a control panel.
   * <p>
   * Re-linking is allowed. It used to be refused outright once a device had any panel at all,
   * which meant a pull station could never be moved to a different panel — the only remedy was to
   * break the block and place a new one — and the caller was told nothing about why the click did
   * nothing.
   *
   * @param blockPos the panel to link to
   *
   * @return what the link attempt did, for the caller to report
   */
  public LinkResult setLinkedPanelPos(BlockPos blockPos) {
    if (hasLinkedPanel() && linkedPanelX == blockPos.getX() && linkedPanelY == blockPos.getY()
        && linkedPanelZ == blockPos.getZ()) {
      return LinkResult.ALREADY_LINKED;
    }
    boolean wasLinked = hasLinkedPanel();
    linkedPanelX = blockPos.getX();
    linkedPanelY = blockPos.getY();
    linkedPanelZ = blockPos.getZ();
    markDirty();
    return wasLinked ? LinkResult.RELINKED : LinkResult.LINKED;
  }

  /**
   * Forgets the panel this device reports to, when it is unlinked with the linker.
   *
   * @since 2026.9
   */
  public void clearLinkedPanel() {
    linkedPanelX = 0;
    linkedPanelY = UNLINKED_Y;
    linkedPanelZ = 0;
    markDirty();
  }

  /**
   * Records a block this device has flooded, so it can be drained again when the panel is reset.
   *
   * @param pos the position water was placed at
   */
  public void addDischargedWater(BlockPos pos) {
    if (dischargedWater.size() >= MAX_TRACKED_WATER || dischargedWater.contains(pos)) {
      return;
    }
    dischargedWater.add(pos);
    markDirty();
  }

  /**
   * Drains every block this device flooded and forgets them.
   * <p>
   * Only blocks that are still water are cleared, so anything a player has since built or placed
   * in a flooded spot is left alone.
   *
   * @param world the world to clear the water in
   *
   * @return the number of blocks actually drained
   */
  public int clearDischargedWater(World world) {
    if (dischargedWater.isEmpty()) {
      return 0;
    }
    int cleared = 0;
    for (BlockPos pos : dischargedWater) {
      if (!world.isBlockLoaded(pos)) {
        continue;
      }
      IBlockState state = world.getBlockState(pos);
      if (state.getMaterial() == Material.WATER) {
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        cleared++;
      }
    }
    dischargedWater.clear();
    markDirty();
    return cleared;
  }
}
