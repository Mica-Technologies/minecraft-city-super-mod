package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IRegistryDelegate;

/**
 * Points every state of a CSM block whose blockstate file is a {@code multipart} one at a single
 * model location, instead of one location per state.
 *
 * <p>Vanilla names a model location after each state ({@code csm:block#a=1,b=2,...}) and the
 * model loader resolves each one on its own. For a multipart blockstate every one of those
 * locations resolves to the same {@code MultipartModel}, which bakes to one
 * {@code MultipartBakedModel} that picks its parts from the state it is given when it is drawn.
 * So the per-state locations only cost: the location strings, built for every state at load and
 * again when the block model shapes are rebuilt; a missing-variant exception per state (that is
 * how Forge's variant loader discovers a location is multipart); and an entry per state in the
 * model cache, the loader's maps and the baked model registry. About 170 thousand of CSM's 290
 * thousand states are on multipart blockstates (the standpipes, exit signs, scaffold, panes,
 * mast arm curves, furniture runs, fences...).</p>
 *
 * <p>Whether a blockstate is multipart is known only once its file is read, and resource packs
 * can change that at every reload, so the mapper decides per block and per model loader: the
 * first time it is asked for a block while a loader is loading, it asks that loader for the
 * block's definition (which the loader was about to read anyway, and caches). A definition with
 * multipart data and no variant named after a state maps every state to
 * {@code csm:block#multipart}; the variants such files also carry ({@code inventory} and its
 * like, for the item) are untouched. Any other definition gets vanilla's default mapping,
 * exactly as if no mapper were registered. That mapping is built once in the loader's pass, as
 * vanilla builds it once, and handed to both of the loader's calls for that block.</p>
 *
 * <p>Nothing that is drawn changes: the one location bakes the same multipart model every
 * per-state location did, and it receives the real state. <b>The rule this makes:</b> never
 * look a CSM multipart block's baked model up by a per-state location
 * ({@code new ModelResourceLocation(name, "facing=north,...")}); ask the block model shapes for
 * the state's model. {@code -Dcsm.noMultipartStateMapper=true} leaves every block on vanilla's
 * mapping.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmMultipartStateMapper implements IStateMapper {

  /** Set to {@code true} to leave every CSM block on vanilla's per-state model locations. */
  public static final String DISABLE_PROPERTY = "csm.noMultipartStateMapper";

  /** The variant every state of a multipart block is mapped to. */
  public static final String VARIANT = "multipart";

  private static final CsmMultipartStateMapper INSTANCE = new CsmMultipartStateMapper();

  private Object variantLoader;
  private Field variantLoaderLoader;
  private Method getDefinition;
  private Field definitionVariants;

  /** The loader the decisions below were made for. */
  private WeakReference<Object> decidedFor = new WeakReference<>(null);
  /** Per block: its one location, or {@code null} for vanilla's mapping. */
  private final Map<Block, ModelResourceLocation> decisions = new IdentityHashMap<>();
  /** Vanilla mappings built in the loader's first call for a block, kept for its second. */
  private final Map<Block, Map<IBlockState, ModelResourceLocation>> pending =
      new IdentityHashMap<>();
  /** Blocks whose kept mapping was handed out; later calls build a fresh one, as vanilla. */
  private final Map<Block, Boolean> consumed = new IdentityHashMap<>();
  /** Whether the loader is still in its pass over the blocks (until its bake event). */
  private boolean loading;

  private int multipartBlocks;
  private long multipartStates;

  private CsmMultipartStateMapper() {
  }

  /**
   * Registers the mapper for every CSM block with more than one state that has no state mapper
   * of its own. Call from {@code ModelRegistryEvent}, when the blocks' registry names are set
   * (a state mapper is keyed on the block's registry delegate, whose equality is its name).
   *
   * @param blocks CSM's blocks
   */
  public static void register(Iterable<Block> blocks) {
    if (Boolean.getBoolean(DISABLE_PROPERTY)) {
      Csm.getLogger().info("Multipart state mapper off (-D{}=true)", DISABLE_PROPERTY);
      return;
    }
    Map<IRegistryDelegate<Block>, IStateMapper> existing;
    try {
      INSTANCE.resolve();
      Field f = ModelLoader.class.getDeclaredField("customStateMappers");
      f.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<IRegistryDelegate<Block>, IStateMapper> map =
          (Map<IRegistryDelegate<Block>, IStateMapper>) f.get(null);
      existing = map;
    } catch (Throwable t) {
      Csm.getLogger().warn("Multipart state mapper: not registered, every block keeps its "
          + "per-state model locations", t);
      return;
    }
    Map<Block, Boolean> own = new IdentityHashMap<>();
    for (IRegistryDelegate<Block> delegate : existing.keySet()) {
      own.put(delegate.get(), Boolean.TRUE);
    }
    int registered = 0;
    for (Block block : blocks) {
      if (block.getRegistryName() == null || own.containsKey(block)
          || block.getBlockState().getValidStates().size() < 2) {
        continue;
      }
      ModelLoader.setCustomStateMapper(block, INSTANCE);
      registered++;
    }
    MinecraftForge.EVENT_BUS.register(INSTANCE);
    Csm.getLogger().info("Multipart state mapper registered for {} blocks", registered);
  }

  /**
   * Logs what the loader that just finished mapped, and lets go of anything it did not ask for
   * a second time.
   *
   * @param event the bake event
   */
  @SubscribeEvent
  public synchronized void onModelBake(ModelBakeEvent event) {
    loading = false;
    pending.clear();
    Csm.getLogger().info("Multipart state mapper: {} blocks, {} states on one model location "
        + "each", multipartBlocks, multipartStates);
  }

  private void resolve() throws ReflectiveOperationException {
    Class<?> cls = Class.forName("net.minecraftforge.client.model.ModelLoader$VariantLoader");
    variantLoader = cls.getEnumConstants()[0];
    variantLoaderLoader = cls.getDeclaredField("loader");
    variantLoaderLoader.setAccessible(true);
    getDefinition = ObfuscationReflectionHelper.findMethod(ModelBakery.class, "func_177586_a",
        ModelBlockDefinition.class, ResourceLocation.class);
    definitionVariants = ObfuscationReflectionHelper.findField(ModelBlockDefinition.class,
        "field_178332_b");
  }

  @Override
  public synchronized Map<IBlockState, ModelResourceLocation> putStateModelLocations(
      Block block) {
    Object loader = currentLoader();
    if (loader != decidedFor.get()) {
      decisions.clear();
      pending.clear();
      consumed.clear();
      loading = true;
      multipartBlocks = 0;
      multipartStates = 0;
      decidedFor = new WeakReference<>(loader);
    }
    ModelResourceLocation one;
    if (decisions.containsKey(block)) {
      one = decisions.get(block);
    } else {
      one = decide(loader, block);
      decisions.put(block, one);
    }
    List<IBlockState> states = block.getBlockState().getValidStates();
    if (one != null) {
      Map<IBlockState, ModelResourceLocation> map = new IdentityHashMap<>(states.size());
      for (IBlockState state : states) {
        map.put(state, one);
      }
      return map;
    }
    Map<IBlockState, ModelResourceLocation> kept = pending.remove(block);
    if (kept != null) {
      consumed.put(block, Boolean.TRUE);
      return kept;
    }
    Map<IBlockState, ModelResourceLocation> map =
        new DefaultStateMapper().putStateModelLocations(block);
    if (loading && !consumed.containsKey(block)) {
      pending.put(block, map);
    }
    return map;
  }

  private Object currentLoader() {
    try {
      return variantLoaderLoader.get(variantLoader);
    } catch (Throwable t) {
      return null;
    }
  }

  /**
   * One location for every state of the block, or {@code null} for vanilla's mapping.
   */
  private ModelResourceLocation decide(Object loader, Block block) {
    ResourceLocation name = block.getRegistryName();
    if (loader == null || name == null) {
      return null;
    }
    try {
      ModelBlockDefinition definition = (ModelBlockDefinition) getDefinition.invoke(loader, name);
      if (definition == null || !definition.hasMultipartData()
          || definition.hasVariant(VARIANT)) {
        return null;
      }
      Map<?, ?> variants = (Map<?, ?>) definitionVariants.get(definition);
      for (Object key : variants.keySet()) {
        // A variant named after a state ("a=1,b=2") would win over the multipart for that state.
        if (key instanceof String && (((String) key).indexOf('=') >= 0
            || "normal".equals(key))) {
          return null;
        }
      }
      multipartBlocks++;
      multipartStates += block.getBlockState().getValidStates().size();
      return new ModelResourceLocation(name, VARIANT);
    } catch (Throwable t) {
      return null;
    }
  }
}
