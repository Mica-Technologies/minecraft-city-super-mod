package com.micatechnologies.minecraft.csm.powergrid.fe;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.tabs.CsmTabPowerGrid;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemEWSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemNSSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class BlockForgeEnergyProducer extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:rfprod" )
    public static final Block block = null;

    public BlockForgeEnergyProducer( ElementsCitySuperMod instance ) {
        super( instance, 1879 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "rfprod" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @Override
    public void init( FMLInitializationEvent event ) {
        GameRegistry.registerTileEntity( TileEntityForgeEnergyProducer.class, "csm:tileentityforgeenergyproducer" );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:rfprod", "inventory" ) );
    }

    public static class BlockCustom extends Block implements ITileEntityProvider
    {

        public BlockCustom() {
            super( Material.ANVIL );
            setUnlocalizedName( "rfprod" );
            setSoundType( SoundType.ANVIL );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( CsmTabPowerGrid.get() );
        }

        @Override
        @ParametersAreNonnullByDefault
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
        }

		@Override
        @ParametersAreNonnullByDefault
        public boolean canProvidePower( IBlockState p_canProvidePower_1_ ) {
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
            p_addInformation_3_.add( I18n.format( "csm.highvoltage" ) );
        }

        @Override
        @ParametersAreNonnullByDefault
        public TileEntity createNewTileEntity( World world, int i ) {
            return new TileEntityForgeEnergyProducer();
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
            // Increment tick rate if tile entity is present and valid
            if ( p_onBlockActivated_4_.isSneaking() ) {
                TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                if ( tileEntity instanceof TileEntityForgeEnergyProducer ) {
                    TileEntityForgeEnergyProducer tileEntityForgeEnergyProducer
                            = ( TileEntityForgeEnergyProducer ) tileEntity;
                    int tickRate = tileEntityForgeEnergyProducer.incrementTickRate();
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        double tickRateSeconds = ( double ) tickRate / 20.0;
                        p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                "Producer infinite output rate set to " +
                                        tickRate +
                                        " ticks" +
                                        " (" +
                                        tickRateSeconds +
                                        " seconds)." ) );
                    }
                }

                return true;
            } else {
                return super.onBlockActivated( p_onBlockActivated_1_,
                                               p_onBlockActivated_2_,
                                               p_onBlockActivated_3_,
                                               p_onBlockActivated_4_,
                                               p_onBlockActivated_5_,
                                               p_onBlockActivated_6_,
                                               p_onBlockActivated_7_,
                                               p_onBlockActivated_8_,
                                               p_onBlockActivated_9_ );
            }
        }

        @Override
        public boolean hasTileEntity( IBlockState p_hasTileEntity_1_ ) {
            return true;
        }
    }
}
