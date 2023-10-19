package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class LangFileSortTool
{
    private static final String LANG_FILE_FOLDER_PATH_RELATIVE = "src/main/resources/assets/csm/lang";
    private static final String LANG_FILE_EXTENSION            = ".lang";

    public static void main( String[] args ) {

        CsmToolUtility.doToolExecuteWrapped( "CSM Lang File Sorting Tool", args, ( devEnvironmentPath ) -> {
            // Sort lang files
            sortLangFiles( devEnvironmentPath, LANG_FILE_FOLDER_PATH_RELATIVE );
        } );
    }

    public static void sortLangFiles( File devEnvironmentPath, String langFileFolderPathRelative ) throws Exception {
        // Get lang file folder path
        File langFileFolderPath = new File( devEnvironmentPath, langFileFolderPathRelative );

        // Get lang file folder path
        File[] langFiles = langFileFolderPath.listFiles( ( dir, name ) -> name.endsWith( LANG_FILE_EXTENSION ) );

        // Sort each lang file
        for ( File langFile : langFiles ) {
            sortLangFile( langFile );
        }
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