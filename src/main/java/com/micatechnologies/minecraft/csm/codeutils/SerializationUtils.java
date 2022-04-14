package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for serialization and deserialization of Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.2.0
 */
public class SerializationUtils
{
    //region: Static Methods

    /**
     * Gets a {@link NBTTagLongArray} of serialized {@link BlockPos} long values from the given {@link List} of
     * {@link BlockPos}es.
     *
     * @param list The {@link List} of {@link BlockPos}es to serialize.
     *
     * @return A {@link NBTTagLongArray} of serialized {@link BlockPos} long values from the given {@link List} of
     *         {@link BlockPos}es.
     *
     * @since 1.0
     */
    public static NBTTagLongArray getBlockPosNBTArrayFromBlockPosList( List< BlockPos > list ) {
        // Convert list of block pos to longs
        long[] longs = list.stream().mapToLong( BlockPos::toLong ).toArray();
        return new NBTTagLongArray( longs );
    }

    /**
     * Gets a {@link List} of {@link BlockPos}es from the given {@link NBTBase} containing serialized {@link BlockPos}
     * long values.
     *
     * @param nbtBase The {@link NBTBase} containing serialized {@link BlockPos} long values to deserialize.
     *
     * @return A {@link List} of {@link BlockPos}es from the given {@link NBTBase} containing serialized
     *         {@link BlockPos} long values.
     *
     * @since 1.0
     */
    public static List< BlockPos > getBlockPosListFromBlockPosNBTArray( NBTBase nbtBase ) {
        List< BlockPos > returnList;
        if ( !( nbtBase instanceof NBTTagLongArray ) ) {
            returnList = new ArrayList<>();
        }
        else {
            long[] blockPosLongArray = ObfuscationReflectionHelper.getPrivateValue( NBTTagLongArray.class,
                                                                                    ( NBTTagLongArray ) nbtBase, 0 );
            blockPosLongArray = blockPosLongArray == null ? new long[ 0 ] : blockPosLongArray;
            returnList = new ArrayList<>( blockPosLongArray.length );
            for ( long blockPosLong : blockPosLongArray ) {
                returnList.add( BlockPos.fromLong( blockPosLong ) );
            }
        }
        return returnList;
    }

    //endregion
}
