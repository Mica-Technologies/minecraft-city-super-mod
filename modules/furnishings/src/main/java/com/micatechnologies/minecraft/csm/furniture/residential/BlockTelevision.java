package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.furniture.residential.WidePieces.IWidePiece;
import com.micatechnologies.minecraft.csm.furniture.residential.WidePieces.ItemWidePiece;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * A TV: a flat screen on its stand, a flat screen hung on the wall, either one block or two
 * wide, or an old tube TV. Right-click changes the channel ({@link TvChannel}: off, news, sports,
 * nature, the colour bars, snow), each an animated texture the blockstate picks for the screen;
 * the channel is kept by a {@link TileEntityTelevision} in each of its blocks, and a click on
 * either block of a two-block TV changes both.
 *
 * <p>A TV on a stand stands on whatever is under it, as the counter pieces do ({@link #REST}),
 * so on a TV stand or a sideboard it stands on the top, not above it. A wall TV hangs at the
 * height it is drawn in its block and ignores what is under it. A two-block TV is placed and
 * broken as one ({@link WidePieces}); a one-block TV is always {@link WidePieces#PART} 0.</p>
 *
 * <p>The facing and {@link WidePieces#PART} are stored; {@link #REST} and {@link #CHANNEL} are
 * actual state.</p>
 *
 * @since 2026.9
 */
public class BlockTelevision extends BlockCounterPiece
    implements ICsmTileEntityProvider, IWidePiece {

  /** What the screen shows. */
  public static final PropertyEnum<TvChannel> CHANNEL =
      PropertyEnum.create("channel", TvChannel.class);

  private final boolean wide;
  private final boolean wall;
  private final AxisAlignedBB[] boxes;

  /**
   * Constructs a TV.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor (or on the wall), in
   *                     sixteenths; for a two-block TV across both blocks, x 0 to 32
   * @param wide         whether it is two blocks wide
   * @param wall         whether it hangs on the wall rather than standing
   */
  public BlockTelevision(String registryName, int[] box, boolean wide, boolean wall) {
    super(registryName, box, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID);
    this.wide = wide;
    this.wall = wall;
    this.boxes = new AxisAlignedBB[]{WidePieces.cellBox(box, 0), WidePieces.cellBox(box, 1)};
    setDefaultState(getDefaultState().withProperty(WidePieces.PART, 0)
        .withProperty(CHANNEL, TvChannel.OFF));
  }

  @Override
  public boolean isWide() {
    return wide;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, REST, WidePieces.PART, CHANNEL);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(WidePieces.PART, (meta >> 2) & 1);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(WidePieces.PART) << 2);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos).withProperty(CHANNEL,
        channelAt(world, pos));
    return wall ? s.withProperty(REST, SurfaceRest.FLOOR) : s;
  }

  /**
   * The channel of the TV at {@code pos}: off if its tile entity is not there. Safe off the main
   * thread: a chunk being rendered is read without creating a tile entity in it.
   *
   * @param world the world
   * @param pos   the TV
   *
   * @return its channel
   */
  public static TvChannel channelAt(IBlockAccess world, BlockPos pos) {
    TileEntity te = world instanceof ChunkCache
        ? ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK)
        : world.getTileEntity(pos);
    return te instanceof TileEntityTelevision ? ((TileEntityTelevision) te).getChannel()
        : TvChannel.OFF;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = boxes[state.getValue(WidePieces.PART)];
    if (!wall && state.getPropertyKeys().contains(REST)) {
      return box.offset(0, -state.getValue(REST).getDrop() / 16.0, 0);
    }
    return box;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      TvChannel next = channelAt(world, pos).next();
      setChannel(world, pos, next);
      if (wide) {
        BlockPos other = WidePieces.otherCell(pos, state);
        if (WidePieces.isOtherCell(world, other, state)) {
          setChannel(world, other, next);
        }
      }
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, next == TvChannel.OFF ? 0.6F : 0.8F);
    }
    return true;
  }

  private static void setChannel(World world, BlockPos pos, TvChannel channel) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityTelevision) {
      ((TileEntityTelevision) te).setChannel(channel);
    }
  }

  // --- a two-block TV is placed and broken as one ------------------------------------------

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemWidePiece(this);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(WidePieces.PART, 0);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (wide) {
      WidePieces.placeOther(world, pos, state);
    }
  }

  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (wide) {
      WidePieces.harvested(world, pos, state, player);
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (wide) {
      WidePieces.broken(world, pos, state);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (wide) {
      WidePieces.checkOther(world, pos, state);
    }
    super.neighborChanged(state, world, pos, block, fromPos);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(WidePieces.PART) == 1 ? Items.AIR
        : super.getItemDropped(state, rand, fortune);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return wide ? EnumPushReaction.BLOCK : super.getPushReaction(state);
  }

  // --- the channel -------------------------------------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTelevision.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitytelevision";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityTelevision();
  }
}
