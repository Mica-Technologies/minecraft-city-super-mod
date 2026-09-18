package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A door closer: right-click a door to fit one. The door then shuts itself three seconds after it
 * is opened, and shows a surface-mounted closer on its inside face with its arm up to the header.
 * Sneak-clicking the door with an empty hand takes it off and gives the item back.
 *
 * <p>An add-on rather than a property of the door, as a real closer is: any door can have one,
 * and none does to begin with.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class ItemDoorCloser extends AbstractItem {

  private static ItemDoorCloser instance;

  /**
   * Constructs the {@link ItemDoorCloser}.
   *
   * @since 1.0
   */
  public ItemDoorCloser() {
    instance = this;
  }

  /**
   * The registered item, for handing one back when a closer is taken off.
   *
   * @return the item
   *
   * @since 1.0
   */
  static ItemDoorCloser instance() {
    return instance;
  }

  @Override
  public String getItemRegistryName() {
    return "door_closer";
  }

  /**
   * Fits the closer. {@code onItemUseFirst}, not {@code onItemUse}: a right-click is offered to the
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
    if (!((BlockBuildingDoor) state.getBlock()).fitCloser(worldIn, pos)) {
      return EnumActionResult.FAIL;
    }
    if (!player.capabilities.isCreativeMode) {
      player.getHeldItem(hand).shrink(1);
    }
    worldIn.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 0.8F, 1.0F);
    return EnumActionResult.SUCCESS;
  }
}
