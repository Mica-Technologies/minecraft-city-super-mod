package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

@ElementsCitySuperMod.ModElement.Tag
public class BlockFireAlarmControlPanel extends ElementsCitySuperMod.ModElement
{
    public static final String blockRegistryName = "firealarmcontrolpanel";
    @GameRegistry.ObjectHolder( "csm:" + blockRegistryName )
    public static final Block  block             = null;

    public BlockFireAlarmControlPanel( ElementsCitySuperMod instance ) {
        super( instance, 2029 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( blockRegistryName ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @Override
    public void init( FMLInitializationEvent event ) {
        GameRegistry.registerTileEntity( TileEntityFireAlarmControlPanel.class,
                                         "csm" + ":tileentityfirealarmcontrolpanel" );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:" + blockRegistryName,
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends Block implements ITileEntityProvider
    {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( blockRegistryName );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( TabFireAlarms.tab );
        }

        public static EnumFacing getFacingFromEntity( BlockPos clickedBlock, EntityLivingBase entity ) {
            return EnumFacing.getFacingFromVector( ( float ) ( entity.posX - clickedBlock.getX() ),
                                                   ( float ) ( entity.posY - clickedBlock.getY() ),
                                                   ( float ) ( entity.posZ - clickedBlock.getZ() ) );
        }

        @Override
        public boolean isOpaqueCube( IBlockState p_isOpaqueCube_1_ ) {
            return false;
        }

        @Override
        public IBlockState getStateFromMeta( int meta ) {
            return getDefaultState().withProperty( FACING, EnumFacing.getHorizontal( meta ) );
        }

        @Override
        public int getMetaFromState( IBlockState state ) {
            return state.getValue( FACING ).getHorizontalIndex();
        }

        @Override
        public boolean isFullCube( IBlockState state ) {
            return false;
        }

        @Override
        public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
            switch ( state.getValue( FACING ) ) {
                case SOUTH:
                default:
                    return new AxisAlignedBB( 1D, 0D, 0.2D, 0D, 1D, 0D );
                case NORTH:
                    return new AxisAlignedBB( 0D, 0D, 0.8D, 1D, 1D, 1D );
                case WEST:
                    return new AxisAlignedBB( 0.8D, 0D, 1D, 1D, 1D, 0D );
                case EAST:
                    return new AxisAlignedBB( 0.2D, 0D, 0D, 0D, 1D, 1D );
            }
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
                                                     .getItem() instanceof ItemFireAlarmLinker.ItemCustom ) ) {
                return super.onBlockActivated( p_onBlockActivated_1_, p_onBlockActivated_2_, p_onBlockActivated_3_,
                                               p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
                                               p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_ );
            }

            // Check if panel tile entity still present/valid
            boolean valid = true;
            try {
                TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                if ( !( tileEntity instanceof TileEntityFireAlarmControlPanel ) ) {
                    valid = false;
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "Alarm panel tile entity is an instance of an improper " +
                                        "super class. Cannot operate. Will attempt to replace..." ) );
                    }
                }
            }
            catch ( Exception e ) {
                valid = false;
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage(
                            new TextComponentString( "Alarm panel tile entity has failed. Replace this panel!" ) );
                }
            }

            // If panel tile entity invalid, try to recover.
            if ( !valid ) {
                try {
                    p_onBlockActivated_1_.setTileEntity( p_onBlockActivated_2_, new TileEntityFireAlarmControlPanel() );
                    valid = true;
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "Broken alarm panel tile entity has been replaced. Appliances may need to be re-linked." ) );
                    }
                }
                catch ( Exception e ) {
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "Unable to replace broken alarm panel tile entity. Replace this panel!" ) );
                    }
                }
            }

            if ( valid ) {
                TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                if ( tileEntity instanceof TileEntityFireAlarmControlPanel ) {
                    TileEntityFireAlarmControlPanel tileEntityFireAlarmControlPanel
                            = ( TileEntityFireAlarmControlPanel ) tileEntity;

                    boolean alarmState = tileEntityFireAlarmControlPanel.getAlarmState();
                    if ( alarmState ) {
                        tileEntityFireAlarmControlPanel.setAlarmState( false );
                        if ( !p_onBlockActivated_1_.isRemote ) {
                            p_onBlockActivated_4_.sendMessage(
                                    new TextComponentString( "Panel alarm status has been reset!" ) );
                        }
                    }

                    if ( p_onBlockActivated_4_.isSneaking() ) {
                        tileEntityFireAlarmControlPanel.switchSound();
                        if ( !p_onBlockActivated_1_.isRemote ) {
                            p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                    "Switching alarm panel sound to " +
                                            tileEntityFireAlarmControlPanel.getCurrentSoundName() ) );
                        }
                    }
                }
            }

            return true;
        }

        @Override
        public IBlockState getStateForPlacement( World worldIn,
                                                 BlockPos pos,
                                                 EnumFacing facing,
                                                 float hitX,
                                                 float hitY,
                                                 float hitZ,
                                                 int meta,
                                                 EntityLivingBase placer )
        {
            return this.getDefaultState().withProperty( FACING, placer.getHorizontalFacing().getOpposite() );
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer( this, FACING );
        }

        @Override
        @ParametersAreNonnullByDefault
        public void addInformation( ItemStack p_addInformation_1_,
                                    World p_addInformation_2_,
                                    List< String > p_addInformation_3_,
                                    ITooltipFlag p_addInformation_4_ )
        {
            super.addInformation( p_addInformation_1_, p_addInformation_2_, p_addInformation_3_, p_addInformation_4_ );
            p_addInformation_3_.add( I18n.format( "csm.firealarmcontrolpanel.note" ) );
        }

        @Override
        @ParametersAreNonnullByDefault
        public TileEntity createNewTileEntity( World world, int i ) {
            return new TileEntityFireAlarmControlPanel();
        }

        @Override
        public boolean canConnectRedstone( IBlockState p_canConnectRedstone_1_,
                                           IBlockAccess p_canConnectRedstone_2_,
                                           BlockPos p_canConnectRedstone_3_,
                                           @Nullable EnumFacing p_canConnectRedstone_4_ )
        {
            return true;
        }

        @Override
        public void neighborChanged( IBlockState state,
                                     World world,
                                     BlockPos pos,
                                     Block blockIn,
                                     BlockPos p_189540_5_ )
        {
            // Check for redstone power and set alarm storm state
            boolean isPowered = world.isBlockPowered( pos );
            TileEntity tileEntity = world.getTileEntity( pos );
            if ( tileEntity instanceof TileEntityFireAlarmControlPanel ) {
                TileEntityFireAlarmControlPanel tileEntityFireAlarmControlPanel
                        = ( TileEntityFireAlarmControlPanel ) tileEntity;
                tileEntityFireAlarmControlPanel.setAlarmStormState( isPowered );
            }
        }
    }
}

