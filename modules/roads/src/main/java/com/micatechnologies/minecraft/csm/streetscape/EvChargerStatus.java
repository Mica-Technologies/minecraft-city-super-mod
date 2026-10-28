package com.micatechnologies.minecraft.csm.streetscape;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * What an EV charger's status light shows (#315): green while it is available, blue while it is
 * charging, red when it has a fault. Stored in metadata bits 2 and 3, above the facing, so a
 * charger is placed in any state by command ({@code meta = facing | status << 2}).
 *
 * <p>Redstone decides between available and charging: a powered charger is charging. A fault
 * is the player's: a right-click with an empty hand sets it and another clears it, and while it
 * is set redstone changes nothing. Nothing ticks; the state is worked out again when a
 * neighbour changes or the charger is placed.</p>
 *
 * @since 2026.10
 */
public enum EvChargerStatus implements IStringSerializable {
  AVAILABLE("available"),
  CHARGING("charging"),
  FAULT("fault");

  /** The blockstate property, shared by every charger. */
  public static final PropertyEnum<EvChargerStatus> PROPERTY = PropertyEnum.create("status",
      EvChargerStatus.class);

  private final String name;

  EvChargerStatus(String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  /** The status stored in a metadata value's bits 2 and 3; an unused value reads available. */
  public static EvChargerStatus fromMeta(int meta) {
    int bits = (meta >> 2) & 3;
    return bits < values().length ? values()[bits] : AVAILABLE;
  }

  /** Metadata: the facing in bits 0 and 1, the status in bits 2 and 3. */
  public static int toMeta(IBlockState state, int facingMeta) {
    return (facingMeta & 3) | (state.getValue(PROPERTY).ordinal() << 2);
  }

  /**
   * Sets the status from the redstone at {@code pos}, unless the charger has a fault.
   *
   * @return the state the charger has now
   */
  public static IBlockState refresh(World world, BlockPos pos, IBlockState state) {
    if (world.isRemote || state.getValue(PROPERTY) == FAULT) {
      return state;
    }
    EvChargerStatus want = world.isBlockPowered(pos) ? CHARGING : AVAILABLE;
    if (state.getValue(PROPERTY) != want) {
      state = state.withProperty(PROPERTY, want);
      world.setBlockState(pos, state, 2);
    }
    return state;
  }

  /** Sets or clears a fault, and tells the player which. */
  public static void toggleFault(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (world.isRemote) {
      return;
    }
    if (state.getValue(PROPERTY) == FAULT) {
      // Cleared: back to whatever the redstone says.
      IBlockState cleared = state.withProperty(PROPERTY, AVAILABLE);
      world.setBlockState(pos, cleared, 2);
      cleared = refresh(world, pos, cleared);
      player.sendStatusMessage(new TextComponentString("EV charger: fault cleared ("
          + cleared.getValue(PROPERTY).getName() + ")"), true);
    } else {
      world.setBlockState(pos, state.withProperty(PROPERTY, FAULT), 2);
      player.sendStatusMessage(new TextComponentString("EV charger: fault"), true);
    }
  }
}
