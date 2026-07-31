package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.ItemCraftingPart;
import javax.annotation.Nonnull;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for crafting parts — the intermediate tier of the mod's survival crafting chain.
 *
 * <p>Vanilla ores and ingots are crafted into the parts in this tab, and those parts are in turn
 * crafted into City Super Mod blocks. The mod remains creative-focused; this tab exists so that
 * its content is also reachable in survival.</p>
 *
 * <p>Unlike every other tab, this one holds only items, so it supplies its creative-tab icon by
 * overriding {@link #getTabIconStack()} rather than {@link #getTabIcon()}.</p>
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2026.7
 */
@CsmTab.Load(order = 13)
public class CsmTabMaterials extends CsmTab {

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabHidden() {
    return false;
  }

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabmaterials";
  }

  /**
   * Gets the {@link ItemStack} to display as the tab icon. This tab contains only items, so it
   * supplies its icon directly instead of via {@link #getTabIcon()}.
   *
   * @return the sheet metal part, the most widely used part in the mod
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public ItemStack getTabIconStack() {
    Item icon = CsmRegistry.getItem(CsmParts.SHEET_METAL);
    if (icon == null) {
      throw new IllegalStateException("Tab icon item not found for tab: " + getTabId());
    }
    return new ItemStack(icon, 1);
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabSearchable() {
    return false;
  }

  /**
   * Initializes all the items belonging to the tab.
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabItem(new ItemCraftingPart(CsmParts.SHEET_METAL,
        "Stock material for most CSM equipment bodies"));
    initTabItem(new ItemCraftingPart(CsmParts.FASTENER_KIT,
        "Bolts, nuts and washers for mounting equipment"));
    initTabItem(new ItemCraftingPart(CsmParts.POLE_SECTION,
        "Tube stock for poles, mounts and signposts"));
    initTabItem(new ItemCraftingPart(CsmParts.ENCLOSURE_SHELL,
        "Weatherproof cabinet for controllers and panels"));
    initTabItem(new ItemCraftingPart(CsmParts.WIRING_HARNESS,
        "Conductor loom for powered equipment"));
    initTabItem(new ItemCraftingPart(CsmParts.CONTROL_BOARD,
        "Logic board for controllers and smart equipment"));
    initTabItem(new ItemCraftingPart(CsmParts.LED_MODULE,
        "Diode cluster for signals and light fixtures"));
    initTabItem(new ItemCraftingPart(CsmParts.LENS_ASSEMBLY,
        "Optical lens for signal heads and beacons"));
    initTabItem(new ItemCraftingPart(CsmParts.SOUNDER_DRIVER,
        "Driver and cone for horns, speakers and tweeters"));
    initTabItem(new ItemCraftingPart(CsmParts.REFLECTIVE_SHEETING,
        "Retroreflective film for sign faces"));
    initTabItem(new ItemCraftingPart(CsmParts.SIGN_BLANK,
        "Undecorated sign panel; press it to make a road sign"));
    initTabItem(new ItemCraftingPart(CsmParts.CONCRETE_MIX,
        "Cement, sand and aggregate for concrete materials"));
    initTabItem(new ItemCraftingPart(CsmParts.DUCTING,
        "Sheet metal ductwork for HVAC equipment"));
    initTabItem(new ItemCraftingPart(CsmParts.OPTICAL_SENSOR,
        "Sensing element for traffic detection equipment"));
  }
}
