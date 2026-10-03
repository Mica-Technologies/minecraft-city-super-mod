package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A preemption detector: the small head on a mast arm that sees an approaching emergency
 * vehicle's emitter and calls the controller's emergency preempt, after the optical detectors
 * real intersections carry.
 * <p>
 * It looks the way it faces, towards the approach it serves: place it standing on that approach,
 * facing the junction. It is linked to a circuit with the Signal Link Tool, like a sensor, and a
 * preempt set to trigger on that circuit's detectors (DET on the controller's PREEMPT screen)
 * fires while any of them is called. Redstone calls it too, which is how a fire station's alert
 * relay holds the road for its trucks. The white confirmation lamp underneath lights while it has
 * a call.
 * <p>
 * Right-click steps its range (60 to 300 blocks); sneak and right-click steps how wide it looks.
 *
 * @version 1.0
 * @see TileEntityPreemptDetector
 * @since 2026.10
 */
public class BlockPreemptDetector extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider, ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /**
   * Whether the detector has a call, shown on its confirmation lamp. Written by the tile entity
   * on a change only.
   *
   * @since 1.0
   */
  public static final PropertyBool CALLED = PropertyBool.create("called");

  /**
   * The detector's box facing north: the housing and hood, and the stem up to the arm.
   *
   * @since 1.0
   */
  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(5 / 16.0, 4 / 16.0, 1 / 16.0, 11 / 16.0, 1.0, 12 / 16.0);

  public BlockPreemptDetector() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
    setDefaultState(getDefaultState().withProperty(CALLED, false));
  }

  @Override
  public String getBlockRegistryName() {
    return "preempt_detector";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, CALLED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(CALLED, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(CALLED) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(CALLED, false);
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

  // --- redstone ------------------------------------------------------------------------------

  @Override
  public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
    super.onBlockAdded(worldIn, pos, state);
    readRedstone(worldIn, pos);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    readRedstone(worldIn, pos);
  }

  private static void readRedstone(World world, BlockPos pos) {
    if (world.isRemote) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityPreemptDetector) {
      ((TileEntityPreemptDetector) te).setPowered(world.isBlockPowered(pos));
    }
  }

  // --- settings ------------------------------------------------------------------------------

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    Item held = playerIn.getHeldItem(hand).getItem();
    // The link tool links the detector to a circuit; let its own click handling run.
    if (held instanceof ItemSignalLinkTool || held instanceof ItemSensorZoneTool) {
      return false;
    }
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (worldIn.isRemote) {
      return true;
    }
    TileEntity te = worldIn.getTileEntity(pos);
    if (!(te instanceof TileEntityPreemptDetector)) {
      return false;
    }
    TileEntityPreemptDetector detector = (TileEntityPreemptDetector) te;
    if (playerIn.isSneaking()) {
      int halfAngle = detector.cycleHalfAngle();
      playerIn.sendMessage(new TextComponentString(
          "Preempt detector now looks " + halfAngle + "° either side of straight ahead."));
    } else {
      int range = detector.cycleRange();
      playerIn.sendMessage(new TextComponentString(
          "Preempt detector range: " + range + " blocks."));
    }
    return true;
  }

  // --- tile entity ---------------------------------------------------------------------------

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPreemptDetector.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitypreemptdetector";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityPreemptDetector();
  }
}
