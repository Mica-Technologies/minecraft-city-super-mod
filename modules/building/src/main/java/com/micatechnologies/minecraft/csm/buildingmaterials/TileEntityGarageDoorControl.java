package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * What a garage door control remembers: the door (or opener) it is linked to, and for a keypad
 * its code and who owns it. Data only: never ticked, never drawn.
 *
 * <p>The code never leaves the server. The tag a client is sent carries only whether a code is set
 * and who the owner is, which is what the keypad screen needs to show; a client that could read the
 * code could open anyone's door. A wrong code costs a try, and five wrong tries lock the keypad for
 * that player for thirty seconds.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityGarageDoorControl extends AbstractTileEntity {

  /** Digits in a code. */
  public static final int MIN_DIGITS = 4;
  public static final int MAX_DIGITS = 6;

  private static final int MAX_FAILS = 5;
  private static final long LOCKOUT_TICKS = 600L;

  @Nullable
  private BlockPos target;
  private String code = "";
  /** Client side: whether a code is set, since the code itself is not sent. */
  private boolean hasCode;
  @Nullable
  private UUID owner;

  /** Wrong tries per player, and when a locked-out player may try again. Not saved. */
  private final transient Map<UUID, int[]> fails = new HashMap<>();
  private final transient Map<UUID, Long> lockedUntil = new HashMap<>();

  void setTarget(BlockPos target) {
    this.target = target.toImmutable();
    markDirtySync(world, pos, true);
  }

  void setOwner(UUID owner) {
    this.owner = owner;
    markDirtySync(world, pos, true);
  }

  /**
   * Whether this player may change the code or relink the control: its owner, or anyone while it
   * has none (one put up by a command).
   *
   * @param player the player
   *
   * @return whether they may
   *
   * @since 1.0
   */
  public boolean mayManage(UUID player) {
    return owner == null || owner.equals(player);
  }

  public boolean hasCode() {
    return world != null && world.isRemote ? hasCode : !code.isEmpty();
  }

  /**
   * Gives the linked door (or opener) a command.
   *
   * @param command what to do
   *
   * @return whether something linked was there to obey it
   *
   * @since 1.0
   */
  boolean operate(BlockGarageDoor.Command command) {
    if (target == null || !world.isBlockLoaded(target)) {
      return false;
    }
    IBlockState state = world.getBlockState(target);
    if (state.getBlock() instanceof BlockGarageDoor) {
      ((BlockGarageDoor) state.getBlock()).command(world, target, command);
      return true;
    }
    if (state.getBlock() instanceof BlockGarageDoorOpener) {
      return ((BlockGarageDoorOpener) state.getBlock()).operate(world, target, state, command);
    }
    return false;
  }

  /**
   * A code entered on the keypad, on the server.
   *
   * @param player who entered it
   * @param entered the digits
   *
   * @since 1.0
   */
  void enter(EntityPlayer player, String entered) {
    UUID id = player.getUniqueID();
    long now = world.getTotalWorldTime();
    Long until = lockedUntil.get(id);
    if (until != null && now < until) {
      player.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.keypad_locked"), true);
      return;
    }
    if (code.isEmpty()) {
      player.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.keypad_no_code"),
          true);
      return;
    }
    if (!code.equals(entered)) {
      int[] n = fails.computeIfAbsent(id, k -> new int[1]);
      n[0]++;
      if (n[0] >= MAX_FAILS) {
        n[0] = 0;
        lockedUntil.put(id, now + LOCKOUT_TICKS);
      }
      world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BASS, SoundCategory.BLOCKS, 0.6F, 0.5F);
      player.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.keypad_wrong"), true);
      return;
    }
    fails.remove(id);
    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 0.6F, 1.6F);
    if (!operate(BlockGarageDoor.Command.TOGGLE)) {
      player.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.not_linked"), true);
    }
  }

  /**
   * A new code set from the keypad, on the server. Only its owner may, and a keypad with no owner
   * becomes the setter's.
   *
   * @param player who set it
   * @param digits the new code
   *
   * @since 1.0
   */
  void setCode(EntityPlayer player, String digits) {
    if (!mayManage(player.getUniqueID())) {
      player.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.not_owner"), true);
      return;
    }
    if (digits.length() < MIN_DIGITS || digits.length() > MAX_DIGITS) {
      return;
    }
    if (owner == null) {
      owner = player.getUniqueID();
    }
    code = digits;
    markDirtySync(world, pos, true);
    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 0.6F, 2.0F);
    player.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.keypad_code_set"),
        true);
  }

  // NBT keys, short as every CSM tile entity's are.
  private static final String KEY_TARGET = "tg";
  private static final String KEY_CODE = "c";
  private static final String KEY_HAS_CODE = "hc";
  private static final String KEY_OWNER = "o";

  @Override
  public void readNBT(NBTTagCompound compound) {
    target = compound.hasKey(KEY_TARGET) ? BlockPos.fromLong(compound.getLong(KEY_TARGET)) : null;
    String c = compound.getString(KEY_CODE);
    code = c.matches("[0-9]{" + MIN_DIGITS + "," + MAX_DIGITS + "}") ? c : "";
    hasCode = compound.getBoolean(KEY_HAS_CODE) || !code.isEmpty();
    owner = compound.hasUniqueId(KEY_OWNER) ? compound.getUniqueId(KEY_OWNER) : null;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (target != null) {
      compound.setLong(KEY_TARGET, target.toLong());
    }
    if (!code.isEmpty()) {
      compound.setString(KEY_CODE, code);
    }
    compound.setBoolean(KEY_HAS_CODE, !code.isEmpty());
    if (owner != null) {
      compound.setUniqueId(KEY_OWNER, owner);
    }
    return compound;
  }

  /**
   * What clients are sent: everything but the code.
   *
   * @since 1.0
   */
  @Override
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = super.getUpdateTag();
    tag.removeTag(KEY_CODE);
    return tag;
  }
}
