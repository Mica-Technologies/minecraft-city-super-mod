package com.micatechnologies.minecraft.csm.technology.radio;

/**
 * One preset a handheld radio can be turned to: what its screen calls it, and the frequency it
 * stands for.
 *
 * <p>The frequency is what makes two presets the same channel. Radios of different makes name
 * and number their presets differently (the consumer radio's channel 1 is one of the dual-band
 * radio's memories), so anything that ever carries a transmission from one radio to another has
 * to match on {@link #getFrequencyHz()}, never on a preset's index or name.</p>
 *
 * @since 2026.11
 */
public final class RadioChannel {

  private final String name;
  private final int frequencyHz;
  private final boolean receiveOnly;

  /**
   * @param name        what the screen shows for the preset
   * @param frequencyHz its frequency, in hertz
   * @param receiveOnly whether the preset only listens (a weather broadcast)
   */
  RadioChannel(String name, int frequencyHz, boolean receiveOnly) {
    this.name = name;
    this.frequencyHz = frequencyHz;
    this.receiveOnly = receiveOnly;
  }

  /** What the screen shows for the preset. */
  public String getName() {
    return name;
  }

  /** The preset's frequency, in hertz: the identity of the channel across makes of radio. */
  public int getFrequencyHz() {
    return frequencyHz;
  }

  /** Whether the preset only listens. */
  public boolean isReceiveOnly() {
    return receiveOnly;
  }

  /** The frequency as a radio's screen writes it: megahertz to four places, {@code 462.5625}. */
  public String getFrequencyText() {
    int tenThousandths = frequencyHz / 100;
    int whole = tenThousandths / 10000;
    int fraction = tenThousandths % 10000;
    StringBuilder text = new StringBuilder().append(whole).append('.');
    String digits = Integer.toString(fraction);
    for (int i = digits.length(); i < 4; i++) {
      text.append('0');
    }
    return text.append(digits).toString();
  }
}
