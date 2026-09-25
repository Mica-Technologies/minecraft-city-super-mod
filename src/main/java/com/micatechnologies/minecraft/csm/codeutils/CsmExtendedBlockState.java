package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * {@link CsmBlockStateContainer} for a block with unlisted properties: Forge's
 * {@link ExtendedBlockState}, with its states finding their neighbours by arithmetic instead of
 * a per-state table.
 *
 * <p>The extended states behave as Forge's do, call for call (including that a state carrying
 * unlisted values keeps pointing at the clean state it was made from); only the table lookup
 * inside them is replaced.</p>
 *
 * @since 2026.9
 */
public class CsmExtendedBlockState extends ExtendedBlockState {

  /**
   * Creates the container.
   *
   * @param block              the block
   * @param properties         its listed properties
   * @param unlistedProperties its unlisted properties
   */
  public CsmExtendedBlockState(Block block, IProperty<?>[] properties,
      IUnlistedProperty<?>[] unlistedProperties) {
    super(block, properties, unlistedProperties);
    CsmStateLayout.attach(this);
  }

  /** Vanilla's text, naming the class it stands in for, so messages read the same. */
  @Override
  public String toString() {
    return com.google.common.base.MoreObjects.toStringHelper(ExtendedBlockState.class)
        .add("block", Block.REGISTRY.getNameForObject(getBlock()))
        .add("properties", com.google.common.collect.Iterables.transform(getProperties(),
            p -> p == null ? "<NULL>" : p.getName()))
        .toString();
  }

  @Override
  protected StateImplementation createState(Block block,
      ImmutableMap<IProperty<?>, Comparable<?>> properties,
      @Nullable ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties) {
    if (unlistedProperties == null || unlistedProperties.isEmpty()) {
      return new CsmBlockStateContainer.State(block, properties);
    }
    return new State(block, properties, unlistedProperties, null, 0, null);
  }

  /** Forge's extended state, finding its neighbours through the block's layout. */
  protected static class State extends ExtendedStateImplementation
      implements CsmStateLayout.Holder {

    private CsmStateLayout layout;
    private int index;

    protected State(Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties,
        ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties,
        @Nullable CsmStateLayout layout, int index, @Nullable IBlockState clean) {
      super(block, properties, unlistedProperties, null, clean);
      this.layout = layout;
      this.index = index;
    }

    @Override
    public void csmAttach(CsmStateLayout layout, int index) {
      this.layout = layout;
      this.index = index;
    }

    @Override
    public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property,
        V value) {
      // Forge's ExtendedStateImplementation.withProperty, with the table read replaced.
      IBlockState clean = layout.with(this, index, property, value);
      IBlockState cleanState = getClean();
      if (clean == cleanState) {
        return this;
      }
      if (this == cleanState) {
        return clean;
      }
      int cleanIndex = clean == this ? index : ((State) clean).index;
      return new State(getBlock(), clean.getProperties(), getUnlistedProperties(), layout,
          cleanIndex, cleanState);
    }

    @Override
    public <V> IExtendedBlockState withProperty(IUnlistedProperty<V> property,
        @Nullable V value) {
      // Forge's, making this class's states rather than its own.
      ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlisted = getUnlistedProperties();
      Optional<?> oldValue = unlisted.get(property);
      if (oldValue == null) {
        throw new IllegalArgumentException("Cannot set unlisted property " + property
            + " as it does not exist in " + getBlock().getBlockState());
      }
      if (Objects.equals(oldValue.orElse(null), value)) {
        return this;
      }
      if (!property.isValid(value)) {
        throw new IllegalArgumentException("Cannot set unlisted property " + property + " to "
            + value + " on block " + Block.REGISTRY.getNameForObject(getBlock())
            + ", it is not an allowed value");
      }
      boolean clean = true;
      ImmutableMap.Builder<IUnlistedProperty<?>, Optional<?>> builder = ImmutableMap.builder();
      for (Map.Entry<IUnlistedProperty<?>, Optional<?>> entry : unlisted.entrySet()) {
        IUnlistedProperty<?> key = entry.getKey();
        Optional<?> newValue = key.equals(property) ? Optional.ofNullable(value)
            : entry.getValue();
        if (newValue.isPresent()) {
          clean = false;
        }
        builder.put(key, newValue);
      }
      if (clean) {
        return (IExtendedBlockState) getClean();
      }
      return new State(getBlock(), getProperties(), builder.build(), layout, index, getClean());
    }

    @Override
    public ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> getPropertyValueTable() {
      return layout.table(index);
    }

    /** Vanilla builds the neighbour table here; this container never builds one. */
    @Override
    public void buildPropertyValueTable(
        Map<Map<IProperty<?>, Comparable<?>>, StateImplementation> map) {
      // Intentionally empty.
    }
  }
}
