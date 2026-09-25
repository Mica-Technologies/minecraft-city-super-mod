package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.cache.Cache;
import com.micatechnologies.minecraft.csm.codeutils.CsmMemStats.BlockRow;
import com.micatechnologies.minecraft.csm.codeutils.CsmMemStats.PropertyRow;
import com.micatechnologies.minecraft.csm.codeutils.CsmMemStats.Report;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client half of {@code /csm memstats}: reads the model manager's baked registry and attributes
 * every baked model and quad to the block (or item) whose state first reaches it.
 *
 * <p><b>Read only, and careful about it.</b> Forge's OBJ baked models build their quads lazily, on
 * the first {@code getQuads} call. Asking every OBJ model for its quads would build the quads of
 * every OBJ variant nobody has looked at yet and keep them for the rest of the session, so this
 * class never calls {@code getQuads}: it finds quads by reading the fields of the baked models
 * (lists and maps of {@code BakedQuad}, child models), and reports an OBJ model whose quads are not
 * built yet as "unbuilt", with an estimate from its face count.</p>
 *
 * <p><b>What "changes" a property makes.</b> For every property of a CSM block the report pairs
 * each state with the same state at another value of that property and says what differs: nothing
 * (the same model location), the same baked instance, an identical bake (the same quads baked
 * twice), a texture or UV swap (the same positions), a rigid transform (the same quads up to
 * rotation and translation), or geometry. The comparison uses order-independent signatures of a
 * model's quads, so it is exact up to hash collisions.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmMemStatsModels implements CsmMemStats.ModelProbe {

  /** Model and quad signatures, summed over distinct quads so the order does not matter. */
  private static final class Sig {

    long pos;
    long full;
    long rigid;
    long quads;
  }

  /** What one OBJ file costs across all the variants that bake it. */
  private static final class ObjStats {

    final String location;
    int instances;
    int built;
    long builtQuads;
    long faces = -1;
    final Set<String> owners = new LinkedHashSet<>();

    ObjStats(String location) {
      this.location = location;
    }
  }

  private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();
  private final ReferenceOpenHashSet<Object> countedQuads = new ReferenceOpenHashSet<>();
  private final ReferenceOpenHashSet<Object> seenModels = new ReferenceOpenHashSet<>();
  private final IdentityHashMap<Object, Sig> sigCache = new IdentityHashMap<>();
  private final IdentityHashMap<Object, ObjStats> objStats = new IdentityHashMap<>();
  private final Map<String, Integer> modelClasses = new TreeMap<>();
  private final Map<String, Long> quadClasses = new TreeMap<>();
  private final Map<Integer, Long> vertexLengths = new TreeMap<>();
  private final LongArrayList globalQuadHashes = new LongArrayList();
  private final LongArrayList globalVertexHashes = new LongArrayList();
  private boolean globalDedup;

  // Per-owner accumulators, reset by begin().
  private long ownerQuads;
  private long ownerQuadBytes;
  private int ownerNewModels;
  private int ownerSharedModels;
  private int ownerObjBakes;
  private int ownerObjUnbuilt;
  private long ownerObjUnbuiltQuads;
  private String owner = "";
  private final LongArrayList ownerQuadHashes = new LongArrayList();

  private Field objQuadsField;
  private Field objModelField;
  private Field objStateField;
  private Field objTexturesField;
  private Field objLocationField;
  private Field unpackedDataField;

  @Override
  public void probe(Report report) {
    globalDedup = report.dedup;
    Minecraft mc = Minecraft.getMinecraft();
    BlockModelShapes shapes = mc.getBlockRendererDispatcher().getBlockModelShapes();
    ModelManager manager = shapes.getModelManager();
    IRegistry<ModelResourceLocation, IBakedModel> registry =
        ObfuscationReflectionHelper.getPrivateValue(ModelManager.class, manager, "field_174958_a");
    Map<IBlockState, ModelResourceLocation> stateLocations =
        shapes.getBlockStateMapper().putAllStateModelLocations();
    resolveForgeFields();

    Set<ModelResourceLocation> reached = new java.util.HashSet<>();
    for (BlockRow row : report.rows) {
      probeBlock(row, registry, stateLocations, reached);
    }

    // Item models: every "inventory" location, charged to the block of that name if there is one.
    List<String> items = new ArrayList<>();
    items.add("location,owner,quads,quadBytes,sharesStateModel");
    long orphanLocations = 0;
    long orphanQuads = 0;
    long orphanBytes = 0;
    for (ModelResourceLocation key : new ArrayList<>(registry.getKeys())) {
      if (reached.contains(key)) {
        continue;
      }
      IBakedModel model = registry.getObject(key);
      if (model == null) {
        continue;
      }
      begin(key.toString());
      Sig sig = walkTop(model);
      boolean inventory = "inventory".equals(key.getVariant());
      BlockRow row = inventory ? report.byId.get(key.getNamespace() + ":" + key.getPath()) : null;
      boolean shares = false;
      if (row != null) {
        row.itemLocations++;
        row.itemQuads += ownerQuads;
        row.itemQuadBytes += ownerQuadBytes;
        shares = sharesStateModel(row, stateLocations, registry, sig);
        row.itemSharesStateModel |= shares;
      }
      if (inventory) {
        items.add(CsmMemStats.csv(key, row == null ? "item" : row.id, ownerQuads,
            ownerQuadBytes, shares));
      } else {
        orphanLocations++;
        orphanQuads += ownerQuads;
        orphanBytes += ownerQuadBytes;
      }
      flushOwnerHashes();
    }
    report.extraFiles.put("items.csv", items);
    report.extraFiles.put("obj_models.csv", objCsv());

    notes(report, registry, orphanLocations, orphanQuads, orphanBytes);
  }

  // ---------------------------------------------------------------------------------------------
  // Blocks
  // ---------------------------------------------------------------------------------------------

  private void probeBlock(BlockRow row, IRegistry<ModelResourceLocation, IBakedModel> registry,
      Map<IBlockState, ModelResourceLocation> stateLocations, Set<ModelResourceLocation> reached) {
    begin(row.id);
    Map<IBlockState, IBakedModel> modelOf = new IdentityHashMap<>();
    Set<ModelResourceLocation> locations = new java.util.HashSet<>();
    ReferenceOpenHashSet<Object> models = new ReferenceOpenHashSet<>();
    for (IBlockState state : row.block.getBlockState().getValidStates()) {
      ModelResourceLocation mrl = stateLocations.get(state);
      if (mrl == null) {
        row.missingLocations++;
        continue;
      }
      locations.add(mrl);
      IBakedModel model = registry.getObject(mrl);
      if (model == null) {
        row.missingLocations++;
        continue;
      }
      modelOf.put(state, model);
      models.add(model);
    }
    reached.addAll(locations);
    for (Object model : models) {
      walkTop(model);
    }
    row.modelsProbed = true;
    row.locations = locations.size();
    row.models = models.size();
    row.leafModels = ownerNewModels;
    row.sharedLeafModels = ownerSharedModels;
    row.quads = ownerQuads;
    row.quadBytes = ownerQuadBytes;
    row.objBakes = ownerObjBakes;
    row.objUnbuilt = ownerObjUnbuilt;
    row.objUnbuiltQuadsEstimate = ownerObjUnbuiltQuads;
    long dup = duplicatesIn(ownerQuadHashes);
    row.duplicateQuads = dup;
    row.duplicateQuadBytes = ownerQuads == 0 ? 0 : Math.round(ownerQuadBytes * (double) dup
        / ownerQuads);
    flushOwnerHashes();
    if (row.isCsm()) {
      classifyProperties(row, stateLocations, modelOf);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void classifyProperties(BlockRow row,
      Map<IBlockState, ModelResourceLocation> stateLocations,
      Map<IBlockState, IBakedModel> modelOf) {
    Collection<IProperty<?>> props = row.block.getBlockState().getProperties();
    int index = 0;
    for (IProperty<?> prop : props) {
      PropertyRow pr = row.properties.get(index++);
      Collection<?> values = prop.getAllowedValues();
      Object first = values.iterator().next();
      ReferenceOpenHashSet<Object> fixedModels = new ReferenceOpenHashSet<>();
      for (IBlockState state : row.block.getBlockState().getValidStates()) {
        if (!first.equals(state.getValue(prop))) {
          continue;
        }
        IBakedModel baseModel = modelOf.get(state);
        if (baseModel != null) {
          fixedModels.add(baseModel);
        }
        ModelResourceLocation baseLoc = stateLocations.get(state);
        for (Object value : values) {
          if (value.equals(first)) {
            continue;
          }
          IBlockState other = state.withProperty((IProperty) prop, (Comparable) value);
          ModelResourceLocation otherLoc = stateLocations.get(other);
          IBakedModel otherModel = modelOf.get(other);
          if (baseLoc != null && baseLoc.equals(otherLoc)) {
            pr.pairsSameLocation++;
          } else if (baseModel == null || otherModel == null) {
            pr.pairsUnknown++;
          } else if (baseModel == otherModel) {
            pr.pairsSameModel++;
          } else {
            Sig a = sigCache.get(baseModel);
            Sig b = sigCache.get(otherModel);
            if (a == null || b == null) {
              pr.pairsUnknown++;
            } else if (a.full == b.full && a.quads == b.quads) {
              pr.pairsIdentical++;
            } else if (a.pos == b.pos && a.quads == b.quads) {
              pr.pairsTexture++;
            } else if (a.rigid == b.rigid && a.quads == b.quads) {
              pr.pairsTransform++;
            } else {
              pr.pairsGeometry++;
            }
          }
        }
      }
      pr.modelsWithPropertyFixed = fixedModels.size();
    }
  }

  private boolean sharesStateModel(BlockRow row,
      Map<IBlockState, ModelResourceLocation> stateLocations,
      IRegistry<ModelResourceLocation, IBakedModel> registry, Sig itemSig) {
    for (IBlockState state : row.block.getBlockState().getValidStates()) {
      ModelResourceLocation mrl = stateLocations.get(state);
      IBakedModel model = mrl == null ? null : registry.getObject(mrl);
      Sig sig = model == null ? null : sigCache.get(model);
      if (sig != null && sig.full == itemSig.full && sig.quads == itemSig.quads) {
        return true;
      }
    }
    return false;
  }

  // ---------------------------------------------------------------------------------------------
  // Walking models
  // ---------------------------------------------------------------------------------------------

  private void begin(String ownerId) {
    owner = ownerId;
    ownerQuads = 0;
    ownerQuadBytes = 0;
    ownerNewModels = 0;
    ownerSharedModels = 0;
    ownerObjBakes = 0;
    ownerObjUnbuilt = 0;
    ownerObjUnbuiltQuads = 0;
    ownerQuadHashes.clear();
  }

  private void flushOwnerHashes() {
    ownerQuadHashes.clear();
  }

  /**
   * Walks a model reached from a block state or item location, counting what has not been counted
   * yet and returning its signature (memoised).
   */
  private Sig walkTop(Object model) {
    Sig cached = sigCache.get(model);
    if (cached != null) {
      // Everything under it was counted already; still record the reach for "shared" figures.
      ownerSharedModels++;
      return cached;
    }
    Sig sig = new Sig();
    walk(model, sig, new ReferenceOpenHashSet<>(), 0);
    sigCache.put(model, sig);
    return sig;
  }

  private void walk(Object node, Sig sig, ReferenceOpenHashSet<Object> local, int depth) {
    if (node == null || depth > 12 || !local.add(node)) {
      return;
    }
    if (node instanceof BakedQuad) {
      quad((BakedQuad) node, sig);
      return;
    }
    if (node instanceof IBakedModel) {
      if (seenModels.add(node)) {
        ownerNewModels++;
        modelClasses.merge(node.getClass().getName(), 1, Integer::sum);
      } else {
        ownerSharedModels++;
      }
      if (node instanceof net.minecraftforge.client.model.obj.OBJModel.OBJBakedModel) {
        obj(node, sig);
        return;
      }
      Sig known = sigCache.get(node);
      if (known != null && depth > 0) {
        add(sig, known);
        return;
      }
      if (depth > 0) {
        // Memoise child models too, so a part shared by many variants is hashed once.
        Sig child = new Sig();
        scanFields(node, child, local, depth);
        sigCache.put(node, child);
        add(sig, child);
        return;
      }
      scanFields(node, sig, local, depth);
      return;
    }
    if (node instanceof Map) {
      for (Map.Entry<?, ?> e : ((Map<?, ?>) node).entrySet()) {
        walkValue(e.getKey(), sig, local, depth + 1);
        walkValue(e.getValue(), sig, local, depth + 1);
      }
      return;
    }
    if (node instanceof Collection) {
      for (Object o : (Collection<?>) node) {
        walkValue(o, sig, local, depth + 1);
      }
      return;
    }
    if (node instanceof Object[]) {
      for (Object o : (Object[]) node) {
        walkValue(o, sig, local, depth + 1);
      }
      return;
    }
    if (node instanceof Cache) {
      walk(((Cache<?, ?>) node).asMap(), sig, local, depth + 1);
      return;
    }
    // A helper object that belongs to a model class (WeightedBakedModel$WeightedModel): look
    // inside it for models and quads.
    Class<?> enclosing = node.getClass().getEnclosingClass();
    if (enclosing != null && IBakedModel.class.isAssignableFrom(enclosing)) {
      scanFields(node, sig, local, depth);
    }
  }

  private void walkValue(Object value, Sig sig, ReferenceOpenHashSet<Object> local, int depth) {
    if (value == null || value instanceof String || value instanceof Number
        || value instanceof Enum || value instanceof ResourceLocation
        || value instanceof TextureAtlasSprite || value instanceof VertexFormat) {
      return;
    }
    walk(value, sig, local, depth);
  }

  private void scanFields(Object node, Sig sig, ReferenceOpenHashSet<Object> local, int depth) {
    for (Field f : fieldsOf(node.getClass())) {
      Object value;
      try {
        value = f.get(node);
      } catch (Throwable e) {
        continue;
      }
      if (value == null || value instanceof IModel) {
        continue;
      }
      if (value instanceof BakedQuad || value instanceof IBakedModel || value instanceof Map
          || value instanceof Collection || value instanceof Object[] || value instanceof Cache) {
        walk(value, sig, local, depth + 1);
      } else {
        Class<?> enclosing = value.getClass().getEnclosingClass();
        if (enclosing != null && IBakedModel.class.isAssignableFrom(enclosing)) {
          walk(value, sig, local, depth + 1);
        }
      }
    }
  }

  private List<Field> fieldsOf(Class<?> type) {
    List<Field> fields = fieldCache.get(type);
    if (fields != null) {
      return fields;
    }
    fields = new ArrayList<>();
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
          continue;
        }
        try {
          f.setAccessible(true);
          fields.add(f);
        } catch (Throwable ignored) {
          // Skip a field the JVM will not open.
        }
      }
    }
    fieldCache.put(type, fields);
    return fields;
  }

  private static void add(Sig into, Sig from) {
    into.pos += from.pos;
    into.full += from.full;
    into.rigid += from.rigid;
    into.quads += from.quads;
  }

  // ---------------------------------------------------------------------------------------------
  // OBJ
  // ---------------------------------------------------------------------------------------------

  private void resolveForgeFields() {
    try {
      Class<?> baked = OBJModel.OBJBakedModel.class;
      objQuadsField = accessible(baked.getDeclaredField("quads"));
      objModelField = accessible(baked.getDeclaredField("model"));
      objStateField = accessible(baked.getDeclaredField("state"));
      objTexturesField = accessible(baked.getDeclaredField("textures"));
      objLocationField = accessible(OBJModel.class.getDeclaredField("modelLocation"));
      unpackedDataField = accessible(net.minecraftforge.client.model.pipeline.UnpackedBakedQuad
          .class.getDeclaredField("unpackedData"));
    } catch (Throwable ignored) {
      // Leave the fields null; OBJ models are then walked generically.
    }
  }

  private static Field accessible(Field f) {
    f.setAccessible(true);
    return f;
  }

  private void obj(Object baked, Sig sig) {
    ownerObjBakes++;
    try {
      Object model = objModelField.get(baked);
      Object quads = objQuadsField.get(baked);
      Object state = objStateField.get(baked);
      Object textures = objTexturesField.get(baked);
      ObjStats stats = objStats.get(model);
      if (stats == null) {
        stats = new ObjStats(String.valueOf(objLocationField.get(model)));
        objStats.put(model, stats);
      }
      stats.instances++;
      if (stats.owners.size() < 4) {
        stats.owners.add(owner);
      }
      if (stats.faces < 0) {
        stats.faces = faces((OBJModel) model);
      }
      // An opaque signature, the same whether or not the quads are built: the OBJ file, the
      // model state (the transform) and the texture map.
      long objId = System.identityHashCode(model);
      long stateHash = state == null ? 0 : safeHash(state);
      long texHash = textures == null ? 0 : safeHash(textures);
      Sig own = new Sig();
      own.pos = mix(objId * 31 + stateHash);
      own.full = mix(own.pos * 31 + texHash);
      own.rigid = mix(objId * 17 + texHash);
      if (quads instanceof List) {
        stats.built++;
        List<?> list = (List<?>) quads;
        stats.builtQuads += list.size();
        Sig ignored = new Sig();
        for (Object q : list) {
          if (q instanceof BakedQuad) {
            quad((BakedQuad) q, ignored);
          }
        }
        own.quads = list.size();
      } else {
        ownerObjUnbuilt++;
        ownerObjUnbuiltQuads += Math.max(0, stats.faces);
        own.quads = Math.max(0, stats.faces);
      }
      add(sig, own);
    } catch (Throwable e) {
      // Fields missing: nothing more to learn about this one.
    }
  }

  private static long safeHash(Object o) {
    try {
      return o.hashCode();
    } catch (Throwable e) {
      return System.identityHashCode(o);
    }
  }

  private static long faces(OBJModel model) {
    long count = 0;
    try {
      for (OBJModel.Group group : model.getMatLib().getGroups().values()) {
        count += group.getFaces().size();
      }
    } catch (Throwable ignored) {
      // Unknown.
    }
    return count;
  }

  // ---------------------------------------------------------------------------------------------
  // Quads
  // ---------------------------------------------------------------------------------------------

  private void quad(BakedQuad q, Sig sig) {
    int[] data = q.getVertexData();
    VertexFormat format = q.getFormat();
    int stride = format == null ? 7 : Math.max(1, format.getIntegerSize());
    long vHash = hashInts(data);
    long tex = q.getSprite() == null ? 0 : System.identityHashCode(q.getSprite());
    long qHash = mix(vHash ^ mix(tex * 131 + q.getTintIndex() * 7L
        + (q.getFace() == null ? 9 : q.getFace().ordinal()) * 3L + (q.shouldApplyDiffuseLighting()
        ? 1 : 0) + (format == null ? 0 : System.identityHashCode(format)) * 1_000_003L));
    long pHash = 0x51A3L;
    float[] xyz = new float[12];
    for (int v = 0; v < 4 && v * stride + 2 < data.length; v++) {
      for (int k = 0; k < 3; k++) {
        int bits = data[v * stride + k];
        pHash = mix(pHash * 31 + bits);
        xyz[v * 3 + k] = Float.intBitsToFloat(bits);
      }
    }
    long[] edges = new long[6];
    int e = 0;
    for (int a = 0; a < 4; a++) {
      for (int b = a + 1; b < 4; b++) {
        float dx = xyz[a * 3] - xyz[b * 3];
        float dy = xyz[a * 3 + 1] - xyz[b * 3 + 1];
        float dz = xyz[a * 3 + 2] - xyz[b * 3 + 2];
        edges[e++] = Math.round((dx * dx + dy * dy + dz * dz) * 4096.0);
      }
    }
    Arrays.sort(edges);
    long rHash = tex;
    for (long edge : edges) {
      rHash = mix(rHash * 31 + edge);
    }
    sig.pos += mix(pHash);
    sig.full += qHash;
    sig.rigid += mix(rHash);
    sig.quads++;

    if (!countedQuads.add(q)) {
      return;
    }
    long bytes = CsmMemStats.BAKED_QUAD_BYTES + align8(16 + 4L * data.length);
    String cls = q.getClass().getSimpleName();
    if (unpackedDataField != null
        && q instanceof net.minecraftforge.client.model.pipeline.UnpackedBakedQuad) {
      bytes += 900; // the unpacked float[4][elements][4] it also keeps
    }
    quadClasses.merge(cls, 1L, Long::sum);
    vertexLengths.merge(data.length, 1L, Long::sum);
    ownerQuads++;
    ownerQuadBytes += bytes;
    ownerQuadHashes.add(qHash);
    if (globalDedup) {
      globalQuadHashes.add(qHash);
      globalVertexHashes.add(vHash);
    }
  }

  private static long align8(long n) {
    return (n + 7) & ~7L;
  }

  private static long hashInts(int[] data) {
    long h = 0x9E3779B97F4A7C15L ^ data.length;
    for (int v : data) {
      h = mix(h ^ (v & 0xFFFFFFFFL));
    }
    return h;
  }

  /** murmur3's 64-bit finaliser. */
  private static long mix(long z) {
    z = (z ^ (z >>> 33)) * 0xFF51AFD7ED558CCDL;
    z = (z ^ (z >>> 33)) * 0xC4CEB9FE1A85EC53L;
    return z ^ (z >>> 33);
  }

  private static long duplicatesIn(LongArrayList hashes) {
    if (hashes.size() < 2) {
      return 0;
    }
    long[] a = hashes.toLongArray();
    Arrays.sort(a);
    long dup = 0;
    for (int i = 1; i < a.length; i++) {
      if (a[i] == a[i - 1]) {
        dup++;
      }
    }
    return dup;
  }

  // ---------------------------------------------------------------------------------------------
  // Report
  // ---------------------------------------------------------------------------------------------

  private List<String> objCsv() {
    List<ObjStats> all = new ArrayList<>(objStats.values());
    all.sort((a, b) -> Long.compare(b.instances * Math.max(1, b.faces),
        a.instances * Math.max(1, a.faces)));
    List<String> out = new ArrayList<>();
    out.add("objLocation,bakedInstances,builtInstances,facesPerBake,builtQuads,"
        + "quadsIfAllBuilt,estBytesIfAllBuilt,owners");
    for (ObjStats s : all) {
      long ifAll = s.instances * Math.max(0, s.faces);
      out.add(CsmMemStats.csv(s.location, s.instances, s.built, s.faces, s.builtQuads, ifAll,
          ifAll * (CsmMemStats.BAKED_QUAD_BYTES + 128), String.join(" ", s.owners)));
    }
    return out;
  }

  private void notes(Report report, IRegistry<ModelResourceLocation, IBakedModel> registry,
      long orphanLocations, long orphanQuads, long orphanBytes) {
    List<String> n = report.notes;
    n.add(String.format(Locale.ROOT, "Model registry: %d locations, %d distinct baked models"
            + " reached (%d model objects incl. children), %d quads counted",
        registry.getKeys().size(), sigCache.size(), seenModels.size(), countedQuads.size()));
    n.add(String.format(Locale.ROOT, "Locations no block state or block item reaches: %d"
        + " (%d quads, %.1f MB)", orphanLocations, orphanQuads, CsmMemStats.mb(orphanBytes)));
    long objInstances = 0;
    long objBuilt = 0;
    long objIfAll = 0;
    long objBuiltQuads = 0;
    for (ObjStats s : objStats.values()) {
      objInstances += s.instances;
      objBuilt += s.built;
      objBuiltQuads += s.builtQuads;
      objIfAll += s.instances * Math.max(0, s.faces);
    }
    n.add(String.format(Locale.ROOT, "OBJ: %d files, %d baked instances, %d built (%d quads);"
            + " all built would be %d quads (~%.1f MB)", objStats.size(), objInstances,
        objBuilt, objBuiltQuads, objIfAll,
        CsmMemStats.mb(objIfAll * (CsmMemStats.BAKED_QUAD_BYTES + 128))));
    if (globalDedup) {
      long[] q = globalQuadHashes.toLongArray();
      long[] v = globalVertexHashes.toLongArray();
      Arrays.sort(q);
      Arrays.sort(v);
      long distinctQ = distinct(q);
      long distinctV = distinct(v);
      n.add(String.format(Locale.ROOT, "Dedup: %d quads counted; %d distinct quads (%.1f%%"
              + " duplicate), %d distinct vertex arrays (%.1f%% duplicate)", q.length, distinctQ,
          100.0 * (q.length - distinctQ) / Math.max(1, q.length), distinctV,
          100.0 * (v.length - distinctV) / Math.max(1, v.length)));
    }
    n.add("Quad classes: " + quadClasses);
    n.add("Vertex data lengths (ints): " + vertexLengths);
    List<Map.Entry<String, Integer>> classes = new ArrayList<>(modelClasses.entrySet());
    classes.sort((a, b) -> b.getValue() - a.getValue());
    n.add("Baked model classes: " + classes.subList(0, Math.min(15, classes.size())));
    n.add(unbakedCache());
  }

  private static long distinct(long[] sorted) {
    if (sorted.length == 0) {
      return 0;
    }
    long d = 1;
    for (int i = 1; i < sorted.length; i++) {
      if (sorted[i] != sorted[i - 1]) {
        d++;
      }
    }
    return d;
  }

  /** What ModelLoaderRegistry still holds after baking: the unbaked models, kept statically. */
  private static String unbakedCache() {
    try {
      Field f = ModelLoaderRegistry.class.getDeclaredField("cache");
      f.setAccessible(true);
      Map<?, ?> cache = (Map<?, ?>) f.get(null);
      Reference2IntOpenHashMap<Class<?>> byClass = new Reference2IntOpenHashMap<>();
      for (Object v : cache.values()) {
        if (v != null) {
          byClass.addTo(v.getClass(), 1);
        }
      }
      Map<String, Integer> named = new TreeMap<>();
      for (Map.Entry<Class<?>, Integer> e : byClass.entrySet()) {
        named.put(e.getKey().getName().replace("net.minecraftforge.client.model.", ""),
            e.getValue());
      }
      return "ModelLoaderRegistry cache (unbaked models kept after baking): " + cache.size()
          + " entries " + named;
    } catch (Throwable e) {
      return "ModelLoaderRegistry cache: unreadable (" + e + ")";
    }
  }
}
