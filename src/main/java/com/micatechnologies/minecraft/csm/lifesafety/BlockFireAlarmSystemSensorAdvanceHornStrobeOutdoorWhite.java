package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorWhite extends ElementsCitySuperMod.ModElement
{
    public static final String blockRegistryName = "firealarmsystemsensoradvancehornstrobeoutdoorwhite";
    @GameRegistry.ObjectHolder( "csm:" + blockRegistryName )
    public static final Block  block             = null;

    public BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorWhite( ElementsCitySuperMod instance ) {
        super( instance, 2101 );
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

    public static class BlockCustom extends AbstractBlockFireAlarmSounder
    {
        @Override
        public String getSoundResourceName( IBlockState blockState) {
            return "csm:spectralert";
        }

        @Override
        public int getSoundTickLen(IBlockState blockState) {
            return 60;
        }

        @Override
        public String getBlockRegistryName() {
            return blockRegistryName;
        }
    }
}
