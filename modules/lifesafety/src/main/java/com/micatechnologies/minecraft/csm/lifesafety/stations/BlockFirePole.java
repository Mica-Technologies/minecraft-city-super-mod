package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * A firehouse brass pole, stacked floor to floor. Right-click it to grab it: you are pulled onto
 * the pole (a flying player stops flying) and slide down at a steady pace, faster than a ladder,
 * held to the pole so you do not drift off it, and with no fall damage at the bottom. Dropping or
 * stepping into its cell does the same. Sneaking grips the pole and holds you where you are.
 *
 * <p>The slide is applied as the entity moves through the cell ({@link #onEntityCollision}): a
 * player's motion is decided on its own client, so it needs no packet and no tile entity. Fall
 * damage is decided on the server from the distance the client reports, which a reset here does
 * not reach, so {@link FallHandler} cancels the fall of anything that lands in a pole's cell.</p>
 *
 * @since 2026.9
 */
public class BlockFirePole extends AbstractBlock {

  /** Blocks per tick a slide settles at: about ten a second. */
  public static final double SLIDE_SPEED = 0.5;

  private static final AxisAlignedBB POLE = new AxisAlignedBB(6.5 / 16, 0, 6.5 / 16, 9.5 / 16, 1,
      9.5 / 16);

  public BlockFirePole() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 2.0F, 6.0F, 0.0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "fire_pole";
  }

  @Override
  public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
    slide(entity, pos);
  }

  /**
   * Grabs the pole: the player is put on it, stops flying, and starts down. Both sides run this,
   * since the client decides a player's own motion and flight.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (player.capabilities.isFlying) {
      player.capabilities.isFlying = false;
      if (world.isRemote) {
        player.sendPlayerAbilities();
      }
    }
    // Onto the pole, at the height the player already is, clear of the floor they stood on.
    double y = Math.min(player.posY, pos.getY() + 0.9);
    player.setPositionAndUpdate(pos.getX() + 0.5, Math.max(y, pos.getY() + 0.1),
        pos.getZ() + 0.5);
    player.motionX = 0;
    player.motionZ = 0;
    player.motionY = -SLIDE_SPEED;
    player.fallDistance = 0;
    return true;
  }

  /**
   * Holds an entity to the pole: a steady slide down, or a grip while it sneaks, and a pull toward
   * the pole's axis so a slide does not carry the player off it.
   */
  static void slide(Entity entity, BlockPos pos) {
    if (!(entity instanceof EntityLivingBase) || entity.onGround) {
      return;
    }
    if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isFlying) {
      return;
    }
    if (entity.isSneaking()) {
      entity.motionY = 0;
    } else if (entity.motionY < -SLIDE_SPEED || entity.motionY > 0) {
      entity.motionY = -SLIDE_SPEED;
    }
    entity.motionX = (pos.getX() + 0.5 - entity.posX) * 0.3;
    entity.motionZ = (pos.getZ() + 0.5 - entity.posZ) * 0.3;
    entity.fallDistance = 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return POLE;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return true;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  /**
   * Cancels fall damage for anything that lands on a fire pole, at its foot or anywhere along
   * it. Registered on the Forge event bus from the module's pre-initialization.
   */
  public static class FallHandler {

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
      Entity entity = event.getEntity();
      BlockPos feet = new BlockPos(entity.posX, entity.posY + 0.1, entity.posZ);
      if (entity.world.getBlockState(feet).getBlock() instanceof BlockFirePole
          || entity.world.getBlockState(feet.down()).getBlock() instanceof BlockFirePole) {
        event.setDistance(0);
        event.setCanceled(true);
      }
    }
  }
}
