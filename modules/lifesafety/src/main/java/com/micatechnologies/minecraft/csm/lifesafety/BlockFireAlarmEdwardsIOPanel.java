package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * An Edwards iO fire alarm control panel, in red or white. It is the same panel as
 * {@link BlockFireAlarmControlPanel} in every way that matters -- tile entity, GUI, linking,
 * alarm and storm behaviour -- and differs only in its tall, narrow cabinet and in the Edwards
 * sounds its buzzer makes.
 *
 * @since 2026.9
 */
public class BlockFireAlarmEdwardsIOPanel extends BlockFireAlarmControlPanel {

  /**
   * The registry name, for the {@code AbstractBlock} constructor, which asks for it before this
   * class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  /**
   * The cabinet: 15.6 wide by 26 tall (1.625 blocks, rising into the block above) by 3 deep,
   * centred on the wall. A real iO64 is 0.6 as wide as it is tall; any taller at that ratio would
   * be wider than the block.
   */
  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(0.0125, 0.0, 0.8125, 0.9875, 1.625, 1.0);

  private final String registryName;

  public BlockFireAlarmEdwardsIOPanel(String registryName) {
    this(initRegistryName(registryName), registryName);
  }

  private BlockFireAlarmEdwardsIOPanel(Void ignored, String registryName) {
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  public String getPanelTitle() {
    return "CSM iO64";
  }

  @Override
  public LifeSafetySounds getBuzzerAlarmSound() {
    return LifeSafetySounds.EDWARDS_IO_ALARM;
  }

  @Override
  public LifeSafetySounds getBuzzerTroubleSound() {
    return LifeSafetySounds.EDWARDS_IO_TROUBLE;
  }

  @Override
  public LifeSafetySounds getBuzzerResetSound() {
    return LifeSafetySounds.EDWARDS_IO_RESET;
  }
}
