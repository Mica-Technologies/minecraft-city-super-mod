package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.codeutils.CsmSoundRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;

/**
 * The sounds shipped by the CSM: Sports &amp; Venues module, all synthesised by
 * {@code dev-env-utils/scripts/gen_venues_sounds.py}: the ticket scanner's accept and reject
 * tones and the stadium turnstile's ratchet as its arms index round.
 *
 * <p>Each constant's name is the sound's {@code sounds.json} key and the path of its registry
 * name, so a sound event is {@code csm:&lt;key&gt;}.</p>
 *
 * @since 2026.10
 */
public enum VenuesSounds implements ICsmSound {
  VENUE_SCANNER_ACCEPT("venue_scanner_accept"),
  VENUE_SCANNER_DENY("venue_scanner_deny"),
  VENUE_TURNSTILE("venue_turnstile");

  private final String soundName;

  VenuesSounds(String soundName) {
    this.soundName = soundName;
  }

  @Override
  public String getSoundName() {
    return soundName;
  }

  /**
   * Hands every sound in this enum to Core's sound registrar. Called from the module's
   * {@code preInit}, which Forge runs before it fires the sound registry event.
   */
  public static void registerSounds() {
    CsmSoundRegistry.register(values());
  }
}
