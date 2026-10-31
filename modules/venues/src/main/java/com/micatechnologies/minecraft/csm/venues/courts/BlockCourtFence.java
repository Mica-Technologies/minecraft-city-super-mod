package com.micatechnologies.minecraft.csm.venues.courts;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A court's chain-link fence ({@code court_fence}) and an indoor court's divider curtain
 * ({@code court_divider_curtain}): a post in the middle of the block and a panel to every side
 * that meets the same block or a solid face, as a pane joins. They stack: the top course
 * (nothing of the same block above) has the top rail. A chain-link fence may wear a windscreen,
 * drawn on the court side of the mesh; the divider is a solid vinyl course at the bottom and mesh
 * above. The chain-link's look is Building's, drawn again here (no dependency).
 *
 * <p><b>Metadata</b>: the windscreen's (or the curtain's) colour: fence 0 none, 1 green, 2 blue,
 * 3 black; curtain 0 green, 1 blue, 2 black, 3 grey. A sneak-click with an empty hand steps it.
 * Actual state: {@code north}, {@code south}, {@code east}, {@code west}, {@code top}.</p>
 *
 * @since 2026.10
 */
public class BlockCourtFence extends AbstractBlock {

  /** The registry name, for the base constructor, which asks for it before fields are set. */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  public static final PropertyInteger COLOUR = PropertyInteger.create("colour", 0, 3);
  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyBool TOP = PropertyBool.create("top");

  private final boolean curtain;
  private final String registryName;

  /**
   * Constructs a fence.
   *
   * @param registryName its registry name
   * @param curtain      the divider curtain rather than the chain-link
   */
  public BlockCourtFence(String registryName, boolean curtain) {
    super(stash(registryName, curtain), curtain ? SoundType.CLOTH : SoundType.METAL, "pickaxe",
        0, 1.5F, 3.0F, 0.0F, 0);
    this.curtain = curtain;
    this.registryName = registryName;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(COLOUR, 0).withProperty(NORTH, false)
        .withProperty(SOUTH, false).withProperty(EAST, false).withProperty(WEST, false)
        .withProperty(TOP, true));
  }

  private static Material stash(String registryName, boolean curtain) {
    PENDING.set(registryName);
    return curtain ? Material.CLOTH : Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
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

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, COLOUR, NORTH, SOUTH, EAST, WEST, TOP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(COLOUR, meta & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(COLOUR);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState();
  }

  private boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos at = pos.offset(side);
    IBlockState s = world.getBlockState(at);
    if (s.getBlock() == this) {
      return true;
    }
    return s.getBlockFaceShape(world, at, side.getOpposite()) == BlockFaceShape.SOLID;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NORTH, joins(world, pos, EnumFacing.NORTH))
        .withProperty(SOUTH, joins(world, pos, EnumFacing.SOUTH))
        .withProperty(EAST, joins(world, pos, EnumFacing.EAST))
        .withProperty(WEST, joins(world, pos, EnumFacing.WEST))
        .withProperty(TOP, world.getBlockState(pos.up()).getBlock() != this);
  }

  /** The boxes of the post and each joined panel. */
  private List<AxisAlignedBB> boxes(IBlockState actual, double top) {
    List<AxisAlignedBB> out = new ArrayList<>();
    out.add(new AxisAlignedBB(0.44, 0, 0.44, 0.56, top, 0.56));
    if (actual.getValue(NORTH)) {
      out.add(new AxisAlignedBB(0.47, 0, 0, 0.53, top, 0.5));
    }
    if (actual.getValue(SOUTH)) {
      out.add(new AxisAlignedBB(0.47, 0, 0.5, 0.53, top, 1));
    }
    if (actual.getValue(WEST)) {
      out.add(new AxisAlignedBB(0, 0, 0.47, 0.5, top, 0.53));
    }
    if (actual.getValue(EAST)) {
      out.add(new AxisAlignedBB(0.5, 0, 0.47, 1, top, 0.53));
    }
    return out;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState a = getActualState(state, source, pos);
    AxisAlignedBB u = null;
    for (AxisAlignedBB b : boxes(a, 1.0)) {
      u = u == null ? b : u.union(b);
    }
    return u;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull AxisAlignedBB entityBox, @Nonnull List<AxisAlignedBB> collidingBoxes,
      @Nullable Entity entity, boolean isActualState) {
    IBlockState a = isActualState ? state : getActualState(state, world, pos);
    double top = a.getValue(TOP) ? 1.5 : 1.0;
    for (AxisAlignedBB b : boxes(a, top)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, b);
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int c = (state.getValue(COLOUR) + 1) & 3;
      world.setBlockState(pos, state.withProperty(COLOUR, c), 3);
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.venues.courts." + (curtain ? "curtain" : "screen") + "." + c), true);
    }
    return true;
  }
}
