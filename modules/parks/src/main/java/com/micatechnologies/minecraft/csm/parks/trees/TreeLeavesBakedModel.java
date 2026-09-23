package com.micatechnologies.minecraft.csm.parks.trees;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A tree leaves block's baked model: {@link TreeLeavesGeometry} for the shape in the extended
 * state, faced with the species' leaf-cluster sprite. Cached per shape and graphics setting (at
 * most 256 x 2 lists per block); switching Fast/Fancy rebuilds the chunks, which asks again.
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TreeLeavesBakedModel implements IBakedModel {

  private final TreeLeafType type;
  private final TextureAtlasSprite leaves;
  private final ConcurrentHashMap<Integer, List<BakedQuad>> cache = new ConcurrentHashMap<>();

  public TreeLeavesBakedModel(TreeLeafType type, TextureAtlasSprite leaves) {
    this.type = type;
    this.leaves = leaves;
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    if (side != null) {
      return Collections.emptyList();
    }
    int key = TreeLeavesGeometry.key(63, 0);
    if (state instanceof IExtendedBlockState) {
      Integer value = ((IExtendedBlockState) state).getValue(BlockTreeLeaves.SHAPE);
      if (value != null) {
        key = value;
      }
    }
    boolean fancy = Minecraft.getMinecraft().gameSettings.fancyGraphics;
    int cacheKey = key | (fancy ? 1 << 8 : 0);
    List<BakedQuad> quads = cache.get(cacheKey);
    if (quads == null) {
      List<TreeLogGeometry.Quad> shape = TreeLeavesGeometry.quads(type, key, fancy);
      List<BakedQuad> baked = new ArrayList<>(shape.size());
      for (TreeLogGeometry.Quad q : shape) {
        baked.add(TreeLogBakedModel.bakeQuad(q, leaves));
      }
      quads = Collections.unmodifiableList(baked);
      cache.put(cacheKey, quads);
    }
    return quads;
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
    return leaves;
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
