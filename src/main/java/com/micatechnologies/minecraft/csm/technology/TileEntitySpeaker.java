package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Backing tile entity for every block in the speaker family (Atlas, Bose, FourJay, JBL,
 * Valcom, etc.). Each speaker holds an optional link to a {@link TileEntityRedstoneTTS}.
 * When linked, the TTS module extends its broadcast radius to include each speaker's
 * position, so a player anywhere in the union of speaker coverage areas hears the
 * announcement.
 *
 * <p>Like the HVAC vent relay, this TE pauses ticking entirely while the speaker is not
 * linked — purely cosmetic placements impose no per-tick cost.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class TileEntitySpeaker extends AbstractTickableTileEntity {

  private static final String NBT_HAS_LINK = "hasLink";
  private static final String NBT_LINK_X = "linkX";
  private static final String NBT_LINK_Y = "linkY";
  private static final String NBT_LINK_Z = "linkZ";

  private BlockPos linkedTtsPos = null;

  // region Link management

  public boolean hasLink() {
    return linkedTtsPos != null;
  }

  public BlockPos getLinkedTtsPos() {
    return linkedTtsPos;
  }

  public void setLinkedTtsPos(BlockPos pos) {
    this.linkedTtsPos = pos == null ? null : pos.toImmutable();
    if (world != null && !world.isRemote) {
      markDirtySync(world, this.pos, true);
    }
  }

  public void clearLink() {
    setLinkedTtsPos(null);
  }

  // endregion

  // region Tick

  @Override
  public boolean doClientTick() {
    return false;
  }

  /** Skip ticking entirely when this speaker has no TTS link — cosmetic placements stay free. */
  @Override
  public boolean pauseTicking() {
    return linkedTtsPos == null;
  }

  @Override
  public long getTickRate() {
    return 40;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote || linkedTtsPos == null) {
      return;
    }
    if (!world.isBlockLoaded(linkedTtsPos)) {
      return;
    }
    TileEntity te = world.getTileEntity(linkedTtsPos);
    if (!(te instanceof TileEntityRedstoneTTS)) {
      // The TTS module was removed or replaced — drop the dangling link so the
      // speaker can return to fully cosmetic mode.
      clearLink();
    }
  }

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.getBoolean(NBT_HAS_LINK)) {
      linkedTtsPos = new BlockPos(
          compound.getInteger(NBT_LINK_X),
          compound.getInteger(NBT_LINK_Y),
          compound.getInteger(NBT_LINK_Z));
    } else {
      linkedTtsPos = null;
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (linkedTtsPos != null) {
      compound.setBoolean(NBT_HAS_LINK, true);
      compound.setInteger(NBT_LINK_X, linkedTtsPos.getX());
      compound.setInteger(NBT_LINK_Y, linkedTtsPos.getY());
      compound.setInteger(NBT_LINK_Z, linkedTtsPos.getZ());
    } else {
      compound.setBoolean(NBT_HAS_LINK, false);
    }
    return compound;
  }

  // endregion
}
