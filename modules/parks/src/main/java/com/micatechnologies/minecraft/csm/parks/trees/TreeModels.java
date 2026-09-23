package com.micatechnologies.minecraft.csm.parks.trees;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Puts the tree kit's baked models in place of the placeholders their blockstates name.
 *
 * <p>A log's blockstate lists {@code axis=x}, {@code axis=y} and {@code axis=z} (and
 * {@code inventory}), each a plain JSON model of a straight log. The JSON ones stay for the item
 * and, just as usefully, put the bark on the texture atlas; the three world variants are replaced
 * here, at bake time, by one {@link TreeLogBakedModel} per log. No custom state mapper is used:
 * one registered in pre-initialization is keyed on a registry name that does not exist yet, and
 * collapses every block into one entry (see PEDESTAL_POLE_SYSTEM.md, "The trap that design walked
 * into").</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class TreeModels {

  private TreeModels() {
  }

  public static void register() {
    MinecraftForge.EVENT_BUS.register(new TreeModels());
  }

  @SubscribeEvent
  public void onModelBake(ModelBakeEvent event) {
    for (Block block : CsmRegistry.getBlocks()) {
      if (block instanceof BlockTreeLog) {
        BlockTreeLog log = (BlockTreeLog) block;
        TextureAtlasSprite bark = Minecraft.getMinecraft().getTextureMapBlocks()
            .getAtlasSprite(log.getWood().getBarkTexture());
        TreeLogBakedModel model = new TreeLogBakedModel(log.getWidth(), bark);
        for (EnumFacing.Axis axis : EnumFacing.Axis.values()) {
          event.getModelRegistry().putObject(new ModelResourceLocation(
              block.getRegistryName(), "axis=" + axis.getName()), model);
        }
      } else if (block instanceof BlockTreeLeaves) {
        BlockTreeLeaves leaves = (BlockTreeLeaves) block;
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
            .getAtlasSprite(leaves.getTexture());
        event.getModelRegistry().putObject(
            new ModelResourceLocation(block.getRegistryName(), "normal"),
            new TreeLeavesBakedModel(leaves.getLeafType(), sprite));
      }
    }
  }
}
