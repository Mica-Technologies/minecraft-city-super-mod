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
import com.micatechnologies.minecraft.csm.furniture.market.BlockDisplayCooler;
import com.micatechnologies.minecraft.csm.furniture.market.BlockGondola;
import com.micatechnologies.minecraft.csm.furniture.market.BlockMarketRun;
import com.micatechnologies.minecraft.csm.furniture.market.BlockMarketTall;
import com.micatechnologies.minecraft.csm.furniture.market.BlockVerifoneMx915;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBathroomFixture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterPiece;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenCabinet;
import com.micatechnologies.minecraft.csm.furniture.residential.FixtureMaterial;
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
 * bulk bins, the checkout (belt, scanner and bagging counters that join into a lane, registers,
 * the receipt printer, card terminals including the Verifone MX915, the self-checkout, the
 * customer service desk, the candy rack) and the shop floor's fixtures: carts and the cart
 * corral, basket stacks, security gates, aisle signs, a magazine rack and a bottle return
 * machine.
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
    initTabBlock(new BlockDisplayCase("deli_case_black", new int[]{0, 0, 2, 16, 19, 16}));

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
  }
}
