package com.micatechnologies.minecraft.csm.venues.pool;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
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
 * A sign or board whose words are typed in: a pool's scoreboard ({@code pool_scoreboard}) or a
 * golf hole's tee sign ({@code golf_hole_sign}). The casing is baked; the words are
 * {@link TileEntityVenueSignRenderer}'s, from {@link TileEntityVenueSign}. A click opens the venue
 * text editor. Metadata is the facing, towards the reader.
 *
 * @since 2026.10
 */
public class BlockVenueSign extends BlockVenueUnit implements ICsmTileEntityProvider {

  /** What the sign shows, and what its lines mean. */
  public enum Style {
    /** A pool's scoreboard: l0 the event, l1 the heat, l2 the swimmers by lane (commas), l3
     * their times (spaces), l4 their places (spaces), l5 the race clock. */
    POOL_BOARD("pool", VenueColours.NAVY, new int[]{24, 24, 128, 96, 24, 8},
        new String[]{"100 FREESTYLE", "HEAT 1", "", "", "", "0:00.00"}),
    /** A golf hole's tee sign: l0 the hole, l1 its par, l2 the yards from each tee (spaces,
     * back tee first), l3 a line under them (the stroke index, a name). */
    GOLF_HOLE("golf", 5, new int[]{2, 1, 20, 16},
        new String[]{"1", "4", "412 388 341", "HCP 7"});

    final String key;
    final int colour;
    final int[] lengths;
    final String[] defaults;

    Style(String key, int colour, int[] lengths, String[] defaults) {
      this.key = key;
      this.colour = colour;
      this.lengths = lengths;
      this.defaults = defaults;
    }
  }

  private final Style style;

  /**
   * Constructs a sign.
   *
   * @param registryName its registry name
   * @param style        what it shows
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockVenueSign(String registryName, Style style, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    this.style = style;
  }

  /** What it shows. */
  public Style style() {
    return style;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!(world.getTileEntity(pos) instanceof TileEntityVenueSign)) {
      return false;
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
    return TileEntityVenueSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityvenuesign";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityVenueSign();
  }
}
