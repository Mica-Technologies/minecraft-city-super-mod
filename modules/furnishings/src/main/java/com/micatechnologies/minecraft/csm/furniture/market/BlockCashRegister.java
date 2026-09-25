package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterPiece;
import com.micatechnologies.minecraft.csm.furniture.residential.IResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.ResidentialStorageHelper;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityResidentialStorage;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A cash register or point-of-sale terminal, set on a checkout counter or any other counter
 * ({@link BlockCounterPiece}: it stands on whatever is under it). Its cash drawer holds nine
 * slots; right-click opens it with the drawer's ring, and closing it slides it shut.
 *
 * @since 2026.9
 */
public class BlockCashRegister extends BlockCounterPiece
    implements ICsmTileEntityProvider, IResidentialStorage {

  private static final int SLOTS = 9;

  /**
   * Constructs a register.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   */
  public BlockCashRegister(String registryName, int[] box) {
    super(registryName, box, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID);
  }

  @Override
  public int getSlots() {
    return SLOTS;
  }

  @Nullable
  @Override
  public ICsmSound getOpenSound() {
    return FurnishingsSounds.REGISTER_DRAWER;
  }

  @Nullable
  @Override
  public ICsmSound getCloseSound() {
    return FurnishingsSounds.DRAWER_CLOSE;
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

  /** What the drawer held drops where it stood. */
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
  public boolean hasTileEntity(IBlockState state) {
    return true;
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
    return new TileEntityResidentialStorage(SLOTS);
  }
}
