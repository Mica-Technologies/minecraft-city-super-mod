package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.CsmConstants;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Makes CSM's baked models share their identical quads, once baking is done.
 *
 * <p>Forge bakes one model per blockstate variant, and every variant that names a submodel bakes
 * that submodel again, even with the same transform and textures. So the same quad exists many
 * times over: across all modules about 7 million baked quads hold fewer than 800 thousand
 * distinct ones. Each copy is a {@link BakedQuad} and its own 28-int vertex array, about 170
 * bytes, and together the copies were about a gigabyte of the heap.</p>
 *
 * <p>After the bake ({@link ModelBakeEvent}, at the lowest priority so every other handler has
 * already wrapped or replaced what it wanted), this walks every baked model under a {@code csm:}
 * location and, in each {@link SimpleBakedModel}'s quad lists, replaces every quad with the first
 * quad seen with the same content: the same vertex data, tint index, face, sprite, diffuse
 * lighting flag and vertex format. The lists are the builder's {@code ArrayList}s, so they are
 * edited in place and no model is replaced. Nothing on screen can change: the quads a model hands
 * out are equal, field for field, to the ones it held before.</p>
 *
 * <p>What it leaves alone:</p>
 * <ul>
 *   <li>Forge's OBJ models ({@link OBJModel.OBJBakedModel}). They build their quads lazily, on
 *   the first {@code getQuads}, and are not touched.</li>
 *   <li>Any quad class other than {@link BakedQuad} itself (Forge's {@code UnpackedBakedQuad},
 *   the retextured quads), and any list that is not an {@code ArrayList}.</li>
 *   <li>CSM's own baked model classes (doors, tree logs and leaves): they build and cache their
 *   quads on demand, so the walk does not look inside them.</li>
 * </ul>
 *
 * <p><b>The rule this makes:</b> a baked quad is shared, so never write into the array
 * {@link BakedQuad#getVertexData()} returns. Nothing in CSM, Forge or vanilla does; code that
 * needs changed vertices builds a new quad. Code that compares quads by identity (the ceiling
 * fan's blades are the quads its running model does not have) keeps working, since two quads
 * that are now one instance were equal in every field before.</p>
 *
 * <p>The same pass points every <em>empty</em> quad list and item override list at one shared
 * empty list. A {@link SimpleBakedModel} has seven quad lists (one per face and the general one)
 * and most are empty, and every baked model has an override list that almost never has an
 * override, each its own {@code ArrayList}: 1.2 million empty lists, about 27 MB. <b>So never
 * add to a list a baked model hands out</b>, which was already wrong, since the list is the
 * model's own.</p>
 *
 * <p><b>Other mods' models</b>, with the {@code performance.shareOtherModsQuads} switch on (off
 * by default): the same walk and the same equality over every other namespace's models, each
 * namespace with its own map, so a mod's quads are only ever replaced by an equal quad of the
 * same mod's. Only {@link SimpleBakedModel} lists (that exact class, as for CSM's) and plain
 * {@link BakedQuad}s are touched; any baked model class that is not Minecraft's or Forge's own
 * is a custom implementation that may build, cache or change its quads its own way, so it is
 * skipped, and so are OBJ models. Their empty lists are left as they are: another mod may still
 * add to a list of its own after the bake, which CSM's empty-list sharing would make throw. It is
 * a switch, not the default, because the saving depends on a rule CSM cannot check for other
 * mods: that nothing writes into a quad's vertex data once it is baked. Some mods bake the same
 * quad many times over (one model set in the dev client and the Alto pack holds about 1.5
 * million duplicates, some 240 MB).</p>
 *
 * <p><b>Turned on in game</b> ({@code /csmclient quadsharing on}, {@link #shareOtherModsNow()}),
 * the other mods' pass runs at once over the live models, on the client thread, while the chunk
 * builder threads may be reading the same lists. That is safe without stopping them, because the
 * other mods' pass only ever does one thing to a list: {@code ArrayList.set} at an index the list
 * already has. That is not a structural change (no resize, no {@code modCount} change, so no
 * iterator throws); a reference write is atomic, so a reader sees the old quad or the new one,
 * and the two are equal in every field; and every field of a {@link BakedQuad} is final, so the
 * shared quad is seen whole on every thread (and was built at the bake, before the builders were
 * handed any work). It never trims a list, never swaps a list for the shared empty one and never
 * writes a model's field: those are CSM-only, done at the bake. Turning the switch off cannot
 * un-share, and need not, since a shared quad is equal to the one it replaced: the next launch or
 * resource reload bakes the models afresh and leaves other mods' quads alone.</p>
 *
 * <p>The dedup maps live only for the pass. A resource reload bakes new models and posts the
 * event again, so the pass reruns on them.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmQuadSharing {

  /** Content equality over everything a {@link BakedQuad} holds. */
  private static final Hash.Strategy<BakedQuad> CONTENT = new Hash.Strategy<BakedQuad>() {
    @Override
    public int hashCode(BakedQuad q) {
      if (q == null) {
        return 0;
      }
      int h = Arrays.hashCode(q.getVertexData());
      h = h * 31 + q.getTintIndex();
      h = h * 31 + (q.getFace() == null ? -1 : q.getFace().ordinal());
      h = h * 31 + System.identityHashCode(q.getSprite());
      h = h * 31 + (q.shouldApplyDiffuseLighting() ? 1 : 0);
      return h;
    }

    @Override
    public boolean equals(BakedQuad a, BakedQuad b) {
      if (a == b) {
        return true;
      }
      if (a == null || b == null) {
        return false;
      }
      return a.getTintIndex() == b.getTintIndex()
          && a.getFace() == b.getFace()
          && a.getSprite() == b.getSprite()
          && a.shouldApplyDiffuseLighting() == b.shouldApplyDiffuseLighting()
          && Objects.equals(a.getFormat(), b.getFormat())
          && Arrays.equals(a.getVertexData(), b.getVertexData());
    }
  };

  /** The one empty list every empty quad list and override list is pointed at. */
  private static final List<Object> EMPTY = Collections.emptyList();

  /** About what a {@link BakedQuad} costs besides its vertex array: header and six fields. */
  private static final int QUAD_OBJECT_BYTES = 40;

  /** The last other mods' pass this session, at a bake or from the command; null if none. */
  private static volatile OtherModsPass lastOtherModsPass;

  /** Whether the models baked last have had the other mods' pass, at the bake or since. */
  private static volatile boolean otherModsShared;

  /** What one pass over other mods' models did. */
  public static final class OtherModsPass {

    private final boolean live;
    private final long quads;
    private final int models;
    private final int namespaces;
    private final long replaced;
    private final long distinct;
    private final long skippedModels;
    private final String most;
    private final long mostReplaced;
    private final long bytes;
    private final long millis;

    OtherModsPass(boolean live, long quads, int models, int namespaces, long replaced,
        long distinct, long skippedModels, String most, long mostReplaced, long bytes,
        long millis) {
      this.live = live;
      this.quads = quads;
      this.models = models;
      this.namespaces = namespaces;
      this.replaced = replaced;
      this.distinct = distinct;
      this.skippedModels = skippedModels;
      this.most = most;
      this.mostReplaced = mostReplaced;
      this.bytes = bytes;
      this.millis = millis;
    }

    /** @return true if it ran from the command over live models, false if at a bake */
    public boolean isLive() {
      return live;
    }

    /** @return the quads looked at */
    public long getQuads() {
      return quads;
    }

    /** @return the models walked */
    public int getModels() {
      return models;
    }

    /** @return the namespaces (mods) walked */
    public int getNamespaces() {
      return namespaces;
    }

    /** @return the quads replaced by an equal quad of the same mod's */
    public long getReplaced() {
      return replaced;
    }

    /** @return the distinct quads kept */
    public long getDistinct() {
      return distinct;
    }

    /** @return the baked model classes skipped as custom implementations */
    public long getSkippedModels() {
      return skippedModels;
    }

    /** @return the namespace with the most quads replaced, or null if none were */
    @Nullable
    public String getMost() {
      return most;
    }

    /** @return how many quads were replaced in {@link #getMost()} */
    public long getMostReplaced() {
      return mostReplaced;
    }

    /**
     * About how much heap the replaced quads held: each one's vertex array and object. An upper
     * bound, since a replaced quad something else still holds is not freed.
     *
     * @return bytes
     */
    public long getBytes() {
      return bytes;
    }

    /** @return how long the pass took, in milliseconds */
    public long getMillis() {
      return millis;
    }
  }

  private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();
  private final Map<Class<?>, Boolean> modelHelpers = new HashMap<>();
  private Field generalQuadsField;
  private Field faceQuadsField;
  private Field overridesField;
  private long emptied;
  private Object2ObjectOpenCustomHashMap<BakedQuad, BakedQuad> canonical;
  private ReferenceOpenHashSet<Object> visited;
  /** Whether the models being walked are CSM's own (empty lists shared too) or another mod's. */
  private boolean ownModels;
  private long seen;
  private long replaced;
  private long skippedModels;
  private long replacedBytes;

  /** @return the last other mods' pass this session, or null if none has run */
  @Nullable
  public static OtherModsPass lastOtherModsPass() {
    return lastOtherModsPass;
  }

  /** @return whether the models baked last have had the other mods' pass */
  public static boolean isOtherModsShared() {
    return otherModsShared;
  }

  /**
   * Runs the other mods' pass now over the live baked models, as {@code /csmclient quadsharing
   * on} does. Call it on the client thread, so it cannot overlap a resource reload; the chunk
   * builder threads may keep running (see the class comment for why that is safe).
   *
   * @return what it did; null if it could not reach the model registry
   */
  @Nullable
  public static OtherModsPass shareOtherModsNow() {
    IRegistry<ModelResourceLocation, IBakedModel> registry;
    try {
      ModelManager manager =
          Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()
              .getModelManager();
      registry = ObfuscationReflectionHelper.getPrivateValue(ModelManager.class, manager,
          "field_174958_a");
    } catch (Throwable t) {
      Csm.getLogger().warn("Could not reach the baked model registry to share quads", t);
      return null;
    }
    if (registry == null) {
      return null;
    }
    OtherModsPass pass;
    try {
      pass = new CsmQuadSharing().runOtherMods(registry, true);
    } catch (Throwable t) {
      // Quads already replaced are equal to the ones they replaced.
      Csm.getLogger().warn("Sharing other mods' baked quads stopped early; models are unaffected",
          t);
      return null;
    }
    log(pass);
    lastOtherModsPass = pass;
    otherModsShared = true;
    return pass;
  }

  /**
   * The other mods' pass over a registry on its own, for {@link #shareOtherModsNow()} and tests.
   *
   * @param registry the baked models
   * @param live     whether the models are already in use
   *
   * @return what it did
   */
  OtherModsPass runOtherMods(IRegistry<ModelResourceLocation, IBakedModel> registry,
      boolean live) {
    visited = new ReferenceOpenHashSet<>(1 << 16);
    try {
      return shareOtherMods(registry, live);
    } finally {
      canonical = null;
      visited = null;
      fieldCache.clear();
      modelHelpers.clear();
    }
  }

  /**
   * Runs the pass on the freshly baked registry.
   *
   * @param event the bake event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onModelBake(ModelBakeEvent event) {
    long start = System.nanoTime();
    otherModsShared = false;
    canonical = new Object2ObjectOpenCustomHashMap<>(1 << 20, CONTENT);
    visited = new ReferenceOpenHashSet<>(1 << 19);
    seen = 0;
    replaced = 0;
    skippedModels = 0;
    emptied = 0;
    try {
      generalQuadsField = ObfuscationReflectionHelper.findField(SimpleBakedModel.class,
          "field_177563_a");
      faceQuadsField = ObfuscationReflectionHelper.findField(SimpleBakedModel.class,
          "field_177561_b");
      overridesField = ObfuscationReflectionHelper.findField(ItemOverrideList.class,
          "field_188023_b");
      IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
      ownModels = true;
      for (ModelResourceLocation key : registry.getKeys()) {
        if (CsmConstants.MOD_NAMESPACE.equals(key.getNamespace())) {
          visit(registry.getObject(key), 0);
        }
      }
      Csm.getLogger().info(
          "Shared identical baked quads: {} quads in {} models, {} replaced by an equal quad, "
              + "{} distinct kept ({} model classes skipped); {} empty lists shared, {} ms",
          seen, visited.size(), replaced, canonical.size(), skippedModels, emptied,
          (System.nanoTime() - start) / 1_000_000L);
      if (CsmConfig.isShareOtherModsQuads()) {
        OtherModsPass pass = shareOtherMods(registry, false);
        log(pass);
        lastOtherModsPass = pass;
        otherModsShared = true;
      }
    } catch (Throwable t) {
      // Sharing is only a saving; a model class this does not understand must never stop the
      // game loading. Quads already replaced are equal to the ones they replaced.
      Csm.getLogger().warn("Baked quad sharing stopped early; models are unaffected", t);
    } finally {
      canonical = null;
      visited = null;
      fieldCache.clear();
      modelHelpers.clear();
    }
  }

  /**
   * The same pass over every other namespace's models, each namespace with a map of its own,
   * leaving their lists where they are ({@code performance.shareOtherModsQuads}).
   */
  private OtherModsPass shareOtherMods(IRegistry<ModelResourceLocation, IBakedModel> registry,
      boolean live) {
    long start = System.nanoTime();
    replacedBytes = 0;
    Map<String, List<ModelResourceLocation>> byNamespace = new HashMap<>();
    for (ModelResourceLocation key : registry.getKeys()) {
      if (!CsmConstants.MOD_NAMESPACE.equals(key.getNamespace())) {
        byNamespace.computeIfAbsent(key.getNamespace(), n -> new ArrayList<>()).add(key);
      }
    }
    ownModels = false;
    long seenBefore = seen;
    long replacedBefore = replaced;
    long skippedBefore = skippedModels;
    int modelsBefore = visited.size();
    long distinct = 0;
    String most = null;
    long mostReplaced = 0;
    for (Map.Entry<String, List<ModelResourceLocation>> e : byNamespace.entrySet()) {
      canonical = new Object2ObjectOpenCustomHashMap<>(CONTENT);
      long replacedHere = replaced;
      for (ModelResourceLocation key : e.getValue()) {
        visit(registry.getObject(key), 0);
      }
      distinct += canonical.size();
      if (replaced - replacedHere > mostReplaced) {
        mostReplaced = replaced - replacedHere;
        most = e.getKey();
      }
    }
    return new OtherModsPass(live, seen - seenBefore, visited.size() - modelsBefore,
        byNamespace.size(), replaced - replacedBefore, distinct, skippedModels - skippedBefore,
        most, mostReplaced, replacedBytes, (System.nanoTime() - start) / 1_000_000L);
  }

  private static void log(OtherModsPass p) {
    Csm.getLogger().info(
        "Shared other mods' identical baked quads (performance.shareOtherModsQuads{}): {} quads "
            + "in {} models of {} namespaces, {} replaced by an equal quad of the same mod's "
            + "(about {} MB), {} distinct kept ({} model classes skipped; most replaced: {}, {}), "
            + "{} ms",
        p.isLive() ? ", live" : "", p.getQuads(), p.getModels(), p.getNamespaces(),
        p.getReplaced(), Math.round(p.getBytes() / (1024.0 * 1024.0)), p.getDistinct(), p.getSkippedModels(),
        p.getMost(), p.getMostReplaced(), p.getMillis());
  }

  private void visit(Object model, int depth) {
    if (!(model instanceof IBakedModel) || depth > 16 || !visited.add(model)) {
      return;
    }
    if (model instanceof OBJModel.OBJBakedModel) {
      return;
    }
    Class<?> type = model.getClass();
    if (type == SimpleBakedModel.class) {
      share((SimpleBakedModel) model);
      return;
    }
    String name = type.getName();
    if (!name.startsWith("net.minecraft.") && !name.startsWith("net.minecraftforge.")) {
      // CSM's own and other mods' baked models build and cache quads their own way.
      skippedModels++;
      return;
    }
    // A wrapper or container (Forge's perspective wrapper, MultiModel.Baked, weighted and
    // multipart models): look for the models it holds.
    for (Field f : fieldsOf(type)) {
      Object value;
      try {
        value = f.get(model);
      } catch (Throwable e) {
        continue;
      }
      visitValue(value, depth + 1);
    }
  }

  private void visitValue(Object value, int depth) {
    if (value == null || depth > 16) {
      return;
    }
    if (value instanceof IBakedModel) {
      visit(value, depth);
    } else if (value instanceof ItemOverrideList) {
      if (ownModels) {
        shareOverrides((ItemOverrideList) value);
      }
    } else if (value instanceof Map) {
      for (Object v : ((Map<?, ?>) value).values()) {
        visitValue(v, depth + 1);
      }
    } else if (value instanceof Collection) {
      for (Object v : (Collection<?>) value) {
        if (v instanceof BakedQuad) {
          return; // a quad list outside a SimpleBakedModel: left as it is
        }
        visitValue(v, depth + 1);
      }
    } else if (value instanceof Pair) {
      visitValue(((Pair<?, ?>) value).getLeft(), depth + 1);
      visitValue(((Pair<?, ?>) value).getRight(), depth + 1);
    } else if (isModelHelper(value.getClass())) {
      // WeightedBakedModel's entries: a helper class that belongs to a model class.
      for (Field f : fieldsOf(value.getClass())) {
        try {
          visitValue(f.get(value), depth + 1);
        } catch (Throwable ignored) {
          // Unreadable field: nothing to share there.
        }
      }
    }
  }

  /**
   * Whether a class is a helper nested in a model class. Decided once per class, since
   * {@code getEnclosingClass()} is slow and the walk asks it of millions of values.
   */
  private boolean isModelHelper(Class<?> type) {
    Boolean known = modelHelpers.get(type);
    if (known == null) {
      Class<?> enclosing = type.getEnclosingClass();
      known = enclosing != null && IBakedModel.class.isAssignableFrom(enclosing);
      modelHelpers.put(type, known);
    }
    return known;
  }

  private void share(SimpleBakedModel model) {
    share(model.getQuads(null, null, 0L));
    for (EnumFacing side : EnumFacing.values()) {
      share(model.getQuads(null, side, 0L));
    }
    if (!ownModels) {
      return; // another mod's lists stay its own: it may still add to them
    }
    try {
      List<?> general = (List<?>) generalQuadsField.get(model);
      if (general != null && general != EMPTY && general.isEmpty()) {
        generalQuadsField.set(model, EMPTY);
        emptied++;
      }
      @SuppressWarnings("unchecked")
      Map<EnumFacing, List<BakedQuad>> faces =
          (Map<EnumFacing, List<BakedQuad>>) faceQuadsField.get(model);
      if (faces instanceof EnumMap) {
        for (Map.Entry<EnumFacing, List<BakedQuad>> e : faces.entrySet()) {
          List<BakedQuad> list = e.getValue();
          if (list != null && list != (Object) EMPTY && list.isEmpty()) {
            e.setValue(Collections.emptyList());
            emptied++;
          }
        }
      }
    } catch (IllegalAccessException | RuntimeException ignored) {
      // A model built some other way keeps its own lists.
    }
    shareOverrides(model.getOverrides());
  }

  private void shareOverrides(ItemOverrideList overrides) {
    if (overrides == null) {
      return;
    }
    try {
      List<?> list = (List<?>) overridesField.get(overrides);
      if (list != null && list != EMPTY && list.isEmpty()) {
        overridesField.set(overrides, EMPTY);
        emptied++;
      }
    } catch (IllegalAccessException | RuntimeException ignored) {
      // Kept as it is.
    }
  }

  private void share(List<BakedQuad> quads) {
    if (!(quads instanceof ArrayList)) {
      return;
    }
    ArrayList<BakedQuad> list = (ArrayList<BakedQuad>) quads;
    for (int i = 0; i < list.size(); i++) {
      BakedQuad q = list.get(i);
      if (q == null || q.getClass() != BakedQuad.class) {
        continue;
      }
      seen++;
      BakedQuad first = canonical.get(q);
      if (first == null) {
        canonical.put(q, q);
      } else if (first != q) {
        // Not a structural change: safe while the chunk builders read the list (class comment).
        list.set(i, first);
        replaced++;
        replacedBytes += QUAD_OBJECT_BYTES + 16L + 4L * q.getVertexData().length;
      }
    }
    if (ownModels) {
      list.trimToSize();
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
        if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()
            || f.getType() == String.class) {
          continue;
        }
        try {
          f.setAccessible(true);
          fields.add(f);
        } catch (Throwable ignored) {
          // A field the JVM will not open holds nothing this pass needs.
        }
      }
    }
    fieldCache.put(type, fields);
    return fields;
  }
}
