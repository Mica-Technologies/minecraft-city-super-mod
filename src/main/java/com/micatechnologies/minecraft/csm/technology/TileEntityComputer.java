package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Backing tile entity for the powered-computer family. The on/off state lives on the block's
 * {@code POWERED} property; this TE only stores per-instance gag-GUI state — currently the
 * notepad text the player has typed. Kept light because it sits behind every iMac, iMac Pro,
 * and MacBook Pro placement.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class TileEntityComputer extends AbstractTileEntity {

  private static final String NBT_NOTEPAD = "np";
  private static final int MAX_NOTEPAD_CHARS = 2000;

  private String notepadText = "";

  public String getNotepadText() {
    return notepadText;
  }

  /**
   * Server-side setter that stores notepad text and pushes the new state to clients. Long
   * inputs are trimmed to {@link #MAX_NOTEPAD_CHARS} so a runaway paste can't bloat NBT.
   */
  public void setNotepadText(String newText) {
    if (newText == null) {
      newText = "";
    }
    if (newText.length() > MAX_NOTEPAD_CHARS) {
      newText = newText.substring(0, MAX_NOTEPAD_CHARS);
    }
    if (newText.equals(this.notepadText)) {
      return;
    }
    this.notepadText = newText;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(NBT_NOTEPAD)) {
      notepadText = compound.getString(NBT_NOTEPAD);
    } else {
      notepadText = "";
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (notepadText != null && !notepadText.isEmpty()) {
      compound.setString(NBT_NOTEPAD, notepadText);
    }
    return compound;
  }
}
