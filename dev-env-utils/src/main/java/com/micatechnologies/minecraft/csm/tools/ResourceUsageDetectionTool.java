package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ResourceUsageDetectionTool
{
    private static final String   RESOURCE_FOLDER_PATH_RELATIVE   = "src/main/resources/assets/csm";
    private static final String   CODE_FOLDER_PATH_RELATIVE       = "src/main/java/com/micatechnologies/minecraft/csm";
    private static final String   SOUNDS_JAVA_CLASS_PATH_RELATIVE = CODE_FOLDER_PATH_RELATIVE + "/CsmSounds.java";
    private static final String[] RESOURCE_TYPE_NAMES             = { "blockstate",
                                                                      "model",
                                                                      "texture",
                                                                      "sound",
                                                                      "lang" };

    public static void main( String[] args ) {

        CsmToolUtility.doToolExecuteWrapped( "CSM Resource Usage Detection Tool", args, ( devEnvironmentPath ) -> {
            // Get the first resource type from the user
            int resourceTypeFromUserInput = getResourceTypeFromUserInput();

            // Loop while user input is not empty
            while ( resourceTypeFromUserInput > 0 && resourceTypeFromUserInput <= RESOURCE_TYPE_NAMES.length ) {
                // Get the resource name from the user
                String resourceNameFromUserInput = getResourceNameFromUserInput( resourceTypeFromUserInput );

                // Check for resource usage
                checkForResourceUsage( devEnvironmentPath, RESOURCE_TYPE_NAMES[ resourceTypeFromUserInput - 1 ],
                                       resourceNameFromUserInput );

                // Get the next resource type from the user
                resourceTypeFromUserInput = getResourceTypeFromUserInput();
            }
        } );
    }

    private static void checkForResourceUsage( File devEnvironmentPath,
                                               String resourceTypeFromUserInput,
                                               String resourceNameFromUserInput )
    {
        // Get the resource folder
        File resourceFolder = new File( devEnvironmentPath, RESOURCE_FOLDER_PATH_RELATIVE );

        // Check for resource usage based on type
        switch ( resourceTypeFromUserInput ) {
            case "blockstate":
                // Check for matching blockstate and lang resource name pair
                File blockstateFile = new File( resourceFolder, "blockstates/" + resourceNameFromUserInput + ".json" );
                File langFile = new File( resourceFolder, "lang/en_us.lang" );
                if ( blockstateFile.exists() && containsResourceName( langFile, resourceNameFromUserInput ) ) {
                    System.out.println(
                            "Blockstate and lang resource name pair exists for: " + resourceNameFromUserInput );
                }
                else {
                    System.out.println( "Missing blockstate or lang resource for: " + resourceNameFromUserInput );
                }
                break;
            case "model":
                // Check for usage in blockstates and other models
                File modelFile = new File( resourceFolder, "models/block/" + resourceNameFromUserInput + ".json" );
                if ( modelFile.exists() ) {
                    // TODO: Further checks in blockstates and other models
                }
                else {
                    System.out.println( "Model resource not found for: " + resourceNameFromUserInput );
                }
                break;
            case "texture":
                // Check for usage in models
                // TODO: Implement texture usage check in models
                break;
            case "sound":
                // Check for usage in sounds.json
                File soundsJsonFile = new File( resourceFolder, "sounds.json" );
                if ( containsResourceName( soundsJsonFile, resourceNameFromUserInput ) ) {
                    System.out.println( "Sound resource is used in sounds.json for: " + resourceNameFromUserInput );
                }
                else {
                    System.out.println( "Sound resource not found in sounds.json for: " + resourceNameFromUserInput );
                }
                break;
            case "lang":
                // Check for matching lang resource name
                File specificLangFile = new File( resourceFolder, "lang/" + resourceNameFromUserInput + ".lang" );
                if ( specificLangFile.exists() ) {
                    System.out.println( "Lang resource exists for: " + resourceNameFromUserInput );
                }
                else {
                    System.out.println( "Lang resource not found for: " + resourceNameFromUserInput );
                }
                break;
            default:
                System.out.println( "Unknown resource type: " + resourceTypeFromUserInput );
                break;
        }
    }

    private static boolean containsResourceName( File file, String resourceName ) {
        // Check if the file contains the specified resource name
        try {
            Scanner scanner = new Scanner( file );
            while ( scanner.hasNextLine() ) {
                String line = scanner.nextLine();
                if ( line.contains( resourceName ) ) {
                    scanner.close();
                    return true;
                }
            }
            scanner.close();
        }
        catch ( FileNotFoundException e ) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getResourceNameFromUserInput( int resourceTypeFromUserInput ) {
        return JOptionPane.showInputDialog( null, "Enter the name of the " +
                RESOURCE_TYPE_NAMES[ resourceTypeFromUserInput - 1 ] +
                " resource to check for usage:", "Resource Name Input", JOptionPane.QUESTION_MESSAGE );
    }

    private static int getResourceTypeFromUserInput() {
        String promptMessage = "Enter the resource type:\n";
        for ( int i = 0; i < RESOURCE_TYPE_NAMES.length; i++ ) {
            promptMessage += ( i + 1 ) + " for " + RESOURCE_TYPE_NAMES[ i ] + ", ";
        }
        promptMessage = promptMessage.substring( 0, promptMessage.length() - 2 ) +
                "."; // Remove the trailing comma and space

        while ( true ) {
            try {
                String inputString = JOptionPane.showInputDialog( null, promptMessage, "Resource Type Input",
                                                                  JOptionPane.QUESTION_MESSAGE );

                // If the user closes the dialog or clicks Cancel
                if ( inputString == null ) {
                    return -1;
                }

                int input = Integer.parseInt( inputString );
                if ( input > 0 && input <= RESOURCE_TYPE_NAMES.length ) {
                    return input;
                }
                else {
                    JOptionPane.showMessageDialog( null, "Invalid input. Please enter a number between 1 and " +
                            RESOURCE_TYPE_NAMES.length +
                            ".", "Error", JOptionPane.ERROR_MESSAGE );
                }
            }
            catch ( NumberFormatException e ) {
                JOptionPane.showMessageDialog( null, "Please enter a valid integer.", "Error",
                                               JOptionPane.ERROR_MESSAGE );
            }
        }
    }
}