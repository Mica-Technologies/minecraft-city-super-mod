package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterPiece;
import com.micatechnologies.minecraft.csm.furniture.residential.SurfaceRest;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Verifone MX915 payment terminal. Mostly a decorative checkout prop, but right-clicking
 * plays a card reader's approval beep so the block feels like a checkout terminal in shops,
 * fare gates, etc.
 *
 * <p>It moved from the Technology module to the Market &amp; Store tab with its registry name,
 * metadata (facing 0–5) and asset paths unchanged, so placed terminals load as they were. Lying
 * flat (facing a horizontal way), it now stands on whatever is under it as the Residential
 * counter pieces do ({@link BlockCounterPiece#REST}, actual state, see {@link SurfaceRest}): on a
 * checkout counter it lies on the counter top rather than hovering over it. Stood on end (up or
 * down) it keeps its old place.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockVerifoneMx915 extends AbstractBlockRotatableNSEWUD {

  private static final AxisAlignedBB BBOX = new AxisAlignedBB(
      0.3125, 0.0, 0.3125, 0.6875, 0.0625, 0.6875);

  public BlockVerifoneMx915() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0.6F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "vf915";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, BlockCounterPiece.REST);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
    IBlockState s = super.getActualState(state, worldIn, pos);
    SurfaceRest rest = s.getValue(FACING).getAxis().isHorizontal()
        ? SurfaceRest.under(worldIn, pos) : SurfaceRest.FLOOR;
    return s.withProperty(BlockCounterPiece.REST, rest);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (state.getPropertyKeys().contains(BlockCounterPiece.REST)) {
      return BBOX.offset(0, -state.getValue(BlockCounterPiece.REST).getDrop() / 16.0, 0);
    }
    return BBOX;
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
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!worldIn.isRemote) {
      // Pass null source so every nearby client (including the activator) hears it
      // positionally; the server is authoritative about the beep.
      SoundEvent beep = FurnishingsSounds.VERIFONE_MX915.getSoundEvent();
      if (beep != null) {
        worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, beep,
            SoundCategory.BLOCKS, 1.0F, 1.0F);
      }
    }
    return true;
  }
}
