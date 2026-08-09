package com.micatechnologies.minecraft.csm.novelties;

/**
 * The control panel of an arcade cabinet, as the games see it. Games never touch LWJGL key codes;
 * {@code ArcadeGui} polls the keyboard once per frame and fills this in, so a game only deals in
 * six abstract controls plus (for the cabinets that had a trackball) a pointer.
 *
 * <p>Each control exposes two readings: <em>held</em>, true for as long as the control is down, and
 * <em>pressed</em>, true only on the frame the control went down. Because the simulation may take
 * several fixed steps per rendered frame, the GUI clears the pressed edges after the first step so a
 * single tap can never register twice.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeInput {

  /**
   * The abstract controls available on a cabinet.
   *
   * @since 1.0
   */
  public enum Button {
    /**
     * Joystick left.
     */
    LEFT,
    /**
     * Joystick right.
     */
    RIGHT,
    /**
     * Joystick up.
     */
    UP,
    /**
     * Joystick down.
     */
    DOWN,
    /**
     * The primary (fire) button.
     */
    FIRE,
    /**
     * The secondary button — thrust, hyperspace, superzapper, depending on the cabinet.
     */
    ALT
  }

  /**
   * Whether each control is currently held down.
   *
   * @since 1.0
   */
  private final boolean[] held = new boolean[Button.values().length];

  /**
   * Whether each control went down since the last poll.
   *
   * @since 1.0
   */
  private final boolean[] pressed = new boolean[Button.values().length];

  /**
   * The pointer's X position in playfield coordinates.
   *
   * @since 1.0
   */
  private float pointerX;

  /**
   * The pointer's Y position in playfield coordinates.
   *
   * @since 1.0
   */
  private float pointerY;

  /**
   * Whether the pointer was clicked since the last poll.
   *
   * @since 1.0
   */
  private boolean pointerClicked;

  /**
   * Records the current state of a control, deriving the pressed edge from the previous state.
   *
   * @param button  the control
   * @param isDown  whether the control is down this frame
   *
   * @since 1.0
   */
  public void setHeld(Button button, boolean isDown) {
    int index = button.ordinal();
    if (isDown && !held[index]) {
      pressed[index] = true;
    }
    held[index] = isDown;
  }

  /**
   * Retrieves whether the specified control is currently held down.
   *
   * @param button the control
   *
   * @return {@code true} if the control is down
   *
   * @since 1.0
   */
  public boolean held(Button button) {
    return held[button.ordinal()];
  }

  /**
   * Retrieves whether the specified control went down this step.
   *
   * @param button the control
   *
   * @return {@code true} if the control was newly pressed
   *
   * @since 1.0
   */
  public boolean pressed(Button button) {
    return pressed[button.ordinal()];
  }

  /**
   * Records the pointer position, in playfield coordinates.
   *
   * @param x the pointer X position
   * @param y the pointer Y position
   *
   * @since 1.0
   */
  public void setPointer(float x, float y) {
    this.pointerX = x;
    this.pointerY = y;
  }

  /**
   * Retrieves the pointer's X position in playfield coordinates.
   *
   * @return the pointer X position
   *
   * @since 1.0
   */
  public float getPointerX() {
    return pointerX;
  }

  /**
   * Retrieves the pointer's Y position in playfield coordinates.
   *
   * @return the pointer Y position
   *
   * @since 1.0
   */
  public float getPointerY() {
    return pointerY;
  }

  /**
   * Records that the pointer was clicked.
   *
   * @since 1.0
   */
  public void setPointerClicked() {
    this.pointerClicked = true;
  }

  /**
   * Retrieves whether the pointer was clicked this step.
   *
   * @return {@code true} if the pointer was clicked
   *
   * @since 1.0
   */
  public boolean pointerClicked() {
    return pointerClicked;
  }

  /**
   * Clears every pressed edge, leaving held states intact. Called by the GUI after the first
   * simulation step of a frame so a tap is seen exactly once.
   *
   * @since 1.0
   */
  public void clearEdges() {
    java.util.Arrays.fill(pressed, false);
    pointerClicked = false;
  }

  /**
   * Clears every recorded control state. Used when a game is (re)started so held keys from the menu
   * do not leak into the first frame of play.
   *
   * @since 1.0
   */
  public void clearAll() {
    java.util.Arrays.fill(pressed, false);
    java.util.Arrays.fill(held, false);
    pointerClicked = false;
  }
}
