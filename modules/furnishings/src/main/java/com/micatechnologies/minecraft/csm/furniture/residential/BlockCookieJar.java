package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
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
 * A ceramic cookie jar for the counter: nine slots that take cookies only
 * ({@link TileEntityCookieJar}), opened on right-click to the clink of its lid, with the storage
 * screen of the kitchen cabinets. It stands on the counter like any {@link BlockCounterPiece}.
 *
 * @since 2026.9
 */
public class BlockCookieJar extends BlockCounterPiece
    implements ICsmTileEntityProvider, IResidentialStorage {

  /** How many slots it has: a row. */
  public static final int SLOTS = 9;

  /**
   * Constructs a cookie jar.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   */
  public BlockCookieJar(String registryName, int[] box) {
    super(registryName, box, Material.GLASS, SoundType.GLASS, BlockRenderLayer.SOLID);
  }

  @Override
  public int getSlots() {
    return SLOTS;
  }

  @Nullable
  @Override
  public ICsmSound getOpenSound() {
    return FurnishingsSounds.JAR_LID;
  }

  @Nullable
  @Override
  public ICsmSound getCloseSound() {
    return FurnishingsSounds.JAR_LID;
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
    return TileEntityCookieJar.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitycookiejar";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityCookieJar();
  }
}
