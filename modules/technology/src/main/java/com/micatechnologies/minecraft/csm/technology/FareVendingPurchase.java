package com.micatechnologies.minecraft.csm.technology;

/**
 * Catalogue of purchase options offered by the {@link BlockFareVendingMachine}. Each entry
 * pairs the visible label with the emerald cost and either the trip count baked into a new
 * card, the trip count added to an existing card, or the sentinel value 0 for the
 * single-use ticket.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public enum FareVendingPurchase {
  SINGLE_TICKET("Single-Use Ticket",          1,  0,  Kind.TICKET),
  NEW_CARD_1   ("New Card — 1 trip",          1,  1,  Kind.NEW_CARD),
  NEW_CARD_2   ("New Card — 2 trips",         2,  2,  Kind.NEW_CARD),
  NEW_CARD_5   ("New Card — 5 trips",         5,  5,  Kind.NEW_CARD),
  NEW_CARD_10  ("New Card — 10 trips",       10, 10,  Kind.NEW_CARD),
  NEW_CARD_25  ("New Card — 25 trips",       25, 25,  Kind.NEW_CARD),
  RELOAD_1     ("Reload Card +1 trip",        1,  1,  Kind.RELOAD),
  RELOAD_5     ("Reload Card +5 trips",       5,  5,  Kind.RELOAD),
  RELOAD_25    ("Reload Card +25 trips",     25, 25,  Kind.RELOAD);

  public enum Kind {
    /** Single-use paper fare ticket: produces one {@link ItemFareTicket}. */
    TICKET,
    /** New transit card pre-loaded with {@code trips} trips. */
    NEW_CARD,
    /** Adds {@code trips} to a {@link ItemTransitCard} already in the player's main hand. */
    RELOAD
  }

  public final String label;
  public final int costEmeralds;
  public final int trips;
  public final Kind kind;

  FareVendingPurchase(String label, int costEmeralds, int trips, Kind kind) {
    this.label = label;
    this.costEmeralds = costEmeralds;
    this.trips = trips;
    this.kind = kind;
  }

  /** Lookup by ordinal, with bounds-check returning null for invalid network input. */
  public static FareVendingPurchase fromOrdinal(int ordinal) {
    FareVendingPurchase[] values = values();
    if (ordinal < 0 || ordinal >= values.length) {
      return null;
    }
    return values[ordinal];
  }
}
