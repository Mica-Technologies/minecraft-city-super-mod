package com.micatechnologies.minecraft.csm.venues.lines;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A plain court, track or sand cell, where no line falls: {@code court_acrylic} and
 * {@code track_surface} in any of the sixteen {@link SurfaceColours} (metadata, a tint over one
 * light grey sprite, the same sprite and colour a painted cell writes into its vertices; placed by
 * hand a track is red and a court blue), and
 * {@code sports_sand}, a beach court's or a pit's sand. A sneak-click with an empty hand steps a
 * coloured surface's colour.
 *
 * @since 2026.10
 */
public class BlockSportSurface extends BlockArenaFloorBase {

  private PropertyInteger colour;
  private final boolean coloured;
  private final int defaultColour;

  /**
   * Constructs a surface.
   *
   * @param registryName its registry name
   * @param surface      what it is made of
   * @param coloured     whether it takes a surface colour
   */
  public BlockSportSurface(String registryName, Surface surface, boolean coloured) {
    this(registryName, surface, coloured, surface == Surface.POLYURETHANE ? 4 : 2);
  }

  /**
   * Constructs a surface placed by hand, and shown as an item, in a colour of its own.
   *
   * @param registryName  its registry name
   * @param surface       what it is made of
   * @param coloured      whether it takes a surface colour
   * @param defaultColour the colour it is placed in by hand ({@link SurfaceColours})
   */
  public BlockSportSurface(String registryName, Surface surface, boolean coloured,
      int defaultColour) {
    super(registryName, surface, coloured);
    this.coloured = coloured;
    this.defaultColour = SurfaceColours.clamp(defaultColour);
    if (colour != null) {
      setDefaultState(blockState.getBaseState().withProperty(colour, 0));
    }
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    if (Boolean.TRUE.equals(pendingArg())) {
      colour = PropertyInteger.create("colour", 0, SurfaceColours.COUNT - 1);
      return new CsmBlockStateContainer(this, colour);
    }
    return new CsmBlockStateContainer(this);
  }

  /** Whether it takes a surface colour. */
  public boolean isColoured() {
    return coloured;
  }

  /** The colour it is placed in by hand and shown in as an item: a track red, a court blue. */
  public int defaultColour() {
    return defaultColour;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return colour == null ? getDefaultState()
        : getDefaultState().withProperty(colour, defaultColour);
  }

  /** A state's colour index (0 for sand). */
  public int colourOf(IBlockState state) {
    return colour == null || state.getBlock() != this ? 0 : state.getValue(colour);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return colour == null ? getDefaultState() : getDefaultState().withProperty(colour, meta & 15);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return colour == null ? 0 : state.getValue(colour);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (colour == null || !player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int c = (state.getValue(colour) + 1) % SurfaceColours.COUNT;
      world.setBlockState(pos, state.withProperty(colour, c), 3);
      player.sendStatusMessage(new TextComponentTranslation(SurfaceColours.key(c)), true);
    }
    return true;
  }
}
