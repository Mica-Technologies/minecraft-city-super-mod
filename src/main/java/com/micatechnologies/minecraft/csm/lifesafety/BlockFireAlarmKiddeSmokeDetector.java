package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;
import com.micatechnologies.minecraft.csm.tabs.CsmTabLifeSafety;

@ElementsCitySuperMod.ModElement.Tag
public class BlockFireAlarmKiddeSmokeDetector extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:kiddesmoke" )
    public static final Block block = null;

    public BlockFireAlarmKiddeSmokeDetector( ElementsCitySuperMod instance ) {
        super( instance, 46 );
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
                                                    new ModelResourceLocation( "csm:kiddesmoke", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockDirectional.FACING;
        private             boolean           red    = false;

        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "kiddesmoke" );
            setUnlocalizedName( "kiddesmoke" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( CsmTabLifeSafety.get() );
            this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
        }

        @Override
        public IBlockState getStateFromMeta( int meta ) {
            return this.getDefaultState().withProperty( FACING, EnumFacing.getFront( meta ) );
        }

        @Override
        public int getMetaFromState( IBlockState state ) {
            return ( ( EnumFacing ) state.getValue( FACING ) ).getIndex();
        }

        @Override
        public boolean isFullCube( IBlockState state ) {
            return false;
        }

        @Override
        public EnumBlockRenderType getRenderType( IBlockState state ) {
            return EnumBlockRenderType.MODEL;
        }

        @Override
        public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
            switch ( ( EnumFacing ) state.getValue( BlockDirectional.FACING ) ) {
                case SOUTH:
                default:
                    return new AxisAlignedBB( 0.8D, 0.8D, 0.8D, 0.2D, 1D, 0.2D );
                case NORTH:
                    return new AxisAlignedBB( 0.2D, 0.8D, 0.2D, 0.8D, 1D, 0.8D );
                case WEST:
                    return new AxisAlignedBB( 0.2D, 0.8D, 0.8D, 0.8D, 1D, 0.2D );
                case EAST:
                    return new AxisAlignedBB( 0.8D, 0.8D, 0.2D, 0.2D, 1D, 0.8D );
                case UP:
                    return new AxisAlignedBB( 0.2D, 0.8D, 0.8D, 0.8D, 0.2D, 1D );
                case DOWN:
                    return new AxisAlignedBB( 0.2D, 0.2D, 0.2D, 0.8D, 0.8D, 0D );
            }
        }

        @Override
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
        }

        @Override
        public void updateTick( World world, BlockPos pos, IBlockState state, Random random ) {
            super.updateTick( world, pos, state, random );
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            if ( world.isBlockIndirectlyGettingPowered( new BlockPos( x, y, z ) ) > 0 ) {
                world.playSound((EntityPlayer) null, x, y, z,
                                (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:smokealarm")),
                                SoundCategory.NEUTRAL, (float) 4, (float) 1);
                world.scheduleUpdate( new BlockPos( x, y, z ), this, this.tickRate( world ) );
            }
        }

        @Override
        public void neighborChanged( IBlockState state,
                                     World world,
                                     BlockPos pos,
                                     Block neighborBlock,
                                     BlockPos fromPos )
        {
            super.neighborChanged( state, world, pos, neighborBlock, fromPos );
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            Block block = this;
            if ( world.isBlockIndirectlyGettingPowered( new BlockPos( x, y, z ) ) > 0 ) {
                {
                    world.playSound((EntityPlayer) null, x, y, z,
                                    (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:smokealarm")),
                                    SoundCategory.NEUTRAL, (float) 4, (float) 1);
                    world.scheduleUpdate( new BlockPos( x, y, z ), this, this.tickRate( world ) );
                }
            }
        }

        @Override
        public int tickRate( World world ) {
            return 80;
        }

        @SideOnly( Side.CLIENT )
        @Override
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }

        @Override
        public boolean onBlockActivated( World world,
                                         BlockPos pos,
                                         IBlockState state,
                                         EntityPlayer entity,
                                         EnumHand hand,
                                         EnumFacing side,
                                         float hitX,
                                         float hitY,
                                         float hitZ )
        {
            super.onBlockActivated( world, pos, state, entity, hand, side, hitX, hitY, hitZ );
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            world.playSound((EntityPlayer) null, x, y, z,
                            (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:smokealarm")),
                            SoundCategory.NEUTRAL, (float) 4, (float) 1);
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
            return this.getDefaultState()
                       .withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos, placer ) );
        }

        @Override
        public int getWeakPower( IBlockState state, IBlockAccess baccess, BlockPos pos, EnumFacing side ) {
            return red ? 15 : 0;
        }

        @Override
        public int getStrongPower( IBlockState state, IBlockAccess baccess, BlockPos pos, EnumFacing side ) {
            return red ? 15 : 0;
        }

        @Override
        protected net.minecraft.block.state.BlockStateContainer createBlockState() {
            return new net.minecraft.block.state.BlockStateContainer( this, new IProperty[]{ FACING } );
        }

        @Override
        public boolean canConnectRedstone( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side ) {
            return true;
        }
    }
}
