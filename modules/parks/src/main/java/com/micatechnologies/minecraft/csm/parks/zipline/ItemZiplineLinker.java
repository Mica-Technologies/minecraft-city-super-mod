package com.micatechnologies.minecraft.csm.parks.zipline;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Strings a zipline: click a launch anchor, then a landing anchor. The line is checked first (its
 * length, a clear path for a rider, a platform to land on) and strung only if it passes.
 * Sneak-click either anchor to take its line down. The launch anchor picked is kept on the stack,
 * not the item, so two players linking at once do not cross their lines.
 *
 * @version 1.0
 * @since 2026.11
 */
public class ItemZiplineLinker extends AbstractItem {

  private static final String NBT_LAUNCH = "zipLaunch";

  public ItemZiplineLinker() {
    super(0, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "zipline_linker";
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockZiplineAnchor)) {
      return EnumActionResult.PASS;
    }
    if (world.isRemote) {
      return EnumActionResult.SUCCESS;
    }
    ItemStack stack = player.getHeldItem(hand);
    boolean launch = ((BlockZiplineAnchor) state.getBlock()).isLaunch();
    if (player.isSneaking()) {
      if (Ziplines.unlink(world, pos)) {
        player.sendStatusMessage(new TextComponentTranslation("csm.zipline.unlinked"), true);
      }
      clear(stack);
      return EnumActionResult.SUCCESS;
    }
    if (launch) {
      NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
      tag.setLong(NBT_LAUNCH, pos.toLong());
      stack.setTagCompound(tag);
      player.sendStatusMessage(new TextComponentTranslation("csm.zipline.first",
          Ziplines.fmt(pos)), true);
      return EnumActionResult.SUCCESS;
    }
    BlockPos from = selected(stack);
    if (from == null) {
      player.sendStatusMessage(new TextComponentTranslation("csm.zipline.pick_launch"), true);
      return EnumActionResult.SUCCESS;
    }
    TextComponentTranslation why = Ziplines.check(world, from, pos);
    if (why != null) {
      player.sendStatusMessage(why, false);
      return EnumActionResult.SUCCESS;
    }
    Ziplines.link(world, from, pos);
    clear(stack);
    ZiplineLine line = ZiplineLine.of(from, pos);
    player.sendStatusMessage(new TextComponentTranslation("csm.zipline.linked",
        String.format("%.1f", line.length()), String.format("%.1f", line.drop())), false);
    return EnumActionResult.SUCCESS;
  }

  @Nullable
  private static BlockPos selected(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag != null && tag.hasKey(NBT_LAUNCH) ? BlockPos.fromLong(tag.getLong(NBT_LAUNCH))
        : null;
  }

  private static void clear(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag != null) {
      tag.removeTag(NBT_LAUNCH);
      if (tag.isEmpty()) {
        stack.setTagCompound(null);
      }
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    tooltip.add(I18n.format("csm.zipline.linker.tooltip"));
    BlockPos from = selected(stack);
    if (from != null) {
      tooltip.add(I18n.format("csm.zipline.first", Ziplines.fmt(from)));
    }
  }
}
