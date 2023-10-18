package com.micatechnologies.minecraft.csm;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BatchRenameTool
{
    public static void main( String[] args ) {
        System.out.println( "Running CSM Batch Rename Tool..." );

        // Input folder path (../../../../../../../batchRenameToolInput)
        final String processPath = "E:\\source\\repos\\minecraft-city-super-mod\\dev-env-utils\\batchRenameToolInput";

        // Output folder path (../../../../../../../batchRenameToolOutput)
        final String outputPath = "E:\\source\\repos\\minecraft-city-super-mod\\dev-env-utils\\batchRenameToolOutput";

        // File name replacements
        /*
         * NOTE: Multiple `to`s are supported to allow for multiple replacement variations of the same original file(s).
         *
         * EXAMPLE: Replacing "A" with "B" in file names
         * > FileNameReplacement.from( "A" ).to( "B" );
         *
         * EXAMPLE: Replacing "A" with "B" and "C" in file names, and "X" with "Y" in contents of files renamed with "C"
         * > FileNameReplacement.from( "A" ).to( "B" ).to( "C", FileContentReplacement.from( "X" ).to( "Y" ) );
         *
         * EXAMPLE: Replacing "A" with "B" and "C" in both file names and corresponding contents
         * > FileNameReplacement.from( "A" ).to( "B", true ).to( "C", true );
         */
        final FileNameReplacement[] fileNameReplacements = new FileNameReplacement[]{ FileNameReplacement
                                                                                              .from( "blackmetal" ).to(
                        "bluemetal",
                        FileContentReplacement.from( "blackmetal" ).to( "bluemetal" ),
                        FileContentReplacement.from( "metal_black" ).to( "metal_blue" ) ).to( "redmetal",
                                                                                              FileContentReplacement
                                                                                                      .from( "blackmetal" )
                                                                                                      .to( "redmetal" ),
                                                                                              FileContentReplacement
                                                                                                      .from( "metal_black" )
                                                                                                      .to( "metal_red" ) ).to(
                "greenmetal",
                FileContentReplacement.from( "blackmetal" ).to( "greenmetal" ),
                FileContentReplacement.from( "metal_black" ).to( "metal_green" ) ) };

        // Process folder
        processFolder( processPath, outputPath, fileNameReplacements );

        System.out.println( "Finished Running CSM Batch Rename Tool." );
    }

    private static void processFolder( String processPath,
                                       String outputPath,
                                       FileNameReplacement[] fileNameReplacements,
                                       int depth )
    {
        // Create log buffer string based on depth (for indenting)
        StringBuilder logBuffer = new StringBuilder();
        for ( int i = 0; i < depth; i++ ) {
            logBuffer.append( "  " );
        }
        String logIndent = logBuffer.toString();

        System.out.println( logIndent + "Processing folder: " + processPath );

        try {
            // Create output folder if it doesn't exist
            File outputFolder = new File( outputPath );
            if ( !outputFolder.exists() ) {
                outputFolder.mkdirs();
            }
            else {
                // Delete all files in output folder
                for ( File file : outputFolder.listFiles() ) {
                    if ( file.isFile() && !file.getName().equalsIgnoreCase( ".gitkeep" ) ) {
                        file.delete();
                    }
                }
            }

            // Recursively process all files and folders in the input folder
            File processFolder = new File( processPath );
            for ( File file : processFolder.listFiles() ) {
                if ( file.isDirectory() ) {
                    System.out.println( logIndent + "  Entering directory: " + file.getName() );
                    processFolder( file.getAbsolutePath(), outputPath, fileNameReplacements );
                }
                else {
                    System.out.println( logIndent + "  Processing file: " + file.getName() );

                    // Process file name replacements
                    String fileName = file.getName();
                    for ( FileNameReplacement fileNameReplacement : fileNameReplacements ) {
                        if ( fileName.contains( fileNameReplacement.getOriginalText() ) ) {
                            if ( fileNameReplacement.getReplacementTexts().isEmpty() ) {
                                throw new IllegalArgumentException( "No new/replacement file name text(s) found for " +
                                                                    "original file name text: " +
                                                                    fileNameReplacement.getOriginalText() );
                            }
                            else {
                                for ( String replacementText : fileNameReplacement.getReplacementTexts() ) {
                                    // Create new file + name
                                    File newFile = new File( outputFolder,
                                                             fileName.replaceAll( fileNameReplacement.getOriginalText(),
                                                                                  replacementText ) );
                                    String newFileName = newFile.getName();

                                    // Process file content replacements (if any)
                                    if ( fileNameReplacement.getFileContentReplacements().isEmpty() ||
                                         !fileNameReplacement.getFileContentReplacements()
                                                             .containsKey( replacementText ) ) {
                                        // No file content replacements, just copy the file
                                        FileUtils.copyFile( file, newFile );
                                        System.out.println( logIndent + "    ->" + newFileName + " (copied)" );
                                    }
                                    else {
                                        // Get file contents and replace original text with replacement text
                                        String fileContent = FileUtils.readFileToString( file );
                                        for ( FileContentReplacement fileContentReplacement : fileNameReplacement
                                                .getFileContentReplacements().get( replacementText ) ) {
                                            fileContent
                                                    = fileContent.replaceAll( fileContentReplacement.getOriginalText(),
                                                                              fileContentReplacement.getReplacementText() );
                                        }

                                        // Write file contents to new file
                                        FileUtils.writeStringToFile( newFile, fileContent );
                                        System.out.println( logIndent + "    ->" + newFileName + " (copied+modified)" );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch ( Exception e ) {
            System.err.println( "Failed to process folder: " + processPath );
            e.printStackTrace();
        }

        System.out.println( "Finished processing folder: " + processPath );
    }

    private static void processFolder( String processPath,
                                       String outputPath,
                                       FileNameReplacement[] fileNameReplacements )
    {
        processFolder( processPath, outputPath, fileNameReplacements, 0 );
    }

    private static class FileNameReplacement
    {
        protected final String                                             originalText;
        protected final ArrayList< String >                                replacementTexts        = new ArrayList<>();
        protected final Map< String, ArrayList< FileContentReplacement > > fileContentReplacements = new HashMap<>();

        private FileNameReplacement( String originalText ) {
            this.originalText = originalText;
        }

        public static FileNameReplacement from( String originalText ) {
            return new FileNameReplacement( originalText );
        }

        public FileNameReplacement to( String replacementText, boolean replaceInFileContents ) {
            if ( replaceInFileContents ) {
                return to( replacementText, FileContentReplacement.from( originalText ).to( replacementText ) );
            }
            else {
                return to( replacementText );
            }
        }

        public FileNameReplacement to( String replacementText ) {
            checkReplacementTextExists( replacementText );
            this.replacementTexts.add( replacementText );
            return this;
        }

        public FileNameReplacement to( String replacementText, FileContentReplacement... fileContentReplacements ) {
            checkReplacementTextExists( replacementText );
            this.replacementTexts.add( replacementText );
            for ( FileContentReplacement fileContentReplacement : fileContentReplacements ) {
                if ( !this.fileContentReplacements.containsKey( replacementText ) ) {
                    this.fileContentReplacements.put( replacementText, new ArrayList<>() );
                }
                this.fileContentReplacements.get( replacementText ).add( fileContentReplacement );
            }
            return this;
        }

        private void checkReplacementTextExists( String replacementText ) {
            if ( this.replacementTexts.contains( replacementText ) ) {
                throw new IllegalArgumentException( "Duplicate replacement text: " +
                                                    replacementText +
                                                    " for original text: " +
                                                    originalText );
            }
        }

        public String getOriginalText() {
            return originalText;
        }

        public ArrayList< String > getReplacementTexts() {
            return replacementTexts;
        }

        public Map< String, ArrayList< FileContentReplacement > > getFileContentReplacements() {
            return fileContentReplacements;
        }
    }

    private static class FileContentReplacement
    {
        private final String originalText;
        private       String replacementText;

        private FileContentReplacement( String originalText ) {
            this.originalText = originalText;
            this.replacementText = originalText;
        }

        public static FileContentReplacement from( String originalText ) {
            return new FileContentReplacement( originalText );
        }

        public FileContentReplacement to( String replacementText ) {
            this.replacementText = replacementText;
            return this;
        }

        public String getOriginalText() {
            return originalText;
        }

        public String getReplacementText() {
            return replacementText;
        }
    }
}
