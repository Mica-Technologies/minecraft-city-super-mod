package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.collect.ImmutableMap;
import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Once baking is done, makes CSM's baked models share their equal model names and transforms.
 *
 * <p>Two kinds of small immutable value are repeated hundreds of thousands of times after a
 * bake, one copy per variant:</p>
 * <ul>
 *   <li><b>Model location strings.</b> Every {@link ModelResourceLocation} in the model registry
 *   was parsed from its own string, so it holds its own copy of the namespace, the path and the
 *   variant ({@code facing=north,lit=true}). For CSM that was 300 thousand of each, but only
 *   4 thousand distinct paths and 46 thousand distinct variants: about 85 MB.</li>
 *   <li><b>Transforms.</b> Forge gives every baked variant its own {@link TRSRTransformation}s
 *   (each with its own matrix) for its rotation and its item display, and every vanilla-style
 *   model its own {@link ItemCameraTransforms}. 460 thousand transforms held 1,078 distinct
 *   matrices, and 290 thousand camera transforms were all equal: about 60 MB.</li>
 * </ul>
 *
 * <p>After the bake ({@link ModelBakeEvent}, lowest priority) this points every registry key's
 * three strings at one shared copy of each, and walks the baked models under {@code csm:}
 * locations, replacing each transform, each map of transforms and each camera transform with
 * the first equal one it met. Equal means exactly equal: the same bits in every float, the same
 * map class and entry order. Nothing on screen changes, because every replacement is equal in
 * value to what it replaces. With every module the names take about 0.1 s and the transforms
 * about a second (1.7 million objects walked, 600 thousand references replaced by 1,041
 * transforms and 272 camera transforms).</p>
 *
 * <p>The values are immutable in practice: a {@link TRSRTransformation} hands out copies of its
 * matrix and vectors, and camera transforms are only read. The one thing a transform computes
 * later, its decomposition, is computed here before it is shared, so threads that meet it later
 * only read it. <b>The rule this makes:</b> never mutate a transform, a model's transform map,
 * a camera transform or a registry key's strings; they are shared.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmBakedModelInterning {

  private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();
  private final Map<Class<?>, Boolean> walkableClasses = new HashMap<>();
  private Map<Bits, TRSRTransformation> transforms;
  private Map<List<Object>, ImmutableMap<?, ?>> maps;
  private Map<Object, Optional<?>> optionals;
  private Map<Bits, ItemCameraTransforms> cameras;
  /** Each value already looked up, to its shared equal: most are met many times. */
  private Reference2ObjectOpenHashMap<Object, Object> seenValues;
  private ReferenceOpenHashSet<Object> visited;
  private Field pairRight;
  private Field cameraField;
  private Field simpleCameraField;
  private long replaced;

  /**
   * Runs both passes on the freshly baked registry.
   *
   * @param event the bake event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onModelBake(ModelBakeEvent event) {
    internNames(event.getModelRegistry());
    internTransforms(event.getModelRegistry());
  }

  /** Points every registry key's namespace, path and variant at one shared string. */
  private void internNames(IRegistry<ModelResourceLocation, IBakedModel> registry) {
    long start = System.nanoTime();
    try {
      Field namespace = ObfuscationReflectionHelper.findField(ResourceLocation.class,
          "field_110626_a");
      Field path = ObfuscationReflectionHelper.findField(ResourceLocation.class,
          "field_110625_b");
      Field variant = ObfuscationReflectionHelper.findField(ModelResourceLocation.class,
          "field_177519_c");
      Map<String, String> strings = new HashMap<>(1 << 17);
      long keys = 0;
      long shared = 0;
      for (ModelResourceLocation key : registry.getKeys()) {
        keys++;
        shared += intern(key, namespace, strings) + intern(key, path, strings)
            + intern(key, variant, strings);
      }
      Csm.getLogger().info("Interned model location names: {} strings of {} registry keys "
              + "replaced by {} shared ones, {} ms", shared, keys, strings.size(),
          (System.nanoTime() - start) / 1_000_000L);
    } catch (Throwable t) {
      // Only a saving; a key already interned is equal to what it was.
      Csm.getLogger().warn("Could not intern model location names; models are unaffected", t);
    }
  }

  private static int intern(Object key, Field field, Map<String, String> strings)
      throws IllegalAccessException {
    String s = (String) field.get(key);
    if (s == null) {
      return 0;
    }
    String first = strings.putIfAbsent(s, s);
    if (first == null || first == s) {
      return 0;
    }
    field.set(key, first);
    return 1;
  }

  /** Shares equal transforms and camera transforms across CSM's baked models. */
  private void internTransforms(IRegistry<ModelResourceLocation, IBakedModel> registry) {
    long start = System.nanoTime();
    transforms = new HashMap<>();
    maps = new HashMap<>();
    optionals = new HashMap<>();
    cameras = new HashMap<>();
    seenValues = new Reference2ObjectOpenHashMap<>(1 << 20);
    visited = new ReferenceOpenHashSet<>(1 << 21);
    replaced = 0;
    try {
      cameraField = ObfuscationReflectionHelper.findField(ModelBlock.class, "field_178320_j");
      simpleCameraField = ObfuscationReflectionHelper.findField(SimpleBakedModel.class,
          "field_177558_f");
      for (ModelResourceLocation key : registry.getKeys()) {
        if (CsmConstants.MOD_NAMESPACE.equals(key.getNamespace())) {
          visit(registry.getObject(key), 0);
        }
      }
      Csm.getLogger().info("Interned baked model transforms: {} replaced; {} distinct "
              + "transforms, {} transform maps, {} camera transforms kept, {} objects walked, "
              + "{} ms", replaced, transforms.size(), maps.size(), cameras.size(),
          visited.size(), (System.nanoTime() - start) / 1_000_000L);
    } catch (Throwable t) {
      Csm.getLogger().warn("Transform interning stopped early; models are unaffected", t);
    } finally {
      transforms = null;
      maps = null;
      optionals = null;
      cameras = null;
      seenValues = null;
      visited = null;
      fieldCache.clear();
      walkableClasses.clear();
    }
  }

  /** Walks one object of Forge's or vanilla's model classes, interning what its fields hold. */
  private void visit(Object node, int depth)
      throws IllegalAccessException, NoSuchFieldException {
    if (node == null || depth > 24 || !visited.add(node)) {
      return;
    }
    if (node instanceof ModelBlock) {
      ModelBlock block = (ModelBlock) node;
      Object camera = cameraField.get(block);
      if (camera instanceof ItemCameraTransforms) {
        ItemCameraTransforms first = camera((ItemCameraTransforms) camera);
        if (first != camera) {
          cameraField.set(block, first);
          replaced++;
        }
      }
      return;
    }
    if (node instanceof SimpleBakedModel) {
      // Quads, sprite and overrides hold no transform; only its camera transform matters.
      Object camera = simpleCameraField.get(node);
      if (camera instanceof ItemCameraTransforms && camera.getClass() == ItemCameraTransforms.class) {
        ItemCameraTransforms first = camera((ItemCameraTransforms) camera);
        if (first != camera) {
          simpleCameraField.set(node, first);
          replaced++;
        }
      }
      return;
    }
    for (Field f : fieldsOf(node.getClass())) {
      Object value = f.get(node);
      Object first = intern(value, depth);
      if (first != value && f.getType().isInstance(first)) {
        f.set(node, first);
        replaced++;
      }
    }
  }

  /**
   * The shared equal of a field's value when it is one this pass interns, after walking into
   * it when it is a model, a model state or a container of them; otherwise the value itself.
   */
  private Object intern(Object value, int depth)
      throws IllegalAccessException, NoSuchFieldException {
    if (value == null) {
      return null;
    }
    Object known = seenValues.get(value);
    if (known != null) {
      return known;
    }
    Object first = internNew(value, depth);
    if (value instanceof TRSRTransformation || value instanceof ItemCameraTransforms
        || value instanceof Optional || value instanceof ImmutableMap) {
      seenValues.put(value, first);
    }
    return first;
  }

  private Object internNew(Object value, int depth)
      throws IllegalAccessException, NoSuchFieldException {
    if (value instanceof TRSRTransformation) {
      return transform((TRSRTransformation) value);
    }
    if (value instanceof ItemCameraTransforms) {
      return value.getClass() == ItemCameraTransforms.class
          ? camera((ItemCameraTransforms) value) : value;
    }
    if (value instanceof Optional) {
      Optional<?> o = (Optional<?>) value;
      if (o.isPresent() && o.get() instanceof TRSRTransformation) {
        TRSRTransformation t = transform((TRSRTransformation) o.get());
        Optional<?> first = optionals.get(t);
        if (first == null) {
          first = t == o.get() ? o : Optional.of(t);
          optionals.put(t, first);
        }
        return first;
      }
      return value;
    }
    if (value instanceof ImmutableMap) {
      return map((ImmutableMap<?, ?>) value, depth);
    }
    if (value instanceof Collection) {
      for (Object o : (Collection<?>) value) {
        if (o instanceof BakedQuad || !walkable(o)) {
          break;
        }
        visit(o, depth + 1);
      }
      return value;
    }
    if (value instanceof Map) {
      for (Object o : ((Map<?, ?>) value).values()) {
        if (walkable(o)) {
          visit(o, depth + 1);
        }
      }
      return value;
    }
    if (value instanceof Pair) {
      Pair<?, ?> p = (Pair<?, ?>) value;
      if (walkable(p.getLeft())) {
        visit(p.getLeft(), depth + 1);
      }
      if (walkable(p.getRight())) {
        visit(p.getRight(), depth + 1);
      } else if (p instanceof ImmutablePair && p.getRight() instanceof TRSRTransformation) {
        // MultiModel.Baked's transforms: (model, transform) pairs.
        TRSRTransformation t = transform((TRSRTransformation) p.getRight());
        if (t != p.getRight()) {
          if (pairRight == null) {
            pairRight = ImmutablePair.class.getField("right");
            pairRight.setAccessible(true);
          }
          pairRight.set(p, t);
          replaced++;
        }
      }
      return value;
    }
    if (walkable(value)) {
      visit(value, depth + 1);
    }
    return value;
  }

  /** Whether an object is one of the model classes the walk looks inside. */
  private boolean walkable(Object o) {
    if (o == null) {
      return false;
    }
    // Decided once per class: getEnclosingClass() is slow, and it is asked of every value.
    Boolean known = walkableClasses.get(o.getClass());
    if (known == null) {
      known = walkableClass(o.getClass());
      walkableClasses.put(o.getClass(), known);
    }
    return known;
  }

  private static boolean walkableClass(Class<?> type) {
    if (OBJModel.class.isAssignableFrom(type) || TextureAtlasSprite.class.isAssignableFrom(type)
        || VertexFormat.class.isAssignableFrom(type) || ResourceLocation.class.isAssignableFrom(type)
        || type.isEnum() || Enum.class.isAssignableFrom(type)
        || TRSRTransformation.class.isAssignableFrom(type)
        || ItemTransformVec3f.class.isAssignableFrom(type)) {
      return false;
    }
    Class<?> enclosing = type.getEnclosingClass();
    if (!(IBakedModel.class.isAssignableFrom(type) || IModelState.class.isAssignableFrom(type)
        || IModel.class.isAssignableFrom(type) || ModelBlock.class.isAssignableFrom(type)
        || net.minecraft.client.renderer.block.model.ItemOverrideList.class.isAssignableFrom(type)
        // WeightedBakedModel's entries: a helper class that belongs to a model class.
        || enclosing != null && IBakedModel.class.isAssignableFrom(enclosing))) {
      return false;
    }
    String name = type.getName();
    return name.startsWith("net.minecraft.") || name.startsWith("net.minecraftforge.");
  }

  /** An immutable map: shared if it maps to transforms only, otherwise walked. */
  private Object map(ImmutableMap<?, ?> map, int depth)
      throws IllegalAccessException, NoSuchFieldException {
    boolean allTransforms = !map.isEmpty();
    for (Object v : map.values()) {
      if (!(v instanceof TRSRTransformation)) {
        allTransforms = false;
        break;
      }
    }
    if (!allTransforms) {
      for (Object v : map.values()) {
        if (v instanceof Pair) {
          intern(v, depth + 1);
        } else if (walkable(v)) {
          visit(v, depth + 1);
        }
      }
      return map;
    }
    List<Object> key = new ArrayList<>(map.size() * 2 + 1);
    key.add(map.getClass());
    boolean changed = false;
    for (Map.Entry<?, ?> e : map.entrySet()) {
      TRSRTransformation t = transform((TRSRTransformation) e.getValue());
      changed |= t != e.getValue();
      key.add(e.getKey());
      key.add(new Identity(t));
    }
    ImmutableMap<?, ?> first = maps.get(key);
    if (first != null) {
      return first;
    }
    ImmutableMap<?, ?> kept = map;
    if (changed) {
      // Rebuilt from the shared transforms, in the same order. A builder gives a map of the
      // same behaviour; the key names the original class, so only maps of one class meet.
      ImmutableMap.Builder<Object, Object> b = ImmutableMap.builder();
      for (Map.Entry<?, ?> e : map.entrySet()) {
        b.put(e.getKey(), transform((TRSRTransformation) e.getValue()));
      }
      kept = b.build();
    }
    maps.put(key, kept);
    return kept;
  }

  private TRSRTransformation transform(TRSRTransformation t) {
    Object known = seenValues.get(t);
    if (known != null) {
      return (TRSRTransformation) known;
    }
    Matrix4f m = t.getMatrix();
    Bits key = new Bits(new float[]{m.m00, m.m01, m.m02, m.m03, m.m10, m.m11, m.m12, m.m13,
        m.m20, m.m21, m.m22, m.m23, m.m30, m.m31, m.m32, m.m33});
    TRSRTransformation first = transforms.get(key);
    if (first == null) {
      // Compute the lazily built parts now, on this thread, before the transform is shared.
      try {
        t.getTranslation();
        t.transformNormal(new Vector3f(0, 1, 0));
      } catch (RuntimeException ignored) {
        // A transform that cannot be decomposed fails the same way wherever it is used.
      }
      transforms.put(key, t);
      first = t;
    }
    seenValues.put(t, first);
    return first;
  }

  private ItemCameraTransforms camera(ItemCameraTransforms c) {
    Object known = seenValues.get(c);
    if (known != null) {
      return (ItemCameraTransforms) known;
    }
    float[] f = new float[72];
    int i = 0;
    for (ItemTransformVec3f v : new ItemTransformVec3f[]{c.thirdperson_left, c.thirdperson_right,
        c.firstperson_left, c.firstperson_right, c.head, c.gui, c.ground, c.fixed}) {
      if (v == null) {
        return c; // never the case in vanilla or Forge; left as it is
      }
      for (org.lwjgl.util.vector.Vector3f x : new org.lwjgl.util.vector.Vector3f[]{v.rotation,
          v.translation, v.scale}) {
        f[i++] = x.x;
        f[i++] = x.y;
        f[i++] = x.z;
      }
    }
    ItemCameraTransforms first = cameras.putIfAbsent(new Bits(f), c);
    first = first == null ? c : first;
    seenValues.put(c, first);
    return first;
  }

  private List<Field> fieldsOf(Class<?> type) {
    List<Field> fields = fieldCache.get(type);
    if (fields != null) {
      return fields;
    }
    fields = new ArrayList<>();
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        Class<?> t = f.getType();
        if (Modifier.isStatic(f.getModifiers()) || t.isPrimitive() || t == String.class
            || t.isArray() || !mayHold(t)) {
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

  /** Whether a field of this declared type can hold anything the walk interns or enters. */
  private static boolean mayHold(Class<?> t) {
    if (t == Object.class || t.isInterface() && !java.util.function.Function.class.isAssignableFrom(t)) {
      return true;
    }
    return TRSRTransformation.class.isAssignableFrom(t)
        || ItemCameraTransforms.class.isAssignableFrom(t)
        || IBakedModel.class.isAssignableFrom(t) || IModelState.class.isAssignableFrom(t)
        || IModel.class.isAssignableFrom(t) || ModelBlock.class.isAssignableFrom(t)
        || net.minecraft.client.renderer.block.model.ItemOverrideList.class.isAssignableFrom(t)
        || Map.class.isAssignableFrom(t) || Collection.class.isAssignableFrom(t)
        || Pair.class.isAssignableFrom(t) || Optional.class.isAssignableFrom(t)
        || java.lang.reflect.Modifier.isAbstract(t.getModifiers());
  }

  /** A key of floats compared bit for bit, so -0 and 0, or two NaNs, are told apart. */
  private static final class Bits {

    private final int[] bits;
    private final int hash;

    Bits(float[] values) {
      bits = new int[values.length];
      for (int i = 0; i < values.length; i++) {
        bits[i] = Float.floatToRawIntBits(values[i]);
      }
      hash = Arrays.hashCode(bits);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Bits && ((Bits) other).hash == hash
          && Arrays.equals(((Bits) other).bits, bits);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }

  /** A key that compares its object by identity. */
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
