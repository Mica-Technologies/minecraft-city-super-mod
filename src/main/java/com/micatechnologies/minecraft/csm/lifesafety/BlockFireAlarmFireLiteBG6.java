package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockFireAlarmFireLiteBG6 extends ElementsCitySuperMod.ModElement
{
    public static final String blockRegistryName = "firealarmfirelitebg6";
    @GameRegistry.ObjectHolder( "csm:" + blockRegistryName )
    public static final Block  block             = null;

    public BlockFireAlarmFireLiteBG6( ElementsCitySuperMod instance ) {
        super( instance, 2110 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( blockRegistryName ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @Override
    public void init( FMLInitializationEvent event ) {
        GameRegistry.registerTileEntity( TileEntityFireAlarmSensor.class,
                                         "csm" + ":tileentityfirealarmsensor" );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:" + blockRegistryName,
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockFireAlarmActivator
    {

        @Override
        public boolean onBlockActivated( World world,
                                         BlockPos blockPos,
                                         IBlockState blockState,
                                         EntityPlayer entityPlayer,
                                         EnumHand enumHand,
                                         EnumFacing enumFacing,
                                         float p_onBlockActivated_7_,
                                         float p_onBlockActivated_8_,
                                         float p_onBlockActivated_9_ )
        {
            if ( entityPlayer.inventory.getCurrentItem() != null &&
                    ( entityPlayer.inventory.getCurrentItem().getItem() instanceof ItemFireAlarmLinker.ItemCustom ) ) {
                return super.onBlockActivated( world, blockPos, blockState, entityPlayer, enumHand, enumFacing,
                                               p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_ );
            }

            boolean activated = activateLinkedPanel( world, blockPos,entityPlayer );
            if ( !activated && !world.isRemote ) {
                entityPlayer.sendMessage( new TextComponentString( "WARNING: This pull station has lost connection, " +
                                                                           "has failed or is otherwise not functional." ) );

            }
            return true;
        }

        @Override
        public String getBlockRegistryName() {
            return blockRegistryName;
        }

        @Override
        public int getBlockTickRate() {
            return 20;
        }

        @Override
        public void onTick( World world, BlockPos blockPos, IBlockState blockState ) {
            // Do nothing
        }
    }
}
