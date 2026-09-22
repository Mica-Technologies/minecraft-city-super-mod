package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Arrow;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Legend;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;

/**
 * What one exit sign block offers: which values of each option it allows, what a freshly placed
 * one is set to, whether it reacts to redstone, how brightly it lights, and the presets its
 * creative tab lists.
 *
 * <p>Each option a block offers more than one value of becomes a block state property holding
 * only those values, so the multipart model needs parts for nothing the block cannot show and
 * the state count stays as small as the block allows. An option with a single value has no
 * property at all. The state count is the product of every property's size; check
 * {@link #stateCount()} before offering another value (the largest block in the mod has 5,184).
 *
 * <p>A spec is built once, in a static field of its block class, because the block state
 * container is created inside the {@code Block} constructor, before any instance field is set.
 *
 * @since 2026.9
 */
public final class ExitSignSpec {

  private final PropertyEnum<Arrow> arrow;
  private final PropertyEnum<Letters> letters;
  private final PropertyEnum<Housing> housing;
  private final PropertyEnum<Mount> mount;
  private final PropertyEnum<Heads> heads;
  private final PropertyEnum<Legend> legend;
  private final List<Arrow> arrows;
  private final List<Letters> letterColours;
  private final List<Housing> housings;
  private final List<Mount> mounts;
  private final List<Heads> headTypes;
  private final List<Legend> legends;
  private final ExitSignConfig defaults;
  private final boolean mainsPowered;
  private final int lightValue;
  private final List<ExitSignConfig> presets;

  private ExitSignSpec(Builder b) {
    this.arrows = b.arrows;
    this.letterColours = b.letters;
    this.housings = b.housings;
    this.mounts = b.mounts;
    this.headTypes = b.heads;
    this.legends = b.legends;
    this.arrow = property("arrow", Arrow.class, arrows);
    this.letters = property("letters", Letters.class, letterColours);
    this.housing = property("housing", Housing.class, housings);
    this.mount = property("mount", Mount.class, mounts);
    this.heads = property("heads", Heads.class, headTypes);
    this.legend = property("legend", Legend.class, legends);
    this.defaults = new ExitSignConfig(arrows.get(0), letterColours.get(0), housings.get(0),
        mounts.get(0), headTypes.get(0), legends.get(0));
    // Redstone is mains power, and all it changes is whether emergency heads light: a sign that
    // cannot have heads has no use for the property, and leaving it out halves its state count.
    this.mainsPowered = b.mainsPowered && headTypes.stream().anyMatch(h -> h != Heads.NONE);
    this.lightValue = b.lightValue;
    List<ExitSignConfig> clamped = new ArrayList<>();
    for (Preset preset : b.presets) {
      ExitSignConfig config = defaults.withLetters(preset.letters).withHousing(preset.housing);
      clamped.add(clamp(preset.heads != null ? config.withHeads(preset.heads) : config));
    }
    if (clamped.isEmpty()) {
      clamped.add(defaults);
    }
    this.presets = Collections.unmodifiableList(clamped);
  }

