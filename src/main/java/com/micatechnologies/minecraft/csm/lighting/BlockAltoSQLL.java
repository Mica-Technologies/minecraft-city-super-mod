package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockAltoSQLL extends ElementsCitySuperMod.ModElement
{
    public static final String elementId = "altosqll";

    @GameRegistry.ObjectHolder( "csm:" + elementId )
    public static final Block block = null;

    public BlockAltoSQLL( ElementsCitySuperMod instance ) {
        super( instance, 349 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom() );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:" + elementId, "inventory" ) );
    }

    public static class BlockCustom extends AbstractBrightLight
    {
        @Override
        public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
            switch ( state.getValue( BlockHorizontal.FACING ) ) {
                case SOUTH:
                default:
                    return new AxisAlignedBB( 1D, 0D, 1D, 0D, 0.5D, 0D );
                case NORTH:
                    return new AxisAlignedBB( 0D, 0D, 0D, 1D, 0.5D, 1D );
                case WEST:
                    return new AxisAlignedBB( 0D, 0D, 1D, 1D, 0.5D, 0D );
                case EAST:
                    return new AxisAlignedBB( 1D, 0D, 0D, 0D, 0.5D, 1D );
            }
        }

        @Override
        public String getBlockRegistryName() {
            return elementId;
        }

        @Override
        public int getBrightLightXOffset() {
            return 0;
        }
    }
}
