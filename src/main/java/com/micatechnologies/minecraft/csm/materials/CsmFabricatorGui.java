package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Mouse;

/**
 * The Fabricator picker: a searchable, scrollable list of every fabricable block in the mod.
 *
 * <p>The list is built once on open by walking {@link CsmRegistry} and keeping every block that
 * {@link CsmFabricatorCosts} prices, so it always reflects the real registry rather than a
 * hand-maintained list. Entries are grouped by creative tab, which the player can cycle through,
 * and filtered by a free-text search over the block's display name.</p>
 *
 * <p>Affordability is recomputed every frame from the player's inventory, so picking parts up or
 * spending them is reflected immediately without reopening the screen.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public class CsmFabricatorGui extends GuiScreen {

  private static final int BUTTON_ID_CLOSE = 0;
  private static final int BUTTON_ID_CATEGORY = 1;
  private static final int BUTTON_ID_QUANTITY = 2;
  private static final int BUTTON_ID_ROW_BASE = 100;

  private static final int PANEL_W = 320;
  private static final int PANEL_H = 222;
  private static final int HEADER_H = 46;
  private static final int ROW_H = 18;
  private static final int VISIBLE_ROWS = 7;

  private static final int COLOR_BG = 0xFF20262E;
  private static final int COLOR_HEADER_BG = 0xFF161B21;
  private static final int COLOR_ROW_BG = 0xFF2A323C;
  private static final int COLOR_TEXT = 0xFFFFFFFF;
  private static final int COLOR_DIM = 0xFFA8B2BF;
  private static final int COLOR_OK = 0xFF7FD98A;
  private static final int COLOR_NO = 0xFFE0736B;

  /** Batch sizes the quantity button cycles through. */
  private static final int[] QUANTITIES = {1, 4, 8, 16, 32, 64};

  /** "All categories" sentinel for the category filter. */
  private static final String CATEGORY_ALL = "All";

  private final BlockPos fabricatorPos;

  private final List<Entry> allEntries = new ArrayList<>();
  private final List<String> categories = new ArrayList<>();
  private List<Entry> filtered = new ArrayList<>();

  private GuiTextField searchField;
  private GuiButton categoryButton;
  private GuiButton quantityButton;
  private final List<GuiButton> rowButtons = new ArrayList<>();

  private int categoryIndex;
  private int quantityIndex;
  private int scroll;

  private int panelX;
  private int panelY;

  public CsmFabricatorGui(BlockPos fabricatorPos) {
    this.fabricatorPos = fabricatorPos;
  }

  /** One fabricable block: its registry name, display name, category and part cost. */
  private static final class Entry {
    private final String registryName;
    private final String displayName;
    private final String category;
    private final Map<String, Integer> cost;
    private final ItemStack icon;

    private Entry(String registryName, String displayName, String category,
        Map<String, Integer> cost, ItemStack icon) {
      this.registryName = registryName;
      this.displayName = displayName;
      this.category = category;
      this.cost = cost;
      this.icon = icon;
    }
  }

  @Override
  public void initGui() {
    super.initGui();
    ScaledResolution sr = new ScaledResolution(this.mc);
    panelX = (sr.getScaledWidth() - PANEL_W) / 2;
    panelY = (sr.getScaledHeight() - PANEL_H) / 2;

    if (allEntries.isEmpty()) {
      buildEntries();
    }

    this.buttonList.clear();
    rowButtons.clear();

    Keyboard0.enableRepeat();

    searchField = new GuiTextField(0, this.fontRenderer, panelX + 8, panelY + 20, 168, 14);
    searchField.setMaxStringLength(48);
    searchField.setFocused(true);

    categoryButton = new GuiButton(BUTTON_ID_CATEGORY, panelX + 182, panelY + 19, 88, 16,
        categoryLabel());
    this.buttonList.add(categoryButton);

    quantityButton = new GuiButton(BUTTON_ID_QUANTITY, panelX + 274, panelY + 19, 38, 16,
        "x" + QUANTITIES[quantityIndex]);
    this.buttonList.add(quantityButton);

    for (int i = 0; i < VISIBLE_ROWS; i++) {
      GuiButton row = new GuiButton(BUTTON_ID_ROW_BASE + i, panelX + 8,
          panelY + HEADER_H + i * ROW_H, PANEL_W - 16, ROW_H - 2, "");
      row.visible = false;
      this.buttonList.add(row);
      rowButtons.add(row);
    }

    this.buttonList.add(new GuiButton(BUTTON_ID_CLOSE, panelX + (PANEL_W - 70) / 2,
        panelY + PANEL_H - 22, 70, 18, "Close"));

    applyFilter();
  }

  /** Walks the registry once, keeping every block the cost model prices. */
  private void buildEntries() {
    Map<String, Boolean> seenCategories = new LinkedHashMap<>();
    for (Block block : CsmRegistry.getBlocks()) {
      Map<String, Integer> cost = CsmFabricatorCosts.getCost(block);
      if (cost == null || cost.isEmpty()) {
        continue;
      }
      Item item = Item.getItemFromBlock(block);
      if (item == net.minecraft.init.Items.AIR) {
        continue;
      }
      ItemStack stack = new ItemStack(item, 1);
      String display = stack.getDisplayName();
      String category = prettyCategory(CsmTab.getTabId(block.getCreativeTab()));
      String registryName = block.getRegistryName() == null
          ? null
          : block.getRegistryName().getPath();
      if (registryName == null) {
        continue;
      }
      allEntries.add(new Entry(registryName, display, category, cost, stack));
      seenCategories.put(category, Boolean.TRUE);
    }

    allEntries.sort((a, b) -> {
      int byCategory = a.category.compareToIgnoreCase(b.category);
      return byCategory != 0 ? byCategory : a.displayName.compareToIgnoreCase(b.displayName);
    });

    categories.add(CATEGORY_ALL);
    categories.addAll(seenCategories.keySet());
  }

  /** Turns a tab id such as "tabtrafficsignals" into "Traffic Signals" for display. */
  private static String prettyCategory(String tabId) {
    if (tabId == null) {
      return CATEGORY_ALL;
    }
    String base = tabId.startsWith("tab") ? tabId.substring(3) : tabId;
    switch (base) {
      case "buildingmaterials": return "Building Materials";
      case "furniture": return "Furniture";
      case "gaming": return "Gaming";
      case "hvac": return "HVAC";
      case "lifesafety": return "Life Safety";
      case "lighting": return "Lighting";
      case "novelties": return "Novelties";
      case "powergrid": return "Power Grid";
      case "roadsigns": return "Road Signs";
      case "technology": return "Technology";
      case "trafficaccessories": return "Traffic Accessories";
      case "trafficsignals": return "Traffic Signals";
      default: return base;
    }
  }

  private String categoryLabel() {
    return categories.isEmpty() ? CATEGORY_ALL : categories.get(categoryIndex);
  }

  /** Recomputes the visible list from the search text and selected category. */
  private void applyFilter() {
    String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
    String category = categoryLabel();
    List<Entry> result = new ArrayList<>();
    for (Entry entry : allEntries) {
      if (!CATEGORY_ALL.equals(category) && !entry.category.equals(category)) {
        continue;
      }
      if (!query.isEmpty() && !entry.displayName.toLowerCase(Locale.ROOT).contains(query)) {
        continue;
      }
      result.add(entry);
    }
    filtered = result;
    scroll = Math.max(0, Math.min(scroll, maxScroll()));
  }

  private int maxScroll() {
    return Math.max(0, filtered.size() - VISIBLE_ROWS);
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    drawRect(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, COLOR_BG);
    drawRect(panelX, panelY, panelX + PANEL_W, panelY + HEADER_H - 8, COLOR_HEADER_BG);

    fontRenderer.drawString("CSM Fabricator", panelX + 8, panelY + 6, COLOR_TEXT);
    String count = filtered.size() + " of " + allEntries.size();
    fontRenderer.drawString(count, panelX + PANEL_W - fontRenderer.getStringWidth(count) - 8,
        panelY + 6, COLOR_DIM);

    searchField.drawTextBox();
    if (searchField.getText().isEmpty() && !searchField.isFocused()) {
      fontRenderer.drawString("Search...", panelX + 12, panelY + 23, COLOR_DIM);
    }

    EntityPlayer player = this.mc.player;
    int quantity = QUANTITIES[quantityIndex];

    for (int i = 0; i < rowButtons.size(); i++) {
      GuiButton row = rowButtons.get(i);
      int index = scroll + i;
      if (index >= filtered.size()) {
        row.visible = false;
        continue;
      }
      row.visible = true;
      row.displayString = "";
      Entry entry = filtered.get(index);

      int rowY = panelY + HEADER_H + i * ROW_H;
      drawRect(panelX + 8, rowY, panelX + PANEL_W - 8, rowY + ROW_H - 2, COLOR_ROW_BG);

      RenderHelper.enableGUIStandardItemLighting();
      this.itemRender.renderItemAndEffectIntoGUI(entry.icon, panelX + 11, rowY);
      RenderHelper.disableStandardItemLighting();

      boolean affordable = canAfford(player, entry, quantity);
      String name = trim(entry.displayName, 150);
      fontRenderer.drawString(name, panelX + 31, rowY + 5, affordable ? COLOR_TEXT : COLOR_DIM);

      String costText = describeCost(entry.cost, quantity);
      fontRenderer.drawString(costText,
          panelX + PANEL_W - 12 - fontRenderer.getStringWidth(costText), rowY + 5,
          affordable ? COLOR_OK : COLOR_NO);
    }

    super.drawScreen(mouseX, mouseY, partialTicks);

    if (maxScroll() > 0) {
      String hint = "Scroll for more";
      fontRenderer.drawString(hint, panelX + 8, panelY + PANEL_H - 34, COLOR_DIM);
    }
  }

  private String trim(String text, int maxWidth) {
    if (fontRenderer.getStringWidth(text) <= maxWidth) {
      return text;
    }
    return fontRenderer.trimStringToWidth(text, maxWidth - 8) + "...";
  }

  /** Formats a cost map scaled by the batch size, e.g. "2 Sheet Metal + 2 Fastener Kit". */
  private static String describeCost(Map<String, Integer> cost, int quantity) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, Integer> entry : cost.entrySet()) {
      if (builder.length() > 0) {
        builder.append(" + ");
      }
      Item part = CsmRegistry.getItem(entry.getKey());
      String name = part == null ? entry.getKey() : new ItemStack(part).getDisplayName();
      builder.append(entry.getValue() * quantity).append(' ').append(name);
    }
    return builder.toString();
  }

  private static boolean canAfford(EntityPlayer player, Entry entry, int quantity) {
    if (player == null) {
      return false;
    }
    InventoryPlayer inv = player.inventory;
    for (Map.Entry<String, Integer> required : entry.cost.entrySet()) {
      Item part = CsmRegistry.getItem(required.getKey());
      if (part == null) {
        return false;
      }
      int held = 0;
      for (int i = 0; i < inv.mainInventory.size(); i++) {
        ItemStack stack = inv.mainInventory.get(i);
        if (stack.getItem() == part) {
          held += stack.getCount();
        }
      }
      if (held < required.getValue() * quantity) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0) {
      scroll = Math.max(0, Math.min(maxScroll(), scroll + (wheel > 0 ? -1 : 1)));
    }
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    searchField.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (searchField.textboxKeyTyped(typedChar, keyCode)) {
      applyFilter();
      scroll = 0;
      return;
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == BUTTON_ID_CLOSE) {
      this.mc.displayGuiScreen(null);
      return;
    }
    if (button.id == BUTTON_ID_CATEGORY) {
      categoryIndex = (categoryIndex + 1) % Math.max(1, categories.size());
      categoryButton.displayString = categoryLabel();
      scroll = 0;
      applyFilter();
      return;
    }
    if (button.id == BUTTON_ID_QUANTITY) {
      quantityIndex = (quantityIndex + 1) % QUANTITIES.length;
      quantityButton.displayString = "x" + QUANTITIES[quantityIndex];
      return;
    }
    int rowIndex = button.id - BUTTON_ID_ROW_BASE;
    if (rowIndex < 0 || rowIndex >= VISIBLE_ROWS) {
      return;
    }
    int index = scroll + rowIndex;
    if (index >= filtered.size()) {
      return;
    }
    Entry entry = filtered.get(index);
    // The server re-checks affordability; this only avoids sending a request that must fail.
    if (!canAfford(this.mc.player, entry, QUANTITIES[quantityIndex])) {
      return;
    }
    CsmNetwork.sendToServer(
        new CsmFabricatePacket(fabricatorPos, entry.registryName, QUANTITIES[quantityIndex]));
  }

  @Override
  public void onGuiClosed() {
    super.onGuiClosed();
    Keyboard0.disableRepeat();
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    searchField.updateCursorCounter();
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  /**
   * Thin wrapper around LWJGL keyboard repeat, isolated so the import does not leak into the
   * rest of the class.
   */
  private static final class Keyboard0 {
    private Keyboard0() {}

    static void enableRepeat() {
      org.lwjgl.input.Keyboard.enableRepeatEvents(true);
    }

    static void disableRepeat() {
      org.lwjgl.input.Keyboard.enableRepeatEvents(false);
    }
  }
}
