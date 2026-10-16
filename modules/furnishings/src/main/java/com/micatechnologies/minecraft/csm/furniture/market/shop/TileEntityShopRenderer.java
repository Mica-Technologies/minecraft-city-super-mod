package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

/**
 * Draws the item a shop sells on its shelves -- one item at each place its {@link ShopDisplay}
 * fills for the stock it holds (every place when its stock is unlimited or it is decorative) --
 * and, close up, its price on a card at the shelf edge. The shelves themselves are the block's
 * baked model; this draws only the goods.
 *
 * <p>Items are drawn as an item frame draws them (the {@code FIXED} transform): blocks as small
 * blocks, items as flat cut-outs facing the aisle. Each is scaled to fit its place's box, by the
 * box its own model fills ({@link #measure}), so a flat item (which {@code FIXED} draws a whole
 * block tall), a block or a model with display transforms of its own all stay inside their place
 * and clear of each other, the shelves and the glass. The item's model
 * is looked up once per change of template and kept on the tile entity, so a frame costs one
 * model draw per place shown and allocates nothing; and a plain item's whole shelf is compiled into
 * a display list, replayed until the template or the fill changes. Nothing is drawn past the configured item
 * distance ({@link TileEntityShop#getMaxRenderDistanceSquared()}), and the card only within its
 * own, shorter distance.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityShopRenderer extends TileEntitySpecialRenderer<TileEntityShop> {

  /** How far in front of the card the price sits, in sixteenths. */
  private static final double PRINT_LIFT = 0.02;
  /** The price's capital height on the card, in sixteenths. */
  private static final double PRICE_CAP = 1.4;
  private static final float CAP_UNITS = 7.0f;
  /** An emerald's picture on the card, with its gap, in font units. */
  private static final int EMERALD_UNITS = 10;
  /**
   * The clearance kept between an item and the sides of its place, in sixteenths: two items side
   * by side, or an item and a shelf's back or the glass, never share a plane and fight.
   */
  private static final float GAP = 0.15f;
  /** How far above the shelf top an item stands, in sixteenths, so its base never fights it. */
  private static final float LIFT = 0.1f;
  /** The thinnest an item is taken to be, in sixteenths, so a flat plane scales by its others. */
  private static final float MIN_EXTENT = 0.01f;

  /**
   * Each shop's shelf of goods, compiled once per template and fill: re-tessellating every item
   * every frame cost about 7 microseconds an item (3.6 ms for 64 full shops).
   */
  private static final CsmDisplayListCache CACHE = new CsmDisplayListCache("shop goods");

  @Override
  public void render(TileEntityShop te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    World world = te.getWorld();
    ItemStack item = te.getTemplate();
    if (world == null || item.isEmpty()) {
      return;
    }
    IBlockState state = world.getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof IShopBlock) || !state.getPropertyKeys()
        .contains(BlockHorizontal.FACING)) {
      return;
    }
    ShopDisplay display = ((IShopBlock) block).getShopDisplay();
    int shown = te.isDecorative() ? display.getPlaces()
        : display.shown(te.getShownStock(), item.getMaxStackSize(), te.isInfinite());
    EnumFacing facing = state.getValue(BlockHorizontal.FACING);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // Turned so that the model's front (its north face) faces +z here; a model point
    // (mx, my, mz) is then ((8 - mx) / 16, my / 16, (8 - mz) / 16).
    GlStateManager.rotate(-facing.getHorizontalAngle(), 0F, 1F, 0F);
    if (shown > 0) {
      drawItems(te, item, display, shown);
    }
    double labelDistance = CsmConfig.getShopsLabelDistance();
    double dx = x + 0.5;
    double dy = y + 0.5;
    double dz = z + 0.5;
    if (dx * dx + dy * dy + dz * dz <= labelDistance * labelDistance) {
      drawPrice(te, display);
    }
    GlStateManager.popMatrix();
  }

  private void drawItems(TileEntityShop te, ItemStack item, ShopDisplay display, int shown) {
    Minecraft mc = Minecraft.getMinecraft();
    RenderItem renderItem = mc.getRenderItem();
    IBakedModel model = (IBakedModel) te.clientModel;
    float[] extent = te.clientModelExtent;
    if (model == null || extent == null || te.clientModelFor != item) {
      model = renderItem.getItemModelWithOverrides(item, te.getWorld(), null);
      extent = measure(model);
      te.clientModel = model;
      te.clientModelExtent = extent;
      te.clientModelFor = item;
    }
    mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.enableRescaleNormal();
    GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
        GlStateManager.DestFactor.ZERO);
    RenderHelper.enableStandardItemLighting();

    // A plain item model is one texture and no cached GL state, so the whole shelf of it can be
    // replayed from a display list; the atlas and lighting above are set outside it every frame
    // (TRAFFIC_SIGNAL_SYSTEM.md, "Display lists: one texture, no cached state"). A glint or a
    // built-in renderer (a chest) binds textures and state of its own, so those draw directly.
    if (!model.isBuiltInRenderer() && !item.hasEffect()) {
      long key = ((long) System.identityHashCode(item) << 8) ^ shown;
      int list = CACHE.get(te.getPos(), key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = CACHE.allocate(te.getPos(), key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawPlaces(renderItem, model, extent, item, display, shown);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        drawPlaces(renderItem, model, extent, item, display, shown);
      }
    } else {
      drawPlaces(renderItem, model, extent, item, display, shown);
    }

    RenderHelper.disableStandardItemLighting();
    GlStateManager.disableRescaleNormal();
    GlStateManager.disableBlend();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }

  /**
   * Draws the item at each of the first {@code shown} places, row by row: each scaled, the same
   * on every axis, to the largest size that fits its place's box less a small gap all round, then
   * stood on the shelf (a hair above it) and centred in the box across and front to back.
   */
  private static void drawPlaces(RenderItem renderItem, IBakedModel model, float[] extent,
      ItemStack item, ShopDisplay display, int shown) {
    // The item's box as an item frame places it, in sixteenths.
    float ex = Math.max((extent[3] - extent[0]) * 16.0f, MIN_EXTENT);
    float ey = Math.max((extent[4] - extent[1]) * 16.0f, MIN_EXTENT);
    float ez = Math.max((extent[5] - extent[2]) * 16.0f, MIN_EXTENT);
    float midX = (extent[0] + extent[3]) / 2.0f;
    float midZ = (extent[2] + extent[5]) / 2.0f;
    int left = shown;
    for (float[] row : display.getRows()) {
      int count = (int) row[ShopDisplay.COUNT];
      if (count <= 0 || left <= 0) {
        continue;
      }
      float spacing = (row[ShopDisplay.X1] - row[ShopDisplay.X0]) / count;
      float z0 = row[ShopDisplay.Z0];
      float z1 = row[ShopDisplay.Z1];
      float scale = Math.min(Math.min((spacing - 2 * GAP) / ex,
          (row[ShopDisplay.SIZE] - LIFT) / ey), (z1 - z0 - 2 * GAP) / ez);
      if (scale <= 0) {
        continue;
      }
      // In this frame a model point (mx, my, mz) is ((8 - mx) / 16, my / 16, (8 - mz) / 16).
      double ty = (row[ShopDisplay.Y] + LIFT) / 16.0 - scale * extent[1];
      double tz = (8 - (z0 + z1) / 2.0) / 16.0 - scale * midZ;
      for (int i = 0; i < count && left > 0; i++, left--) {
        float mx = row[ShopDisplay.X0] + spacing * (i + 0.5f);
        GlStateManager.pushMatrix();
        GlStateManager.translate((8 - mx) / 16.0 - scale * midX, ty, tz);
        GlStateManager.scale(scale, scale, scale);
        IBakedModel transformed = ForgeHooksClient.handleCameraTransforms(model,
            TransformType.FIXED, false);
        renderItem.renderItem(item, transformed);
        GlStateManager.popMatrix();
      }
    }
  }

  /**
   * The box an item's model fills when drawn as {@link #drawPlaces} draws it before scaling (the
   * {@code FIXED} transform, then {@code renderItem}'s half-block shift), as {min x, y, z, max x,
   * y, z} in blocks. Measured from the model's own quads, since a {@code FIXED} transform makes a
   * flat item a whole block tall, a block half a block, and a model with transforms of its own
   * anything at all. A built-in renderer (a chest) has no quads, and is taken as filling its
   * block. Run once per change of template, so its allocations are not per frame.
   */
  static float[] measure(IBakedModel model) {
    Pair<? extends IBakedModel, Matrix4f> placed = model.handlePerspective(TransformType.FIXED);
    IBakedModel drawn = placed.getLeft() != null ? placed.getLeft() : model;
    Matrix4f matrix = placed.getRight();
    float[] box = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
    Point3f p = new Point3f();
    boolean any = false;
    if (!drawn.isBuiltInRenderer()) {
      try {
        for (int f = 0; f <= EnumFacing.VALUES.length; f++) {
          EnumFacing side = f < EnumFacing.VALUES.length ? EnumFacing.VALUES[f] : null;
          for (BakedQuad quad : drawn.getQuads(null, side, 0L)) {
            VertexFormat format = quad.getFormat();
            int offset = positionOffset(format);
            if (offset < 0) {
              continue;
            }
            int stride = format.getIntegerSize();
            int[] data = quad.getVertexData();
            for (int v = 0; v < data.length / stride; v++) {
              int at = v * stride + offset;
              p.set(Float.intBitsToFloat(data[at]), Float.intBitsToFloat(data[at + 1]),
                  Float.intBitsToFloat(data[at + 2]));
              include(box, p, matrix);
              any = true;
            }
          }
        }
      } catch (RuntimeException e) {
        // A model that cannot be asked for its quads without a block state: take its block.
        any = false;
      }
    }
    if (!any) {
      for (int c = 0; c < 8; c++) {
        p.set(c & 1, (c >> 1) & 1, (c >> 2) & 1);
        include(box, p, matrix);
      }
    }
    return box;
  }

  /** The position's offset in a vertex format, in ints, or -1 if it has none. */
  private static int positionOffset(VertexFormat format) {
    for (int i = 0; i < format.getElementCount(); i++) {
      VertexFormatElement element = format.getElement(i);
      if (element.getUsage() == VertexFormatElement.EnumUsage.POSITION
          && element.getType() == VertexFormatElement.EnumType.FLOAT) {
        return format.getOffset(i) / 4;
      }
    }
    return -1;
  }

  /** Grows the box to hold a model point, shifted and transformed as {@code renderItem} does. */
  private static void include(float[] box, Point3f p, Matrix4f matrix) {
    p.x -= 0.5f;
    p.y -= 0.5f;
    p.z -= 0.5f;
    if (matrix != null) {
      matrix.transform(p);
    }
    box[0] = Math.min(box[0], p.x);
    box[1] = Math.min(box[1], p.y);
    box[2] = Math.min(box[2], p.z);
    box[3] = Math.max(box[3], p.x);
    box[4] = Math.max(box[4], p.y);
    box[5] = Math.max(box[5], p.z);
  }

  /** Releases the shop's cached shelf, when its tile entity goes. */
  public static void release(BlockPos pos) {
    CACHE.invalidate(pos);
  }

  /**
   * The price on a small white card, centred on the display's label point: "$1.50", or a number
   * and an emerald's picture; "4 / " in front when a sale is more than one item.
   */
  private void drawPrice(TileEntityShop te, ShopDisplay display) {
    boolean money = te.isMoneyMode();
    String price = money ? ShopEconomy.format(true, te.getSymbol(), te.getSellPrice(true))
        : String.valueOf((long) te.getSellPrice(false));
    String text = te.getSellAmount() > 1 && !te.isDecorative()
        ? te.getSellAmount() + " / " + price : price;
    Minecraft mc = Minecraft.getMinecraft();
    FontRenderer font = mc.fontRenderer;
    float[] at = display.getLabel();
    int textWidth = font.getStringWidth(text);
    int width = money ? textWidth : textWidth + EMERALD_UNITS;
    double scale = PRICE_CAP / CAP_UNITS;
    double halfW = (width * scale) / 2.0 + 0.5;
    double halfH = PRICE_CAP / 2.0 + 0.5;

    GlStateManager.pushMatrix();
    GlStateManager.translate((8 - at[0]) / 16.0, at[1] / 16.0, (8 - at[2]) / 16.0);
    GlStateManager.disableLighting();
    GlStateManager.disableTexture2D();
    // The card, in sixteenths.
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    float k = 1.0f / 16.0f;
    buf.pos(-halfW * k, -halfH * k, 0).color(250, 250, 245, 255).endVertex();
    buf.pos(halfW * k, -halfH * k, 0).color(250, 250, 245, 255).endVertex();
    buf.pos(halfW * k, halfH * k, 0).color(250, 250, 245, 255).endVertex();
    buf.pos(-halfW * k, halfH * k, 0).color(250, 250, 245, 255).endVertex();
    tess.draw();
    GlStateManager.enableTexture2D();
    GlStateManager.depthMask(false);
    GlStateManager.translate(0, 0, PRINT_LIFT / 16.0);
    GlStateManager.scale(scale / 16.0, -scale / 16.0, scale / 16.0);
    GlStateManager.translate(-width / 2.0, -CAP_UNITS / 2.0, 0);
    font.drawString(text, 0, 0, te.isDecorative() ? 0x606060 : 0xC01010);
    if (!money) {
      // The emerald's own item sprite, a glyph wide, after the number.
      TextureAtlasSprite sprite = mc.getRenderItem().getItemModelMesher()
          .getParticleIcon(Items.EMERALD);
      mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
      float x0 = textWidth + 1.0f;
      float x1 = x0 + 8.0f;
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
      buf.pos(x0, -0.5, 0).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
      buf.pos(x0, 7.5, 0).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
      buf.pos(x1, 7.5, 0).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
      buf.pos(x1, -0.5, 0).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
      GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
      GlStateManager.enableAlpha();
      tess.draw();
    }
    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.popMatrix();
  }
}
