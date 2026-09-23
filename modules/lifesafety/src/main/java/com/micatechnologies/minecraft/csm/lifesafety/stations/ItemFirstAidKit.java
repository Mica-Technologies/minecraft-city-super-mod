package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A first aid kit. Hold right-click for a second and a half to patch yourself up: four hearts,
 * one kit used. Kits then rest for {@link #COOLDOWN_TICKS}, so they heal a scrape rather than win
 * a fight; the cooldown shows on the hotbar. It will not open at full health.
 *
 * @since 2026.9
 */
public class ItemFirstAidKit extends AbstractItem {

  /** Health restored, in half hearts. */
  public static final float HEAL = 8F;
  /** Ticks to use. */
  public static final int USE_TICKS = 30;
  /** Ticks every kit rests after one is used. */
  public static final int COOLDOWN_TICKS = 600;

  public ItemFirstAidKit() {
    super(0, 8);
  }

  @Override
  public String getItemRegistryName() {
    return "first_aid_kit";
  }

  @Override
  @Nonnull
  public EnumAction getItemUseAction(@Nonnull ItemStack stack) {
    return EnumAction.BOW;
  }

  @Override
  public int getMaxItemUseDuration(@Nonnull ItemStack stack) {
    return USE_TICKS;
  }

  @Override
  @Nonnull
  public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (player.getHealth() >= player.getMaxHealth()) {
      return new ActionResult<>(EnumActionResult.FAIL, stack);
    }
    player.setActiveHand(hand);
    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
  }

  @Override
  @Nonnull
  public ItemStack onItemUseFinish(@Nonnull ItemStack stack, @Nonnull World world,
      @Nonnull EntityLivingBase user) {
    if (!world.isRemote) {
      user.heal(HEAL);
      world.playSound(null, user.posX, user.posY, user.posZ, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
          SoundCategory.PLAYERS, 1F, 1.2F);
    }
    if (user instanceof EntityPlayer) {
      EntityPlayer player = (EntityPlayer) user;
      player.getCooldownTracker().setCooldown(this, COOLDOWN_TICKS);
      if (!player.capabilities.isCreativeMode) {
        stack.shrink(1);
      }
    }
    return stack;
  }

  @Override
  @SideOnly(Side.CLIENT)
  @SuppressWarnings("deprecation")
  public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
      @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
    tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("csm.lifesafety.first_aid.hint"));
  }
}
