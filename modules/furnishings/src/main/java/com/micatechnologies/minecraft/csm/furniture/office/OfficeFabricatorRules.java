package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Commercial &amp; Office tab: desks and tables are timber
 * on a steel frame, storage timber and fittings, a filing cabinet and a locker sheet steel;
 * cubicle panels and seating are wool on steel (a name plate or sign adds its paper insert);
 * the boards are their surface in a frame; the things on a desk and the copier are electronics priced as the Technology tab's are, the ring
 * light and the desk lamp by their lamps, the camera and the projectors by their lenses. The
 * school's plastic seating is steel, the lectern and podium timber, the trophy case timber and
 * glass with a little gold, the map and the globe paper.
 *
 * <p>Priced by registry name, whose piece is its first words and whose finish is its last
 * ({@code office_desk_pedestal_walnut}). {@code audit_fabricator_costs.py} mirrors this.</p>
 *
 * @since 2026.9
 */
public final class OfficeFabricatorRules {

  /** The Commercial &amp; Office tab. */
  public static final String TAB_ID = "tabcommercialoffice";

  private static final String MC_PLANKS = "minecraft:planks";
  private static final String MC_WOOL = "minecraft:wool";
  private static final String MC_IRON_INGOT = "minecraft:iron_ingot";
  private static final String MC_PAPER = "minecraft:paper";
  private static final String MC_GLASS = "minecraft:glass";
  private static final String MC_COAL = "minecraft:coal";
  private static final String MC_GLASS_PANE = "minecraft:glass_pane";
  private static final String MC_GOLD_NUGGET = "minecraft:gold_nugget";

  private OfficeFabricatorRules() {
  }

  /**
   * Prices a block in the Commercial &amp; Office tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    List<FabricatorIngredient> equipment = equipment(registryName);
    if (equipment != null) {
      return equipment;
    }
    List<FabricatorIngredient> seating = seating(registryName);
    if (seating != null) {
      return seating;
    }
    if (registryName.startsWith("office_desk_pedestal_")
        || registryName.startsWith("teacher_desk_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("office_desk_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("reception_desk_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 5),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("filing_cabinet_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("office_shelving_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_PAPER, 2));
    }
    if (registryName.startsWith("conference_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("cubicle_panel_nameplate_")
        || registryName.startsWith("cubicle_panel_sign_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 2),
          FabricatorIngredient.any(MC_IRON_INGOT, 1), FabricatorIngredient.any(MC_PAPER, 1));
    }
    if (registryName.startsWith("cubicle_panel_half_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("cubicle_panel_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 2),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("whiteboard_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("chalkboard_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_COAL, 1));
    }
    if (registryName.startsWith("cork_board_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    if (registryName.startsWith("projector_screen_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("pull_down_map_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PAPER, 3),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("lectern_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("podium_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("trophy_case_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_GLASS_PANE, 3),
          FabricatorIngredient.any(MC_GOLD_NUGGET, 3));
    }
    if (registryName.startsWith("desk_globe_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PAPER, 2),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("pencil_sharpener_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("classroom_flag_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 2),
          FabricatorIngredient.any(MC_PLANKS, 1));
    }
    if (registryName.startsWith("av_cart_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("cafeteria_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.any(MC_IRON_INGOT, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("locker_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("green_screen_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 2),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }

  /**
   * The chairs and the bench: upholstery on steel, as the bedroom's desk chair is; the school
   * desk is a plank top and a plastic seat on steel tube.
   *
   * @param registryName the registry name
   *
   * @return the cost, or null if it is not seating
   */
  @Nullable
  private static List<FabricatorIngredient> seating(String registryName) {
    if (registryName.startsWith("task_chair_") || registryName.startsWith("conference_chair_")
        || registryName.startsWith("gaming_chair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.any(MC_WOOL, 2));
    }
    if (registryName.startsWith("guest_chair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("waiting_bench_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("tablet_arm_desk_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("classroom_chair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("school_desk_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    return null;
  }

  /**
   * The electronics, the lamps and the small things on a desk.
   *
   * @param registryName the registry name
   *
   * @return the cost, or null if it is none of these
   */
  @Nullable
  private static List<FabricatorIngredient> equipment(String registryName) {
    if (registryName.startsWith("copier_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("desktop_computer_") || registryName.startsWith("laptop_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("retro_computer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.any(MC_GLASS, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("computer_tower_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("desk_phone_") || registryName.startsWith("fax_machine_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("desk_lamp_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("ring_light_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 2),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("studio_camera_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("ceiling_projector_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("overhead_projector_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
          FabricatorIngredient.any(MC_GLASS, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("pen_holder_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("paper_tray_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    return null;
  }
}
