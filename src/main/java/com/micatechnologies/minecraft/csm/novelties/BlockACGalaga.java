package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockACGalaga extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:acgalaga" )
    public static final Block block = null;

    public BlockACGalaga( ElementsCitySuperMod instance ) {
        super( instance, 1104 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "acgalaga" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:acgalaga", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockDirectional.FACING;

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "acgalaga" );
            setSoundType( SoundType.STONE );
            setHardness( 1F );
            setResistance( 10F );
            setLightLevel( 1F );
            setLightOpacity( 0 );
            setCreativeTab( TabNovelties.tab );
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
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
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
            world.playSound((EntityPlayer) null, x, y, z,
                            (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation( "csm:galaga_cabinet")),
                            SoundCategory.NEUTRAL, (float) 1, (float) 1);
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
