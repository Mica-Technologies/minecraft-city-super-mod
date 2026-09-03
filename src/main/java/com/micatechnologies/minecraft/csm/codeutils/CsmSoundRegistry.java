package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmConstants;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;

/**
 * The mod's sound registrar. Every module hands Core the names of the sounds it ships and Core
 * registers the union of them from its own registry-event listener.
 *
 * <p>Core registers them because Forge takes the registry name prefix from the mod container
 * that owns the listener: registering from Core keeps every sound event named
 * {@code csm:&lt;name&gt;} no matter which jar the {@code .ogg} came from, which is what saved
 * selections, {@code sounds.json} and the fire alarm packets all refer to.</p>
 *
 * <p>Modules register their names from their {@code preInit}. That is early enough: Forge runs
 * every mod's {@code preInit} before it fires the {@link RegistryEvent.Register} events, so the
 * union is complete by the time {@link #registerAll(RegistryEvent.Register)} runs.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public class CsmSoundRegistry {

  /**
   * The names of the sounds to register, in the order they were handed over. A set, so a name
   * offered twice is still registered once — Forge throws on a duplicate registry name.
   *
   * @since 1.0
   */
  private static final Set<String> SOUND_NAMES = new LinkedHashSet<>();

  /**
   * Registers the given sound names for creation during the sound registry event.
   *
   * @param soundNames the names of the sounds to register
   *
   * @since 1.0
   */
  public static void register(String... soundNames) {
    Collections.addAll(SOUND_NAMES, soundNames);
  }

  /**
   * Registers the names of the given sounds for creation during the sound registry event. This
   * is the overload a module's sound enum uses: {@code CsmSoundRegistry.register(X.values())}.
   *
   * @param sounds the sounds to register
   *
   * @since 1.0
   */
  public static void register(ICsmSound... sounds) {
    for (ICsmSound sound : sounds) {
      SOUND_NAMES.add(sound.getSoundName());
    }
  }

  /**
   * Gets the names of every sound registered so far, in registration order.
   *
   * @return the registered sound names
   *
   * @since 1.0
   */
  public static Collection<String> getRegisteredSoundNames() {
    return Collections.unmodifiableCollection(SOUND_NAMES);
  }

  /**
   * Creates and registers a sound event for every registered sound name. This method is called
   * from Core's sound registry event listener and should not be called from anywhere else.
   *
   * @param event the sound registry event
   *
   * @since 1.0
   */
  public static void registerAll(RegistryEvent.Register<SoundEvent> event) {
    for (String soundName : SOUND_NAMES) {
      final ResourceLocation soundResourceLocation =
          new ResourceLocation(CsmConstants.MOD_NAMESPACE, soundName);
      final SoundEvent soundEvent =
          new SoundEvent(soundResourceLocation).setRegistryName(soundResourceLocation);
      event.getRegistry().register(soundEvent);
    }
  }
}
