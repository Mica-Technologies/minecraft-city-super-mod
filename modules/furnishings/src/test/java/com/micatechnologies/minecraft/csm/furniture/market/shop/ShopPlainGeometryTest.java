package com.micatechnologies.minecraft.csm.furniture.market.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/**
 * Which item models a shop compiles an enchanted shelf from ({@link
 * TileEntityShopRenderer#isPlainGeometry}): only those whose every quad draws with no change of
 * lighting or shading, since Forge's emissive-item path switches GL lighting for the others
 * through cached calls a display list cannot hold, and never a built-in renderer.
 */
class ShopPlainGeometryTest {

  @Test
  void ordinaryItemQuadsArePlain() {
    assertTrue(TileEntityShopRenderer.isPlainGeometry(
        model(false, quad(true, DefaultVertexFormats.ITEM))));
  }

  @Test
  void aQuadWithoutDiffuseShadingIsNot() {
    assertFalse(TileEntityShopRenderer.isPlainGeometry(model(false,
        quad(true, DefaultVertexFormats.ITEM), quad(false, DefaultVertexFormats.ITEM))));
  }

  @Test
  void aQuadThatCanCarryItsOwnLightIsNot() {
    // BLOCK has a lightmap element (UV 1): such a quad may carry light the item path applies.
    assertFalse(TileEntityShopRenderer.isPlainGeometry(
        model(false, quad(true, DefaultVertexFormats.BLOCK))));
  }

  @Test
  void aBuiltInRendererIsNot() {
    assertFalse(TileEntityShopRenderer.isPlainGeometry(model(true)));
  }

  private static BakedQuad quad(boolean diffuse, VertexFormat format) {
    return new BakedQuad(new int[format.getIntegerSize() * 4], -1, EnumFacing.UP, null, diffuse,
        format);
  }

  /** A model whose general (unsided) quads are these, with no transforms of its own. */
  private static IBakedModel model(boolean builtIn, BakedQuad... quads) {
    List<BakedQuad> general = Arrays.asList(quads);
    return new IBakedModel() {
      @Override
      public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
          long rand) {
        return side == null ? general : Collections.emptyList();
      }

      @Override
      public boolean isAmbientOcclusion() {
        return false;
      }

      @Override
      public boolean isGui3d() {
        return false;
      }

      @Override
      public boolean isBuiltInRenderer() {
        return builtIn;
      }

      @Override
      public TextureAtlasSprite getParticleTexture() {
        return null;
      }

      @Override
      public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
      }

      @Override
      public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
      }

      // Forge's default goes through ForgeHooksClient, which needs LWJGL.
      @Override
      public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
        return Pair.of(this, null);
      }
    };
  }
}
