package com.micatechnologies.minecraft.csm.transit.canopy;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Builds a bus station canopy in two clicks. Hold a {@link BlockCanopyDeck} in the off hand (or
 * anywhere in the hotbar) and, optionally, a {@link BlockCanopyColumn} and the
 * {@link BlockCanopyDownlight} in the hotbar; click the ground at one corner, then at the other.
 * The tool lays the deck over the rectangle at the clearance it is set to (a sneaking click in the
 * air steps it through {@link #MIN_CLEARANCE} to {@link #MAX_CLEARANCE}), stands columns from the
 * ground up to it and hangs the lights under it.
 *
 * <ul>
 *   <li>A flat style's columns stand a block in from the edges (two, on a side of
 *   {@link #DEEP_INSET_FROM} or more), at most {@link #SPACING} apart both ways; lights hang every
 *   third block.</li>
 *   <li>A shaped style's profile runs across the rectangle's shorter side, which is cut down to
 *   whole modules; columns stand under the phases {@link CanopyStyle#columnAt} names, at most
 *   {@link #SPACING} apart along the run, and lights under the low phases.</li>
 * </ul>
 *
 * <p>Nothing is placed unless every cell is free. In survival the decks, columns and lights come
 * out of the inventory, and nothing is placed unless all of them are there. A click in the air
 * (not sneaking) forgets the first corner.</p>
 *
 * @since 2026.11
 */
public class ItemCanopyTool extends AbstractItem {

  public static final int MIN_CLEARANCE = 4;
  public static final int MAX_CLEARANCE = 12;
  public static final int DEFAULT_CLEARANCE = 6;
  /** The longest side of one canopy. */
  public static final int MAX_SIDE = 64;
  /** The most blocks between columns: a bus station's bays run 8 to 12 m. */
  public static final int SPACING = 9;
  /** From this length up, the outer columns stand two blocks in from the edge, not one. */
  public static final int DEEP_INSET_FROM = 10;
  /** Blocks between lights. */
  public static final int LIGHT_SPACING = 3;

  private static final String TAG_CLEARANCE = "clearance";
  private static final String TAG_CORNER = "corner";

  public ItemCanopyTool() {
    super(0, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "canopy_tool";
  }

  private static NBTTagCompound tag(ItemStack stack) {
    if (!stack.hasTagCompound()) {
      stack.setTagCompound(new NBTTagCompound());
    }
    return stack.getTagCompound();
  }

  private static int clearance(ItemStack stack) {
    NBTTagCompound t = stack.getTagCompound();
    return t != null && t.hasKey(TAG_CLEARANCE) ? t.getInteger(TAG_CLEARANCE) : DEFAULT_CLEARANCE;
  }

  @Override
  @Nonnull
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (!world.isRemote) {
      if (player.isSneaking()) {
        int next = clearance(stack) + 1;
        if (next > MAX_CLEARANCE) {
          next = MIN_CLEARANCE;
        }
        tag(stack).setInteger(TAG_CLEARANCE, next);
        player.sendStatusMessage(new TextComponentTranslation("csm.transit.canopy.clearance",
            next), true);
      } else if (tag(stack).hasKey(TAG_CORNER)) {
        tag(stack).removeTag(TAG_CORNER);
        player.sendStatusMessage(new TextComponentTranslation("csm.transit.canopy.cleared"), true);
      }
    }
    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      return EnumActionResult.SUCCESS;
    }
    ItemStack stack = player.getHeldItem(hand);
    NBTTagCompound t = tag(stack);
    if (!t.hasKey(TAG_CORNER)) {
      t.setLong(TAG_CORNER, pos.toLong());
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.canopy.corner",
          pos.getX(), pos.getY(), pos.getZ()), true);
      return EnumActionResult.SUCCESS;
    }
    BlockPos a = BlockPos.fromLong(t.getLong(TAG_CORNER));
    TextComponentTranslation result = build(world, player, a, pos, clearance(stack));
    if (result.getKey().equals("csm.transit.canopy.built")) {
      t.removeTag(TAG_CORNER);
    }
    player.sendStatusMessage(result, true);
    return EnumActionResult.SUCCESS;
  }

  /** The first stack of an item block whose block is a {@code type}: off hand, then hotbar. */
  @Nullable
  private static ItemStack find(EntityPlayer player, Class<? extends Block> type) {
    ItemStack off = player.getHeldItemOffhand();
    if (isOf(off, type)) {
      return off;
    }
    for (int i = 0; i < 9; i++) {
      ItemStack s = player.inventory.getStackInSlot(i);
      if (isOf(s, type)) {
        return s;
      }
    }
    return null;
  }

  private static boolean isOf(ItemStack s, Class<? extends Block> type) {
    return !s.isEmpty() && s.getItem() instanceof ItemBlock
        && type.isInstance(((ItemBlock) s.getItem()).getBlock());
  }

  /**
   * Positions from lo to hi, a block in from each end (two on a long side, so the deck
   * overhangs as a real canopy's does), at most SPACING apart.
   */
  static List<Integer> along(int lo, int hi) {
    List<Integer> out = new ArrayList<>();
    int len = hi - lo + 1;
    if (len <= 3) {
      out.add(lo + len / 2);
      return out;
    }
    int inset = len >= DEEP_INSET_FROM ? 2 : 1;
    int a = lo + inset;
    int b = hi - inset;
    int gaps = Math.max(1, (b - a + SPACING - 1) / SPACING);
    for (int i = 0; i <= gaps; i++) {
      out.add(a + Math.round((b - a) * i / (float) gaps));
    }
    return out;
  }

  private TextComponentTranslation build(World world, EntityPlayer player, BlockPos a,
      BlockPos b, int clearance) {
    ItemStack deckStack = find(player, BlockCanopyDeck.class);
    if (deckStack == null) {
      return new TextComponentTranslation("csm.transit.canopy.no_deck");
    }
    BlockCanopyDeck deck = (BlockCanopyDeck) ((ItemBlock) deckStack.getItem()).getBlock();
    ItemStack columnStack = find(player, BlockCanopyColumn.class);
    ItemStack lightStack = find(player, BlockCanopyDownlight.class);
    CanopyStyle style = deck.getStyle();

    int x0 = Math.min(a.getX(), b.getX());
    int x1 = Math.max(a.getX(), b.getX());
    int z0 = Math.min(a.getZ(), b.getZ());
    int z1 = Math.max(a.getZ(), b.getZ());
    if (x1 - x0 + 1 > MAX_SIDE || z1 - z0 + 1 > MAX_SIDE) {
      return new TextComponentTranslation("csm.transit.canopy.too_big", MAX_SIDE);
    }
    int ground = Math.min(a.getY(), b.getY()) + 1;
    int deckY = ground + clearance;
    if (deckY >= world.getHeight()) {
      return new TextComponentTranslation("csm.transit.canopy.blocked", x0, deckY, z0);
    }

    // a shaped profile runs across the shorter side, cut to whole modules
    EnumFacing.Axis axis = (x1 - x0) <= (z1 - z0) ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
    int module = style.getModule();
    TextComponentTranslation trimmed = null;
    if (module > 1) {
      int depth = axis == EnumFacing.Axis.X ? x1 - x0 + 1 : z1 - z0 + 1;
      int whole = depth / module * module;
      if (whole == 0) {
        return new TextComponentTranslation("csm.transit.canopy.too_small", module);
      }
      if (whole != depth) {
        if (axis == EnumFacing.Axis.X) {
          x1 = x0 + whole - 1;
        } else {
          z1 = z0 + whole - 1;
        }
        trimmed = new TextComponentTranslation("csm.transit.canopy.trimmed", whole, module);
      }
    }

    List<BlockPos> decks = new ArrayList<>();
    List<IBlockState> deckStates = new ArrayList<>();
    for (int x = x0; x <= x1; x++) {
      for (int z = z0; z <= z1; z++) {
        int phase = axis == EnumFacing.Axis.X ? x - x0 : z - z0;
        decks.add(new BlockPos(x, deckY, z));
        deckStates.add(deck.stateFor(axis, phase));
      }
    }

    // columns and lights
    List<BlockPos> columnFeet = new ArrayList<>();
    List<BlockPos> lights = new ArrayList<>();
    if (module == 1) {
      if (columnStack != null) {
        for (int x : along(x0, x1)) {
          for (int z : along(z0, z1)) {
            columnFeet.add(new BlockPos(x, ground, z));
          }
        }
      }
    } else if (columnStack != null) {
      boolean acrossX = axis == EnumFacing.Axis.X;
      List<Integer> run = acrossX ? along(z0, z1) : along(x0, x1);
      int lo = acrossX ? x0 : z0;
      int hi = acrossX ? x1 : z1;
      for (int c = lo; c <= hi; c++) {
        boolean closing = c == hi && style.hasClosingColumn();
        if (!closing && !style.columnAt((c - lo) % module)) {
          continue;
        }
        for (int r : run) {
          columnFeet.add(acrossX ? new BlockPos(c, ground, r) : new BlockPos(r, ground, c));
        }
      }
    }
    if (lightStack != null) {
      for (int x = x0 + 1; x <= x1; x += module == 1 || axis != EnumFacing.Axis.X
          ? LIGHT_SPACING : 1) {
        for (int z = z0 + 1; z <= z1; z += module == 1 || axis != EnumFacing.Axis.Z
            ? LIGHT_SPACING : 1) {
          int phase = axis == EnumFacing.Axis.X ? x - x0 : z - z0;
          if (module > 1 && !style.lightAt(phase % module)) {
            continue;
          }
          BlockPos at = new BlockPos(x, deckY - 1, z);
          if (!columnFeet.contains(new BlockPos(x, ground, z))) {
            lights.add(at);
          }
        }
      }
    }

    // every cell must be free
    for (BlockPos p : decks) {
      if (!free(world, p, player)) {
        return new TextComponentTranslation("csm.transit.canopy.blocked", p.getX(), p.getY(),
            p.getZ());
      }
    }
    int columnBlocks = 0;
    for (BlockPos foot : columnFeet) {
      for (int y = ground; y < deckY; y++) {
        BlockPos p = new BlockPos(foot.getX(), y, foot.getZ());
        if (!free(world, p, player)) {
          return new TextComponentTranslation("csm.transit.canopy.blocked", p.getX(), p.getY(),
              p.getZ());
        }
        columnBlocks++;
      }
    }
    for (BlockPos p : lights) {
      if (!free(world, p, player)) {
        return new TextComponentTranslation("csm.transit.canopy.blocked", p.getX(), p.getY(),
            p.getZ());
      }
    }

    if (!player.capabilities.isCreativeMode) {
      boolean enough = count(player, deckStack) >= decks.size()
          && (columnStack == null || count(player, columnStack) >= columnBlocks)
          && (lightStack == null || count(player, lightStack) >= lights.size());
      if (!enough) {
        return new TextComponentTranslation("csm.transit.canopy.short", decks.size(),
            columnBlocks, lights.size());
      }
      take(player, deckStack.copy(), decks.size());
      if (columnStack != null) {
        take(player, columnStack.copy(), columnBlocks);
      }
      if (lightStack != null) {
        take(player, lightStack.copy(), lights.size());
      }
    }

    for (int i = 0; i < decks.size(); i++) {
      world.setBlockState(decks.get(i), deckStates.get(i), 3);
    }
    if (columnStack != null) {
      Block column = ((ItemBlock) columnStack.getItem()).getBlock();
      for (BlockPos foot : columnFeet) {
        for (int y = ground; y < deckY; y++) {
          world.setBlockState(new BlockPos(foot.getX(), y, foot.getZ()),
              column.getDefaultState(), 3);
        }
      }
    }
    if (lightStack != null) {
      Block light = ((ItemBlock) lightStack.getItem()).getBlock();
      for (BlockPos p : lights) {
        world.setBlockState(p, light.getDefaultState(), 3);
      }
    }
    if (trimmed != null) {
      player.sendMessage(trimmed);
    }
    return new TextComponentTranslation("csm.transit.canopy.built", x1 - x0 + 1, z1 - z0 + 1,
        clearance, decks.size(), columnFeet.size(), lights.size());
  }

  private static boolean free(World world, BlockPos p, EntityPlayer player) {
    return world.getBlockState(p).getBlock().isReplaceable(world, p)
        && player.canPlayerEdit(p, EnumFacing.UP, ItemStack.EMPTY);
  }

  private static int count(EntityPlayer player, ItemStack like) {
    int n = 0;
    for (ItemStack s : player.inventory.mainInventory) {
      if (ItemStack.areItemsEqual(s, like)) {
        n += s.getCount();
      }
    }
    for (ItemStack s : player.inventory.offHandInventory) {
      if (ItemStack.areItemsEqual(s, like)) {
        n += s.getCount();
      }
    }
    return n;
  }

  private static void take(EntityPlayer player, ItemStack like, int n) {
    List<ItemStack> stacks = new ArrayList<>(player.inventory.offHandInventory);
    stacks.addAll(player.inventory.mainInventory);
    for (ItemStack s : stacks) {
      if (n <= 0) {
        return;
      }
      if (ItemStack.areItemsEqual(s, like)) {
        int k = Math.min(n, s.getCount());
        s.shrink(k);
        n -= k;
      }
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
      @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
    tooltip.add(I18n.format("csm.transit.canopy.clearance", clearance(stack)));
    NBTTagCompound t = stack.getTagCompound();
    if (t != null && t.hasKey(TAG_CORNER)) {
      BlockPos c = BlockPos.fromLong(t.getLong(TAG_CORNER));
      tooltip.add(I18n.format("csm.transit.canopy.corner", c.getX(), c.getY(), c.getZ()));
    }
  }
}
