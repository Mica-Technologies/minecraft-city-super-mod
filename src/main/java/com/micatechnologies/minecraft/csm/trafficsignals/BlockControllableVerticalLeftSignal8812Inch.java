package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.tabs.CsmTabTrafficSignals;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockControllableVerticalLeftSignal8812Inch extends AbstractBlockControllableSignal
{
    public BlockControllableVerticalLeftSignal8812Inch() {
        super( Material.ROCK );
    }

    @Override
    public SIGNAL_SIDE getSignalSide( World world, BlockPos blockPos ) {
        return SIGNAL_SIDE.LEFT;
    }

    @Override
    public boolean doesFlash() {
        return true;
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
        return "controllableverticalleftsignal8812inch";
    }
}
