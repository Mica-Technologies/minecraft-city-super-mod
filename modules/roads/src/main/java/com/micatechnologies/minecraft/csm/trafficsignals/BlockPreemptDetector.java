package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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
 * A preemption detector: the small black head clamped on top of a mast arm that sees an
 * approaching emergency vehicle's emitter and calls the controller's emergency preempt, after the
 * optical detectors real intersections carry. Place it on top of the arm; its clamp reaches down
 * round a thin traffic pole in the cell below.
 * <p>
 * It looks the way it faces, towards the approach it serves: place it standing on that approach,
 * facing the junction. It is linked to a circuit with the Signal Link Tool, like a sensor, and a
 * preempt set to trigger on that circuit's detectors (DET on the controller's PREEMPT screen)
 * fires while any of them is called. Redstone calls it too, which is how a fire station's alert
 * relay holds the road for its trucks. It has no lamp of its own: a preemption beacon linked to
 * the same circuit is the confirmation light.
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
   * The detector's box: the head and its stem, standing on the floor of its cell (the clamp below
   * it hugs the arm in the cell underneath).
   *
   * @since 1.0
   */
  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(5 / 16.0, 0.0, 5 / 16.0, 11 / 16.0, 14.6 / 16.0, 11 / 16.0);

  public BlockPreemptDetector() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "preempt_detector";
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3);   // a detector placed with its old lamp bit loads
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
