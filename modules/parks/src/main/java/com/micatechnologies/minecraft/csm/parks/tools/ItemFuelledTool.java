package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.parks.ParksSounds;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A small-engine tree tool: the chainsaw ({@link ItemChainsaw}) and the stump grinder
 * ({@link ItemStumpGrinder}). What they share is the engine, which this class runs; what each
 * does with it while running is the subclass's.
 *
 * <ul>
 *   <li><b>Fuel.</b> It burns whatever a furnace burns, held on the stack as burn ticks
 *   ({@link ChainsawFuel}). Sneak and right-click while it is off to pour in one item from the
 *   inventory, the best that still fits; a lava bucket gives its bucket back.</li>
 *   <li><b>Pull start.</b> It starts off. Each right-click pulls the cord; it catches on the
 *   third to fifth pull. Running, it idles, burning fuel while held. Sneak and right-click to stop
 *   it; it also stops when the tank runs dry or when it has not been held for a few seconds.</li>
 * </ul>
 *
 * <p>Fuel and running state live on the stack, written at most once a second while idling so the
 * held item is not resynchronised every tick, and a change of fuel never replays the equip
 * animation. Not enchantable. The NBT keys are the chainsaw's, unchanged from before the grinder
 * shared them, so a saved chainsaw keeps its fuel.</p>
 *
 * @since 2026.10
 */
public abstract class ItemFuelledTool extends AbstractItem {

  private static final String NBT_FUEL = "csm_fuel";
  private static final String NBT_RUNNING = "csm_running";
  private static final String NBT_PULLS = "csm_pulls";
  private static final String NBT_PULLS_NEEDED = "csm_pulls_needed";
  private static final String NBT_HELD_AT = "csm_held_at";

  /** The prefix of the fuel messages and tooltip lines every engine tool shares. */
  private static final String SHARED = "csm.parks.chainsaw.";

  /** How often a running tool burns fuel and plays its idle sound, in ticks. */
  private static final int IDLE_PERIOD = 20;
  /** How long a running tool may go unheld before it stalls, in ticks. */
  private static final int STALL_AFTER = 60;
  /** Ticks between cord pulls. */
  private static final int PULL_COOLDOWN = 8;

  protected ItemFuelledTool(int durability) {
    super(durability, 1);
    setNoRepair();
    setFull3D();
  }

  /**
   * The prefix of this tool's own messages: {@code <prefix>.cold}, {@code .started},
   * {@code .stopped} and {@code .ranout}.
   */
  protected abstract String getMessagePrefix();

  /** The translation key of the tooltip line saying what the tool does while running. */
  protected abstract String getUseTooltipKey();

  /** The engine catching on the last pull. */
  protected abstract ParksSounds getStartSound();

  /** One second of idle, played back to back while it runs. */
  protected abstract ParksSounds getIdleSound();

  /** The volume the idle sound plays at. */
  protected float getIdleVolume() {
    return 0.6F;
  }

  /** The translation key of one of this tool's own messages. */
  public String messageKey(String suffix) {
    return getMessagePrefix() + "." + suffix;
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

  protected static void stop(ItemStack stack) {
    NBTTagCompound tag = tag(stack);
    tag.setBoolean(NBT_RUNNING, false);
    tag.setInteger(NBT_PULLS, 0);
    tag.setInteger(NBT_PULLS_NEEDED, 0);
  }

  protected static void play(World world, EntityPlayer player, ParksSounds sound, float volume) {
    SoundEvent event = sound.getSoundEvent();
    if (event != null) {
      world.playSound(null, player.posX, player.posY, player.posZ, event, SoundCategory.PLAYERS,
          volume, 0.95F + world.rand.nextFloat() * 0.1F);
    }
  }

  /**
   * Pays for work done on {@code logs} logs, out of survival: fuel by {@link ChainsawFuel#afterCut}
   * and one durability a log, stopping the engine with a message when the tank runs dry.
   */
  protected void payForLogs(ItemStack stack, EntityPlayer player, int logs) {
    if (player.capabilities.isCreativeMode) {
      return;
    }
    int fuel = ChainsawFuel.afterCut(getFuel(stack), logs);
    setFuel(stack, fuel);
    if (fuel <= 0) {
      stop(stack);
      player.sendStatusMessage(new TextComponentTranslation(messageKey("ranout")), true);
    }
    stack.damageItem(logs, player);
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
        player.sendStatusMessage(new TextComponentTranslation(messageKey("stopped")), true);
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
      player.sendStatusMessage(new TextComponentTranslation(SHARED + "nofuel"), true);
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
    play(world, player, getStartSound(), 1.0F);
    player.sendStatusMessage(new TextComponentTranslation(messageKey("started")), true);
  }

  /**
   * Pours one fuel item from the inventory into the tank: the one burning longest that still
   * fits. Tools and other damageable items are never burned. In creative, an empty inventory
   * fills the tank.
   */
  private void refuel(World world, EntityPlayer player, ItemStack tool) {
    int fuel = getFuel(tool);
    List<ItemStack> inventory = player.inventory.mainInventory;
    int best = -1;
    int bestBurn = 0;
    boolean anyFuel = false;
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack s = inventory.get(i);
      if (s.isEmpty() || s == tool || s.isItemStackDamageable()) {
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
        setFuel(tool, ChainsawFuel.CAPACITY);
        refuelled(world, player, tool);
        return;
      }
      player.sendStatusMessage(
          new TextComponentTranslation(SHARED + (anyFuel ? "full" : "nofuelitem")), true);
      return;
    }
    ItemStack source = inventory.get(best);
    ItemStack container = source.getItem().getContainerItem(source);
    setFuel(tool, fuel + bestBurn);
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
    refuelled(world, player, tool);
  }

  private static void refuelled(World world, EntityPlayer player, ItemStack tool) {
    world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_EMPTY,
        SoundCategory.PLAYERS, 0.8F, 0.8F);
    player.sendStatusMessage(new TextComponentTranslation(SHARED + "refuelled",
        ChainsawFuel.percent(getFuel(tool)) + "%"), true);
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
        player.sendStatusMessage(new TextComponentTranslation(messageKey("ranout")), true);
        return;
      }
      play(world, player, getIdleSound(), getIdleVolume());
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
    tooltip.add(I18n.format(SHARED + (isRunning(stack) ? "tooltip.running" : "tooltip.off")));
    tooltip.add(I18n.format(SHARED + "tooltip.fuel", ChainsawFuel.percent(getFuel(stack)) + "%"));
    tooltip.add(I18n.format(SHARED + "tooltip.start"));
    tooltip.add(I18n.format(SHARED + "tooltip.sneak"));
    tooltip.add(I18n.format(getUseTooltipKey()));
  }
}
