package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The door swing tool: right-click a door, either half, to make it swing the other way -- in if
 * it swung out, out if it swung in -- and its pair with it, so a pair always swings the same way.
 * Only a shut door that is not moving; a custom door's movement is set in the Door Workshop.
 *
 * <p>Sneaking as a door is placed does the same for a new door; this is for one already hung.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class ItemDoorSwingTool extends AbstractItem {

  /**
   * Constructs the {@link ItemDoorSwingTool}.
   *
   * @since 1.0
   */
  public ItemDoorSwingTool() {
    setMaxStackSize(1);
  }

  @Override
  public String getItemRegistryName() {
    return "door_swing_tool";
  }

  /**
   * Flips the door. {@code onItemUseFirst}, not {@code onItemUse}: a right-click is offered to the
   * block before the item, and the door would take it and open.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public EnumActionResult onItemUseFirst(EntityPlayer player, World worldIn, BlockPos pos,
      EnumFacing facing, float hitX, float hitY, float hitZ, EnumHand hand) {
    IBlockState state = worldIn.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockBuildingDoor)) {
      return EnumActionResult.PASS;
    }
    if (worldIn.isRemote) {
      return EnumActionResult.SUCCESS;
    }
    BlockBuildingDoor.FlipResult result =
        ((BlockBuildingDoor) state.getBlock()).flipSwing(worldIn, pos);
    switch (result) {
      case OUT:
      case IN:
        player.sendStatusMessage(new TextComponentTranslation(
            result == BlockBuildingDoor.FlipResult.OUT ? "gui.csm.door.swing_out"
                : "gui.csm.door.swing_in"), true);
        worldIn.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5F,
            1.3F);
        return EnumActionResult.SUCCESS;
      case BUSY:
        player.sendStatusMessage(new TextComponentTranslation("gui.csm.door.swing_busy"), true);
        return EnumActionResult.FAIL;
      default:
        player.sendStatusMessage(new TextComponentTranslation("gui.csm.door.swing_fixed"), true);
        return EnumActionResult.FAIL;
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn,
      @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
    tooltip.add(I18n.format("gui.csm.door.swing_tool.tip"));
  }
}
