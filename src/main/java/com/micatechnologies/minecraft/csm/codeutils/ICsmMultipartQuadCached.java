package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marks a block whose multipart model should keep each state's quads once found
 * ({@link CsmMultipartQuadCache}).
 *
 * <p>Vanilla's multipart model tests every rule of the blockstate against the state on every
 * call, seven calls a block each time a chunk section is rebuilt. That is nothing for a block of
 * a few rules, but the light pole arms' blockstates are hundreds of rules listing up to a few
 * thousand {@code shape} values between them (a cell model and each mount stub is a rule naming
 * the shapes that draw it). A block whose model is such a blockstate implements this, and Core
 * keeps each state's quads the first time it is drawn.</p>
 *
 * <p>Only for a block every part of whose model is the same whatever the position: no weighted
 * random parts, since the cache keeps the first position's pick for every block in that
 * state.</p>
 *
 * @since 2026.11
 */
public interface ICsmMultipartQuadCached {

}
