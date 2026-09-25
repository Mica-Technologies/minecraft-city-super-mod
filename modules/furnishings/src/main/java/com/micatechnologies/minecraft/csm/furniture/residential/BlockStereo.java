package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A hi-fi stereo with a turntable on top that plays music discs as a jukebox does. Right-click
 * with a disc puts it on the turntable and it plays, with the jukebox's own record event (so the
 * "Now Playing" line, the record volume and the sound's range are vanilla's); right-click again
 * takes the disc off, stopping it, and breaking the stereo does both. While a disc is on it
 * ({@link #RECORD}, stored), the disc is drawn on the platter and the display lights.
 *
 * <p>It stands on whatever is under it, as the counter pieces do, so it can go on a TV stand or
 * a sideboard. The disc is kept by a {@link TileEntityStereo}.</p>
 *
 * @since 2026.9
 */
public class BlockStereo extends BlockCounterPiece implements ICsmTileEntityProvider {

  /** Whether a disc is on the turntable. */
  public static final PropertyBool RECORD = PropertyBool.create("record");

  /** The world event a jukebox plays a record with, and stops it with (data 0). */
  private static final int EVENT_RECORD = 1010;

  /**
   * Constructs a stereo.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   */
  public BlockStereo(String registryName, int[] box) {
    super(registryName, box, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID);
    setDefaultState(getDefaultState().withProperty(RECORD, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, REST, RECORD);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(RECORD, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(RECORD) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(RECORD, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    ItemStack held = player.getHeldItem(hand);
    if (!state.getValue(RECORD) && !(held.getItem() instanceof ItemRecord)) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityStereo)) {
      return true;
    }
    TileEntityStereo stereo = (TileEntityStereo) te;
    if (state.getValue(RECORD)) {
      eject(world, pos, stereo);
      world.setBlockState(pos, state.withProperty(RECORD, false), 3);
    } else {
      ItemStack disc = held.copy();
      disc.setCount(1);
      stereo.setRecord(disc);
      if (!player.capabilities.isCreativeMode) {
        held.shrink(1);
      }
      world.setBlockState(pos, state.withProperty(RECORD, true), 3);
      world.playEvent(null, EVENT_RECORD, pos, Item.getIdFromItem(disc.getItem()));
    }
    return true;
  }

  /** Stops the music and drops the disc on top of the stereo, as a jukebox ejects one. */
  private static void eject(World world, BlockPos pos, TileEntityStereo stereo) {
    ItemStack disc = stereo.getRecord();
    world.playEvent(EVENT_RECORD, pos, 0);
    if (disc.isEmpty()) {
      return;
    }
    stereo.setRecord(ItemStack.EMPTY);
    EntityItem item = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.6,
        pos.getZ() + 0.5, disc.copy());
    item.setDefaultPickupDelay();
    world.spawnEntity(item);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityStereo) {
        eject(world, pos, (TileEntityStereo) te);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(@Nonnull IBlockState state) {
    return true;
  }

  /** As a jukebox's: which disc is playing, 1 to 12 in the vanilla discs' order, else 15. */
  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(@Nonnull IBlockState state, World world,
      @Nonnull BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityStereo) {
      ItemStack disc = ((TileEntityStereo) te).getRecord();
      if (!disc.isEmpty()) {
        int id = Item.getIdFromItem(disc.getItem()) + 1 - Item.getIdFromItem(
            net.minecraft.init.Items.RECORD_13);
        return id >= 1 && id <= 12 ? id : 15;
      }
    }
    return 0;
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityStereo.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitystereo";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityStereo();
  }
}
