package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marker for a fixture that sits on the top of a pole, slipped over the tenon a pole top carries
 * for it: a post-top light, a park light.
 *
 * <p>A pole that draws its own ends -- the concrete poles, {@code BlockTrafficPoleConcrete} --
 * shows a collar and tenon at an end whose neighbour implements this, instead of the cap it
 * wears in the open or the plain end it shows against any other block. The fixture's own model
 * sits over the tenon, which is how the two read as one joint.
 *
 * <p>It is a Core marker because the fixtures live in the Lighting module and the poles in Roads,
 * and a module may only reference Core. The factory light classes expose a nested flavour that
 * implements it, for the same reason {@link ICsmPoleFitted} is a marker: whether a block is one is
 * a fact about its type, not a field.
 *
 * @since 2026.9
 */
public interface ICsmPostTopFixture {
}
