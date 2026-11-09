package com.micatechnologies.minecraft.csm.api;

import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Lets a module's door be linked to Building's door controls -- the keypad, the push button and
 * the control station -- without naming Building, which owns the controls and the door locks: a
 * holding cell door in Life Safety, say.
 *
 * <p>Building registers its {@link Service} from its pre-initialization. A door that wants to be
 * controlled implements {@link Door}, hands a sneak-click to {@link #finishLink} (which takes the
 * click only while its player is linking a control), and asks {@link #isLocked} before it lets
 * someone open it by hand. Without Building installed nothing links and nothing is locked.</p>
 *
 * @since 2026.11
 */
public final class CsmDoorControl {

  /** What a control tells its door to do. */
  public enum Command {
    TOGGLE, OPEN, CLOSE, STOP
  }

  /**
   * A door, in any module, that a Building door control can link to. Every position handed to
   * these methods is the door's key: the one position a whole door is linked and locked by (for a
   * stack of blocks, its lowest).
   */
  public interface Door {

    /** The key of the door that has a block at {@code pos}. */
    BlockPos doorKey(World world, BlockPos pos);

    /** Obeys a control. Server side. */
    void doorCommand(World world, BlockPos key, Command command);

    /** Whether a keypad linked to this door locks it. */
    default boolean isLockable() {
      return true;
    }
  }

  /** What Building provides. */
  public interface Service {

    /**
     * Finishes the control link this player started, onto the door at {@code key}.
     *
     * @return whether a link was waiting, and so whether the click was taken
     */
    boolean finishLink(EntityPlayer player, World world, BlockPos key);

    /** Whether the door at {@code key} is locked by a keypad that is still there. Server side. */
    boolean isLocked(World world, BlockPos key);

    /** The door at {@code key} is gone: forget its lock. Server side. */
    void doorRemoved(World world, BlockPos key);
  }

  @Nullable
  private static volatile Service service;

  private CsmDoorControl() {
  }

  /** Registered by Building. */
  public static void setService(Service s) {
    service = s;
  }

  /** See {@link Service#finishLink}; false without Building. */
  public static boolean finishLink(EntityPlayer player, World world, BlockPos key) {
    Service s = service;
    return s != null && s.finishLink(player, world, key);
  }

  /** See {@link Service#isLocked}; false without Building. */
  public static boolean isLocked(World world, BlockPos key) {
    Service s = service;
    return s != null && s.isLocked(world, key);
  }

  /** See {@link Service#doorRemoved}. */
  public static void doorRemoved(World world, BlockPos key) {
    Service s = service;
    if (s != null) {
      s.doorRemoved(world, key);
    }
  }
}
