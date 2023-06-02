package com.micatechnologies.minecraft.csm.powergrid.fe;

import com.micatechnologies.minecraft.csm.tabs.CsmTabPowerGrid;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class BlockForgeEnergyToRedstone extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:rftors" )
    public static final Block block = null;

    public BlockForgeEnergyToRedstone( ElementsCitySuperMod instance ) {
        super( instance, 1156 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "rftors" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @Override
    public void init( FMLInitializationEvent event ) {
        GameRegistry.registerTileEntity( TileEntityForgeEnergyConsumer.class, "csm:tileentityforgeenergyconsumer" );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:rftors", "inventory" ) );
    }

    public static class BlockCustom extends Block implements ITileEntityProvider
    {

        public static final PropertyBool POWERED = PropertyBool.create( "powered" );

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "rftors" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( CsmTabPowerGrid.get() );
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
        @ParametersAreNonnullByDefault
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
        }

        @Override
        @ParametersAreNonnullByDefault
        public int getWeakPower( IBlockState p_getWeakPower_1_,
                                 IBlockAccess p_getWeakPower_2_,
                                 BlockPos p_getWeakPower_3_,
                                 EnumFacing p_getWeakPower_4_ )
        {
            return p_getWeakPower_1_.getValue( POWERED ) ? 15 : 0;
        }

        @Override
        @ParametersAreNonnullByDefault
        public boolean canProvidePower( IBlockState p_canProvidePower_1_ ) {
            return true;
        }

        @Override
        @ParametersAreNonnullByDefault
        public int getStrongPower( IBlockState p_getStrongPower_1_,
                                   IBlockAccess p_getStrongPower_2_,
                                   BlockPos p_getStrongPower_3_,
                                   EnumFacing p_getStrongPower_4_ )
        {
            return p_getStrongPower_1_.getValue( POWERED ) ? 15 : 0;
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer( this, POWERED );
        }

        @Override
        @ParametersAreNonnullByDefault
        public void addInformation( ItemStack p_addInformation_1_,
                                    World p_addInformation_2_,
                                    List< String > p_addInformation_3_,
                                    ITooltipFlag p_addInformation_4_ )
        {
            super.addInformation( p_addInformation_1_, p_addInformation_2_, p_addInformation_3_, p_addInformation_4_ );
            p_addInformation_3_.add( I18n.format( "csm.highvoltage" ) );
        }

        @Override
        @ParametersAreNonnullByDefault
        public TileEntity createNewTileEntity( World world, int i ) {
            return new TileEntityForgeEnergyConsumer();
        }

        @Override
        public boolean hasTileEntity( IBlockState p_hasTileEntity_1_ ) {
            return true;
        }
    }
}
