package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.micatechnologies.minecraft.csm.buildingmaterials.plaques.PlaqueLayout.Piece;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
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
 * An address plaque's model: the plate, its numerals and its street line, laid out by
 * {@link PlaqueLayout} from the tile entity's {@link AddressPlaque} and baked into the chunk mesh.
 *
 * <p>The blockstate's model is empty (particle only); this wrapper adds the quads, drawn with the
 * wall to the north and turned to the block's facing as a blockstate's {@code y} would turn them.
 * The plate's face comes from its nine-slice tile on the body sheet and its sides are the style's
 * edge colour; every glyph is a window on the glyph sheet tinted in its vertices. Quads are
 * cached by style, facing and plaque, so a street of plaques with the same number bakes once.
 * Nothing runs per frame and nothing ticks: a plaque costs what a few dozen quads in the chunk
 * cost.</p>
 *
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public class PlaqueBakedModel extends BakedModelWrapper<IBakedModel> {

  private static final Cache<Key, List<BakedQuad>> CACHE =
      CacheBuilder.newBuilder().maximumSize(2048).build();

  private final PlaqueStyle style;

  PlaqueBakedModel(IBakedModel original, PlaqueStyle style) {
    super(original);
    this.style = style;
  }

  /** Puts the sheets on the block atlas and the wrapper in place. Client pre-initialisation. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new Object() {
      @SubscribeEvent
      public void onStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation(PlaqueSheet.GLYPH_SPRITE));
        event.getMap().registerSprite(new ResourceLocation(PlaqueSheet.BODY_SPRITE));
        CACHE.invalidateAll();
      }
    });
    CsmBakedModelWrappers.register(PlaqueBakedModel::wrap);
  }

  /** Wraps every world variant of every plaque block; the item model is left alone. */
  @Nullable
  static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof PlaqueBakedModel || "inventory".equals(location.getVariant())) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    if (!(block instanceof BlockAddressPlaque)) {
      return null;
    }
    return new PlaqueBakedModel(model, ((BlockAddressPlaque) block).getStyle());
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    List<BakedQuad> base = originalModel.getQuads(state, side, rand);
    if (side != null || !(state instanceof IExtendedBlockState)
        || !state.getPropertyKeys().contains(BlockAddressPlaque.FACING)) {
      return base;
    }
    AddressPlaque plaque = ((IExtendedBlockState) state).getValue(BlockAddressPlaque.PLAQUE);
    if (plaque == null) {
      return base;
    }
    EnumFacing facing = state.getValue(BlockAddressPlaque.FACING);
    List<BakedQuad> overlay;
    try {
      overlay = CACHE.get(new Key(style, facing, plaque), () -> bake(style, facing, plaque));
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

  static List<BakedQuad> bake(PlaqueStyle style, EnumFacing facing, AddressPlaque plaque) {
    TextureAtlasSprite[] sheets = {
        Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(PlaqueSheet.GLYPH_SPRITE),
        Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(PlaqueSheet.BODY_SPRITE)};
    float[][] sizes = {{PlaqueSheet.GLYPH_W, PlaqueSheet.GLYPH_H},
        {PlaqueSheet.BODY, PlaqueSheet.BODY}};
    int turns = (facing.getHorizontalIndex() + 2) % 4;
    PlaqueLayout.Result r = PlaqueLayout.layout(style, plaque);
    List<BakedQuad> out = new ArrayList<>(r.pieces.size() + 4);
    if (r.plate != null) {
      sides(out, sheets[1], r.plate, r.depth, r.edgeRgb, turns);
    }
    for (Piece p : r.pieces) {
      out.add(quad(sheets[p.sheet], sizes[p.sheet], p, turns));
    }
    return out;
  }

  /** One piece: facing the reader (south, with the wall north) or, a back piece, the wall. */
  private static BakedQuad quad(TextureAtlasSprite sheet, float[] size, Piece p, int turns) {
    // corners anticlockwise as seen by whoever the face is for, with each one's u and v
    float[][] c = p.back
        ? new float[][]{{p.x1, p.y0, p.u1, p.v1}, {p.x0, p.y0, p.u0, p.v1},
        {p.x0, p.y1, p.u0, p.v0}, {p.x1, p.y1, p.u1, p.v0}}
        : new float[][]{{p.x0, p.y0, p.u0, p.v1}, {p.x1, p.y0, p.u1, p.v1},
            {p.x1, p.y1, p.u1, p.v0}, {p.x0, p.y1, p.u0, p.v0}};
    int[] data = new int[28];
    for (int i = 0; i < 4; i++) {
      vertex(data, i, c[i][0], c[i][1], p.d, turns, p.rgb, sheet,
          c[i][2] / size[0] * 16f, c[i][3] / size[1] * 16f);
    }
    EnumFacing normal = turn(p.back ? EnumFacing.NORTH : EnumFacing.SOUTH, turns);
    return new BakedQuad(data, -1, normal, sheet, p.shade, DefaultVertexFormats.BLOCK);
  }

  /** The plate's four edges, in its edge colour, shaded by the world as any block side is. */
  private static void sides(List<BakedQuad> out, TextureAtlasSprite sheet, float[] plate,
      float depth, int rgb, int turns) {
    float x0 = plate[0];
    float y0 = plate[1];
    float x1 = plate[2];
    float y1 = plate[3];
    float u = (PlaqueSheet.WHITE[0] + PlaqueSheet.WHITE[2] / 2f) / PlaqueSheet.BODY * 16f;
    float v = (PlaqueSheet.WHITE[1] + PlaqueSheet.WHITE[2] / 2f) / PlaqueSheet.BODY * 16f;
    float[][][] faces = {
        {{x1, y0, 0}, {x1, y1, 0}, {x1, y1, depth}, {x1, y0, depth}},     // east
        {{x0, y0, depth}, {x0, y1, depth}, {x0, y1, 0}, {x0, y0, 0}},     // west
        {{x0, y1, depth}, {x1, y1, depth}, {x1, y1, 0}, {x0, y1, 0}},     // up
        {{x0, y0, 0}, {x1, y0, 0}, {x1, y0, depth}, {x0, y0, depth}}};    // down
    EnumFacing[] normals = {EnumFacing.EAST, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN};
    for (int f = 0; f < 4; f++) {
      int[] data = new int[28];
      for (int i = 0; i < 4; i++) {
        float[] p = faces[f][i];
        vertex(data, i, p[0], p[1], p[2], turns, rgb, sheet, u, v);
      }
      out.add(new BakedQuad(data, -1, turn(normals[f], turns), sheet, true,
          DefaultVertexFormats.BLOCK));
    }
  }

  /**
   * Writes one vertex in the BLOCK format (position, colour, texture, lightmap; seven ints),
   * turned about the block's middle by {@code turns} quarter turns as a blockstate's {@code y}
   * turns a model (north to east).
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
    data[o + 4] = Float.floatToRawIntBits(sheet.getInterpolatedU(u));
    data[o + 5] = Float.floatToRawIntBits(sheet.getInterpolatedV(v));
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

  /** The cache's key: the block, its facing and what it says. */
  private static final class Key {

    private final PlaqueStyle style;
    private final EnumFacing facing;
    private final AddressPlaque plaque;

    Key(PlaqueStyle style, EnumFacing facing, AddressPlaque plaque) {
      this.style = style;
      this.facing = facing;
      this.plaque = plaque;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Key)) {
        return false;
      }
      Key k = (Key) o;
      return k.style == style && k.facing == facing && k.plaque.equals(plaque);
    }

    @Override
    public int hashCode() {
      return Objects.hash(style, facing, plaque);
    }
  }
}
