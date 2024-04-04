package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityRedstoneTTSUpdatePacket;
import com.mojang.text2speech.Narrator;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityRedstoneTTS extends AbstractTileEntity {

  private static final String TTS_STRING_KEY = "ttsString";
  private String ttsString = "Setup is Required!";

  /**
   * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(TTS_STRING_KEY)) {
      ttsString = compound.getString(TTS_STRING_KEY);
    }
  }

  /**
   * Returns the NBT tag compound with the tile entity's NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(TTS_STRING_KEY, ttsString);
    return compound;
  }

  private AtomicBoolean isTtsPlaying = new AtomicBoolean(false);

  public void readTtsString() {
    Thread ttsThread = new Thread(() -> {
      try {
        if (isTtsPlaying.get()) {
          return;
        }
        isTtsPlaying.set(true);
        Narrator narrator = Narrator.getNarrator();
        narrator.say(ttsString);
        isTtsPlaying.set(false);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    ttsThread.start();
  }

  public String getTtsString() {
    return ttsString;
  }

  public void setTtsStringFromGui(String newTtsString) {
    if (world.isRemote) { // Check if we're on the client side
      // Send packet to server with the new TTS string and tile entity position
      CsmNetwork.sendToServer(new TileEntityRedstoneTTSUpdatePacket(this.pos, newTtsString));
    } else {
      // Update the TTS string on the server side
      setTtsString(newTtsString);
    }
  }

  public void setTtsString(String ttsString) {
    if (world.isRemote) {
      System.err.println("Attempted to set TTS string on client side! This is a bug!");
    }
    this.ttsString = ttsString;
    markDirtySync(getWorld(), getPos(), true);
  }
}

