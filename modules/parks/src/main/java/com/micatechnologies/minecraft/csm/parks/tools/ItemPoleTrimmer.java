package com.micatechnologies.minecraft.csm.parks.tools;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The pole trimmer (pole saw and pruner): the tree shears on a long pole. Right-click a branch,
 * leaves or moss up to {@link #REACH} blocks away to cut it, so a canopy can be trimmed from the
 * ground.
 *
 * <p>Right-click, not left: the game's own reach decides what a left-click mines, so the long
 * reach is this item's own ray trace, run on both sides from the player's eyes.</p>
 *
 * @since 2026.10
 */
public class ItemPoleTrimmer extends ItemBranchTool {

  /** How far the trimmer reaches, in blocks. */
  public static final double REACH = 8.0;

  public ItemPoleTrimmer() {
    super(350);
  }

  @Override
  public String getItemRegistryName() {
    return "pole_trimmer";
  }

  @Override
  protected String getUseTooltipKey() {
    return "csm.parks.trimmer.tooltip.use";
  }

  @Override
  @Nonnull
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    Vec3d eyes = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
    Vec3d end = eyes.add(player.getLookVec().scale(REACH));
    RayTraceResult hit = world.rayTraceBlocks(eyes, end, false, false, false);
    if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
      return new ActionResult<>(EnumActionResult.PASS, stack);
    }
    return new ActionResult<>(tryCut(world, player, hit.getBlockPos(), stack), stack);
  }
}
