package com.micatechnologies.minecraft.csm.venues.lines;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Puts {@link LineArtBakedModel} round every court and track anchor's and painted cell's baked
 * model (through {@link CsmBakedModelWrappers}, so it holds under VintageFix's dynamic bakes),
 * puts every sprite a material names on the block atlas, and keeps the painted cells' quads,
 * cached by {@link LineArtKey}: a 400 m track is about 5,700 different cells, a few quads each,
 * baked as each is first drawn. The cache is emptied at every bake (the sprites move).
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public final class LineArtModels {

  /** Painted cells on a whole track and a few courts, with room to spare. */
  private static final int CACHE = 24_576;

  private static final Cache<LineArtKey, LineArtBakedModel.Quads> CACHED =
      CacheBuilder.newBuilder().maximumSize(CACHE).build();

  private static volatile Map<String, TextureAtlasSprite> sprites = new HashMap<>();
  private static volatile Set<String> wrapped;

  private LineArtModels() {
  }

  /** Registers the handlers; called from the venues client proxy's pre-initialization. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new Events());
    CsmBakedModelWrappers.register(LineArtModels::wrap);
  }

  /** Every texture a material can name, in any family. */
  static Set<String> textures() {
    Set<String> out = new HashSet<>();
    for (LineArtFamily fam : LineArtFamily.ALL) {
      for (LineArtMaterial m : LineArtMaterials.ALL) {
        out.add(fam.sprite(m.sprite));
      }
    }
    return out;
  }

  /** A key's quads, painted on first use; null before the sprites are known. */
  @Nullable
  static LineArtBakedModel.Quads quads(LineArtKey key) {
    Map<String, TextureAtlasSprite> s = sprites;
    if (s.isEmpty()) {
      return null;
    }
    try {
      return CACHED.get(key, () -> LineArtBakedModel.bake(key, name -> {
        TextureAtlasSprite sp = s.get(name);
        return sp != null ? sp : Minecraft.getMinecraft().getTextureMapBlocks()
            .getMissingSprite();
      }));
    } catch (ExecutionException | RuntimeException e) {
      return null;
    }
  }

  @Nullable
  private static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof LineArtBakedModel) {
      return null;
    }
    Set<String> names = wrapped;
    if (names == null) {
      names = new HashSet<>();
      for (Block block : CsmRegistry.getBlocks()) {
        if (block instanceof BlockLineCell && block.getRegistryName() != null) {
          names.add(block.getRegistryName().toString());
        }
      }
      wrapped = names;
    }
    String name = location.getNamespace() + ":" + location.getPath();
    return names.contains(name) ? new LineArtBakedModel(model) : null;
  }

  /** The event handlers. */
  public static final class Events {

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
      for (String t : textures()) {
        event.getMap().registerSprite(new ResourceLocation(t));
      }
    }

    /** Reads the sprites before any model is wrapped, and drops the old quads. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onModelBake(ModelBakeEvent event) {
      Map<String, TextureAtlasSprite> found = new HashMap<>();
      for (String t : textures()) {
        found.put(t, Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(t));
      }
      sprites = found;
      CACHED.invalidateAll();
    }
  }
}
