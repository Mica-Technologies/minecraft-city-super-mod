package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Arrow;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Legend;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

/**
 * Holds an exit sign's setup ({@link ExitSignConfig}). The block reads it in
 * {@code getActualState} to pick the multipart model's parts; nothing here ticks or renders.
 *
 * <p>The config is saved raw. The block clamps it to what it offers when it reads it, so this
 * class does not need to know which block it belongs to, which it cannot while a chunk loads.
 *
 * @since 2026.9
 */
public class TileEntityExitSign extends AbstractTileEntity {

  /** What an exit sign with no saved setup reads as, before its block clamps it. */
  public static final ExitSignConfig UNSET = new ExitSignConfig(Arrow.NONE, Letters.RED,
      Housing.WHITE, Mount.WALL, Heads.NONE, Legend.EXIT);

  private ExitSignConfig config = UNSET;

  public ExitSignConfig getConfig() {
    return config;
  }

  /**
   * Changes the setup and, on the server, saves it and sends it to every client watching the
   * chunk. The light is rechecked because the emergency heads change how bright the block is.
   */
  public void setConfig(ExitSignConfig newConfig) {
    if (newConfig.equals(config)) {
      return;
    }
    config = newConfig;
    if (world != null) {
      IBlockState state = world.getBlockState(pos);
      markDirtySync(world, pos, state, true);
      world.checkLight(pos);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    config = ExitSignConfig.read(compound, UNSET);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    return config.write(compound);
  }

  /** A client learns of a new setup here; its emergency heads may have changed its light. */
  @Override
  public void onDataPacket(NetworkManager networkManager, SPacketUpdateTileEntity pkt) {
    ExitSignConfig before = config;
    super.onDataPacket(networkManager, pkt);
    if (world != null && !before.equals(config)) {
      world.checkLight(pos);
    }
  }

  /** Only a setup change alters the baked model, so only that rebuilds the chunk section. */
  @Override
  protected long getBakedModelKey() {
    return config.pack();
  }
}
