package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * What a {@link BlockStandpipePassthrough} is dressed as: the block (registry name and metadata)
 * whose model it draws round the pipe, and whether a player paid a block for it in survival, so it
 * is given back. Nothing ticks; the look is baked into the chunk mesh by
 * {@link StandpipePassthroughModels}, which reads it through the block's extended state.
 *
 * <p>Short NBT keys: {@code mb} the block, {@code mm} its metadata, {@code pd} paid.</p>
 *
 * @since 2026.10
 */
public class TileEntityStandpipePassthrough extends AbstractTileEntity {

  @Nullable
  private IBlockState mimic;
  private boolean paid;

  @Override
  @SuppressWarnings("deprecation")
  public void readNBT(NBTTagCompound compound) {
    mimic = null;
    if (compound.hasKey("mb")) {
      Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(compound.getString("mb")));
      if (block != null && BlockStandpipePassthrough.canMimic(
          block.getStateFromMeta(compound.getByte("mm")))) {
        mimic = block.getStateFromMeta(compound.getByte("mm"));
      }
    }
    paid = compound.getBoolean("pd");
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (mimic != null && mimic.getBlock().getRegistryName() != null) {
      compound.setString("mb", mimic.getBlock().getRegistryName().toString());
      compound.setByte("mm", (byte) mimic.getBlock().getMetaFromState(mimic));
      if (paid) {
        compound.setBoolean("pd", true);
      }
    }
    return compound;
  }

  @Nullable
  public IBlockState getMimic() {
    return mimic;
  }

  public boolean isPaid() {
    return paid;
  }

  /** Dresses the pass-through as {@code state} (or bare, for null) and syncs it to clients. */
  public void setMimic(@Nullable IBlockState state, boolean paid) {
    this.mimic = state;
    this.paid = state != null && paid;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, world.getBlockState(pos), true);
    }
  }

  @Override
  protected long getBakedModelKey() {
    return mimic == null ? 0 : mimic.hashCode();
  }
}
