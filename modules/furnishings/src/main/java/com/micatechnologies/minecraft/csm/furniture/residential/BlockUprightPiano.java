package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * An upright piano, two blocks wide, that plays: a right-click on its keyboard plays the key
 * under the cursor, with a note-block note (the harp, vanilla's piano voice) and a note
 * particle over the key. The keyboard is two octaves, fifteen white keys and ten black, low
 * notes to the player's left as on a real one; a click on the back half of the keys near a
 * black key plays the black key. A click anywhere else on the piano does nothing.
 *
 * <p>The keyboard's place in the model, facing north across both blocks, is the generator's
 * ({@code gen_furniture_living.py}, {@code PIANO_KEYS}); the constants here must match it.</p>
 *
 * @since 2026.9
 */
public class BlockUprightPiano extends BlockResidentialWide {

  /** The keyboard's ends, front, back and white key top, in sixteenths across both blocks. */
  private static final double KEYS_X0 = 5;
  private static final double KEYS_X1 = 27;
  private static final double KEYS_FRONT = 3.5;
  private static final double KEYS_BACK = 7.5;
  private static final double KEYS_TOP = 11.75;
  /** Fifteen white keys: two octaves, C to C. */
  private static final int WHITE_KEYS = 15;
  /** Each white key's semitone within its octave, C D E F G A B. */
  private static final int[] WHITE_SEMITONE = {0, 2, 4, 5, 7, 9, 11};
  /** Whether a black key follows each white key in an octave: after C, D, F, G and A. */
  private static final boolean[] BLACK_AFTER = {true, true, false, true, true, true, false};

  /**
   * Constructs a piano.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north across both blocks, in sixteenths
   */
  public BlockUprightPiano(String registryName, int[] box) {
    super(registryName, box);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    int note = noteAt(state, hitX * 16.0, hitY * 16.0, hitZ * 16.0);
    if (note < 0) {
      return false;
    }
    if (!world.isRemote) {
      float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
      world.playSound(null, pos, SoundEvents.BLOCK_NOTE_HARP, SoundCategory.RECORDS, 1.0F, pitch);
      if (world instanceof WorldServer) {
        // A count of zero spawns one particle whose x speed is its colour, as a note block's.
        ((WorldServer) world).spawnParticle(EnumParticleTypes.NOTE, pos.getX() + hitX,
            pos.getY() + hitY + 0.35, pos.getZ() + hitZ, 0, note / 24.0, 0.0, 0.0, 1.0);
      }
    }
    return true;
  }

  /**
   * The note under a click, in semitones above the keyboard's lowest C (0 to 24), or -1 if the
   * click is not on the keys.
   *
   * @param state the block's state
   * @param wx    the click across the block, west to east, in sixteenths
   * @param wy    the click's height in the block, in sixteenths
   * @param wz    the click across the block, north to south, in sixteenths
   *
   * @return the note, or -1
   */
  static int noteAt(IBlockState state, double wx, double wy, double wz) {
    // Into the model's frame, facing north, undoing the blockstate's turn.
    double mx;
    double mz;
    switch (state.getValue(FACING)) {
      case EAST:
        mx = wz;
        mz = 16 - wx;
        break;
      case SOUTH:
        mx = 16 - wx;
        mz = 16 - wz;
        break;
      case WEST:
        mx = 16 - wz;
        mz = wx;
        break;
      default:
        mx = wx;
        mz = wz;
        break;
    }
    mx += 16 * state.getValue(WidePieces.PART);
    if (mx < KEYS_X0 || mx > KEYS_X1 || mz < KEYS_FRONT - 0.3 || mz > KEYS_BACK + 0.3
        || wy < KEYS_TOP - 3 || wy > KEYS_TOP + 1.5) {
      return -1;
    }
    // Facing the piano, its +x end is on the player's left: the bass.
    double t = (KEYS_X1 - mx) / (KEYS_X1 - KEYS_X0) * WHITE_KEYS;
    int white = Math.max(0, Math.min(WHITE_KEYS - 1, (int) t));
    double within = t - white;
    int inOctave = white % 7;
    int note = 12 * (white / 7) + WHITE_SEMITONE[inOctave];
    boolean backHalf = mz > (KEYS_FRONT + KEYS_BACK) / 2 && wy >= KEYS_TOP - 0.25;
    if (backHalf) {
      if (within > 0.62 && BLACK_AFTER[inOctave] && note < 24) {
        return note + 1;
      }
      if (within < 0.38 && inOctave > 0 && BLACK_AFTER[inOctave - 1]) {
        return note - 1;
      }
    }
    return note;
  }
}
