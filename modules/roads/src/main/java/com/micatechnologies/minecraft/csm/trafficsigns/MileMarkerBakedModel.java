package com.micatechnologies.minecraft.csm.trafficsigns;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.model.ITransformation;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.vector.Vector3f;

/**
 * A mile marker's model: the plate its blockstate draws, with the legend its tile entity sets
 * added as quads baked into the chunk mesh.
 *
 * <p>The legend arrives through {@link BlockMileMarkerSign#LEGEND}. {@link MileMarkerFaces} lays
 * it out in the unturned model, and each piece is baked by the vanilla {@link FaceBakery} under
 * the same transformation the blockstate's {@code facing} variant gives the plate -- a y turn of
 * {@link DirectionEight#getRotationDegrees()}, built the way Forge's blockstate loader builds
 * it -- so the legend and the plate cannot disagree about which way the sign faces, the eight
 * facings included. Quads are cached by (legend, facing, shift): a highway of markers bakes each
 * number once, and nothing here runs per frame.</p>
 *
 * <p>A route number takes its colour from its shield, written into the quad's vertex colour; both
 * lighting pipelines multiply their shade and ambient occlusion into that colour, so the number is
 * shaded exactly as the shield under it is (the route marker sign's renderer has to do that by
 * hand).</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class MileMarkerBakedModel extends BakedModelWrapper<IBakedModel> {

  private static final FaceBakery BAKERY = new FaceBakery();

  /**
   * A turn of nothing, passed where the bakery takes an element's rotation. Given none, the
   * bakery squares every face up to the nearest side of the block (its applyFacing), which undoes
   * the 45 degree turn of a diagonal facing; given this, it leaves the corners where the facing's
   * transformation put them. The plate models carry the same zero rotation for the same reason.
   */
  private static final BlockPartRotation NO_TURN =
      new BlockPartRotation(new Vector3f(0.5f, 0.5f, 0.5f), EnumFacing.Axis.Y, 0.0f, false);

  private final MileMarkerLayout layout;
  private final TextureAtlasSprite sheet;
  private final Map<GuideSignShieldType, TextureAtlasSprite> shields;
  private final Cache<Key, List<BakedQuad>> cache =
      CacheBuilder.newBuilder().maximumSize(2048).build();

  /**
   * Wraps one baked variant of a mile marker's blockstate.
   *
   * @param original the variant's own model
   * @param layout   the plate
   * @param sheet    the glyph sheet's sprite
   * @param shields  each route shield's face sprite
   */
  public MileMarkerBakedModel(IBakedModel original, MileMarkerLayout layout,
      TextureAtlasSprite sheet, Map<GuideSignShieldType, TextureAtlasSprite> shields) {
    super(original);
    this.layout = layout;
    this.sheet = sheet;
    this.shields = shields;
  }

  private static final class Key {

    final MileMarkerLegend legend;
    final DirectionEight facing;
    final SignShift shift;

    Key(MileMarkerLegend legend, DirectionEight facing, SignShift shift) {
      this.legend = legend;
      this.facing = facing;
      this.shift = shift;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Key)) {
        return false;
      }
      Key k = (Key) o;
      return facing == k.facing && shift == k.shift && legend.equals(k.legend);
    }

    @Override
    public int hashCode() {
      return Objects.hash(legend, facing, shift);
    }
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    List<BakedQuad> base = originalModel.getQuads(state, side, rand);
    if (side != null) {
      return base;
    }
    MileMarkerLegend legend = null;
    DirectionEight facing = DirectionEight.N;
    SignShift shift = SignShift.NONE;
    if (state != null) {
      if (state instanceof IExtendedBlockState) {
        legend = ((IExtendedBlockState) state).getValue(BlockMileMarkerSign.LEGEND);
      }
      facing = state.getValue(AbstractBlockSign.FACING);
      shift = state.getValue(AbstractBlockSign.SHIFT);
    }
    if (legend == null) {
      legend = MileMarkerLegend.defaultFor(layout);
    }
    List<BakedQuad> overlay;
    Key key = new Key(legend, facing, shift);
    try {
      overlay = cache.get(key, () -> bake(key));
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

  private List<BakedQuad> bake(Key key) {
    ITransformation turn = transformation(key.facing, layout.getLift());
    List<BakedQuad> out = new ArrayList<>();
    for (MileMarkerFaces.Piece piece : MileMarkerFaces.pieces(layout, key.legend, key.shift)) {
      TextureAtlasSprite sprite = piece.source == MileMarkerFaces.Source.SHEET ? sheet
          : shields.get(key.legend.getShield());
      if (sprite == null) {
        continue;
      }
      BlockPartFace face = new BlockPartFace(EnumFacing.NORTH, -1, "",
          new BlockFaceUV(piece.uv, 0));
      BakedQuad quad = BAKERY.makeBakedQuad(new Vector3f(piece.x0, piece.y0, piece.z),
          new Vector3f(piece.x1, piece.y1, piece.z), face, sprite, EnumFacing.NORTH, turn,
          NO_TURN, false, true);
      if (piece.colour != -1) {
        tint(quad.getVertexData(), piece.colour);
      }
      out.add(quad);
    }
    return out;
  }

  /**
   * The transformation the blockstate gives a facing, built as Forge's blockstate loader builds a
   * {@code "transform": {"translation": [0, lift, 0], "rotation": [{"x": 0}, {"y": a},
   * {"z": 0}]}}: a quaternion about y and the plate's lift, moved from the block's centre to its
   * corner.
   *
   * @param facing the facing
   * @param lift   the plate's lift, in model units ({@link MileMarkerLayout#getLift()})
   */
  static ITransformation transformation(DirectionEight facing, float lift) {
    float degrees = facing.getRotationDegrees();
    if (degrees == 0.0f && lift == 0.0f) {
      return TRSRTransformation.identity();
    }
    Quat4f rotation = null;
    if (degrees != 0.0f) {
      rotation = new Quat4f();
      rotation.set(new AxisAngle4d(0, 1, 0, Math.toRadians(degrees)));
    }
    javax.vecmath.Vector3f translation =
        lift == 0.0f ? null : new javax.vecmath.Vector3f(0, lift / 16.0f, 0);
    return TRSRTransformation.blockCenterToCorner(
        new TRSRTransformation(translation, rotation, null, null));
  }

  /** Writes an opaque 0xRRGGBB into every vertex's colour (ITEM format, ABGR in memory). */
  private static void tint(int[] data, int rgb) {
    int abgr = 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    int stride = data.length / 4;
    for (int v = 0; v < 4; v++) {
      data[v * stride + 3] = abgr;
    }
  }

  /**
   * Keeps this model as the one drawn: the wrapped model's own answer names itself, and the item
   * renderer would then draw the plate without its legend.
   */
  @Override
  @Nonnull
  public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
      @Nonnull ItemCameraTransforms.TransformType type) {
    Pair<? extends IBakedModel, Matrix4f> pair = originalModel.handlePerspective(type);
    return Pair.of(this, pair.getRight());
  }
}
