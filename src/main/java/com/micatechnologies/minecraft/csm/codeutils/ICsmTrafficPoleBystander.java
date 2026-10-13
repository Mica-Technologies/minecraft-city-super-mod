package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marker interface for blocks that stand beside a traffic pole rather than on it: trees and
 * plantings, park amenities, furniture, props.
 *
 * <p>A pole sprouts a mount stub toward whatever is next to it, which is right for a wall or a
 * sign and wrong for a bench, a crate or a tree trunk that happens to stand there. These are
 * {@link ICsmTrafficPoleIgnored}, so no stub reaches them.
 *
 * <p>They differ from the rest of that marker at a pole's ends. The other ignored blocks are pole
 * hardware -- a signal head on the post top -- which a pole's end runs straight into. A bystander
 * is not, so the pole family's end rules treat it as they treat vanilla leaves or a carpet: the
 * end still wears its cap.
 *
 * @see AbstractBlockTrafficPole#IGNORE_BLOCK
 * @since 2026.10
 */
public interface ICsmTrafficPoleBystander extends ICsmTrafficPoleIgnored {
}
