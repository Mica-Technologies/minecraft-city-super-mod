package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The shower enclosure: a porcelain tray, glass screens on its two sides and a fixed glass
 * panel at the front beside an open entry, and a rain head on a riser against the wall behind
 * it, two blocks tall and placed and broken as one ({@link BlockResidentialTall}). The glass is
 * a translucent texture, so the block draws in the translucent layer.
 *
 * <p>It can be walked into: only the tray and the glass panes collide, so a player stands in
 * the tray under the rose. Right-click either half turns the water on and off
 * ({@link ShowerSpray}); {@link ShowerSpray#ON} is stored in both halves, in the bit above
 * {@link #UPPER}, and the upper half, which holds the rose, does the spraying.</p>
 *
 * @since 2026.9
 */
public class BlockShower extends BlockResidentialTall {

  /** The top of the tray, in blocks: where the water lands. */
  public static final double TRAY_TOP = 1.5 / 16.0;

  /** The middle of the rose's underside in the upper half, facing north, in sixteenths. */
  private static final double[] ROSE = {8, 9.25, 11};

  /** What collides in the lower half, facing north, in sixteenths: the tray and the glass. */
  private static final AxisAlignedBB[] LOWER = {
      box(0.5, 0, 0.5, 15.5, 1.5, 15.5),
      box(0.5, 1.5, 0.5, 1, 16, 15.5),
      box(15, 1.5, 0.5, 15.5, 16, 15.5),
      box(0.5, 1.5, 0.5, 5, 16, 1)};
  /** What collides in the upper half: the glass, to the top of the screens. */
  private static final AxisAlignedBB[] UPPER_PANES = {
      box(0.5, 0, 0.5, 1, 14, 15.5),
      box(15, 0, 0.5, 15.5, 14, 15.5),
      box(0.5, 0, 0.5, 5, 14, 1)};

  /**
   * Constructs a shower enclosure.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths from the floor (so up to 32)
   */
  public BlockShower(String registryName, int[] box) {
    super(registryName, box, FixtureMaterial.GLASS.getMaterial(),
        FixtureMaterial.GLASS.getSound(), FixtureMaterial.GLASS.getHardness());
    setDefaultState(getDefaultState().withProperty(ShowerSpray.ON, false));
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, UPPER, ShowerSpray.ON);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 7).withProperty(ShowerSpray.ON, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(ShowerSpray.ON) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(ShowerSpray.ON, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      BlockPos lower = state.getValue(UPPER) ? pos.down() : pos;
      BlockPos upper = lower.up();
      boolean on = !state.getValue(ShowerSpray.ON);
      for (BlockPos half : new BlockPos[]{lower, upper}) {
        IBlockState s = world.getBlockState(half);
        if (s.getBlock() == this) {
          world.setBlockState(half, s.withProperty(ShowerSpray.ON, on), 3);
        }
      }
      ShowerSpray.turned(world, upper, on);
      if (on) {
        world.scheduleUpdate(upper, this, ShowerSpray.EVERY);
      }
    }
    return true;
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (world.isRemote || !state.getValue(UPPER) || !state.getValue(ShowerSpray.ON)) {
      return;
    }
    ShowerSpray.spray(world, pos, state.getValue(FACING), ROSE);
    world.scheduleUpdate(pos, this, ShowerSpray.EVERY);
  }

  /** Only the tray and the glass collide, so the shower can be stood in. */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing facing = state.getValue(FACING);
    for (AxisAlignedBB part : state.getValue(UPPER) ? UPPER_PANES : LOWER) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(part, facing));
    }
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockState state,
      @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
    return state.getValue(UPPER) ? null : RotationUtils.rotateBoundingBoxByFacing(LOWER[0],
        state.getValue(FACING));
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}
