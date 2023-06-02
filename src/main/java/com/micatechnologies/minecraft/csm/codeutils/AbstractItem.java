package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.Item;

/**
 * Abstract item class which provides common methods and properties for all items in this mod.
 *
 * @version 1.0
 * @see Item
 * @since 2023.3
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractItem extends Item implements IHasModel
{
    /**
     * Constructs an AbstractItem.
     *
     * @param maxDamage    The maximum damage of the item.
     * @param maxStackSize The maximum stack size of the item.
     *
     * @since 1.0
     */
    public AbstractItem( int maxDamage, int maxStackSize )
    {
        setUnlocalizedName( getItemRegistryName() );
        setRegistryName( CsmConstants.MOD_NAMESPACE, getItemRegistryName() );
        setMaxDamage( maxDamage );
        setMaxStackSize( maxStackSize );
        CsmRegistry.registerItem( this );
    }

    /**
     * Retrieves the registry name of the item.
     *
     * @return The registry name of the item.
     *
     * @since 1.0
     */
    public abstract String getItemRegistryName();

    /**
     * Registers the block's model.
     *
     * @since 1.0
     */
    @Override
    public void registerModels() {
        Csm.proxy.setCustomModelResourceLocation( this, 0, "inventory" );
    }
}
