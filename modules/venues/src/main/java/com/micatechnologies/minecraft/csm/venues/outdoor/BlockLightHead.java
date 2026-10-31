package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.light.CsmLightFootprints;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A stadium light tower's head ({@code stadium_light_head}, five floodlights across and four
 * rows up; {@code stadium_light_head_small}, three across and two up): banks of LED floodlights
 * on a steel frame, aimed down at the field, on top of a {@link BlockLightPole} shaft. A
 * {@link BlockVenueUnit} drawn whole by its master, the bottom middle cell, which sits on the
 * shaft's top.
 *
 * <p><b>Light.</b> Lit, the master gives light level 15 and the lenses are drawn bright (with an
 * OptiFine {@code _e} glow), and the head lights a footprint of the field in front of the tower
 * with Core's light-up air: a grid of {@link #gridCount()} x {@link #gridCount()} columns every
 * {@link #GRID_SPACING} blocks, centred {@link #gridDistance()} blocks out along the facing
 * ({@link TileEntityLightHead}). Core's {@link CsmLightFootprints} places and removes them a few
 * a tick, never loading a chunk, and the head only ever removes the ones it placed.</p>
 *
 * <p><b>Switching.</b> A click with an empty hand switches it. Redstone at the master switches
 * it too, on the signal's edges: power arriving lights it, power going darkens it, and a click in
 * between still works. It heals as Lighting's fixtures do: when it is placed, when its chunk
 * loads and on random ticks, a lit head fills any column left dark and a dark one clears what it
 * left.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the floodlights aim;
 * bit 2 lit. Placed by hand it is lit and aims at the placer.</p>
 *
 * @since 2026.10
 */
public class BlockLightHead extends BlockVenueUnit implements ICsmTileEntityProvider {

  /** Switched on. Stored, bit 2. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  /** Blocks between two columns of a head's footprint. */
  public static final int GRID_SPACING = 3;

  /**
   * Constructs a head.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockLightHead(String registryName, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(LIT, true));
    // The heal: a random tick (routed to updateTick) brings the footprint in line with the state
    setTickRandomly(true);
  }

  /** Whether this is the large head (three rows of floodlights over the master) or the small. */
  private boolean isLarge() {
    return topLayer() >= 2;
  }

  /**
   * Columns along each side of the footprint.
   *
   * @return 9 for the large head, 5 for the small
   */
  public int gridCount() {
    return isLarge() ? 9 : 5;
  }

  /**
   * How far out along the facing the footprint's centre is, in blocks.
   *
   * @return 16 for the large head, 9 for the small
   */
  public int gridDistance() {
    return isLarge() ? 16 : 9;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LIT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(LIT, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIT) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(LIT, true);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? 15 : 0;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      boolean lit = !state.getValue(LIT);
      setLit(world, pos, state, lit);
      world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5F,
          lit ? 1.1F : 0.8F);
    }
    return true;
  }

  private void setLit(World world, BlockPos pos, IBlockState state, boolean lit) {
    world.setBlockState(pos, state.withProperty(LIT, lit), 3);
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityLightHead) {
      CsmLightFootprints.request((TileEntityLightHead) te);
    }
  }

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      // The tile entity is made after this returns; a paste has its floor in place a tick later
      world.scheduleUpdate(pos, this, 2);
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    super.updateTick(world, pos, state, rand);
    if (world.isRemote) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityLightHead) {
      TileEntityLightHead head = (TileEntityLightHead) te;
      if (state.getValue(LIT) || head.hasPlacedLight()) {
        CsmLightFootprints.request(head);
      }
    }
  }

  /** Redstone switches the head on the signal's edges, so a click is not undone. */
  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    if (world.isRemote) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityLightHead)) {
      return;
    }
    TileEntityLightHead head = (TileEntityLightHead) te;
    boolean powered = world.isBlockPowered(pos);
    if (powered != head.wasPowered()) {
      head.setPowered(powered);
      if (state.getValue(LIT) != powered) {
        setLit(world, pos, state, powered);
      }
    }
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityLightHead) {
        CsmLightFootprints.released((TileEntityLightHead) te);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityLightHead.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityvenuelighthead";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityLightHead();
  }
}
