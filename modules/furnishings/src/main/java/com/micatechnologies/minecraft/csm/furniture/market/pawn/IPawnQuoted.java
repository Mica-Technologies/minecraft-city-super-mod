package com.micatechnologies.minecraft.csm.furniture.market.pawn;

/**
 * A pawn counter's container that the server sends quotes to ({@link PawnQuotePacket}).
 *
 * @since 2026.10
 */
public interface IPawnQuoted {

  /** Client side: the latest quote. */
  void setQuote(PawnQuotePacket quote);

  /** Client side: the latest quote, or null before the first arrives. */
  PawnQuotePacket getQuote();
}
