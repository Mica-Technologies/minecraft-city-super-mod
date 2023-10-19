package com.micatechnologies.minecraft.csm.tools.tool_framework;

import java.io.File;

/**
 * The {@link CsmToolUtility} class provides utility methods for the CSM development tools (dev-env-utils).
 *
 * @version 1.0
 */
public class CsmToolUtility
{

    /**
     * The log message to print when the tool arguments count is invalid.
     *
     * @since 1.0
     */
    private static final String INVALID_TOOL_ARGS_COUNT_LOG_MESSAGE
            = "Invalid tool arguments. Expected 1 argument containing the development environment path.";

    /**
     * The log message to print when the tool arguments development environment folder specified is invalid.
     *
     * @since 1.0
     */
    private static final String INVALID_TOOL_ARGS_PATH_LOG_MESSAGE
            = "Invalid tool arguments. Development environment path does not exist or is not a folder.";

    /**
     * Executes the specified {@link CsmToolRunnable} as a tool, checking that the required arguments are included and
     * valid.The tool executes wrapped in a try-catch block. If an exception occurs while executing the specified
     * {@link CsmToolRunnable}, an error message is printed to the console.
     *
     * @param toolName the name of the tool being run (for logging purposes)
     * @param toolArgs the tool arguments
     * @param toolExec the tool executable to run
     *
     * @since 1.0
     */
    public static void doToolExecuteWrapped( String toolName, String[] toolArgs, CsmToolRunnable toolExec ) {
        // Validate tool arguments and get dev environment path
        File validToolArgDevEnvPath = validateToolArgs( toolArgs );

        // Execute tool
        if ( validToolArgDevEnvPath != null ) {
            doExecuteWrapped( toolName, validToolArgDevEnvPath, toolExec );
        }
        else {
            System.err.println( "Unable to start " + toolName + " due to invalid tool arguments." );
        }
    }

    /**
     * Validates the specified tool arguments, which are the arguments passed to the main method of a CSM development
     * tool. If the specified tool arguments are invalid, an error message is printed to the console.
     *
     * @param args the tool arguments to validate
     *
     * @return the resulting {@link File} object representing the development environment folder specified in the tool
     *         arguments, or null if the tool arguments are invalid.
     *
     * @since 1.0
     */
    public static File validateToolArgs( String[] args ) {
        // Validate tool arguments
        boolean valid = args.length == 1;

        // Try to parse first tool argument as environment path
        File environmentPathFile = null;
        if ( valid ) {
            String environmentPath = getPlatformIndependentPath( args[ 0 ] );
            environmentPathFile = new File( environmentPath );
            valid = environmentPathFile.exists();
            if ( !valid ) {
                System.err.println( INVALID_TOOL_ARGS_PATH_LOG_MESSAGE );
                environmentPathFile = null;
            }
        }
        else {
            System.err.println( INVALID_TOOL_ARGS_COUNT_LOG_MESSAGE );
        }

        // Return resulting file or null
        return environmentPathFile;
    }

    /**
     * Returns the specified path with all backslashes replaced with forward slashes.
     *
     * @param path the path to convert
     *
     * @return the specified path with all backslashes replaced with forward slashes
     *
     * @since 1.0
     */
    public static String getPlatformIndependentPath( String path ) {
        return path.replace( "\\", "/" );
    }

    /**
     * Executes the specified {@link CsmToolRunnable} wrapped in a try-catch block. If an exception occurs while
     * executing the specified {@link CsmToolRunnable}, an error message is printed to the console.
     *
     * @param execName      the name of the executable being run (for logging purposes)
     * @param execPathParam the path parameter to pass to the executable being run
     * @param exec          the executable to run
     *
     * @since 1.0
     */
    public static void doExecuteWrapped( String execName, File execPathParam, CsmToolRunnable exec ) {
        System.out.println( "Running " + execName + "..." );
        try {
            exec.run( execPathParam );
        }
        catch ( Exception e ) {
            System.err.println( "An Error Occurred While Running " + execName + "!" );
            e.printStackTrace();
        }
        System.out.println( "Finished Running " + execName + "." );
    }
}
