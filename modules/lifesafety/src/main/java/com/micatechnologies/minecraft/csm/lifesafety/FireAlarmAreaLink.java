package com.micatechnologies.minecraft.csm.lifesafety;

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
    checkVolume(a, b);
    BlockPos panelPos = panel.getPos();
    Result result = new Result();
    for (BlockPos.MutableBlockPos cell : BlockPos.getAllInBoxMutable(a, b)) {
      if (!world.isBlockLoaded(cell)) {
        result.unloaded++;
        continue;
      }
      Block block = world.getBlockState(cell).getBlock();
      if (block instanceof IFireAlarmPanelFollower) {
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
        if (panel.addLinkedAlarm(cell.toImmutable())) {
          result.appliancesLinked++;
        } else {
          result.appliancesAlready++;
        }
      } else if (block instanceof AbstractBlockFireAlarmActivator) {
        TileEntity te = world.getTileEntity(cell);
        if (te instanceof TileEntityFireAlarmSensor) {
          TileEntityFireAlarmSensor.LinkResult linked =
              ((TileEntityFireAlarmSensor) te).setLinkedPanelPos(panelPos);
          boolean indexed = panel.addLinkedInitiatingDevice(cell.toImmutable());
          if (linked == TileEntityFireAlarmSensor.LinkResult.RELINKED) {
            result.initiatingMoved++;
          } else if (linked == TileEntityFireAlarmSensor.LinkResult.ALREADY_LINKED && !indexed) {
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
