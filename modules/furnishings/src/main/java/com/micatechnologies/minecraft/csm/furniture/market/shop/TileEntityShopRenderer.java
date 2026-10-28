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
import net.minecraft.util.ResourceLocation;
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
 * a display list, replayed until the template or the fill changes. An enchanted item's shelf is two
 * lists, its models and its glint geometry, the glint's state and scrolling texture matrices set
 * around them every frame as vanilla sets them; an item with a built-in renderer (a chest) is drawn
 * live. Nothing is drawn past the configured item
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

  /** How {@link #drawPlaces} draws each item: whole, its model alone, or its glint geometry. */
  private static final int PLAIN = 0;
  private static final int BASE = 1;
  private static final int GLINT = 2;
  /** Set in a shelf's key for its glint list, beside its base list in the same cache entry. */
  private static final long GLINT_KEY = 1L << 62;
  /** {@code RenderItem.RES_ITEM_GLINT}, and the colour {@code renderEffect} draws it in. */
  private static final ResourceLocation GLINT_TEXTURE =
      new ResourceLocation("textures/misc/enchanted_item_glint.png");
  private static final int GLINT_COLOUR = -8372020;

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
      te.clientModelPlain = isPlainGeometry(model);
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
    // (TRAFFIC_SIGNAL_SYSTEM.md, "Display lists: one texture, no cached state").
    //
    // An enchanted item is the same model drawn twice more under the glint texture, so it is two
    // lists: the shelf's base models, and the shelf's glint geometry (the same quads in the glint's
    // colour), replayed twice under RenderItem.renderEffect's state and its two scrolling texture
    // matrices, set here every frame. All the bases are drawn before all the glints rather than
    // item by item; the glint's depth test is EQUAL, so each item's glint still lands only on its
    // own surface and never through another item in front of it.
    //
    // A built-in renderer (a chest, a banner, a shield, a skull, another mod's item renderer)
    // draws live: it binds textures of its own (a banner's pattern texture is made on demand, a
    // skull's skin arrives from the network), sets cached GL state, and may compile its own model
    // lists on first use, none of which a display list can hold.
    long key = ((long) System.identityHashCode(item) << 8) ^ shown;
    if (model.isBuiltInRenderer()) {
      drawPlaces(renderItem, model, extent, item, display, shown, PLAIN);
    } else if (!item.hasEffect()) {
      replay(te, key, renderItem, model, extent, item, display, shown, PLAIN);
    } else if (te.clientModelPlain) {
      replay(te, key, renderItem, model, extent, item, display, shown, BASE);
      drawGlint(mc, te, key ^ GLINT_KEY, renderItem, model, extent, item, display, shown);
    } else {
      // Quads that switch lighting or shading as they draw: vanilla's way, every frame.
      drawPlaces(renderItem, model, extent, item, display, shown, PLAIN);
    }

    RenderHelper.disableStandardItemLighting();
    GlStateManager.disableRescaleNormal();
    GlStateManager.disableBlend();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }

  /**
   * Replays the shelf's list for a key and a way of drawing, compiling it if it is missing, or
   * draws it directly if the driver refuses a list.
   */
  private static void replay(TileEntityShop te, long key, RenderItem renderItem,
      IBakedModel model, float[] extent, ItemStack item, ShopDisplay display, int shown,
      int mode) {
    int list = CACHE.get(te.getPos(), key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = CACHE.allocate(te.getPos(), key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        drawPlaces(renderItem, model, extent, item, display, shown, mode);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      drawPlaces(renderItem, model, extent, item, display, shown, mode);
    }
  }

  /**
   * The enchantment glint over the shelf, as {@code RenderItem.renderEffect} draws it over each
   * item: depth equal and unwritten, lighting off, additive, the glint texture, and the shelf's
   * glint geometry drawn twice, each under its own scaled, scrolling and turned texture matrix on
   * vanilla's clock. The texture matrices are set here, outside the list, and the matrix mode is
   * back on the model view before each call, since the list's own placing (push, translate, scale)
   * applies to whichever matrix is current when it is replayed. Vanilla's state after the glint
   * is restored as vanilla restores it.
   */
  private static void drawGlint(Minecraft mc, TileEntityShop te, long key, RenderItem renderItem,
      IBakedModel model, float[] extent, ItemStack item, ShopDisplay display, int shown) {
    GlStateManager.depthMask(false);
    GlStateManager.depthFunc(GL11.GL_EQUAL);
    GlStateManager.disableLighting();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR,
        GlStateManager.DestFactor.ONE);
    mc.getTextureManager().bindTexture(GLINT_TEXTURE);
    GlStateManager.matrixMode(GL11.GL_TEXTURE);
    GlStateManager.pushMatrix();
    GlStateManager.scale(8.0F, 8.0F, 8.0F);
    float f = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
    GlStateManager.translate(f, 0.0F, 0.0F);
    GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
    GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    replay(te, key, renderItem, model, extent, item, display, shown, GLINT);
    GlStateManager.matrixMode(GL11.GL_TEXTURE);
    GlStateManager.popMatrix();
    GlStateManager.pushMatrix();
    GlStateManager.scale(8.0F, 8.0F, 8.0F);
    float f1 = (float) (Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
    GlStateManager.translate(-f1, 0.0F, 0.0F);
    GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);
    GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    replay(te, key, renderItem, model, extent, item, display, shown, GLINT);
    GlStateManager.matrixMode(GL11.GL_TEXTURE);
    GlStateManager.popMatrix();
    GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.enableLighting();
    GlStateManager.depthFunc(GL11.GL_LEQUAL);
    GlStateManager.depthMask(true);
    mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
  }

  /**
   * Whether every quad of the model is plain geometry: drawn by {@code RenderItem.renderModel}
   * with no change of lighting or shading between its quads (Forge's emissive-item path switches
   * GL lighting and the lightmap for a quad that carries its own light or skips diffuse shading,
   * through cached calls a display list cannot hold). Only such a model's base and glint are
   * drawn by {@link #drawModel}; vanilla's own path draws the rest. Run once per change of
   * template.
   */
  static boolean isPlainGeometry(IBakedModel model) {
    try {
      // The model drawn is the one the FIXED transform hands back, as in drawPlaces.
      IBakedModel placed = model.handlePerspective(TransformType.FIXED).getLeft();
      IBakedModel drawn = placed != null ? placed : model;
      if (model.isBuiltInRenderer() || drawn.isBuiltInRenderer()) {
        return false;
      }
      for (int f = 0; f <= EnumFacing.VALUES.length; f++) {
        EnumFacing side = f < EnumFacing.VALUES.length ? EnumFacing.VALUES[f] : null;
        for (BakedQuad quad : drawn.getQuads(null, side, 0L)) {
          if (!quad.shouldApplyDiffuseLighting()
              || (quad.getFormat() != DefaultVertexFormats.ITEM
              && quad.getFormat().hasUvOffset(1))) {
            return false;
          }
        }
      }
    } catch (RuntimeException e) {
      return false;
    }
    return true;
  }

  /**
   * A model's quads at the item's place, in one colour or (with {@code -1}) the item's own tints:
   * {@code RenderItem.renderItem}'s half-block shift and {@code renderModel}, without the glint.
   * Geometry only.
   */
  private static void drawModel(RenderItem renderItem, IBakedModel model, int colour,
      ItemStack item) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(-0.5F, -0.5F, -0.5F);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
    for (EnumFacing side : EnumFacing.VALUES) {
      renderItem.renderQuads(buf, model.getQuads(null, side, 0L), colour, item);
    }
    renderItem.renderQuads(buf, model.getQuads(null, null, 0L), colour, item);
    tess.draw();
    GlStateManager.popMatrix();
  }

  /**
   * Draws the item at each of the first {@code shown} places, row by row: each scaled, the same
   * on every axis, to the largest size that fits its place's box less a small gap all round, then
   * stood on the shelf (a hair above it) and centred in the box across and front to back. In
   * {@link #PLAIN} each is {@code renderItem}'s whole draw; in {@link #BASE} and {@link #GLINT}
   * the model's quads alone, in the item's colours or in the glint's.
   */
  private static void drawPlaces(RenderItem renderItem, IBakedModel model, float[] extent,
      ItemStack item, ShopDisplay display, int shown, int mode) {
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
        if (mode == PLAIN) {
          renderItem.renderItem(item, transformed);
        } else {
          drawModel(renderItem, transformed, mode == GLINT ? GLINT_COLOUR : -1, item);
        }
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
