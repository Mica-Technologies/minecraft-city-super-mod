package com.micatechnologies.minecraft.csm.buildingmaterials;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Keeps a click meant for a door from landing on whatever is behind it.
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
 * <p><b>An open cathedral leaf.</b> Its latch column's half lies along the jamb in the cell inside
 * the hinge column's, a cell of air that the game's look ray never asks about, so a click on it
 * went to the wall behind: it placed the held block there, or broke the wall. A click whose ray
 * meets that half before what it landed on is the door's instead: a right click works the door as
 * a click on the leaf would (open, shut, link), and a left click does nothing.</p>
 *
 * @version 1.1
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
   * Gives a click on an open cathedral leaf's far half to the door, and otherwise denies the held
   * item's use when the click went through a door's cell.
   *
   * @param event the right click on a block
   *
   * @since 1.0
   */
  @SubscribeEvent
  public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    EntityPlayer player = event.getEntityPlayer();
    World world = event.getWorld();
    BlockPos target = event.getPos();
    Vec3d hit = event.getHitVec();
    if (hit == null) {
      return;
    }
    LeafHit leaf = openLeafHit(world, player, target, hit);
    if (leaf != null) {
      event.setCanceled(true);
      event.setCancellationResult(EnumActionResult.SUCCESS);
      if (!world.isRemote) {
        IBlockState state = world.getBlockState(leaf.pos);
        state.getBlock().onBlockActivated(world, leaf.pos, state, player, event.getHand(),
            leaf.face, (float) (leaf.hit.x - leaf.pos.getX()),
            (float) (leaf.hit.y - leaf.pos.getY()), (float) (leaf.hit.z - leaf.pos.getZ()));
      }
      return;
    }
    if (player.isSneaking() || event.getItemStack().isEmpty()) {
      return;
    }
    if (isDoor(world.getBlockState(target).getBlock())) {
      return; // a click on the door itself: the door handles it
    }
    if (throughDoor(world, player, target, hit)) {
      event.setUseItem(Event.Result.DENY);
    }
  }

  /**
   * Keeps a left click on an open cathedral leaf's far half from breaking the wall behind it.
   *
   * @param event the left click on a block
   *
   * @since 1.1
   */
  @SubscribeEvent
  public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
    Vec3d hit = event.getHitVec();
    if (hit != null && openLeafHit(event.getWorld(), event.getEntityPlayer(), event.getPos(), hit)
        != null) {
      event.setCanceled(true);
    }
  }

  /** Where a look ray met an open cathedral leaf, and the leaf's cell that has its box. */
  private static final class LeafHit {

    final BlockPos pos;
    final EnumFacing face;
    final Vec3d hit;

    LeafHit(BlockPos pos, EnumFacing face, Vec3d hit) {
      this.pos = pos;
      this.face = face;
      this.hit = hit;
    }
  }

  /**
   * Whether the ray from the player's eyes to the hit point meets an open cathedral leaf's latch
   * half before it reaches the target: walks the cells the ray crosses, and for each looks for a
   * hinge column cell beside it whose open leaf reaches into it ({@code hinge.offset(facing)}).
   */
  @Nullable
  private static LeafHit openLeafHit(World world, EntityPlayer player, BlockPos target,
      Vec3d hit) {
    Vec3d eye = player.getPositionEyes(1.0F);
    Vec3d d = hit.subtract(eye);
    double length = d.length();
    if (length < 1.0E-6 || length > 8.0) {
      return null;
    }
    int steps = MathHelper.ceil(length / STEP);
    BlockPos last = null;
    for (int i = 0; i <= steps; i++) {
      double t = (double) i / steps;
      BlockPos p = new BlockPos(eye.x + d.x * t, eye.y + d.y * t, eye.z + d.z * t);
      if (p.equals(last)) {
        continue;
      }
      last = p;
      if (p.equals(target) || !world.isBlockLoaded(p)) {
        break;
      }
      for (EnumFacing f : EnumFacing.HORIZONTALS) {
        BlockPos h = p.offset(f.getOpposite());
        IBlockState s = world.getBlockState(h);
        if (!(s.getBlock() instanceof BlockCathedralDoor) || !s.getValue(BlockCathedralDoor.OPEN)
            || s.getValue(BlockCathedralDoor.FACING) != f) {
          continue;
        }
        BlockCathedralDoor door = (BlockCathedralDoor) s.getBlock();
        if (door.column(world::getBlockState, h, s) != BlockCathedralDoor.Column.HINGE) {
          continue;
        }
        AxisAlignedBB box = s.getBoundingBox(world, h).offset(h);
        RayTraceResult r = box.calculateIntercept(eye, hit);
        if (r != null) {
          return new LeafHit(h, r.sideHit, r.hitVec);
        }
      }
    }
    return null;
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
      if (isDoor(world.getBlockState(p).getBlock())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDoor(Block block) {
    return block instanceof BlockBuildingDoor || block instanceof BlockCathedralDoor;
  }
}
