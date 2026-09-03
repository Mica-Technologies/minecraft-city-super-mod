package com.micatechnologies.minecraft.csm.tts;

import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Sided proxy for the Text to Speech module on the client side. This is where MaryTTS is
 * introduced to the rest of the mod: the engine is registered with the speech facade during
 * pre-initialization and starts loading during post-initialization, which is when the mod
 * started loading it before this module existed.
 *
 * <p>Both steps are client-only. {@link CsmTts} is {@code @SideOnly(CLIENT)} and there is no
 * audio device to speak through on a dedicated server, so the common proxy does nothing.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class CsmTextToSpeechClientProxy extends CsmTextToSpeechCommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    CsmTts.setEngine(new MaryTtsEngine());
  }

  @Override
  public void postInit(FMLPostInitializationEvent event) {
    CsmTts.startInit();
  }
}
