package com.micatechnologies.minecraft.csm.transit;

/**
 * The transit module's proxy on the client. The fare equipment is drawn from baked models and
 * needs no renderer, so there is nothing client-only to wire yet; the class exists so the
 * departure boards and anything else with a renderer have a place to bind it.
 *
 * @since 2026.9
 */
public class CsmTransitClientProxy extends CsmTransitCommonProxy {
}
