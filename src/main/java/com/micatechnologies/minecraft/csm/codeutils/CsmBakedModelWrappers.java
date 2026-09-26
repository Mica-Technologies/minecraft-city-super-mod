package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Puts a module's wrapper round the baked models it picks, however the models are baked.
 *
 * <p>The usual way to wrap a baked model is to walk the model registry's keys in
 * {@link ModelBakeEvent} and put a wrapper in place of each model wanted. That fails without a
 * word under VintageFix's dynamic resources, which a player's modpack may well run: models are
 * baked when first drawn instead of at load, the registry handed to the event lists only the
 * models some mod put into it, and a model is dropped a few minutes after it was last drawn and
 * baked again. So the walk finds nothing, and what is drawn is the plain model. That is how a
 * glazed door's glass came out opaque in such a pack (issue #242): its split between the
 * translucent and cutout passes was never put in place, so the glass was drawn in the cutout
 * pass as well.</p>
 *
 * <p>A wrapper registered here is applied both ways: to every key of the registry after the
 * bake, as before, and, where VintageFix is installed, to each model VintageFix bakes, through
 * the event it posts for that ({@code org.embeddedt.vintagefix.event.DynamicModelBakeEvent}).
 * That event is listened for by reflection, so VintageFix is never a build dependency. A
 * wrapper may be called from a chunk builder thread in that case, so it must be thread-safe,
 * and it may be called again for a model it already wrapped after a reload.</p>
 *
 * <p><b>The rule this makes:</b> never wrap a model by walking the registry's keys in a
 * {@code ModelBakeEvent} handler of your own; register a {@link Wrapper} here. Putting a model
 * of your own under a fixed key ({@code putObject}) is fine as it is: VintageFix keeps those.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmBakedModelWrappers {

  /** VintageFix's event for a model it has just baked. */
  private static final String DYNAMIC_BAKE_EVENT =
      "org.embeddedt.vintagefix.event.DynamicModelBakeEvent";

  private static final List<Wrapper> WRAPPERS = new CopyOnWriteArrayList<>();

  private static boolean installed;

  private CsmBakedModelWrappers() {
  }

  /**
   * Wraps a baked model, or leaves it.
   */
  @FunctionalInterface
  public interface Wrapper {

    /**
     * @param location the model's location
     * @param model    the baked model, possibly one this wrapper already returned
     * @return the model to use in its place, or {@code null} to leave it as it is
     */
    @Nullable
    IBakedModel wrap(ModelResourceLocation location, IBakedModel model);
  }

  /**
   * Registers a wrapper. Call from a module's client {@code preInit}, before the models are
   * baked.
   *
   * @param wrapper the wrapper
   */
  public static void register(Wrapper wrapper) {
    WRAPPERS.add(wrapper);
  }

  /**
   * Starts applying the registered wrappers. Called once from Core's client proxy.
   */
  public static synchronized void install() {
    if (installed) {
      return;
    }
    installed = true;
    MinecraftForge.EVENT_BUS.register(new CsmBakedModelWrappers.Events());
    installDynamicBakeListener();
  }

  /** Applies every wrapper in turn; the model itself when none applies. */
  static IBakedModel apply(ModelResourceLocation location, IBakedModel model) {
    IBakedModel out = model;
    for (Wrapper wrapper : WRAPPERS) {
      IBakedModel wrapped = wrapper.wrap(location, out);
      if (wrapped != null) {
        out = wrapped;
      }
    }
    return out;
  }

  /** The registry walk, for the game's own bake. */
  public static final class Events {

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
      if (WRAPPERS.isEmpty()) {
        return;
      }
      IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
      // Copied first: the loop replaces entries in the registry it walks.
      for (ModelResourceLocation key : new ArrayList<>(registry.getKeys())) {
        IBakedModel model = registry.getObject(key);
        if (model != null) {
          IBakedModel out = apply(key, model);
          if (out != model) {
            registry.putObject(key, out);
          }
        }
      }
    }
  }

  /**
   * Listens for VintageFix's per-model bake event, if VintageFix is installed. The listener goes
   * straight into the event's listener list, as the event bus itself would put it, since the
   * event's class cannot be named here.
   */
  private static void installDynamicBakeListener() {
    Class<?> type;
    try {
      type = Class.forName(DYNAMIC_BAKE_EVENT);
    } catch (ClassNotFoundException | LinkageError e) {
      return;
    }
    try {
      Field location = type.getField("location");
      Field bakedModel = type.getField("bakedModel");
      Field busId = EventBus.class.getDeclaredField("busID");
      busId.setAccessible(true);
      // Forge gives every event class a no-argument constructor as it loads it.
      Event prototype = (Event) type.getConstructor().newInstance();
      prototype.getListenerList().register(busId.getInt(MinecraftForge.EVENT_BUS),
          EventPriority.LOW, event -> {
            try {
              Object where = location.get(event);
              Object model = bakedModel.get(event);
              if (where instanceof ModelResourceLocation && model instanceof IBakedModel) {
                IBakedModel out = apply((ModelResourceLocation) where, (IBakedModel) model);
                if (out != model) {
                  bakedModel.set(event, out);
                }
              }
            } catch (IllegalAccessException e) {
              // Fields checked public above; nothing to do but leave the model.
            }
          });
      Csm.getLogger().info("Baked model wrappers: applying to VintageFix's dynamic bakes too");
    } catch (ReflectiveOperationException | RuntimeException e) {
      Csm.getLogger().warn("Baked model wrappers: VintageFix is installed but its bake event "
          + "could not be listened to; models it bakes will not be wrapped", e);
    }
  }
}
