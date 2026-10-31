package com.micatechnologies.minecraft.csm.venues.field;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import com.micatechnologies.minecraft.csm.venues.arena.TileEntityArenaFloorAnchor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * A football or soccer field's marking anchor: a field cell showing its own piece of the field,
 * round which {@link BlockFieldPainted} cells show theirs. A field has anchors on a grid, each
 * owning the picture cells nearest it (within 12 cells), every one a kind of its own:
 * {@code football_marking_anchor} holds kinds 0-7, {@code football_marking_anchor_2} the rest, and
 * the soccer pair the same. {@code gen_venues_football.py --layout} says which kind goes where.
 *
 * <p><b>Metadata</b>: bit 0 the field's axis (clear: the field's length runs east-west and the
 * anchor's forward is north; set: the field runs north-south, forward east), bits 1-3 the kind
 * within the block. Placed by hand it takes the axis the placer looks across; a sneak-click with
 * an empty hand steps the kind.</p>
 *
 * <p>Its tile entity is the arena anchor's: no data, no tick; it asks for the painted cells round
 * it to be drawn again when it is loaded, changed or removed.</p>
 *
 * @since 2026.10
 */
public class BlockFieldAnchor extends BlockArenaFloorBase implements ICsmTileEntityProvider,
    IFieldCell {

  /** The field runs north-south (forward east). Stored, bit 0. */
  public static final PropertyBool EAST = PropertyBool.create("east");

  private PropertyInteger kind;
  private final FieldFamily family;
  private final int first;
  private final int kinds;

  /**
   * Constructs an anchor block.
   *
   * @param registryName its registry name
   * @param family       the pictures it anchors
   * @param first        the family's kind its kind 0 is
   * @param kinds        how many kinds it has (1-8)
   */
  public BlockFieldAnchor(String registryName, FieldFamily family, int first, int kinds) {
    super(registryName, Surface.TURF, kinds);
    this.family = family;
    this.first = first;
    this.kinds = kinds;
    setDefaultState(blockState.getBaseState().withProperty(EAST, false).withProperty(kind, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    kind = PropertyInteger.create("kind", 0, Math.max(1, (Integer) pendingArg()) - 1);
    return new CsmBlockStateContainer(this, EAST, kind);
  }

  /** The pictures it anchors. */
  FieldFamily family() {
    return family;
  }

  /** The family's kind for a state. */
  int kindIndex(IBlockState state) {
    return first + state.getValue(kind);
  }

  /** The way the picture's forward runs: north, or east. */
  EnumFacing forward(IBlockState state) {
    return state.getValue(EAST) ? EnumFacing.EAST : EnumFacing.NORTH;
  }

  @Override
  public int tintRotation(IBlockState state) {
    if (state.getBlock() != this) {
      return 0;
    }
    int v = family.at(kindIndex(state), 0, 0);
    return ((state.getValue(EAST) ? 1 : 0) + 2 * Math.max(0, v & 1)) & 3;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(EAST, (meta & 1) != 0)
        .withProperty(kind, Math.min(kinds - 1, (meta >> 1) & 7));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(EAST) ? 1 : 0) | state.getValue(kind) << 1;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(EAST,
        placer.getHorizontalFacing().getAxis() == EnumFacing.Axis.X);
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
      player.sendStatusMessage(new TextComponentString("#" + (first + k)), true);
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
