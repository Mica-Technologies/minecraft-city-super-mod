package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import com.micatechnologies.minecraft.csm.venues.arena.TileEntityArenaFloorAnchor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A ballpark marking's anchor: a field block showing the middle of its picture, round which
 * {@link BlockBallparkPainted} cells show the rest. Three groups, each a block and its third base
 * side twin ({@code _third}), which draws the same pictures mirrored:
 *
 * <ul>
 *   <li>{@code ballpark_marking_anchor}: 0 the home plate area (the 13 ft dirt circle, batter's
 *   and catcher's boxes, the foul lines' starts, the base paths leaving it), 1 the first base
 *   cutout (third base on the twin), 2 the first base coach's box, 3 an on-deck circle;</li>
 *   <li>{@code ballpark_arc_anchor}: 0-2 the infield arc's centre, middle and end segments;</li>
 *   <li>{@code softball_marking_anchor}: 0 softball's home plate area, 1 the pitcher's circle
 *   with the pitcher's plate, 2-3 softball's infield arc, centre and end.</li>
 * </ul>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the picture's forward
 * runs: towards centre field; bits 2-3 the kind. Placed by hand it faces the way the placer
 * looks; a sneak-click with an empty hand steps the kind.</p>
 *
 * <p>Its tile entity is the arena anchor's: no data, no tick; it asks for the painted cells
 * round it to be drawn again when it is loaded, changed or removed.</p>
 *
 * @since 2026.10
 */
public class BlockBallparkAnchor extends BlockArenaFloorBase
    implements ICsmTileEntityProvider, IBallparkAnchor {

  /** The way the picture faces. Stored, bits 0-1. */
  public static final PropertyDirection FACING = PropertyDirection.create("facing",
      EnumFacing.Plane.HORIZONTAL);

  /** Each group's kinds, as lang keys under {@code csm.venues.ballpark.kind}. */
  private static final String[][] KEYS = {
      {"bb_home", "bb_cutout", "coach", "on_deck"},
      {"arc0", "arc1", "arc2"},
      {"sb_home", "sb_circle", "arc0", "arc1"}};

  private PropertyInteger kind;
  private final BallparkFamily family;
  private final int group;
  private final boolean third;
  private final int kinds;

  /**
   * Constructs an anchor.
   *
   * @param registryName its registry name
   * @param family       the pictures it anchors
   * @param group        its group (0 markings, 1 the arc, 2 softball)
   * @param third        the third base side twin, drawing its pictures mirrored
   * @param kinds        how many kinds it has
   */
  public BlockBallparkAnchor(String registryName, BallparkFamily family, int group,
      boolean third, int kinds) {
    super(registryName, Surface.CLAY, kinds);
    this.family = family;
    this.group = group;
    this.third = third;
    this.kinds = kinds;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(kind, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    kind = PropertyInteger.create("kind", 0, Math.max(1, (Integer) pendingArg()) - 1);
    return new CsmBlockStateContainer(this, FACING, kind);
  }

  @Override
  public BallparkFamily family() {
    return family;
  }

  @Override
  public int kindIndex(IBlockState state) {
    return group * 4 + state.getValue(kind);
  }

  @Override
  public boolean third() {
    return third;
  }

  @Override
  public EnumFacing pictureFacing(IBlockState state) {
    return state.getValue(FACING);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(kind, Math.min(kinds - 1, (meta >> 2) & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(kind) << 2;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int k = (state.getValue(kind) + 1) % kinds;
      world.setBlockState(pos, state.withProperty(kind, k), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.floor.kind",
          new TextComponentTranslation("csm.venues.ballpark.kind." + KEYS[group][k])), true);
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityArenaFloorAnchor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityarenaflooranchor";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityArenaFloorAnchor();
  }
}
