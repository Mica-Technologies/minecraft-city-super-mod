package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A custom door as an item: its settings ride in the stack's NBT, from the Door Workshop to the
 * placed door's tile entity and back when it is broken, so a door keeps what it was made of.
 *
 * @version 1.0
 * @since 2026.9
 */
public class ItemCustomDoor extends ItemBlock {

  /**
   * Constructs the {@link ItemCustomDoor}.
   *
   * @param block the custom door
   *
   * @since 1.0
   */
  public ItemCustomDoor(Block block) {
    super(block);
  }

  /**
   * The settings a door item carries.
   *
   * @param stack the stack
   *
   * @return its settings, or the default door's
   *
   * @since 1.0
   */
  public static CustomDoorSettings getSettings(ItemStack stack) {
    return CustomDoorSettings.read(stack.getTagCompound());
  }

  /**
   * Gives a door item settings.
   *
   * @param stack    the stack
   * @param settings the settings
   *
   * @since 1.0
   */
  public static void setSettings(ItemStack stack, CustomDoorSettings settings) {
    NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
    settings.write(tag);
    stack.setTagCompound(tag);
  }

  /**
   * Places the door as any door is placed, then hands its settings to the tile entity.
   *
   * @since 1.0
   */
  @Override
  public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
      @Nonnull World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY,
      float hitZ, @Nonnull IBlockState newState) {
    if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
      return false;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityCustomDoor) {
      ((TileEntityCustomDoor) te).setSettings(getSettings(stack));
    }
    return true;
  }

  private static String materialName(IBlockState state) {
    ItemStack s = new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state));
    return s.isEmpty() || state.getBlock() == Blocks.AIR ? state.getBlock().getLocalizedName()
        : s.getDisplayName();
  }

  /**
   * Lists what the door is made of and how it behaves.
   *
   * @since 1.0
   */
  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn,
      @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
    CustomDoorSettings s = getSettings(stack);
    tooltip.add(I18n.format("gui.csm.door.tip.frame", materialName(s.frame())));
    tooltip.add(I18n.format("gui.csm.door.tip.upper", materialName(s.upper())));
    tooltip.add(I18n.format("gui.csm.door.tip.lower", materialName(s.lower())));
    tooltip.add(I18n.format("gui.csm.door.tip.movement",
        I18n.format("gui.csm.door.movement." + s.movement().key())));
    tooltip.add(I18n.format("gui.csm.door.tip.sound",
        I18n.format("gui.csm.door.sound." + s.sound().key())));
    tooltip.add(I18n.format("gui.csm.door.tip.speed", s.openTicks() / 20.0));
    if (s.autoCloseTicks() > 0) {
      tooltip.add(I18n.format("gui.csm.door.tip.autoclose", s.autoCloseTicks() / 20.0));
    }
    tooltip.add(I18n.format("gui.csm.door.tip.redstone",
        I18n.format("gui.csm.door.redstone." + s.redstone().key())));
    if (s.proximity()) {
      tooltip.add(I18n.format("gui.csm.door.tip.proximity"));
    }
  }
}
