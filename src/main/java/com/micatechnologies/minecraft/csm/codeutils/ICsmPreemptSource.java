package com.micatechnologies.minecraft.csm.codeutils;

import java.util.List;
import net.minecraft.world.World;

/**
 * Something that knows where the preemption emitters in a world are: emergency vehicles running
 * their lights, or anything else that should call a traffic signal preempt as it approaches.
 * <p>
 * Roads' preempt detector asks {@link CsmPreemptSources} rather than any source directly, so
 * Roads never names the module or the mod a source comes from. A source is registered from its
 * module's pre-initialization.
 *
 * @version 1.0
 * @see CsmPreemptSources
 * @since 2026.10
 */
public interface ICsmPreemptSource {

  /**
   * Adds every emitter in the world that is on now. Called at most once a world tick, and only
   * on the server, when some detector asks; the result is shared by every detector that tick.
   * <p>
   * This runs from a tile entity's update, so it must be cheap: one sweep of what the source
   * already tracks, never a search of the world's blocks or a chunk load.
   *
   * @param world the world
   * @param out   the list to add the emitters to
   *
   * @since 1.0
   */
  void collectEmitters(World world, List<CsmPreemptEmitter> out);
}
