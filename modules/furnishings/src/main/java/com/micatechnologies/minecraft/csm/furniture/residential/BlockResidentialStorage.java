package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * A piece of Residential furniture that stores things, like a chest: the TV stand (nine slots a
 * block) and the sideboard (eighteen). It joins into runs as the other run pieces do, but each
 * block keeps its own slots. Right-click opens them ({@link ContainerResidentialStorage}); a
 * hopper reaches them from any side; breaking the block drops what it held.
 *
 * @since 2026.9
 */
public class BlockResidentialStorage extends BlockResidentialRun implements ICsmTileEntityProvider {

  /** The storage screen's GUI id, unique across the mod. */
  public static final int GUI_ID = 35;

  private final int slots;

  /**
   * Constructs a storage piece.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param slots        how many slots a block holds, a multiple of nine
   */
  public BlockResidentialStorage(String registryName, int[] box, int slots) {
    super(registryName, box, false);
    this.slots = slots;
  }

  /**
   * How many slots a block holds.
   *
   * @return the slot count
   */
  public int getSlots() {
    return slots;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityResidentialStorage) {
        ((TileEntityResidentialStorage) te).ensureSlots(slots);
        player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
      }
    }
    return true;
  }

  /** What it held drops where it stood. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityResidentialStorage) {
        ((TileEntityResidentialStorage) te).forEachItem(stack -> InventoryHelper.spawnItemStack(
            world, pos.getX(), pos.getY(), pos.getZ(), stack.copy()));
      }
    }
    super.breakBlock(world, pos, state);
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
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityResidentialStorage
        ? ItemHandlerHelper.calcRedstoneFromInventory(((TileEntityResidentialStorage) te)
        .getItems()) : 0;
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
    return new TileEntityResidentialStorage(slots);
  }
}
