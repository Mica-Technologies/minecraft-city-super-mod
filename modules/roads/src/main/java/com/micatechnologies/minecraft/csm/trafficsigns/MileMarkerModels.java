package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Puts {@link MileMarkerBakedModel} round every baked variant of every mile marker, the
 * inventory one included, and makes sure the glyph sheet is on the block atlas.
 *
 * <p>No custom state mapper: one registered in pre-initialization is keyed on a registry name
 * that does not exist yet (see {@code PEDESTAL_POLE_SYSTEM.md}). The variants the blockstate
 * already bakes are wrapped where they are, found by their registry name.</p>
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

  /** Registers the event handlers; called from the Roads client proxy's pre-initialization. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new MileMarkerModels());
  }

  /**
   * The blockstates name the sheet already, which puts it on the atlas; this makes it
   * independent of how a blockstate's unused texture keys are treated.
   */
  @SubscribeEvent
  public void onTextureStitch(TextureStitchEvent.Pre event) {
    event.getMap().registerSprite(new ResourceLocation(MileMarkerLayout.SHEET));
  }

  @SubscribeEvent
  public void onModelBake(ModelBakeEvent event) {
    Map<String, MileMarkerLayout> markers = new HashMap<>();
    for (Block block : CsmRegistry.getBlocks()) {
      if (block instanceof BlockMileMarkerSign && block.getRegistryName() != null) {
        markers.put(block.getRegistryName().toString(), ((BlockMileMarkerSign) block).getLayout());
      }
    }
    if (markers.isEmpty()) {
      return;
    }
    TextureAtlasSprite sheet = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite(MileMarkerLayout.SHEET);
    Map<GuideSignShieldType, TextureAtlasSprite> shields = shieldFaces(event);
    // Copied first: the loop replaces entries in the registry it walks.
    for (ModelResourceLocation key : new ArrayList<>(event.getModelRegistry().getKeys())) {
      MileMarkerLayout layout = markers.get(key.getNamespace() + ":" + key.getPath());
      if (layout == null) {
        continue;
      }
      IBakedModel model = event.getModelRegistry().getObject(key);
      if (model != null && !(model instanceof MileMarkerBakedModel)) {
        event.getModelRegistry().putObject(key,
            new MileMarkerBakedModel(model, layout, sheet, shields));
      }
    }
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
