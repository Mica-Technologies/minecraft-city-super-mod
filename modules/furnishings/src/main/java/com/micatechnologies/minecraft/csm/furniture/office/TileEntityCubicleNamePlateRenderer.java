package com.micatechnologies.minecraft.csm.furniture.office;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Prints the words on a cubicle panel's name plate or sign. The holder and the blank insert are
 * the panel's baked model, so this draws only the text: a few dozen glyph quads, and nothing at
 * all for a blank plate or past {@link TileEntityCubicleNamePlate}'s render distance.
 *
 * <p>The print is the game's own font, as a vanilla sign's is: crisp at the sizes a plate is
 * read at, and always there. (CSM's Highway Gothic font ships with the Roads module, which this
 * module does not require.) It takes the world's light as the plate does: the tile entity
 * dispatcher has already set the lightmap from the panel's own cell, which a panel does not
 * shade. A line too long for the insert is condensed rather than shrunk, as a printed insert
 * would be. Where everything sits is {@link CubicleSignStyle}'s, which repeats the generator's
 * numbers; whether a sign is drawn wide is read from the world at most once a second and kept on
 * the tile entity, since placing a panel beside it fires nothing here.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityCubicleNamePlateRenderer
    extends TileEntitySpecialRenderer<TileEntityCubicleNamePlate> {

  /** The height of the game font's capitals, in font units, from the top of a row. */
  private static final float CAP_UNITS = 7.0f;
  private static final long WIDE_REFRESH_TICKS = 20L;

  @Override
  public void render(TileEntityCubicleNamePlate te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    World world = te.getWorld();
    if (world == null) {
      return;
    }
    boolean any = false;
    for (int i = 0; i < TileEntityCubicleNamePlate.MAX_LINES && !any; i++) {
      any = !te.getLine(i).isEmpty();
    }
    if (!any) {
      return;
    }
    IBlockState state = world.getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof BlockCubiclePanelNamed)) {
      return;
    }
    CubicleSignStyle style = ((BlockCubiclePanelNamed) block).getStyle();
    if (style.canWiden()) {
      long now = world.getTotalWorldTime();
      if (te.wideReadAt < 0 || now - te.wideReadAt >= WIDE_REFRESH_TICKS
          || now < te.wideReadAt) {
        te.wideReadAt = now;
        te.wide = state.getActualState(world, te.getPos())
            .getValue(BlockCubiclePanelNamed.WIDE);
      }
    }
    CubicleSignStyle.Layout layout = style.getLayout(te.wide);
    EnumFacing facing = state.getValue(BlockCubiclePanelNamed.FACING);
    FontRenderer font = Minecraft.getMinecraft().fontRenderer;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // Turned so that the model's plate (on its north face) faces +z here; a model point
    // (mx, my, mz) is then ((8 - mx) / 16, my / 16, (8 - mz) / 16).
    GlStateManager.rotate(-facing.getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate(0, 0,
        (8 - CubicleSignStyle.FACE_Z + CubicleSignStyle.PRINT_LIFT) / 16.0);
    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);

    double maxWidth = layout.getPrintWidth();
    int lines = Math.min(layout.getLineCount(), TileEntityCubicleNamePlate.MAX_LINES);
    for (int i = 0; i < lines; i++) {
      String text = te.getLine(i);
      if (text.isEmpty()) {
        continue;
      }
      CubicleSignStyle.Line line = layout.getLine(i);
      int width = font.getStringWidth(text);
      // Font units to sixteenths.
      double scale = line.getCapHeight() / CAP_UNITS;
      double scaleX = Math.min(scale, maxWidth / Math.max(1, width));
      GlStateManager.pushMatrix();
      GlStateManager.translate(0, line.getCentreY() / 16.0, 0);
      GlStateManager.scale(scaleX / 16.0, -scale / 16.0, scale / 16.0);
      // The capitals' middle on the line's middle, the text centred across.
      GlStateManager.translate(-width / 2.0, -CAP_UNITS / 2.0, 0);
      font.drawString(text, 0, 0, line.getColour());
      GlStateManager.popMatrix();
    }

    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.popMatrix();
  }
}
