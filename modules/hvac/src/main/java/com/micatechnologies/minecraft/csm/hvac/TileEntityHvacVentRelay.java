package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * A vent: where a system's conditioned air enters a room. It is linked to a thermostat (primary
 * or zone) and the system blows into the region of the room the vent sits in; see
 * {@link HvacSystemControl}.
 *
 * <p>A linked vent is an anchor of the thermal simulation, and saves the temperature of its
 * region so a room served only by vents also comes back at the temperature it had. An unlinked,
 * purely decorative vent costs nothing: it neither ticks nor anchors.</p>
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacVentRelay extends AbstractTileEntity {

  private static final String NBT_HAS_LINK = "hasLink";
  private static final String NBT_LINK_X = "linkX";
  private static final String NBT_LINK_Y = "linkY";
  private static final String NBT_LINK_Z = "linkZ";
  private static final String NBT_SAVED_TEMP = "sT";
  /** Retired: the old per-vent contribution. Removed on load. */
  private static final String OLD_NBT_CONTRIBUTION = "contribution";

  /** Position of the linked thermostat, or null if not linked. */
  private BlockPos linkedThermostatPos = null;

  /** Temperature of the vent's region when last saved, or NaN. */
  private float savedTemp = Float.NaN;

  // region Simulation anchor

  @Override
  public void onLoad() {
    super.onLoad();
    register();
  }

  private void register() {
    if (linkedThermostatPos == null) {
      return;
    }
    HvacThermalWorld w = HvacThermal.get(world);
    if (w != null) {
      w.registerAnchor(pos, ThermalAnchor.VENT, savedTemp);
    }
  }

  @Override
  public void onChunkUnload() {
    unregister();
    super.onChunkUnload();
  }

  @Override
  public void invalidate() {
    unregister();
    super.invalidate();
  }

  private void unregister() {
    HvacThermalWorld w = HvacThermal.peek(world);
    if (w != null) {
      ThermalAnchor a = w.anchor(pos);
      if (a != null && !Float.isNaN(a.temperature())) {
        savedTemp = a.temperature();
      }
      w.unregisterAnchor(pos);
    }
  }

  // endregion

  // region Link Management

  /**
   * Links this vent to a thermostat, if it is within {@code maxDistance} blocks.
   *
   * @return whether the link was made
   */
  public boolean setLinkedThermostat(BlockPos thermostatPos, int maxDistance) {
    if (thermostatPos != null && pos.getDistance(thermostatPos.getX(), thermostatPos.getY(),
        thermostatPos.getZ()) > maxDistance) {
      return false;
    }
    this.linkedThermostatPos = thermostatPos != null ? thermostatPos.toImmutable() : null;
    if (world != null) {
      if (linkedThermostatPos != null) {
        register();
      } else {
        unregister();
      }
      markDirtySync(world, pos);
    }
    return true;
  }

  public void clearLink() {
    setLinkedThermostat(null, Integer.MAX_VALUE);
  }

  public BlockPos getLinkedThermostatPos() {
    return linkedThermostatPos;
  }

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.getBoolean(NBT_HAS_LINK)) {
      linkedThermostatPos = new BlockPos(compound.getInteger(NBT_LINK_X),
          compound.getInteger(NBT_LINK_Y), compound.getInteger(NBT_LINK_Z));
    } else {
      linkedThermostatPos = null;
    }
    savedTemp = compound.hasKey(NBT_SAVED_TEMP) ? compound.getFloat(NBT_SAVED_TEMP) : Float.NaN;
    compound.removeTag(OLD_NBT_CONTRIBUTION);
    // A chunk load reads before the world is set, and onLoad registers after it. A world already
    // set means a write to a vent that is in place (/setblock with a data tag, /blockdata): onLoad
    // has run and will not run again, so the link is taken up, or dropped, here.
    if (world != null) {
      if (linkedThermostatPos != null) {
        register();
      } else {
        unregister();
      }
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (linkedThermostatPos != null) {
      compound.setBoolean(NBT_HAS_LINK, true);
      compound.setInteger(NBT_LINK_X, linkedThermostatPos.getX());
      compound.setInteger(NBT_LINK_Y, linkedThermostatPos.getY());
      compound.setInteger(NBT_LINK_Z, linkedThermostatPos.getZ());
    } else {
      compound.setBoolean(NBT_HAS_LINK, false);
    }
    float t = savedTemp;
    HvacThermalWorld w = HvacThermal.peek(world);
    if (w != null) {
      ThermalAnchor a = w.anchor(pos);
      if (a != null && a.space != null) {
        t = a.temperature();
      }
    }
    if (!Float.isNaN(t)) {
      compound.setFloat(NBT_SAVED_TEMP, t);
    }
    return compound;
  }

  /** No baked model reads this tile entity. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  // endregion
}
