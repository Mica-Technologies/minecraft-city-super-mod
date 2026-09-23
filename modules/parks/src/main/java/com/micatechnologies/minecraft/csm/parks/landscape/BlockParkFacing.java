package com.micatechnologies.minecraft.csm.parks.landscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPoleFitted;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A landscape block that faces something behind it: a tree stake, whose tie reaches back to the
 * trunk in the next cell, and a hanging flower basket on a bracket, whose plate meets the pole or
 * wall behind it ({@link PoleFitted}). The model faces north with what it attaches to at +Z,
 * as every side-mounted accessory in the mod does.
 *
 * @since 2026.9
 */
public class BlockParkFacing extends AbstractBlockRotatableNSEW {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;
  private final boolean collides;

  /**
   * Constructs a facing prop.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   * @param collides     whether it can be walked into
   */
  public BlockParkFacing(String registryName, int[] box, boolean collides) {
    super(stash(registryName), SoundType.WOOD, "axe", 0, 1.0F, 2.0F, 0.0F, 0);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    this.collides = collides;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return collides ? getBoundingBox(state, source, pos) : NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return !collides;
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  /**
   * A tree stake: its tie reaches back (+Z in the model) to the trunk in the next cell, so it
   * turns to face away from a neighbouring log when it is placed, whichever way the player is
   * looking. With no log beside it, it faces the player like any other facing prop.
   */
  public static class TreeStake extends BlockParkFacing {

    public TreeStake(String registryName, int[] box, boolean collides) {
      super(registryName, box, collides);
    }

    @Override
    @Nonnull
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(@Nonnull World world, @Nonnull BlockPos pos,
        @Nonnull EnumFacing side, float hitX, float hitY, float hitZ, int meta,
        @Nonnull EntityLivingBase placer) {
      IBlockState state = super.getStateForPlacement(world, pos, side, hitX, hitY, hitZ, meta,
          placer);
      // Prefer the trunk the player is looking toward, then any trunk beside it.
      EnumFacing look = placer.getHorizontalFacing();
      if (isTrunk(world, pos.offset(look))) {
        return state.withProperty(FACING, look.getOpposite());
      }
      for (EnumFacing f : EnumFacing.HORIZONTALS) {
        if (isTrunk(world, pos.offset(f))) {
          return state.withProperty(FACING, f.getOpposite());
        }
      }
      return state;
    }

    private static boolean isTrunk(World world, BlockPos pos) {
      Block block = world.getBlockState(pos).getBlock();
      return block instanceof BlockTreeLog || block instanceof BlockLog;
    }
  }

  /**
   * A facing prop whose bracket plate is drawn to meet the pole behind it, so a basket hung on a
   * thin or pedestal pole does not float beside it ({@link ICsmPoleFitted}).
   */
  public static class PoleFitted extends BlockParkFacing implements ICsmPoleFitted {

    public PoleFitted(String registryName, int[] box, boolean collides) {
      super(registryName, box, collides);
    }
  }
}
