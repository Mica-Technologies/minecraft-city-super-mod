package com.micatechnologies.minecraft.csm.parks.planters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A planter's model with its plant: the planter's own baked model, and the plant block's own
 * baked model moved down onto the planter's soil and scaled about the middle of the block, baked
 * into the chunk mesh with it. No tile entity renderer: a bed of a thousand planted planters
 * costs what a thousand plants standing on the ground cost, and nothing per frame.
 *
 * <p>The plant arrives through {@link Planters#PLANT}. Its quads are the plant model's own, every
 * side's, copied with their positions moved; their texture, tint index, shading and face are
 * kept, so a grass or fern is coloured by the biome through the planter's block colour
 * ({@link PlanterModels}), and a plant's cull-face quads join the general list, since moved off
 * the block's faces they must not be culled by a neighbour. The scale is the planter's
 * ({@link PlanterSoil}), reduced so the plant reaches no higher than the top of the block above.</p>
 *
 * <p>A planter draws in the cutout and cutout-mipped passes ({@link Planters#drawsIn}): its own
 * faces in its own pass, as before, and the plant's in the pass its block draws in (leaves are
 * mipped, a planter is not). An empty planter returns its own model's quads unchanged in its own
 * pass and nothing in the other, so it looks exactly as it did.</p>
 *
 * <p>Merged quad lists are cached per (plant state, pass) in each wrapper, and a wrapper stands
 * for one planter model variant, so the cache key is (planter model, plant, pass); the side
 * lists are the planter's own, passed through. The cache is bounded, so a world with every plant
 * in every planter holds a few hundred lists, not one per planter.</p>
 *
 * <p>Put in place through {@code CsmBakedModelWrappers}, so a pack running VintageFix's dynamic
 * resources (which bakes models as they are drawn) gets it too.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class PlanterBakedModel extends BakedModelWrapper<IBakedModel> {

  private static final int CACHE_SIZE = 256;

  private final ICsmPlanter planter;
  private final BlockRenderLayer ownLayer;
  private final Cache<Key, List<BakedQuad>> merged =
      CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();

  /**
   * Wraps one baked variant of a planter's blockstate.
   *
   * @param original the variant's own model
   * @param block    the planter
   */
  public PlanterBakedModel(IBakedModel original, Block block) {
    super(original);
    this.planter = (ICsmPlanter) block;
    this.ownLayer = block.getRenderLayer();
  }

  private static final class Key {

    final IBlockState plant;
    final BlockRenderLayer layer;

    Key(IBlockState plant, @Nullable BlockRenderLayer layer) {
      this.plant = plant;
      this.layer = layer;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Key)) {
        return false;
      }
      Key k = (Key) o;
      return plant == k.plant && layer == k.layer;
    }

    @Override
    public int hashCode() {
      return Objects.hash(System.identityHashCode(plant), layer);
    }
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
    boolean own = layer == null || layer == ownLayer;
    IBlockState plant = state instanceof IExtendedBlockState
        ? ((IExtendedBlockState) state).getValue(Planters.PLANT) : null;
    PlanterSoil soil = plant == null ? null : planter.getSoil(state);
    if (plant == null || soil == null) {
      return own ? originalModel.getQuads(state, side, rand) : Collections.emptyList();
    }
    if (side != null) {
      return own ? without(originalModel.getQuads(state, side, rand), soil)
          : Collections.emptyList();
    }
    try {
      return merged.get(new Key(plant, layer), () -> merge(state, rand, plant, layer, own, soil));
    } catch (ExecutionException | RuntimeException e) {
      return own ? originalModel.getQuads(state, null, rand) : Collections.emptyList();
    }
  }

  private List<BakedQuad> merge(IBlockState state, long rand, IBlockState plant,
      @Nullable BlockRenderLayer layer, boolean own, PlanterSoil soil) {
    List<BakedQuad> out = new ArrayList<>();
    if (own) {
      out.addAll(without(originalModel.getQuads(state, null, rand), soil));
    }
    if (layer == null || layer == layerOf(plant)) {
      out.addAll(plantQuads(plant, soil));
    }
    return Collections.unmodifiableList(out);
  }

  /** The pass a plant is drawn in: cutout-mipped if its block's is, else cutout. */
  static BlockRenderLayer layerOf(IBlockState plant) {
    return plant.getBlock().getRenderLayer() == BlockRenderLayer.CUTOUT_MIPPED
        ? BlockRenderLayer.CUTOUT_MIPPED : BlockRenderLayer.CUTOUT;
  }

  /** The planter's own quads, less any with the texture it leaves out while planted. */
  private static List<BakedQuad> without(List<BakedQuad> quads, PlanterSoil soil) {
    if (soil.getHiddenTexture() == null || quads.isEmpty()) {
      return quads;
    }
    List<BakedQuad> out = new ArrayList<>(quads.size());
    for (BakedQuad quad : quads) {
      if (!soil.hides(quad.getSprite().getIconName())) {
        out.add(quad);
      }
    }
    return out;
  }

  /** The plant's own model's quads, every side's, moved onto the soil and scaled. */
  private static List<BakedQuad> plantQuads(IBlockState plant, PlanterSoil soil) {
    IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
        .getModelForState(plant);
    List<BakedQuad> source = new ArrayList<>(model.getQuads(plant, null, 0L));
    for (EnumFacing facing : EnumFacing.values()) {
      source.addAll(model.getQuads(plant, facing, 0L));
    }
    float top = 0.0F;
    for (BakedQuad quad : source) {
      int offset = positionOffset(quad.getFormat());
      if (offset < 0) {
        continue;
      }
      int[] data = quad.getVertexData();
      int stride = quad.getFormat().getIntegerSize();
      for (int v = 0; v < 4; v++) {
        top = Math.max(top, Float.intBitsToFloat(data[v * stride + offset + 1]));
      }
    }
    float scale = soil.scaleFor(top * 16.0F);
    float lift = soil.getSoil() / 16.0F;
    List<BakedQuad> out = new ArrayList<>(source.size());
    for (BakedQuad quad : source) {
      BakedQuad moved = move(quad, scale, lift);
      if (moved != null) {
        out.add(moved);
      }
    }
    return out;
  }

  /** Where a vertex's position starts, in ints, or -1 if it is not three floats. */
  private static int positionOffset(VertexFormat format) {
    for (int i = 0; i < format.getElementCount(); i++) {
      VertexFormatElement element = format.getElement(i);
      if (element.getUsage() == VertexFormatElement.EnumUsage.POSITION) {
        return element.getType() == VertexFormatElement.EnumType.FLOAT
            && element.getElementCount() >= 3 ? format.getOffset(i) / 4 : -1;
      }
    }
    return -1;
  }

  /** A copy of a quad scaled about the block's middle and its foot, then lifted. */
  @Nullable
  private static BakedQuad move(BakedQuad quad, float scale, float lift) {
    VertexFormat format = quad.getFormat();
    int offset = positionOffset(format);
    if (offset < 0) {
      return null;
    }
    int[] data = quad.getVertexData().clone();
    int stride = format.getIntegerSize();
    for (int v = 0; v < 4; v++) {
      int i = v * stride + offset;
      float x = Float.intBitsToFloat(data[i]);
      float y = Float.intBitsToFloat(data[i + 1]);
      float z = Float.intBitsToFloat(data[i + 2]);
      data[i] = Float.floatToRawIntBits(0.5F + (x - 0.5F) * scale);
      data[i + 1] = Float.floatToRawIntBits(lift + y * scale);
      data[i + 2] = Float.floatToRawIntBits(0.5F + (z - 0.5F) * scale);
    }
    return new BakedQuad(data, quad.getTintIndex(), quad.getFace(), quad.getSprite(),
        quad.shouldApplyDiffuseLighting(), format);
  }
}
