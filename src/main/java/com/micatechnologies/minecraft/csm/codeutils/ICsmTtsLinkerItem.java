package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marks the item that binds speakers to a speech broadcast source — in practice the TTS Linker,
 * which lives in the Text to Speech module.
 *
 * <p>A block whose right-click does something of its own has to stand aside while this item is
 * held, or the linker could never be used on it. The speaker blocks are Technology's and cannot
 * name the linker's class, because Text to Speech already requires Technology and a reference
 * back the other way would be a compile-time cycle between the two jars. Core carries the type
 * they recognise it by instead.</p>
 *
 * <p>There is nothing to implement: the only question ever asked of it is whether the held item
 * is the linker.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public interface ICsmTtsLinkerItem {

}
