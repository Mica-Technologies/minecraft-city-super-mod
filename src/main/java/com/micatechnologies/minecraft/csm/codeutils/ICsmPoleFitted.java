package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marker for a side-mounted accessory whose model is drawn to meet the pole behind it, and which
 * therefore wears a different model per pole width.
 *
 * <p>A block implementing this gets {@link CsmPoleFit#PROPERTY} added to its state container by
 * {@link AbstractBlockRotatableNSEW} or {@link AbstractBlockRotatableNSEWUD}, and resolved in its
 * actual state from the block behind it. Its blockstate must carry a {@code polefit} variant
 * block naming the {@code _thin} and {@code _pedestal} models, which
 * {@code dev-env-utils/scripts/gen_pole_fit_models.py} generates and wires up from the catalogue
 * it holds. A block that implements this without an entry in that catalogue renders the same
 * model for every fit, which is harmless but pointless.
 *
 * <p>It is a marker rather than a constructor flag because the decision is needed inside
 * {@code createBlockState}, which the {@link net.minecraft.block.Block} constructor calls before
 * any subclass field exists. The factory block classes expose a {@code PoleFitted} subclass for
 * exactly this reason.
 *
 * <p>A fitted arm is also {@link ICsmTrafficPoleIgnored}: it draws its own joint to the pole,
 * which is exactly the hardware a pole's mount stub depicts, and the two together read as a
 * broken joint -- a band and bracket sprouting into a plate that is already bolted on. Extending
 * the marker here, rather than asking every implementer to remember both, is what keeps the two
 * from drifting apart; {@code IGNORE_BLOCK} matches by assignability, so the sub-interface is
 * covered.
 *
 * @see CsmPoleFit
 * @since 2026.9
 */
public interface ICsmPoleFitted extends ICsmTrafficPoleIgnored {
}
