package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods for serialization and deserialization of Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2022.1.0
 */
public class SerializationUtils
{
    //region: Static Methods

    /**
     * Gets an array of serialized {@link BlockPos} int values from the given {@link List} of {@link BlockPos}es.
     *
     * @param list The {@link List} of {@link BlockPos}es to serialize.
     *
     * @return An array of serialized {@link BlockPos} int values from the given {@link List} of {@link BlockPos}es.
     *
     * @since 1.0
     */
    public static int[] getBlockPosIntArrayFromList( List< BlockPos > list ) {
        return list.stream().mapToInt( ( BlockPos pos ) -> ( int ) pos.toLong() ).toArray();
    }

    /**
     * Gets a {@link List} of {@link BlockPos}es from the given array of serialized {@link BlockPos} int values.
     *
     * @param array The array of serialized {@link BlockPos} int values to deserialize.
     *
     * @return A {@link List} of {@link BlockPos}es from the given array of serialized {@link BlockPos} int values.
     *
     * @since 1.0
     */
    public static List< BlockPos > getBlockPosListFromIntArray( int[] array ) {
        return Arrays.stream( array ).mapToObj( BlockPos::fromLong ).collect( Collectors.toList() );
    }

    //endregion
}
