package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Legend;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side item model registration for the exit signs, kept out of the block class so the
 * server never loads a client class. A sign's presets differ only in colour, so one icon per
 * block would show every preset alike: instead the stack's setup picks its icon.
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
final class ExitSignItemModels {

  private ExitSignItemModels() {
  }

  static void register(AbstractBlockExitSign block) {
    Item item = Item.getItemFromBlock(block);
    ExitSignSpec spec = block.getSpec();
    ExitSignConfig base = spec.getDefaults();
    for (Legend legend : spec.getLegends()) {
      for (Letters letters : spec.getLetterColours()) {
        for (Housing housing : spec.getHousings()) {
          for (Heads heads : spec.getHeadTypes()) {
            ModelBakery.registerItemVariants(item, location(block, base.withLegend(legend)
                .withLetters(letters).withHousing(housing).withHeads(heads)));
          }
        }
      }
    }
    ModelLoader.setCustomMeshDefinition(item, stack -> location(block, block.configOf(stack)));
  }

  private static ModelResourceLocation location(AbstractBlockExitSign block,
      ExitSignConfig config) {
    return new ModelResourceLocation(
        new ResourceLocation("csm", block.getItemModelName(config)), "inventory");
  }
}
