package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEmergencyLightBlack;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEmergencyLightFactory;
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
    initTabBlock(new BlockEmergencyLightFactory("emergency_light_twin_head_white", new AxisAlignedBB(0.156250, 0.250000, 0.625000, 0.843750, 0.906250, 1.000000), new float[][]{new float[]{3.8F, 10.8F, 10F, 7.2F, 14.2F, 11F}, new float[]{8.8F, 10.8F, 10F, 12.2F, 14.2F, 11F}}));
    initTabBlock(new BlockEmergencyLightFactory("emergency_light_twin_head_black", new AxisAlignedBB(0.156250, 0.250000, 0.625000, 0.843750, 0.906250, 1.000000), new float[][]{new float[]{3.8F, 10.8F, 10F, 7.2F, 14.2F, 11F}, new float[]{8.8F, 10.8F, 10F, 12.2F, 14.2F, 11F}}));
    initTabBlock(new BlockEmergencyLightFactory("emergency_light_twin_head_gray", new AxisAlignedBB(0.156250, 0.250000, 0.625000, 0.843750, 0.906250, 1.000000), new float[][]{new float[]{3.8F, 10.8F, 10F, 7.2F, 14.2F, 11F}, new float[]{8.8F, 10.8F, 10F, 12.2F, 14.2F, 11F}}));
    initTabBlock(new BlockEmergencyLightFactory("emergency_light_led_bar", new AxisAlignedBB(0.062500, 0.375000, 0.812500, 0.937500, 0.625000, 1.000000), new float[][]{new float[]{2F, 6.8F, 13.5F, 14F, 8.8F, 14F}}));
    initTabBlock(new BlockEmergencyLightFactory("emergency_remote_head_single", new AxisAlignedBB(0.312500, 0.312500, 0.625000, 0.687500, 0.687500, 1.000000), new float[][]{new float[]{6.1F, 6.1F, 10.5F, 9.9F, 9.9F, 11.5F}}));
    initTabBlock(new BlockEmergencyLightFactory("emergency_remote_head_twin", new AxisAlignedBB(0.125000, 0.312500, 0.625000, 0.875000, 0.687500, 1.000000), new float[][]{new float[]{3.1F, 6.1F, 10.5F, 6.9F, 9.9F, 11.5F}, new float[]{9.1F, 6.1F, 10.5F, 12.9F, 9.9F, 11.5F}}));
    initTabBlock(new BlockEmergencyLightFactory("emergency_wall_pack", new AxisAlignedBB(0.187500, 0.125000, 0.562500, 0.812500, 0.750000, 1.000000), new float[][]{new float[]{3.6F, 2.6F, 9.9F, 12.4F, 7F, 10.5F}}));
    initTabBlock(new BlockEmergencyLightFactory("emergency_downlight_recessed", new AxisAlignedBB(0.218750, 0.218750, 0.906250, 0.781250, 0.781250, 1.000000), new float[][]{new float[]{5.2F, 5.2F, 14.9F, 10.8F, 10.8F, 15.5F}}));
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
