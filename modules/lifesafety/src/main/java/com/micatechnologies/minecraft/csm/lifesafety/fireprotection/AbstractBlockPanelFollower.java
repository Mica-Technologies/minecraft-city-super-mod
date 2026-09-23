package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.api.firealarm.CsmFireAlarmQuery;
import com.micatechnologies.minecraft.csm.api.firealarm.FireAlarmPanelRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.lifesafety.IFireAlarmPanelFollower;
import com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmSensor;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A wall device that follows a fire alarm control panel ({@link IFireAlarmPanelFollower}): it
 * looks at the panel once a second and shows whether it is in alarm as {@link #ALARM}.
 *
 * <p>Linked, it follows its own panel, through {@link FireAlarmPanelRegistry} (the panels in
 * alarm), so a panel in an unloaded chunk is still known. Unlinked, it follows any fire alarm
 * sounding near it ({@link CsmFireAlarmQuery#isFireAlarmActiveNear}), which is what a player
 * who never picks up the linker expects of a door holder in a building with an alarm.</p>
 *
 * <p>Stored in metadata: the facing (two bits) and {@link #ALARM} (the third), so the model and
 * redstone read it without a tile entity lookup.</p>
 *
 * @since 2026.9
 */
public abstract class AbstractBlockPanelFollower extends BlockFireProtectionProp implements
    ICsmTileEntityProvider, IFireAlarmPanelFollower {

  public static final PropertyBool ALARM = PropertyBool.create("alarm");

  /** Ticks between looks at the panel. */
  private static final int POLL_TICKS = 20;

  protected AbstractBlockPanelFollower(String registryName, int[] box) {
    super(registryName, box, false);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(ALARM, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, ALARM);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(ALARM) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(ALARM, (meta & 4) != 0);
  }

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      world.scheduleUpdate(pos, this, POLL_TICKS);
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    boolean alarm = isInAlarm(world, pos);
    if (alarm != state.getValue(ALARM)) {
      world.setBlockState(pos, state.withProperty(ALARM, alarm), 3);
      onAlarmChanged(world, pos, alarm);
    }
    world.scheduleUpdate(pos, this, POLL_TICKS);
  }

  /** Whether the panel this device follows is in alarm. */
  public static boolean isInAlarm(World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    BlockPos panel = te instanceof TileEntityFireAlarmSensor
        ? ((TileEntityFireAlarmSensor) te).getLinkedPanelPos(world) : null;
    if (panel == null) {
      return CsmFireAlarmQuery.isFireAlarmActiveNear(world, pos);
    }
    return FireAlarmPanelRegistry.getActiveFireAlarmPanels(world.provider.getDimension())
        .contains(panel);
  }

  /**
   * Called on the server when {@link #ALARM} changes, after the new state is set.
   *
   * @param world the world
   * @param pos   the device
   * @param alarm whether the panel is now in alarm
   */
  protected void onAlarmChanged(World world, BlockPos pos, boolean alarm) {
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFireAlarmSensor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfirealarmsensor";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityFireAlarmSensor();
  }
}
