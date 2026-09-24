package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Which light switch placed a relay, so the relay can tell, when its neighbours change, whether
 * that switch is still there, still on and still linked through it. Never ticks; never synced
 * to a client beyond the block itself.
 *
 * @since 2026.9
 */
public class TileEntitySwitchRelay extends AbstractTileEntity {

  private static final String KEY_SWITCH = "s";

  @Nullable
  private BlockPos switchPos;

  /**
   * The switch that placed the relay.
   *
   * @return its position, or null if not known
   */
  @Nullable
  public BlockPos getSwitch() {
    return switchPos;
  }

  /**
   * Records the switch that placed the relay.
   *
   * @param switchPos its position
   */
  public void setSwitch(BlockPos switchPos) {
    this.switchPos = switchPos;
    markDirty();
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    int[] p = compound.getIntArray(KEY_SWITCH);
    switchPos = p.length == 3 ? new BlockPos(p[0], p[1], p[2]) : null;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (switchPos != null) {
      compound.setIntArray(KEY_SWITCH,
          new int[]{switchPos.getX(), switchPos.getY(), switchPos.getZ()});
    }
    return compound;
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
