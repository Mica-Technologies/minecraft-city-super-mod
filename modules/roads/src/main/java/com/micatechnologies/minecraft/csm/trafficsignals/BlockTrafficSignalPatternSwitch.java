package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
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
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The pattern select switch: a cabinet switch that puts every ADVANCED signal controller linked to
 * it onto a chosen coordination pattern while it is on, an event egress plan for a stadium's
 * junctions say, and hands them back to their clocks when it is off.
 *
 * <p>The switch is thrown by hand (right-click) or by redstone: power arriving turns it on and
 * power leaving turns it off, so a lever, a button's latch or a command block all work, and it can
 * still be thrown by hand in between. It is linked to a controller with the signal link tool, the
 * controller first and then the switch, and one switch may be linked to any number of
 * controllers.</p>
 *
 * <p>The switch knows nothing about its controllers. Each controller reads the switches linked to
 * it ({@link TileEntityTrafficSignalController}), and its own plan says which pattern a switch
 * selects, so one switch can mean pattern 2 at one junction and pattern 4 at the next. There is no
 * tile entity: the whole state is the block state.</p>
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class BlockTrafficSignalPatternSwitch extends AbstractBlockRotatableNSEW
    implements ICsmNoSnowAccumulation {

  /** Whether the switch is on. */
  public static final PropertyBool ON = PropertyBool.create("on");

  /**
   * Whether redstone reached the switch at the last look. Kept only to tell an edge from a level:
   * the switch follows power as it arrives and leaves, and is free to be thrown by hand otherwise.
   * It draws nothing.
   */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** Facing north: the box on the block's south face. gen_pattern_select_switch.py's BOUNDS. */
  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(4.5 / 16.0, 3.0 / 16.0, 12.5 / 16.0, 11.5 / 16.0, 13.0 / 16.0, 1.0);

  public BlockTrafficSignalPatternSwitch() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
    setDefaultState(getDefaultState().withProperty(ON, false).withProperty(POWERED, false));
  }

  @Override
  public String getBlockRegistryName() {
    return "traffic_signal_pattern_switch";
  }

  /**
   * Whether there is a switch at a position and it is on.
   *
   * @param world the world
   * @param pos   the position to look at; the caller checks that it is loaded
   *
   * @return {@code TRUE} or {@code FALSE} for a switch, {@code null} if the block is not one
   */
  @Nullable
  public static Boolean readSwitch(IBlockAccess world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockTrafficSignalPatternSwitch)) {
      return null;
    }
    return state.getValue(ON);
  }

  // --- state ---------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, ON, POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3)
        .withProperty(ON, (meta & 4) != 0)
        .withProperty(POWERED, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state)
        | (state.getValue(ON) ? 4 : 0)
        | (state.getValue(POWERED) ? 8 : 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess worldIn,
      BlockPos pos) {
    return state;   // a wall box: nothing here is fitted to a pole
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    boolean powered = worldIn.isBlockPowered(pos);
    return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(ON, powered).withProperty(POWERED, powered);
  }

  // --- throwing it ---------------------------------------------------------------------------

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    // The link tool links the switch; it must not also throw it. (A block's activation runs before
    // the held item's use, and returning true here would swallow the tool's click.)
    if (player.getHeldItem(hand).getItem() instanceof ItemSignalLinkTool) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    boolean on = !state.getValue(ON);
    world.setBlockState(pos, state.withProperty(ON, on), 3);
    world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.4F,
        on ? 0.6F : 0.5F);
    player.sendStatusMessage(new TextComponentString(
        on ? "Pattern select: ON (linked controllers change at their next cycle)"
            : "Pattern select: OFF (linked controllers return at their next cycle)"), true);
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    if (world.isRemote) {
      return;
    }
    IBlockState here = world.getBlockState(pos);
    if (here.getBlock() != this) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != here.getValue(POWERED)) {
      // An edge: the switch follows the power. Between edges it is the hand's to throw.
      world.setBlockState(pos, here.withProperty(POWERED, powered).withProperty(ON, powered), 3);
    }
  }

  // --- shape ---------------------------------------------------------------------------------

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
}
