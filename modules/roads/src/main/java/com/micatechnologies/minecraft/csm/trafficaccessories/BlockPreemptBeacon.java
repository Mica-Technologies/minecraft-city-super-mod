package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The red preemption confirmation beacon. It lights for redstone, as it always has, and for a
 * signal controller: linked to a circuit with the Signal Link Tool, it is lit while a preempt
 * triggered from that circuit runs (ADVANCED mode), so a driver sees the intersection has
 * answered the emitter.
 */
public class BlockPreemptBeacon extends AbstractPoweredBlockRotatableNSEWUD
    implements ICsmTileEntityProvider, ITrafficBeaconBlock, ICsmNoSnowAccumulation,
    ICsmTrafficPoleIgnored {

  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(0.000000, -0.421875, 0.406250, 0.562500, 0.156250, 0.593750);

  public BlockPreemptBeacon() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "tlpreemptbeacon";
  }

  /**
   * Whether the controller lights this beacon for an emergency-vehicle preempt from any circuit,
   * not only for a preempt its own circuit triggers. True for the red beacons, which tell every
   * approach that an emergency vehicle has the intersection; the white and blue confirmation
   * lights say only which approach was answered.
   */
  public boolean isLitForAnyEmergencyPreempt() {
    return true;
  }

  /** Redstone or the controller: lit while either says so. */
  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    showPower(world, pos);
  }

  /**
   * Sets the beacon's POWERED state from redstone and its controller together.
   *
   * @param world the world
   * @param pos   the beacon
   */
  public static void showPower(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockPreemptBeacon)) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    boolean lit = world.getRedstonePowerFromNeighbors(pos) > 0
        || (te instanceof TileEntityTrafficBeacon && ((TileEntityTrafficBeacon) te).isControllerLit());
    if (state.getValue(POWERED) != lit) {
      world.setBlockState(pos, state.withProperty(POWERED, lit), 3);
    }
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficBeacon();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficBeacon.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitytrafficbeaconpreempt";
  }

  @Override
  public float[] getBeaconLensFrom() {
    return new float[]{6.0f, -1.5f, 6.5f};
  }

  @Override
  public float[] getBeaconLensTo() {
    return new float[]{9.0f, 2.5f, 9.5f};
  }

  @Override
  public float getBeaconColorR() {
    return 1.0f;
  }

  @Override
  public float getBeaconColorG() {
    return 0.15f;
  }

  @Override
  public float getBeaconColorB() {
    return 0.1f;
  }
}
