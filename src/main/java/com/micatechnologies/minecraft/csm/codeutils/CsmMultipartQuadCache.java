package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.MultipartBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Keeps each state's quads for the multipart models of blocks that ask for it
 * ({@link ICsmMultipartQuadCached}), so the rules are tested once a state rather than on every
 * draw.
 *
 * <p>Vanilla's {@link MultipartBakedModel#getQuads} walks every rule and tests its predicate,
 * which for a value list ({@code "shape": "3|7|11"}) is one comparison a value. A signal arm's
 * blockstate is up to 424 rules and 7,500 values, and a chunk rebuild asks each block seven
 * times (once per side and once for the general quads). The answer depends on the state alone
 * for these blocks, so the first answer for a state and side is kept and handed out after.</p>
 *
 * <p>States are the block's own canonical objects, so they are keyed by identity through the
 * map's {@code equals} (a state does not override it). An extended state is not canonical, and
 * is passed on uncached. The lists are filled from the chunk builder threads; a race between two
 * threads only computes the same list twice.</p>
 *
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public final class CsmMultipartQuadCache {

  private static final int GENERAL = EnumFacing.values().length;

  private CsmMultipartQuadCache() {
  }

  /**
   * The {@link CsmBakedModelWrappers} wrapper: a cache round the multipart model of a block that
   * implements {@link ICsmMultipartQuadCached}.
   *
   * @param location the model's location
   * @param model    the baked model
   *
   * @return the cached model, or {@code null} to leave the model as it is
   */
  @Nullable
  public static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (!CsmConstants.MOD_NAMESPACE.equals(location.getNamespace())
        || !CsmMultipartStateMapper.VARIANT.equals(location.getVariant())
        || !(model instanceof MultipartBakedModel)) {
      return null;
    }
    Block block = Block.REGISTRY.getObject(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    return block instanceof ICsmMultipartQuadCached ? new Cached(model) : null;
  }

  /** A multipart model whose quads are kept per state and side. */
  public static final class Cached extends BakedModelWrapper<IBakedModel> {

    private final Map<IBlockState, AtomicReferenceArray<List<BakedQuad>>> byState =
        new ConcurrentHashMap<>();

    Cached(IBakedModel model) {
      super(model);
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
        long rand) {
      if (state == null || state instanceof IExtendedBlockState) {
        return originalModel.getQuads(state, side, rand);
      }
      AtomicReferenceArray<List<BakedQuad>> lists =
          byState.computeIfAbsent(state, s -> new AtomicReferenceArray<>(GENERAL + 1));
      int index = side == null ? GENERAL : side.ordinal();
      List<BakedQuad> quads = lists.get(index);
      if (quads == null) {
        List<BakedQuad> found = originalModel.getQuads(state, side, rand);
        quads = found.isEmpty() ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(found));
        lists.set(index, quads);
      }
      return quads;
    }
  }
}
