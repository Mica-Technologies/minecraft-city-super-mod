package com.micatechnologies.minecraft.csm.signage;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/**
 * Binds ad textures, filtered smoothly and mipmapped.
 *
 * <p>An ad is its own texture, loaded the first time a board shows it, rather than a sprite in the
 * block atlas: 33 ads in four shapes at up to 1024 pixels would crowd the atlas for everything
 * else. The texture manager loads such a texture with nearest filtering and no mipmaps, which is
 * right for a block's sixteen pixels and wrong for an ad's thousand: seen from across a street,
 * a 1024-pixel ad squeezed onto a few hundred screen pixels shimmers and its text crawls. So the
 * first bind of each texture turns on trilinear filtering and builds its mipmaps.</p>
 *
 * <p>A resource reload uploads every texture afresh, without mipmaps, so {@link #reset()} is
 * called after one and the next bind builds them again.</p>
 */
@SideOnly(Side.CLIENT)
public final class AdTextures {

  private static final Map<ITextureObject, Boolean> PREPARED =
      Collections.synchronizedMap(new WeakHashMap<>());

  private AdTextures() {
  }

  /** Binds {@code texture}, preparing it on its first bind. */
  public static void bind(ResourceLocation texture) {
    TextureManager manager = Minecraft.getMinecraft().getTextureManager();
    manager.bindTexture(texture);
    ITextureObject object = manager.getTexture(texture);
    if (object == null || PREPARED.containsKey(object)) {
      return;
    }
    PREPARED.put(object, Boolean.TRUE);
    if (GLContext.getCapabilities().OpenGL30) {
      // The texture manager allocates a plain texture with its top mip level and its greatest
      // LOD both 0, so mipmaps made without lifting them are never sampled: the LED grid aliased
      // into dark bands across a screen at range until these were raised.
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 1000);
      GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 1000F);
      GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
          GL11.GL_LINEAR_MIPMAP_LINEAR);
    } else {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    }
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
  }

  /** Forgets which textures were prepared: after a resource reload they are all new uploads. */
  public static void reset() {
    PREPARED.clear();
  }
}
