package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

/**
 * A block that tells Core which of its states need a model of their own.
 *
 * <p>A Forge-format blockstate bakes one variant for every combination of the properties in a
 * block's model locations, drawn or not (PERFORMANCE_AND_SECURITY.md, "State and memory budget
 * for new blocks"). Two kinds of combination are baked for nothing, and a block carrying this
 * interface names them:
 *
 * <ul>
 *   <li><b>A property that never changes what is drawn.</b> A stored property is sometimes only
 *       an input to {@code getActualState}: a concrete pole stores which base its owner picked
 *       and works out from that and its neighbours what each end of the shaft shows. The end
 *       properties pick the models; the stored choice picks nothing, yet in the model location
 *       it multiplies the variants by its number of values. Such a property is listed by
 *       {@link #getModelIgnoredProperties}, is left out of the model locations, and is not
 *       named in the blockstate file.</li>
 *   <li><b>A state the block is never drawn in.</b> Properties set in {@code getActualState}
 *       are rarely independent: a pole's plinth is only ever at the end that points down, and a
 *       pole lying on its side has no foot at all. {@link #isModelStateReachable} says no to a
 *       combination the actual state never produces, and every such state shares the default
 *       state's model location instead of having one, and a variant, of its own.</li>
 * </ul>
 *
 * <p>Core registers {@code CsmModelStateRulesMapper} for every block carrying this interface.
 * Such a block keeps per-state model locations otherwise: {@link CsmMultipartStateMapper} leaves
 * a block that has a mapper of its own alone, so this is not for a multipart blockstate.
 *
 * <p><b>Get {@link #isModelStateReachable} wrong and nothing fails.</b> A state it wrongly calls
 * unreachable is drawn as the block's default state, with no error. Keep the rule to what
 * {@code getActualState} plainly cannot produce, and say yes when unsure.
 *
 * @since 2026.11
 */
public interface ICsmModelStateRules {

  /**
   * The properties the block's model locations leave out.
   *
   * @return the properties that do not change the model; never {@code null}
   */
  default IProperty<?>[] getModelIgnoredProperties() {
    return new IProperty<?>[0];
  }

  /**
   * Whether the block can be drawn in a state. Asked once per state, when the models load.
   *
   * @param state one of the block's valid states
   *
   * @return {@code false} if {@code getActualState} never produces this combination
   */
  default boolean isModelStateReachable(IBlockState state) {
    return true;
  }
}
