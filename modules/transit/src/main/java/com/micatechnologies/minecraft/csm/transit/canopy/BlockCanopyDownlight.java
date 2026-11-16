package com.micatechnologies.minecraft.csm.transit.canopy;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A downlight hung under a bus station canopy deck: it stands in the block under the deck,
 * flush with the soffit. It is lit when placed. A click with an empty hand, or a change of
 * redstone power (on when power comes, off when it goes), switches every downlight under the
 * same canopy: the decks joined to the one above this light, walked side to side up to
 * {@link #REACH} tiles. Stored: {@link #LIT} (1) and {@link #POWERED} (2), the power it last saw.
 *
 * @since 2026.11
 */
public class BlockCanopyDownlight extends AbstractBlock {

  public static final PropertyBool LIT = PropertyBool.create("lit");
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** The most deck tiles a switch walks. */
  public static final int REACH = 4096;

  private static final AxisAlignedBB PUCK =
      new AxisAlignedBB(4.5 / 16, 14.75 / 16, 4.5 / 16, 11.5 / 16, 1, 11.5 / 16);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs the downlight.
   *
   * @param registryName its registry name
   */
  public BlockCanopyDownlight(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 1.5F, 6.0F, 0.0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(LIT, true)
        .withProperty(POWERED, false));
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, LIT, POWERED);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(LIT, (meta & 1) != 0)
        .withProperty(POWERED, (meta & 2) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(LIT) ? 1 : 0) | (state.getValue(POWERED) ? 2 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState();
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? 15 : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || player.isSneaking()
        || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      boolean lit = !state.getValue(LIT);
      switchCanopy(world, pos, lit);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, lit ? 0.6F : 0.5F);
    }
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
      switchCanopy(world, pos, powered);
    }
  }

  /** Sets every downlight under the canopy over {@code light}, or this one alone without one. */
  private void switchCanopy(World world, BlockPos light, boolean lit) {
    set(world, light, lit);
    BlockPos start = light.up();
    if (!(world.getBlockState(start).getBlock() instanceof BlockCanopyDeck)) {
      return;
    }
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> open = new ArrayDeque<>();
    open.add(start);
    seen.add(start);
    while (!open.isEmpty() && seen.size() <= REACH) {
      BlockPos at = open.poll();
      set(world, at.down(), lit);
      for (EnumFacing side : EnumFacing.HORIZONTALS) {
        BlockPos next = at.offset(side);
        if (!seen.contains(next) && world.isBlockLoaded(next)
            && world.getBlockState(next).getBlock() instanceof BlockCanopyDeck) {
          seen.add(next);
          open.add(next);
        }
      }
    }
  }

  private void set(World world, BlockPos at, boolean lit) {
    IBlockState s = world.getBlockState(at);
    if (s.getBlock() == this && s.getValue(LIT) != lit) {
      world.setBlockState(at, s.withProperty(LIT, lit), 3);
    }
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return PUCK;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
