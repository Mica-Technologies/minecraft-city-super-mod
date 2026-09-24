package com.micatechnologies.minecraft.csm.furniture.appliance;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What kind of appliance a block is: which {@link ApplianceRecipeBook} it works from, how fast,
 * how many things it takes at once, whether it needs water or fuel, how it sounds and whether it
 * lights up while it works. One spec is shared by every block of that kind (the stainless and
 * the white microwave); the block hands it to its {@link TileEntityAppliance} through
 * {@link IAppliance#getApplianceSpec()}.
 *
 * <p>Built by chaining, once, while the game starts:</p>
 * <pre>{@code
 * ApplianceSpec MICROWAVE = new ApplianceSpec(OVEN).timeFactor(0.375F).inputLimit(1)
 *     .doneSound(FurnishingsSounds.APPLIANCE_BEEP, 1.0F).light(6);
 * }</pre>
 *
 * <p><b>Power.</b> An electric appliance needs no fuel and no redstone: it runs whenever there
 * is something it can use in its input. A spec made with {@link #fuel()} burns furnace fuel
 * from a third slot instead (a grill).</p>
 *
 * <p><b>Water.</b> A spec made with {@link #water(int, BiPredicate)} keeps a tank of that many
 * cycles' water. A water bucket fills it, a water bottle adds one cycle, and wherever
 * {@code plumbed} is true (next to a kitchen sink) it is kept full.</p>
 *
 * <p><b>Supplies.</b> A spec made with {@link #supply(Predicate, String)} uses up one item from
 * the third slot each cycle instead of fuel: the copier's blank book and quill for each copy.
 * It works only while the slot holds one.</p>
 *
 * @since 2026.9
 */
public final class ApplianceSpec {

  private final ApplianceRecipeBook book;
  private float timeFactor = 1.0F;
  private int inputLimit = 64;
  private int waterCapacity;
  @Nullable
  private BiPredicate<World, BlockPos> plumbed;
  private boolean fuel;
  @Nullable
  private Predicate<ItemStack> supply;
  @Nullable
  private String supplyHint;
  @Nullable
  private ICsmSound runSound;
  private int runSoundEvery = 40;
  private float runVolume = 0.5F;
  private float runPitch = 1.0F;
  @Nullable
  private ICsmSound doneSound;
  private float donePitch = 1.0F;
  private int light;

  /**
   * Starts a spec working from {@code book}.
   *
   * @param book the recipes
   */
  public ApplianceSpec(ApplianceRecipeBook book) {
    this.book = book;
  }

  /**
   * Scales every recipe's time: 0.5 is twice as fast.
   *
   * @param factor the factor
   *
   * @return this spec
   */
  public ApplianceSpec timeFactor(float factor) {
    this.timeFactor = factor;
    return this;
  }

  /**
   * How many items the input slot holds (a microwave: one).
   *
   * @param limit the limit, 1 to 64
   *
   * @return this spec
   */
  public ApplianceSpec inputLimit(int limit) {
    this.inputLimit = limit;
    return this;
  }

  /**
   * Makes each cycle use one unit of water from a tank of {@code capacity}.
   *
   * @param capacity how many cycles a full tank (one bucket) lasts
   * @param plumbed  where the tank is kept full without a bucket, or null for nowhere
   *
   * @return this spec
   */
  public ApplianceSpec water(int capacity, @Nullable BiPredicate<World, BlockPos> plumbed) {
    this.waterCapacity = capacity;
    this.plumbed = plumbed;
    return this;
  }

  /**
   * Makes it burn furnace fuel, from a third slot, while it works.
   *
   * @return this spec
   */
  public ApplianceSpec fuel() {
    this.fuel = true;
    return this;
  }

  /**
   * Makes each cycle use up one item from a third slot, which takes only what {@code accepts}
   * accepts: the copier's book and quill. Not for a spec that burns fuel.
   *
   * @param accepts which items the slot takes
   * @param hint    the translation key of the screen's hint for the empty slot
   *
   * @return this spec
   */
  public ApplianceSpec supply(Predicate<ItemStack> accepts, String hint) {
    this.supply = accepts;
    this.supplyHint = hint;
    return this;
  }

  /**
   * A sound played while it works: when a cycle starts and every {@code every} ticks after.
   *
   * @param sound  the sound
   * @param every  how often, in ticks
   * @param volume its volume
   * @param pitch  its pitch
   *
   * @return this spec
   */
  public ApplianceSpec runSound(ICsmSound sound, int every, float volume, float pitch) {
    this.runSound = sound;
    this.runSoundEvery = Math.max(1, every);
    this.runVolume = volume;
    this.runPitch = pitch;
    return this;
  }

  /**
   * The sound played when something is done (put in the output).
   *
   * @param sound the sound
   * @param pitch its pitch
   *
   * @return this spec
   */
  public ApplianceSpec doneSound(ICsmSound sound, float pitch) {
    this.doneSound = sound;
    this.donePitch = pitch;
    return this;
  }

  /**
   * The light it gives while it works (an oven's lamp behind its window).
   *
   * @param level 0 to 15
   *
   * @return this spec
   */
  public ApplianceSpec light(int level) {
    this.light = level;
    return this;
  }

  public ApplianceRecipeBook getBook() {
    return book;
  }

  public float getTimeFactor() {
    return timeFactor;
  }

  public int getInputLimit() {
    return inputLimit;
  }

  /**
   * Whether each cycle uses water.
   *
   * @return true if it does
   */
  public boolean usesWater() {
    return waterCapacity > 0;
  }

  public int getWaterCapacity() {
    return waterCapacity;
  }

  /**
   * Whether the appliance at {@code pos} is plumbed in, its tank kept full.
   *
   * @param world the world
   * @param pos   the appliance
   *
   * @return true if it is
   */
  public boolean isPlumbed(World world, BlockPos pos) {
    return plumbed != null && plumbed.test(world, pos);
  }

  public boolean usesFuel() {
    return fuel;
  }

  /**
   * Whether each cycle uses up an item from the supply slot.
   *
   * @return true if it does
   */
  public boolean usesSupply() {
    return supply != null;
  }

  /**
   * Whether the supply slot takes {@code stack}.
   *
   * @param stack the stack
   *
   * @return true if it does
   */
  public boolean acceptsSupply(ItemStack stack) {
    return supply != null && supply.test(stack);
  }

  /**
   * The translation key of the hint shown over the empty supply slot.
   *
   * @return the key, or null
   */
  @Nullable
  public String getSupplyHint() {
    return supplyHint;
  }

  @Nullable
  public ICsmSound getRunSound() {
    return runSound;
  }

  public int getRunSoundEvery() {
    return runSoundEvery;
  }

  public float getRunVolume() {
    return runVolume;
  }

  public float getRunPitch() {
    return runPitch;
  }

  @Nullable
  public ICsmSound getDoneSound() {
    return doneSound;
  }

  public float getDonePitch() {
    return donePitch;
  }

  public int getLight() {
    return light;
  }
}
