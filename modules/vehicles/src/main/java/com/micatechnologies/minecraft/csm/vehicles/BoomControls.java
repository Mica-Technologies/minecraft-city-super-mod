package com.micatechnologies.minecraft.csm.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPanel;
import minecrafttransportsimulator.jsondefs.JSONConfigClient;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Held keys and a HUD for the boom of a CSM boom truck, for a player in one of its controller
 * seats (the driver's, or the bucket's).
 *
 * <p>Immersive Vehicles' panel acts only on a click, so its boom buttons step; these keys move
 * the boom for as long as they are held. Each tick a key is down sends the panel's own message,
 * an increment of the variable clamped to its range, to the server, which applies it and passes
 * it to every client: the panel does exactly that and applies nothing locally, so neither does
 * this. Keys are read straight from the keyboard, as Immersive Vehicles reads its own, and only
 * with no screen open, so typing a bracket in chat never moves a boom. B flips BOOM, which
 * {@link BoomAutoDriver} turns into a deploy or a stow on the server.</p>
 *
 * <p>The keys are listed under CSM: Vehicles in Controls and can be rebound there; the HUD names
 * whatever they are bound to.</p>
 *
 * <p>The bucket's place is a standing seat that is not a controller, so Immersive Vehicles never
 * drives the truck from it. There Immersive Vehicles' own drive keys (WASD, as the player has
 * them bound) work the boom as well: gas and brake lift, turn left and right swing. And as IV
 * opens its panel only for a controller seat, this opens and closes it on IV's panel key for the
 * bucket, with the same guards IV uses.</p>
 *
 * <p>Client only: {@link CsmVehicles} registers it only on the client side.</p>
 *
 * @since 2026.11
 */
public final class BoomControls {

  private static final String CATEGORY = "key.categories.csm_vehicles";

  /** Scaled pixels kept clear at the bottom: Immersive Vehicles' dashboard HUD and the hotbar. */
  private static final int HUD_CLEARANCE = 80;

  private final KeyBinding liftUp = key("boom_lift_up", Keyboard.KEY_UP);
  private final KeyBinding liftDown = key("boom_lift_down", Keyboard.KEY_DOWN);
  private final KeyBinding swingLeft = key("boom_swing_left", Keyboard.KEY_LEFT);
  private final KeyBinding swingRight = key("boom_swing_right", Keyboard.KEY_RIGHT);
  private final KeyBinding secondOut = key("boom_elbow_out", Keyboard.KEY_RBRACKET);
  private final KeyBinding secondIn = key("boom_elbow_in", Keyboard.KEY_LBRACKET);
  private final KeyBinding boomKey = key("boom_toggle", Keyboard.KEY_B);

  private boolean boomWasDown;
  private boolean panelWasDown;

  private BoomControls() {
  }

  /** Registers the keys and the tick and HUD handlers. */
  public static void register() {
    BoomControls controls = new BoomControls();
    for (KeyBinding binding : new KeyBinding[]{controls.liftUp, controls.liftDown,
        controls.swingLeft, controls.swingRight, controls.secondOut, controls.secondIn,
        controls.boomKey}) {
      ClientRegistry.registerKeyBinding(binding);
    }
    MinecraftForge.EVENT_BUS.register(controls);
  }

  private static KeyBinding key(String name, int code) {
    return new KeyBinding("key.csm_vehicles." + name, code, CATEGORY);
  }

  /** The seat the player is in, when it is a boom truck's controller seat or its bucket. */
  private static PartSeat operatingSeat() {
    if (InterfaceManager.clientInterface == null) {
      return null;
    }
    IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
    if (player == null) {
      return null;
    }
    AEntityB_Existing riding = player.getEntityRiding();
    if (!(riding instanceof PartSeat)) {
      return null;
    }
    PartSeat seat = (PartSeat) riding;
    if (!seat.placementDefinition.isController && !inBucket(seat)) {
      return null;
    }
    return BoomAxes.constants(seat.vehicleOn) != null ? seat : null;
  }

  /** Whether the seat rides the boom's bucket ({@code applyAfter: bucket} in the truck's JSON). */
  private static boolean inBucket(PartSeat seat) {
    return BoomAxes.BUCKET.equals(seat.placementDefinition.applyAfter);
  }

  private static boolean down(KeyBinding binding) {
    return binding != null && down(binding.getKeyCode());
  }

  private static boolean down(int code) {
    return code > 0 && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
  }

