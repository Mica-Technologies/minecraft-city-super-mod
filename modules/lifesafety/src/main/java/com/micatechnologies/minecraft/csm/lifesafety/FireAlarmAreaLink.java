package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.api.CsmFireAlarmLink;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Links or unlinks every fire alarm device in a box to one panel at once: what the area linker
 * does with two corner clicks and {@code /csmfirealarm link} does from a command.
 * <p>
 * A building of a few hundred appliances took one aimed click each, and a wall speaker is a plate
 * an eighth of a block deep, so the crosshair had to find every one. The box makes the panel's
 * wiring a matter of where the building is rather than of aiming at it.
 * <p>
 * Each device is treated exactly as a linker click on it would treat it: appliances go into the
 * panel's list, initiating devices are pointed at the panel and indexed on it, and followers (door
 * holders, annunciators) are pointed at it. A device linked to another panel is moved, as a click
 * moves it; an appliance listed on another panel stays listed there too, as it would after a
 * click, since appliances keep no back-reference to find it by.
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public final class FireAlarmAreaLink {

  /**
   * The most blocks one box may cover: 128 x 128 x 128. A 23-floor tower with a generous margin
   * is about a quarter of this. Every cell is read, so the cap is what bounds the server tick.
   */
  public static final long MAX_VOLUME = 128L * 128L * 128L;

  private FireAlarmAreaLink() {
  }

  /** What one pass over a box did. */
  public static final class Result {

    public int appliancesLinked;
    public int appliancesAlready;
    public int initiatingLinked;
    public int initiatingMoved;
    public int initiatingAlready;
    public int followersLinked;
    public int followersAlready;
    /** Unlinking only: devices that were linked to this panel and no longer are. */
    public int unlinked;
    /** Cells skipped because their chunk was not loaded. */
    public int unloaded;

    public int changed() {
      return appliancesLinked + initiatingLinked + initiatingMoved + followersLinked + unlinked;
    }

    /** One line for chat: what was linked, what already was, and what could not be reached. */
    public String describeLink() {
      StringBuilder sb = new StringBuilder("Linked ")
          .append(appliancesLinked).append(plural(appliancesLinked, " appliance", " appliances"))
          .append(", ").append(initiatingLinked + initiatingMoved)
          .append(plural(initiatingLinked + initiatingMoved, " initiating device",
              " initiating devices"));
      if (initiatingMoved > 0) {
        sb.append(" (").append(initiatingMoved).append(" moved from another panel)");
      }
      if (followersLinked > 0) {
        sb.append(", ").append(followersLinked)
            .append(plural(followersLinked, " follower", " followers"));
      }
      int already = appliancesAlready + initiatingAlready + followersAlready;
      sb.append("; ").append(already).append(" already linked");
      if (unloaded > 0) {
        sb.append("; ").append(unloaded).append(" blocks not loaded were skipped");
      }
      return sb.toString();
    }

    public String describeUnlink() {
      String text = "Unlinked " + unlinked + plural(unlinked, " device", " devices");
      return unloaded > 0 ? text + "; " + unloaded + " blocks not loaded were skipped" : text;
    }

    private static String plural(int n, String one, String many) {
      return n == 1 ? one : many;
    }
  }

  /**
   * How many blocks the box between two corners covers, both corners included.
   */
  public static long volume(BlockPos a, BlockPos b) {
    return (Math.abs((long) a.getX() - b.getX()) + 1)
        * (Math.abs((long) a.getY() - b.getY()) + 1)
        * (Math.abs((long) a.getZ() - b.getZ()) + 1);
  }

  /**
   * Links every device in the box between two corners to a panel. Server side only.
   *
   * @throws IllegalArgumentException if the box is larger than {@link #MAX_VOLUME}
   */
  public static Result link(World world, TileEntityFireAlarmControlPanel panel, BlockPos a,
      BlockPos b) {
    return link(world, panel, a, b, FireAlarmZones.GENERAL);
  }

  /**
   * Links every device in the box to a panel in one zone (#335): appliances and initiating
   * devices are stamped with it, and ones already linked move into it. Followers have no zone.
   *
   * @throws IllegalArgumentException if the box is larger than {@link #MAX_VOLUME}
   * @since 2026.11
   */
  public static Result link(World world, TileEntityFireAlarmControlPanel panel, BlockPos a,
      BlockPos b, int zone) {
    checkVolume(a, b);
    BlockPos panelPos = panel.getPos();
    Result result = new Result();
    for (BlockPos.MutableBlockPos cell : BlockPos.getAllInBoxMutable(a, b)) {
      if (!world.isBlockLoaded(cell)) {
        result.unloaded++;
        continue;
      }
      Block block = world.getBlockState(cell).getBlock();
      if (block instanceof CsmFireAlarmLink.Follower) {
        // another module's follower (a fare gate, a station gate)
        if (((CsmFireAlarmLink.Follower) block).followFireAlarmPanel(world, cell.toImmutable(),
            panelPos)) {
          result.followersLinked++;
        } else {
          result.followersAlready++;
        }
      } else if (block instanceof IFireAlarmPanelFollower) {
        TileEntity te = world.getTileEntity(cell);
        if (te instanceof TileEntityFireAlarmSensor) {
          if (((TileEntityFireAlarmSensor) te).setLinkedPanelPos(panelPos)
              == TileEntityFireAlarmSensor.LinkResult.ALREADY_LINKED) {
            result.followersAlready++;
          } else {
            result.followersLinked++;
          }
        }
      } else if (block instanceof AbstractBlockFireAlarmSounder) {
        if (panel.addLinkedAlarm(cell.toImmutable(), zone)) {
          result.appliancesLinked++;
        } else {
          result.appliancesAlready++;
        }
      } else if (block instanceof AbstractBlockFireAlarmActivator) {
        TileEntity te = world.getTileEntity(cell);
        if (te instanceof TileEntityFireAlarmSensor) {
          TileEntityFireAlarmSensor sensor = (TileEntityFireAlarmSensor) te;
          TileEntityFireAlarmSensor.LinkResult linked = sensor.setLinkedPanelPos(panelPos);
          boolean rezoned = sensor.setZone(zone);
          // a residential alarm's system is the panel now, not its old group of alarms
          sensor.setInterconnectGroup(null);
          boolean indexed = panel.addLinkedInitiatingDevice(cell.toImmutable());
          if (linked == TileEntityFireAlarmSensor.LinkResult.RELINKED) {
            result.initiatingMoved++;
          } else if (linked == TileEntityFireAlarmSensor.LinkResult.ALREADY_LINKED && !indexed
              && !rezoned) {
            result.initiatingAlready++;
          } else {
            result.initiatingLinked++;
          }
        }
      }
    }
    if (result.changed() > 0) {
      panel.afterBulkLink();
    }
    return result;
  }

  /**
   * Unlinks every device in the box from a panel: the area form of a sneak-click. Devices linked
   * to some other panel are left alone. Server side only.
   *
   * @throws IllegalArgumentException if the box is larger than {@link #MAX_VOLUME}
   */
  public static Result unlink(World world, TileEntityFireAlarmControlPanel panel, BlockPos a,
      BlockPos b) {
    checkVolume(a, b);
    BlockPos panelPos = panel.getPos();
    Result result = new Result();
    Set<BlockPos> indexedBefore = new HashSet<>(panel.getLinkedInitiatingDevices());
    // The panel's own lists are walked rather than the box, so a device that has since been
    // broken is unlinked too; only the devices' own back-references need the world.
    result.unlinked = panel.removeLinkedDevices(p -> inBox(p, a, b));
    for (BlockPos.MutableBlockPos cell : BlockPos.getAllInBoxMutable(a, b)) {
      if (!world.isBlockLoaded(cell)) {
        result.unloaded++;
        continue;
      }
      Block block = world.getBlockState(cell).getBlock();
      if (!(block instanceof IFireAlarmPanelFollower)
          && !(block instanceof AbstractBlockFireAlarmActivator)) {
        continue;
      }
      TileEntity te = world.getTileEntity(cell);
      if (te instanceof TileEntityFireAlarmSensor
          && panelPos.equals(((TileEntityFireAlarmSensor) te).getLinkedPanelPos(world))) {
        ((TileEntityFireAlarmSensor) te).clearLinkedPanel();
        // Followers are not on the panel's lists, so they count here. An initiating device was
        // counted as it left the panel's index, unless it reported to the panel without being on
        // it (linked before the panel kept an index).
        if (block instanceof IFireAlarmPanelFollower || !indexedBefore.contains(cell)) {
          result.unlinked++;
        }
      }
    }
    return result;
  }

  /**
   * Interconnects every residential alarm in the box with a home system of alarms that has no
   * panel (#335): the area form of clicking one alarm and then each of the others. Alarms that
   * report to a panel are moved off it. Server side only.
   *
   * @param head the alarm the system is keyed by: its group if it has one, else its own position
   *
   * @return how many joined ({@link Result#initiatingLinked}) and how many already had
   *     ({@link Result#initiatingAlready})
   * @throws IllegalArgumentException if the box is larger than {@link #MAX_VOLUME}
   * @since 2026.11
   */
  public static Result interconnect(World world, TileEntityFireAlarmSensor head, BlockPos a,
      BlockPos b) {
    checkVolume(a, b);
    Result result = new Result();
    BlockPos group = head.getInterconnectGroup();
    if (group == null) {
      group = head.getPos();
      head.setInterconnectGroup(group);
    }
    for (BlockPos.MutableBlockPos cell : BlockPos.getAllInBoxMutable(a, b)) {
      if (!world.isBlockLoaded(cell)) {
        result.unloaded++;
        continue;
      }
      if (!(world.getBlockState(cell).getBlock() instanceof BlockResidentialAlarm)) {
        continue;
      }
      TileEntity te = world.getTileEntity(cell);
      if (!(te instanceof TileEntityFireAlarmSensor)) {
        continue;
      }
      TileEntityFireAlarmSensor alarm = (TileEntityFireAlarmSensor) te;
      BlockPos oldPanel = alarm.getLinkedPanelPos(world);
      if (oldPanel != null) {
        alarm.clearLinkedPanel();
        TileEntity panelTe = world.getTileEntity(oldPanel);
        if (panelTe instanceof TileEntityFireAlarmControlPanel) {
          ((TileEntityFireAlarmControlPanel) panelTe).removeLinkedInitiatingDevice(
              cell.toImmutable());
        }
      }
      if (alarm.setInterconnectGroup(group)) {
        result.initiatingLinked++;
      } else {
        result.initiatingAlready++;
      }
    }
    return result;
  }

  /**
   * Takes every residential alarm in the box off the home system a head alarm is on.
   *
   * @throws IllegalArgumentException if the box is larger than {@link #MAX_VOLUME}
   * @since 2026.11
   */
  public static Result disconnect(World world, TileEntityFireAlarmSensor head, BlockPos a,
      BlockPos b) {
    checkVolume(a, b);
    Result result = new Result();
    BlockPos group = head.getInterconnectGroup();
    for (BlockPos.MutableBlockPos cell : BlockPos.getAllInBoxMutable(a, b)) {
      if (!world.isBlockLoaded(cell)) {
        result.unloaded++;
        continue;
      }
      if (group == null
          || !(world.getBlockState(cell).getBlock() instanceof BlockResidentialAlarm)) {
        continue;
      }
      TileEntity te = world.getTileEntity(cell);
      if (te instanceof TileEntityFireAlarmSensor
          && group.equals(((TileEntityFireAlarmSensor) te).getInterconnectGroup())) {
        ((TileEntityFireAlarmSensor) te).setInterconnectGroup(null);
        result.unlinked++;
      }
    }
    return result;
  }

  /** One line for chat after {@link #interconnect}. */
  public static String describeInterconnect(Result result) {
    String text = "Interconnected " + result.initiatingLinked + " alarm"
        + (result.initiatingLinked == 1 ? "" : "s") + "; " + result.initiatingAlready
        + " already were";
    return result.unloaded > 0 ? text + "; " + result.unloaded
        + " blocks not loaded were skipped" : text;
  }

  private static void checkVolume(BlockPos a, BlockPos b) {
    long volume = volume(a, b);
    if (volume > MAX_VOLUME) {
      throw new IllegalArgumentException("That box is " + volume + " blocks; the most is "
          + MAX_VOLUME + " (128 x 128 x 128). Link a large building in parts.");
    }
  }

  private static boolean inBox(BlockPos p, BlockPos a, BlockPos b) {
    return p.getX() >= Math.min(a.getX(), b.getX()) && p.getX() <= Math.max(a.getX(), b.getX())
        && p.getY() >= Math.min(a.getY(), b.getY()) && p.getY() <= Math.max(a.getY(), b.getY())
        && p.getZ() >= Math.min(a.getZ(), b.getZ()) && p.getZ() <= Math.max(a.getZ(), b.getZ());
  }
}
