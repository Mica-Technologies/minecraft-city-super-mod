package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * The block state container every CSM block uses in place of vanilla's
 * {@link BlockStateContainer}: the same states, in the same order, with the same properties,
 * equality, hash codes, names and metadata, but without a neighbour table on every state.
 *
 * <p>Vanilla builds, for every state, a table of the states one property change away, and
 * {@code withProperty} reads it. For CSM's 290 thousand states that was 7.1 million cells and
 * about 945 MB of the heap, and building them took a good part of pre-init. Here
 * {@code withProperty} finds the neighbour by arithmetic over a per-block numbering of the states
 * ({@link CsmStateLayout}); {@link #getPropertyValueTable()} still answers, building the table
 * vanilla would have held when asked.</p>
 *
 * <p>Use it exactly where {@code new BlockStateContainer(this, ...)} would be written. A block
 * with unlisted properties uses {@link CsmExtendedBlockState}; {@link Builder} picks between them
 * as Forge's builder does.</p>
 *
 * <p>This is our own design, not a copy of any other mod's.</p>
 *
 * @since 2026.9
 */
public class CsmBlockStateContainer extends BlockStateContainer {

  /**
   * Creates the container.
   *
   * @param block      the block
   * @param properties its properties
   */
  public CsmBlockStateContainer(Block block, IProperty<?>... properties) {
    super(block, properties, null);
    CsmStateLayout.attach(this);
  }

  /** Vanilla's text, naming the class it stands in for, so messages read the same. */
  @Override
  public String toString() {
    return com.google.common.base.MoreObjects.toStringHelper(BlockStateContainer.class)
        .add("block", Block.REGISTRY.getNameForObject(getBlock()))
        .add("properties", com.google.common.collect.Iterables.transform(getProperties(),
            p -> p == null ? "<NULL>" : p.getName()))
        .toString();
  }

  @Override
  protected StateImplementation createState(Block block,
      ImmutableMap<IProperty<?>, Comparable<?>> properties,
      @Nullable ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties) {
    return new State(block, properties);
  }

  /** A state that finds its neighbours through the block's {@link CsmStateLayout}. */
  public static class State extends StateImplementation implements CsmStateLayout.Holder {

    private CsmStateLayout layout;
    private int index;

    protected State(Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties) {
      super(block, properties);
    }

    @Override
    public void csmAttach(CsmStateLayout layout, int index) {
      this.layout = layout;
      this.index = index;
    }

    @Override
    public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property,
        V value) {
      return layout.with(this, index, property, value);
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

  /**
   * Forge's {@link BlockStateContainer.Builder}, returning CSM's containers: a
   * {@link CsmBlockStateContainer}, or a {@link CsmExtendedBlockState} when there are unlisted
   * properties.
   */
  public static class Builder {

    private final Block block;
    private final List<IProperty<?>> listed = new ArrayList<>();
    private final List<IUnlistedProperty<?>> unlisted = new ArrayList<>();

    public Builder(Block block) {
      this.block = block;
    }

    public Builder add(IProperty<?>... props) {
      for (IProperty<?> prop : props) {
        listed.add(prop);
      }
      return this;
    }

    public Builder add(IUnlistedProperty<?>... props) {
      for (IUnlistedProperty<?> prop : props) {
        unlisted.add(prop);
      }
      return this;
    }

    public BlockStateContainer build() {
      IProperty<?>[] listedArray = listed.toArray(new IProperty<?>[0]);
      if (unlisted.isEmpty()) {
        return new CsmBlockStateContainer(block, listedArray);
      }
      return new CsmExtendedBlockState(block, listedArray,
          unlisted.toArray(new IUnlistedProperty<?>[0]));
    }
  }
}
