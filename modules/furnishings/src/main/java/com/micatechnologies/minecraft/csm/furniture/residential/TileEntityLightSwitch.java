package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * What a placed light switch is linked to ({@link SwitchLinks}): the block it powers from afar,
 * and, while it is on, where the relay it placed beside that block is. Never ticks; the link
 * never goes to clients.
 *
 * @since 2026.9
 */
public class TileEntityLightSwitch extends AbstractTileEntity {

  private static final String KEY_TARGET = "t";
  private static final String KEY_RELAY = "r";

  @Nullable
  private BlockPos target;
  @Nullable
  private BlockPos relay;

  /**
   * The block the switch is linked to.
   *
   * @return its position, or null if it is not linked
   */
  @Nullable
  public BlockPos getTarget() {
    return target;
  }

  /**
   * Links the switch to a block.
   *
   * @param target its position, or null to unlink
   */
  public void setTarget(@Nullable BlockPos target) {
    this.target = target;
    markDirty();
  }

  /**
   * Where the relay the switch placed is, while it is on.
   *
   * @return its position, or null
   */
  @Nullable
  public BlockPos getRelay() {
    return relay;
  }

  /**
   * Records where the relay the switch placed is.
   *
   * @param relay its position, or null for none
   */
  public void setRelay(@Nullable BlockPos relay) {
    this.relay = relay;
    markDirty();
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    target = read(compound, KEY_TARGET);
    relay = read(compound, KEY_RELAY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    write(compound, KEY_TARGET, target);
    write(compound, KEY_RELAY, relay);
    return compound;
  }

  /** The link stays on the server. */
  @Override
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = super.getUpdateTag();
    tag.removeTag(KEY_TARGET);
    tag.removeTag(KEY_RELAY);
    return tag;
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  @Nullable
  private static BlockPos read(NBTTagCompound compound, String key) {
    int[] p = compound.getIntArray(key);
    return p.length == 3 ? new BlockPos(p[0], p[1], p[2]) : null;
  }

  private static void write(NBTTagCompound compound, String key, @Nullable BlockPos pos) {
    if (pos != null) {
      compound.setIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }
  }
}
