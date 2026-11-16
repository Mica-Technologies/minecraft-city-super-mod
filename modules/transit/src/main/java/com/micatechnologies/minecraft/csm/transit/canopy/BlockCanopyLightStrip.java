package com.micatechnologies.minecraft.csm.transit.canopy;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.light.CsmLightupAir;
import com.micatechnologies.minecraft.csm.codeutils.light.ICsmLightupAirSource;
import java.util.Random;
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
 * A continuous LED strip hung under a bus station canopy deck, in the block under it as the
 * {@link BlockCanopyDownlight} is: a slim white diffuser in a steel channel, flush with the
 * soffit, running along {@link #AXIS}. Strips set end to end along the same axis join into one
 * line ({@link #NEG}, {@link #POS}, actual state), with an end cap only where the line stops.
 *
 * <p>Under a shaped tile it is raised to the soffit and turned to lie on it ({@link #SEAT}, one
 * of {@link CanopyStyle#SEATS}), and it runs across the profile, along the ridge or valley line
 * where the soffit is level, whatever axis it stores ({@link CanopyStyle#runAxisUnder}); under a
 * flat deck it runs the way it was placed.</p>
 *
 * <p>It behaves as the downlight does: lit when placed, light 15; a click with an empty hand or a
 * change of redstone power (at the strip, or at the deck above it, so a photocell on the roof
 * works it) switches every light under the same canopy, downlights and strips alike
 * ({@link CanopyLights}). Lit, a strip projects Core's light-up air into the floor below, from
 * every other block of a line (where x + z is even), since a strip light every block would place
 * twice the air for no more light. Stored: {@link #LIT} (1), {@link #POWERED} (2) and the axis
 * (4 for z).</p>
 *
 * @since 2026.11
 */
public class BlockCanopyLightStrip extends AbstractBlock
    implements ICsmLightupAirSource, ICanopyLight {

  public static final PropertyBool LIT = BlockCanopyDownlight.LIT;
  public static final PropertyBool POWERED = BlockCanopyDownlight.POWERED;
  /** The way the strip runs: stored, but under a shaped tile the tile decides (actual). */
  public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockCanopyDeck.AXIS;
  /** The reach and slope it is raised and turned to under a sloped tile (actual). */
  public static final PropertyInteger SEAT =
      PropertyInteger.create("seat", 0, CanopyStyle.SEATS.length - 1);
  /** The line carries on toward the axis's negative end (north or west) (actual). */
  public static final PropertyBool NEG = PropertyBool.create("neg");
  /** The line carries on toward the axis's positive end (south or east) (actual). */
  public static final PropertyBool POS = PropertyBool.create("pos");

  /** What is clicked: a little wider than the strip, so it can be found from the floor. */
  private static final AxisAlignedBB ALONG_X =
      new AxisAlignedBB(0, 14 / 16.0, 5.5 / 16, 1, 1, 10.5 / 16);
  private static final AxisAlignedBB ALONG_Z =
      new AxisAlignedBB(5.5 / 16, 14 / 16.0, 0, 10.5 / 16, 1, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs the light strip.
   *
   * @param registryName its registry name
   */
  public BlockCanopyLightStrip(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 1.5F, 6.0F, 0.0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(LIT, true)
        .withProperty(POWERED, false).withProperty(AXIS, EnumFacing.Axis.X)
        .withProperty(SEAT, 0).withProperty(NEG, false).withProperty(POS, false));
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
    return new CsmBlockStateContainer(this, LIT, POWERED, AXIS, SEAT, NEG, POS);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(LIT, (meta & 1) != 0)
        .withProperty(POWERED, (meta & 2) != 0)
        .withProperty(AXIS, (meta & 4) != 0 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(LIT) ? 1 : 0) | (state.getValue(POWERED) ? 2 : 0)
        | (state.getValue(AXIS) == EnumFacing.Axis.Z ? 4 : 0);
  }

  /** The axis the strip at {@code pos} is drawn along. */
  private static EnumFacing.Axis runAxis(IBlockAccess world, BlockPos pos, IBlockState state) {
    return CanopyStyle.runAxisUnder(world.getBlockState(pos.up()), state.getValue(AXIS));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing.Axis axis = runAxis(world, pos, state);
    EnumFacing neg = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE, axis);
    return state.withProperty(AXIS, axis)
        .withProperty(SEAT, CanopyStyle.seatUnder(world.getBlockState(pos.up())))
        .withProperty(NEG, joins(world, pos.offset(neg), axis))
        .withProperty(POS, joins(world, pos.offset(neg.getOpposite()), axis));
  }

  /** Whether a strip drawn along the same axis is at {@code at}. */
  private boolean joins(IBlockAccess world, BlockPos at, EnumFacing.Axis axis) {
    IBlockState s = world.getBlockState(at);
    return s.getBlock() == this && runAxis(world, at, s) == axis;
  }

  /**
   * Runs the way the placer looks, unless it is set at the end of a strip, whose line it then
   * carries on.
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    for (EnumFacing side : EnumFacing.HORIZONTALS) {
      IBlockState s = world.getBlockState(pos.offset(side));
      if (s.getBlock() == this && s.getValue(AXIS) == side.getAxis()) {
        return getDefaultState().withProperty(AXIS, side.getAxis());
      }
    }
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
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
      CanopyLights.switchCanopy(world, pos, lit);
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
    boolean powered = CanopyLights.powered(world, pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
      CanopyLights.switchCanopy(world, pos, powered);
    }
  }

  /** Sets this strip on or off, with its light-up air. */
  @Override
  public boolean setCanopyLight(World world, BlockPos at, IBlockState state, boolean lit) {
    if (state.getBlock() == this && state.getValue(LIT) != lit) {
      world.setBlockState(at, state.withProperty(LIT, lit), 3);
      if (projects(at)) {
        CsmLightupAir.projectColumn(lit, world, at, 0, 0);
      }
      return true;
    }
    return false;
  }

  // --- light-up air ---------------------------------------------------------------------------

  /** Whether the strip block at {@code pos} projects light-up air: every other block of a line. */
  static boolean projects(BlockPos pos) {
    return ((pos.getX() + pos.getZ()) & 1) == 0;
  }

  /** A strip arriving lit places its light-up air a tick later, once the floor is there. */
  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote && state.getValue(LIT) && projects(pos)) {
      world.scheduleUpdate(pos, this, 2);
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && state.getValue(LIT) && projects(pos)) {
      CsmLightupAir.projectColumn(true, world, pos, 0, 0);
    }
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (state.getValue(LIT) && projects(pos)) {
      CsmLightupAir.projectColumn(false, world, pos, 0, 0);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public boolean claimsLightupAir(World world, BlockPos pos, IBlockState state, BlockPos air) {
    return state.getValue(LIT) && projects(pos) && air.getX() == pos.getX()
        && air.getZ() == pos.getZ() && air.getY() < pos.getY();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState here = source.getBlockState(pos);
    IBlockState s = here.getBlock() == this ? here : state;
    return runAxis(source, pos, s) == EnumFacing.Axis.X ? ALONG_X : ALONG_Z;
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
