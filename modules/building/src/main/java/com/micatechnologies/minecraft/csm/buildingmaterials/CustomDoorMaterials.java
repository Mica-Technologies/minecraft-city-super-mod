package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Which blocks a custom door can be made of, and (client side) the texture each lends it.
 *
 * <p>A material is any block drawn by an ordinary baked model that has no tile entity of its own --
 * stone, planks, glass, wool, metal blocks, other mods' building blocks -- but not a custom door
 * itself. Its texture is the sprite of its model's north face (a log's side, not its end), or its
 * particle sprite if it has no north face.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CustomDoorMaterials {

  private CustomDoorMaterials() {
  }

  /**
   * Whether a block can be a custom door's material.
   *
   * @param block the block
   *
   * @return whether it can
   *
   * @since 1.0
   */
  @SuppressWarnings("deprecation")
  public static boolean allowed(Block block) {
    if (block == null || block == Blocks.AIR || block instanceof BlockBuildingDoor) {
      return false;
    }
    IBlockState state = block.getDefaultState();
    return state.getRenderType() == EnumBlockRenderType.MODEL && !block.hasTileEntity(state);
  }

  /**
   * The texture a material lends a door.
   *
   * @param state the material
   *
   * @return its sprite
   *
   * @since 1.0
   */
  @SideOnly(Side.CLIENT)
  public static TextureAtlasSprite sprite(IBlockState state) {
    IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
        .getModelForState(state);
    try {
      List<BakedQuad> quads = model.getQuads(state, EnumFacing.NORTH, 0L);
      if (!quads.isEmpty()) {
        return quads.get(0).getSprite();
      }
    } catch (RuntimeException e) {
      // A model that needs world context to answer; its particle texture will do.
    }
    return model.getParticleTexture();
  }
}
