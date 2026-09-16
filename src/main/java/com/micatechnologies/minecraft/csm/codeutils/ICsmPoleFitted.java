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
 * @see CsmPoleFit
 * @since 2026.9
 */
public interface ICsmPoleFitted {
}
