package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

/**
 * Everything a custom door remembers: its three materials and how it behaves. Immutable, so it can
 * key the baked model's quad cache. Written to a door item's NBT by the Door Workshop, copied into
 * the placed door's {@link TileEntityCustomDoor}, and read back from either -- every value clamped
 * or checked on the way in, since an item's NBT is anyone's to edit.
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CustomDoorSettings {

  /**
   * How a custom door opens.
   *
   * @since 1.0
   */
  public enum Movement {
    /** Hinged, as the fixed doors swing. */
    SWING,
    /** Slides sideways toward its hinge side, into the wall; a pair parts in the middle. */
    SLIDE,
    /** A pair slides the same way, the two leaves stacking in one pocket. */
    SLIDE_TOGETHER,
    /** Slides straight up. */
    SLIDE_UP,
    /** The upper half slides up and the lower half down. */
    SPLIT;

    public String key() {
      return name().toLowerCase(Locale.ROOT);
    }

    static Movement of(int ordinal) {
      Movement[] all = values();
      return ordinal >= 0 && ordinal < all.length ? all[ordinal] : SWING;
    }
  }

  /**
   * What a custom door sounds like.
   *
   * @since 1.0
   */
  public enum Sound {
    WOOD(SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, 1.0F),
    IRON(SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundEvents.BLOCK_IRON_DOOR_CLOSE, 1.0F),
    HEAVY(SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundEvents.BLOCK_IRON_DOOR_CLOSE, 0.6F),
    TRAPDOOR(SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE,
        1.0F),
    GATE(SoundEvents.BLOCK_FENCE_GATE_OPEN, SoundEvents.BLOCK_FENCE_GATE_CLOSE, 1.0F),
    PNEUMATIC(SoundEvents.BLOCK_PISTON_EXTEND, SoundEvents.BLOCK_PISTON_CONTRACT, 1.3F),
    SLIDING(SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 0.8F),
    SILENT(null, null, 1.0F);

    private final SoundEvent open;
    private final SoundEvent close;
    private final float pitch;

    Sound(SoundEvent open, SoundEvent close, float pitch) {
      this.open = open;
      this.close = close;
      this.pitch = pitch;
    }

    public SoundEvent event(boolean opening) {
      return opening ? open : close;
    }

    public float pitch() {
      return pitch;
    }

    public String key() {
      return name().toLowerCase(Locale.ROOT);
    }

    static Sound of(int ordinal) {
      Sound[] all = values();
      return ordinal >= 0 && ordinal < all.length ? all[ordinal] : WOOD;
    }
  }

  /**
   * What opens a custom door.
   *
   * @since 1.0
   */
  public enum Redstone {
    /** A hand, and redstone holding it open. */
    NORMAL,
    /** Only redstone; a hand does nothing. */
    REDSTONE_ONLY,
    /** Only a hand; redstone does nothing. */
    HAND_ONLY,
    /** A hand, but a redstone signal locks it shut. */
    REDSTONE_LOCK;

    public String key() {
      return name().toLowerCase(Locale.ROOT);
    }

    static Redstone of(int ordinal) {
      Redstone[] all = values();
      return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NORMAL;
    }
  }

  public static final int MIN_OPEN_TICKS = 4;
  public static final int MAX_OPEN_TICKS = 60;
  public static final int MAX_AUTO_CLOSE_TICKS = 1200;

  /** A plain oak door in an oak frame: what a door with no settings is. */
  public static final CustomDoorSettings DEFAULT = new CustomDoorSettings(
      Blocks.PLANKS.getDefaultState(), Blocks.PLANKS.getDefaultState(),
      Blocks.PLANKS.getDefaultState(), Movement.SWING, Sound.WOOD, 8, 0, Redstone.NORMAL, false);

  private final IBlockState frame;
  private final IBlockState upper;
  private final IBlockState lower;
  private final Movement movement;
  private final Sound sound;
  private final int openTicks;
  private final int autoCloseTicks;
  private final Redstone redstone;
  private final boolean proximity;

  /**
   * Constructs {@link CustomDoorSettings}, clamping what needs clamping.
   *
   * @since 1.0
   */
  public CustomDoorSettings(IBlockState frame, IBlockState upper, IBlockState lower,
      Movement movement, Sound sound, int openTicks, int autoCloseTicks, Redstone redstone,
      boolean proximity) {
    this.frame = frame;
    this.upper = upper;
    this.lower = lower;
    this.movement = movement;
    this.sound = sound;
    this.openTicks = Math.max(MIN_OPEN_TICKS, Math.min(MAX_OPEN_TICKS, openTicks));
    this.autoCloseTicks = Math.max(0, Math.min(MAX_AUTO_CLOSE_TICKS, autoCloseTicks));
    this.redstone = redstone;
    this.proximity = proximity;
  }

  public IBlockState frame() {
    return frame;
  }

  public IBlockState upper() {
    return upper;
  }

  public IBlockState lower() {
    return lower;
  }

  public Movement movement() {
    return movement;
  }

  public Sound sound() {
    return sound;
  }

  public int openTicks() {
    return openTicks;
  }

  public int autoCloseTicks() {
    return autoCloseTicks;
  }

  public Redstone redstone() {
    return redstone;
  }

  public boolean proximity() {
    return proximity;
  }

  /**
   * The same settings with other materials.
   *
   * @since 1.0
   */
  public CustomDoorSettings withMaterials(IBlockState frame, IBlockState upper,
      IBlockState lower) {
    return new CustomDoorSettings(frame, upper, lower, movement, sound, openTicks, autoCloseTicks,
        redstone, proximity);
  }

  /**
   * The same materials with other behaviour.
   *
   * @since 1.0
   */
  public CustomDoorSettings withBehaviour(Movement movement, Sound sound, int openTicks,
      int autoCloseTicks, Redstone redstone, boolean proximity) {
    return new CustomDoorSettings(frame, upper, lower, movement, sound, openTicks,
        autoCloseTicks, redstone, proximity);
  }

  // --- NBT ------------------------------------------------------------------------------------------

  private static final String KEY = "csmDoor";

  private static void writeState(NBTTagCompound tag, String key, IBlockState state) {
    ResourceLocation id = state.getBlock().getRegistryName();
    tag.setString(key, id == null ? "minecraft:planks" : id.toString());
    tag.setByte(key + "m", (byte) state.getBlock().getMetaFromState(state));
  }

  @SuppressWarnings("deprecation")
  private static IBlockState readState(NBTTagCompound tag, String key, IBlockState fallback) {
    if (!tag.hasKey(key)) {
      return fallback;
    }
    Block block = Block.getBlockFromName(tag.getString(key));
    if (block == null || block == Blocks.AIR || !CustomDoorMaterials.allowed(block)) {
      return fallback;
    }
    return block.getStateFromMeta(tag.getByte(key + "m") & 15);
  }

  /**
   * Writes these settings under their own key of {@code compound}.
   *
   * @param compound an item's tag, or a tile entity's
   *
   * @since 1.0
   */
  public void write(NBTTagCompound compound) {
    NBTTagCompound t = new NBTTagCompound();
    writeState(t, "f", frame);
    writeState(t, "u", upper);
    writeState(t, "l", lower);
    t.setByte("mv", (byte) movement.ordinal());
    t.setByte("s", (byte) sound.ordinal());
    t.setShort("ot", (short) openTicks);
    t.setShort("ac", (short) autoCloseTicks);
    t.setByte("r", (byte) redstone.ordinal());
    t.setBoolean("p", proximity);
    compound.setTag(KEY, t);
  }

  /**
   * Reads settings written by {@link #write}, or {@link #DEFAULT} if there are none.
   *
   * @param compound an item's tag, or a tile entity's (may be null)
   *
   * @return the settings
   *
   * @since 1.0
   */
  @Nonnull
  public static CustomDoorSettings read(NBTTagCompound compound) {
    if (compound == null || !compound.hasKey(KEY)) {
      return DEFAULT;
    }
    NBTTagCompound t = compound.getCompoundTag(KEY);
    return new CustomDoorSettings(readState(t, "f", DEFAULT.frame),
        readState(t, "u", DEFAULT.upper), readState(t, "l", DEFAULT.lower),
        Movement.of(t.getByte("mv")), Sound.of(t.getByte("s")), t.getShort("ot"),
        t.getShort("ac"), Redstone.of(t.getByte("r")), t.getBoolean("p"));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CustomDoorSettings)) {
      return false;
    }
    CustomDoorSettings s = (CustomDoorSettings) o;
    return openTicks == s.openTicks && autoCloseTicks == s.autoCloseTicks
        && proximity == s.proximity && frame == s.frame && upper == s.upper && lower == s.lower
        && movement == s.movement && sound == s.sound && redstone == s.redstone;
  }

  @Override
  public int hashCode() {
    return Objects.hash(frame, upper, lower, movement, sound, openTicks, autoCloseTicks,
        redstone, proximity);
  }
}
