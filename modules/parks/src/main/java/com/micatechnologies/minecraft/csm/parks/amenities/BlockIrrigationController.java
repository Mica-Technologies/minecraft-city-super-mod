package com.micatechnologies.minecraft.csm.parks.amenities;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An irrigation controller: a wall box that powers redstone during the morning watering window
 * (5 to 7 in the game's morning), when a real one runs its zones before the parks open.
 * Right-click it to switch the manual override on or off, which runs the water now whatever the
 * time. The green lamp shows when it is watering.
 *
 * <p>It polls the world clock every two seconds by scheduled tick; nothing is stored but the
 * two bits in its metadata.</p>
 *
 * @since 2026.9
 */
public class BlockIrrigationController extends AbstractBlockRotatableNSEW {

  /** Powering its outputs now. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");
  /** The manual override is on. */
  public static final PropertyBool MANUAL = PropertyBool.create("manual");

  /** Game time of 5:00 in the morning (0 is 6:00). */
  static final long WINDOW_START = 23000;
  /** Game time of 7:00 in the morning. */
  static final long WINDOW_END = 1000;
  private static final int POLL_TICKS = 40;

  private static final AxisAlignedBB BOX = new AxisAlignedBB(4 / 16.0, 3 / 16.0, 12 / 16.0,
      12 / 16.0, 13 / 16.0, 1);

  public BlockIrrigationController() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 2.0F, 4.0F, 0.0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POWERED, false).withProperty(MANUAL, false));
  }

  @Override
  public String getBlockRegistryName() {
    return "irrigation_controller";
  }

  /** Whether a world time falls in the morning watering window. */
  static boolean inWindow(long worldTime) {
    long t = Math.floorMod(worldTime, 24000L);
    return t >= WINDOW_START || t < WINDOW_END;
  }

  // --- state ---

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POWERED, MANUAL);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(POWERED) ? 4 : 0)
        | (state.getValue(MANUAL) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(POWERED, (meta & 4) != 0).withProperty(MANUAL, (meta & 8) != 0);
  }

  // --- the schedule ---

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    if (!world.isRemote) {
      world.scheduleUpdate(pos, this, POLL_TICKS);
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    refresh(world, pos, state);
    world.scheduleUpdate(pos, this, POLL_TICKS);
  }

  private void refresh(World world, BlockPos pos, IBlockState state) {
    boolean on = state.getValue(MANUAL) || inWindow(world.getWorldTime());
    if (on != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, on), 3);
      world.notifyNeighborsOfStateChange(pos, this, false);
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (!world.isRemote) {
      IBlockState next = state.withProperty(MANUAL, !state.getValue(MANUAL));
      world.setBlockState(pos, next, 3);
      refresh(world, pos, next);
      player.sendStatusMessage(new TextComponentTranslation(next.getValue(MANUAL)
          ? "csm.parks.irrigation.manual_on" : "csm.parks.irrigation.manual_off"), true);
    }
    return true;
  }

  // --- redstone out ---

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
    return state.getValue(POWERED) ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  // --- shape ---

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
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
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
