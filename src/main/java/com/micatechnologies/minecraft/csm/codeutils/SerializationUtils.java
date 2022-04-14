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
     * Gets a {@link BlockPos} from the given {@link NBTTagCompound} and {@link String} key. If the
     * {@link NBTTagCompound} is null or does not contain the given key, null is returned.
     *
     * @param compound The {@link NBTTagCompound} to get the {@link BlockPos} from.
     * @param nbtKey   The {@link String} key to get the {@link BlockPos} from.
     *
     * @return A {@link BlockPos} from the given {@link NBTTagCompound} and {@link String} key. If the
     *         {@link NBTTagCompound} is null or does not contain the given key, null is returned.
     *
     * @since 1.0
     */
    public static BlockPos getBlockPosFromNBTOrNull( NBTTagCompound compound, String nbtKey ) {
        BlockPos returnVal = null;
        if ( compound != null && compound.hasKey( nbtKey ) ) {
            returnVal = BlockPos.fromLong( compound.getLong( nbtKey ) );
        }
        return returnVal;
    }

    /**
     * Sets the given {@link BlockPos} in the given {@link NBTTagCompound} with the given {@link String} key. If the
     * {@link BlockPos} is null, the key is removed from the {@link NBTTagCompound}.
     *
     * @param compound The {@link NBTTagCompound} to set the {@link BlockPos} in.
     * @param nbtKey   The {@link String} key to set the {@link BlockPos} in.
     * @param blockPos The {@link BlockPos} to set in the {@link NBTTagCompound}. If null, the key is removed from the
     *                 {@link NBTTagCompound}.
     *
     * @since 1.0
     */
    public static void setBlockPosInNBTOrRemoveIfNull( NBTTagCompound compound, String nbtKey, BlockPos blockPos ) {
        if ( blockPos != null ) {
            compound.setLong( nbtKey, blockPos.toLong() );
        }
        else {
            compound.removeTag( nbtKey );
        }
    }

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
