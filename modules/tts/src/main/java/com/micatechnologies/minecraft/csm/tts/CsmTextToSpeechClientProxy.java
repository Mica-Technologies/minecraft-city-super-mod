package com.micatechnologies.minecraft.csm.tts;

import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Sided proxy for the Text to Speech module on the client side. This is where MaryTTS is
 * introduced to the rest of the mod: the engine is registered with the speech facade during
 * pre-initialization. It is not loaded then: MaryTTS takes about 45 MB of the heap and a second
 * and a half of a background thread, so it loads on first use instead (the first thing that
 * speaks, the Redstone TTS screen opening, or an announcing departure board coming into range).
 *
 * <p>This is client-only. {@link CsmTts} is {@code @SideOnly(CLIENT)} and there is no
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
}
