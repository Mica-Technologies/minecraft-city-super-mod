package com.micatechnologies.minecraft.csm.vehicles;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * The unstick key: a rider of any Immersive Vehicles vehicle presses it to have
 * {@link VehicleRescue} lift the vehicle out of the blocks it is wedged in now, rather than after
 * the automatic wait. It flips the vehicle's {@link VehicleRescue#UNSTICK} variable with IV's own
 * toggle packet; the server does the rest and says what it did in chat.
 *
 * <p>Listed under CSM: Vehicles in Controls (Home by default, which neither IV nor vanilla
 * uses). Read straight from the keyboard with no screen open, as {@link BoomControls} reads its
 * keys. Client only.</p>
 *
 * @since 2026.11
 */
public final class VehicleRescueKey {

  private final KeyBinding key = new KeyBinding("key.csm_vehicles.unstick", Keyboard.KEY_HOME,
      "key.categories.csm_vehicles");

  private boolean wasDown;

  private VehicleRescueKey() {
  }

  /** Registers the key and its tick handler. */
  public static void register() {
    VehicleRescueKey rescueKey = new VehicleRescueKey();
    ClientRegistry.registerKeyBinding(rescueKey.key);
    MinecraftForge.EVENT_BUS.register(rescueKey);
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) {
      return;
    }
    int code = key.getKeyCode();
    boolean down = Minecraft.getMinecraft().currentScreen == null && code > 0
        && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
    if (down && !wasDown) {
      PartSeat seat = seat();
      if (seat != null) {
        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(
            seat.vehicleOn.getOrCreateVariable(VehicleRescue.UNSTICK)));
      }
    }
    wasDown = down;
  }

  /** The seat the player is in, any vehicle's, any seat. */
  private static PartSeat seat() {
    if (InterfaceManager.clientInterface == null) {
      return null;
    }
    IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
    if (player == null) {
      return null;
    }
    AEntityB_Existing riding = player.getEntityRiding();
    return riding instanceof PartSeat && ((PartSeat) riding).vehicleOn != null
        ? (PartSeat) riding : null;
  }
}
