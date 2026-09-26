package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.BlockAppleCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockBananaCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockBeetCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockCarrotBarrel;
import com.micatechnologies.minecraft.csm.furniture.BlockCarrotCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockCornCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockGoldenApples;
import com.micatechnologies.minecraft.csm.furniture.BlockGreenAppleCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockLargeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockLettuceCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockOnionCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockOrangeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockPearCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockPotatoeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockTomatoeCrate;
import com.micatechnologies.minecraft.csm.furniture.market.BlockAisleSign;
import com.micatechnologies.minecraft.csm.furniture.market.BlockCashRegister;
import com.micatechnologies.minecraft.csm.furniture.market.BlockDisplayCase;
import com.micatechnologies.minecraft.csm.furniture.market.BlockDepartmentSign;
import com.micatechnologies.minecraft.csm.furniture.market.BlockDisplayCooler;
import com.micatechnologies.minecraft.csm.furniture.market.BlockFittingRoom;
import com.micatechnologies.minecraft.csm.furniture.market.BlockGondola;
import com.micatechnologies.minecraft.csm.furniture.market.BlockMarketRun;
import com.micatechnologies.minecraft.csm.furniture.market.BlockMarketTall;
import com.micatechnologies.minecraft.csm.furniture.market.BlockVerifoneMx915;
import com.micatechnologies.minecraft.csm.furniture.market.StoreAppliances;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBathroomFixture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterAppliance;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterPiece;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenCabinet;
import com.micatechnologies.minecraft.csm.furniture.residential.FixtureMaterial;
import com.micatechnologies.minecraft.csm.furniture.residential.ItemResidentialFood;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenFront;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenLine;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for grocery stores and shops: refrigerated displays that look stocked (glass-door
 * reach-in coolers and freezers, the open dairy case, the island freezer, the ice cream, deli
 * and bakery cases), gondola shelving stocked six ways, the produce crates, stands, scale and
 * bulk bins, the fresh departments (butcher and seafood cases that join the deli case into one
 * counter, bread racks, a self-serve pastry case, the hot food case and a rotisserie oven, a
 * flower bucket stand and a floral cooler, and a coffee station: the coffee bar and cup counter,
 * a commercial brewer and a fountain drink machine, with the fountain drink it pours), the
 * checkout (belt, scanner and bagging counters that join into a lane, registers, the receipt
 * printer, card terminals including the Verifone MX915, the self-checkout, the customer service
 * desk, the candy rack), the tobacco case and the lottery's dispenser and terminal, the pharmacy
 * (drop-off and pick-up counters, a shelf wall, its signs), the shop floor's fixtures: carts and
 * the cart corral, basket stacks, security gates, aisle signs, a magazine rack and a bottle return
 * machine, the kiosks (coin counter, photo printing, movie rental) and the merchandisers outside
 * the door (ice, propane exchange, firewood); and the merchandising: end caps that close the
 * gondola runs, shelf tags and talkers, pallet stacks, bargain bins, seasonal tables, the
 * checkout's candy strip, hanging department signs and standing sale signs, and an apparel corner
 * of clothes rails and racks, folded clothes tables, mannequins and fitting rooms.
 *
 * <p>The produce crates came here from the Furniture tab and the Verifone from the Technology
 * module, their classes and registry names unchanged, so placed ones load as they were. The
 * lines below are printed by {@code gen_furniture_market.py --fragments}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@CsmTab.Load(order = 26)
public class CsmTabMarketStore extends CsmTab {

  @Override
  public String getTabId() {
    return "tabmarketstore";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("shopping_cart_red");
  }

