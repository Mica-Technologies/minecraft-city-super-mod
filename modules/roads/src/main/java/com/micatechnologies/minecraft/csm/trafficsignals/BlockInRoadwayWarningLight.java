package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.InRoadwayLightLens;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.InRoadwayLightLinkMode;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.InRoadwayLightPattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An in-roadway warning light: a fixture set into the pavement at a crosswalk, flashing up and
 * along the road at approaching drivers. A row of them goes across the approach.
 *
 * <p>It links either as a pedestrian beacon, beside an RRFB or a HAWK, or as an ordinary
 * pedestrian signal at a signalised crossing — the mode is a setting, and the block reports it
 * from {@link #getSignalSide}, so the controller puts it in whichever list matches. That is also
 * why the lit state is computed here rather than read straight off the colour: a beacon is lit
 * on everything but off, while a crosswalk fixture has to be dark on don't-walk, and don't-walk
 * <em>is</em> that colour.</p>
 *
 * <p>Like the RRFB it has no renderer: the flash lives in an animated texture, which Minecraft
 * advances from the global tick counter, so every fixture in a row stays in step with every
 * other and with the beacon above them.</p>
 *
 * <p>A real row alternates rather than firing in unison, so each pattern is drawn in two phases
 * and the block picks between them from its own position. Deriving that rather than configuring
 * it means a row laid down in one pass alternates correctly with no setup, and there is no way
 * to get it wrong.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class BlockInRoadwayWarningLight extends AbstractBlockControllableSignal
    implements ICsmTileEntityProvider {

  /**
   * The lens the fixture is showing: dark, or one of the three sequences on one of its two
   * phases. One property rather than three because Forge merges a property map fragment by
   * fragment, so three independent properties cannot between them name a texture that depends
   * on all three.
   */
  public static final PropertyEnum<InRoadwayLightLens> LENS =
      PropertyEnum.create("lens", InRoadwayLightLens.class);

  /**
   * A sixteenth of a block tall, which is a fixture set flush into the road rather than a lump
   * on top of it. The collision box matches, so it can be walked and driven over without a step.
   */
  private static final AxisAlignedBB SHAPE =
      new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D);

  public BlockInRoadwayWarningLight() {
    super(Material.ROCK);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, COLOR, LENS);
  }

  @Override
  @SuppressWarnings("deprecation")
  public @Nonnull IBlockState getActualState(@Nonnull IBlockState state,
      @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
    InRoadwayLightPattern pattern = InRoadwayLightPattern.RRFB;
    InRoadwayLightLinkMode mode = InRoadwayLightLinkMode.BEACON;
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof TileEntityInRoadwayWarningLight) {
      pattern = ((TileEntityInRoadwayWarningLight) te).getPattern();
      mode = ((TileEntityInRoadwayWarningLight) te).getLinkMode();
    }
    if (!isLit(state.getValue(COLOR), mode)) {
      return state.withProperty(LENS, InRoadwayLightLens.OFF);
    }
    // The phase comes from the sum of the horizontal coordinates, so any two fixtures placed
    // side by side -- along either axis, and across a diagonal -- land on opposite halves.
    boolean offbeat = ((pos.getX() + pos.getZ()) & 1) == 1;
    return state.withProperty(LENS, InRoadwayLightLens.of(pattern, offbeat));
  }

  /**
   * Whether a fixture in a given link mode is lit on a given controller colour.
   *
   * <p>Package-visible and static so the two readings can be tested without a world, because
   * they are the one thing about this block that is easy to get backwards.</p>
   *
   * @param color the colour the controller is sending
   * @param mode  which list the fixture is linked into
   *
   * @return {@code true} if the fixture should be flashing
   */
  static boolean isLit(int color, InRoadwayLightLinkMode mode) {
    if (mode == InRoadwayLightLinkMode.CROSSWALK) {
      // Walk and the pedestrian clearance; dark on don't-walk, which is SIGNAL_RED.
      return color == SIGNAL_GREEN || color == SIGNAL_YELLOW;
    }
    // A beacon is dark only when the controller is not calling it at all, exactly as the RRFB.
    return color != SIGNAL_OFF;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityInRoadwayWarningLight.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityinroadwaywarninglight";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityInRoadwayWarningLight();
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    TileEntity te = world.getTileEntity(blockPos);
    if (te instanceof TileEntityInRoadwayWarningLight
        && ((TileEntityInRoadwayWarningLight) te).getLinkMode()
        == InRoadwayLightLinkMode.CROSSWALK) {
      return SIGNAL_SIDE.PEDESTRIAN;
    }
    return SIGNAL_SIDE.PEDESTRIAN_BEACON;
  }

  /**
   * The fixture flashes whenever it is called, so it must be driven in the controller's flash
   * mode as well.
   *
   * @return always {@code true}
   */
  @Override
  public boolean doesFlash() {
    return true;
  }

  @Override
  public String getBlockRegistryName() {
    return "in_roadway_warning_light";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SHAPE;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
