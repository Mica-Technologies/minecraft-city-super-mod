package com.micatechnologies.minecraft.csm.constructionsite;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Keeps a jumping player from clearing a scaffold guardrail.
 *
 * <p>A guardrail belongs to the deck block below the space it guards, so its collision reaches up
 * out of that block's cell. 1.12 only asks blocks from one below an entity's feet upward for
 * collision, so once a jumping player's feet are more than a block above the deck, the deck is no
 * longer asked, and for the top of the jump the rail is not there. Sprint-jumping at a rail
 * cleared it every time. A vanilla fence never has this problem because it lives in the cell at
 * the feet.</p>
 *
 * <p>This asks the one row the world skips, two below the feet, for guardrail boxes. Players only,
 * which are the only thing that sprint-jumps at a rail on purpose, and it costs a handful of block
 * lookups per collision query. Registered on both sides, so the server's idea of where a player
 * can go matches the client's.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class ScaffoldRailCollision {

  private ScaffoldRailCollision() {
  }

  /**
   * Registers the handler. Call from pre-initialization, on both sides.
   *
   * @since 1.0
   */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new ScaffoldRailCollision());
  }

  /**
   * Adds the guardrail boxes of any scaffold two cells below the queried box.
   *
   * @param event the collision query
   *
   * @since 1.0
   */
  @SubscribeEvent
  public void onGetCollisionBoxes(GetCollisionBoxesEvent event) {
    Entity entity = event.getEntity();
    if (!(entity instanceof EntityPlayer)) {
      return;
    }
    World world = event.getWorld();
    AxisAlignedBB box = event.getAabb();
    int y = MathHelper.floor(box.minY) - 2;
    int x0 = MathHelper.floor(box.minX) - 1;
    int x1 = MathHelper.floor(box.maxX) + 1;
    int z0 = MathHelper.floor(box.minZ) - 1;
    int z1 = MathHelper.floor(box.maxZ) + 1;
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    for (int x = x0; x <= x1; x++) {
      for (int z = z0; z <= z1; z++) {
        pos.setPos(x, y, z);
        if (!world.isBlockLoaded(pos)) {
          continue;
        }
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BlockScaffoldFrame) {
          BlockScaffoldFrame.addRailBoxes(world, pos.toImmutable(), box,
              event.getCollisionBoxesList());
        }
      }
    }
  }
}
