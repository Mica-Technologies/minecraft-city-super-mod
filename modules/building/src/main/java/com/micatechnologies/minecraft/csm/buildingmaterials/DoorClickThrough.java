package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Keeps a click meant for a door from placing the held block on whatever is behind it.
 *
 * <p>A click lands on the first box the look ray meets, and a door's box is not always where the
 * door is drawn: a swinging door's box jumps to where the swing ends as it starts (so a second
 * click while it swings goes through the doorway), and a sliding or splitting door leaves its cell
 * clear once open. The click then lands on the floor or wall beyond, and the held block was placed
 * there -- "blocks sometimes place when using doors".</p>
 *
 * <p>So a right click whose ray passes through a door's cell on the way to something else does not
 * use the held item. The block it lands on still gets its own click (a switch through a doorway
 * still works), and sneaking still places, which is how vanilla lets a player force a placement.
 * Registered on both sides, so the client does not show a block the server will not place.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class DoorClickThrough {

  /** Step along the look ray: fine enough that a ray cannot cross a door's cell between samples. */
  private static final double STEP = 0.05;

  private DoorClickThrough() {
  }

  /**
   * Registers the handler. Call from pre-initialization, on both sides.
   *
   * @since 1.0
   */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new DoorClickThrough());
  }

  /**
   * Denies the held item's use when the click went through a door's cell.
   *
   * @param event the right click on a block
   *
   * @since 1.0
   */
  @SubscribeEvent
  public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    EntityPlayer player = event.getEntityPlayer();
    if (player.isSneaking() || event.getItemStack().isEmpty()) {
      return;
    }
    World world = event.getWorld();
    BlockPos target = event.getPos();
    if (world.getBlockState(target).getBlock() instanceof BlockBuildingDoor) {
      return; // a click on the door itself: the door handles it
    }
    Vec3d hit = event.getHitVec();
    if (hit != null && throughDoor(world, player, target, hit)) {
      event.setUseItem(Event.Result.DENY);
    }
  }

  /**
   * Whether the segment from the player's eyes to the hit point crosses a door's cell before the
   * target's. The cells the player stands in are left out, so standing in a doorway still places.
   */
  private static boolean throughDoor(World world, EntityPlayer player, BlockPos target,
      Vec3d hit) {
    Vec3d eye = player.getPositionEyes(1.0F);
    BlockPos eyeCell = new BlockPos(eye);
    BlockPos feetCell = new BlockPos(player.posX, player.posY, player.posZ);
    Vec3d d = hit.subtract(eye);
    double length = d.length();
    if (length < 1.0E-6 || length > 8.0) {
      return false;
    }
    int steps = MathHelper.ceil(length / STEP);
    BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
    int lastX = Integer.MIN_VALUE;
    int lastY = Integer.MIN_VALUE;
    int lastZ = Integer.MIN_VALUE;
    for (int i = 0; i < steps; i++) {
      double t = (double) i / steps;
      int x = MathHelper.floor(eye.x + d.x * t);
      int y = MathHelper.floor(eye.y + d.y * t);
      int z = MathHelper.floor(eye.z + d.z * t);
      if (x == lastX && y == lastY && z == lastZ) {
        continue;
      }
      lastX = x;
      lastY = y;
      lastZ = z;
      p.setPos(x, y, z);
      if (p.equals(target)) {
        return false;
      }
      if (p.equals(eyeCell) || p.equals(feetCell) || !world.isBlockLoaded(p)) {
        continue;
      }
      if (world.getBlockState(p).getBlock() instanceof BlockBuildingDoor) {
        return true;
      }
    }
    return false;
  }
}
