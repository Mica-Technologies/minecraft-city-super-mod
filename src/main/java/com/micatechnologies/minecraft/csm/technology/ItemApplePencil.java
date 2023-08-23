package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.codeutils.AbstractItemSpade;
import com.micatechnologies.minecraft.csm.tabs.CsmTabTechnology;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Set;

public class ItemApplePencil extends AbstractItemSpade
{
    public ItemApplePencil() {
        super( 0, 64, EnumHelper.addToolMaterial( "APPLEPENCIL", 1, 100, 4f, 1f, 2 ) );
        this.attackSpeed = -1.2f;
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
        return "applepencil";
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public Set< String > getToolClasses( ItemStack stack ) {
        HashMap< String, Integer > ret = new HashMap<>();
        ret.put( "spade", 1 );
        return ret.keySet();
    }
}
