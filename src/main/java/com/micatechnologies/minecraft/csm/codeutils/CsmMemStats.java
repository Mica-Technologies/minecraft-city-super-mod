package com.micatechnologies.minecraft.csm.codeutils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * The {@code /csm memstats} report: what every block costs in memory, attributed to the block.
 *
 * <p>Two things dominate a client's live heap once CSM is loaded, and both are per block. Every
 * block state carries a table of its neighbour states (one cell for every other value of every
 * property), and every distinct model variant a blockstate names is baked into its own quads. This
 * class measures the first on either side; on the client, {@link ModelProbe} (supplied by the client
 * proxy) adds the second by reading the model manager's registry. Nothing here changes any state:
 * it reads registries and writes a report.</p>
 *
 * <p>The byte figures are estimates, not measurements. The quad figure counts what the quads hold
 * (a {@code BakedQuad} and its vertex {@code int[]}); the state-table figure uses per-state and
 * per-cell costs calibrated against a heap histogram of a real client (see
 * {@link #TABLE_BYTES_PER_STATE} and {@link #TABLE_BYTES_PER_CELL}). Use them to rank, and confirm
 * a fix with a heap histogram taken after a full GC.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class CsmMemStats {

  /**
   * Estimated bytes of the fixed part of a block state's neighbour table (the table object, its
   * iteration-order arrays and its row and column maps). With {@link #TABLE_BYTES_PER_ROW} and
   * {@link #TABLE_BYTES_PER_CELL}, fitted 2026-09-25 to a measurement: dropping the tables of all
   * 290,376 CSM states (1,871,250 rows, 7,148,426 cells) freed 991 MB of live heap.
   */
  public static final double TABLE_BYTES_PER_STATE = 360.0;

  /** Estimated bytes per row of a neighbour table: one property (with 2+ values) of one state. */
  public static final double TABLE_BYTES_PER_ROW = 170.0;

  /**
   * Estimated bytes per neighbour-table cell (one other value of one property of one state): the
   * cell's entries in the row map and its column map.
   */
  public static final double TABLE_BYTES_PER_CELL = 80.0;

  /** Shallow size of a {@code BakedQuad} with compressed oops. */
  public static final int BAKED_QUAD_BYTES = 40;

  /** Adds the client's model figures to the rows. Supplied by the client proxy only. */
  public interface ModelProbe {

    /**
     * Fills the model columns of every row and adds model-level lines to the report.
     *
     * @param report the report being built
     */
    void probe(Report report);
  }

  /** One property of one block, with how its values change the block's models. */
  public static final class PropertyRow {

    public final String name;
    public final int values;
    /** Distinct values the property takes over the states {@code getStateFromMeta} returns. */
    public int valuesInMeta;
    /** Distinct models when this property is held at one value, over the block's states. */
    public int modelsWithPropertyFixed;
    /** Pairs (a state and the same state with only this property changed) by what changed. */
    public int pairsSameLocation;
    public int pairsSameModel;
    public int pairsIdentical;
    public int pairsTexture;
    public int pairsTransform;
    public int pairsGeometry;
    public int pairsUnknown;

    PropertyRow(String name, int values) {
      this.name = name;
      this.values = values;
    }

    /** @return true if the property is not stored in metadata (actual state only) */
    public boolean actualStateOnly() {
      return values > 1 && valuesInMeta <= 1;
    }

    /** @return the dominant kind of change, for the report */
    public String kind() {
      int[] counts = {pairsSameLocation, pairsSameModel, pairsIdentical, pairsTexture,
          pairsTransform, pairsGeometry, pairsUnknown};
      String[] names = {"no-model-change", "same-model", "identical-bake", "texture-only",
          "transform-only", "geometry", "unknown"};
      int best = -1;
      for (int i = 0; i < counts.length; i++) {
        if (counts[i] > 0 && (best < 0 || counts[i] > counts[best])) {
          best = i;
        }
      }
      return best < 0 ? "" : names[best];
    }
  }

  /** Everything the report knows about one block. */
  public static final class BlockRow {

    public final Block block;
    public final String id;
    public final String namespace;
    public String module = "";
    public String tab = "";
    public String blockClass = "";
    public String baseClass = "";
    public int states;
    public int metaStates;
    public long neighbourCells;
    public long neighbourRows;
    public int actualOnlyProperties;
    public final List<PropertyRow> properties = new ArrayList<>();

    // Model columns: filled on the client by the probe.
    public boolean modelsProbed;
    public int locations;
    public int missingLocations;
    public int models;
    public int leafModels;
    public int sharedLeafModels;
    public long quads;
    public long quadBytes;
    public long duplicateQuads;
    public long duplicateQuadBytes;
    public int objBakes;
    public int objUnbuilt;
    public long objUnbuiltQuadsEstimate;
    public int itemLocations;
    public long itemQuads;
    public long itemQuadBytes;
    public boolean itemSharesStateModel;

    BlockRow(Block block) {
      this.block = block;
      this.id = String.valueOf(block.getRegistryName());
      this.namespace = block.getRegistryName() == null ? "?"
          : block.getRegistryName().getNamespace();
    }

    /** @return estimated bytes of this block's state objects and neighbour tables */
    public long tableBytes() {
      return Math.round(states * TABLE_BYTES_PER_STATE + neighbourRows * TABLE_BYTES_PER_ROW
          + neighbourCells * TABLE_BYTES_PER_CELL);
    }

    /** @return estimated bytes in total: state tables plus block and item quads */
    public long totalBytes() {
      return tableBytes() + quadBytes + itemQuadBytes;
    }

    /** @return true if the block is one of CSM's */
    public boolean isCsm() {
      return "csm".equals(namespace);
    }
  }

  /** The whole report: rows plus free-form summary lines the probe adds. */
  public static final class Report {

    public final List<BlockRow> rows = new ArrayList<>();
    public final Map<String, BlockRow> byId = new HashMap<>();
    /** Extra summary lines (model manager figures), written into summary.txt. */
    public final List<String> notes = new ArrayList<>();
    /** Extra files the probe wants written, by file name. */
    public final Map<String, List<String>> extraFiles = new LinkedHashMap<>();
    public boolean dedup;
  }

  private CsmMemStats() {
  }

  /**
   * Builds the report and, if asked, writes it under {@code outRoot/<timestamp>/}.
   *
   * @param outRoot the folder to write under (created as needed)
   * @param write   true to write the files
   * @param dedup   true to also hash every quad for the duplicate figures (slower, more memory)
   * @param probe   the client's model probe, or null on a server
   *
   * @return chat lines summarising the result
   */
  public static List<String> run(File outRoot, boolean write, boolean dedup,
      @Nullable ModelProbe probe) {
    long start = System.nanoTime();
    Report report = new Report();
    report.dedup = dedup;
    Map<File, String> sources = modSources();
    for (Block block : Block.REGISTRY) {
      if (block.getRegistryName() == null) {
        continue;
      }
      BlockRow row = new BlockRow(block);
      describeBlock(row, sources);
      report.rows.add(row);
      report.byId.put(row.id, row);
    }
    if (probe != null) {
      probe.probe(report);
    }
    long millis = (System.nanoTime() - start) / 1_000_000L;

    List<String> summary = summarise(report);
    List<String> chat = new ArrayList<>();
    chat.add(String.format(Locale.ROOT, "memstats: %d blocks in %d ms%s", report.rows.size(),
        millis, probe == null ? " (states only; models are client side)" : ""));
    for (int i = 0; i < Math.min(8, summary.size()); i++) {
      chat.add(summary.get(i));
    }
    if (write) {
      String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
      File dir = new File(outRoot, stamp);
      try {
        writeAll(dir, report, summary);
        chat.add("Report written to " + dir.getAbsolutePath());
      } catch (IOException e) {
        chat.add("Could not write the report: " + e);
      }
    } else {
      chat.add("Use /csm memstats dump to write the full report.");
    }
    return chat;
  }

  // ---------------------------------------------------------------------------------------------
  // States (both sides)
  // ---------------------------------------------------------------------------------------------

  private static void describeBlock(BlockRow row, Map<File, String> sources) {
    Block block = row.block;
    row.blockClass = block.getClass().getName();
    row.baseClass = baseClassOf(block.getClass());
    row.module = moduleOf(block, sources);
    CreativeTabs tab = block.getCreativeTab();
    row.tab = tab == null ? "" : tabLabel(tab);

    Collection<IBlockState> valid = block.getBlockState().getValidStates();
    row.states = valid.size();
    Collection<IProperty<?>> props = block.getBlockState().getProperties();
    long cellsPerState = 0;
    long rowsPerState = 0;
    for (IProperty<?> prop : props) {
      int values = prop.getAllowedValues().size();
      cellsPerState += values - 1;
      rowsPerState += values > 1 ? 1 : 0;
      row.properties.add(new PropertyRow(prop.getName(), values));
    }
    row.neighbourCells = cellsPerState * row.states;
    row.neighbourRows = rowsPerState * row.states;

    Set<IBlockState> metaStates = new HashSet<>();
    for (int meta = 0; meta < 16; meta++) {
      try {
        metaStates.add(block.getStateFromMeta(meta));
      } catch (Throwable ignored) {
        // Some blocks throw on metadata they never store; that metadata is simply not a state.
      }
    }
    row.metaStates = metaStates.size();
    int index = 0;
    for (IProperty<?> prop : props) {
      Set<Object> seen = new HashSet<>();
      for (IBlockState state : metaStates) {
        if (state != null && state.getPropertyKeys().contains(prop)) {
          seen.add(state.getValue(prop));
        }
      }
      PropertyRow pr = row.properties.get(index++);
      pr.valuesInMeta = seen.size();
      if (pr.actualStateOnly()) {
        row.actualOnlyProperties++;
      }
    }
  }

  /** @return a tab's label; {@code getTabLabel} is client only, so the field is read directly */
  private static String tabLabel(CreativeTabs tab) {
    try {
      return String.valueOf(ObfuscationReflectionHelper.getPrivateValue(CreativeTabs.class, tab,
          "field_78034_o"));
    } catch (Throwable e) {
      return "?";
    }
  }

  /** @return the first CSM base class ({@code codeutils}) or vanilla/Forge class above a block */
  static String baseClassOf(Class<?> type) {
    Class<?> c = type.getSuperclass();
    while (c != null && c != Object.class) {
      String name = c.getName();
      if (name.startsWith("com.micatechnologies.minecraft.csm.codeutils.")
          || name.startsWith("net.minecraft.") || name.startsWith("net.minecraftforge.")) {
        return c.getSimpleName();
      }
      c = c.getSuperclass();
    }
    return type.getSimpleName();
  }

  private static Map<File, String> modSources() {
    Map<File, String> map = new HashMap<>();
    for (ModContainer mod : Loader.instance().getActiveModList()) {
      File source = mod.getSource();
      if (source != null) {
        map.put(canonical(source), mod.getModId());
      }
    }
    return map;
  }

  private static final Map<Class<?>, String> MODULE_CACHE = new HashMap<>();

  private static String moduleOf(Block block, Map<File, String> sources) {
    Class<?> type = block.getClass();
    String cached = MODULE_CACHE.get(type);
    if (cached != null) {
      return cached;
    }
    String module = block.getRegistryName().getNamespace();
    try {
      // The launch class loader gives a jar's classes a "jar:file:...!/path/X.class" location and
      // a folder's classes the class file itself, so strip to the jar or walk up to the folder.
      URL url = type.getProtectionDomain().getCodeSource().getLocation();
      String spec = url.toString();
      if (spec.startsWith("jar:")) {
        int bang = spec.indexOf("!/");
        spec = spec.substring(4, bang < 0 ? spec.length() : bang);
      }
      File file = canonical(new File(new URL(spec).toURI()));
      String id = null;
      for (File f = file; f != null && id == null; f = f.getParentFile()) {
        id = sources.get(f);
      }
      if (id != null) {
        module = id;
      } else if ("csm".equals(module)) {
        module = "csm?" + file.getName();
      }
    } catch (Throwable ignored) {
      // Keep the namespace.
    }
    MODULE_CACHE.put(type, module);
    return module;
  }

  private static File canonical(File file) {
    try {
      return file.getCanonicalFile();
    } catch (IOException e) {
      return file.getAbsoluteFile();
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Summary and files
  // ---------------------------------------------------------------------------------------------

  private static final class Totals {

    long blocks;
    long states;
    long cells;
    long tableBytes;
    long models;
    long quads;
    long quadBytes;
    long duplicateQuads;
    long duplicateQuadBytes;
    long itemQuadBytes;
    long objBakes;
    long objUnbuilt;
    long objUnbuiltQuads;

    void add(BlockRow r) {
      blocks++;
      states += r.states;
      cells += r.neighbourCells;
      tableBytes += r.tableBytes();
      models += r.models;
      quads += r.quads;
      quadBytes += r.quadBytes;
      duplicateQuads += r.duplicateQuads;
      duplicateQuadBytes += r.duplicateQuadBytes;
      itemQuadBytes += r.itemQuadBytes;
      objBakes += r.objBakes;
      objUnbuilt += r.objUnbuilt;
      objUnbuiltQuads += r.objUnbuiltQuadsEstimate;
    }

    long total() {
      return tableBytes + quadBytes + itemQuadBytes;
    }

    String line(String label) {
      return String.format(Locale.ROOT,
          "%-22s blocks %6d  states %8d  cells %9d  tables ~%6.1f MB  models %7d  quads %9d"
              + "  quadMB %7.1f  dupQuads %8d (%5.1f MB)  itemMB %5.1f  obj %6d (%d unbuilt)"
              + "  total ~%7.1f MB",
          label, blocks, states, cells, mb(tableBytes), models, quads, mb(quadBytes),
          duplicateQuads, mb(duplicateQuadBytes), mb(itemQuadBytes), objBakes, objUnbuilt,
          mb(total()));
    }
  }

  static double mb(long bytes) {
    return bytes / (1024.0 * 1024.0);
  }

  private static List<String> summarise(Report report) {
    Totals csm = new Totals();
    Totals vanilla = new Totals();
    Totals other = new Totals();
    Map<String, Totals> byModule = new TreeMap<>();
    for (BlockRow r : report.rows) {
      if (r.isCsm()) {
        csm.add(r);
        byModule.computeIfAbsent(r.module, k -> new Totals()).add(r);
      } else if ("minecraft".equals(r.namespace)) {
        vanilla.add(r);
      } else {
        other.add(r);
      }
    }
    List<String> lines = new ArrayList<>();
    lines.add(csm.line("CSM"));
    lines.add(vanilla.line("minecraft"));
    lines.add(other.line("other mods"));
    Runtime rt = Runtime.getRuntime();
    lines.add(String.format(Locale.ROOT, "heap used %d MB of %d MB (not after a GC)",
        (rt.totalMemory() - rt.freeMemory()) >> 20, rt.maxMemory() >> 20));
    lines.add("");
    lines.add("Per module:");
    for (Map.Entry<String, Totals> e : byModule.entrySet()) {
      lines.add("  " + e.getValue().line(e.getKey()));
    }
    lines.add("");
    lines.addAll(report.notes);
    return lines;
  }

  private static void writeAll(File dir, Report report, List<String> summary)
      throws IOException {
    if (!dir.isDirectory() && !dir.mkdirs()) {
      throw new IOException("cannot create " + dir);
    }
    List<String> sum = new ArrayList<>(summary);
    sum.add("");
    sum.add(String.format(Locale.ROOT, "Estimate constants: %.0f bytes/state + %.0f bytes/row +"
            + " %.0f bytes/cell; %d bytes per BakedQuad plus its int[]", TABLE_BYTES_PER_STATE,
        TABLE_BYTES_PER_ROW, TABLE_BYTES_PER_CELL, BAKED_QUAD_BYTES));
    write(new File(dir, "summary.txt"), sum);

    List<String> blocks = new ArrayList<>();
    blocks.add("id,module,tab,baseClass,blockClass,states,metaStates,properties,"
        + "actualOnlyProperties,neighbourRows,neighbourCells,tableBytes,locations,missingLocations,models,"
        + "leafModels,sharedLeafModels,quads,quadBytes,duplicateQuads,duplicateQuadBytes,objBakes,"
        + "objUnbuilt,objUnbuiltQuadsEstimate,itemLocations,itemQuads,itemQuadBytes,"
        + "itemSharesStateModel,totalBytes");
    for (BlockRow r : report.rows) {
      StringBuilder props = new StringBuilder();
      for (PropertyRow p : r.properties) {
        if (props.length() > 0) {
          props.append(' ');
        }
        props.append(p.name).append('=').append(p.values);
        if (p.actualStateOnly()) {
          props.append('*');
        }
      }
      blocks.add(csv(r.id, r.module, r.tab, r.baseClass, r.blockClass, r.states, r.metaStates,
          props, r.actualOnlyProperties, r.neighbourRows, r.neighbourCells, r.tableBytes(), r.locations,
          r.missingLocations, r.models, r.leafModels, r.sharedLeafModels, r.quads, r.quadBytes,
          r.duplicateQuads, r.duplicateQuadBytes, r.objBakes, r.objUnbuilt,
          r.objUnbuiltQuadsEstimate, r.itemLocations, r.itemQuads, r.itemQuadBytes,
          r.itemSharesStateModel, r.totalBytes()));
    }
    write(new File(dir, "blocks.csv"), blocks);

    List<String> props = new ArrayList<>();
    props.add("id,module,property,values,valuesInMeta,actualStateOnly,states,models,"
        + "modelsWithPropertyFixed,kind,pairsNoModelChange,pairsSameModel,pairsIdenticalBake,"
        + "pairsTextureOnly,pairsTransformOnly,pairsGeometry,pairsUnknown");
    for (BlockRow r : report.rows) {
      if (!r.isCsm()) {
        continue;
      }
      for (PropertyRow p : r.properties) {
        props.add(csv(r.id, r.module, p.name, p.values, p.valuesInMeta, p.actualStateOnly(),
            r.states, r.models, p.modelsWithPropertyFixed, p.kind(), p.pairsSameLocation,
            p.pairsSameModel, p.pairsIdentical, p.pairsTexture, p.pairsTransform,
            p.pairsGeometry, p.pairsUnknown));
      }
    }
    write(new File(dir, "properties.csv"), props);

    write(new File(dir, "modules.csv"), groupCsv(report, true));
    write(new File(dir, "baseclasses.csv"), groupCsv(report, false));
    for (Map.Entry<String, List<String>> e : report.extraFiles.entrySet()) {
      write(new File(dir, e.getKey()), e.getValue());
    }
  }

  private static List<String> groupCsv(Report report, boolean byModule) {
    Map<String, Totals> groups = new TreeMap<>();
    for (BlockRow r : report.rows) {
      String key = byModule ? (r.isCsm() ? r.module : "[" + r.namespace + "]")
          : (r.isCsm() ? r.baseClass : "[" + r.namespace + "] " + r.baseClass);
      groups.computeIfAbsent(key, k -> new Totals()).add(r);
    }
    List<Map.Entry<String, Totals>> sorted = new ArrayList<>(groups.entrySet());
    sorted.sort(Comparator.comparingLong((Map.Entry<String, Totals> e) -> e.getValue().total())
        .reversed());
    List<String> out = new ArrayList<>();
    out.add((byModule ? "module" : "baseClass") + ",blocks,states,neighbourCells,tableBytes,"
        + "models,quads,quadBytes,duplicateQuads,duplicateQuadBytes,itemQuadBytes,objBakes,"
        + "objUnbuilt,objUnbuiltQuadsEstimate,totalBytes");
    for (Map.Entry<String, Totals> e : sorted) {
      Totals t = e.getValue();
      out.add(csv(e.getKey(), t.blocks, t.states, t.cells, t.tableBytes, t.models, t.quads,
          t.quadBytes, t.duplicateQuads, t.duplicateQuadBytes, t.itemQuadBytes, t.objBakes,
          t.objUnbuilt, t.objUnbuiltQuads, t.total()));
    }
    return out;
  }

  /**
   * Joins values into one CSV line, quoting any that need it.
   *
   * @param values the cells
   *
   * @return the line
   */
  public static String csv(Object... values) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      String s = String.valueOf(values[i]);
      if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
        sb.append('"').append(s.replace("\"", "\"\"")).append('"');
      } else {
        sb.append(s);
      }
    }
    return sb.toString();
  }

  private static void write(File file, List<String> lines) throws IOException {
    try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
        Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
      for (String line : lines) {
        out.println(line);
      }
    }
  }
}
