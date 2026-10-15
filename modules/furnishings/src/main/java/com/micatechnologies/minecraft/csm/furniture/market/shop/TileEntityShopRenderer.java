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
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws the item a shop sells on its shelves -- one item at each place its {@link ShopDisplay}
 * fills for the stock it holds (every place when its stock is unlimited or it is decorative) --
 * and, close up, its price on a card at the shelf edge. The shelves themselves are the block's
 * baked model; this draws only the goods.
 *
 * <p>Items are drawn as an item frame draws them (the {@code FIXED} transform), scaled to the
 * row's size: blocks as small blocks, items as flat cut-outs facing the aisle. The item's model
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
    if (model == null || te.clientModelFor != item) {
      model = renderItem.getItemModelWithOverrides(item, te.getWorld(), null);
      te.clientModel = model;
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
          drawPlaces(renderItem, model, item, display, shown);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        drawPlaces(renderItem, model, item, display, shown);
      }
    } else {
      drawPlaces(renderItem, model, item, display, shown);
    }

    RenderHelper.disableStandardItemLighting();
    GlStateManager.disableRescaleNormal();
    GlStateManager.disableBlend();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }

  /** Draws the item at each of the first {@code shown} places, row by row. */
  private static void drawPlaces(RenderItem renderItem, IBakedModel model, ItemStack item,
      ShopDisplay display, int shown) {
    int left = shown;
    for (float[] row : display.getRows()) {
      int count = (int) row[5];
      if (count <= 0 || left <= 0) {
        continue;
      }
      float spacing = (row[1] - row[0]) / count;
      // The FIXED transform draws an item half a block tall, centred on the origin.
      float scale = row[4] / 8.0f;
      for (int i = 0; i < count && left > 0; i++, left--) {
        float mx = row[0] + spacing * (i + 0.5f);
        GlStateManager.pushMatrix();
        GlStateManager.translate((8 - mx) / 16.0, (row[2] + row[4] / 2.0f) / 16.0,
            (8 - row[3]) / 16.0);
        GlStateManager.scale(scale, scale, scale);
        IBakedModel transformed = ForgeHooksClient.handleCameraTransforms(model,
            TransformType.FIXED, false);
        renderItem.renderItem(item, transformed);
        GlStateManager.popMatrix();
      }
    }
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
