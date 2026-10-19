package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingLayout.Piece;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A hospital wayfinding panel cell's model: the slab and frame its blockstate draws, with the
 * cell's share of the sign added as quads from one sheet and baked into the chunk mesh.
 *
 * <p>The sign is laid out once across the whole panel ({@link HospitalWayfindingLayout}) in the
 * reader's space, and each piece clipped to this cell's square, so the cells of a panel show one
 * sign with no seams and no cell draws outside its own block, except the hanger rods, which a
 * top-row cell carries up to the ceiling. The back carries the same layout seen from behind, its
 * arrow mirrored so it points the same way in the world (unless the sign is single-sided). Every
 * piece's colour is in its vertices and is drawn unshaded, so the scheme's colours come out as
 * they are; the panel's own light (8) keeps it readable in a dim corridor. Quads are cached by
 * facing and {@link HospitalWayfindingFace}, so identical signs bake once.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class HospitalWayfindingBakedModel extends BakedModelWrapper<IBakedModel> {

  static final String SHEET = "csm:blocks/healthcare/systems/wayfinding_sheet";

  /** The slab's faces (the generator's PANEL_Z0 and PANEL_Z1), in sixteenths. */
  private static final float FACE_Z0 = 6.0f;
  private static final float FACE_Z1 = 10.0f;
  /** How far in front of the slab's face the background and the art are, in sixteenths. */
  private static final float BACKGROUND_LIFT = 0.2f;
  private static final float ART_LIFT = 0.5f;
  /** Half a hanger rod's width, in sixteenths (Transit's), and its colour. */
  private static final float ROD = 0.55f;
  private static final int ROD_RGB = 0xA4A8AC;

  private static final Cache<Key, List<BakedQuad>> CACHE =
      CacheBuilder.newBuilder().maximumSize(2048).build();

  HospitalWayfindingBakedModel(IBakedModel original) {
    super(original);
  }

  /** Puts the sheet on the block atlas and the wrapper in place. Client pre-initialisation. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new Object() {
      @SubscribeEvent
      public void onStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation(SHEET));
      }
    });
    CsmBakedModelWrappers.register(HospitalWayfindingBakedModel::wrap);
  }

  /**
   * Wraps the panel's world model: the one multipart model CSM bakes for every state of a
   * multipart blockstate ({@code #multipart}), or a per-variant one; the item model is left
   * alone. The facing is read from the state each time, since one model serves all four.
   */
  @Nullable
  static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof HospitalWayfindingBakedModel || !"csm".equals(location.getNamespace())
        || !"hospital_wayfinding_panel".equals(location.getPath())
        || "inventory".equals(location.getVariant())) {
      return null;
    }
    return new HospitalWayfindingBakedModel(model);
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    List<BakedQuad> base = originalModel.getQuads(state, side, rand);
    if (side != null || !(state instanceof IExtendedBlockState)) {
      return base;
    }
    HospitalWayfindingFace face = ((IExtendedBlockState) state)
        .getValue(BlockHospitalWayfindingPanel.FACE);

    if (face == null || !state.getPropertyKeys().contains(BlockHospitalWayfindingPanel.FACING)) {
      return base;
    }
    EnumFacing facing = state.getValue(BlockHospitalWayfindingPanel.FACING);
    List<BakedQuad> overlay;
    try {
      overlay = CACHE.get(new Key(facing, face), () -> bake(facing, face));
    } catch (ExecutionException e) {
      overlay = Collections.emptyList();
    }
    if (overlay.isEmpty()) {
      return base;
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

  private static List<BakedQuad> bake(EnumFacing facing, HospitalWayfindingFace f) {
    TextureAtlasSprite sheet = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite(SHEET);
    List<BakedQuad> out = new ArrayList<>();
    int turns = facing.getHorizontalIndex() >= 0 ? (facing.getHorizontalIndex() + 2) % 4 : 0;
    if (f.drawsSign()) {
      // the front: the reader's x runs to model -x; this cell is x col..col+1
      for (Piece p : HospitalWayfindingLayout.face(f, f.arrow)) {
        Piece c = p.clip(f.col, f.row, f.col + 1, f.row + 1);
        if (c != null) {
          out.add(quad(sheet, c, true, f.col, f.row, layerZ(c, true), turns));
        }
      }
      if (f.doubleSided) {
        int backCol = f.width - 1 - f.col;
        for (Piece p : HospitalWayfindingLayout.face(f, f.arrow.mirrored())) {
          Piece c = p.clip(backCol, f.row, backCol + 1, f.row + 1);
          if (c != null) {
            out.add(quad(sheet, c, false, backCol, f.row, layerZ(c, false), turns));
          }
        }
      }
    }
    int[] white = HospitalWayfindingGlyphs.WHITE;
    float wu = white[0] + white[2] / 2f;
    float wv = white[1] + white[2] / 2f;
    for (int i = 0; i + 1 < f.rods.length; i += 2) {
      float x = 16f * (f.col + 1 - f.rods[i]);
      float y1 = 16f + 16f * f.rods[i + 1];
      rod(out, sheet, x, y1, wu, wv, turns);
    }
    return out;
  }

  private static float layerZ(Piece p, boolean front) {
    float lift = p.layer == HospitalWayfindingLayout.LAYER_BACKGROUND ? BACKGROUND_LIFT
        : ART_LIFT;
    return front ? FACE_Z0 - lift : FACE_Z1 + lift;
  }

  /**
   * One piece as a quad, in model sixteenths before the facing's turn. On the front the reader
   * looks south at the model's north face, so their x (from the cell's left edge, col) runs to
   * model -x from x = 16; on the back they look north, and theirs runs to model +x from 0.
   */
  private static BakedQuad quad(TextureAtlasSprite sheet, Piece p, boolean front, int col,
      int row, float z, int turns) {
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

  /** A hanger rod from the top of the cell up {@code y1}, square, its sides shaded by hand. */
  private static void rod(List<BakedQuad> out, TextureAtlasSprite sheet, float x, float y1,
      float u, float v, int turns) {
    float x0 = x - ROD;
    float x1 = x + ROD;
    float z0 = 8 - ROD;
    float z1 = 8 + ROD;
    float y0 = 16;
    // each side as (xa, za) -> (xb, zb), wound anticlockwise seen from outside
    float[][] sides = {{x1, z0, x0, z0}, {x0, z1, x1, z1}, {x0, z0, x0, z1}, {x1, z1, x1, z0}};
    EnumFacing[] normals = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};
    float[] shade = {0.8f, 0.8f, 0.6f, 0.6f};
    for (int s = 0; s < 4; s++) {
      float[] e = sides[s];
      int rgb = scale(ROD_RGB, shade[s]);
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
   * blockstate turns the slab ({@code y} 90 a turn: north to east).
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
    float k = 16f / HospitalWayfindingGlyphs.SHEET;
    data[o + 4] = Float.floatToRawIntBits(sheet.getInterpolatedU(u * k));
    data[o + 5] = Float.floatToRawIntBits(sheet.getInterpolatedV(v * k));
    data[o + 6] = 0;
  }

  private static EnumFacing turn(EnumFacing f, int turns) {
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
    private final HospitalWayfindingFace face;

    Key(EnumFacing facing, HospitalWayfindingFace face) {
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
