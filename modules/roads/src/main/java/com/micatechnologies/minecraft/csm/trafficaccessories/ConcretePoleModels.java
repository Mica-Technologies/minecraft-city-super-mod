package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleConcrete.EndStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a {@link BlockTrafficPoleConcrete} with the ends its neighbours give it, from the same
 * blockstate variants it was always drawn from.
 *
 * <p>What each end of the shaft shows ({@code endn}, {@code ends}) is worked out from the
 * neighbours, so it used to be actual state; in the state table it multiplied the owner's stored
 * choices of top and base into 20,736 states a pole. It is unlisted now
 * ({@link BlockTrafficPoleConcrete#getExtendedState}), and a pole's state table is its facing,
 * mounts and the two choices: 576 states. The model is unchanged. The blockstate file still names
 * every end pair, and each variant that has one,
 * {@code endn=<n>,ends=<s>,facing=...,mountwest=...}, is still loaded and baked, as an extra
 * variant of the pole's item ({@link #registerVariants}), exactly as many of them as the state
 * mapper used to hand out (1,408: {@link BlockTrafficPoleConcrete#isEndPairReachable} times the
 * 16 mount combinations). A state's own location names only the facing and mounts; its model is
 * wrapped here, and draws the baked variant of the same facing and mounts with the state's two
 * ends, looked up once per pair and kept.</p>
 *
 * <p>A state without its ends (a caller that never asks for the extended state) draws the
 * location's own model: the blockstate's default for the facing and mounts.</p>
 *
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public final class ConcretePoleModels {

  private static final EndStyle[] ENDS = EndStyle.values();

  private ConcretePoleModels() {
  }

  /** Registers the wrapper and the variant loading; called from the Roads client pre-init. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new ConcretePoleModels());
    CsmBakedModelWrappers.register(ConcretePoleModels::wrap);
  }

  /**
   * Has every variant a pole can be drawn in loaded and baked: each reachable end pair with each
   * facing and mount combination, named as the state mapper named them when the ends were listed.
   */
  @SubscribeEvent
  public void registerVariants(ModelRegistryEvent event) {
    for (Block block : CsmRegistry.getBlocks()) {
      if (!(block instanceof BlockTrafficPoleConcrete) || block.getRegistryName() == null) {
        continue;
      }
      Item item = Item.getItemFromBlock(block);
      if (item == null || item == net.minecraft.init.Items.AIR) {
        continue;
      }
      List<ResourceLocation> variants = new ArrayList<>();
      for (EnumFacing facing : EnumFacing.values()) {
        for (EndStyle north : ENDS) {
          for (EndStyle south : ENDS) {
            if (!BlockTrafficPoleConcrete.isEndPairReachable(facing, north, south)) {
              continue;
            }
            for (int mounts = 0; mounts < 16; mounts++) {
              variants.add(new ModelResourceLocation(block.getRegistryName(),
                  ends(north, south) + baseVariant(facing, mounts)));
            }
          }
        }
      }
      ModelLoader.registerItemVariants(item, variants.toArray(new ResourceLocation[0]));
    }
  }

  /** The leading part of a variant with ends: the end properties sort before {@code facing}. */
  private static String ends(EndStyle north, EndStyle south) {
    return "endn=" + north.getName() + ",ends=" + south.getName() + ",";
  }

  /** A state's own location's variant: facing and mounts, as the state mapper writes them. */
  private static String baseVariant(EnumFacing facing, int mounts) {
    return "facing=" + facing.getName()
        + ",mountdown=" + ((mounts & 1) != 0)
        + ",mounteast=" + ((mounts & 2) != 0)
        + ",mountup=" + ((mounts & 4) != 0)
        + ",mountwest=" + ((mounts & 8) != 0);
  }

  /** The wrapper round a pole state's own model; {@code null} for any other model. */
  @Nullable
  private static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof WithEnds || "inventory".equals(location.getVariant())
        || location.getVariant().startsWith("endn=")
        || !"csm".equals(location.getNamespace())) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    return block instanceof BlockTrafficPoleConcrete ? new WithEnds(model, location) : null;
  }

  /** A pole state's model: the baked variant for its ends. */
  private static final class WithEnds extends BakedModelWrapper<IBakedModel> {

    private final ResourceLocation block;
    private final String variant;
    /** The variant for each end pair, by {@code north * ENDS.length + south}, once looked up. */
    private final AtomicReferenceArray<IBakedModel> byEnds =
        new AtomicReferenceArray<>(ENDS.length * ENDS.length);

    WithEnds(IBakedModel original, ModelResourceLocation location) {
      super(original);
      this.block = new ResourceLocation(location.getNamespace(), location.getPath());
      this.variant = location.getVariant();
    }

    /** The model for a state's ends, or this location's own when the state carries none. */
    private IBakedModel forState(@Nullable IBlockState state) {
      if (!(state instanceof IExtendedBlockState)) {
        return originalModel;
      }
      IExtendedBlockState ext = (IExtendedBlockState) state;
      EndStyle north = ext.getValue(BlockTrafficPoleConcrete.END_NORTH);
      EndStyle south = ext.getValue(BlockTrafficPoleConcrete.END_SOUTH);
      if (north == null || south == null) {
        return originalModel;
      }
      int slot = north.ordinal() * ENDS.length + south.ordinal();
      IBakedModel found = byEnds.get(slot);
      if (found == null) {
        found = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()
            .getModelManager().getModel(
                new ModelResourceLocation(block, ends(north, south) + variant));
        byEnds.set(slot, found);
      }
      return found;
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
        long rand) {
      return forState(state).getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion(@Nonnull IBlockState state) {
      return forState(state).isAmbientOcclusion(state);
    }
  }
}
