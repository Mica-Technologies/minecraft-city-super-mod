package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class providing a clean, friendly interface for the creation of {@link CreativeTabs}.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.3
 */
public abstract class CsmTab
{
    /**
     * The count of items in the stack used for the tab icon.
     *
     * @since 1.0
     */
    private static final int TAB_ICON_STACK_ITEM_COUNT = 1;

    /**
     * The map of all {@link CsmTab} instances created.
     *
     * @since 1.0
     */
    private static final Map< Class< ? extends CsmTab >, CsmTab > TABS = new HashMap<>();

    /**
     * The underlying creative tab instance being created and interfaced.
     *
     * @since 1.0
     */
    private final CreativeTabs tab;

    /**
     * Constructor for the {@link CsmTab} which creates the creative tab using the tab ID, tab icon, and tab searchable
     * property return from the respective abstract method implementations.
     *
     * @since 1.0
     */
    public CsmTab() {
        if ( !getTabHidden() ) {
            tab = new CreativeTabs( getTabId() )
            {
                /**
                 * Creative tab implementation method for getting the tab icon item.
                 *
                 * @return an {@link ItemStack} to display as the tab icon item
                 */
                @SideOnly( Side.CLIENT )
                @Override
                @Nonnull
                public ItemStack getTabIconItem() {
                    return new ItemStack( getTabIcon(), TAB_ICON_STACK_ITEM_COUNT );
                }

                /**
                 * Creative tab implementation method for getting if the tab is searchable.
                 *
                 * @return {@code true} if the tab is searchable (has a search bar),
                 *         {@code false} otherwise
                 */
                @SideOnly( Side.CLIENT )
                public boolean hasSearchBar() {
                    return getTabSearchable();
                }
            };
        }
        else {
            tab = null;
        }
    }

    /**
     * Gets the underlying {@link CreativeTabs} instance being created and interfaced.
     *
     * @return the underlying {@link CreativeTabs} instance being created and interfaced
     *
     * @since 1.0
     */
    public CreativeTabs getTab() {
        return tab;
    }

    /**
     * Gets the ID (unique identifier) of the tab.
     *
     * @return the ID of the tab
     *
     * @since 1.0
     */
    public abstract String getTabId();

    /**
     * Gets the block to use as the icon of the tab
     *
     * @return the block to use as the icon of the tab
     *
     * @since 1.0
     */
    public abstract Block getTabIcon();

    /**
     * Gets a boolean indicating if the tab is searchable (has its own search bar).
     *
     * @return {@code true} if the tab is searchable, otherwise {@code false}
     *
     * @since 1.0
     */
    public abstract boolean getTabSearchable();

    /**
     * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
     *
     * @return {@code true} if the tab is hidden, otherwise {@code false}
     *
     * @since 1.0
     */
    public abstract boolean getTabHidden();

    /**
     * Initializes all the elements belonging to the tab.
     *
     * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
     *
     * @since 1.0
     */
    public abstract void initTabElements( FMLPreInitializationEvent fmlPreInitializationEvent );

    /**
     * Initializes the element with the specified class.
     *
     * @param entryClass                the class of the element to initialize
     * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
     *
     * @return the initialized element
     *
     * @since 1.0
     */
    private Object initTabElement( Class< ? > entryClass, FMLPreInitializationEvent fmlPreInitializationEvent ) {
        try {
            return entryClass.newInstance();
        }
        catch ( Exception e ) {
            fmlPreInitializationEvent.getModLog().error( "Error initializing tab element: " + entryClass.getName(), e );
        }
        return null;
    }

    /**
     * Initializes the block with the specified class.
     *
     * @param blockClass                the class of the block to initialize
     * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
     *
     * @since 1.0
     */
    public Block initTabBlock( Class< ? extends Block > blockClass,
                               FMLPreInitializationEvent fmlPreInitializationEvent )
    {
        Block block = null;
        Object entry = initTabElement( blockClass, fmlPreInitializationEvent );
        if ( entry != null ) {
            block = blockClass.cast( entry ).setCreativeTab( tab );
        }
        return block;
    }

    /**
     * Initializes the item with the specified class.
     *
     * @param itemClass                 the class of the item to initialize
     * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
     *
     * @since 1.0
     */
    public Item initTabItem( Class< ? extends Item > itemClass,
                             FMLPreInitializationEvent fmlPreInitializationEvent )
    {
        Item item = null;
        Object entry = initTabElement( itemClass, fmlPreInitializationEvent );
        if ( entry != null ) {
            item = itemClass.cast( entry ).setCreativeTab( tab );
        }
        return item;
    }

    /**
     * Initializes all of the {@link CsmTab} implementations found in the {@link ASMDataTable} of the
     * {@link FMLPreInitializationEvent} during mod startup.
     *
     * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} to get the {@link ASMDataTable} from
     *                                  containing the {@link CsmTab} implementations
     *
     * @throws Exception if an error occurs during initialization
     * @since 1.0
     */
    public static void initTabs( FMLPreInitializationEvent fmlPreInitializationEvent ) throws Exception {
        for ( ASMDataTable.ASMData asmData : fmlPreInitializationEvent.getAsmData()
                                                                      .getAll( CsmTab.Load.class.getName() ) ) {
            Class< ? > clazz = Class.forName( asmData.getClassName() );
            if ( clazz.getSuperclass() == CsmTab.class ) {
                Class< ? extends CsmTab > csmTabClass = clazz.asSubclass( CsmTab.class );
                CsmTab tab = csmTabClass.newInstance();
                TABS.put( csmTabClass, tab );
                tab.initTabElements( fmlPreInitializationEvent );
            }
        }
    }

    /**
     * Gets the {@link CreativeTabs} instance for the {@link CsmTab} implementation class.
     *
     * @param clazz the {@link CsmTab} implementation class to get the {@link CreativeTabs} instance for
     *
     * @return the {@link CreativeTabs} instance for the {@link CsmTab} implementation class
     *
     * @since 1.0
     */
    public static CreativeTabs get( Class< ? extends CsmTab > clazz ) {
        return TABS.get( clazz ).getTab();
    }

    /**
     * The annotation interface used to identify/indicate implementations of {@link CsmTab} which shall be
     * loaded/enabled.
     *
     * @since 1.0
     */
    @Retention( RetentionPolicy.RUNTIME )
    @Target( { ElementType.TYPE } )
    public @interface Load
    {
    }
}
