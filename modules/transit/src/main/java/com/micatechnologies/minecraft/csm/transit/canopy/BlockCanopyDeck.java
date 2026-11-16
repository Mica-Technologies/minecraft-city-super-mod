package com.micatechnologies.minecraft.csm.transit.canopy;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A tile of bus station canopy, drawn by {@code gen_transit_canopies.py}, in one
 * {@link CanopyStyle} and one agency's livery. Tiles are set at the clearance wanted, on
 * {@link BlockCanopyColumn}s, usually by the {@link ItemCanopyTool}; the deck lies at the foot of
 * the block, so its underside is the clearance line and a deck set on a column rests on it.
 *
 * <p>Tiles of the same block join on all four world sides ({@link #NORTH} to {@link #WEST},
 * actual state), and the fascia is drawn only round the outside. A shaped style's tile also
 * stores its {@link #PHASE}, its place in the style's module, and {@link #AXIS}, which way the
 * profile runs (meta: phase, plus 8 for axis z). Placed by hand it takes both from a tile of the
 * same block next to it along the axis the placer faces; a sneaking click with an empty hand
 * steps its phase on.</p>
 *
 * @since 2026.11
 */
public class BlockCanopyDeck extends AbstractBlock implements ICsmGlassFronted {

  public static final PropertyEnum<EnumFacing.Axis> AXIS =
      PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
  public static final PropertyInteger PHASE = PropertyInteger.create("phase", 0, 5);
  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");

  /** What a player stands on or bumps: the deck's slab at the foot of the block. */
  private static final AxisAlignedBB DECK = new AxisAlignedBB(0, 0, 0, 1, 3 / 16.0, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final CanopyStyle style;

  /**
   * Constructs a deck.
   *
   * @param registryName its registry name
   * @param style        its style
   */
  public BlockCanopyDeck(String registryName, CanopyStyle style) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.5F, 8.0F, 0.0F, 0);
    this.registryName = registryName;
    this.style = style;
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X)
        .withProperty(PHASE, 0).withProperty(NORTH, false).withProperty(EAST, false)
        .withProperty(SOUTH, false).withProperty(WEST, false));
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

  public CanopyStyle getStyle() {
    return style;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, PHASE, NORTH, EAST, SOUTH, WEST);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(PHASE, Math.min(5, meta & 7))
        .withProperty(AXIS, (meta & 8) != 0 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(PHASE) | (state.getValue(AXIS) == EnumFacing.Axis.Z ? 8 : 0);
  }

  /**
   * A state of this deck at a phase, along an axis; a flat style keeps both at their defaults.
   *
   * @param axis  which way the profile runs
   * @param phase the tile's place in the module
   *
   * @return the state
   */
  public IBlockState stateFor(EnumFacing.Axis axis, int phase) {
    if (style == null || !style.isShaped()) {
      return getDefaultState();
    }
    return getDefaultState().withProperty(AXIS, axis)
        .withProperty(PHASE, Math.floorMod(phase, style.getModule()));
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    if (style == null || !style.isShaped()) {
      return getDefaultState();
    }
    EnumFacing.Axis axis = placer.getHorizontalFacing().getAxis();
    EnumFacing back = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE, axis);
    for (int sign : new int[]{1, -1}) {
      BlockPos at = sign > 0 ? pos.offset(back) : pos.offset(back.getOpposite());
      IBlockState s = world.getBlockState(at);
      if (s.getBlock() == this && s.getValue(AXIS) == axis) {
        return stateFor(axis, s.getValue(PHASE) + sign);
      }
    }
    // a tile beside one along the other axis carries on its phase and axis
    for (EnumFacing side : EnumFacing.HORIZONTALS) {
      IBlockState s = world.getBlockState(pos.offset(side));
      if (s.getBlock() == this && s.getValue(AXIS) != side.getAxis()) {
        return stateFor(s.getValue(AXIS), s.getValue(PHASE));
      }
    }
    return stateFor(axis, 0);
  }

  /** A sneaking click with an empty hand steps a shaped tile's phase on. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (style == null || !style.isShaped() || hand != EnumHand.MAIN_HAND
        || !player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int phase = (state.getValue(PHASE) + 1) % style.getModule();
      world.setBlockState(pos, state.withProperty(PHASE, phase), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.canopy.phase",
          phase + 1, style.getModule()), true);
    }
    return true;
  }

  /**
   * Passes a neighbour change down to a downlight hung under this tile, so power at the deck
   * (a photocell on the roof) reaches it.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    BlockPos below = pos.down();
    if (!world.isRemote && !below.equals(fromPos)
        && world.getBlockState(below).getBlock() instanceof BlockCanopyDownlight) {
      world.neighborChanged(below, this, pos);
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NORTH, joins(world, pos.north()))
        .withProperty(EAST, joins(world, pos.east()))
        .withProperty(SOUTH, joins(world, pos.south()))
        .withProperty(WEST, joins(world, pos.west()));
  }

  private boolean joins(IBlockAccess world, BlockPos at) {
    return world.getBlockState(at).getBlock() == this;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return style != null && style.isShaped() ? FULL_BLOCK_AABB : DECK;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return DECK;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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
    return style != null ? style.getLayer() : BlockRenderLayer.CUTOUT;
  }

  /** A glazed deck draws its glass in the translucent pass and its steel in the cutout one. */
  @Override
  public boolean isGlassFronted() {
    return getBlockRenderLayer() == BlockRenderLayer.TRANSLUCENT;
  }
}
