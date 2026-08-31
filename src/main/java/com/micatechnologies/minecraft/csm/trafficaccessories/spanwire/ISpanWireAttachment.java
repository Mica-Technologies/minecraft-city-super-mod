package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;

/**
 * A tile entity that can take part in a span of messenger cable -- an anchor at a pole, or a
 * hanger mount along the span.
 *
 * <p>Both kinds hold a complete copy of the span they belong to (see {@link SpanWireDefinition}),
 * so {@link SpanWireManager} can write, update and tear down a span without caring which kind of
 * attachment it is talking to at any given position.
 */
public interface ISpanWireAttachment {

  /** The span this attachment belongs to, or null if it is not part of one. */
  @Nullable
  SpanWireDefinition getSpan();

  /**
   * Replaces the span this attachment belongs to. Passing null detaches it. Implementations are
   * responsible for persisting the change and syncing it to clients.
   */
  void setSpan(@Nullable SpanWireDefinition span);

  /** This attachment's position, used to locate itself within its own span. */
  BlockPos getAttachmentPos();
}
