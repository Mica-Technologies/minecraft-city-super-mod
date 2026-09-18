package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.buildingmaterials.CsmBuilding;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A tower crane's configuration screen: model and livery as buttons that cycle, and sliders for
 * jib length, slew, trolley, hook drop and luff.
 *
 * <p>Every change is previewed on the crane as it is made, on this client only. Done sends the
 * configuration to the server, which checks and clamps it and sends it to everyone; Cancel, or
 * Escape, puts the crane back as it was. Opened from any block of the crane, so {@code clicked} --
 * which the server checks reach against -- may be the foot of the mast.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class CraneHeadGui extends GuiScreen implements GuiSlider.ISlider {

  private static final int BTN_MODEL = 0;
  private static final int BTN_LIVERY = 1;
  private static final int SLD_JIB = 2;
  private static final int SLD_SLEW = 3;
  private static final int SLD_TROLLEY = 4;
  private static final int SLD_HOOK = 5;
  private static final int SLD_LUFF = 6;
  private static final int BTN_DONE = 7;
  private static final int BTN_CANCEL = 8;

  private static final int W = 200;
  private static final int H = 20;
  private static final int GAP = 24;

  private final TileEntityCraneHead te;
  private final BlockPos clicked;

  /** The configuration as it was when the screen opened, for Cancel. */
  private final CraneModel oldModel;
  private final CraneLivery oldLivery;
  private final int oldJib;
  private final float oldSlew;
  private final float oldTrolley;
  private final int oldHook;
  private final float oldLuff;

  private CraneModel model;
  private CraneLivery livery;
  private GuiButton modelButton;
  private GuiButton liveryButton;
  private GuiSlider jib;
  private GuiSlider slew;
  private GuiSlider trolley;
  private GuiSlider hook;
  private GuiSlider luff;
  private boolean sent;

  /**
   * Constructs a {@link CraneHeadGui}.
   *
   * @param te      the crane's head
   * @param clicked the block that was clicked to open it
   *
   * @since 1.0
   */
  public CraneHeadGui(TileEntityCraneHead te, BlockPos clicked) {
    this.te = te;
    this.clicked = clicked;
    this.oldModel = te.getModel();
    this.oldLivery = te.getLivery();
    this.oldJib = te.getJibLength();
    this.oldSlew = te.getSlew();
    this.oldTrolley = te.getTrolley();
    this.oldHook = te.getHookDrop();
    this.oldLuff = te.getLuff();
    this.model = oldModel;
    this.livery = oldLivery;
  }

  @Override
  public void initGui() {
    buttonList.clear();
    int x = (width - W) / 2;
    int y = height / 2 - GAP * 4 - 6;
    modelButton = new GuiButton(BTN_MODEL, x, y, W, H, modelLabel());
    buttonList.add(modelButton);
    y += GAP;
    liveryButton = new GuiButton(BTN_LIVERY, x, y, W, H, liveryLabel());
    buttonList.add(liveryButton);
    y += GAP;
    int s = te.getScale();
    jib = slider(SLD_JIB, x, y, "gui.csm.crane.jib", " m",
        TileEntityCraneHead.MIN_JIB * s, TileEntityCraneHead.MAX_JIB * s, te.getJibLength());
    y += GAP;
    slew = slider(SLD_SLEW, x, y, "gui.csm.crane.slew", "°", 0, 359,
        te.getSlew() < 0 ? te.getSlew() + 360 : te.getSlew());
    y += GAP;
    trolley = slider(SLD_TROLLEY, x, y, "gui.csm.crane.trolley", "%", 0, 100,
        te.getTrolley() * 100);
    y += GAP;
    hook = slider(SLD_HOOK, x, y, "gui.csm.crane.hook", " m", 1,
        TileEntityCraneHead.MAX_HOOK_DROP, te.getHookDrop());
    y += GAP;
    luff = slider(SLD_LUFF, x, y, "gui.csm.crane.luff", "°",
        TileEntityCraneHead.MIN_LUFF, TileEntityCraneHead.MAX_LUFF, te.getLuff());
    y += GAP + 6;
    buttonList.add(new GuiButton(BTN_DONE, x, y, W / 2 - 2, H, I18n.format("gui.done")));
    buttonList.add(new GuiButton(BTN_CANCEL, x + W / 2 + 2, y, W / 2 - 2, H,
        I18n.format("gui.cancel")));
    updateEnabled();
  }

  private GuiSlider slider(int id, int x, int y, String key, String suffix, double min,
      double max, double value) {
    GuiSlider slider = new GuiSlider(id, x, y, W, H, I18n.format(key) + ": ", suffix, min, max,
        value, false, true, this);
    buttonList.add(slider);
    return slider;
  }

  private String modelLabel() {
    return I18n.format("gui.csm.crane.model") + ": "
        + I18n.format("gui.csm.crane.model." + model.name().toLowerCase());
  }

  private String liveryLabel() {
    return I18n.format("gui.csm.crane.livery") + ": "
        + I18n.format("gui.csm.crane.livery." + livery.getName());
  }

  /** The luff is a luffing crane's; the trolley is everyone else's. */
  private void updateEnabled() {
    luff.enabled = model == CraneModel.LUFFING;
    trolley.enabled = model != CraneModel.LUFFING;
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case BTN_MODEL:
        model = CraneModel.values()[(model.ordinal() + 1) % CraneModel.values().length];
        modelButton.displayString = modelLabel();
        updateEnabled();
        preview();
        break;
      case BTN_LIVERY:
        livery = CraneLivery.values()[(livery.ordinal() + 1) % CraneLivery.values().length];
        liveryButton.displayString = liveryLabel();
        preview();
        break;
      case BTN_DONE:
        CsmBuilding.NETWORK.sendToServer(new CraneHeadConfigPacket(clicked, model, livery,
            jib.getValueInt(), (float) slew.getValue(), (float) (trolley.getValue() / 100.0),
            hook.getValueInt(), (float) luff.getValue()));
        sent = true;
        mc.displayGuiScreen(null);
        break;
      case BTN_CANCEL:
        mc.displayGuiScreen(null);
        break;
      default:
        break;
    }
  }

  @Override
  public void onChangeSliderValue(GuiSlider slider) {
    preview();
  }

  /** Shows the configuration on the crane, on this client only. */
  private void preview() {
    if (jib == null || slew == null || trolley == null || hook == null || luff == null) {
      return;
    }
    te.setConfiguration(model, livery, jib.getValueInt(), (float) slew.getValue(),
        (float) (trolley.getValue() / 100.0), hook.getValueInt(), (float) luff.getValue());
  }

  /**
   * Closing without Done -- Cancel or Escape -- puts the crane back as it was. After Done the
   * server's reply replaces the preview anyway.
   *
   * @since 1.0
   */
  @Override
  public void onGuiClosed() {
    if (!sent) {
      te.setConfiguration(oldModel, oldLivery, oldJib, oldSlew, oldTrolley, oldHook, oldLuff);
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    drawCenteredString(fontRenderer, I18n.format("gui.csm.crane.title"), width / 2,
        height / 2 - GAP * 4 - 22, 0xFFFFFF);
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
