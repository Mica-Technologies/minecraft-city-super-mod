package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A glazed door's model, split between the cutout and translucent passes: the faces that carry
 * glass in the translucent pass, everything else -- the leaf's edges, handles, push bar and a
 * fitted closer -- in the cutout pass.
 *
 * <p>Minecraft draws the translucent pass without writing depth ({@code depthMask(false)}), and
 * orders its faces by a distance sort that is only redone as the camera moves. That is right for
 * glass and wrong for anything solid: a door whose whole model was translucent had its closer's
 * body, arm and shoe drawn in whatever order the sort left them, so from some angles the leaf
 * behind a closer body was painted over all but a sliver of it and the arm was left hanging with
 * nothing to meet (issues #235 and #236). In the cutout pass the hardware writes depth like any block, and
 * the glass drawn after it is hidden where the hardware stands in front.</p>
 *
 * <p>A face carries glass when it wears one of the door's own face textures ({@code _upper},
 * {@code _lower}), which is where every lite, pane and storefront glazing is drawn; the edge and
 * hardware textures are opaque. With no pass set -- the swing renderer, the breaking overlay -- the
 * model is whole.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class DoorLayerModel extends BakedModelWrapper<IBakedModel> {

  public DoorLayerModel(IBakedModel original) {
    super(original);
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    List<BakedQuad> quads = super.getQuads(state, side, rand);
    BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
    if (layer == null || quads.isEmpty()) {
      return quads;
    }
    boolean translucent = layer == BlockRenderLayer.TRANSLUCENT;
    List<BakedQuad> out = new ArrayList<>(quads.size());
    for (BakedQuad quad : quads) {
      if (glass(quad) == translucent) {
        out.add(quad);
      }
    }
    return out;
  }

  /** Whether a quad wears a door face texture, the only ones with glass drawn on them. */
  static boolean glass(BakedQuad quad) {
    String name = quad.getSprite().getIconName();
    return name.contains("/doors/") && (name.endsWith("_upper") || name.endsWith("_lower"));
  }

  /** The glazed doors' registry names, found on first use (after every block is registered). */
  private static volatile Set<String> glazed;

  /**
   * Puts the split model in place of a glazed door's, as it is baked. Registered with
   * {@link CsmBakedModelWrappers}, which applies it however the models are baked: walking the
   * registry here instead left every door whole in a pack running VintageFix, so the glass was
   * drawn opaque in the cutout pass (issue #242).
   *
   * @param location the model's location
   * @param model    the baked model
   * @return the split model, or {@code null} if this is not a glazed door's world model
   * @since 1.1
   */
  @Nullable
  public static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof DoorLayerModel || "inventory".equals(location.getVariant())
        || !glazedDoors().contains(location.getNamespace() + ":" + location.getPath())) {
      return null;
    }
    return new DoorLayerModel(model);
  }

  private static Set<String> glazedDoors() {
    Set<String> names = glazed;
    if (names == null) {
      names = ForgeRegistries.BLOCKS.getValuesCollection().stream()
          .filter(b -> b.getClass() == BlockBuildingDoor.class && ((BlockBuildingDoor) b).glazed())
          .map(Block::getRegistryName).filter(Objects::nonNull)
          .map(Object::toString).collect(Collectors.toSet());
      glazed = names;
    }
    return names;
  }
}
