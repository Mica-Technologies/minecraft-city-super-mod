package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalAPSSoundSchemes;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Tile entity for a European push button: the APS behaviour of every push button (locator tone,
 * press confirmation, walk sound, linked as a pedestrian accessory) plus the request lamp, the
 * yellow panel that lights once the button has been pressed and stays lit until the walk comes
 * ("Signal kommt").
 *
 * <p>The lamp follows the request count every pedestrian accessory already keeps: lit while a
 * request is held and the crossing is not on walk. The count is server-side only, so the lamp is
 * its own flag, synced to the client only when it changes and read by the block's actual
 * state.</p>
 *
 * @since 2026.10
 */
public class TileEntityTrafficSignalAPSEuro extends TileEntityTrafficSignalAPS {

  /** Written only while lit, so a button with no request carries no extra tag. */
  private static final String REQUEST_LAMP_KEY = "rqL";

  private boolean requestLamp = false;

  public TileEntityTrafficSignalAPSEuro() {
    super(TrafficSignalAPSSoundSchemes.EURO);
  }

  /** Whether the request lamp is lit. */
  public boolean isRequestLampLit() {
    return requestLamp;
  }

  /**
   * Brings the request lamp up to date with the request count and the crossing's state, and
   * syncs it to the client if it changed. Server side only.
   */
  public void updateRequestLamp() {
    if (world == null || world.isRemote) {
      return;
    }
    IBlockState state = world.getBlockState(pos);
    if (!state.getPropertyKeys().contains(AbstractBlockControllableSignal.COLOR)) {
      return;
    }
    boolean walking =
        state.getValue(AbstractBlockControllableSignal.COLOR)
            == AbstractBlockControllableSignal.SIGNAL_GREEN;
    boolean lit = getRequestCount() > 0 && !walking;
    if (lit != requestLamp) {
      requestLamp = lit;
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public void onTick() {
    super.onTick();
    updateRequestLamp();
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    if (requestLamp) {
      compound.setBoolean(REQUEST_LAMP_KEY, true);
    }
    return compound;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    requestLamp = compound.hasKey(REQUEST_LAMP_KEY) && compound.getBoolean(REQUEST_LAMP_KEY);
  }
}