  /**
   * Whether one of Immersive Vehicles' keys is held, read from its binding rather than through
   * {@code isPressed()}, which keeps IV's own pressed state and would upset its momentary keys.
   * A key bound to a mouse button is not read.
   */
  private static boolean down(ControlsKeyboard control) {
    JSONConfigClient.ConfigKeyboard config = control.config;
    return config != null && !config.isMouseButton && down(config.keyCode);
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) {
      return;
    }
    Minecraft mc = Minecraft.getMinecraft();
    PartSeat seat = operatingSeat();
    boolean bucket = seat != null && inBucket(seat);
    // the panel key works with the panel open too, so it can close it
    boolean panelDown = bucket && down(ControlsKeyboard.CAR_PANEL);
    if (panelDown && !panelWasDown) {
      togglePanel(seat.vehicleOn);
    }
    panelWasDown = panelDown;

    EntityVehicleF_Physics vehicle = mc.currentScreen == null && seat != null ? seat.vehicleOn
        : null;
    if (vehicle == null) {
      boomWasDown = false;
      return;
    }
    Map<String, Double> constants = BoomAxes.constants(vehicle);
    hold(vehicle, BoomAxes.LIFT,
        down(liftUp) || bucket && down(ControlsKeyboard.CAR_GAS),
        down(liftDown) || bucket && down(ControlsKeyboard.CAR_BRAKE),
        BoomAxes.LIFT_RATE, 0, BoomAxes.liftMax(constants));
    hold(vehicle, BoomAxes.SWING,
        down(swingLeft) || bucket && down(ControlsKeyboard.CAR_TURN_L),
        down(swingRight) || bucket && down(ControlsKeyboard.CAR_TURN_R),
        BoomAxes.SWING_RATE, -BoomAxes.SWING_LIMIT, BoomAxes.SWING_LIMIT);
    hold(vehicle, BoomAxes.second(constants), down(secondOut), down(secondIn),
        BoomAxes.secondRate(constants), 0, BoomAxes.secondMax(constants));
    boolean boomDown = down(boomKey);
    if (boomDown && !boomWasDown) {
      InterfaceManager.packetInterface.sendToServer(
          new PacketEntityVariableToggle(vehicle.getOrCreateVariable(BoomAxes.BOOM)));
    }
    boomWasDown = boomDown;
  }

  /**
   * Opens IV's panel for the bucket, or closes it, as IV does for a controller seat
   * ({@code ControlSystem.controlPanel}): only for a player who could start the truck, closing it
   * unless its text is being edited, and opening it only with no other screen up.
   */
  private static void togglePanel(EntityVehicleF_Physics vehicle) {
    if (!vehicle.canPlayerStartEngines(InterfaceManager.clientInterface.getClientPlayer())) {
      return;
    }
    if (AGUIBase.activeInputGUI instanceof GUIPanel) {
      if (!AGUIBase.activeInputGUI.editingText) {
        AGUIBase.activeInputGUI.close();
      }
    } else if (!InterfaceManager.clientInterface.isGUIOpen()) {
      new GUIPanel(vehicle);
    }
  }

  /** Moves an axis while either way is held; both at once, or two keys one way, is one rate. */
  private static void hold(EntityVehicleF_Physics vehicle, String variable, boolean more,
      boolean less, double rate, double min, double max) {
    int direction = (more ? 1 : 0) - (less ? 1 : 0);
    if (direction == 0) {
      return;
    }
    ComputedVariable var = vehicle.getOrCreateVariable(variable);
    double now = var.currentValue;
    if (direction > 0 ? now >= max : now <= min) {
      return;                       // at the stop: nothing to send
    }
    InterfaceManager.packetInterface.sendToServer(
        new PacketEntityVariableIncrement(var, direction * rate, min, max));
  }

  @SubscribeEvent
  public void onOverlay(RenderGameOverlayEvent.Post event) {
    if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
      return;
    }
    Minecraft mc = Minecraft.getMinecraft();
    if (mc.gameSettings.hideGUI || mc.gameSettings.showDebugInfo) {
      return;
    }
    PartSeat seat = operatingSeat();
    if (seat == null) {
      return;
    }
    EntityVehicleF_Physics vehicle = seat.vehicleOn;
    boolean bucket = inBucket(seat);
    Map<String, Double> constants = BoomAxes.constants(vehicle);
    boolean elbow = BoomAxes.hasElbow(constants);
    double lift = vehicle.getOrCreateVariable(BoomAxes.LIFT).currentValue;
    double second = vehicle.getOrCreateVariable(BoomAxes.second(constants)).currentValue;
    double swing = vehicle.getOrCreateVariable(BoomAxes.SWING).currentValue;
    boolean stowed = Math.abs(lift) < 1e-3 && Math.abs(second) < 1e-3 && Math.abs(swing) < 1e-3;

    List<String[]> rows = new ArrayList<>();
    rows.add(new String[]{I18n.format("csm_vehicles.boom.hud.lift"),
        String.format(Locale.ROOT, "%.0f°", lift), keys(liftUp, liftDown)
        + (bucket ? ", " + keys(ControlsKeyboard.CAR_GAS, ControlsKeyboard.CAR_BRAKE) : "")});
    rows.add(elbow
        ? new String[]{I18n.format("csm_vehicles.boom.hud.elbow"),
        String.format(Locale.ROOT, "%.0f°", second), keys(secondOut, secondIn)}
        : new String[]{I18n.format("csm_vehicles.boom.hud.reach"),
            String.format(Locale.ROOT, "%.1f m", second), keys(secondOut, secondIn)});
    String side = Math.abs(swing) < 0.5 ? ""
        : I18n.format(swing > 0 ? "csm_vehicles.boom.hud.left" : "csm_vehicles.boom.hud.right");
    rows.add(new String[]{I18n.format("csm_vehicles.boom.hud.swing"),
        String.format(Locale.ROOT, "%.0f° %s", Math.abs(swing), side).trim(),
        keys(swingLeft, swingRight)
            + (bucket ? ", " + keys(ControlsKeyboard.CAR_TURN_L, ControlsKeyboard.CAR_TURN_R)
            : "")});
    String boomLine = I18n.format(stowed ? "csm_vehicles.boom.hud.deploy"
        : "csm_vehicles.boom.hud.stow", keyName(boomKey));

    FontRenderer font = mc.fontRenderer;
    int labelWidth = 0;
    int valueWidth = 0;
    int keyWidth = 0;
    for (String[] row : rows) {
      labelWidth = Math.max(labelWidth, font.getStringWidth(row[0]));
      valueWidth = Math.max(valueWidth, font.getStringWidth(row[1]));
      keyWidth = Math.max(keyWidth, font.getStringWidth(row[2]));
    }
    String title = I18n.format("csm_vehicles.boom.hud.title");
    int width = Math.max(labelWidth + valueWidth + keyWidth + 16,
        Math.max(font.getStringWidth(title), font.getStringWidth(boomLine))) + 8;
    int lines = rows.size() + 2;
    ScaledResolution res = event.getResolution();
    int x = 4;
    // above Immersive Vehicles' own dashboard, which fills the bottom of the screen in a cab
    int y = res.getScaledHeight() - HUD_CLEARANCE - lines * 10;
    net.minecraft.client.gui.Gui.drawRect(x - 2, y - 2, x + width, y + lines * 10, 0x80000000);
    font.drawStringWithShadow(title, x + 2, y, 0xFFD040);
    int row = 1;
    for (String[] cells : rows) {
      int ry = y + row * 10;
      font.drawStringWithShadow(cells[0], x + 2, ry, 0xFFFFFF);
      font.drawStringWithShadow(cells[1], x + 2 + labelWidth + 8 + valueWidth
          - font.getStringWidth(cells[1]), ry, 0xFFFFFF);
      font.drawStringWithShadow(cells[2], x + 2 + labelWidth + valueWidth + 16, ry, 0xFFD040);
      row++;
    }
    font.drawStringWithShadow(boomLine, x + 2, y + row * 10, 0xA0A0A0);
  }

  /** The two keys of an axis as the player has them bound, e.g. the up and down arrows. */
  private static String keys(KeyBinding a, KeyBinding b) {
    return keyName(a) + " / " + keyName(b);
  }

  /** Two of Immersive Vehicles' keys as the player has them bound in IV's config, e.g. W / S. */
  private static String keys(ControlsKeyboard a, ControlsKeyboard b) {
    return keyName(a) + " / " + keyName(b);
  }

  private static String keyName(ControlsKeyboard control) {
    JSONConfigClient.ConfigKeyboard config = control.config;
    if (config == null || config.isMouseButton) {
      return "?";
    }
    String name = Keyboard.getKeyName(config.keyCode);
    return name == null ? "?" : prettyKey(name);
  }

  /**
   * A key's name as it is printed on the key. LWJGL names the punctuation keys in words, so those
   * get their symbol; the rest are written as words, not shouted. Arrow glyphs come from the
   * small unicode font and are hard to read, so the arrow keys keep their names.
   */
  static String keyName(KeyBinding binding) {
    return prettyKey(binding.getDisplayName());
  }

  private static String prettyKey(String name) {
    switch (name) {
      case "LBRACKET":
        return "[";
      case "RBRACKET":
        return "]";
      case "SEMICOLON":
        return ";";
      case "APOSTROPHE":
        return "'";
      case "COMMA":
        return ",";
      case "PERIOD":
        return ".";
      case "SLASH":
        return "/";
      case "MINUS":
        return "-";
      case "EQUALS":
        return "=";
      default:
        return name.length() > 1 && name.equals(name.toUpperCase(Locale.ROOT))
            ? name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT) : name;
    }
  }
}
