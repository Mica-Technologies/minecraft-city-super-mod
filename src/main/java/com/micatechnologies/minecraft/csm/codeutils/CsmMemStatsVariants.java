package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmConstants;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * {@code /csm memstats variants}: how many of CSM's blockstate variants, and of the models baked
 * from them, are the same thing written out again.
 *
 * <p>Forge bakes one model per variant object, and every variant of a Forge blockstate is its own
 * object even when it names the same model, textures and transform as another. This counts, over
 * the unbaked variant models Forge keeps in {@link ModelLoaderRegistry}'s cache, how many distinct
 * variants there are by content (model, rotation, uvlock, weight, textures, custom data, smooth
 * and gui3d flags, transform and sub-models), and how many distinct parts (a variant's base model
 * or one sub-model, with the transform it is baked under). Those are the numbers of bakes a cache
 * keyed on content would do. It then counts the baked {@link SimpleBakedModel}s under
 * {@code csm:} locations and how many of them hold exactly the same quads and flags, which is
 * what sharing baked models after the bake would leave.</p>
 *
 * <p>Read only. It reads fields by reflection and never calls {@code getQuads} or {@code bake}.
 * A transform it cannot compare by value is compared by identity, so the counts can only
 * understate the duplication, never overstate it.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmMemStatsVariants {

  private static final String LOADER = "net.minecraftforge.client.model.ModelLoader";
  private static final String FORGE_VARIANT =
      "net.minecraftforge.client.model.BlockStateLoader$ForgeVariant";
  private static final String SUB_MODEL =
      "net.minecraftforge.client.model.BlockStateLoader$SubModel";

  private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();

  private CsmMemStatsVariants() {
  }

  /**
   * Runs the count. Call on the client thread.
   *
   * @return the report lines
   */
  public static List<String> run() {
    List<String> out = new ArrayList<>();
    long start = System.nanoTime();
    CsmMemStatsVariants v = new CsmMemStatsVariants();
    try {
      v.unbaked(out);
    } catch (Throwable e) {
      out.add("variants: unbaked cache unreadable (" + e + ")");
    }
    try {
      v.baked(out);
    } catch (Throwable e) {
      out.add("variants: baked registry unreadable (" + e + ")");
    }
    out.add(String.format(Locale.ROOT, "variants: done in %d ms",
        (System.nanoTime() - start) / 1_000_000L));
    return out;
  }

  private void unbaked(List<String> out) throws Exception {
    Field cacheField = ModelLoaderRegistry.class.getDeclaredField("cache");
    cacheField.setAccessible(true);
    Map<?, ?> cache = (Map<?, ?>) cacheField.get(null);
    Class<?> weighted = Class.forName(LOADER + "$WeightedRandomModel");
    Class<?> multipart = Class.forName(LOADER + "$MultipartModel");
    Field variantsField = accessible(weighted.getDeclaredField("variants"));

    long variantModels = 0;
    long multipartModels = 0;
    long variantEntries = 0;
    long parts = 0;
    Set<Object> distinctModels = new HashSet<>();
    Set<Object> distinctParts = new HashSet<>();
    long objParts = 0;
    Set<Object> distinctObjParts = new HashSet<>();
    Map<String, long[]> byFile = new HashMap<>();
    for (Map.Entry<?, ?> e : cache.entrySet()) {
      Object key = e.getKey();
      if (!(key instanceof ModelResourceLocation)
          || !CsmConstants.MOD_NAMESPACE.equals(((ResourceLocation) key).getNamespace())) {
        continue;
      }
      Object model = e.getValue();
      if (multipart.isInstance(model)) {
        multipartModels++;
        continue;
      }
      if (!weighted.isInstance(model)) {
        continue;
      }
      variantModels++;
      List<?> variants = (List<?>) variantsField.get(model);
      List<Object> modelKey = new ArrayList<>(variants.size());
      for (Object variant : variants) {
        variantEntries++;
        List<Object> vk = variantKey((Variant) variant);
        modelKey.add(vk);
        for (List<Object> part : partKeys((Variant) variant)) {
          parts++;
          distinctParts.add(part);
          if (isObj(part)) {
            objParts++;
            distinctObjParts.add(part);
          }
        }
      }
      boolean isNew = distinctModels.add(modelKey);
      long[] f = byFile.computeIfAbsent(((ResourceLocation) key).getPath(), k -> new long[2]);
      f[0]++;
      if (isNew) {
        f[1]++;
      }
    }
    out.add(String.format(Locale.ROOT,
        "variants: %d csm variant models (%d variant entries), %d distinct by content (%.1f%%"
            + " duplicate); %d multipart models", variantModels, variantEntries,
        distinctModels.size(),
        100.0 * (variantModels - distinctModels.size()) / Math.max(1, variantModels),
        multipartModels));
    out.add(String.format(Locale.ROOT,
        "variants: %d parts to bake (base models and sub-models), %d distinct (%.1f%% duplicate)",
        parts, distinctParts.size(),
        100.0 * (parts - distinctParts.size()) / Math.max(1, parts)));
    out.add(String.format(Locale.ROOT,
        "variants: of those, %d are OBJ models, %d distinct (%.1f%% duplicate)", objParts,
        distinctObjParts.size(),
        100.0 * (objParts - distinctObjParts.size()) / Math.max(1, objParts)));
    List<Map.Entry<String, long[]>> files = new ArrayList<>(byFile.entrySet());
    files.sort((a, b) -> Long.compare(b.getValue()[0] - b.getValue()[1],
        a.getValue()[0] - a.getValue()[1]));
    StringBuilder sb = new StringBuilder("variants: most repeated blockstates (variants/distinct):");
    for (int i = 0; i < Math.min(15, files.size()); i++) {
      Map.Entry<String, long[]> f = files.get(i);
      sb.append(' ').append(f.getKey()).append('=').append(f.getValue()[0]).append('/')
          .append(f.getValue()[1]);
    }
    out.add(sb.toString());
  }

  /** A variant by content; the blockstate file it came from is left out on purpose. */
  private List<Object> variantKey(Variant v) throws Exception {
    List<Object> k = new ArrayList<>();
    k.add(v.getClass().getName());
    k.add(v.getModelLocation());
    k.add(v.getRotation());
    k.add(v.isUvLock());
    k.add(v.getWeight());
    if (FORGE_VARIANT.equals(v.getClass().getName())) {
      k.add(get(v, "textures"));
      k.add(get(v, "customData"));
      k.add(get(v, "smooth"));
      k.add(get(v, "gui3d"));
      k.add(stateKey(get(v, "state")));
      Map<?, ?> subs = (Map<?, ?>) get(v, "parts");
      List<Object> sk = new ArrayList<>();
      for (Map.Entry<?, ?> s : subs.entrySet()) {
        sk.add(s.getKey());
        sk.add(subKey(s.getValue()));
      }
      k.add(sk);
    }
    return k;
  }

  /** The separately baked parts of a variant: its base model and each sub-model. */
  private List<List<Object>> partKeys(Variant v) throws Exception {
    List<List<Object>> keys = new ArrayList<>();
    if (!FORGE_VARIANT.equals(v.getClass().getName())) {
      keys.add(Arrays.asList("vanilla", v.getModelLocation(), v.getRotation(), v.isUvLock()));
      return keys;
    }
    Object state = stateKey(get(v, "state"));
    keys.add(Arrays.asList("base", v.getModelLocation(), v.isUvLock(), get(v, "textures"),
        get(v, "customData"), get(v, "smooth"), get(v, "gui3d"), state));
    for (Object sub : ((Map<?, ?>) get(v, "parts")).values()) {
      // A sub-model is baked under the variant's transform composed with its own.
      keys.add(Arrays.asList("sub", state, subKey(sub)));
    }
    return keys;
  }

  /** Whether a part key names an OBJ model (the base model's or a sub-model's location). */
  private static boolean isObj(List<Object> part) {
    Object loc = "sub".equals(part.get(0)) ? ((List<?>) part.get(2)).get(5) : part.get(1);
    return loc instanceof ResourceLocation
        && ((ResourceLocation) loc).getPath().endsWith(".obj");
  }

  private List<Object> subKey(Object sub) throws Exception {
    return Arrays.asList(stateKey(get(sub, "state")), get(sub, "uvLock"), get(sub, "smooth"),
        get(sub, "gui3d"), get(sub, "textures"), get(sub, "model"), get(sub, "customData"));
  }

  /** Transforms and rotations compare by value; any other state only by identity. */
  private static Object stateKey(Object state) {
    if (state == null || state instanceof TRSRTransformation || state instanceof Enum) {
      return state;
    }
    if (state instanceof IModelState) {
      return new Identity(state);
    }
    return state;
  }

  private void baked(List<String> out) throws Exception {
    ModelManager manager = Minecraft.getMinecraft().getBlockRendererDispatcher()
        .getBlockModelShapes().getModelManager();
    IRegistry<ModelResourceLocation, IBakedModel> registry =
        ObfuscationReflectionHelper.getPrivateValue(ModelManager.class, manager, "field_174958_a");
    ReferenceOpenHashSet<Object> seen = new ReferenceOpenHashSet<>();
    List<SimpleBakedModel> simple = new ArrayList<>();
    for (ModelResourceLocation loc : registry.getKeys()) {
      if (CsmConstants.MOD_NAMESPACE.equals(loc.getNamespace())) {
        collect(registry.getObject(loc), seen, simple, 0);
      }
    }
    Field general = ObfuscationReflectionHelper.findField(SimpleBakedModel.class,
        "field_177563_a");
    Field faces = ObfuscationReflectionHelper.findField(SimpleBakedModel.class, "field_177561_b");
    Set<List<Object>> distinct = new HashSet<>();
    long quads = 0;
    long distinctQuads = 0;
    for (SimpleBakedModel m : simple) {
      Map<?, ?> faceMap = (Map<?, ?>) faces.get(m);
      List<Object> k = new ArrayList<>();
      int n = ((List<?>) general.get(m)).size();
      k.add(general.get(m));
      for (EnumFacing f : EnumFacing.values()) {
        List<?> l = (List<?>) faceMap.get(f);
        k.add(l);
        n += l == null ? 0 : l.size();
      }
      k.add(m.isAmbientOcclusion());
      k.add(m.isGui3d());
      k.add(new Identity(m.getParticleTexture()));
      quads += n;
      if (distinct.add(k)) {
        distinctQuads += n;
      }
    }
    out.add(String.format(Locale.ROOT,
        "variants: %d SimpleBakedModels under csm: locations hold %d quad references; %d are"
            + " distinct by content (%.1f%% duplicate), holding %d quad references",
        simple.size(), quads, distinct.size(),
        100.0 * (simple.size() - distinct.size()) / Math.max(1, simple.size()), distinctQuads));
  }

  /** Follows baked wrappers (never unbaked models) down to their SimpleBakedModels. */
  private void collect(Object node, ReferenceOpenHashSet<Object> seen, List<SimpleBakedModel> into,
      int depth) throws IllegalAccessException {
    if (node == null || depth > 12 || !seen.add(node)) {
      return;
    }
    if (node instanceof SimpleBakedModel) {
      into.add((SimpleBakedModel) node);
      return;
    }
    if (node instanceof Collection) {
      for (Object o : (Collection<?>) node) {
        collect(o, seen, into, depth + 1);
      }
      return;
    }
    if (node instanceof Map) {
      for (Object o : ((Map<?, ?>) node).values()) {
        collect(o, seen, into, depth + 1);
      }
      return;
    }
    if (!(node instanceof IBakedModel) && !isBakedHolder(node)) {
      return;
    }
    for (Field f : fieldsOf(node.getClass())) {
      collect(f.get(node), seen, into, depth + 1);
    }
  }

  /** Objects inside a baked model that can hold further baked models (weighted entries). */
  private static boolean isBakedHolder(Object o) {
    String n = o.getClass().getName();
    return n.startsWith("net.minecraft.client.renderer.block.model.WeightedBakedModel")
        || n.startsWith("net.minecraftforge.client.model.MultiModel");
  }

  private List<Field> fieldsOf(Class<?> type) {
    return fieldCache.computeIfAbsent(type, t -> {
      List<Field> fields = new ArrayList<>();
      for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
        for (Field f : c.getDeclaredFields()) {
          // Skip statics, primitives and the outer-instance links (they lead to the unbaked
          // model and from there to the whole model loader).
          if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()
              || f.isSynthetic()) {
            continue;
          }
          fields.add(accessible(f));
        }
      }
      return fields;
    });
  }

  private static Object get(Object o, String name) throws Exception {
    for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
      try {
        return accessible(c.getDeclaredField(name)).get(o);
      } catch (NoSuchFieldException ignored) {
        // keep looking in the superclass
      }
    }
    throw new NoSuchFieldException(o.getClass().getName() + "." + name);
  }

  private static Field accessible(Field f) {
    f.setAccessible(true);
    return f;
  }

  /** Wraps an object so a key compares it by identity. */
  private static final class Identity {

    private final Object o;

    Identity(Object o) {
      this.o = o;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Identity && ((Identity) other).o == o;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(o);
    }
  }
}
