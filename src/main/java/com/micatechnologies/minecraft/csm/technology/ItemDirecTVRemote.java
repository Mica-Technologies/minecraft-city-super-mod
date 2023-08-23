package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.tabs.CsmTabTechnology;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemDirecTVRemote extends AbstractItem
{

    @Override
    public void addInformation( ItemStack itemstack, World world, List< String > list, ITooltipFlag flag ) {
        super.addInformation( itemstack, world, list, flag );
        list.add( "This remote does nothing and is only for looks!" );
    }

    /**
     * Retrieves the registry name of the item.
     *
     * @return The registry name of the item.
     *
     * @since 1.0
     */
    @Override
    public String getItemRegistryName() {
        return "directvremote";
    }
}

