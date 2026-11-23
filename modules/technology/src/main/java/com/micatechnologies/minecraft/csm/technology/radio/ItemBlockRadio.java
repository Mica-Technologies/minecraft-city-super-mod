package com.micatechnologies.minecraft.csm.technology.radio;

import com.micatechnologies.minecraft.csm.Csm;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A handheld radio in the hand. Using it opens the radio's screen; it is only set down as a block
 * when its holder sneaks and clicks a surface, since a radio that planted itself on every
 * right-click would be no use to carry.
 *
 * <p>Its settings are in the stack's {@code BlockEntityTag}, under the tile entity's own keys
 * ({@link RadioSettings}). One radio to a slot: two radios are two sets of settings.</p>
 *
 * @since 2026.11
 */
public class ItemBlockRadio extends ItemBlock {

  public ItemBlockRadio(BlockRadio block) {
    super(block);
    setMaxStackSize(1);
  }

  /** The make of radio this item is. */
  public RadioModel getRadioModel() {
    return ((BlockRadio) getBlock()).getRadioModel();
  }

  /**
   * A held radio's settings, narrowed to its make.
   *
   * @param stack a stack of a radio
   *
   * @return its settings; the defaults for a stack that is not a radio or has never been set
   */
  public static RadioSettings getSettings(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlockRadio)) {
      return RadioSettings.DEFAULT;
    }
    RadioModel model = ((ItemBlockRadio) stack.getItem()).getRadioModel();
    return RadioSettings.read(stack.getSubCompound(RadioSettings.STACK_TAG)).forModel(model);
  }

  /**
   * Stores settings on a held radio.
   *
   * @param stack    a stack of a radio
   * @param settings the settings to store
   */
  public static void setSettings(ItemStack stack, RadioSettings settings) {
    NBTTagCompound tag = stack.getOrCreateSubCompound(RadioSettings.STACK_TAG);
    settings.write(tag);
  }

  /**
   * Places the radio only for a sneaking holder. Passing otherwise lets the click fall through to
   * {@link #onItemRightClick}, so the screen opens whether or not a block is in reach.
   */
  @Nonnull
  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking()) {
      return EnumActionResult.PASS;
    }
    return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
  }

  @Nonnull
  @Override
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (world.isRemote) {
      // The screen is the client's alone; what it changes comes back in RadioSettingsPacket.
      player.openGui(Csm.instance, BlockRadio.GUI_ID_HELD, world, hand.ordinal(), 0, 0);
    }
    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
      @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
    RadioModel model = getRadioModel();
    RadioSettings settings = getSettings(stack);
    RadioChannel channel = model.getChannels().get(settings.getChannel());
    tooltip.add(I18n.format("csm.radio.tooltip.channel", channel.getName(),
        channel.getFrequencyText()));
    tooltip.add(I18n.format(settings.isOn() ? "csm.radio.tooltip.on" : "csm.radio.tooltip.off"));
    if (!settings.getUnitId().isEmpty()) {
      tooltip.add(I18n.format("csm.radio.tooltip.unit", settings.getUnitId()));
    }
    tooltip.add(I18n.format("csm.radio.tooltip.use"));
    tooltip.add(I18n.format("csm.radio.tooltip.place"));
  }
}
