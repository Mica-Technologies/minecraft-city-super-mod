package com.micatechnologies.minecraft.csm.trafficsignals;

public enum BlankoutBoxConfigAction {
    CYCLE_BODY_COLOR,
    CYCLE_VISOR_COLOR,
    CYCLE_VISOR_TYPE,
    CYCLE_MOUNT_TYPE,
    CYCLE_BODY_TILT,
    CYCLE_SIGN_TYPE,
    /**
     * Cycles the block's {@code COLOR} signal-state property — the same value a linked controller
     * drives. This is how an unlinked box (a No-U-Turn or Train legend, say) is switched on and off
     * by hand, without any extra state of its own.
     */
    CYCLE_SIGNAL_COLOR
}
