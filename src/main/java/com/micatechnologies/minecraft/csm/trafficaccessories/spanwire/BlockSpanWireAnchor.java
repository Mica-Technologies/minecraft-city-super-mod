package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The pole-side terminus of a span of messenger cable: the plate and eyebolt the cable is dead
 * ended on.
 *
 * <p>Two of these, linked with the span wire tool, define a span. Everything else about a span
 * -- where it sags to, which mounts hang off it -- follows from the pair.
 *
 * <p>The block itself carries only a facing and a hitbox. The cable is drawn from Phase 2 and is
 * purely visual: it has no collision and cannot be clicked (the plan's D9), so this block and
 * the hanger mounts are the only things a player can actually interact with on a span. That is
 * why breaking one has to be handled here.
 */
public class BlockSpanWireAnchor extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider, ICsmNoSnowAccumulation {

  /**
   * Backing plate against the pole plus the eyebolt standing off it. Deliberately generous
   * around the eye: this is the grab handle for the whole span, since the cable itself cannot be
   * clicked.
   *
   * <p>One box, for the default facing only. {@link AbstractBlockRotatableNSEW} rotates the
   * hitbox with the block; adding a per-facing switch here would rotate it twice and land it on
   * the wrong face.
   */
  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(0.28125D, 0.34375D, 0.0D, 0.71875D, 0.96875D, 0.5625D);

  public BlockSpanWireAnchor() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "spanwireanchor";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySpanWireAnchor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityspanwireanchor";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntitySpanWireAnchor();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  /**
   * Dissolves the whole span before the tile entity that knows about it is removed. Cable cannot
   * dead end on nothing, so losing an anchor takes the span with it -- unlike losing a hanger,
   * which the cable simply closes over.
   */
  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    SpanWireManager.onAttachmentRemoved(worldIn, pos);
    // An anchor may also be the lower one a box span's tether dead-ends on, in which case it
    // belongs to no span of its own and the span above has to be told its tether just lost an
    // end. Run after the removal above, and before the block actually goes, for the same reason
    // the guy anchor cleanup does: the search has to see the world it is correcting.
    SpanWireManager.onTetherAnchorRemoved(worldIn, pos);
    super.breakBlock(worldIn, pos, state);
  }
}
