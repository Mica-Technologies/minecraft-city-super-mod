package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
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
 * RRFB (Rectangular Rapid Flashing Beacon): two rectangular amber indications in one housing,
 * mounted with a pedestrian crossing sign at an uncontrolled or midblock crossing. Dark until a
 * pedestrian calls it, then firing alternating rapid bursts at approaching traffic.
 *
 * <p>Links as a {@link SIGNAL_SIDE#PEDESTRIAN_BEACON}, so a controller drives it exactly as it
 * drives the HAWK: dark on the off state, lit while the crossing is being served.</p>
 *
 * <p>Unlike the configurable signal heads, this block has no custom renderer. The lamps run
 * their sequence inside an animated texture rather than at render time, which is what keeps
 * them in step: Minecraft advances texture animations from the global tick counter, so every
 * RRFB in the world flashes together the way a pair facing each other across a crossing should.
 * The strip is the 16-frame, 800 ms sequence FHWA Interim Approval IA-21 requires -- a
 * left/right wig-wag, then two flashes with <em>both</em> lamps lit, then a 250 ms dark gap. See
 * {@code dev-env-utils/scripts/gen_rrfb_textures.py}, which checks the sequence against the
 * standard's timings rather than against an assumption about it.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class BlockControllableRrfb extends AbstractBlockControllableSignal
    implements ICsmTileEntityProvider {

  /**
   * Housing paint, and whether the bar carries lamps on both faces.
   *
   * <p>Neither is in the metadata — facing and colour state already use all sixteen values —
   * so both are filled in from the tile entity in {@link #getActualState}, the same way
   * {@code AbstractBrightLightPoleColored} takes its colour from the pole beneath it. That
   * keeps the appearance options out of the saved block state while still letting the
   * blockstate JSON choose the model and textures from them.</p>
   */
  public static final PropertyEnum<TrafficSignalBodyColor> HOUSING =
      PropertyEnum.create("housing", TrafficSignalBodyColor.class);

  /** Whether the bar shows lamps on its back face as well as its front. */
  public static final PropertyBool DOUBLE_SIDED = PropertyBool.create("doublesided");

  public BlockControllableRrfb() {
    super(Material.ROCK);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, COLOR, HOUSING, DOUBLE_SIDED);
  }

  @Override
  @SuppressWarnings("deprecation")
  public @Nonnull IBlockState getActualState(@Nonnull IBlockState state,
      @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
    TrafficSignalBodyColor housing = TrafficSignalBodyColor.FLAT_BLACK;
    boolean doubleSided = true;
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof TileEntityRrfb) {
      housing = ((TileEntityRrfb) te).getHousingColor();
      doubleSided = ((TileEntityRrfb) te).isDoubleSided();
    }
    return state.withProperty(HOUSING, housing).withProperty(DOUBLE_SIDED, doubleSided);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityRrfb.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityrrfb";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityRrfb();
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.PEDESTRIAN_BEACON;
  }

  /**
   * The beacon flashes whenever it is called, so it must be driven in the controller's flash
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
    return "controllablerrfb";
  }

  /**
   * Matches the housing drawn by the model: the 15 x 4 unit bar held against the back of the
   * block, rather than the whole cube. Sitting at the back is what lets the unit be placed
   * flush on a wall or a mount, with nothing of it hanging into the block behind.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.03125D, 0.375D, 0.80625D, 0.96875D, 0.625D, 0.99875D);
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Cutout so the transparent area around the housing does not draw as a black panel.
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
