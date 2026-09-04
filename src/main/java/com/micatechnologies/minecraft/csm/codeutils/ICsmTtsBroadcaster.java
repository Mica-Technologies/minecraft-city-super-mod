package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marks a tile entity that speakers can be linked to as a speech broadcast source — in practice
 * the Redstone TTS Module's tile entity, which lives in the Text to Speech module.
 *
 * <p>The speaker blocks are Technology's, but the module they link to is not, and Technology may
 * not reference the Text to Speech module: Text to Speech already requires Technology, so a
 * reference back the other way would be a compile-time cycle between the two jars. A speaker
 * still has to recognise a live broadcast source to know that its link is stale, so Core carries
 * the type it recognises it by and neither module has to know the other's classes.</p>
 *
 * <p>There is nothing to implement. A speaker only ever asks whether the tile entity at the
 * position it remembers is still a broadcast source; every method it would call on one is called
 * from the Text to Speech module, which has the concrete type in hand.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public interface ICsmTtsBroadcaster {

}
