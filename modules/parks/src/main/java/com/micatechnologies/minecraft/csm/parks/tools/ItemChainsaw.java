package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.parks.ParksSounds;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import com.micatechnologies.minecraft.csm.parks.trees.TreeFelling;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The chainsaw: fells a whole tree from one cut, at the price of fuel and a little patience.
 *
 * <ul>
 *   <li><b>Fuel.</b> It burns whatever a furnace burns, held in the saw as burn ticks
 *   ({@link ChainsawFuel}). Sneak and right-click while it is off to pour in one item from the
 *   inventory, the best that still fits; a lava bucket gives its bucket back.</li>
 *   <li><b>Pull start.</b> It starts off. Each right-click pulls the cord; it catches on the
 *   third to fifth pull. Running, it idles, burning fuel while held. Sneak and right-click to stop
 *   it; it also stops when the tank runs dry or when it has not been held for a few seconds.</li>
 *   <li><b>Cutting.</b> Running, it mines wood very fast. Cutting a log fells the tree standing on
 *   it at once: this module's trees by {@link TreeFelling}, anyone else's by
 *   {@link AnyTreeFelling}; sneak to cut only that log. Fuel and wear are paid per log felled,
 *   and the felling leaves a few brush piles around the stump ({@link BrushPiles}).</li>
 * </ul>
 *
 * <p>Fuel and running state live on the stack, written at most once a second while idling so the
 * held item is not resynchronised every tick, and a change of fuel never replays the equip
 * animation. Not enchantable: a chainsaw is not an axe to be given Efficiency, Fortune or
 * Unbreaking.</p>
 *
 * @since 2026.10
 */
public class ItemChainsaw extends AbstractItem {

  private static final String NBT_FUEL = "csm_fuel";
  private static final String NBT_RUNNING = "csm_running";
  private static final String NBT_PULLS = "csm_pulls";
  private static final String NBT_PULLS_NEEDED = "csm_pulls_needed";
  private static final String NBT_HELD_AT = "csm_held_at";

  /** How often a running saw burns fuel and plays its idle sound, in ticks. */
  private static final int IDLE_PERIOD = 20;
  /** How long a running saw may go unheld before it stalls, in ticks. */
  private static final int STALL_AFTER = 60;
  /** Ticks between cord pulls. */
  private static final int PULL_COOLDOWN = 8;
  /** How fast it cuts wood while running. */
  private static final float WOOD_SPEED = 20.0F;
  /** How fast it cuts leaves while running. */
  private static final float LEAVES_SPEED = 8.0F;

  public ItemChainsaw() {
    super(1200, 1);
    setNoRepair();
    setFull3D();
  }

  @Override
  public String getItemRegistryName() {
    return "chainsaw";
  }

  // --- stack state ---

