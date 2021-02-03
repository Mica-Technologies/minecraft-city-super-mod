package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

@ElementsCitySuperMod.ModElement.Tag
public class BlockTrafficSignalController extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:signalcontroller" )
    public static final Block block = null;

    public BlockTrafficSignalController( ElementsCitySuperMod instance ) {
        super( instance, 1997 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "signalcontroller" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @Override
    public void init( FMLInitializationEvent event ) {
        GameRegistry.registerTileEntity( TileEntityTrafficSignalController.class,
                                         "csm" + ":tileentitytrafficsignalcontroller" );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:signalcontroller", "inventory" ) );
    }

    public static class BlockCustom extends Block implements ITileEntityProvider
    {

        public static final PropertyBool POWERED = PropertyBool.create( "powered" );

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "signalcontroller" );
            setSoundType( SoundType.GROUND );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( TabTrafficSignals.tab );
            setTickRandomly( true );
        }

        @Override
        public void neighborChanged( IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos p_189540_5_ )
        {
            int powered = world.isBlockIndirectlyGettingPowered( pos );
            world.setBlockState( pos, state.withProperty( POWERED, powered > 0 ), 3 );
        }

        public static EnumFacing getFacingFromEntity( BlockPos clickedBlock, EntityLivingBase entity ) {
            return EnumFacing.getFacingFromVector( ( float ) ( entity.posX - clickedBlock.getX() ),
                                                   ( float ) ( entity.posY - clickedBlock.getY() ),
                                                   ( float ) ( entity.posZ - clickedBlock.getZ() ) );
        }

        @Override
        public IBlockState getStateFromMeta( int meta ) {
            return getDefaultState().withProperty( POWERED, meta == 1 );
        }

        @Override
        public int getMetaFromState( IBlockState state ) {
            return state.getValue( POWERED ) ? 1 : 0;
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer( this, POWERED );
        }

        @Override
        public void onBlockPlacedBy( World world,
                                     BlockPos pos,
                                     IBlockState state,
                                     EntityLivingBase placer,
                                     ItemStack stack )
        {
            int powered = world.isBlockIndirectlyGettingPowered( pos );
            world.setBlockState( pos, state.withProperty( POWERED, powered > 0 ), 2 );
        }

        @Override
        public void randomTick( World p_randomTick_1_,
                                BlockPos p_randomTick_2_,
                                IBlockState p_randomTick_3_,
                                Random p_randomTick_4_ )
        {
            TileEntity tileEntity = p_randomTick_1_.getTileEntity( p_randomTick_2_ );
            if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                TileEntityTrafficSignalController tileEntityTrafficSignalController
                        = ( TileEntityTrafficSignalController ) tileEntity;
                final long timeSinceLastTick = System.currentTimeMillis() -
                        tileEntityTrafficSignalController.getLastPhaseChangeTime();
                if ( timeSinceLastTick > maxTimeSinceLastTick( p_randomTick_1_, p_randomTick_2_ ) ) {
                    p_randomTick_1_.scheduleUpdate( p_randomTick_2_, this,
                                                    getTickRate( p_randomTick_1_, p_randomTick_2_ ) );
                }
            }
            super.randomTick( p_randomTick_1_, p_randomTick_2_, p_randomTick_3_, p_randomTick_4_ );
        }

        private double maxTimeSinceLastTick( World w, BlockPos p ) {
            // Get max time since last tick (x50 for ticks -> millis) (x1.5 to account for drift)
            return ((double)getTickRate( w, p )) * 50.0 * 1.5;
        }

        @Override
        public void updateTick( World p_updateTick_1_,
                                BlockPos p_updateTick_2_,
                                IBlockState p_updateTick_3_,
                                Random p_updateTick_4_ )
        {
            p_updateTick_1_.scheduleUpdate( p_updateTick_2_, this, getTickRate( p_updateTick_1_, p_updateTick_2_ ) );

            try {
                // Check if receiving power
                boolean powered = p_updateTick_3_.getValue( POWERED );

                TileEntity tileEntity = p_updateTick_1_.getTileEntity( p_updateTick_2_ );

                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    tileEntityTrafficSignalController.cycleSignals( powered );

                }
            }
            catch ( Exception e ) {
                System.err.println( "An error occurred while ticking a traffic signal controller: " );
                e.printStackTrace();
            }

        }

        public int getTickRate( World w, BlockPos p ) {
            int tickRate = 4;
            TileEntity tileEntity = w.getTileEntity( p );

            if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                TileEntityTrafficSignalController tileEntityTrafficSignalController
                        = ( TileEntityTrafficSignalController ) tileEntity;
                tickRate = tileEntityTrafficSignalController.getCycleTickRate();
            }
            return tickRate;
        }

        @Override
        public int tickRate( World p_tickRate_1_ ) {
            return 20;
        }

        @Override
        public void onBlockAdded( World p_onBlockAdded_1_, BlockPos p_onBlockAdded_2_, IBlockState p_onBlockAdded_3_ ) {
            p_onBlockAdded_1_.scheduleUpdate( p_onBlockAdded_2_, this,
                                              getTickRate( p_onBlockAdded_1_, p_onBlockAdded_2_ ) );
            super.onBlockAdded( p_onBlockAdded_1_, p_onBlockAdded_2_, p_onBlockAdded_3_ );
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
            if ( p_onBlockActivated_4_.inventory.getCurrentItem() != null &&
                    ( p_onBlockActivated_4_.inventory.getCurrentItem()
                                                     .getItem() instanceof ItemEWSignalLinker.ItemCustom ||
                            p_onBlockActivated_4_.inventory.getCurrentItem()
                                                           .getItem() instanceof ItemNSSignalLinker.ItemCustom ) ) {
                return super.onBlockActivated( p_onBlockActivated_1_, p_onBlockActivated_2_, p_onBlockActivated_3_,
                                               p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
                                               p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_ );
            }
            TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );

            // Check if controller tile entity still present/valid
            boolean valid = true;
            try {
                tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                if ( !( tileEntity instanceof TileEntityTrafficSignalController ) ) {
                    valid = false;
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "Controller tile entity is not an instance of traffic signal " +
                                        "controller tile entity. Cannot operate. Will attempt to replace..." ) );
                    }
                }
            }
            catch ( Exception e ) {
                valid = false;
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage( new TextComponentString(
                            "Controller tile entity has failed. Cannot operate. Will attempt to replace..." ) );
                }
            }

            // If controller tile entity invalid, try to recover.
            if ( !valid ) {
                try {
                    p_onBlockActivated_1_.setTileEntity( p_onBlockActivated_2_,
                                                         new TileEntityTrafficSignalController() );
                    valid = true;
                    tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "Broken controller tile entity has been replaced. Signals may need to be re-linked." ) );
                    }
                }
                catch ( Exception e ) {
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "Unable to replace broken controller tile entity. Replace this block." ) );
                    }
                }
            }

            // Check for ticking loss
            if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                TileEntityTrafficSignalController tileEntityTrafficSignalController
                        = ( TileEntityTrafficSignalController ) tileEntity;
                final long timeSinceLastTick = System.currentTimeMillis() -
                        tileEntityTrafficSignalController.getLastPhaseChangeTime();
                if ( timeSinceLastTick > maxTimeSinceLastTick( p_onBlockActivated_1_, p_onBlockActivated_2_ ) ) {
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "This block appears to be non-ticking. Attempting to restore..." ) );
                    }
                    p_onBlockActivated_1_.scheduleUpdate( p_onBlockActivated_2_, this,
                                                          getTickRate( p_onBlockActivated_1_, p_onBlockActivated_2_ ) );
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "Block ticking has been scheduled. If this did not resolve the problem, replace this block." ) );
                    }
                }
            }

            // Increment cycle index
            if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                TileEntityTrafficSignalController tileEntityTrafficSignalController
                        = ( TileEntityTrafficSignalController ) tileEntity;
                String cycleTitle = tileEntityTrafficSignalController.incrementCycleIndex();
                if ( !p_onBlockActivated_1_.isRemote && valid ) {
                    p_onBlockActivated_4_.sendMessage(
                            new TextComponentString( "Switching controller to mode: " + cycleTitle ) );
                }
            }

            return true;
        }

        @Override
        @ParametersAreNonnullByDefault
        public void addInformation( ItemStack p_addInformation_1_,
                                    World p_addInformation_2_,
                                    List< String > p_addInformation_3_,
                                    ITooltipFlag p_addInformation_4_ )
        {
            super.addInformation( p_addInformation_1_, p_addInformation_2_, p_addInformation_3_, p_addInformation_4_ );
            p_addInformation_3_.add( I18n.format( "csm.signalcontroller" ) );
        }

        @Override
        @ParametersAreNonnullByDefault
        public TileEntity createNewTileEntity( World world, int i ) {
            return new TileEntityTrafficSignalController();
        }
    }
}