  @Nullable
  private static <E extends Enum<E> & net.minecraft.util.IStringSerializable> PropertyEnum<E>
  property(String name, Class<E> type, List<E> allowed) {
    return allowed.size() > 1 ? PropertyEnum.create(name, type, allowed) : null;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** The block state properties this sign's options take, in a stable order. */
  public List<IProperty<?>> properties() {
    List<IProperty<?>> out = new ArrayList<>();
    for (IProperty<?> p : Arrays.<IProperty<?>>asList(arrow, letters, housing, mount, heads,
        legend)) {
      if (p != null) {
        out.add(p);
      }
    }
    return out;
  }

  /**
   * How many block states a sign with this spec has: four facings, powered or not if it is
   * mains powered, times every option property's size.
   */
  public int stateCount() {
    int count = 4 * (mainsPowered ? 2 : 1);
    for (IProperty<?> p : properties()) {
      count *= p.getAllowedValues().size();
    }
    return count;
  }

  /** {@code config} with every value this sign does not offer replaced by its default. */
  public ExitSignConfig clamp(ExitSignConfig config) {
    return new ExitSignConfig(
        arrows.contains(config.getArrow()) ? config.getArrow() : arrows.get(0),
        letterColours.contains(config.getLetters()) ? config.getLetters() : letterColours.get(0),
        housings.contains(config.getHousing()) ? config.getHousing() : housings.get(0),
        mounts.contains(config.getMount()) ? config.getMount() : mounts.get(0),
        headTypes.contains(config.getHeads()) ? config.getHeads() : headTypes.get(0),
        legends.contains(config.getLegend()) ? config.getLegend() : legends.get(0));
  }

  /** {@code state} with its option properties set from {@code config}, clamped first. */
  public IBlockState apply(IBlockState state, ExitSignConfig config) {
    ExitSignConfig c = clamp(config);
    if (arrow != null) {
      state = state.withProperty(arrow, c.getArrow());
    }
    if (letters != null) {
      state = state.withProperty(letters, c.getLetters());
    }
    if (housing != null) {
      state = state.withProperty(housing, c.getHousing());
    }
    if (mount != null) {
      state = state.withProperty(mount, c.getMount());
    }
    if (heads != null) {
      state = state.withProperty(heads, c.getHeads());
    }
    if (legend != null) {
      state = state.withProperty(legend, c.getLegend());
    }
    return state;
  }

  public ExitSignConfig getDefaults() {
    return defaults;
  }

  /**
   * Whether the sign reacts to redstone: powered is mains on, unpowered is on battery. Only a sign
   * that can have emergency heads does, since nothing else about it changes on battery.
   */
  public boolean isMainsPowered() {
    return mainsPowered;
  }

  /** The light the sign gives off with its emergency heads dark. */
  public int getLightValue() {
    return lightValue;
  }

  public List<ExitSignConfig> getPresets() {
    return presets;
  }

  public List<Arrow> getArrows() {
    return arrows;
  }

  public List<Letters> getLetterColours() {
    return letterColours;
  }

  public List<Housing> getHousings() {
    return housings;
  }

  public List<Mount> getMounts() {
    return mounts;
  }

  public List<Heads> getHeadTypes() {
    return headTypes;
  }

  public List<Legend> getLegends() {
    return legends;
  }

  /** A preset as asked for, resolved against the finished spec's defaults when it is built. */
  private static final class Preset {

    final Letters letters;
    final Housing housing;
    @Nullable
    final Heads heads;

    Preset(Letters letters, Housing housing, @Nullable Heads heads) {
      this.letters = letters;
      this.housing = housing;
      this.heads = heads;
    }
  }

  /** Builds a spec. Every option offers all of its values unless narrowed; the first is the
   * default. */
  public static final class Builder {

    private List<Arrow> arrows = Arrays.asList(Arrow.values());
    private List<Letters> letters = Arrays.asList(Letters.values());
    private List<Housing> housings = Arrays.asList(Housing.WHITE, Housing.BLACK);
    private List<Mount> mounts = Arrays.asList(Mount.values());
    private List<Heads> heads = Arrays.asList(Heads.values());
    private List<Legend> legends = Arrays.asList(Legend.values());
    private boolean mainsPowered = true;
    private int lightValue = 12;
    private final List<Preset> presets = new ArrayList<>();

    private Builder() {
    }

    public Builder housings(Housing... values) {
      housings = list(values);
      return this;
    }

    public Builder letters(Letters... values) {
      letters = list(values);
      return this;
    }

    public Builder mounts(Mount... values) {
      mounts = list(values);
      return this;
    }

    public Builder heads(Heads... values) {
      heads = list(values);
      return this;
    }

    /**
     * A sign with no mains supply at all, such as a self-luminous one: it ignores redstone and has
     * no powered property even if it had heads.
     */
    public Builder unpowered() {
      mainsPowered = false;
      return this;
    }

    public Builder lightValue(int value) {
      lightValue = value;
      return this;
    }

    /** Adds a creative tab preset: the defaults with the letters and housing given. */
    public Builder preset(Letters letterColour, Housing finish) {
      return preset(letterColour, finish, null);
    }

    /** Adds a creative tab preset with emergency heads (or the default heads if null). */
    public Builder preset(Letters letterColour, Housing finish, @Nullable Heads headType) {
      presets.add(new Preset(letterColour, finish, headType));
      return this;
    }

    public ExitSignSpec build() {
      return new ExitSignSpec(this);
    }

    @SafeVarargs
    private static <E> List<E> list(E... values) {
      if (values.length == 0) {
        throw new IllegalArgumentException("an option needs at least one value");
      }
      return Collections.unmodifiableList(Arrays.asList(values));
    }
  }
}
