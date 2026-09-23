package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEmergencyLightBlack;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEmergencyLightWhite;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.BlockExitSignCombo;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.BlockExitSignDieCast;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.BlockExitSignExplosionProof;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.BlockExitSignPhotoluminescent;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.BlockExitSignTraditionalFlat;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.BlockExitSignTraditionalRounded;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.BlockExitSignVandalResistant;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Exits &amp; Emergency Lighting tab of the Life Safety module: exit and stair signs, and
 * the emergency lights that take over when the power fails. Moved out of the one Life Safety
 * tab with their registry names unchanged.
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 20)
public class CsmTabExitsEmergency extends CsmTab {

  @Override
  public String getTabId() {
    return "tabexitsemergency";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("mclacodeapprovedexitsignisa");
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
    initTabBlock(BlockEmergencyLightWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockEmergencyLightBlack.class, fmlPreInitializationEvent);
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsigndownarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsigndownarrowsinglesided", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsignleftarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsignrightarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsignupleftarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsignuprightarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsign", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsigndual", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsigndualisa", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignisa", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignisasinglesided", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignleft", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignleftisa", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignright", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignrightisa", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(BlockExitSignTraditionalFlat.class, fmlPreInitializationEvent);
    initTabBlock(BlockExitSignTraditionalRounded.class, fmlPreInitializationEvent);
    initTabBlock(BlockExitSignCombo.class, fmlPreInitializationEvent);
    initTabBlock(BlockExitSignDieCast.class, fmlPreInitializationEvent);
    initTabBlock(BlockExitSignVandalResistant.class, fmlPreInitializationEvent);
    initTabBlock(BlockExitSignPhotoluminescent.class, fmlPreInitializationEvent);
    initTabBlock(BlockExitSignExplosionProof.class, fmlPreInitializationEvent);
    initTabBlock(new BlockRotatableNSEWUDFactory("exitsignsinglesided", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedstairssign", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedstairssigndual", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedstairssignleft", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedstairssignright", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("stairssignonesided", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
  }
}
