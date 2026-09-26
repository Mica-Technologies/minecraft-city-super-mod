package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * {@code /csm statecheck}: checks every CSM block's state container against a vanilla container
 * built over the same properties, state by state. For every state, property and value it
 * compares {@code withProperty} and {@code cycleProperty}, the neighbour table, the state's
 * properties, hash code and name, and the order of the states; and for every state that
 * {@code getStateFromMeta(getMetaFromState(s))} lands on a valid state with the same metadata.
 * Read only. The vanilla containers are built one block at a time and dropped.
 *
 * @see CsmBlockStateContainer
 * @since 2026.9
 */
public final class CsmStateCheck {

  private CsmStateCheck() {
  }

  /**
   * Runs the check.
   *
   * @return report lines
   */
  public static List<String> run() {
    long start = System.nanoTime();
    int blocks = 0;
    int ours = 0;
    long states = 0;
    long checks = 0;
    List<String> failures = new ArrayList<>();
    List<String> vanilla = new ArrayList<>();
    long vanillaStates = 0;
    for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
      ResourceLocation id = block.getRegistryName();
      if (id == null || !"csm".equals(id.getNamespace())) {
        continue;
      }
      blocks++;
      BlockStateContainer c = block.getBlockState();
      boolean mine = c instanceof CsmBlockStateContainer || c instanceof CsmExtendedBlockState;
      if (!mine) {
        vanilla.add(id + " (" + c.getValidStates().size() + ")");
        vanillaStates += c.getValidStates().size();
      } else {
        ours++;
      }
      states += c.getValidStates().size();
      try {
        checks += check(block, c, failures);
      } catch (Throwable t) {
        failures.add(id + ": threw " + t);
      }
    }
    List<String> out = new ArrayList<>();
    out.add(String.format(Locale.ROOT,
        "statecheck: %d CSM blocks (%d on CSM's container), %d states, %d comparisons, "
            + "%d failures, %d ms", blocks, ours, states, checks, failures.size(),
        (System.nanoTime() - start) / 1_000_000L));
    for (int i = 0; i < Math.min(20, failures.size()); i++) {
      out.add("FAIL " + failures.get(i));
    }
    out.add(String.format(Locale.ROOT, "statecheck: %d blocks on vanilla's container (%d states)"
        + "%s", vanilla.size(), vanillaStates, vanilla.isEmpty() ? "" : ": "
        + String.join(", ", vanilla.subList(0, Math.min(40, vanilla.size())))));
    return out;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static long check(Block block, BlockStateContainer c, List<String> failures) {
    String id = String.valueOf(block.getRegistryName());
    long checks = 0;
    IProperty<?>[] props = c.getProperties().toArray(new IProperty<?>[0]);
    BlockStateContainer ref = c instanceof ExtendedBlockState
        ? new CsmExtendedBlockState(block, props, ((ExtendedBlockState) c).getUnlistedProperties()
        .toArray(new IUnlistedProperty<?>[0]))
        : new CsmBlockStateContainer(block, props);
    ImmutableList<IBlockState> mine = c.getValidStates();
    ImmutableList<IBlockState> theirs = ref.getValidStates();
    if (mine.size() != theirs.size()) {
      failures.add(id + ": " + mine.size() + " states, vanilla " + theirs.size());
      return 1;
    }
    if (!namesOf(c).equals(namesOf(ref))) {
      failures.add(id + ": properties " + namesOf(c) + " vs " + namesOf(ref));
    }
    if (!c.toString().equals(ref.toString())) {
      failures.add(id + ": container name " + c + " vs " + ref);
    }
    // Vanilla state -> CSM state at the same position; positions must agree on properties.
    Map<IBlockState, IBlockState> toMine = new IdentityHashMap<>();
    Set<IBlockState> mineSet = Collections.newSetFromMap(new IdentityHashMap<>());
    for (int i = 0; i < mine.size(); i++) {
      IBlockState a = mine.get(i);
      IBlockState b = theirs.get(i);
      checks++;
      if (!a.getProperties().equals(b.getProperties()) || a.hashCode() != b.hashCode()
          || !a.toString().equals(b.toString())
          || !new ArrayList<>(a.getPropertyKeys()).equals(new ArrayList<>(b.getPropertyKeys()))) {
        failures.add(id + ": state " + i + " is " + a + ", vanilla " + b);
        return checks;
      }
      for (IProperty p : props) {
        checks++;
        if (!a.getValue(p).equals(b.getValue(p)) || !a.getPropertyKeys().contains(p)) {
          failures.add(id + ": state " + a + " value of " + p.getName() + " is " + a.getValue(p)
              + ", vanilla " + b.getValue(p));
          return checks;
        }
      }
      toMine.put(b, a);
      mineSet.add(a);
    }
    for (int i = 0; i < mine.size(); i++) {
      IBlockState a = mine.get(i);
      IBlockState b = theirs.get(i);
      for (IProperty p : props) {
        for (Object v : p.getAllowedValues()) {
          checks++;
          IBlockState ra = a.withProperty(p, (Comparable) v);
          IBlockState rb = b.withProperty(p, (Comparable) v);
          if (ra != toMine.get(rb)) {
            failures.add(id + ": " + a + " with " + p.getName() + "=" + v + " gave " + ra
                + ", vanilla " + rb);
            return checks;
          }
        }
        checks++;
        if (a.cycleProperty(p) != toMine.get(b.cycleProperty(p))) {
          failures.add(id + ": " + a + " cycle " + p.getName());
          return checks;
        }
      }
      ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> ta = ((BlockStateBase) a).getPropertyValueTable();
      ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> tb = ((BlockStateBase) b).getPropertyValueTable();
      checks++;
      if (ta.size() != tb.size()) {
        failures.add(id + ": " + a + " table size " + ta.size() + ", vanilla " + tb.size());
        return checks;
      }
      for (ImmutableTable.Cell<IProperty<?>, Comparable<?>, IBlockState> cell : tb.cellSet()) {
        if (ta.get(cell.getRowKey(), cell.getColumnKey()) != toMine.get(cell.getValue())) {
          failures.add(id + ": " + a + " table cell " + cell.getRowKey().getName() + "="
              + cell.getColumnKey());
          return checks;
        }
      }
      // Metadata round trip.
      checks++;
      int meta = block.getMetaFromState(a);
      IBlockState back = block.getStateFromMeta(meta);
      if (!mineSet.contains(back) || block.getMetaFromState(back) != meta) {
        failures.add(id + ": meta " + meta + " of " + a + " reads back as " + back);
        return checks;
      }
    }
    return checks;
  }

  private static List<String> namesOf(BlockStateContainer c) {
    List<String> names = new ArrayList<>();
    for (IProperty<?> p : c.getProperties()) {
      names.add(p.getName());
    }
    return names;
  }
}
