package com.micatechnologies.minecraft.csm.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
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

  /** The boom truck the player is in a controller seat of, or null. */
  private static EntityVehicleF_Physics operated() {
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
    if (!seat.placementDefinition.isController) {
      return null;
    }
    EntityVehicleF_Physics vehicle = seat.vehicleOn;
    return BoomAxes.constants(vehicle) != null ? vehicle : null;
  }

  private static boolean down(KeyBinding binding) {
    int code = binding.getKeyCode();
    return code > 0 && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) {
      return;
    }
    Minecraft mc = Minecraft.getMinecraft();
    EntityVehicleF_Physics vehicle = mc.currentScreen == null ? operated() : null;
    if (vehicle == null) {
      boomWasDown = false;
      return;
    }
    Map<String, Double> constants = BoomAxes.constants(vehicle);
    hold(vehicle, BoomAxes.LIFT, liftUp, liftDown, BoomAxes.LIFT_RATE, 0,
        BoomAxes.liftMax(constants));
    hold(vehicle, BoomAxes.SWING, swingLeft, swingRight, BoomAxes.SWING_RATE,
        -BoomAxes.SWING_LIMIT, BoomAxes.SWING_LIMIT);
    hold(vehicle, BoomAxes.second(constants), secondOut, secondIn,
        BoomAxes.secondRate(constants), 0, BoomAxes.secondMax(constants));
    boolean boomDown = down(boomKey);
    if (boomDown && !boomWasDown) {
      InterfaceManager.packetInterface.sendToServer(
          new PacketEntityVariableToggle(vehicle.getOrCreateVariable(BoomAxes.BOOM)));
    }
    boomWasDown = boomDown;
  }

  private static void hold(EntityVehicleF_Physics vehicle, String variable, KeyBinding more,
      KeyBinding less, double rate, double min, double max) {
    int direction = (down(more) ? 1 : 0) - (down(less) ? 1 : 0);
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
    EntityVehicleF_Physics vehicle = operated();
    if (vehicle == null) {
      return;
    }
    Map<String, Double> constants = BoomAxes.constants(vehicle);
    boolean elbow = BoomAxes.hasElbow(constants);
    double lift = vehicle.getOrCreateVariable(BoomAxes.LIFT).currentValue;
    double second = vehicle.getOrCreateVariable(BoomAxes.second(constants)).currentValue;
    double swing = vehicle.getOrCreateVariable(BoomAxes.SWING).currentValue;
    boolean stowed = Math.abs(lift) < 1e-3 && Math.abs(second) < 1e-3 && Math.abs(swing) < 1e-3;

    List<String[]> rows = new ArrayList<>();
    rows.add(new String[]{I18n.format("csm_vehicles.boom.hud.lift"),
        String.format(Locale.ROOT, "%.0f°", lift), keys(liftUp, liftDown)});
    rows.add(elbow
        ? new String[]{I18n.format("csm_vehicles.boom.hud.elbow"),
        String.format(Locale.ROOT, "%.0f°", second), keys(secondOut, secondIn)}
        : new String[]{I18n.format("csm_vehicles.boom.hud.reach"),
            String.format(Locale.ROOT, "%.1f m", second), keys(secondOut, secondIn)});
    String side = Math.abs(swing) < 0.5 ? ""
        : I18n.format(swing > 0 ? "csm_vehicles.boom.hud.left" : "csm_vehicles.boom.hud.right");
    rows.add(new String[]{I18n.format("csm_vehicles.boom.hud.swing"),
        String.format(Locale.ROOT, "%.0f° %s", Math.abs(swing), side).trim(),
        keys(swingLeft, swingRight)});
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

  /**
   * A key's name as it is printed on the key. LWJGL names the punctuation keys in words, so those
   * get their symbol; the rest are written as words, not shouted. Arrow glyphs come from the
   * small unicode font and are hard to read, so the arrow keys keep their names.
   */
  static String keyName(KeyBinding binding) {
    String name = binding.getDisplayName();
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
