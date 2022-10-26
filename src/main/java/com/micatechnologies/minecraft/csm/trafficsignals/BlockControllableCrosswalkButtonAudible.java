package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.Sounds;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalRequester;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalTickableRequester;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
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

import java.util.Random;

@ElementsCitySuperMod.ModElement.Tag
public class BlockControllableCrosswalkButtonAudible extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:controllablecrosswalkbuttonaudible" )
    public static final Block block = null;

    public BlockControllableCrosswalkButtonAudible( ElementsCitySuperMod instance ) {
        super( instance, 2021 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom() );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @Override
    public void init( FMLInitializationEvent event ) {
        GameRegistry.registerTileEntity( TileEntityTrafficSignalTickableRequester.class,
                                         "csm" + ":tileentitytrafficsignaltickablerequester" );

        GameRegistry.registerTileEntity( TileEntityTrafficSignalAPS.class, "csm" + ":tileentitytrafficsignalaps" );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:controllablecrosswalkbuttonaudible",
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockTrafficSignalTickableRequester
    {
        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "controllablecrosswalkbuttonaudible" );
            setUnlocalizedName( "controllablecrosswalkbuttonaudible" );
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
        public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
            return 0;
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
                case UP:
                    return new AxisAlignedBB( 0D, 0.2D, 0D, 1D, 0D, 1D );
                case DOWN:
                    return new AxisAlignedBB( 0D, 0.8D, 1D, 1D, 1D, 0D );
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
            // Switch to next sound if sneak-clicked
            if ( p_onBlockActivated_4_.isSneaking() ) {
                TileEntity rawTileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                if ( rawTileEntity instanceof TileEntityTrafficSignalAPS ) {
                    TileEntityTrafficSignalAPS tileEntity = ( TileEntityTrafficSignalAPS ) rawTileEntity;
                    String newSoundName = tileEntity.switchSound();
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage(
                                new TextComponentString( "APS has switched voice mode to: " + newSoundName ) );
                    }
                }
            }

            // Play onPress from tile entity
            if ( p_onBlockActivated_3_.getValue( COLOR ) == SIGNAL_RED ||
                    p_onBlockActivated_3_.getValue( COLOR ) == SIGNAL_YELLOW ) {
                TileEntity rawTileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                if ( rawTileEntity instanceof TileEntityTrafficSignalAPS ) {
                    TileEntityTrafficSignalAPS tileEntity = ( TileEntityTrafficSignalAPS ) rawTileEntity;
                    tileEntity.onPress();
                }
            }

            return super.onBlockActivated( p_onBlockActivated_1_, p_onBlockActivated_2_, p_onBlockActivated_3_,
                                           p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
                                           p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_ );
        }

        /**
         * Creates a new tile entity instance for this block.
         *
         * @param world The world in which the tile entity will be created.
         * @param i     The metadata value of the block.
         *
         * @return A new tile entity instance.
         *
         * @see TileEntity
         * @see TileEntityTrafficSignalTickableRequester
         */
        @Override
        public TileEntity createNewTileEntity( World world, int i ) {
            return new TileEntityTrafficSignalAPS();
        }
    }
}
