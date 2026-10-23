package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingLayout.Piece;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A wayfinding panel cell's model: its share of the slab and frame, the sign, and the rods or
 * posts, baked into the chunk mesh from the cell's {@link WayfindingFace}.
 *
 * <p>The sign is laid out once across the whole panel ({@link WayfindingLayout}) and each piece
 * clipped to this cell's square, so the cells of a panel show one sign with no seams. A cell on
 * the panel's outer edge clips only on its inner sides, so a panel scaled past its cells, or hung
 * lower than them, is drawn whole by its edge cells; the rods and posts reach past the cell to
 * the ceiling and the floor. The back carries the same layout seen from behind, its arrows
 * mirrored so they point the same way in the world, unless the sign is one-sided or on a wall.
 * Every piece's colour is in its vertices and is drawn unshaded, so a scheme's colours come out
 * as they are; the slab's edges and the rods are shaded by hand. Quads are cached by facing and
 * face, so identical signs bake once.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class WayfindingBakedModel extends BakedModelWrapper<IBakedModel> {

  /** How far in front of the slab's face each layer is, in sixteenths. */
  private static final float[] LIFT = {0.2f, 0.35f, 0.5f};
  /** Half a hanger rod's width and half a post's, in sixteenths, and their colour. */
  private static final float ROD = 0.55f;
  private static final float POST = 1.0f;
  private static final int METAL_RGB = 0xA4A8AC;
  /** Beyond any cell: a clip edge that does not cut. */
  private static final float OPEN = 1000f;

  private static final Cache<Key, List<BakedQuad>> CACHE =
      CacheBuilder.newBuilder().maximumSize(4096).build();

  WayfindingBakedModel(IBakedModel original) {
    super(original);
  }

  /** Puts the sheets on the block atlas and the wrapper in place. Client pre-initialisation. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new Object() {
      @SubscribeEvent
      public void onStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation(WayfindingSheet.GLYPH_SPRITE));
        event.getMap().registerSprite(new ResourceLocation(WayfindingSheet.ICON_SPRITE));
        CACHE.invalidateAll();
      }
    });
    CsmBakedModelWrappers.register(WayfindingBakedModel::wrap);
  }

  /**
   * Wraps a panel's world models: every variant of every block that is a wayfinding panel; the
   * item model is left alone. The facing is read from the state each time.
   */
  @Nullable
  static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof WayfindingBakedModel || "inventory".equals(location.getVariant())) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    if (!(block instanceof AbstractBlockWayfindingPanel)) {
      return null;
    }
    return new WayfindingBakedModel(model);
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    List<BakedQuad> base = originalModel.getQuads(state, side, rand);
    if (side != null || !(state instanceof IExtendedBlockState)
        || !state.getPropertyKeys().contains(AbstractBlockWayfindingPanel.FACING)) {
      return base;
    }
    WayfindingFace face = ((IExtendedBlockState) state)
        .getValue(AbstractBlockWayfindingPanel.FACE);
    if (face == null) {
      return base;
    }
    EnumFacing facing = state.getValue(AbstractBlockWayfindingPanel.FACING);
    List<BakedQuad> overlay;
    try {
      overlay = CACHE.get(new Key(facing, face), () -> bake(facing, face));
    } catch (ExecutionException e) {
      overlay = Collections.emptyList();
    }
    if (base.isEmpty()) {
      return overlay;
    }
    List<BakedQuad> out = new ArrayList<>(base.size() + overlay.size());
    out.addAll(base);
    out.addAll(overlay);
    return out;
  }

  @Override
  public boolean isAmbientOcclusion() {
    return false;
  }

  // --- baking --------------------------------------------------------------------------------

  private static List<BakedQuad> bake(EnumFacing facing, WayfindingFace f) {
    TextureAtlasSprite[] sheets = {
        Minecraft.getMinecraft().getTextureMapBlocks()
            .getAtlasSprite(WayfindingSheet.GLYPH_SPRITE),
        Minecraft.getMinecraft().getTextureMapBlocks()
            .getAtlasSprite(WayfindingSheet.ICON_SPRITE)};
    List<BakedQuad> out = new ArrayList<>();
    int turns = facing.getHorizontalIndex() >= 0 ? (facing.getHorizontalIndex() + 2) % 4 : 0;
    WayfindingSign sign = f.sign;
    int cw = f.cellsWide();
    int ch = f.cellsHigh();
    int col = f.cellCol();
    int row = f.cellRow();
    float px0 = f.panelX0();
    float py0 = f.panelY0();
    float pw = f.panelWidth();
    float ph = f.panelHeight();
    float z0 = f.slabZ0();
    float z1 = f.slabZ1();
    boolean wall = f.mount == WayfindingMount.WALL;

    // this cell's square, open on the panel's outer edges, from the front and from the back
    float cy0 = row == 0 ? -OPEN : row;
    float cy1 = row == ch - 1 ? OPEN : row + 1;
    float fx0 = col == 0 ? -OPEN : col;
    float fx1 = col == cw - 1 ? OPEN : col + 1;
    int backCol = cw - 1 - col;
    float bx0 = backCol == 0 ? -OPEN : backCol;
    float bx1 = backCol == cw - 1 ? OPEN : backCol + 1;

    // the slab: its front and back in the frame's colour, its edges shaded
    int frame = frameColour(sign.background());
    Piece slab = WayfindingLayout.white(px0, py0, px0 + pw, py0 + ph, frame,
        WayfindingLayout.LAYER_BACKGROUND);
    Piece front = slab.clip(fx0, cy0, fx1, cy1);
    if (front != null) {
      out.add(quad(sheets, front, true, col, row, z0, turns));
      if (!wall) {
        Piece back = slab.clip(bx0, cy0, bx1, cy1);
        if (back != null) {
          out.add(quad(sheets, back, false, backCol, row, z1, turns));
        }
      }
      edges(out, sheets[0], f, front, col, row, frame, turns);
    }

    if (f.drawsSign()) {
      for (Piece p : WayfindingLayout.face(sign, pw, ph, false)) {
        Piece c = p.moved(px0, py0).clip(fx0, cy0, fx1, cy1);
        if (c != null) {
          out.add(quad(sheets, c, true, col, row, z0 - LIFT[c.layer], turns));
        }
      }
      if (f.drawsBack()) {
        for (Piece p : WayfindingLayout.face(sign, pw, ph, true)) {
          Piece c = p.moved(px0, py0).clip(bx0, cy0, bx1, cy1);
          if (c != null) {
            out.add(quad(sheets, c, false, backCol, row, z1 + LIFT[c.layer], turns));
          }
        }
      }
    }

    float zMid = (z0 + z1) / 2f;
    for (int i = 0; i + 1 < f.rods.length; i += 2) {
      float x = 16f * (col + 1 - f.rods[i]);
      float y0 = 16f * (py0 + ph - row);
      float y1 = 16f + 16f * f.rods[i + 1];
      if (y1 > y0) {
        bar(out, sheets[0], x, zMid, ROD, y0, y1, turns);
      }
    }
    for (int i = 0; i + 1 < f.posts.length; i += 2) {
      float x = 16f * (col + 1 - f.posts[i]);
      float y0 = -16f * f.posts[i + 1];
      float y1 = 16f * (py0 - row);
      if (y1 > y0) {
        bar(out, sheets[0], x, zMid, POST, y0, y1, turns);
      }
    }
    return out;
  }

  /**
   * The slab's four edges where they fall in this cell (front is the cell's clipped share of
   * the slab's front, so its edges are the slab's only where the slab's are inside it).
   */
  private static void edges(List<BakedQuad> out, TextureAtlasSprite sheet, WayfindingFace f,
      Piece front, int col, int row, int frame, int turns) {
    float px0 = f.panelX0();
    float py0 = f.panelY0();
    float px1 = px0 + f.panelWidth();
    float py1 = py0 + f.panelHeight();
    float z0 = f.slabZ0();
    float z1 = f.slabZ1();
    // model x of the reader's x, in this cell
    float mxl = 16f * (col + 1 - front.x0);
    float mxr = 16f * (col + 1 - front.x1);
    float my0 = 16f * (front.y0 - row);
    float my1 = 16f * (front.y1 - row);
    float u = WayfindingSheet.WHITE[0] + WayfindingSheet.WHITE[2] / 2f;
    float v = WayfindingSheet.WHITE[1] + WayfindingSheet.WHITE[2] / 2f;
    if (Math.abs(front.x0 - px0) < 1e-4f) {      // the reader's left edge: model +x side
      side(out, sheet, new float[][]{{mxl, my0, z1}, {mxl, my0, z0}, {mxl, my1, z0},
          {mxl, my1, z1}}, EnumFacing.EAST, scale(frame, 0.6f), u, v, turns);
    }
    if (Math.abs(front.x1 - px1) < 1e-4f) {      // the reader's right edge: model -x side
      side(out, sheet, new float[][]{{mxr, my0, z0}, {mxr, my0, z1}, {mxr, my1, z1},
          {mxr, my1, z0}}, EnumFacing.WEST, scale(frame, 0.6f), u, v, turns);
    }
    if (Math.abs(front.y1 - py1) < 1e-4f) {
      side(out, sheet, new float[][]{{mxr, my1, z0}, {mxr, my1, z1}, {mxl, my1, z1},
          {mxl, my1, z0}}, EnumFacing.UP, frame, u, v, turns);
    }
    if (Math.abs(front.y0 - py0) < 1e-4f) {
      side(out, sheet, new float[][]{{mxl, my0, z0}, {mxl, my0, z1}, {mxr, my0, z1},
          {mxr, my0, z0}}, EnumFacing.DOWN, scale(frame, 0.5f), u, v, turns);
    }
  }

  private static void side(List<BakedQuad> out, TextureAtlasSprite sheet, float[][] corners,
      EnumFacing normal, int rgb, float u, float v, int turns) {
    int[] data = new int[28];
    for (int i = 0; i < 4; i++) {
      vertex(data, i, corners[i][0], corners[i][1], corners[i][2], turns, rgb, sheet, u, v);
    }
    out.add(new BakedQuad(data, -1, turn(normal, turns), sheet, false,
        DefaultVertexFormats.BLOCK));
  }

  /** The frame's colour: the background's, darkened, as an anodised edge round the face. */
  static int frameColour(int background) {
    return scale(background, 0.55f) + 0x0E0E0E;
  }

  /**
   * One piece as a quad, in model sixteenths before the facing's turn. On the front the reader
   * looks south at the model's north face, so their x (from the cell's left edge, col) runs to
   * model -x from x = 16; on the back they look north, and theirs runs to model +x from 0.
   */
  private static BakedQuad quad(TextureAtlasSprite[] sheets, Piece p, boolean front, int col,
      int row, float z, int turns) {
    TextureAtlasSprite sheet = sheets[p.sheet];
    // wound anticlockwise as the reader of either face sees it
    float[][] corners = {{p.x0, p.y0}, {p.x1, p.y0}, {p.x1, p.y1}, {p.x0, p.y1}};
    int[] data = new int[28];
    for (int i = 0; i < 4; i++) {
      float rx = corners[i][0];
      float ry = corners[i][1];
      float mx = front ? 16f * (col + 1 - rx) : 16f * (rx - col);
      float my = 16f * (ry - row);
      vertex(data, i, mx, my, z, turns, p.rgb, sheet, p.u(rx, ry), p.v(rx, ry));
    }
    EnumFacing normal = turn(front ? EnumFacing.NORTH : EnumFacing.SOUTH, turns);
    return new BakedQuad(data, -1, normal, sheet, false, DefaultVertexFormats.BLOCK);
  }

  /** A square bar from y0 to y1 at (x, z), half width h, its sides shaded by hand. */
  private static void bar(List<BakedQuad> out, TextureAtlasSprite sheet, float x, float z,
      float h, float y0, float y1, int turns) {
    float u = WayfindingSheet.WHITE[0] + WayfindingSheet.WHITE[2] / 2f;
    float v = WayfindingSheet.WHITE[1] + WayfindingSheet.WHITE[2] / 2f;
    float x0 = x - h;
    float x1 = x + h;
    float z0 = z - h;
    float z1 = z + h;
    // each side as (xa, za) -> (xb, zb), wound anticlockwise seen from outside
    float[][] sides = {{x1, z0, x0, z0}, {x0, z1, x1, z1}, {x0, z0, x0, z1}, {x1, z1, x1, z0}};
    EnumFacing[] normals = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};
    float[] shade = {0.8f, 0.8f, 0.6f, 0.6f};
    for (int s = 0; s < 4; s++) {
      float[] e = sides[s];
      int rgb = scale(METAL_RGB, shade[s]);
      int[] data = new int[28];
      vertex(data, 0, e[0], y0, e[1], turns, rgb, sheet, u, v);
      vertex(data, 1, e[2], y0, e[3], turns, rgb, sheet, u, v);
      vertex(data, 2, e[2], y1, e[3], turns, rgb, sheet, u, v);
      vertex(data, 3, e[0], y1, e[1], turns, rgb, sheet, u, v);
      out.add(new BakedQuad(data, -1, turn(normals[s], turns), sheet, false,
          DefaultVertexFormats.BLOCK));
    }
  }

  /**
   * Writes one vertex in the BLOCK format (position, colour, texture, lightmap; seven ints),
   * the position turned about the block's middle by {@code turns} quarter turns as the
   * blockstate turns a model ({@code y} 90 a turn: north to east).
   */
  private static void vertex(int[] data, int i, float x, float y, float z, int turns, int rgb,
      TextureAtlasSprite sheet, float u, float v) {
    for (int t = 0; t < turns; t++) {
      float nx = 16 - z;
      z = x;
      x = nx;
    }
    int o = i * 7;
    data[o] = Float.floatToRawIntBits(x / 16f);
    data[o + 1] = Float.floatToRawIntBits(y / 16f);
    data[o + 2] = Float.floatToRawIntBits(z / 16f);
    data[o + 3] = 0xFF000000 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF);
    float k = 16f / WayfindingSheet.SHEET;
    data[o + 4] = Float.floatToRawIntBits(sheet.getInterpolatedU(u * k));
    data[o + 5] = Float.floatToRawIntBits(sheet.getInterpolatedV(v * k));
    data[o + 6] = 0;
  }

  private static EnumFacing turn(EnumFacing f, int turns) {
    if (f.getAxis() == EnumFacing.Axis.Y) {
      return f;
    }
    for (int t = 0; t < turns; t++) {
      f = f.rotateY();
    }
    return f;
  }

  private static int scale(int rgb, float k) {
    int r = Math.round((rgb >> 16 & 0xFF) * k);
    int g = Math.round((rgb >> 8 & 0xFF) * k);
    int b = Math.round((rgb & 0xFF) * k);
    return r << 16 | g << 8 | b;
  }

  /** The cache's key: a facing and what the cell draws. */
  private static final class Key {

    private final EnumFacing facing;
    private final WayfindingFace face;

    Key(EnumFacing facing, WayfindingFace face) {
      this.facing = facing;
      this.face = face;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Key && ((Key) o).facing == facing && ((Key) o).face.equals(face);
    }

    @Override
    public int hashCode() {
      return Objects.hash(facing, face);
    }
  }
}