  private static NBTTagCompound tag(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null) {
      tag = new NBTTagCompound();
      stack.setTagCompound(tag);
    }
    return tag;
  }

  public static boolean isRunning(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag != null && tag.getBoolean(NBT_RUNNING);
  }

  public static int getFuel(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag != null ? Math.max(0, tag.getInteger(NBT_FUEL)) : 0;
  }

  private static void setFuel(ItemStack stack, int fuel) {
    tag(stack).setInteger(NBT_FUEL, Math.max(0, Math.min(ChainsawFuel.CAPACITY, fuel)));
  }

  private static void stop(ItemStack stack) {
    NBTTagCompound tag = tag(stack);
    tag.setBoolean(NBT_RUNNING, false);
    tag.setInteger(NBT_PULLS, 0);
    tag.setInteger(NBT_PULLS_NEEDED, 0);
  }

  private static void play(World world, EntityPlayer player, ParksSounds sound, float volume) {
    SoundEvent event = sound.getSoundEvent();
    if (event != null) {
      world.playSound(null, player.posX, player.posY, player.posZ, event, SoundCategory.PLAYERS,
          volume, 0.95F + world.rand.nextFloat() * 0.1F);
    }
  }

  // --- right-click: pull, stop, refuel ---

  @Override
  @Nonnull
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (hand != EnumHand.MAIN_HAND) {
      return new ActionResult<>(EnumActionResult.PASS, stack);
    }
    boolean running = isRunning(stack);
    if (running && !player.isSneaking()) {
      return new ActionResult<>(EnumActionResult.PASS, stack);
    }
    if (world.isRemote) {
      return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    if (player.isSneaking()) {
      if (running) {
        stop(stack);
        player.sendStatusMessage(new TextComponentTranslation("csm.parks.chainsaw.stopped"),
            true);
      } else {
        refuel(world, player, stack);
      }
    } else {
      pull(world, player, stack);
    }
    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
  }

  private void pull(World world, EntityPlayer player, ItemStack stack) {
    player.getCooldownTracker().setCooldown(this, PULL_COOLDOWN);
    play(world, player, ParksSounds.CHAINSAW_PULL, 0.8F);
    if (getFuel(stack) <= 0) {
      player.sendStatusMessage(new TextComponentTranslation("csm.parks.chainsaw.nofuel"), true);
      return;
    }
    NBTTagCompound tag = tag(stack);
    int needed = tag.getInteger(NBT_PULLS_NEEDED);
    if (needed <= 0) {
      needed = ChainsawFuel.pullsToStart(world.rand.nextInt(3));
      tag.setInteger(NBT_PULLS_NEEDED, needed);
    }
    int pulls = tag.getInteger(NBT_PULLS) + 1;
    if (pulls < needed) {
      tag.setInteger(NBT_PULLS, pulls);
      return;
    }
    tag.setInteger(NBT_PULLS, 0);
    tag.setInteger(NBT_PULLS_NEEDED, 0);
    tag.setBoolean(NBT_RUNNING, true);
    tag.setLong(NBT_HELD_AT, world.getTotalWorldTime());
    play(world, player, ParksSounds.CHAINSAW_START, 1.0F);
    player.sendStatusMessage(new TextComponentTranslation("csm.parks.chainsaw.started"), true);
  }

  /**
   * Pours one fuel item from the inventory into the tank: the one burning longest that still
   * fits. Tools and other damageable items are never burned. In creative, an empty inventory
   * fills the tank.
   */
  private void refuel(World world, EntityPlayer player, ItemStack saw) {
    int fuel = getFuel(saw);
    List<ItemStack> inventory = player.inventory.mainInventory;
    int best = -1;
    int bestBurn = 0;
    boolean anyFuel = false;
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack s = inventory.get(i);
      if (s.isEmpty() || s == saw || s.isItemStackDamageable()) {
        continue;
      }
      int burn = TileEntityFurnace.getItemBurnTime(s);
      if (burn <= 0) {
        continue;
      }
      anyFuel = true;
      if (ChainsawFuel.accepts(fuel, burn) && burn > bestBurn) {
        best = i;
        bestBurn = burn;
      }
    }
    if (best < 0) {
      if (player.capabilities.isCreativeMode && !anyFuel) {
        setFuel(saw, ChainsawFuel.CAPACITY);
        refuelled(world, player, saw);
        return;
      }
      player.sendStatusMessage(new TextComponentTranslation(anyFuel
          ? "csm.parks.chainsaw.full" : "csm.parks.chainsaw.nofuelitem"), true);
      return;
    }
    ItemStack source = inventory.get(best);
    ItemStack container = source.getItem().getContainerItem(source);
    setFuel(saw, fuel + bestBurn);
    if (!player.capabilities.isCreativeMode) {
      source.shrink(1);
      if (!container.isEmpty()) {
        if (source.isEmpty()) {
          inventory.set(best, container);
        } else if (!player.inventory.addItemStackToInventory(container)) {
          player.dropItem(container, false);
        }
      }
    }
    refuelled(world, player, saw);
  }

  private static void refuelled(World world, EntityPlayer player, ItemStack saw) {
    world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_EMPTY,
        SoundCategory.PLAYERS, 0.8F, 0.8F);
    player.sendStatusMessage(new TextComponentTranslation("csm.parks.chainsaw.refuelled",
        ChainsawFuel.percent(getFuel(saw)) + "%"), true);
  }

  // --- running ---

  @Override
  public void onUpdate(@Nonnull ItemStack stack, World world, @Nonnull Entity entity, int slot,
      boolean isSelected) {
    if (world.isRemote || !isRunning(stack)) {
      return;
    }
    if (!(entity instanceof EntityPlayer)) {
      stop(stack);
      return;
    }
    EntityPlayer player = (EntityPlayer) entity;
    long now = world.getTotalWorldTime();
    NBTTagCompound tag = tag(stack);
    if (isSelected && player.getHeldItemMainhand() == stack) {
      if (now % IDLE_PERIOD != 0) {
        return;
      }
      tag.setLong(NBT_HELD_AT, now);
      if (!player.capabilities.isCreativeMode) {
        setFuel(stack, ChainsawFuel.afterIdle(getFuel(stack), IDLE_PERIOD));
      }
      if (getFuel(stack) <= 0) {
        stop(stack);
        player.sendStatusMessage(new TextComponentTranslation("csm.parks.chainsaw.ranout"),
            true);
        return;
      }
      play(world, player, ParksSounds.CHAINSAW_IDLE, 0.6F);
    } else if (now - tag.getLong(NBT_HELD_AT) > STALL_AFTER) {
      stop(stack);
    }
  }

  @Override
  public boolean onEntityItemUpdate(EntityItem entityItem) {
    if (!entityItem.world.isRemote && isRunning(entityItem.getItem())) {
      stop(entityItem.getItem());
    }
    return false;
  }

  @Override
  public boolean shouldCauseReequipAnimation(ItemStack oldStack, @Nonnull ItemStack newStack,
      boolean slotChanged) {
    return slotChanged || oldStack.getItem() != newStack.getItem();
  }

  // --- cutting ---

  @Override
  public float getDestroySpeed(@Nonnull ItemStack stack, IBlockState state) {
    if (!isRunning(stack)) {
      return 1.0F;
    }
    Material m = state.getMaterial();
    if (m == Material.WOOD || state.getBlock() instanceof BlockLog) {
      return WOOD_SPEED;
    }
    if (m == Material.LEAVES || m == Material.PLANTS || m == Material.VINE) {
      return LEAVES_SPEED;
    }
    return 1.0F;
  }

  @Override
  public int getHarvestLevel(ItemStack stack, @Nonnull String toolClass,
      @Nullable EntityPlayer player, @Nullable IBlockState blockState) {
    return isRunning(stack) && "axe".equals(toolClass) ? 3 : -1;
  }

  @Override
  @Nonnull
  public Set<String> getToolClasses(ItemStack stack) {
    return isRunning(stack) ? Collections.singleton("axe") : Collections.emptySet();
  }

  /**
   * A running saw cutting a log (the break event has already passed): the log is cut here, the
   * tree on it felled, and fuel, wear and brush piles paid for every log. Returning {@code true}
   * keeps the game from breaking the log a second time. Sneaking, or anything that is not a log,
   * goes the ordinary way.
   */
  @Override
  public boolean onBlockStartBreak(@Nonnull ItemStack stack, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player) {
    World world = player.world;
    if (world.isRemote || !isRunning(stack) || player.isSneaking()) {
      return false;
    }
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    boolean ours = block instanceof BlockTreeLog;
    if (!ours && !AnyTrees.isLog(world, pos, state)) {
      return false;
    }
    boolean creative = player.capabilities.isCreativeMode;
    TileEntity te = world.getTileEntity(pos);
    world.playEvent(player, 2001, pos, Block.getStateId(state));
    block.onBlockHarvested(world, pos, state, player);
    if (!world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3)) {
      return true;
    }
    block.onPlayerDestroy(world, pos, state);
    if (!creative) {
      block.harvestBlock(world, player, pos, state, te, stack.copy());
    }
    int felled;
    if (ours) {
      felled = TreeFelling.fell(world, pos, player).size();
    } else {
      felled = AnyTrees.fell(world, pos, player);
    }
    int logs = felled + 1;
    play(world, player, ParksSounds.CHAINSAW_CUT, 1.0F);
    if (felled > 0) {
      BrushPiles.place(world, player, pos, logs);
    }
    if (!creative) {
      int fuel = ChainsawFuel.afterCut(getFuel(stack), logs);
      setFuel(stack, fuel);
      if (fuel <= 0) {
        stop(stack);
        player.sendStatusMessage(new TextComponentTranslation("csm.parks.chainsaw.ranout"),
            true);
      }
      stack.damageItem(logs, player);
    }
    return true;
  }

  @Override
  public boolean onBlockDestroyed(@Nonnull ItemStack stack, @Nonnull World world,
      @Nonnull IBlockState state, @Nonnull BlockPos pos, @Nonnull EntityLivingBase entity) {
    if (!world.isRemote && isRunning(stack) && state.getBlockHardness(world, pos) > 0.0F) {
      stack.damageItem(1, entity);
    }
    return true;
  }

  // --- no enchanting ---

  @Override
  public boolean isEnchantable(@Nonnull ItemStack stack) {
    return false;
  }

  @Override
  public int getItemEnchantability() {
    return 0;
  }

  @Override
  public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
    return false;
  }

  @Override
  public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
    return false;
  }

  // --- tooltip ---

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add(I18n.format(isRunning(stack) ? "csm.parks.chainsaw.tooltip.running"
        : "csm.parks.chainsaw.tooltip.off"));
    tooltip.add(I18n.format("csm.parks.chainsaw.tooltip.fuel",
        ChainsawFuel.percent(getFuel(stack)) + "%"));
    tooltip.add(I18n.format("csm.parks.chainsaw.tooltip.start"));
    tooltip.add(I18n.format("csm.parks.chainsaw.tooltip.sneak"));
    tooltip.add(I18n.format("csm.parks.chainsaw.tooltip.fell"));
  }
}
