package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

@ElementsCitySuperMod.ModElement.Tag
public class BlockControllableTattleTaleBeacon extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:controllabletattletalebeacon" )
    public static final Block block = null;

    public BlockControllableTattleTaleBeacon( ElementsCitySuperMod instance ) {
        super( instance, 2119 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom() );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @Override
    public void init( FMLInitializationEvent event ) {
        GameRegistry.registerTileEntity( TileEntityTattleTaleBeacon.class, "csm" + ":tileentitytattletalebeacon" );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:controllabletattletalebeacon",
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockControllableSignal implements ITileEntityProvider
    {
        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "controllabletattletalebeacon" );
            setUnlocalizedName( "controllabletattletalebeacon" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( TabTrafficSignals.tab );
            this.setDefaultState(
                    this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ).withProperty( COLOR, 3 ) );
        }

        @Override
        public SIGNAL_SIDE getSignalSide( World world, BlockPos blockPos ) {
            // Get tile entity and cycle mode
            SIGNAL_SIDE side = SIGNAL_SIDE.AHEAD;
            TileEntity tileEntity = world.getTileEntity( blockPos );
            if ( tileEntity instanceof TileEntityTattleTaleBeacon ) {
                TileEntityTattleTaleBeacon tileEntityTattleTaleBeacon = ( TileEntityTattleTaleBeacon ) tileEntity;
                side = tileEntityTattleTaleBeacon.getSignalSide();
            }

            return side;
        }

        @Override
        public boolean onBlockActivated( World p_onBlockActivated_1_,
                                         BlockPos p_onBlockActivated_2_,
                                         IBlockState p_onBlockActivated_3_,
                                         EntityPlayer p_onBlockActivated_4_,
                                         EnumHand p_onBlockActivated_5_,
                                         EnumFacing p_onBlockActivated_6_,
                                         float p_onBlockActivated_7_,
                                         float p_onBlockActivated_8_,
                                         float p_onBlockActivated_9_ )
        {
            if ( !( p_onBlockActivated_4_.inventory.getCurrentItem()
                                                   .getItem() instanceof ItemEWSignalLinker.ItemCustom ) &&
                    !( p_onBlockActivated_4_.inventory.getCurrentItem()
                                                      .getItem() instanceof ItemNSSignalLinker.ItemCustom ) ) {
                // Get tile entity and cycle mode
                TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                if ( tileEntity instanceof TileEntityTattleTaleBeacon ) {
                    TileEntityTattleTaleBeacon tileEntityTattleTaleBeacon = ( TileEntityTattleTaleBeacon ) tileEntity;
                    tileEntityTattleTaleBeacon.cycleMode(p_onBlockActivated_4_);
                }
            }

            return true;
        }

        @Nullable
        @Override
        public TileEntity createNewTileEntity( World world, int i ) {
            return new TileEntityTattleTaleBeacon();
        }
    }
}
