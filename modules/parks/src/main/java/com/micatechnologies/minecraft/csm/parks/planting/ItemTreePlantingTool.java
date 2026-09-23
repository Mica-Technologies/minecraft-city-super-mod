package com.micatechnologies.minecraft.csm.parks.planting;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkProp;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Tree Planting Tool: plants a whole tree of the selected species where it is used, built from
 * ordinary log and leaves blocks, so it can be edited block by block afterwards.
 *
 * <p>Right-click a block to plant on it; the tree leans and reaches the way the player faces, so
 * standing on a sidewalk facing the road plants a tree that arches over the road. Sneak and
 * right-click to change species. Every block of the tree is checked before any is placed: if
 * anything but air, plants, snow or a ground cover is in the way, or the player may not build
 * there, nothing is planted. Clicking a ground cover plants through it, on the ground below.</p>
 *
 * @since 2026.9
 */
public class ItemTreePlantingTool extends AbstractItem {

  private static final String NBT_PRESET = "csm_tree_preset";

  public ItemTreePlantingTool() {
    super(0, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "tree_planting_tool";
  }

  public static TreePreset getPreset(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    int i = tag != null ? tag.getInteger(NBT_PRESET) : 0;
    TreePreset[] values = TreePreset.values();
    return values[i >= 0 && i < values.length ? i : 0];
  }

  private static void cycle(ItemStack stack, EntityPlayer player) {
    TreePreset next = TreePreset.values()[(getPreset(stack).ordinal() + 1)
        % TreePreset.values().length];
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null) {
      tag = new NBTTagCompound();
      stack.setTagCompound(tag);
    }
    tag.setInteger(NBT_PRESET, next.ordinal());
    player.sendStatusMessage(new TextComponentTranslation("csm.parks.planting.mode",
        new TextComponentTranslation(next.getTranslationKey())), true);
  }

  @Override
  @Nonnull
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (player.isSneaking()) {
      if (!world.isRemote) {
        cycle(stack, player);
      }
      return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    return new ActionResult<>(EnumActionResult.PASS, stack);
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      return EnumActionResult.SUCCESS;
    }
    ItemStack stack = player.getHeldItem(hand);
    if (player.isSneaking()) {
      cycle(stack, player);
      return EnumActionResult.SUCCESS;
    }
    BlockPos base = replaceable(world, pos) ? pos : pos.offset(facing);
    TreePreset preset = getPreset(stack);
    TreePlan plan = TreeGenerators.grow(preset, player.getHorizontalFacing(),
        new Random(world.rand.nextLong()));

    int blocked = 0;
    for (BlockPos rel : plan.parts().keySet()) {
      BlockPos at = base.add(rel);
      if (at.getY() < 0 || at.getY() > 255 || !world.isBlockModifiable(player, at)
          || !player.canPlayerEdit(at, EnumFacing.UP, stack) || !replaceable(world, at)) {
        blocked++;
      }
    }
    if (blocked > 0) {
      player.sendStatusMessage(new TextComponentTranslation("csm.parks.planting.blocked",
          new TextComponentTranslation(preset.getTranslationKey()), blocked), true);
      return EnumActionResult.FAIL;
    }

    // Logs first, then leaves, then what hangs from them, so each has what it needs in place.
    for (TreePlan.Kind kind : TreePlan.Kind.values()) {
      for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
        if (e.getValue().kind == kind) {
          IBlockState state = resolve(e.getValue());
          if (state != null) {
            world.setBlockState(base.add(e.getKey()), state, 3);
          }
        }
      }
    }
    return EnumActionResult.SUCCESS;
  }

  /**
   * Air, plants, snow, and a thin covering on the ground (a mulch or gravel cover, a carpet): a
   * tree is planted through a ground cover, so its trunk stands on the ground beneath rather than
   * a block up on top of the cover.
   */
  private static boolean replaceable(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    Material m = state.getMaterial();
    Block block = state.getBlock();
    boolean cover = block instanceof BlockParkProp
        && ((BlockParkProp) block).getKind() == BlockParkProp.Kind.COVER;
    return block.isReplaceable(world, pos) || m == Material.PLANTS || m == Material.VINE
        || m == Material.CARPET || cover;
  }

  @Nullable
  private static IBlockState resolve(TreePlan.Part part) {
    Block block = CsmRegistry.getBlock(part.block);
    if (block == null) {
      return null;
    }
    IBlockState state = block.getDefaultState();
    if (block instanceof BlockTreeLog && part.axis != null) {
      state = state.withProperty(BlockTreeLog.AXIS, part.axis);
    }
    return state;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    List<String> lines = new ArrayList<>();
    lines.add(I18n.format("csm.parks.planting.tooltip.use"));
    lines.add(I18n.format("csm.parks.planting.tooltip.cycle"));
    lines.add(I18n.format("csm.parks.planting.tooltip.current",
        I18n.format(getPreset(stack).getTranslationKey())));
    tooltip.addAll(lines);
  }
}
