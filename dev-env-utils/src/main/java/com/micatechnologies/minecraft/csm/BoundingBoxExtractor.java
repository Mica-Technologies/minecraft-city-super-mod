package com.micatechnologies.minecraft.csm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class BoundingBoxExtractor
{

    public static void main( String[] args ) {
        System.out.println( "Running CSM Bounding Box Extractor Tool..." );

        String modelFolderPath = "E:\\source\\repos\\minecraft-city-super-mod\\src\\main\\resources\\assets\\csm" +
                "\\models\\custom";
        String modelBoundingBoxFolderPath
                = "E:\\source\\repos\\minecraft-city-super-mod\\dev-env-utils\\boundingBoxExtractorToolOutput";
        File modelFolder = new File( modelFolderPath );
        File modelBoundingBoxFolder = new File( modelBoundingBoxFolderPath );
        try {
            extractBoundingBoxFromModelsInFolder( modelFolder, modelBoundingBoxFolder );
        }
        catch ( IOException e ) {
            System.err.println( "Failed to complete CSM Bounding Box Extractor Tool!" );
        }

        System.out.println( "Finished Running CSM Bounding Box Extractor Tool." );
    }

    public static void extractBoundingBoxFromModelsInFolder( File modelFolder, File modelBoundingBoxFolder )
    throws IOException
    {
        System.out.println( "Processing folder: " + modelFolder.getAbsolutePath() );

        for ( File modelFile : modelFolder.listFiles() ) {
            if ( modelFile.isDirectory() ) {
                System.out.println( "Entering directory: " + modelFile.getName() );
                extractBoundingBoxFromModelsInFolder( modelFile,
                                                      new File( modelBoundingBoxFolder, modelFile.getName() ) );
            }
            else if ( modelFile.getName().endsWith( ".json" ) ) {
                System.out.println( "Processing JSON file: " + modelFile.getName() );

                try {
                    // Read the JSON file
                    String jsonFileContent = FileUtils.readFileToString( modelFile );
                    JsonObject blockModelJson = new Gson().fromJson( jsonFileContent, JsonObject.class );
                    writeCombinedBoundingBox( blockModelJson, modelBoundingBoxFolder, modelFile.getName() );
                }
                catch ( Exception e ) {
                    System.err.println( "Failed to process JSON file: " + modelFile.getName() );
                }
            }
            else {
                System.out.println( "Skipping non-JSON file: " + modelFile.getName() );
            }
        }
    }

    public static void writeCombinedBoundingBox( JsonObject blockModelJson,
                                                 File modelBoundingBoxFolder,
                                                 String modelName ) throws IOException

    {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;

        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        JsonArray elements = blockModelJson.getAsJsonArray( "elements" );
        for ( JsonElement element : elements ) {
            JsonObject cuboid = element.getAsJsonObject();
            double[] from = toArray( cuboid.getAsJsonArray( "from" ) );
            double[] to = toArray( cuboid.getAsJsonArray( "to" ) );

            minX = Math.min( minX, from[ 0 ] );
            minY = Math.min( minY, from[ 1 ] );
            minZ = Math.min( minZ, from[ 2 ] );

            maxX = Math.max( maxX, to[ 0 ] );
            maxY = Math.max( maxY, to[ 1 ] );
            maxZ = Math.max( maxZ, to[ 2 ] );
        }

        File boundingBoxFile = new File( modelBoundingBoxFolder, modelName );
        String javaCode = fromJsonToJavaCode( new double[]{ minX, minY, minZ }, new double[]{ maxX, maxY, maxZ } );
        FileUtils.writeStringToFile( boundingBoxFile, javaCode, false );

        // Rounded bounding box
        minX = roundToSignificantValue(minX);
        minY = roundToSignificantValue(minY);
        minZ = roundToSignificantValue(minZ);
        maxX = roundToSignificantValue(maxX);
        maxY = roundToSignificantValue(maxY);
        maxZ = roundToSignificantValue(maxZ);

        javaCode = fromJsonToJavaCode( new double[]{ minX, minY, minZ }, new double[]{ maxX, maxY, maxZ } );
        File roundedBoundingBoxFile = new File( modelBoundingBoxFolder, modelName.replace( ".json","_rounded.json" ) );
        FileUtils.writeStringToFile( roundedBoundingBoxFile, javaCode, false );
    }

    private static double[] toArray( JsonArray jsonArray ) {
        return new double[]{ jsonArray.get( 0 ).getAsDouble(),
                             jsonArray.get( 1 ).getAsDouble(),
                             jsonArray.get( 2 ).getAsDouble() };
    }

    private static double roundToSignificantValue( double value ) {
        double[] significantValues = { -16, 0, 16, 32 };
        double closestValue = significantValues[ 0 ];
        double smallestDifference = Math.abs( value - closestValue );

        for ( int i = 1; i < significantValues.length; i++ ) {
            double difference = Math.abs( value - significantValues[ i ] );
            if ( difference < smallestDifference ) {
                smallestDifference = difference;
                closestValue = significantValues[ i ];
            }
        }
        return closestValue;
    }

    private static String fromJsonToJavaCode( double[] from, double[] to ) {
        double x1 = from[ 0 ] / 16.0;
        double y1 = from[ 1 ] / 16.0;
        double z1 = from[ 2 ] / 16.0;
        double x2 = to[ 0 ] / 16.0;
        double y2 = to[ 1 ] / 16.0;
        double z2 = to[ 2 ] / 16.0;

        return String.format( "    /**\n" +
                                      "     * Retrieves the bounding box of the block.\n" +
                                      "     *\n" +
                                      "     * @param state  the block state\n" +
                                      "     * @param source the block access\n" +
                                      "     * @param pos    the block position\n" +
                                      "     *\n" +
                                      "     * @return The bounding box of the block.\n" +
                                      "     *\n" +
                                      "     * @since 1.0\n" +
                                      "     */\n" +
                                      "    @Override\n" +
                                      "    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {\n" +
                                      "        return new AxisAlignedBB(%f, %f, %f, %f, %f, %f);\n" +
                                      "    }", x1, y1, z1, x2, y2, z2 );
    }

}