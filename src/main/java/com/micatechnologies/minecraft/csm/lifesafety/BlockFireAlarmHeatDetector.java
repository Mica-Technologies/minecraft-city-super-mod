package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockFireAlarmHeatDetector extends ElementsCitySuperMod.ModElement
{
    public static final String blockRegistryName = "firealarmheatdetector";
    @GameRegistry.ObjectHolder( "csm:" + blockRegistryName )
    public static final Block  block             = null;

    public BlockFireAlarmHeatDetector( ElementsCitySuperMod instance ) {
        super( instance, 2120 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( blockRegistryName ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:" + blockRegistryName,
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockFireAlarmDetector
    {
        @Override
        public String getBlockRegistryName() {
            return blockRegistryName;
        }

        @Override
        public void onFire( World world, BlockPos blockPos, IBlockState blockState ) {

        }
    }
}
