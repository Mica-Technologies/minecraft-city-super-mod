package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Half;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Hinge;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The client side of custom doors: puts {@link CustomDoorBakedModel} in place of the block's model,
 * and draws the doors that are moving.
 *
 * <p>Moving doors are drawn once a frame from {@link CustomDoorMotion}, after the world, from the
 * same quads the door rests in (its closed pose) under the movement's transform: a swing about the
 * hinge pivot, a slide by the fraction of the way it has come. Not a tile entity renderer: a TESR is
 * looked at for every door every frame, moving or not, where this looks only at the few moving now.
 * When a move is over the door leaves the registry and its chunk is redrawn, and the baked model
 * draws it where it came to rest.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CustomDoorRenderer {

  private static final ModelResourceLocation MODEL =
      new ModelResourceLocation("csm:custom_door", "normal");

  private static CustomDoorBakedModel model = new CustomDoorBakedModel();

  private CustomDoorRenderer() {
  }

  /**
   * Hooks it up: every state of the block to the one model location the baked model replaces, and
   * the event handlers.
   *
   * @param block the custom door block
   *
   * @since 1.0
   */
  public static void register(BlockCustomDoor block) {
    ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
      @Override
      protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
        return MODEL;
      }
    });
    MinecraftForge.EVENT_BUS.register(new CustomDoorRenderer.Events());
  }

  /**
   * Draws a door -- both halves, from the lower block's corner, its outside to the south as the
   * baked model has it -- part way open. The texture atlas must be bound. Used for moving doors in
   * the world and for the Door Workshop's preview, so the two cannot disagree.
   *
   * @param settings the door
   * @param hinge    its hinge side
   * @param paired   whether it is half of a pair
   * @param openness how far open, 0 to 1
   *
   * @since 1.0
   */
  public static void drawDoor(CustomDoorSettings settings, Hinge hinge, boolean paired,
      double openness) {
    for (Half half : Half.values()) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(0, half == Half.UPPER ? 1 : 0, 0);
      float[] d = CustomDoorGeometry.slide(settings.movement(), half, hinge, paired);
      if (d == null) {
        boolean left = hinge == Hinge.LEFT;
        double px = (left ? CustomDoorGeometry.PIVOT_X : 16 - CustomDoorGeometry.PIVOT_X) / 16;
        double pz = CustomDoorGeometry.PIVOT_Z / 16;
        GlStateManager.translate(px, 0, pz);
        GlStateManager.rotate((float) (90 * openness) * (left ? 1 : -1), 0F, 1F, 0F);
        GlStateManager.translate(-px, 0, -pz);
      } else {
        GlStateManager.translate(d[0] / 16 * openness, d[1] / 16 * openness,
            d[2] / 16 * openness);
      }
      List<BakedQuad> quads = model.quads(settings, half, EnumFacing.NORTH, hinge, false,
          paired, null);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
      for (BakedQuad quad : quads) {
        LightUtil.renderQuadColor(buffer, quad, 0xFFFFFFFF);
      }
      tessellator.draw();
      GlStateManager.popMatrix();
    }
  }

  /**
   * The event handlers.
   *
   * @since 1.0
   */
  public static final class Events {

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
      model = new CustomDoorBakedModel();
      event.getModelRegistry().putObject(MODEL, model);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
      if (event.getWorld().isRemote) {
        CustomDoorMotion.clear();
      }
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
      Map<BlockPos, CustomDoorMotion.Move> moving = CustomDoorMotion.all();
      if (moving.isEmpty()) {
        return;
      }
      Minecraft mc = Minecraft.getMinecraft();
      World world = mc.world;
      Entity view = mc.getRenderViewEntity();
      if (world == null || view == null) {
        return;
      }
      float pt = event.getPartialTicks();
      // Not world time + pt: that sum is a float, which cannot hold an old world's tick count.
      double now = TileEntityGarageDoor.clock(world.getTotalWorldTime(), pt);
      double cx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
      double cy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
      double cz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;

      mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
      RenderHelper.disableStandardItemLighting();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
          GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      GlStateManager.enableAlpha();
      Iterator<Map.Entry<BlockPos, CustomDoorMotion.Move>> it = moving.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<BlockPos, CustomDoorMotion.Move> e = it.next();
        BlockPos pos = e.getKey();
        CustomDoorMotion.Move move = e.getValue();
        IBlockState lower = world.getBlockState(pos);
        if (move.done(now) || !(lower.getBlock() instanceof BlockCustomDoor)) {
          it.remove();
          world.markBlockRangeForRenderUpdate(pos, pos.up());
          continue;
        }
        draw(world, pos, (BlockCustomDoor) lower.getBlock(), move,
            CsmConfig.isDoorAnimationEnabled() ? move.openness(now) : (move.opening ? 1 : 0),
            cx, cy, cz);
      }
      GlStateManager.disableBlend();
      RenderHelper.enableStandardItemLighting();
    }

    private void draw(World world, BlockPos pos, BlockCustomDoor block,
        CustomDoorMotion.Move move, double openness, double cx, double cy, double cz) {
      IBlockState door = block.getActualState(world.getBlockState(pos), world, pos);
      IBlockState ext = block.getExtendedState(door, world, pos);
      CustomDoorSettings settings = BlockCustomDoor.settings(world, pos);
      boolean paired = ext instanceof IExtendedBlockState && Boolean.TRUE.equals(
          ((IExtendedBlockState) ext).getValue(BlockCustomDoor.PAIRED));
      EnumFacing facing = door.getValue(BlockBuildingDoor.FACING);
      Hinge hinge = door.getValue(BlockBuildingDoor.HINGE);
      int light = world.getCombinedLight(pos, 0);
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
          light >> 16);

      GlStateManager.pushMatrix();
      GlStateManager.translate(pos.getX() - cx + 0.5, pos.getY() - cy, pos.getZ() - cz + 0.5);
      GlStateManager.rotate(-(facing.getHorizontalAngle() + 180F), 0F, 1F, 0F);
      GlStateManager.translate(-0.5, 0, -0.5);
      drawDoor(settings, hinge, paired, openness);
      GlStateManager.popMatrix();
    }
  }
}
