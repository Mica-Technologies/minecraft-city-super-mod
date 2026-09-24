package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A French-door refrigerator, two blocks tall: placed as one piece, as a door is, into its block
 * and the one above it, and broken as one piece from either. The lower half ({@link #UPPER}
 * false) holds the tile entity and its 27 slots; right-clicking either half opens them, with
 * the sound of the door seal. Only the lower half drops the refrigerator.
 *
 * <p>The halves are drawn by {@code gen_furniture_kitchen.py} from one model cut at the block
 * line, so each is lit in its own block. {@link #UPPER} is stored, in the bit above the
 * facing.</p>
 *
 * @since 2026.9
 */
public class BlockRefrigerator extends BlockResidentialFurniture
    implements ICsmTileEntityProvider, IResidentialStorage {

  /** Whether this is the upper half. */
  public static final PropertyBool UPPER = PropertyBool.create("upper");

  /** Slots: three rows, a chest's worth. */
  private static final int SLOTS = 27;
  private static final int[] BOX = {0, 0, 1, 16, 16, 16};
  private static final AxisAlignedBB UPPER_BOX_NORTH =
      new AxisAlignedBB(0, 0, 1 / 16.0, 1, 13 / 16.0, 1);

  /**
   * Constructs a refrigerator.
   *
   * @param registryName its registry name, ending in its finish
   */
  public BlockRefrigerator(String registryName) {
    super(registryName, BOX, false);
    setDefaultState(getDefaultState().withProperty(UPPER, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, UPPER);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(UPPER, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(UPPER) ? 4 : 0);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(UPPER) ? UPPER_BOX_NORTH : super.getBlockBoundingBox(state, source,
        pos);
  }

  @Override
  public int getSlots() {
    return SLOTS;
  }

  @Override
  public BlockPos getStoragePos(IBlockAccess world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() == this && state.getValue(UPPER) ? pos.down() : pos;
  }

  @Nullable
  @Override
  public ICsmSound getOpenSound() {
    return FurnishingsSounds.FRIDGE_OPEN;
  }

  @Nullable
  @Override
  public ICsmSound getCloseSound() {
    return FurnishingsSounds.FRIDGE_CLOSE;
  }

  // --- placing and breaking as one piece ---------------------------------------------------

  @Override
  public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos) {
    return pos.getY() < world.getHeight() - 1 && super.canPlaceBlockAt(world, pos)
        && world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up());
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(UPPER, false);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    world.setBlockState(pos.up(), state.withProperty(UPPER, true), 3);
  }

  /** A creative player breaking the upper half takes the lower with it, dropping nothing. */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(UPPER) && player.capabilities.isCreativeMode) {
      BlockPos below = pos.down();
      if (world.getBlockState(below).getBlock() == this) {
        world.setBlockToAir(below);
      }
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /**
   * The other half goes with this one: the lower half drops what it held and clears the upper;
   * an upper half broken on its own breaks the lower, which drops the refrigerator.
   */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (state.getValue(UPPER)) {
      BlockPos below = pos.down();
      if (world.getBlockState(below).getBlock() == this) {
        world.destroyBlock(below, true);
      }
    } else {
      ResidentialStorageHelper.dropContents(world, pos);
      if (world.getBlockState(pos.up()).getBlock() == this) {
        world.setBlockToAir(pos.up());
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(UPPER) ? Items.AIR : super.getItemDropped(state, rand, fortune);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  // --- storage -------------------------------------------------------------------------

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    ResidentialStorageHelper.open(world, state.getValue(UPPER) ? pos.down() : pos, player, this);
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(@Nonnull IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(@Nonnull IBlockState state, World world,
      @Nonnull BlockPos pos) {
    return ResidentialStorageHelper.comparator(world, getStoragePos(world, pos));
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return !state.getValue(UPPER);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityResidentialStorage.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityresidentialstorage";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return (meta & 4) != 0 ? null : new TileEntityResidentialStorage(SLOTS);
  }
}
