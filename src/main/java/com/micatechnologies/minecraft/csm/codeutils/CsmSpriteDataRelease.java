package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Once the block atlas is uploaded and the models are baked, lets go of the pixel copies CSM's
 * still sprites keep in the Java heap.
 *
 * <p>Vanilla keeps every sprite's pixels, at every mipmap level, in
 * {@link TextureAtlasSprite}'s frame data after it has uploaded them to the atlas texture. Only
 * two things read them afterwards: the animation tick, which uploads the next frame of an
 * animated sprite, and a bake that turns a sprite's pixels into geometry (an item drawn from its
 * texture, {@code builtin/generated}). The atlas is in video memory and draws from there. CSM's
 * sprites cover three quarters of an 8192 by 8192 atlas, so their copies were about 250 MB of
 * the heap that nothing used.</p>
 *
 * <p>After the bake ({@link ModelBakeEvent}, lowest priority, so every bake that reads pixels
 * has run), this empties the frame data of every {@code csm:} sprite that is not animated,
 * with vanilla's own {@link TextureAtlasSprite#clearFramesTextureData()}. Animated sprites keep
 * theirs. A sprite's size, position and UVs are separate fields and are untouched. Other mods'
 * and vanilla's sprites are left alone, since some of them are baked again later (Forge's
 * dynamic bucket reads its sprites' pixels each time it meets a new fluid).</p>
 *
 * <p>A resource reload stitches a new atlas from new sprite objects, reading every texture from
 * its file again, and this reruns after its bake. Changing the mipmap level in the video
 * settings is such a reload.</p>
 *
 * <p><b>The rule this makes:</b> never read a CSM sprite's pixels after the bake
 * ({@link TextureAtlasSprite#getFrameTextureData(int)} on a still sprite: its frame count is 0).
 * Code that needs a texture's pixels reads the PNG from the resource manager itself; nothing in
 * CSM reads an atlas sprite's pixels today.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmSpriteDataRelease {

  /**
   * Empties the frame data of CSM's still sprites.
   *
   * @param event the bake event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onModelBake(ModelBakeEvent event) {
    long start = System.nanoTime();
    try {
      TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
      @SuppressWarnings("unchecked")
      Map<String, TextureAtlasSprite> uploaded = (Map<String, TextureAtlasSprite>)
          ObfuscationReflectionHelper.findField(TextureMap.class, "field_94252_e").get(atlas);
      String prefix = CsmConstants.MOD_NAMESPACE + ":";
      int released = 0;
      int animated = 0;
      long pixels = 0;
      for (TextureAtlasSprite sprite : uploaded.values()) {
        if (sprite == null || !sprite.getIconName().startsWith(prefix)
            || sprite.getFrameCount() == 0) {
          continue;
        }
        if (sprite.hasAnimationMetadata()) {
          animated++;
          continue;
        }
        pixels += (long) sprite.getIconWidth() * sprite.getIconHeight();
        sprite.clearFramesTextureData();
        released++;
      }
      Csm.getLogger().info("Released the pixel data of {} still CSM sprites ({} Mpx at full "
              + "size) after upload; {} animated sprites keep theirs, {} ms", released,
          pixels / 1_000_000L, animated, (System.nanoTime() - start) / 1_000_000L);
    } catch (Throwable t) {
      // An atlas whose internals differ: keep the data, which only costs memory.
      Csm.getLogger().warn("Could not release sprite pixel data; textures are unaffected", t);
    }
  }
}
