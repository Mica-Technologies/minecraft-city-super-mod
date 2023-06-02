package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
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
public class BlockFireAlarmSprinklerBlack extends ElementsCitySuperMod.ModElement
{
    public static final String blockRegistryName = "firealarmsprinklerblack";
    @GameRegistry.ObjectHolder( "csm:" + blockRegistryName )
    public static final Block  block             = null;

    public BlockFireAlarmSprinklerBlack( ElementsCitySuperMod instance ) {
        super( instance, 2119 );
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
            int waterX = blockPos.getX();
            int waterY = blockPos.getY() - 1;
            int waterZ = blockPos.getZ();
            BlockPos waterBlockPos = new BlockPos( waterX, waterY, waterZ );
            IBlockState previousBlockState = world.getBlockState( waterBlockPos );

            world.setBlockState( waterBlockPos, Blocks.FLOWING_WATER.getDefaultState(), 3 );
            world.notifyBlockUpdate( waterBlockPos, previousBlockState, Blocks.FLOWING_WATER.getDefaultState(), 3 );
            world.notifyNeighborsOfStateChange( waterBlockPos, Blocks.FLOWING_WATER, true );
        }

    }
}
