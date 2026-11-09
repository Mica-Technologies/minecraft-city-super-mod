package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A home fire and security keypad (#335): the small panel of a house's alarm system, the keypad
 * by the front door. It is a fire alarm control panel in every way that matters -- tile entity,
 * screen, linking, zones, drill, silence, reset -- as {@link BlockFireAlarmEdwardsIOPanel} is,
 * and differs in its cabinet, its title and its keypad beeps.
 *
 * <p>A home needs no panel: residential smoke and CO alarms interconnect with each other on
 * their own ({@link ResidentialInterconnect}). Linked to a keypad instead, they report to it, it
 * shows which one tripped and in which zone, and it sounds every alarm in the house from one
 * place, with the horns or sirens linked to it as appliances.</p>
 *
 * @since 2026.11
 */
public class BlockFireAlarmHomePanel extends BlockFireAlarmControlPanel {

  /**
   * The registry name, for the {@code AbstractBlock} constructor, which asks for it before this
   * class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;

  public BlockFireAlarmHomePanel(String registryName, AxisAlignedBB boundingBox) {
    this(initRegistryName(registryName), registryName, boundingBox);
  }

  private BlockFireAlarmHomePanel(Void ignored, String registryName, AxisAlignedBB boundingBox) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
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
    return boundingBox;
  }

  @Override
  public String getPanelTitle() {
    return "CSM HOME";
  }

  @Override
  public String getPanelSubtitle() {
    return "HOME FIRE & SECURITY";
  }

  @Override
  public LifeSafetySounds getBuzzerAlarmSound() {
    return LifeSafetySounds.HOME_KEYPAD_ALARM;
  }

  @Override
  public LifeSafetySounds getBuzzerTroubleSound() {
    return LifeSafetySounds.HOME_KEYPAD_TROUBLE;
  }

  @Override
  public LifeSafetySounds getBuzzerResetSound() {
    return LifeSafetySounds.HOME_KEYPAD_RESET;
  }
}
