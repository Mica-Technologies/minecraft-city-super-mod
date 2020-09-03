package com.micatechnologies.minecraft.csm.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class AbstractBlockControllableCrosswalkAccessory extends AbstractBlockControllableSignal
{
    public AbstractBlockControllableCrosswalkAccessory( Material p_i46399_1_, MapColor p_i46399_2_ ) {
        super( p_i46399_1_, p_i46399_2_ );
    }

    public AbstractBlockControllableCrosswalkAccessory( Material p_i45394_1_ ) {
        super( p_i45394_1_ );
    }

    @Override
    public SIGNAL_SIDE getSignalSide() {
        return SIGNAL_SIDE.CROSSWALK;
    }

}