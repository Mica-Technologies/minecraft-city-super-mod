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
 * A downlight hung under a bus station canopy deck: it stands in the block under the deck,
 * flush with the soffit, raised into the deck's block and tilted to lie on a sloped tile's
 * soffit ({@link #REACH}, {@link #TILT}). It is lit when placed. A click with an empty hand,
 * or a change of redstone power (on when power comes, off when it goes), switches every
 * light under the same canopy, downlights and light strips alike ({@link CanopyLights}): the
 * decks joined to the one above this light, walked side to side up to {@link #WALK_LIMIT} tiles. Power at the deck above counts too
 * ({@link BlockCanopyDeck} passes its neighbour changes down), so an inverted daylight sensor set
 * on the roof works the canopy's lights as a photocell does. Stored: {@link #LIT} (1) and
 * {@link #POWERED} (2), the power it last saw.
 *
 * <p>Lit, it projects Core's light-up air into the floor below it, as the street lights do
 * ({@link CsmLightupAir#projectColumn}): a canopy is 4 to 12 blocks up, and its own light 15
 * would reach the pavement at a third of that. A light placed lit (by the tool, a paste or a
 * command) places its air on the tick after it arrives.</p>
 *
 * @since 2026.11
 */
public class BlockCanopyDownlight extends AbstractBlock
    implements ICsmLightupAirSource, ICanopyLight {

  public static final PropertyBool LIT = PropertyBool.create("lit");
  public static final PropertyBool POWERED = PropertyBool.create("powered");
  /** How far up into the deck's block the light is raised to meet a sloped soffit (actual). */
  public static final PropertyInteger REACH =
      PropertyInteger.create("reach", 0, CanopyStyle.REACH_PX.length - 1);
  /** Tilted to lie flush on a sloped soffit (actual): {@link CanopyStyle#tiltUnder}. */
  public static final PropertyInteger TILT = PropertyInteger.create("tilt", 0, 8);

  /** The most deck tiles a switch walks. */
  public static final int WALK_LIMIT = CanopyLights.WALK_LIMIT;

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
        .withProperty(POWERED, false).withProperty(REACH, 0).withProperty(TILT, 0));
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
    return new CsmBlockStateContainer(this, LIT, POWERED, REACH, TILT);
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
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState above = world.getBlockState(pos.up());
    return state.withProperty(REACH, CanopyStyle.reachUnder(above))
        .withProperty(TILT, CanopyStyle.tiltUnder(above));
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
    // power at the light, or at the deck it hangs from: a photocell (an inverted daylight
    // sensor) set on the canopy's roof switches the lights under it
    boolean powered = CanopyLights.powered(world, pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
      CanopyLights.switchCanopy(world, pos, powered);
    }
  }

  /** Sets this light on or off, with its light-up air. */
  @Override
  public boolean setCanopyLight(World world, BlockPos at, IBlockState state, boolean lit) {
    if (state.getBlock() == this && state.getValue(LIT) != lit) {
      world.setBlockState(at, state.withProperty(LIT, lit), 3);
      CsmLightupAir.projectColumn(lit, world, at, 0, 0);
      return true;
    }
    return false;
  }

  // --- light-up air ---------------------------------------------------------------------------

  /** A light arriving lit places its light-up air a tick later, once the floor is there. */
  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote && state.getValue(LIT)) {
      world.scheduleUpdate(pos, this, 2);
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && state.getValue(LIT)) {
      CsmLightupAir.projectColumn(true, world, pos, 0, 0);
    }
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (state.getValue(LIT)) {
      CsmLightupAir.projectColumn(false, world, pos, 0, 0);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public boolean claimsLightupAir(World world, BlockPos pos, IBlockState state, BlockPos air) {
    return state.getValue(LIT) && air.getX() == pos.getX() && air.getZ() == pos.getZ()
        && air.getY() < pos.getY();
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
