package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The bell schedule controller: a wall unit that rings class periods by the world's time.
 * Right-click opens its screen ({@link BellControllerGui}): the bells, each a time of the game
 * day with a tone and, for an announcement, its text, and a ring-now button. Speakers, the
 * clock/speaker panels' speakers and hallway bells are linked to it with the Bell System Linker.
 *
 * <p>Each ring also gives a one-second redstone pulse ({@link #POWERED}, stored with the facing),
 * strongly into the block it hangs on as a button does, so a school's bells can drive anything
 * else. The pulse is a scheduled block tick, not a ticking tile entity.</p>
 *
 * @since 2026.10
 */
public class BlockBellController extends BlockSchoolFixture implements ICsmTileEntityProvider {

  /** The controller's GUI id; unique across the mod. */
  public static final int GUI_ID = 44;

  /** Whether the controller is giving its pulse. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** The pulse's length, in ticks. */
  private static final int PULSE_TICKS = 20;

  /**
   * Constructs the controller.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockBellController(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(POWERED, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(POWERED) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(POWERED, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || player.isSneaking()) {
      return false;
    }
    player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  // --- the pulse -----------------------------------------------------------------------------

  /**
   * Starts the controller's pulse, if the block at {@code pos} is a controller.
   *
   * @param world the world
   * @param pos   the controller
   */
  static void pulse(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockBellController)) {
      return;
    }
    BlockBellController block = (BlockBellController) state.getBlock();
    if (!state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, true), 3);
      block.notifyBehind(world, pos, state);
    }
    world.scheduleUpdate(pos, block, PULSE_TICKS);
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, false), 3);
      notifyBehind(world, pos, state);
    }
  }

  private void notifyBehind(World world, BlockPos pos, IBlockState state) {
    world.notifyNeighborsOfStateChange(pos, this, false);
    world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()), this,
        false);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (state.getValue(POWERED)) {
      notifyBehind(world, pos, state);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
    return state.getValue(POWERED) ? 15 : 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos,
      EnumFacing side) {
    return state.getValue(POWERED) && state.getValue(FACING) == side ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  // --- tile entity ---------------------------------------------------------------------------

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBellController.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitybellcontroller";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityBellController();
  }
}
