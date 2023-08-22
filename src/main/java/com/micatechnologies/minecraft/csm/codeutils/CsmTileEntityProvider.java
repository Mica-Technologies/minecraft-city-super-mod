package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.powergrid.fe.TileEntityForgeEnergyProducer;
import javafx.scene.effect.Reflection;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

public abstract class CsmTileEntityProvider< T extends AbstractTileEntity >
        implements ICsmTileEntityProvider
{

    /**
     * Constructs an {@link AbstractBlock} instance.
     *
     * @param material         The material of the block.
     * @param soundType        The sound type of the block.
     * @param harvestToolClass The harvest tool class of the block.
     * @param harvestLevel     The harvest level of the block.
     * @param hardness         The block's hardness.
     * @param resistance       The block's resistance to explosions.
     * @param lightLevel       The block's light level.
     * @param lightOpacity     The block's light opacity.
     *
     * @since 1.0
     */
    public CsmTileEntityProvider( Material material,
                                  SoundType soundType,
                                  String harvestToolClass,
                                  int harvestLevel,
                                  float hardness,
                                  float resistance,
                                  float lightLevel,
                                  int lightOpacity )
    {
        super( material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel, lightOpacity );
    }

    @Override
    @ParametersAreNonnullByDefault
    public TileEntity createNewTileEntity( World world, int i ) {
        // Return new created from type parameter
        return
    }

    @Override
    public boolean hasTileEntity( IBlockState p_hasTileEntity_1_ ) {
        return true;
    }
}
