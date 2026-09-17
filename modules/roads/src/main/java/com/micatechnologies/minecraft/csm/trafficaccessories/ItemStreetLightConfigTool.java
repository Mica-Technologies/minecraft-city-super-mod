package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * The Street Light Configuration Tool: customises street light and pedestrian pole hardware in
 * place, the way the signal and fire alarm configuration tools customise their devices.
 *
 * <ul>
 *   <li><b>Right-click</b> a block applies the current mode to it.</li>
 *   <li><b>Sneak + right-click</b>, on a block or in the air, steps to the next mode.</li>
 * </ul>
 *
 * <p>Its first mode fits or removes the ball finial on a {@link BlockTrafficPolePedestal}. The
 * modes live in {@link ItemStreetLightConfigToolMode} so the decorative light poles can join it
 * later without a second tool.
 *
 * <p>Everything that changes the world or talks to the player happens on the server. The client
 * answers {@link EnumActionResult#SUCCESS} to every block click so the arm swings and, more
 * importantly, so the click does not fall through to {@link #onItemRightClick} as well: a sneak
 * click on a block would otherwise step the mode twice.
 *
 * @author Mica Technologies
 * @since 2026.9.17
 */
public class ItemStreetLightConfigTool extends AbstractItem {

  /** NBT key of the mode ordinal; the same key the other configuration tools use. */
  private static final String NBT_MODE_KEY = "csm_tool_mode";

  @Override
  public String getItemRegistryName() {
    return "street_light_config_tool";
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (worldIn.isRemote) {
      return EnumActionResult.SUCCESS;
    }
    ItemStack heldStack = player.getHeldItem(hand);
    if (player.isSneaking()) {
      switchToNextMode(heldStack);
      sayMode(player, heldStack);
      return EnumActionResult.SUCCESS;
    }

    switch (getMode(heldStack)) {
      case TOGGLE_BALL_FINIAL:
        return toggleBallFinial(player, worldIn, pos, facing, heldStack);
      default:
        return EnumActionResult.PASS;
    }
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn,
      EnumHand handIn) {
    ItemStack heldStack = playerIn.getHeldItem(handIn);
    if (!playerIn.isSneaking()) {
      return new ActionResult<>(EnumActionResult.PASS, heldStack);
    }
    if (!worldIn.isRemote) {
      switchToNextMode(heldStack);
      sayMode(playerIn, heldStack);
    }
    return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
  }

  /**
   * Fits the ball finial to the pole that was clicked, or removes it.
   *
   * <p>On an upright pole the click is carried to the top block of the stack first. The finial
   * only ever shows on the top block, a tall pole's top is usually out of reach, and setting the
   * bit on a block in the middle of a stack would do nothing visible, which would read as the
   * tool not working.
   */
  private static EnumActionResult toggleBallFinial(EntityPlayer player, World world,
      BlockPos pos, EnumFacing side, ItemStack stack) {
    IBlockState clicked = world.getBlockState(pos);
    if (!(clicked.getBlock() instanceof BlockTrafficPolePedestal)) {
      say(player, "Not a pedestal traffic pole. "
          + ItemStreetLightConfigToolMode.TOGGLE_BALL_FINIAL.getFriendlyName()
          + " works on pedestal traffic poles.");
      return EnumActionResult.FAIL;
    }

    BlockPos top = topOfStack(world, pos);
    IBlockState state = world.getBlockState(top);
    if (!player.canPlayerEdit(top, side, stack) || !world.isBlockModifiable(player, top)) {
      say(player, "You cannot change that pole here.");
      return EnumActionResult.FAIL;
    }

    boolean fitted = !BlockTrafficPolePedestal.hasFinial(state);
    // Flag 3: notify neighbours and send the change to clients, which re-render the chunk, so
    // the ball appears at once. The choice is metadata, so it saves with the chunk.
    world.setBlockState(top, BlockTrafficPolePedestal.withFinial(state, fitted), 3);

    if (!fitted) {
      say(player, "Ball finial removed.");
    } else if (BlockTrafficPolePedestal.isFinialShown(world, top)) {
      say(player, "Ball finial fitted.");
    } else {
      say(player, "Ball finial fitted. It will show once the top of the pole is clear.");
    }
    return EnumActionResult.SUCCESS;
  }

  /**
   * The top block of an upright pedestal pole stack, or {@code pos} itself for a pole lying on
   * its side (which has no stack to climb).
   */
  private static BlockPos topOfStack(World world, BlockPos pos) {
    if (!isUprightPedestal(world.getBlockState(pos))) {
      return pos;
    }
    BlockPos top = pos;
    while (top.getY() < world.getHeight() - 1 && isUprightPedestal(world.getBlockState(top.up()))) {
      top = top.up();
    }
    return top;
  }

  private static boolean isUprightPedestal(IBlockState state) {
    return state.getBlock() instanceof BlockTrafficPolePedestal
        && state.getValue(BlockTrafficPolePedestal.FACING).getAxis() == EnumFacing.Axis.Y;
  }

  /**
   * The tool's current mode, read from the stack.
   *
   * @param stack the tool stack
   *
   * @return the mode, or the first mode if none is stored or the stored one is out of range
   */
  public static ItemStreetLightConfigToolMode getMode(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    ItemStreetLightConfigToolMode[] values = ItemStreetLightConfigToolMode.values();
    if (tag != null && tag.hasKey(NBT_MODE_KEY)) {
      int ordinal = tag.getInteger(NBT_MODE_KEY);
      if (ordinal >= 0 && ordinal < values.length) {
        return values[ordinal];
      }
    }
    return values[0];
  }

  /**
   * Steps the stack to the next mode, wrapping round to the first.
   *
   * @param stack the tool stack
   */
  public static void switchToNextMode(ItemStack stack) {
    int next = (getMode(stack).ordinal() + 1) % ItemStreetLightConfigToolMode.values().length;
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null) {
      tag = new NBTTagCompound();
      stack.setTagCompound(tag);
    }
    tag.setInteger(NBT_MODE_KEY, next);
  }

  private static void sayMode(EntityPlayer player, ItemStack stack) {
    say(player, "Street Light Config Mode: " + getMode(stack).getFriendlyName());
  }

  private static void say(EntityPlayer player, String message) {
    player.sendMessage(new TextComponentString(message));
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Customize street light and pedestrian pole hardware:");
    list.add("fit or remove the ball finial on a pedestal traffic pole.");
    list.add("Right-click to apply. Sneak + right-click to switch modes.");
    list.add("Current mode: " + getMode(itemstack).getFriendlyName());
  }
}
