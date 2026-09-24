package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A chest freezer: 27 slots under a lid, and it freezes water. A water bottle left in it
 * becomes ice and a water bucket packed ice, each giving back its empty bottle or bucket
 * ({@link TileEntityResidentialFreezer}). Otherwise it is a chest, with the refrigerator's door
 * seal sound.
 *
 * @since 2026.9
 */
public class BlockChestFreezer extends BlockResidentialFurniture
    implements ICsmTileEntityProvider, IResidentialStorage {

  private static final int SLOTS = 27;

  /**
   * Constructs a chest freezer.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockChestFreezer(String registryName, int[] box) {
    super(registryName, box, false);
  }

  @Override
  public int getSlots() {
    return SLOTS;
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

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    ResidentialStorageHelper.open(world, pos, player, this);
    return true;
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    ResidentialStorageHelper.dropContents(world, pos);
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
    return ResidentialStorageHelper.comparator(world, pos);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityResidentialFreezer.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityresidentialfreezer";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityResidentialFreezer(SLOTS);
  }
}
