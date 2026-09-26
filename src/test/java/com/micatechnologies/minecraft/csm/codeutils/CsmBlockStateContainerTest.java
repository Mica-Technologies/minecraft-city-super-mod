package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCustomDoor;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockPCC;
import com.micatechnologies.minecraft.csm.constructionsite.BlockScaffoldFrame;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.BlockExitSignTraditionalRounded;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockStandpipeWallPipe;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLeaves;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLeafType;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import com.micatechnologies.minecraft.csm.parks.trees.TreeWood;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleConcrete;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleMastArmCurve;
import com.micatechnologies.minecraft.csm.trafficaccessories.MastArmCurveProfile;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockDynamicRouteMarkerSign;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusStopFlag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Holds CSM's state containers to vanilla's: for every state, property and value,
 * {@code withProperty} and {@code cycleProperty} must land on the state vanilla's container
 * lands on, the neighbour table must match, and properties, hash codes, names and state order
 * must be identical. Checked on the heaviest real blocks (one per base class and the biggest
 * state counts) and on synthetic property sets, including extended states with unlisted values.
 * The in-game {@code /csm statecheck} runs the same comparison over every registered block.
 */
class CsmBlockStateContainerTest {

  enum Six implements IStringSerializable {
    A, B, C, D, E, F;

    @Override
    public String getName() {
      return name().toLowerCase();
    }
  }

  @BeforeAll
  static void bootstrap() {
    // Vanilla's sounds, materials and registries, which block constructors read.
    Bootstrap.register();
  }

  private static IProperty<?>[] pending;
  private static IUnlistedProperty<?>[] pendingUnlisted;

  /** A plain block whose container is built from the properties set just before it is made. */
  static final class TestBlock extends Block {

    TestBlock() {
      super(Material.ROCK);
    }

    @Override
    protected BlockStateContainer createBlockState() {
      return pendingUnlisted == null || pendingUnlisted.length == 0
          ? new CsmBlockStateContainer(this, pending)
          : new CsmExtendedBlockState(this, pending, pendingUnlisted);
    }
  }

  static Stream<Arguments> realBlocks() {
    List<Arguments> out = new ArrayList<>();
    add(out, "standpipe_wall_pipe_main_red", () -> new BlockStandpipeWallPipe(
        "standpipe_wall_pipe_main_red", 4.0F, BlockStandpipeWallPipe.Fitting.NONE, null));
    add(out, "exit_sign_traditional_rounded", BlockExitSignTraditionalRounded::new);
    add(out, "trafficpoleverticalconcrete",
        () -> new BlockTrafficPoleConcrete("trafficpoleverticalconcrete", 6.0, 8.0));
    add(out, "dynamic_route_marker_sign", BlockDynamicRouteMarkerSign::new);
    add(out, "bus_stop_flag_cityline", () -> new BlockBusStopFlag("bus_stop_flag_cityline"));
    add(out, "trafficpolemastarmcurve10x2concrete", () -> new BlockTrafficPoleMastArmCurve(
        "trafficpolemastarmcurve10x2concrete", MastArmCurveProfile.P10X2));
    add(out, "scaffold_frame", BlockScaffoldFrame::new);
    add(out, "pcc (NSEWUD)", BlockPCC::new);
    add(out, "trafficpolehorizontalangleblack (diagonal)",
        BlockTrafficPoleHorizontalAngleBlack::new);
    add(out, "custom door (extended)", BlockCustomDoor::new);
    add(out, "tree log (extended)",
        () -> new BlockTreeLog("tree_log_elm_thin", TreeWood.ELM, TreeLogWidth.THIN));
    add(out, "tree leaves (extended)", () -> new BlockTreeLeaves("tree_leaves_elm",
        TreeLeafType.BROADLEAF, "csm:blocks/parks/leaves_elm"));
    return out.stream();
  }

