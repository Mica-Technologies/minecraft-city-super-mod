package com.micatechnologies.minecraft.csm.parks.trees;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A tree log's baked model: {@link TreeLogGeometry} for the connection mask in the block's
 * extended state, faced with the wood's bark, baked into the chunk like any block's.
 *
 * <p>Quads are built once per mask and cached, so a forest of logs in a handful of shapes bakes
 * a handful of quad lists. Nothing is culled against neighbours: a log's arms stop at the block
 * boundary and are joined, not hidden, by the next log's.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TreeLogBakedModel implements IBakedModel {

  private static final VertexFormat FORMAT = DefaultVertexFormats.ITEM;
  private static final int CACHE_LIMIT = 4096;

  private final TreeLogWidth width;
  private final TextureAtlasSprite bark;
  private final ConcurrentHashMap<Long, List<BakedQuad>> cache = new ConcurrentHashMap<>();

  public TreeLogBakedModel(TreeLogWidth width, TextureAtlasSprite bark) {
    this.width = width;
    this.bark = bark;
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    if (side != null) {
      return Collections.emptyList();
    }
    long mask = 0L;
    if (state instanceof IExtendedBlockState) {
      Long value = ((IExtendedBlockState) state).getValue(BlockTreeLog.CONNECTIONS);
      if (value != null) {
        mask = value;
      }
    }
    return quads(mask);
  }

  /** The quads for one mask, baked on first use. */
  public List<BakedQuad> quads(long mask) {
    List<BakedQuad> quads = cache.get(mask);
    if (quads == null) {
      if (cache.size() > CACHE_LIMIT) {
        cache.clear();
      }
      quads = bake(TreeLogGeometry.quads(width, mask));
      cache.put(mask, quads);
    }
    return quads;
  }

  private List<BakedQuad> bake(List<TreeLogGeometry.Quad> shape) {
    List<BakedQuad> out = new ArrayList<>(shape.size());
    for (TreeLogGeometry.Quad q : shape) {
      out.add(bakeQuad(q, bark));
    }
    return Collections.unmodifiableList(out);
  }

  /**
   * Bakes one geometry quad against a sprite.
   *
   * @param q      the quad, positions in sixteenths and UVs 0-16
   * @param sprite the sprite its UVs index
   *
   * @return the baked quad
   */
  public static BakedQuad bakeQuad(TreeLogGeometry.Quad q, TextureAtlasSprite sprite) {
    double[] n = q.faceNormal();
    UnpackedBakedQuad.Builder b = new UnpackedBakedQuad.Builder(FORMAT);
    b.setQuadOrientation(EnumFacing.getFacingFromVector((float) n[0], (float) n[1], (float) n[2]));
    b.setTexture(sprite);
    b.setApplyDiffuseLighting(true);
    for (int c = 0; c < 4; c++) {
      for (int e = 0; e < FORMAT.getElementCount(); e++) {
        VertexFormatElement el = FORMAT.getElement(e);
        switch (el.getUsage()) {
          case POSITION:
            b.put(e, (float) (q.pos[c][0] / 16), (float) (q.pos[c][1] / 16),
                (float) (q.pos[c][2] / 16), 1F);
            break;
          case UV:
            if (el.getIndex() == 0) {
              b.put(e, sprite.getInterpolatedU(q.uv[c][0]), sprite.getInterpolatedV(q.uv[c][1]),
                  0F, 1F);
            } else {
              b.put(e);
            }
            break;
          case COLOR:
            b.put(e, 1F, 1F, 1F, 1F);
            break;
          case NORMAL:
            b.put(e, (float) q.normal[c][0], (float) q.normal[c][1], (float) q.normal[c][2], 0F);
            break;
          default:
            b.put(e);
            break;
        }
      }
    }
    return b.build();
  }

  @Override
  public boolean isAmbientOcclusion() {
    return true;
  }

  @Override
  public boolean isGui3d() {
    return true;
  }

  @Override
  public boolean isBuiltInRenderer() {
    return false;
  }

  @Override
  @Nonnull
  public TextureAtlasSprite getParticleTexture() {
    return bark;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public ItemCameraTransforms getItemCameraTransforms() {
    return ItemCameraTransforms.DEFAULT;
  }

  @Override
  @Nonnull
  public ItemOverrideList getOverrides() {
    return ItemOverrideList.NONE;
  }
}
