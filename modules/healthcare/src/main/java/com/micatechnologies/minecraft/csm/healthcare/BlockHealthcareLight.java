package com.micatechnologies.minecraft.csm.healthcare;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
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
 * A wall light on the Lighting module's conventions: the bed's reading light, and the base of
 * the nurse call dome light.
 *
 * <p>Metadata is {@code facing * 4 + state}, the facing's horizontal index (S 0, W 1, N 2, E 3)
 * as Lighting's {@code AbstractBrightLight} stores it, and the state one of Lighting's four:
 * 0 off under redstone control, 1 on under redstone control, 2 off by hand, 3 on by hand. A
 * generator places a light lit with {@code facing * 4 + 3}. Redstone switches the light in states
 * 0 and 1; a click goes 0/1 to 2, 2 to 3, and 3 back to redstone control, as Lighting's does.</p>
 *
 * <p>A light here lights its own cell ({@link #getLightValue}); it places no light-up air below
 * it, since a reading light or a dome light lights the wall it is on and not a street below.
 * (The light-up air block is the Lighting module's, which this module cannot name.)</p>
 *
 * @since 2026.10
 */
public class BlockHealthcareLight extends AbstractBlockRotatableNSEW {

  /** Lighting's four states. */
  public static final PropertyInteger STATE = PropertyInteger.create("state", 0, 3);

  public static final int RS_OFF = 0;
  public static final int RS_ON = 1;
  public static final int MAN_OFF = 2;
  public static final int MAN_ON = 3;

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;
  private final int lightLevel;

  /**
   * Constructs a light.
   *
   * @param registryName its registry name
   * @param box          its box facing north (on a wall at +Z), in sixteenths
   * @param lightLevel   the light it gives when lit, 0-15
   */
  public BlockHealthcareLight(String registryName, int[] box, int lightLevel) {
    super(stash(registryName), HealthcareMaterial.PLASTIC.getSound(), "pickaxe", 0, 1.0F, 2.0F,
        0.0F, 0, false);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    this.lightLevel = lightLevel;
    setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(STATE, RS_OFF));
    PENDING.remove();
  }

  private static net.minecraft.block.material.Material stash(String registryName) {
    PENDING.set(registryName);
    return HealthcareMaterial.PLASTIC.getMaterial();
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, STATE);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta >> 2 & 3))
        .withProperty(STATE, meta & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() * 4 + state.getValue(STATE);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing front = facing.getAxis().isHorizontal()
        ? facing : placer.getHorizontalFacing().getOpposite();
    int state = world.isBlockPowered(pos) ? RS_ON : RS_OFF;
    return getDefaultState().withProperty(FACING, front).withProperty(STATE, state);
  }

  /**
   * Whether a state is lit.
   *
   * @param state a state of this block
   *
   * @return true in states 1 and 3
   */
  public static boolean isLit(IBlockState state) {
    int s = state.getValue(STATE);
    return s == RS_ON || s == MAN_ON;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    // The state given, not the world's: vanilla compares the old state's light with the new
    // one's after the new one is already in the world, so reading the world would always match.
    return state.getBlock() == this && isLit(state) ? lightLevel : 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (world.isRemote) {
      return;
    }
    int s = state.getValue(STATE);
    if (s == RS_OFF || s == RS_ON) {
      int want = world.isBlockPowered(pos) ? RS_ON : RS_OFF;
      if (want != s) {
        world.setBlockState(pos, state.withProperty(STATE, want), 3);
      }
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      int s = state.getValue(STATE);
      int next;
      if (s == RS_OFF || s == RS_ON) {
        next = MAN_OFF;
      } else if (s == MAN_OFF) {
        next = MAN_ON;
      } else {
        next = world.isBlockPowered(pos) ? RS_ON : RS_OFF;
      }
      world.setBlockState(pos, state.withProperty(STATE, next), 3);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
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
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
