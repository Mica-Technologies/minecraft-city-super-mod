package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.tabs.CsmTabNovelties;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockOldRecordPlayer extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:oldrecordplayer" )
    public static final Block block = null;

    public BlockOldRecordPlayer( ElementsCitySuperMod instance ) {
        super( instance, 1114 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "oldrecordplayer" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:oldrecordplayer", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockDirectional.FACING;

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "oldrecordplayer" );
            setSoundType( SoundType.STONE );
            setHardness( 1F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( CsmTabNovelties.get() );
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
        public IBlockState withRotation( IBlockState state, Rotation rot ) {
            return state.withProperty( FACING, rot.rotate( ( EnumFacing ) state.getValue( FACING ) ) );
        }

        @Override
        public IBlockState withMirror( IBlockState state, Mirror mirrorIn ) {
            return state.withRotation( mirrorIn.toRotation( ( EnumFacing ) state.getValue( FACING ) ) );
        }

        @Override
        public boolean isFullCube( IBlockState state ) {
            return false;
        }

        @Override
        public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
            switch ( ( EnumFacing ) state.getValue( BlockDirectional.FACING ) ) {
                case SOUTH:
                default:
                    return new AxisAlignedBB( 1D, 0D, 1D, 0D, 0.5D, 0D );
                case NORTH:
                    return new AxisAlignedBB( 0D, 0D, 0D, 1D, 0.5D, 1D );
                case WEST:
                    return new AxisAlignedBB( 0D, 0D, 1D, 1D, 0.5D, 0D );
                case EAST:
                    return new AxisAlignedBB( 1D, 0D, 0D, 0D, 0.5D, 1D );
                case UP:
                    return new AxisAlignedBB( 0D, 1D, 0D, 1D, 0D, 0.5D );
                case DOWN:
                    return new AxisAlignedBB( 0D, 0D, 1D, 1D, 1D, 0.5D );
            }
        }

        @Override
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
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
                                         EnumFacing direction,
                                         float hitX,
                                         float hitY,
                                         float hitZ )
        {
            super.onBlockActivated( world, pos, state, entity, hand, direction, hitX, hitY, hitZ );
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            if ((Math.random() < 0.25)) {
                if (entity instanceof EntityPlayer && !world.isRemote) {
                    ((EntityPlayer) entity).sendStatusMessage( new TextComponentString( "OwO what's this record player doing!?"), (true));
                }
                world.playSound((EntityPlayer) null, x, y, z,
                                (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:oldrecordplayer")),
                                SoundCategory.NEUTRAL, (float) 1, (float) 1);
            } else if ((Math.random() < 0.75)) {
                if (entity instanceof EntityPlayer && !world.isRemote) {
                    ((EntityPlayer) entity).sendStatusMessage(new TextComponentString("OwO what's this record player doing!?"), (true));
                }
                world.playSound((EntityPlayer) null, x, y, z,
                                (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:oldrecordplayer2")),
                                SoundCategory.NEUTRAL, (float) 1, (float) 1);
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
            return this.getDefaultState()
                       .withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos, placer ) );
        }

        @Override
        protected net.minecraft.block.state.BlockStateContainer createBlockState() {
            return new net.minecraft.block.state.BlockStateContainer( this, new IProperty[]{ FACING } );
        }
    }
}
