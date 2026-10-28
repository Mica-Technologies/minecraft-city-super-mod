package com.micatechnologies.minecraft.csm.healthcare;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The block atlas sprites the healthcare renderers draw their screens with, looked up once after
 * each stitch rather than by name every frame ({@code getAtlasSprite} is a string-keyed map
 * lookup). A stitch makes new sprite objects, so {@link Events} forgets them when one finishes and
 * the next frame looks them up again.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public final class HealthcareScreenSprites {

  private static final String ED_BOARD = "csm:blocks/healthcare/clinical/ed_board_screen";
  private static final String NOW_SERVING = "csm:blocks/healthcare/patient/now_serving_screen";

  private static TextureAtlasSprite edBoard;
  private static TextureAtlasSprite nowServing;

  private HealthcareScreenSprites() {
  }

  /** The ED tracking board's screen. */
  public static TextureAtlasSprite edBoard() {
    TextureAtlasSprite sprite = edBoard;
    if (sprite == null) {
      sprite = edBoard = lookUp(ED_BOARD);
    }
    return sprite;
  }

  /** The now-serving display's screen. */
  public static TextureAtlasSprite nowServing() {
    TextureAtlasSprite sprite = nowServing;
    if (sprite == null) {
      sprite = nowServing = lookUp(NOW_SERVING);
    }
    return sprite;
  }

  private static TextureAtlasSprite lookUp(String name) {
    return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
  }

  /** Forgets every sprite, so the next frame looks them up in the new atlas. */
  public static void forget() {
    edBoard = null;
    nowServing = null;
  }

  /** Forgets the sprites when the block atlas has been stitched again. */
  public static final class Events {

    /**
     * Forgets the sprites once the block atlas has been stitched.
     *
     * @param event the stitch event
     */
    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Post event) {
      // Any atlas: a lookup after a stitch costs one map read, a stale sprite a wrong screen.
      forget();
    }
  }
}
