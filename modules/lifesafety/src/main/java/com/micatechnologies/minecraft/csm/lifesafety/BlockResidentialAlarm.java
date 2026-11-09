package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * A residential smoke or carbon monoxide alarm (#335): a device that both detects and sounds, and
 * that interconnects with the other alarms of its home, as hard-wired 120 V alarms do.
 *
 * <p>Smoke alarms sound the temporal-three pattern, CO alarms temporal-four, and a combination
 * alarm (the Nest Protect) sounds as a smoke alarm. Neither gas nor smoke exists in the game, so
 * both kinds look for fire exactly as every detector does (a fire is a source of both).</p>
 *
 * <p>How an alarm finds its system is {@link ResidentialInterconnect}'s: linked to a panel, it
 * reports to the panel and sounds with it; joined to other alarms with the linker, it sounds
 * with them; placed and never linked, it does nothing at all. That last case is the point of
 * building these in place of the decorative blocks that had the same registry names: every
 * {@code kiddesmoke}, {@code adt_co_detector} and {@code nestprotect} already in a world keeps its
 * metadata (the facing, which this base stores the same way), gets no tile entity until a linker
 * touches it, and stays silent until it is linked.</p>
 *
 * <p>The test button (a right-click with an empty hand or anything but a linker) sounds the
 * interconnected alarms for two cycles, or hushes them for ten minutes while they sound.</p>
 *
 * @since 2026.11
 */
public class BlockResidentialAlarm extends AbstractBlockFireAlarmDetector {

  /** How loud one cycle plays: heard to about 16 blocks, a house's worth. */
  public static final float VOLUME = 1.0F;

  /** What an alarm senses, and so the signal it sounds when it is the one that trips. */
  public enum Kind {
    SMOKE,
    CARBON_MONOXIDE,
    /** Smoke and CO in one: sounds as a smoke alarm. */
    COMBINATION
  }

  /**
   * The registry name, for the {@code AbstractBlock} constructor, which asks for it before this
   * class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final Kind kind;
  private final int lightValue;

  /**
   * @param registryName the block's registry name
   * @param boundingBox  its box, facing north
   * @param kind         what it senses
   * @param lightValue   the light it gives, 0-15 (the Nest Protect's ring glows faintly)
   */
  public BlockResidentialAlarm(String registryName, AxisAlignedBB boundingBox, Kind kind,
      int lightValue) {
    this(initRegistryName(registryName), registryName, boundingBox, kind, lightValue);
  }

  private BlockResidentialAlarm(Void ignored, String registryName, AxisAlignedBB boundingBox,
      Kind kind, int lightValue) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.kind = kind;
    this.lightValue = lightValue;
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

  public Kind getKind() {
    return kind;
  }

  /** The signal this alarm sounds when it is the one that detected. */
  public ResidentialInterconnect.Pattern getPattern() {
    return kind == Kind.CARBON_MONOXIDE ? ResidentialInterconnect.Pattern.CO
        : ResidentialInterconnect.Pattern.SMOKE;
  }

  @Override
  protected boolean reportsCarbonMonoxide() {
    return kind == Kind.CARBON_MONOXIDE;
  }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return lightValue;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }

  /**
   * Looks for fire, but only for an alarm on a system: an unlinked one costs nothing and does
   * nothing. No server-wide chat line, unlike the commercial detectors: a house has no central
   * station listening.
   */
  @Override
  public void onTick(World world, BlockPos blockPos, IBlockState blockState) {
    TileEntity te = world.getTileEntity(blockPos);
    if (!(te instanceof TileEntityFireAlarmSensor)) {
      return;
    }
    TileEntityFireAlarmSensor sensor = (TileEntityFireAlarmSensor) te;
    if (!sensor.hasLinkedPanel() && sensor.getInterconnectGroup() == null) {
      return;
    }
    if (findFire(world, blockPos) == null) {
      return;
    }
    trip(world, blockPos, sensor);
  }

  /**
   * This alarm detected (or a command triggered it): its panel goes into alarm and sounds the
   * home, or its group sounds. An alarm whose panel cannot be reached sounds on its own.
   */
  void trip(World world, BlockPos blockPos, TileEntityFireAlarmSensor sensor) {
    if (sensor.hasLinkedPanel()) {
      if (!activateLinkedPanel(world, blockPos, null)) {
        ResidentialInterconnect.trip(world, blockPos, getPattern());
      }
    } else if (sensor.getInterconnectGroup() != null) {
      ResidentialInterconnect.trip(world, sensor.getInterconnectGroup(), getPattern());
    }
  }

  @Override
  public void onFire(World world, BlockPos blockPos, IBlockState blockState, BlockPos firePos) {
    // Sounds through its system; see onTick.
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    ItemStack held = player.getHeldItem(hand);
    if (held.getItem() instanceof ItemFireAlarmLinker
        || held.getItem() instanceof ItemFireAlarmConfigTool) {
      return false;
    }
    // CHECK, not world.getTileEntity: an alarm placed before it became one has no tile entity,
    // and a right-click on it should not make one
    TileEntity te = world.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
    if (!(te instanceof TileEntityFireAlarmSensor)) {
      return false;
    }
    TileEntityFireAlarmSensor sensor = (TileEntityFireAlarmSensor) te;
    BlockPos panel = sensor.getLinkedPanelPos(world);
    BlockPos group = sensor.getInterconnectGroup();
    if (panel == null && group == null) {
      // Never linked: a plain block, as it always was
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    if (panel != null) {
      player.sendMessage(new TextComponentString("This alarm reports to the fire alarm panel at ("
          + panel.getX() + "," + panel.getY() + "," + panel.getZ() + "), "
          + FireAlarmZones.describeZone(sensor.getZone())
          + ". Test the home from the panel (DRILL)."));
    } else if (ResidentialInterconnect.testOrHush(world, group, getPattern())) {
      player.sendMessage(new TextComponentString(
          "Hushed the interconnected alarms for ten minutes."));
    } else {
      player.sendMessage(new TextComponentString("Testing the interconnected alarms."));
    }
    return true;
  }

  @Override
  @ParametersAreNonnullByDefault
  public void addInformation(ItemStack stack, World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add(kind == Kind.CARBON_MONOXIDE ? "Interconnected CO alarm (temporal-four)"
        : "Interconnected smoke alarm (temporal-three)");
    tooltip.add("Link to a home keypad, or click one alarm then the others with a linker");
    tooltip.add("Right-click to test; again while sounding to hush");
  }
}
