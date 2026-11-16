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
 * {@link BlockCanopyDownlight} (or the {@link BlockCanopyLightStrip}) in the hotbar; click the
 * ground at one corner, then at the other. The tool lays the deck over the rectangle at the
 * clearance it is set to (a sneaking click in the air steps it through {@link #MIN_CLEARANCE} to
 * {@link #MAX_CLEARANCE}), stands columns from the ground up to it and hangs the lights under it.
 * A sneaking click looking up (past {@link #LOOK_UP_PITCH}) steps the lights mode instead:
 * downlights, one strip down the middle, or a strip along each long edge ({@link #stripRows}).
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

  /** Lights mode: downlights every third block (the default). */
  public static final int LIGHTS_DOWNLIGHTS = 0;
  /** Lights mode: one light strip down the middle, along the canopy's length. */
  public static final int LIGHTS_STRIP_CENTRE = 1;
  /** Lights mode: two light strips along the canopy's long edges, a block in. */
  public static final int LIGHTS_STRIP_EDGES = 2;
  private static final int LIGHTS_MODES = 3;
  /** Looking further up than this (pitch, degrees; -90 is straight up) a sneaking use steps the
   * lights mode, not the clearance. */
  static final float LOOK_UP_PITCH = -60;

  private static final String TAG_CLEARANCE = "clearance";
  private static final String TAG_CORNER = "corner";
  private static final String TAG_LIGHTS = "lights";

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

  /** The lights mode: {@link #LIGHTS_DOWNLIGHTS}, {@link #LIGHTS_STRIP_CENTRE} or
   * {@link #LIGHTS_STRIP_EDGES}. */
  static int lights(ItemStack stack) {
    NBTTagCompound t = stack.getTagCompound();
    int mode = t != null ? t.getInteger(TAG_LIGHTS) : LIGHTS_DOWNLIGHTS;
    return mode >= 0 && mode < LIGHTS_MODES ? mode : LIGHTS_DOWNLIGHTS;
  }

  /** Whether a sneaking use is the lights-mode gesture: looking up, at the sky or the roof. */
  private static boolean looksUp(EntityPlayer player) {
    return player.isSneaking() && player.rotationPitch < LOOK_UP_PITCH;
  }

  /** Steps the lights mode on and tells the player. */
  private static void stepLights(ItemStack stack, EntityPlayer player) {
    int next = (lights(stack) + 1) % LIGHTS_MODES;
    tag(stack).setInteger(TAG_LIGHTS, next);
    player.sendStatusMessage(new TextComponentTranslation("csm.transit.canopy.lights",
        new TextComponentTranslation("csm.transit.canopy.lights_" + next)), true);
  }

  @Override
  @Nonnull
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (!world.isRemote) {
      if (looksUp(player)) {
        stepLights(stack, player);
      } else if (player.isSneaking()) {
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
    if (looksUp(player)) {
      // under a canopy the look up meets the roof or a light, not the air
      stepLights(stack, player);
      return EnumActionResult.SUCCESS;
    }
    NBTTagCompound t = tag(stack);
    if (!t.hasKey(TAG_CORNER)) {
      t.setLong(TAG_CORNER, pos.toLong());
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.canopy.corner",
          pos.getX(), pos.getY(), pos.getZ()), true);
      return EnumActionResult.SUCCESS;
    }
    BlockPos a = BlockPos.fromLong(t.getLong(TAG_CORNER));
    TextComponentTranslation result = build(world, player, a, pos, clearance(stack),
        lights(stack));
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

  /**
   * The rows across a canopy, from {@code lo} to {@code hi} across its short side, that a strip
   * lights mode hangs its strips in: the middle row ({@link #LIGHTS_STRIP_CENTRE}), or the rows a
   * block in from each long edge ({@link #LIGHTS_STRIP_EDGES}; the middle alone when the canopy
   * is under three deep). A shaped style hangs lights only where its soffit is low
   * ({@link CanopyStyle#lightAt}), so each row is moved to the nearest low row (the lower, on a
   * tie): under a vault the middle of one arch is its crown, so its strip goes to the springing.
   *
   * @param style the deck's style
   * @param lo    the first row across
   * @param hi    the last row across
   * @param mode  the lights mode
   *
   * @return the rows, ascending, no row twice
   */
  static List<Integer> stripRows(CanopyStyle style, int lo, int hi, int mode) {
    List<Integer> targets = new ArrayList<>();
    if (mode == LIGHTS_STRIP_EDGES && hi - lo >= 2) {
      targets.add(lo + 1);
      targets.add(hi - 1);
    } else {
      targets.add(lo + (hi - lo) / 2);
    }
    List<Integer> rows = new ArrayList<>();
    for (int target : targets) {
      int best = Integer.MIN_VALUE;
      for (int c = lo; c <= hi; c++) {
        boolean low = !style.isShaped() || style.lightAt((c - lo) % style.getModule());
        if (low && (best == Integer.MIN_VALUE || Math.abs(c - target) < Math.abs(best - target))) {
          best = c;
        }
      }
      if (best != Integer.MIN_VALUE && !rows.contains(best)) {
        rows.add(best);
      }
    }
    rows.sort(Integer::compare);
    return rows;
  }

  private TextComponentTranslation build(World world, EntityPlayer player, BlockPos a,
      BlockPos b, int clearance, int lightsMode) {
    ItemStack deckStack = find(player, BlockCanopyDeck.class);
    if (deckStack == null) {
      return new TextComponentTranslation("csm.transit.canopy.no_deck");
    }
    BlockCanopyDeck deck = (BlockCanopyDeck) ((ItemBlock) deckStack.getItem()).getBlock();
    ItemStack columnStack = find(player, BlockCanopyColumn.class);
    boolean strips = lightsMode != LIGHTS_DOWNLIGHTS;
    ItemStack lightStack = find(player,
        strips ? BlockCanopyLightStrip.class : BlockCanopyDownlight.class);
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
    // strips run along the canopy's length, across the profile (and so along a shaped tile's
    // level ridge and valley lines), a block in from each end
    EnumFacing.Axis run = axis == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
    if (lightStack != null && strips) {
      boolean acrossX = axis == EnumFacing.Axis.X;
      int r0 = acrossX ? z0 : x0;
      int r1 = acrossX ? z1 : x1;
      if (r1 - r0 >= 2) {
        r0++;
        r1--;
      }
      for (int c : stripRows(style, acrossX ? x0 : z0, acrossX ? x1 : z1, lightsMode)) {
        for (int r = r0; r <= r1; r++) {
          int x = acrossX ? c : r;
          int z = acrossX ? r : c;
          if (!columnFeet.contains(new BlockPos(x, ground, z))) {
            lights.add(new BlockPos(x, deckY - 1, z));
          }
        }
      }
    } else if (lightStack != null) {
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
      IBlockState lightState = light instanceof BlockCanopyLightStrip
          ? light.getDefaultState().withProperty(BlockCanopyLightStrip.AXIS, run)
          : light.getDefaultState();
      for (BlockPos p : lights) {
        world.setBlockState(p, lightState, 3);
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
    tooltip.add(I18n.format("csm.transit.canopy.lights",
        I18n.format("csm.transit.canopy.lights_" + lights(stack))));
    tooltip.add(I18n.format("csm.transit.canopy.controls"));
    NBTTagCompound t = stack.getTagCompound();
    if (t != null && t.hasKey(TAG_CORNER)) {
      BlockPos c = BlockPos.fromLong(t.getLong(TAG_CORNER));
      tooltip.add(I18n.format("csm.transit.canopy.corner", c.getX(), c.getY(), c.getZ()));
    }
  }
}
