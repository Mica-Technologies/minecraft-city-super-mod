package com.micatechnologies.minecraft.csm.tools.tool_framework;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One logical asset folder, spread across the source trees that hold a copy of it.
 *
 * <p>Every module writes into the same {@code assets/csm} domain, so what used to be a single
 * folder such as {@code assets/csm/models/block} is now up to ten folders that the game merges.
 * This class is what a tool holds instead of a {@link File} for one of those: it resolves a name
 * to whichever tree actually has the file, and it lists or walks the union.
 *
 * <p>{@link #file(String)} never returns null. When no tree has the name, it returns the path the
 * file would have in the first tree, so an error message still names something recognisable and
 * {@code File.exists()} still answers the question the caller was asking.
 *
 * @version 1.0
 * @since 2025.9.3
 */
public class AssetFolder
{
    /**
     * The folders that make up this one, in tree order with Core first.
     *
     * @since 1.0
     */
    private final List< File > dirs;

    /**
     * Where this folder would be created if no tree has it, so {@link #file(String)} can still
     * name a path when the folder is missing everywhere.
     *
     * @since 1.0
     */
    private final File fallback;

    /**
     * Creates an asset folder from the specified folders.
     *
     * @param dirs     the folders that exist, in tree order
     * @param fallback the path to name when {@code dirs} is empty
     *
     * @since 1.0
     */
    public AssetFolder( List< File > dirs, File fallback )
    {
        this.dirs = new ArrayList<>( dirs );
        this.fallback = fallback;
    }

    /**
     * Returns the folder at the specified path below {@code assets/csm}, across every tree.
     *
     * @param layout       the layout
     * @param relativePath the path below {@code assets/csm}, e.g. {@code models/block}
     *
     * @return the asset folder
     *
     * @since 1.0
     */
    public static AssetFolder ofAsset( CsmLayout layout, String relativePath )
    {
        return new AssetFolder( layout.assetDirs( relativePath ),
                                new File( new File( layout.repoRoot(),
                                                    "src/main/resources/" + CsmLayout.ASSETS_CSM ),
                                          relativePath ) );
    }

    /**
     * Returns the folder at the specified path below each tree's
     * {@code src/main/java/com/micatechnologies/minecraft/csm}.
     *
     * @param layout       the layout
     * @param relativePath the path below the CSM package root, e.g. {@code tabs}
     *
     * @return the source folder
     *
     * @since 1.0
     */
    public static AssetFolder ofSource( CsmLayout layout, String relativePath )
    {
        List< File > dirs = new ArrayList<>();
        for ( File found : layout.resolveSourceAll( relativePath ) ) {
            if ( found.isDirectory() ) {
                dirs.add( found );
            }
        }
        return new AssetFolder( dirs,
                                new File( new File( layout.repoRoot(),
                                                    "src/main/java/" + CsmLayout.JAVA_PACKAGE ),
                                          relativePath ) );
    }

    /**
     * Returns the folders that make up this one, Core first.
     *
     * @return the folders
     *
     * @since 1.0
     */
    public List< File > dirs()
    {
        return Collections.unmodifiableList( dirs );
    }

    /**
     * Returns whether no tree has this folder.
     *
     * @return true if no tree has this folder
     *
     * @since 1.0
     */
    public boolean isEmpty()
    {
        return dirs.isEmpty();
    }

    /**
     * Returns the named file from whichever tree has it.
     *
     * @param name the file name, which may contain {@code /} for a file in a subfolder
     *
     * @return the file in the tree that has it, or where it would be in the first tree
     *
     * @since 1.0
     */
    public File file( String name )
    {
        for ( File dir : dirs ) {
            File candidate = new File( dir, name );
            if ( candidate.exists() ) {
                return candidate;
            }
        }
        return new File( dirs.isEmpty() ? fallback : dirs.get( 0 ), name );
    }

    /**
     * Returns whether any tree has the named file.
     *
     * @param name the file name
     *
     * @return true if some tree has it
     *
     * @since 1.0
     */
    public boolean has( String name )
    {
        return file( name ).exists();
    }

    /**
     * Returns the immediate contents of every tree's copy of this folder.
     *
     * @return the files and folders directly inside
     *
     * @since 1.0
     */
    public List< File > list()
    {
        List< File > found = new ArrayList<>();
        for ( File dir : dirs ) {
            File[] contents = dir.listFiles();
            if ( contents != null ) {
                found.addAll( List.of( contents ) );
            }
        }
        found.sort( ( a, b ) -> a.getName().compareToIgnoreCase( b.getName() ) );
        return found;
    }

    /**
     * Returns every file below every tree's copy of this folder, at any depth.
     *
     * @param suffix the file name suffix to keep, or "" for every file
     *
     * @return the files
     *
     * @since 1.0
     */
    public List< File > walk( String suffix )
    {
        List< File > found = new ArrayList<>();
        for ( File dir : dirs ) {
            CsmLayout.collectFiles( dir, suffix, found );
        }
        return found;
    }

    /**
     * Returns the names of the immediate contents of this folder, deduplicated across the trees.
     *
     * @return the names
     *
     * @since 1.0
     */
    public Set< String > names()
    {
        Set< String > names = new LinkedHashSet<>();
        for ( File file : list() ) {
            names.add( file.getName() );
        }
        return names;
    }

    /**
     * Returns every tree's copy of this folder as a printable list of paths.
     *
     * @return the paths, comma separated
     *
     * @since 1.0
     */
    @Override
    public String toString()
    {
        return dirs.isEmpty() ? fallback.getPath()
                : dirs.stream().map( File::getPath ).collect( Collectors.joining( ", " ) );
    }
}
