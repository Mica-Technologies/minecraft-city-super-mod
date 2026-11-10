package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.properties.IProperty;

/**
 * A block with properties that never change what it draws, which its model locations therefore
 * leave out.
 *
 * <p>A stored property is sometimes only an input to {@code getActualState}: a concrete pole
 * stores which base its owner picked, and works out from that and its neighbours what each end
 * of the shaft shows. The end properties pick the models; the stored choice picks nothing. Left
 * in the model location it would still multiply the variants a Forge blockstate bakes by its
 * number of values, each an identical copy (PERFORMANCE_AND_SECURITY.md, "State and memory
 * budget for new blocks").
 *
 * <p>Core registers a state mapper that ignores the listed properties for every block carrying
 * this interface, so the blockstate file does not name them at all. Such a block keeps its
 * per-state model locations otherwise: {@link CsmMultipartStateMapper} leaves a block with a
 * mapper of its own alone, so this is not for a block whose blockstate is multipart.
 *
 * @since 2026.11
 */
public interface ICsmModelIgnoredProperties {

  /**
   * The properties the block's model locations leave out.
   *
   * @return the properties that do not change the model; never {@code null}
   */
  IProperty<?>[] getModelIgnoredProperties();
}
