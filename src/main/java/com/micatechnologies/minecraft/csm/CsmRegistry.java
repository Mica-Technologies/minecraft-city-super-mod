package com.micatechnologies.minecraft.csm;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

/**
 * Registry utility class for the City Super Mod.
 *
 * @version 1.0
 * @since 2.0
 * @since 2023.2.1
 */
public class CsmRegistry
{
    /**
     * The map of blocks registered with the mod.
     *
     * @since 1.0
     */
    private static final Map< String, Block > BLOCKS = new HashMap<>();

    /**
     * The list of items registered with the mod.
     *
     * @since 1.0
     */
    private static final List< Item > ITEMS = new ArrayList<>();

    /**
     * The next network message ID to use. This value is incremented each time a new network message is registered.
     *
     * @since 1.0
     */
    private static int nextNetworkMessageId = 0;

    /**
     * Returns the map of blocks registered with the mod.
     *
     * @return The map of blocks registered with the mod.
     *
     * @since 1.0
     */
    public static Map< String, Block > getBlocksMap() {
        return BLOCKS;
    }

    /**
     * Returns the list of blocks registered with the mod.
     *
     * @return The list of blocks registered with the mod.
     *
     * @since 1.0
     */
    public static Collection< Block > getBlocks() {
        return BLOCKS.values();
    }

    /**
     * Returns the block registered with the mod that is of the specified class.
     *
     * @param blockId The ID of the block to return.
     *
     * @return The block registered with the mod that is of the specified class.
     *
     * @since 1.0
     */
    public static Block getBlock( String blockId ) {
        Block block = BLOCKS.get( CsmConstants.MOD_NAMESPACE + ":" + blockId );
        if ( block == null ) {
            block = BLOCKS.get( blockId );
        }
        return block;
    }

    /**
     * Returns the list of items registered with the mod.
     *
     * @return The list of items registered with the mod.
     *
     * @since 1.0
     */
    public static Collection< Item > getItems() {
        return ITEMS;
    }

    /**
     * Registers a block with the mod.
     *
     * @param block The block to register.
     *
     * @since 1.0
     */
    public static void registerBlock( Block block ) {
        // Use the block's registry name as the key
        String key = block.getRegistryName() != null ? block.getRegistryName().toString() : block.getUnlocalizedName();

        // Check if the block is already registered.
        if ( BLOCKS.containsKey( key ) ) {
            throw new IllegalArgumentException( "Block with registry name " + key + " already registered." );
        }

        BLOCKS.put( key, block );
    }

    /**
     * Registers an item with the mod.
     *
     * @param item The item to register.
     *
     * @since 1.0
     */
    public static void registerItem( Item item ) {
        // Check if the item is already registered.
        if ( ITEMS.contains( item ) ) {
            throw new IllegalArgumentException( "Item " + item.getRegistryName() + " already registered." );
        }

        ITEMS.add( item );
    }

    /**
     * Registers a network message with the mod.
     *
     * @param handler      The message handler.
     * @param messageClass The message class.
     * @param sides        The sides to register the message for.
     * @param <T>          The message type.
     * @param <V>          The reply type.
     *
     * @since 1.0
     */
    public static < T extends IMessage, V extends IMessage > void registerNetworkMessage( Class< ? extends IMessageHandler< T, V > > handler,
                                                                                          Class< T > messageClass,
                                                                                          Side... sides )
    {
        for ( Side side : sides ) {
            Csm.PACKET_HANDLER.registerMessage( handler, messageClass, nextNetworkMessageId, side );
        }
        nextNetworkMessageId++;
    }
}
