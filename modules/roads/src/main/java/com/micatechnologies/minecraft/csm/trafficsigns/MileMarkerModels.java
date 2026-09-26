package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
 * Puts {@link MileMarkerBakedModel} round every baked variant of every mile marker, the
 * inventory one included, and makes sure the glyph sheet is on the block atlas.
 *
 * <p>No custom state mapper: one registered in pre-initialization is keyed on a registry name
 * that does not exist yet (see {@code PEDESTAL_POLE_SYSTEM.md}). The variants the blockstate
 * already bakes are wrapped where they are, found by their registry name, through
 * {@link CsmBakedModelWrappers}: walking the model registry here instead finds nothing in a pack
 * running VintageFix, whose models are baked as they are drawn, and the number would never be
 * drawn. What the wrapper needs from other models (the glyph sheet, the shield faces) is read in
 * the bake event, which comes before any model the wrapper sees.</p>
 *
 * <p>The route shields are the Dynamic Route Marker Sign's own faces, read off that sign's baked
 * models rather than named here, because several shields share one texture and which one is the
 * route marker generator's business.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class MileMarkerModels {

  private static final String ROUTE_MARKER = "csm:dynamic_route_marker_sign";

  private MileMarkerModels() {
  }

  /** The glyph sheet and each shield's face, read at the latest bake. */
  private static volatile Sprites sprites;

  /** The mile markers' layouts by registry name, found on first use. */
  private static volatile Map<String, MileMarkerLayout> markers;

  /** Registers the event handlers; called from the Roads client proxy's pre-initialization. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new MileMarkerModels());
    CsmBakedModelWrappers.register(MileMarkerModels::wrap);
  }

  private static final class Sprites {

    final TextureAtlasSprite sheet;
    final Map<GuideSignShieldType, TextureAtlasSprite> shields;

    Sprites(TextureAtlasSprite sheet, Map<GuideSignShieldType, TextureAtlasSprite> shields) {
      this.sheet = sheet;
      this.shields = shields;
    }
  }

  /**
   * The blockstates name the sheet already, which puts it on the atlas; this makes it
   * independent of how a blockstate's unused texture keys are treated.
   */
  @SubscribeEvent
  public void onTextureStitch(TextureStitchEvent.Pre event) {
    event.getMap().registerSprite(new ResourceLocation(MileMarkerLayout.SHEET));
  }

  /**
   * Reads the sprites the wrapper draws with, before any model is wrapped: the game's own wrapping
   * runs after this at normal priority, and VintageFix's after the event.
   */
  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onModelBake(ModelBakeEvent event) {
    TextureAtlasSprite sheet = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite(MileMarkerLayout.SHEET);
    sprites = new Sprites(sheet, shieldFaces(event));
  }

  /** The legend wrapper round a mile marker's baked model; {@code null} for any other model. */
  @Nullable
  private static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    Sprites s = sprites;
    if (s == null || model instanceof MileMarkerBakedModel) {
      return null;
    }
    MileMarkerLayout layout = markers().get(location.getNamespace() + ":" + location.getPath());
    return layout == null ? null : new MileMarkerBakedModel(model, layout, s.sheet, s.shields);
  }

  private static Map<String, MileMarkerLayout> markers() {
    Map<String, MileMarkerLayout> found = markers;
    if (found == null) {
      found = new HashMap<>();
      for (Block block : CsmRegistry.getBlocks()) {
        if (block instanceof BlockMileMarkerSign && block.getRegistryName() != null) {
          found.put(block.getRegistryName().toString(), ((BlockMileMarkerSign) block).getLayout());
        }
      }
      markers = found;
    }
    return found;
  }

  /** Each shield's face sprite, off the route marker sign's own baked models. */
  private static Map<GuideSignShieldType, TextureAtlasSprite> shieldFaces(ModelBakeEvent event) {
    Map<GuideSignShieldType, TextureAtlasSprite> out = new EnumMap<>(GuideSignShieldType.class);
    for (GuideSignShieldType shield : GuideSignShieldType.values()) {
      IBakedModel model = event.getModelRegistry().getObject(new ModelResourceLocation(
          ROUTE_MARKER, "downward=false,facing=n,shield=" + shield.getName() + ",shift=none"));
      if (model == null) {
        continue;
      }
      for (BakedQuad quad : model.getQuads(null, null, 0L)) {
        String name = quad.getSprite().getIconName();
        if (name.contains("trafficsigns/route_marker_") && !name.endsWith("_back")) {
          out.put(shield, quad.getSprite());
          break;
        }
      }
    }
    return out;
  }
}
