package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmConstants;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

/**
 * A sound owned by a module. Module sound enums implement this so that a caller can go from a
 * constant to the {@link SoundEvent} Forge registered for it, exactly as the single mod-wide
 * sound enum used to allow.
 *
 * <p>The resource location is always in the {@code csm} namespace regardless of which module
 * jar carries the sound file: the registry names are user-visible state (they are what
 * {@code sounds.json} and every saved selection refer to), and Core owns the registration, so
 * the namespace never becomes the module's mod id.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public interface ICsmSound {

  /**
   * Gets the name of the sound, which is both its {@code sounds.json} key and the path of its
   * registry name.
   *
   * @return the name of the sound
   *
   * @since 1.0
   */
  String getSoundName();

  /**
   * Gets the resource location of the sound.
   *
   * @return the resource location of the sound
   *
   * @since 1.0
   */
  default ResourceLocation getSoundLocation() {
    return new ResourceLocation(CsmConstants.MOD_NAMESPACE, getSoundName());
  }

  /**
   * Gets the sound event for this sound.
   *
   * <p>This is a registry lookup, so it only returns a sound event once Forge has fired the
   * sound registry event. That is the same constraint the mod-wide sound enum had; every
   * caller reads a sound while the game is running, long after registration.</p>
   *
   * @return the sound event, or {@code null} if the sound has not been registered
   *
   * @since 1.0
   */
  default SoundEvent getSoundEvent() {
    return SoundEvent.REGISTRY.getObject(getSoundLocation());
  }
}
