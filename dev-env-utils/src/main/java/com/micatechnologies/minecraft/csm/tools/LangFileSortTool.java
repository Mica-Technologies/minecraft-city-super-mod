package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmLayout;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class LangFileSortTool
{
    private static final String LANG_FILE_EXTENSION = ".lang";

    public static void main( String[] args ) {

        CsmToolUtility.doToolExecuteWrapped( "CSM Lang File Sorting Tool", args, ( devEnvironmentPath ) -> {
            // Sort lang files
            sortLangFiles( devEnvironmentPath );
        } );
    }

    /**
     * Sorts every lang file in the repository, each in place.
     *
     * <p>Core and every module ship their own share of each locale's lang file under the same
     * {@code assets/csm/lang} path, and the game merges them. Sorting only Core's would leave the
     * other nine unsorted and would say nothing about it.
     *
     * @param devEnvironmentPath the development environment root
     *
     * @throws Exception if a lang file could not be read or written
     */
    public static void sortLangFiles( File devEnvironmentPath ) throws Exception {
        CsmLayout layout = new CsmLayout( devEnvironmentPath );
        List< File > langFiles = layout.allLangFiles();
        if ( langFiles.isEmpty() ) {
            System.err.println( "No " + LANG_FILE_EXTENSION + " files found in any source tree." );
            return;
        }
        for ( File langFile : langFiles ) {
            sortLangFile( langFile );
            System.out.println( "  Sorted " + langFile.getPath() );
        }
        System.out.println( "Sorted " + langFiles.size() + " lang file(s) across "
                                    + layout.assetDirs( "lang" ).size() + " source tree(s)." );
    }

    public static void sortLangFile( File langFile ) throws Exception {

        // Read input lang file into memory (as lines)
        List< String > lines = Files.readAllLines( langFile.toPath() );

        // Note initial line count
        int initialLineCount = lines.size();

        // Sort lines (alphabetically)
        Collections.sort( lines );

        // Write sorted lines to output lang file
        Files.write( langFile.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING );

        // Note final line count
        int finalLineCount = lines.size();

        // Verify that the initial line count matches the final line count
        if ( initialLineCount != finalLineCount ) {
            throw new IllegalStateException(
                    "Initial line count " + initialLineCount + " does not match final line count " + finalLineCount );
        }
    }
}
