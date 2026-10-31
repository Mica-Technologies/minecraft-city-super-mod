package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A banner hung from the roof truss: a championship banner ({@code championship_banner},
 * {@code _small}) or a retired number ({@code retired_number_banner}, {@code _small}). A hung
 * {@link BlockVenueUnit}: the large one is 2 wide and 3 tall, its master the left cell (seen
 * from the north) of the middle row; the small one 1 wide and 2 tall, its master the bottom
 * cell. Its cloth and trim are tinted with the tile entity's colours; its three lines of text
 * are {@link TileEntityVenueBannerRenderer}'s, on both sides.
 *
 * <p>Metadata is the facing. A click opens the text editor; {@code /blockdata} sets the lines
 * and colours ({@link TileEntityVenueBanner}).</p>
 *
 * @since 2026.10
 */
public class BlockVenueBanner extends BlockVenueUnit implements ICsmTileEntityProvider {

  private final boolean large;
  private final boolean retired;

  /**
   * Constructs a banner.
   *
   * @param registryName its registry name
   * @param large        2 x 3 rather than 1 x 2
   * @param retired      a retired number's defaults rather than a championship's
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockVenueBanner(String registryName, boolean large, boolean retired, int[] cells,
      int[] boxes) {
    super(registryName, true, cells, boxes);
    this.large = large;
    this.retired = retired;
  }

  /** 2 x 3 rather than 1 x 2. */
  public boolean isLarge() {
    return large;
  }

  /** A retired number's banner. */
  public boolean isRetired() {
    return retired;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (world.isRemote) {
      VenuesGuiProvider.open(player, world, pos, VenuesGuiProvider.TEXT_GUI_ID);
    }
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityVenueBanner.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityvenuebanner";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityVenueBanner(retired);
  }
}
