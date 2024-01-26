package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class TrafficSignalTESR extends TileEntitySpecialRenderer<TileEntityTrafficSignalHead> {

  @Override
  public void renderTileEntityFast(TileEntityTrafficSignalHead te, double x, double y, double z,
      float partialTicks, int destroyStage, float partial, BufferBuilder buffer) {
    // Get color of the body
    float red = 0.0F;
    float green = 0.0F;
    float blue = 0.0F;
    switch (te.getBodyColor()) {
      case BATTLESHIP_GRAY -> {
        red = 0.361F;
        green = 0.361F;
        blue = 0.361F;
      }
      case YELLOW -> {
        red = 1.0F;
        green = 1.0F;
        blue = 0.0F;
      }
      case DARK_OLIVE_GREEN -> {
        red = 0.333F;
        green = 0.420F;
        blue = 0.184F;
      }
      default -> {
        red = 0.0F;
        green = 0.0F;
        blue = 0.0F;
      }
    }

    // Get facing direction
    DirectionSixteen facing = te.getFacing();
    float rotation = facing.getRotation();

    // Render the body
    renderBody(red, green, blue, rotation, "traffic_signals:models/block/traffic_signal_body",
        "traffic_signals:models/block/traffic_signal_visor",
        "traffic_signals:models/block/traffic_signal_louver",
        3);
  }

  public static void renderBody(float red, float green, float blue, float rotation,
      String bodyModelId, String visorTypeModelId, String louverTypeModelId, int bulbCount) {
    // Push the matrix
    GlStateManager.pushMatrix();

    // Model retrieval and rendering logic (pseudo-code)
    IBakedModel bodyModel = getModelFromRegistry(bodyModelId);
    IBakedModel visorModel =
        visorTypeModelId != null ? getModelFromRegistry(visorTypeModelId) : null;
    IBakedModel louverModel =
        louverTypeModelId != null ? getModelFromRegistry(louverTypeModelId) : null;

    // Setup based on bulbCount
    int startY = 0;
    int height = 16; // Total height for a single bulb, adjust as needed
    if (bulbCount == 1) {
      startY = 2; // Center single bulb
    } else if (bulbCount == 2) {
      startY = 0; // Start at bottom
      height = 24; // Adjust total height to accommodate 2 bulbs
    } else if (bulbCount == 3) {
      startY = -10; // Center three bulbs
      height = 36; // Adjust total height to accommodate 3 bulbs
    }

    // Apply translation to align the back of the signal body with the back of the block
    // Adjust translation based on your model's dimensions and orientation
    GlStateManager.translate(0, startY / 16.0f, 0);

    // Apply rotation
    GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);

    // Apply color
    GlStateManager.color(red, green, blue, 1.0F);

    // Render each bulb section
    for (int i = 0; i < bulbCount; i++) {
      // Render body section
      renderModel(bodyModel);

      // Render visor for this section if applicable
      if (visorModel != null) {
        renderModel(visorModel);
      }

      // Render louver for this section if applicable
      if (louverModel != null) {
        renderModel(louverModel);
      }

      // Translate to the next section position
      GlStateManager.translate(0, height / (float) bulbCount, 0);
    }

    // Reset color to avoid affecting subsequent renders
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    // Pop the matrix
    GlStateManager.popMatrix();
  }

  private static IBakedModel getModelFromRegistry(String modelId) {
    // Implementation depends on how your model registry is set up
    // This is pseudo-code
    return Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager()
        .getModel(new ModelResourceLocation(modelId, "inventory"));
  }

  private static void renderModel(IBakedModel model) {
    // Render the model
    // This is simplified pseudo-code; actual rendering will depend on your setup
    // For example, you might use Minecraft.getMinecraft().getRenderItem().renderItem() methods
    // or similar
  }
}
