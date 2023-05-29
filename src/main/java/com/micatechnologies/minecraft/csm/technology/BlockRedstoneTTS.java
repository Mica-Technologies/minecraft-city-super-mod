package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CitySuperMod;
import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.tabs.CsmTabTechnology;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

@ElementsCitySuperMod.ModElement.Tag
public class BlockRedstoneTTS extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:redstonetts" )
    public static final Block block = null;

    public BlockRedstoneTTS( ElementsCitySuperMod instance ) {
        super( instance, 4210 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "redstonetts" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @Override
    public void init( FMLInitializationEvent event ) {
        GameRegistry.registerTileEntity( TileEntityRedstoneTTS.class, "csm" + ":tileentityredstonetts" );
        System.setProperty( "freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory" );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:redstonetts", "inventory" ) );
    }

    public static class BlockCustom extends Block implements ITileEntityProvider
    {
        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "redstonetts" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 255 );
            setCreativeTab( CsmTabTechnology.get() );
        }

        @Override
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
        }

        @Override
        public boolean hasTileEntity( IBlockState p_hasTileEntity_1_ ) {
            return true;
        }

        @Nullable
        @Override
        public TileEntity createNewTileEntity( World worldIn, int meta ) {
            return new TileEntityRedstoneTTS();
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
        public void neighborChanged( IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos p_189540_5_ )
        {
            if ( world.isBlockPowered( pos ) && !world.isRemote ) {
                // Get tile entity for block
                TileEntity tileEntity = world.getTileEntity( pos );

                // Check if tile entity valid or not
                if ( tileEntity instanceof TileEntityRedstoneTTS ) {
                    // Cast tile entity object to proper type
                    TileEntityRedstoneTTS tileEntityRedstoneTTS = ( TileEntityRedstoneTTS ) tileEntity;

                    // Play TTS voice
                    tileEntityRedstoneTTS.readTtsString();
                }
            }
        }

        @SideOnly( Side.CLIENT )
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
            p_onBlockActivated_4_.openGui( CitySuperMod.instance, 0, p_onBlockActivated_1_,
                                           p_onBlockActivated_2_.getX(), p_onBlockActivated_2_.getY(),
                                           p_onBlockActivated_2_.getZ() );
            return true;
        }
    }
}