  @Override
  public boolean getTabSearchable() {
    return true;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // ---- Refrigerated ----
    // Reach-In Cooler
    initTabBlock(new BlockDisplayCooler("reach_in_cooler_black", new int[]{0, 0, 2, 16, 32, 16}));
    initTabBlock(new BlockDisplayCooler("reach_in_cooler_white", new int[]{0, 0, 2, 16, 32, 16}));

    // Reach-In Freezer
    initTabBlock(new BlockDisplayCooler("reach_in_freezer_black", new int[]{0, 0, 2, 16, 32, 16}));
    initTabBlock(new BlockDisplayCooler("reach_in_freezer_white", new int[]{0, 0, 2, 16, 32, 16}));

    // Open Dairy Case
    initTabBlock(new BlockDisplayCooler("dairy_case_black", new int[]{0, 0, 0, 16, 32, 16}));
    initTabBlock(new BlockDisplayCooler("dairy_case_white", new int[]{0, 0, 0, 16, 32, 16}));

    // Island Freezer
    initTabBlock(new BlockDisplayCase("island_freezer_white", new int[]{0, 0, 1, 16, 15, 15}));

    // Ice Cream Display Freezer
    initTabBlock(new BlockDisplayCase("ice_cream_case_white", new int[]{0, 0, 2, 16, 19, 16}));

    // Deli Service Case
    initTabBlock(new BlockDisplayCase("deli_case_black", new int[]{0, 0, 2, 16, 19, 16}, "service_case"));

    // Bakery Display Case
    initTabBlock(new BlockDisplayCase("bakery_case_oak", new int[]{0, 0, 2, 16, 19, 16}));

    // ---- Shelving ----
    // Gondola Shelving
    initTabBlock(new BlockGondola("gondola_shelf_canned", new int[]{0, 0, 6, 16, 16, 16}));
    initTabBlock(new BlockGondola("gondola_shelf_cereal", new int[]{0, 0, 6, 16, 16, 16}));
    initTabBlock(new BlockGondola("gondola_shelf_snacks", new int[]{0, 0, 6, 16, 16, 16}));
    initTabBlock(new BlockGondola("gondola_shelf_drinks", new int[]{0, 0, 6, 16, 16, 16}));
    initTabBlock(new BlockGondola("gondola_shelf_household", new int[]{0, 0, 6, 16, 16, 16}));
    initTabBlock(new BlockGondola("gondola_shelf_health", new int[]{0, 0, 6, 16, 16, 16}));
    initTabBlock(new BlockGondola("gondola_shelf_empty", new int[]{0, 0, 6, 16, 16, 16}));

    // End Cap Display
    initTabBlock(new BlockGondola("end_cap_canned", new int[]{0, 0, 2, 16, 16, 16}));
    initTabBlock(new BlockGondola("end_cap_cereal", new int[]{0, 0, 2, 16, 16, 16}));
    initTabBlock(new BlockGondola("end_cap_snacks", new int[]{0, 0, 2, 16, 16, 16}));
    initTabBlock(new BlockGondola("end_cap_drinks", new int[]{0, 0, 2, 16, 16, 16}));
    initTabBlock(new BlockGondola("end_cap_household", new int[]{0, 0, 2, 16, 16, 16}));
    initTabBlock(new BlockGondola("end_cap_health", new int[]{0, 0, 2, 16, 16, 16}));

    // ---- Merchandising ----
    // Pallet Stack
    initTabBlock(new BlockBathroomFixture("pallet_stack_drinks", new int[]{0, 0, 0, 16, 16, 16}, FixtureMaterial.WOOD));
    initTabBlock(new BlockBathroomFixture("pallet_stack_water", new int[]{0, 0, 0, 16, 16, 16}, FixtureMaterial.WOOD));
    initTabBlock(new BlockBathroomFixture("pallet_stack_paper", new int[]{0, 0, 0, 16, 16, 16}, FixtureMaterial.WOOD));

    // Bargain Bin
    initTabBlock(new BlockBathroomFixture("bargain_bin_chrome", new int[]{1, 0, 1, 15, 18, 16}, FixtureMaterial.METAL));

    // Cardboard Dump Bin
    initTabBlock(new BlockBathroomFixture("dump_bin_kraft", new int[]{1, 0, 0, 15, 15, 15}, FixtureMaterial.WOOD));

    // Seasonal Display Table
    initTabBlock(new BlockMarketRun("seasonal_table_summer", new int[]{0, 0, 0, 16, 24, 16}, "seasonal_table", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("seasonal_table_harvest", new int[]{0, 0, 0, 16, 24, 16}, "seasonal_table", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("seasonal_table_winter", new int[]{0, 0, 0, 16, 24, 16}, "seasonal_table", BlockRenderLayer.CUTOUT));

    // ---- Produce ----
    // Produce crates (moved from the Furniture tab)
    initTabBlock(BlockAppleCrate.class, fmlPreInitializationEvent); // Apple Crate
    initTabBlock(BlockBananaCrate.class, fmlPreInitializationEvent); // Banana Crate
    initTabBlock(BlockBeetCrate.class, fmlPreInitializationEvent); // Beet Crate
    initTabBlock(BlockCarrotBarrel.class, fmlPreInitializationEvent); // Carrot Barrel
    initTabBlock(BlockCarrotCrate.class, fmlPreInitializationEvent); // Carrot Crate
    initTabBlock(BlockCornCrate.class, fmlPreInitializationEvent); // Corn Crate
    initTabBlock(BlockGoldenApples.class, fmlPreInitializationEvent); // Golden Apples
    initTabBlock(BlockGreenAppleCrate.class, fmlPreInitializationEvent); // Green Apple Crate
    initTabBlock(BlockLargeCrate.class, fmlPreInitializationEvent); // Large Crate
    initTabBlock(BlockLettuceCrate.class, fmlPreInitializationEvent); // Lettuce Crate
    initTabBlock(BlockOnionCrate.class, fmlPreInitializationEvent); // Onion Crate
    initTabBlock(BlockOrangeCrate.class, fmlPreInitializationEvent); // Orange Crate
    initTabBlock(BlockPearCrate.class, fmlPreInitializationEvent); // Pear Crate
    initTabBlock(BlockPotatoeCrate.class, fmlPreInitializationEvent); // Potato Crate
    initTabBlock(BlockTomatoeCrate.class, fmlPreInitializationEvent); // Tomato Crate

    // Produce Stand
    initTabBlock(new BlockMarketRun("produce_stand_apple", new int[]{0, 0, 0, 16, 16, 15}, "produce_stand", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("produce_stand_orange", new int[]{0, 0, 0, 16, 16, 15}, "produce_stand", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("produce_stand_lettuce", new int[]{0, 0, 0, 16, 16, 15}, "produce_stand", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("produce_stand_tomato", new int[]{0, 0, 0, 16, 16, 15}, "produce_stand", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("produce_stand_potato", new int[]{0, 0, 0, 16, 16, 15}, "produce_stand", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("produce_stand_banana", new int[]{0, 0, 0, 16, 16, 15}, "produce_stand", BlockRenderLayer.CUTOUT));

    // Hanging Produce Scale
    initTabBlock(new BlockBathroomFixture("produce_scale_steel", new int[]{4, 1, 5, 12, 16, 11}, FixtureMaterial.METAL));

    // Bulk Bins
    initTabBlock(new BlockMarketRun("bulk_bins_nuts", new int[]{0, 0, 6, 16, 16, 16}, "bulk_bins", BlockRenderLayer.TRANSLUCENT));
    initTabBlock(new BlockMarketRun("bulk_bins_candy", new int[]{0, 0, 6, 16, 16, 16}, "bulk_bins", BlockRenderLayer.TRANSLUCENT));

    // ---- Butcher & Seafood ----
    // Butcher Case
    initTabBlock(new BlockDisplayCase("butcher_case_white", new int[]{0, 0, 2, 16, 24, 16}, "service_case"));
    initTabBlock(new BlockDisplayCase("butcher_case_black", new int[]{0, 0, 2, 16, 24, 16}, "service_case"));

    // Seafood Case
    initTabBlock(new BlockDisplayCase("seafood_case_white", new int[]{0, 0, 2, 16, 24, 16}, "service_case"));
    initTabBlock(new BlockDisplayCase("seafood_case_black", new int[]{0, 0, 2, 16, 24, 16}, "service_case"));

    // ---- Bakery & Hot Food ----
    // Bread Rack
    initTabBlock(new BlockMarketRun("bread_rack_oak", new int[]{0, 0, 2, 16, 20, 16}, "bread_rack", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("bread_rack_walnut", new int[]{0, 0, 2, 16, 20, 16}, "bread_rack", BlockRenderLayer.CUTOUT));

    // Self-Serve Pastry Case
    initTabBlock(new BlockMarketRun("pastry_case_white", new int[]{0, 0, 2, 16, 17, 14}, "pastry_case", BlockRenderLayer.TRANSLUCENT));
    initTabBlock(new BlockMarketRun("pastry_case_walnut", new int[]{0, 0, 2, 16, 17, 14}, "pastry_case", BlockRenderLayer.TRANSLUCENT));

    // Hot Food Case
    initTabBlock(new BlockDisplayCase("hot_food_case_steel", new int[]{0, 0, 2, 16, 23, 15}, "hot_food_case"));

    // Rotisserie Oven
    initTabBlock(new BlockCounterAppliance("rotisserie_oven_steel", new int[]{1, 0, 2, 15, 13, 13}, StoreAppliances.ROTISSERIE));

    // ---- Floral ----
    // Flower Bucket Stand
    initTabBlock(new BlockBathroomFixture("flower_stand_black", new int[]{0, 0, 1, 16, 16, 15}, FixtureMaterial.METAL));
    initTabBlock(new BlockBathroomFixture("flower_stand_oak", new int[]{0, 0, 1, 16, 16, 15}, FixtureMaterial.METAL));

    // Floral Cooler
    initTabBlock(new BlockDisplayCooler("floral_cooler_black", new int[]{0, 0, 2, 16, 32, 16}));
    initTabBlock(new BlockDisplayCooler("floral_cooler_white", new int[]{0, 0, 2, 16, 32, 16}));

    // ---- Coffee & Drinks ----
    // Self-Serve Coffee Bar
    initTabBlock(new BlockMarketRun("coffee_bar_grey", new int[]{0, 0, 1, 16, 20, 16}, "drink_station", 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE, null, 1.0F, BlockRenderLayer.SOLID));
    initTabBlock(new BlockMarketRun("coffee_bar_walnut", new int[]{0, 0, 1, 16, 20, 16}, "drink_station", 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE, null, 1.0F, BlockRenderLayer.SOLID));

    // Cup and Lid Counter
    initTabBlock(new BlockMarketRun("cup_counter_grey", new int[]{0, 0, 1, 16, 20, 16}, "drink_station", 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE, null, 1.0F, BlockRenderLayer.SOLID));
    initTabBlock(new BlockMarketRun("cup_counter_walnut", new int[]{0, 0, 1, 16, 20, 16}, "drink_station", 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE, null, 1.0F, BlockRenderLayer.SOLID));

    // Commercial Coffee Brewer
    initTabBlock(new BlockCounterAppliance("coffee_brewer_steel", new int[]{2, 0, 3, 14, 13, 13}, StoreAppliances.COFFEE_BREWER));
    initTabBlock(new BlockCounterAppliance("coffee_brewer_black", new int[]{2, 0, 3, 14, 13, 13}, StoreAppliances.COFFEE_BREWER));

    // Fountain Drink Machine
    initTabBlock(new BlockCounterAppliance("fountain_machine_red", new int[]{1, 0, 2, 15, 13, 13}, StoreAppliances.FOUNTAIN));
    initTabBlock(new BlockCounterAppliance("fountain_machine_blue", new int[]{1, 0, 2, 15, 13, 13}, StoreAppliances.FOUNTAIN));

    // Fountain Drink (poured by the fountain drink machine)
    initTabItem(new ItemResidentialFood("fountain_drink", 2, 0.3F, true, null));

    // ---- Checkout ----
    // Verifone MX915 (moved from the Technology tab)
    initTabBlock(BlockVerifoneMx915.class, fmlPreInitializationEvent);

    // Checkout Counter with Belt
    initTabBlock(new BlockMarketRun("checkout_belt_grey", new int[]{0, 0, 2, 16, 15, 14}, "checkout", BlockRenderLayer.SOLID));
    initTabBlock(new BlockMarketRun("checkout_belt_walnut", new int[]{0, 0, 2, 16, 15, 14}, "checkout", BlockRenderLayer.SOLID));

    // Checkout Scanner Counter
    initTabBlock(new BlockMarketRun("checkout_scanner_grey", new int[]{0, 0, 2, 16, 19, 14}, "checkout", 0, null, null, FurnishingsSounds.SCANNER_BEEP, 1.0F, BlockRenderLayer.SOLID));
    initTabBlock(new BlockMarketRun("checkout_scanner_walnut", new int[]{0, 0, 2, 16, 19, 14}, "checkout", 0, null, null, FurnishingsSounds.SCANNER_BEEP, 1.0F, BlockRenderLayer.SOLID));

    // Checkout Bagging End
    initTabBlock(new BlockMarketRun("checkout_bagging_grey", new int[]{0, 0, 2, 16, 18, 14}, "checkout", 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE, null, 1.0F, BlockRenderLayer.SOLID));
    initTabBlock(new BlockMarketRun("checkout_bagging_walnut", new int[]{0, 0, 2, 16, 18, 14}, "checkout", 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE, null, 1.0F, BlockRenderLayer.SOLID));

    // POS Terminal
    initTabBlock(new BlockCashRegister("pos_terminal_black", new int[]{2, 0, 3, 14, 10, 13}));

    // Vintage Cash Register
    initTabBlock(new BlockCashRegister("cash_register_brass", new int[]{3, 0, 3, 15, 9, 13}));

    // Receipt Printer
    initTabBlock(new BlockCounterPiece("receipt_printer_black", new int[]{5, 0, 4, 11, 4, 12}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.PRINTER_RUN, 1.8F, null));

    // Card Terminal on Stand
    initTabBlock(new BlockCounterPiece("card_terminal_stand_black", new int[]{5, 0, 5, 11, 6, 11}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.VERIFONE_MX915, 1.0F, null));

    // Bagging Carousel
    initTabBlock(new BlockCounterPiece("bag_carousel_white", new int[]{2, 0, 2, 14, 11, 14}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID));

    // Self-Checkout Kiosk
    initTabBlock(new BlockMarketTall("self_checkout_grey", new int[]{0, 0, 2, 16, 31, 15}, 7, FurnishingsSounds.SCANNER_BEEP));

    // Customer Service Desk
    initTabBlock(new BlockKitchenCabinet("service_desk_blue", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("service_desk_red", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));

    // Checkout Candy Rack
    initTabBlock(new BlockBathroomFixture("impulse_rack_black", new int[]{1, 0, 5, 15, 21, 13}, FixtureMaterial.METAL));

    // Checkout Counter with Candy Strip
    initTabBlock(new BlockMarketRun("checkout_candy_grey", new int[]{0, 0, 0, 16, 15, 14}, "checkout", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("checkout_candy_walnut", new int[]{0, 0, 0, 16, 15, 14}, "checkout", BlockRenderLayer.CUTOUT));

    // ---- Tobacco & Lottery ----
    // Tobacco Case
    initTabBlock(new BlockMarketRun("tobacco_case_black", new int[]{0, 0, 9, 16, 16, 16}, "tobacco_case", 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE, null, 1.0F, BlockRenderLayer.TRANSLUCENT));
    initTabBlock(new BlockMarketRun("tobacco_case_walnut", new int[]{0, 0, 9, 16, 16, 16}, "tobacco_case", 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE, null, 1.0F, BlockRenderLayer.TRANSLUCENT));

    // Lottery Ticket Dispenser
    initTabBlock(new BlockCounterPiece("lottery_dispenser_clear", new int[]{2, 0, 5, 14, 12, 12}, Material.WOOD, SoundType.METAL, BlockRenderLayer.TRANSLUCENT));

    // Lottery Terminal
    initTabBlock(new BlockCounterPiece("lottery_terminal_black", new int[]{3, 0, 5, 15, 15, 13}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.PRINTER_RUN, 1.8F, null));

    // ---- Pharmacy ----
    // Pharmacy Drop-Off Counter
    initTabBlock(new BlockKitchenCabinet("pharmacy_dropoff_teal", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("pharmacy_dropoff_walnut", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));

    // Pharmacy Pick-Up Counter
    initTabBlock(new BlockKitchenCabinet("pharmacy_pickup_teal", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("pharmacy_pickup_walnut", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));

    // Pharmacy Shelf Wall
    initTabBlock(new BlockMarketRun("pharmacy_shelf_white", new int[]{0, 0, 8, 16, 16, 16}, "pharmacy_shelf", BlockRenderLayer.CUTOUT));

    // Pharmacy Sign
    initTabBlock(new BlockBathroomFixture("pharmacy_sign_teal", new int[]{0, 8, 7, 16, 16, 9}, FixtureMaterial.METAL));

    // Consultation Sign
    initTabBlock(new BlockBathroomFixture("consultation_sign_teal", new int[]{0, 8, 7, 16, 16, 9}, FixtureMaterial.METAL));

    // ---- Store ----
    // Shopping Cart
    initTabBlock(new BlockBathroomFixture("shopping_cart_red", new int[]{2, 0, 0, 14, 16, 16}, FixtureMaterial.METAL));
    initTabBlock(new BlockBathroomFixture("shopping_cart_blue", new int[]{2, 0, 0, 14, 16, 16}, FixtureMaterial.METAL));
    initTabBlock(new BlockBathroomFixture("shopping_cart_grey", new int[]{2, 0, 0, 14, 16, 16}, FixtureMaterial.METAL));

    // Cart Corral
    initTabBlock(new BlockMarketRun("cart_corral_steel", new int[]{0, 0, 1, 16, 20, 15}, "cart_corral", BlockRenderLayer.SOLID));

    // Shopping Basket Stack
    initTabBlock(new BlockBathroomFixture("basket_stack_red", new int[]{2, 0, 3, 14, 20, 14}, FixtureMaterial.PLASTIC));
    initTabBlock(new BlockBathroomFixture("basket_stack_green", new int[]{2, 0, 3, 14, 20, 14}, FixtureMaterial.PLASTIC));

    // Security Gate
    initTabBlock(new BlockBathroomFixture("security_gate_grey", new int[]{6, 0, 2, 10, 23, 14}, FixtureMaterial.PLASTIC, FurnishingsSounds.APPLIANCE_BEEP, 1.4F));

    // Aisle Sign
    initTabBlock(new BlockAisleSign("aisle_sign_blue", new int[]{0, 4, 7, 16, 16, 9}));

    // Magazine Rack
    initTabBlock(new BlockBathroomFixture("magazine_rack_black", new int[]{0, 0, 9, 16, 20, 15}, FixtureMaterial.METAL));

    // Bottle Return Machine
    initTabBlock(new BlockMarketTall("bottle_return_machine_green", new int[]{0, 0, 3, 16, 29, 16}, 4, null));

    // ---- Signs & Tags ----
    // Department Sign
    initTabBlock(new BlockDepartmentSign("department_sign_black", new int[]{0, 4, 7, 16, 16, 9}));

    // Sale Sign
    initTabBlock(new BlockBathroomFixture("sale_sign_red", new int[]{3, 0, 4, 13, 24, 12}, FixtureMaterial.METAL));

    // Hot Deal Sign
    initTabBlock(new BlockBathroomFixture("deal_sign_orange", new int[]{3, 0, 4, 13, 24, 12}, FixtureMaterial.METAL));

    // ---- Apparel ----
    // Rolling Clothes Rail
    initTabBlock(new BlockMarketRun("clothing_rail_chrome", new int[]{0, 0, 2, 16, 24, 14}, "clothing_rail", BlockRenderLayer.CUTOUT));

    // Round Clothes Rack
    initTabBlock(new BlockBathroomFixture("round_rack_chrome", new int[]{0, 0, 0, 16, 22, 16}, FixtureMaterial.METAL));

    // Folded Clothes Table
    initTabBlock(new BlockMarketRun("apparel_table_oak", new int[]{0, 3, 1, 16, 16, 15}, "apparel_table", BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockMarketRun("apparel_table_white", new int[]{0, 3, 1, 16, 16, 15}, "apparel_table", BlockRenderLayer.CUTOUT));

    // Mannequin
    initTabBlock(new BlockMarketTall("mannequin_white", new int[]{3, 0, 5, 13, 30, 12}, 0, null));
    initTabBlock(new BlockMarketTall("mannequin_black", new int[]{3, 0, 5, 13, 30, 12}, 0, null));

    // Dress Form
    initTabBlock(new BlockMarketTall("dress_form_linen", new int[]{5, 0, 5, 11, 26, 11}, 0, null));
    initTabBlock(new BlockMarketTall("dress_form_black", new int[]{5, 0, 5, 11, 26, 11}, 0, null));

    // Fitting Room
    initTabBlock(new BlockFittingRoom("fitting_room_white", new int[]{0, 0, 0, 16, 32, 16}));
    initTabBlock(new BlockFittingRoom("fitting_room_walnut", new int[]{0, 0, 0, 16, 32, 16}));

    // ---- Kiosks ----
    // Coin Counting Kiosk
    initTabBlock(new BlockMarketTall("coin_kiosk_blue", new int[]{2, 0, 3, 14, 28, 15}, 7, FurnishingsSounds.APPLIANCE_BEEP));

    // Photo Printing Kiosk
    initTabBlock(new BlockMarketTall("photo_kiosk_white", new int[]{1, 0, 3, 15, 28, 15}, 7, FurnishingsSounds.PRINTER_RUN));

    // DVD Rental Kiosk
    initTabBlock(new BlockMarketTall("dvd_kiosk_purple", new int[]{0, 0, 2, 16, 32, 16}, 7, FurnishingsSounds.APPLIANCE_BEEP));

    // ---- Outdoor ----
    // Ice Merchandiser
    initTabBlock(new BlockMarketRun("ice_merchandiser_white", new int[]{0, 0, 1, 16, 20, 16}, "ice_merchandiser", 27, FurnishingsSounds.FRIDGE_OPEN, FurnishingsSounds.FRIDGE_CLOSE, null, 1.0F, BlockRenderLayer.SOLID));
    initTabBlock(new BlockMarketRun("ice_merchandiser_blue", new int[]{0, 0, 1, 16, 20, 16}, "ice_merchandiser", 27, FurnishingsSounds.FRIDGE_OPEN, FurnishingsSounds.FRIDGE_CLOSE, null, 1.0F, BlockRenderLayer.SOLID));

    // Propane Exchange Cage
    initTabBlock(new BlockMarketRun("propane_cage_grey", new int[]{0, 0, 1, 16, 24, 15}, "propane_cage", BlockRenderLayer.CUTOUT));

    // Firewood Rack
    initTabBlock(new BlockMarketRun("firewood_rack_black", new int[]{0, 0, 2, 16, 23, 15}, "firewood_rack", BlockRenderLayer.CUTOUT));
  }
}
