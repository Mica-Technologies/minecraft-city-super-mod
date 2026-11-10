package com.micatechnologies.minecraft.csm.codeutils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The state mapper of a block that carries {@link ICsmModelStateRules}: vanilla's mapping, less
 * the properties the block says never change its model, with every state the block says it is
 * never drawn in sent to the default state's location.
 *
 * <p>The model loader bakes a variant per distinct location a mapper hands out, so each ignored
 * property divides the block's variants by its number of values and each unreachable state
 * removes one outright.
 *
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public final class CsmModelStateRulesMapper extends StateMapperBase {

  private final Block block;
  private final ICsmModelStateRules rules;
  private final Set<IProperty<?>> ignored;

  /**
   * Constructs the mapper for one block.
   *
   * @param block the block, which must implement {@link ICsmModelStateRules}
   */
  public CsmModelStateRulesMapper(Block block) {
    this.block = block;
    this.rules = (ICsmModelStateRules) block;
    this.ignored = new HashSet<>(Arrays.asList(rules.getModelIgnoredProperties()));
  }

  @Override
  protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
    IBlockState drawn = rules.isModelStateReachable(state) ? state : block.getDefaultState();
    Map<IProperty<?>, Comparable<?>> properties = new LinkedHashMap<>(drawn.getProperties());
    properties.keySet().removeAll(ignored);
    return new ModelResourceLocation(Block.REGISTRY.getNameForObject(block),
        getPropertyString(properties));
  }
}
