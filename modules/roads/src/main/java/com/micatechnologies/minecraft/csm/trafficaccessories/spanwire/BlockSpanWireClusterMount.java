package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A wire mount that carries a cluster of two to four signals from one point on the span.
 *
 * <p><b>Retired</b> into the signal cluster hanger ({@link BlockSignalClusterHanger}): the
 * {@code spanwireclustermount} block is registered from the Roads hidden tab with the hanger as
 * its replacement, so a placed one becomes a hanger on a random tick and keeps its span, mount
 * style, coil style and tether (see {@link #configureReplacement}). It stays a base class because
 * the hanger is built on its tile entity.
 *
 * <p>Everything about taking part in a span it inherits from {@link BlockSpanWireHangerMount},
 * including the configuration screen -- which grows a cluster width row when the mount it opens
 * on is one of these.
 */
public class BlockSpanWireClusterMount extends BlockSpanWireHangerMount {

  public BlockSpanWireClusterMount(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube) {
    super(registryName, boundingBox, renderLayer, hardness, fullCube);
  }

  /**
   * A cluster mount that retires into another block.
   *
   * @param replacementBlockId the block it becomes, or null for a live block
   */
  public BlockSpanWireClusterMount(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube,
      @Nullable String replacementBlockId) {
    super(registryName, boundingBox, renderLayer, hardness, fullCube, replacementBlockId);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySpanWireClusterMount.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityspanwireclustermount";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntitySpanWireClusterMount();
  }

  /**
   * Turns the old cluster mount's data into a hanger's, then puts the span back on every block in
   * it.
   *
   * <p>The second step is not optional. The swap goes through {@code setBlockState}, which calls
   * this block's {@link #breakBlock}, and that drops the mount out of its span on every other
   * attachment ({@link SpanWireManager#onAttachmentRemoved}). The NBT taken before the swap still
   * holds the whole span, so the hanger reads it and the whole span is written out again. Without
   * this the hanger would think itself linked while the anchors drew their cable straight past it.
   */
  @Override
  public void configureReplacement(World world, BlockPos pos, NBTTagCompound oldTileEntityNBT) {
    final TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntitySignalClusterHanger)) {
      super.configureReplacement(world, pos, oldTileEntityNBT);
      return;
    }
    final TileEntitySignalClusterHanger hanger = (TileEntitySignalClusterHanger) te;
    hanger.readNBT(TileEntitySignalClusterHanger.fromRetiredClusterMountNBT(oldTileEntityNBT));
    final SpanWireDefinition span = hanger.getSpan();
    if (span != null) {
      // Writes the span to the hanger too, which marks it dirty and syncs it to clients.
      SpanWireManager.apply(world, span);
    } else {
      hanger.markDirtySync(world, pos, true);
    }
  }
}
