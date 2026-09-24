package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
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
 * A piece of Residential furniture that stores things, like a chest: the TV stand (nine slots a
 * block), the sideboard (eighteen) and the kitchen cabinets. It joins into runs as the other run
 * pieces do, but each block keeps its own slots. Right-click opens them
 * ({@link ContainerResidentialStorage}); a hopper reaches them from any side; breaking the block
 * drops what it held. A piece given no slots (an open shelf) has no tile entity and no screen.
 *
 * @since 2026.9
 */
public class BlockResidentialStorage extends BlockResidentialRun
    implements ICsmTileEntityProvider, IResidentialStorage {

  /** The storage screen's GUI id, unique across the mod. */
  public static final int GUI_ID = 35;

  private final int slots;
  @Nullable
  private final ICsmSound openSound;
  @Nullable
  private final ICsmSound closeSound;

  /**
   * Constructs a storage piece that opens silently.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param slots        how many slots a block holds, a multiple of nine
   */
  public BlockResidentialStorage(String registryName, int[] box, int slots) {
    this(registryName, box, slots, null, null);
  }

  /**
   * Constructs a storage piece with the sounds of its doors or drawers.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param slots        how many slots a block holds, a multiple of nine, or zero for none
   * @param openSound    the sound of opening it, or null
   * @param closeSound   the sound of closing it, or null
   */
  public BlockResidentialStorage(String registryName, int[] box, int slots,
      @Nullable ICsmSound openSound, @Nullable ICsmSound closeSound) {
    super(registryName, box, false);
    this.slots = slots;
    this.openSound = openSound;
    this.closeSound = closeSound;
  }

  @Override
  public int getSlots() {
    return slots;
  }

  @Nullable
  @Override
  public ICsmSound getOpenSound() {
    return openSound;
  }

  @Nullable
  @Override
  public ICsmSound getCloseSound() {
    return closeSound;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking() || slots <= 0) {
      return false;
    }
    ResidentialStorageHelper.open(world, pos, player, this);
    return true;
  }

  /** What it held drops where it stood. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    ResidentialStorageHelper.dropContents(world, pos);
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(@Nonnull IBlockState state) {
    return slots > 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(@Nonnull IBlockState state, World world,
      @Nonnull BlockPos pos) {
    return ResidentialStorageHelper.comparator(world, pos);
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return slots > 0;
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
    return slots > 0 ? new TileEntityResidentialStorage(slots) : null;
  }
}
