package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
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
 * The anchor of a painted floor marking ({@code rink_ice_spot}, {@code court_floor_anchor}): a
 * floor block that shows the middle of its picture and around which
 * {@link BlockArenaFloorPainted} blocks show the rest. The rink's kinds are the end zone
 * face-off circle (0), a neutral zone spot (1), centre ice (2) and the goal crease (3); the
 * court's are a basket end with a natural (0), navy (1) or crimson (2) key, and centre court (3).
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the picture's forward
 * runs (towards centre ice from a goal line, into the court from a baseline); bits 2-3 the kind.
 * Placed by hand it faces the way the placer looks; a sneak-click with an empty hand steps the
 * kind.</p>
 *
 * <p>Its tile entity holds nothing and never ticks. It is there for one thing: when an anchor
 * appears, changes or goes, the painted blocks round it in other chunk sections must be drawn
 * again, and the client only redraws the section a block change is in. The tile entity's load,
 * update and removal are the client-side moments to ask for that redraw; a rink has a dozen
 * anchors, so it costs nothing.</p>
 *
 * @since 2026.10
 */
public class BlockArenaFloorAnchor extends BlockArenaFloorBase implements ICsmTileEntityProvider {

  /** The way the picture faces. Stored, bits 0-1. */
  public static final PropertyDirection FACING = PropertyDirection.create("facing",
      EnumFacing.Plane.HORIZONTAL);
  /** Which picture. Stored, bits 2-3. */
  public static final PropertyInteger KIND = PropertyInteger.create("kind", 0, 3);

  private final ArenaFloorFamily family;

  /**
   * Constructs an anchor.
   *
   * @param registryName its registry name
   * @param family       the markings it anchors
   */
  public BlockArenaFloorAnchor(String registryName, ArenaFloorFamily family) {
    super(registryName, "ice".equals(family.key) ? Surface.ICE : Surface.WOOD, null);
    this.family = family;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(KIND, 0));
  }

  /** The markings it anchors. */
  ArenaFloorFamily family() {
    return family;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, KIND);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(KIND, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(KIND) << 2;
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
      int kind = (state.getValue(KIND) + 1) & 3;
      world.setBlockState(pos, state.withProperty(KIND, kind), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.floor.kind",
          new TextComponentTranslation("csm.venues.floor." + family.key + "." + kind)), true);
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
