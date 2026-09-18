package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.CsmConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a door while it swings: both halves' own closed models, turned about the hinge.
 *
 * <p>Nothing is drawn by the models while a door swings (its {@code swing} state has none), so this
 * draws the leaf and whatever is fitted to it -- handles, push bar, closer -- from the very models
 * the door rests in, turned about the pivot {@code gen_doors.py} makes its open models with. A
 * quarter turn about that pivot IS the open model, so the swing ends on exactly the picture that
 * replaces it. With {@code animateDoors} off, the door is drawn where it is going at once.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityDoorSwingRenderer extends TileEntitySpecialRenderer<TileEntityDoorSwing> {

  /** SHARED with gen_doors.PIVOT: where a left-hinged leaf turns, with the inside to the north. */
  private static final double PIVOT_X = 0.875 / 16;
  private static final double PIVOT_Z = 15.125 / 16;

  @Override
  public void render(TileEntityDoorSwing te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    BlockPos upperPos = te.getPos();
    IBlockState upper = te.getWorld().getBlockState(upperPos);
    if (!(upper.getBlock() instanceof BlockBuildingDoor)) {
      return;
    }
    BlockBuildingDoor block = (BlockBuildingDoor) upper.getBlock();
    IBlockState door = upper.getActualState(te.getWorld(), upperPos);
    EnumFacing facing = door.getValue(BlockBuildingDoor.FACING);
    boolean left = door.getValue(BlockBuildingDoor.HINGE) == BlockBuildingDoor.Hinge.LEFT;
    double p = CsmConfig.isDoorAnimationEnabled() ? te.progress(partialTicks) : 1.0;
    double eased = 1 - (1 - p) * (1 - p);
    double openness = te.isOpening() ? eased : 1 - eased;
    float angle = (float) (90.0 * openness) * (left ? 1 : -1);

    // The closed models, facing north, so the facing and the hinge turn are both applied here.
    IBlockState closed = block.getDefaultState().withProperty(BlockBuildingDoor.OPEN, false)
        .withProperty(BlockBuildingDoor.SWING, false)
        .withProperty(BlockBuildingDoor.HINGE, door.getValue(BlockBuildingDoor.HINGE))
        .withProperty(BlockBuildingDoor.CLOSER, door.getValue(BlockBuildingDoor.CLOSER));
    BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
    int light = te.getWorld().getCombinedLight(upperPos.down(), 0);

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.pushMatrix();
    RenderHelper.disableStandardItemLighting();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >> 16);
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(facing.getHorizontalAngle() + 180F), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    double px = left ? PIVOT_X : 1 - PIVOT_X;
    GlStateManager.translate(px, 0, PIVOT_Z);
    GlStateManager.rotate(angle, 0F, 1F, 0F);
    GlStateManager.translate(-px, 0, -PIVOT_Z);
    for (BlockBuildingDoor.Half half : BlockBuildingDoor.Half.values()) {
      IBlockState s = closed.withProperty(BlockBuildingDoor.HALF, half);
      IBakedModel model = dispatcher.getModelForState(s);
      GlStateManager.pushMatrix();
      // The TE is on the upper half; the lower is a block below it.
      GlStateManager.translate(0, half == BlockBuildingDoor.Half.LOWER ? -1 : 0, 0);
      dispatcher.getBlockModelRenderer().renderModelBrightnessColor(s, model, 1F, 1F, 1F, 1F);
      GlStateManager.popMatrix();
    }
    GlStateManager.disableBlend();
    RenderHelper.enableStandardItemLighting();
    GlStateManager.popMatrix();
  }
}
