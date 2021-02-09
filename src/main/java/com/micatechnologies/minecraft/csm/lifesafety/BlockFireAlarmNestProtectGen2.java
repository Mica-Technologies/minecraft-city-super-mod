package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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
public class BlockFireAlarmNestProtectGen2 extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:nestprotect" )
    public static final Block block = null;

    public BlockFireAlarmNestProtectGen2( ElementsCitySuperMod instance ) {
        super( instance, 50 );
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
                                                    new ModelResourceLocation( "csm:nestprotect", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockDirectional.FACING;
        private             boolean           red    = false;

        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "nestprotect" );
            setUnlocalizedName( "nestprotect" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0.1F );
            setLightOpacity( 0 );
            setCreativeTab( TabFireAlarms.tab );
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
                    return new AxisAlignedBB( 1D, 0D, 0.1D, 0D, 1D, 0D );
                case NORTH:
                    return new AxisAlignedBB( 0D, 0D, 0.9D, 1D, 1D, 1D );
                case WEST:
                    return new AxisAlignedBB( 0.9D, 0D, 1D, 1D, 1D, 0D );
                case EAST:
                    return new AxisAlignedBB( 0.1D, 0D, 0D, 0D, 1D, 1D );
                case UP:
                    return new AxisAlignedBB( 0D, 0.1D, 0D, 1D, 0D, 1D );
                case DOWN:
                    return new AxisAlignedBB( 0D, 0.9D, 1D, 1D, 1D, 0D );
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
                                (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:nest_test")),
                                SoundCategory.NEUTRAL, (float) 5, (float) 1);
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
                                    (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:nest_test")),
                                    SoundCategory.NEUTRAL, (float) 5, (float) 1);
                }
            }
        }

        @Override
        public int tickRate( World world ) {
            return 140;
        }

        @Override
        public void onBlockAdded( World world, BlockPos pos, IBlockState state ) {
            super.onBlockAdded( world, pos, state );
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            Block block = this;
            world.scheduleUpdate( new BlockPos( x, y, z ), this, this.tickRate( world ) );
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
                            (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:nest_test")),
                            SoundCategory.NEUTRAL, (float) 5, (float) 1);
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
