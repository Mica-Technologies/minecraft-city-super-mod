package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A manual tool for small growth: the tree shears and the pole trimmer. Fast on leaves, moss and
 * twig or thin branches, by left-click or by right-click ({@link BranchCutting}); anything bigger
 * it refuses with a hint to fetch the chainsaw ({@link TreeToolEvents}). No fuel, and it wears out.
 * Not enchantable, like every CSM tool.
 *
 * @since 2026.10
 */
public abstract class ItemBranchTool extends AbstractItem {

  /** How fast a branch tool cuts what it may cut. */
  private static final float SPEED = 15.0F;

  protected ItemBranchTool(int durability) {
    super(durability, 1);
    setNoRepair();
    setFull3D();
  }

  /** The translation key of the tool's own usage line in its tooltip. */
  protected abstract String getUseTooltipKey();

  /** Tells the player a tree part is too big for this tool. */
  public void tooBig(EntityPlayer player) {
    player.sendStatusMessage(new TextComponentTranslation("csm.parks.tools.toobig",
        new TextComponentTranslation(getTranslationKey() + ".name")), true);
  }

  /**
   * Cuts at {@code pos} if this tool may: the shared right-click action.
   *
   * @return {@code SUCCESS} when cut (or about to be, on the client), {@code FAIL} when too big,
   *     {@code PASS} for anything that is not a tree
   */
  protected EnumActionResult tryCut(World world, EntityPlayer player, BlockPos pos,
      ItemStack stack) {
    BranchCutting.Verdict verdict = BranchCutting.judge(world, pos, world.getBlockState(pos));
    if (verdict == BranchCutting.Verdict.NOT_A_BRANCH) {
      return EnumActionResult.PASS;
    }
    if (verdict == BranchCutting.Verdict.TOO_BIG) {
      if (!world.isRemote) {
        tooBig(player);
      }
      return EnumActionResult.FAIL;
    }
    if (!world.isRemote && BranchCutting.cut(world, player, pos, stack)) {
      // A snip at a time, not one every four ticks while the button is held.
      player.getCooldownTracker().setCooldown(this, 5);
    }
    return EnumActionResult.SUCCESS;
  }

  // --- mining ---

  @Override
  public float getDestroySpeed(@Nonnull ItemStack stack, IBlockState state) {
    Material m = state.getMaterial();
    if (m == Material.LEAVES || m == Material.PLANTS || m == Material.VINE) {
      return SPEED;
    }
    if (state.getBlock() instanceof BlockTreeLog) {
      TreeLogWidth w = ((BlockTreeLog) state.getBlock()).getWidth();
      if (w == TreeLogWidth.TWIG
          || w == TreeLogWidth.THIN) {
        return SPEED;
      }
    }
    return 1.0F;
  }

  @Override
  public boolean onBlockDestroyed(@Nonnull ItemStack stack, @Nonnull World world,
      @Nonnull IBlockState state, @Nonnull BlockPos pos, @Nonnull EntityLivingBase entity) {
    if (!world.isRemote
        && BranchCutting.judge(world, pos, state) == BranchCutting.Verdict.CUT) {
      stack.damageItem(1, entity);
    }
    return true;
  }

  // --- no enchanting ---

  @Override
  public boolean isEnchantable(@Nonnull ItemStack stack) {
    return false;
  }

  @Override
  public int getItemEnchantability() {
    return 0;
  }

  @Override
  public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
    return false;
  }

  @Override
  public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
    return false;
  }

  // --- tooltip ---

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add(I18n.format(getUseTooltipKey()));
    tooltip.add(I18n.format("csm.parks.branch.tooltip.limit"));
  }
}
