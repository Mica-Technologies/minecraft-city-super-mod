package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

/**
 * How one block's states are numbered, so a state's neighbour is found by arithmetic instead of
 * looked up in a table.
 *
 * <p>Vanilla gives every block state a table of its neighbours: for each property, the state
 * with each other value of it. That is one table cell per other value of every property, per
 * state. CSM's blocks have 290 thousand states and those tables held 7.1 million cells, close to
 * a gigabyte of the heap. Here a block's states are numbered in mixed radix instead: a state's
 * number is the sum, over its properties, of the index of its value times that property's
 * stride. The neighbour with property {@code p} set to another value is then the state whose
 * number differs by {@code (new index - old index) * stride(p)}, read out of one array per
 * block.</p>
 *
 * <p>The properties are in the container's order (sorted by name) and each property's values in
 * the order its {@link IProperty#getAllowedValues()} gives them, which is also the order the
 * vanilla table walks them in.</p>
 *
 * @see CsmBlockStateContainer
 * @see CsmExtendedBlockState
 * @since 2026.9
 */
public final class CsmStateLayout {

  /** Implemented by the states of CSM's containers. */
  public interface Holder {

    /**
     * Attaches the state to its block's layout.
     *
     * @param layout the layout
     * @param index  the state's number in it
     */
    void csmAttach(CsmStateLayout layout, int index);
  }

  private final Block block;
  private final IProperty<?>[] properties;
  private final List<?>[] values;
  /** Value to index for the properties with many values (null for the others). */
  private final ImmutableMap<?, Integer>[] valueIndex;
  private final int[] strides;
  private final IBlockState[] states;

  private CsmStateLayout(BlockStateContainer container) {
    this.block = container.getBlock();
    Collection<IProperty<?>> props = container.getProperties();
    int n = props.size();
    properties = props.toArray(new IProperty<?>[n]);
    values = new List<?>[n];
    @SuppressWarnings("unchecked")
    ImmutableMap<?, Integer>[] lookup = new ImmutableMap[n];
    valueIndex = lookup;
    strides = new int[n];
    int count = 1;
    for (int i = n - 1; i >= 0; i--) {
      values[i] = ImmutableList.copyOf(properties[i].getAllowedValues());
      if (values[i].size() > 8) {
        ImmutableMap.Builder<Object, Integer> b = ImmutableMap.builder();
        for (int v = 0; v < values[i].size(); v++) {
          b.put(values[i].get(v), v);
        }
        valueIndex[i] = b.build();
      }
      strides[i] = count;
      count *= values[i].size();
    }
    states = new IBlockState[count];
  }

  /**
   * Numbers the container's states and attaches each to the layout. Called once, at the end of
   * the container's constructor.
   *
   * @param container the container, whose states all implement {@link Holder}
   */
  static void attach(BlockStateContainer container) {
    CsmStateLayout layout = new CsmStateLayout(container);
    for (IBlockState state : container.getValidStates()) {
      int index = layout.indexOf(state.getProperties());
      if (layout.states[index] != null) {
        throw new IllegalStateException("Two states of " + container + " share number " + index);
      }
      layout.states[index] = state;
      ((Holder) state).csmAttach(layout, index);
    }
    for (IBlockState state : layout.states) {
      if (state == null) {
        throw new IllegalStateException("A state of " + container + " is missing");
      }
    }
  }

  int indexOf(ImmutableMap<IProperty<?>, Comparable<?>> map) {
    int index = 0;
    for (int i = 0; i < properties.length; i++) {
      int v = valueOf(i, map.get(properties[i]));
      if (v < 0) {
        throw new IllegalArgumentException("State value outside its property: " + map);
      }
      index += v * strides[i];
    }
    return index;
  }

  /**
   * The state at the given number.
   *
   * @param index the number
   *
   * @return the state
   */
  IBlockState state(int index) {
    return states[index];
  }

  private int valueOf(int property, Object value) {
    if (value == null) {
      return -1;
    }
    ImmutableMap<?, Integer> lookup = valueIndex[property];
    if (lookup == null) {
      return values[property].indexOf(value);
    }
    Integer v = lookup.get(value);
    return v == null ? -1 : v;
  }

  private int propertyIndex(IProperty<?> property) {
    for (int i = 0; i < properties.length; i++) {
      if (properties[i] == property) {
        return i;
      }
    }
    // Vanilla finds a property by equality (an ImmutableMap lookup), so an equal instance made
    // somewhere else is accepted there too.
    for (int i = 0; i < properties.length; i++) {
      if (properties[i].equals(property)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * {@link IBlockState#withProperty} for the state {@code from}, whose listed values are those
   * of state number {@code index}: {@code from} itself when the value does not change, otherwise
   * the (clean) state with that one value changed. Throws what vanilla throws, with its messages.
   *
   * @param from     the state the call is made on
   * @param index    the number of {@code from}'s listed values
   * @param property the property
   * @param value    the value
   *
   * @return the resulting state
   */
  IBlockState with(IBlockState from, int index, IProperty<?> property, Comparable<?> value) {
    int p = propertyIndex(property);
    if (p < 0) {
      throw new IllegalArgumentException("Cannot set property " + property
          + " as it does not exist in " + block.getBlockState());
    }
    List<?> allowed = values[p];
    int current = (index / strides[p]) % allowed.size();
    if (allowed.get(current) == value) {
      return from;
    }
    int wanted = valueOf(p, value);
    if (wanted < 0) {
      throw new IllegalArgumentException("Cannot set property " + property + " to " + value
          + " on block " + Block.REGISTRY.getNameForObject(block)
          + ", it is not an allowed value");
    }
    if (wanted == current) {
      // An equal but not identical value. Vanilla's table has no cell for the current value, so
      // it throws here; returning the state unchanged is what the caller meant.
      return from;
    }
    return states[index + (wanted - current) * strides[p]];
  }

  /**
   * Builds the neighbour table vanilla would have given state number {@code index}, for anything
   * that asks for it. Built on each call and not kept: holding it is exactly the cost this class
   * exists to avoid.
   *
   * @param index the state's number
   *
   * @return the table, equal to vanilla's
   */
  ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> table(int index) {
    Table<IProperty<?>, Comparable<?>, IBlockState> table = HashBasedTable.create();
    for (int p = 0; p < properties.length; p++) {
      List<?> allowed = values[p];
      int current = (index / strides[p]) % allowed.size();
      for (int v = 0; v < allowed.size(); v++) {
        if (v != current) {
          table.put(properties[p], (Comparable<?>) allowed.get(v),
              states[index + (v - current) * strides[p]]);
        }
      }
    }
    return ImmutableTable.copyOf(table);
  }
}
