package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * Factory variant of {@link BlockRotatableNSEWUDFactory} that attaches a
 * {@link TileEntityHvacVentRelay} to every placement, turning otherwise cosmetic vent
 * blocks into real HVAC distribution endpoints. The TE pauses ticking when no thermostat
 * link exists, so unlinked decorative placements remain effectively free at runtime.
 *
 * <p>All instances share the {@code tileentityhvacventrelay} TE registration; the existing
 * {@link BlockHvacVentRelay} class registers it first and the duplicate-name guard in
 * {@code Csm#init} silently skips re-registration for every other vent block.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockHvacVentFactory extends BlockRotatableNSEWUDFactory
    implements ICsmTileEntityProvider {

  public BlockHvacVentFactory(String registryName, Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      float lightLevel, int lightOpacity, AxisAlignedBB boundingBox,
      boolean opaqueCube, boolean fullCube, boolean connectsRedstone,
      BlockRenderLayer renderLayer, boolean passable, boolean nullCollision) {
    super(registryName, material, soundType, harvestToolClass, harvestLevel, hardness,
        resistance, lightLevel, lightOpacity, boundingBox, opaqueCube, fullCube,
        connectsRedstone, renderLayer, passable, nullCollision);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacVentRelay.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvacventrelay";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacVentRelay();
  }
}
