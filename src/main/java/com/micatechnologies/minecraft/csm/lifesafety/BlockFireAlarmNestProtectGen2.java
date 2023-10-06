package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockFireAlarmNestProtectGen2 extends AbstractBlockRotatableNSEWUD
{
    public BlockFireAlarmNestProtectGen2() {
        super( Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0.1F, 0 );
    }

    /**
     * Retrieves the registry name of the block.
     *
     * @return The registry name of the block.
     *
     * @since 1.0
     */
    @Override
    public String getBlockRegistryName() {
        return "nestprotect";
    }

    /**
     * Retrieves the bounding box of the block.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return new AxisAlignedBB(0D, 0D, 0.9D, 1D, 1D, 1D );
    }

    /**
     * Retrieves whether the block is an opaque cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockIsOpaqueCube( IBlockState state ) {
        return false;
    }

    /**
     * Retrieves whether the block is a full cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is a full cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockIsFullCube( IBlockState state ) {
        return false;
    }

    /**
     * Retrieves whether the block connects to redstone.
     *
     * @param state  the block state
     * @param access the block access
     * @param pos    the block position
     * @param facing the block facing direction
     *
     * @return {@code true} if the block connects to redstone, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockConnectsRedstone( IBlockState state,
                                             IBlockAccess access,
                                             BlockPos pos,
                                             @Nullable EnumFacing facing )
    {
        return true;
    }

    /**
     * Retrieves the block's render layer.
     *
     * @return The block's render layer.
     *
     * @since 1.0
     */
    @Nonnull
    @Override
    public BlockRenderLayer getBlockRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public void neighborChanged( IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos fromPos )
    {
        super.neighborChanged( state, world, pos, neighborBlock, fromPos );
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        Block block = this;
        if ( world.isBlockIndirectlyGettingPowered( new BlockPos( x, y, z ) ) > 0 ) {
            {
                world.playSound( ( EntityPlayer ) null, x, y, z,
                                 ( net.minecraft.util.SoundEvent ) net.minecraft.util.SoundEvent.REGISTRY.getObject(
                                         new ResourceLocation( "csm:nest_test" ) ), SoundCategory.NEUTRAL, ( float ) 5,
                                 ( float ) 1 );
            }
        }
    }

    @SideOnly( Side.CLIENT )
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
        world.playSound( ( EntityPlayer ) null, x, y, z,
                         ( net.minecraft.util.SoundEvent ) net.minecraft.util.SoundEvent.REGISTRY.getObject(
                                 new ResourceLocation( "csm:nest_test" ) ), SoundCategory.NEUTRAL, ( float ) 5,
                         ( float ) 1 );
        return true;
    }
}