  private static void add(List<Arguments> out, String name, Supplier<Block> make) {
    out.add(Arguments.of(name, make));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("realBlocks")
  void realBlockMatchesVanilla(String name, Supplier<Block> make) {
    Block block = make.get();
    BlockStateContainer c = block.getBlockState();
    assertTrue(c instanceof CsmBlockStateContainer || c instanceof CsmExtendedBlockState,
        name + " is on " + c.getClass());
    compare(block, c);
    // Metadata round trip.
    Set<IBlockState> valid = Collections.newSetFromMap(new IdentityHashMap<>());
    valid.addAll(c.getValidStates());
    for (IBlockState s : c.getValidStates()) {
      int meta = block.getMetaFromState(s);
      IBlockState back = block.getStateFromMeta(meta);
      assertTrue(valid.contains(back), name + ": meta " + meta + " reads back outside");
      assertEquals(meta, block.getMetaFromState(back), name + ": meta of " + s);
    }
    assertTrue(valid.contains(block.getDefaultState()), name + ": default state");
  }

  @Test
  void syntheticShapes() {
    PropertyInteger big = PropertyInteger.create("shape", 0, 223);
    PropertyEnum<Six> six = PropertyEnum.create("hang", Six.class);
    PropertyDirection facing = PropertyDirection.create("facing");
    PropertyBool a = PropertyBool.create("a");
    PropertyBool b = PropertyBool.create("b");
    PropertyInteger small = PropertyInteger.create("n", 0, 2);
    IProperty<?>[][] shapes = {
        {},
        {facing},
        {big, facing},
        {six, a, b, small, facing},
        {a},
    };
    for (IProperty<?>[] shape : shapes) {
      pending = shape;
      pendingUnlisted = null;
      Block block = new TestBlock();
      compare(block, block.getBlockState());
    }
  }

  @Test
  void largeIntegerValuesAndErrors() {
    PropertyInteger big = PropertyInteger.create("shape", 0, 223);
    PropertyBool a = PropertyBool.create("a");
    pending = new IProperty<?>[]{big, a};
    pendingUnlisted = null;
    Block block = new TestBlock();
    IBlockState s = block.getDefaultState().withProperty(big, 200);
    assertEquals(200, (int) s.getValue(big));
    // An equal but separately boxed value is the same state (vanilla would throw here).
    assertSame(s, s.withProperty(big, new Integer(200)));
    assertEquals(150, (int) s.withProperty(big, new Integer(150)).getValue(big));
    assertThrows(IllegalArgumentException.class, () -> s.withProperty(big, 224));
    assertThrows(IllegalArgumentException.class,
        () -> s.withProperty(PropertyBool.create("missing"), true));
    // An equal property made elsewhere is accepted, as vanilla's map lookup accepts it.
    assertEquals(false, s.withProperty(PropertyBool.create("a"), false).getValue(a));
  }

  @Test
  void extendedStatesMatchForge() {
    PropertyInteger n = PropertyInteger.create("n", 0, 3);
    PropertyBool a = PropertyBool.create("a");
    IUnlistedProperty<Integer> u = Properties.toUnlisted(PropertyInteger.create("u", 0, 9));
    pending = new IProperty<?>[]{n, a};
    pendingUnlisted = new IUnlistedProperty<?>[]{u};
    Block block = new TestBlock();
    BlockStateContainer mine = block.getBlockState();
    assertTrue(mine instanceof CsmExtendedBlockState);
    compare(block, mine);
    ExtendedBlockState ref = new ExtendedBlockState(block, pending, pendingUnlisted);
    Map<IBlockState, IBlockState> toMine = mapByPosition(mine, ref);
    for (int i = 0; i < mine.getValidStates().size(); i++) {
      IExtendedBlockState cm = (IExtendedBlockState) mine.getValidStates().get(i);
      IExtendedBlockState cr = (IExtendedBlockState) ref.getValidStates().get(i);
      // Dirty (carrying an unlisted value) states.
      IExtendedBlockState dm = cm.withProperty(u, 5);
      IExtendedBlockState dr = cr.withProperty(u, 5);
      assertEquals(dr.getProperties(), dm.getProperties());
      assertEquals(dr.getUnlistedProperties(), dm.getUnlistedProperties());
      assertSame(cm, dm.getClean());
      // Both refuse a value the unlisted property does not accept, with the same message.
      IllegalArgumentException em = assertThrows(IllegalArgumentException.class,
          () -> dm.withProperty(u, null));
      IllegalArgumentException er = assertThrows(IllegalArgumentException.class,
          () -> dr.withProperty(u, null));
      assertEquals(er.getMessage().replaceAll("@[0-9a-f]+", ""),
          em.getMessage().replaceAll("@[0-9a-f]+", ""));
      assertSame(dm, dm.withProperty(u, 5));
      for (int v = 0; v <= 3; v++) {
        IBlockState rm = dm.withProperty(n, v);
        IBlockState rr = dr.withProperty(n, v);
        assertEquals(rr.getProperties(), rm.getProperties());
        assertEquals(rr instanceof IExtendedBlockState, rm instanceof IExtendedBlockState);
        if (rr instanceof IExtendedBlockState) {
          assertEquals(((IExtendedBlockState) rr).getUnlistedProperties(),
              ((IExtendedBlockState) rm).getUnlistedProperties());
          assertSame(toMine.get(((IExtendedBlockState) rr).getClean()),
              ((IExtendedBlockState) rm).getClean());
          assertEquals(((BlockStateBase) rr).getPropertyValueTable().size(),
              ((BlockStateBase) rm).getPropertyValueTable().size());
        } else {
          assertSame(toMine.get(rr), rm);
        }
      }
    }
  }

  private static Map<IBlockState, IBlockState> mapByPosition(BlockStateContainer mine,
      BlockStateContainer ref) {
    Map<IBlockState, IBlockState> toMine = new IdentityHashMap<>();
    for (int i = 0; i < ref.getValidStates().size(); i++) {
      toMine.put(ref.getValidStates().get(i), mine.getValidStates().get(i));
    }
    return toMine;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void compare(Block block, BlockStateContainer mine) {
    IProperty<?>[] props = mine.getProperties().toArray(new IProperty<?>[0]);
    BlockStateContainer ref = mine instanceof ExtendedBlockState
        ? new ExtendedBlockState(block, props,
        ((ExtendedBlockState) mine).getUnlistedProperties().toArray(new IUnlistedProperty<?>[0]))
        : new BlockStateContainer(block, props);
    ImmutableList<IBlockState> ms = mine.getValidStates();
    ImmutableList<IBlockState> rs = ref.getValidStates();
    assertEquals(rs.size(), ms.size());
    assertEquals(ref.toString(), mine.toString());
    assertEquals(new ArrayList<>(ref.getProperties()), new ArrayList<>(mine.getProperties()));
    Map<IBlockState, IBlockState> toMine = mapByPosition(mine, ref);
    for (int i = 0; i < ms.size(); i++) {
      IBlockState m = ms.get(i);
      IBlockState r = rs.get(i);
      assertEquals(r.getProperties(), m.getProperties());
      assertEquals(r.hashCode(), m.hashCode());
      assertEquals(r.toString(), m.toString());
      assertEquals(new ArrayList<>(r.getPropertyKeys()), new ArrayList<>(m.getPropertyKeys()));
      for (IProperty p : props) {
        assertEquals(r.getValue(p), m.getValue(p));
        assertTrue(m.getPropertyKeys().contains(p));
      }
      if (i == 0) {
        PropertyBool absent = PropertyBool.create("zz_absent");
        assertEquals(assertThrows(IllegalArgumentException.class, () -> r.getValue(absent))
                .getMessage(),
            assertThrows(IllegalArgumentException.class, () -> m.getValue(absent)).getMessage());
        assertEquals(r.getPropertyKeys().contains(absent), m.getPropertyKeys().contains(absent));
      }
      for (IProperty p : props) {
        for (Object v : p.getAllowedValues()) {
          assertSame(toMine.get(r.withProperty(p, (Comparable) v)),
              m.withProperty(p, (Comparable) v), m + " " + p.getName() + "=" + v);
        }
        assertSame(toMine.get(r.cycleProperty(p)), m.cycleProperty(p));
      }
      ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> tm = ((BlockStateBase) m).getPropertyValueTable();
      ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> tr = ((BlockStateBase) r).getPropertyValueTable();
      assertEquals(tr.size(), tm.size());
      for (ImmutableTable.Cell<IProperty<?>, Comparable<?>, IBlockState> cell : tr.cellSet()) {
        assertSame(toMine.get(cell.getValue()), tm.get(cell.getRowKey(), cell.getColumnKey()));
      }
      if (m instanceof BlockStateContainer.StateImplementation) {
        assertNull(readTableField((BlockStateContainer.StateImplementation) m));
        assertTrue(readMapField((BlockStateContainer.StateImplementation) m).isEmpty(),
            "a CSM state holds no property map of its own");
      }
    }
  }

  /** The vanilla property map field: the shared empty map in every CSM state. */
  private static Map<?, ?> readMapField(BlockStateContainer.StateImplementation s) {
    try {
      java.lang.reflect.Field f = BlockStateContainer.StateImplementation.class
          .getDeclaredField("properties");
      f.setAccessible(true);
      return (Map<?, ?>) f.get(s);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  /** The vanilla table field itself stays empty: nothing is held per state. */
  private static Object readTableField(BlockStateContainer.StateImplementation s) {
    try {
      java.lang.reflect.Field f = BlockStateContainer.StateImplementation.class
          .getDeclaredField("propertyValueTable");
      f.setAccessible(true);
      return f.get(s);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }
}
