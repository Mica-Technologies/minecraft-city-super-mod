package com.micatechnologies.minecraft.csm;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchUpgradeClassConverter
{
    public static void main( String[] args ) {
        System.out.println( "Running CSM Class Converter..." );

        // Define upgrade path
        final String upgradePath
                = "/Users/ahawk/IdeaProjects/minecraft-city-super-mod/src/main/java/com/micatechnologies/minecraft" +
                "/csm/technology";
        final boolean upgradeIsFileNotFolder = false;

        // Upgrade
        if ( upgradeIsFileNotFolder ) {
            try {
                boolean success = upgradeClass( new File( upgradePath ) );
                if ( !success ) {
                    System.out.println( "Failed to upgrade class: " + upgradePath );
                }
            }
            catch ( Exception e ) {
                System.out.println( "Error during upgrade of class: " + upgradePath );
                e.printStackTrace();
            }
        }
        else {
            for ( File file : Objects.requireNonNull( new File( upgradePath ).listFiles() ) ) {
                try {
                    boolean success = upgradeClass( file );
                    if ( !success ) {
                        System.out.println( "Failed to upgrade class: " + file.getPath() );
                    }
                }
                catch ( Exception e ) {
                    System.out.println( "Error during upgrade of class: " + file.getPath() );
                    e.printStackTrace();
                }
            }
        }

        System.out.println( "Finished Running CSM Class Converter." );
    }

    public static boolean upgradeClass( File file ) throws Exception {
        // Read file to string
        String fileContents = FileUtils.readFileToString( file );

        if ( checkLightingClass( file, fileContents ) ) {
            return upgradeLightingClass( file, fileContents );
        }
        else if ( checkTrafficSignClass( file, fileContents ) ) {
            return upgradeTrafficSignClass( file, fileContents );
        }
        else {
            return upgradeRegularClass( file, fileContents );
        }
    }

    public static boolean checkTrafficSignClass( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();
        return fileContents.contains( "extends AbstractBlockSign" );
    }

    public static boolean upgradeTrafficSignClass( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();

        // Check if class contains previous version
        final String previousVersionHeaderRegex = "@ElementsCitySuperMod\\.ModElement\\.Tag";
        boolean previousVersionHeaderFound = Pattern.compile( previousVersionHeaderRegex )
                                                    .matcher( fileContents )
                                                    .find();

        if ( previousVersionHeaderFound ) {
            // Get block ID
            String blockIdRegex = "public\\sString\\sgetBlockRegistryName\\(\\)\\s?\\{\\s*return\\s?\"(.*)\";";
            int blockIdIndex = 1;
            Matcher matcher = Pattern.compile( blockIdRegex ).matcher( fileContents );
            String blockId;
            if ( matcher.find() ) {
                blockId = matcher.group( blockIdIndex );
            }
            else {

                throw new Exception( "Failed to get block ID from file: " + filePath );
            }

            // Get package name
            String packageNameRegex = "package\\s(.*);";
            int packageNameIndex = 1;
            matcher = Pattern.compile( packageNameRegex ).matcher( fileContents );
            String packageName;
            if ( matcher.find() ) {
                packageName = matcher.group( packageNameIndex );
            }
            else {
                throw new Exception( "Failed to get package name from file: " + filePath );
            }

            // Get block class name
            String classNameRegex = "public\\sclass\\s(.*)\\sextends\\sElementsCitySuperMod\\.ModElement";
            int classNameIndex = 1;
            matcher = Pattern.compile( classNameRegex ).matcher( fileContents );
            String className;
            if ( matcher.find() ) {
                className = matcher.group( classNameIndex );
            }
            else {
                throw new Exception( "Failed to get class name from file: " + filePath );
            }

            // Build new class
            StringBuilder newClass = new StringBuilder();
            newClass.append( "package " +
                                     packageName +
                                     ";\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends AbstractBlockSign\n" +
                                     "{\n" +
                                     "    @Override\n" +
                                     "    public String getBlockRegistryName() {\n" +
                                     "        return \"" +
                                     blockId +
                                     "\";\n" +
                                     "    }\n" +
                                     "}\n" );

            // Write back to file
            FileUtils.writeStringToFile( file, newClass.toString() );
        }

        return true;
    }

    public static boolean checkLightingClass( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();
        return fileContents.contains( "extends AbstractBrightLight" );
    }

    public static boolean upgradeLightingClass( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();

        // Check if class contains previous version
        final String previousVersionHeaderRegex = "@ElementsCitySuperMod\\.ModElement\\.Tag";
        boolean previousVersionHeaderFound = Pattern.compile( previousVersionHeaderRegex )
                                                    .matcher( fileContents )
                                                    .find();

        if ( previousVersionHeaderFound ) {
            // Get block ID
            String blockIdRegex = "static\\sfinal\\sString\\selementId\\s?=\\s?\"(.*)\";";
            int blockIdIndex = 1;
            Matcher matcher = Pattern.compile( blockIdRegex ).matcher( fileContents );
            String blockId;
            if ( matcher.find() ) {
                blockId = matcher.group( blockIdIndex );
            }
            else {

                throw new Exception( "Failed to get block ID from file: " + filePath );
            }

            // Get bright light X offset
            String brightLightXOffsetRegex
                    = "public\\sint\\sgetBrightLightXOffset\\(\\)\\s?\\{\\s*return\\s(.*);\\s*\\}";
            int brightLightXOffsetIndex = 1;
            matcher = Pattern.compile( brightLightXOffsetRegex ).matcher( fileContents );
            String brightLightXOffset;
            if ( matcher.find() ) {
                brightLightXOffset = matcher.group( brightLightXOffsetIndex );
            }
            else {
                throw new Exception( "Failed to get bright light x offset from file: " + filePath );
            }

            // Get block bounding box
            String boundingBoxRegex1
                    = ".*getBoundingBox.*IBlockState\\s*.*\\s*IBlockAccess\\s*.*\\s*BlockPos\\s*.*\\s*(switch\\s.*(\\s*[^}]*default:\\s*([^}\\r\\n]*)[^}]*)})\\s*}";
            String boundingBoxRegex2
                    = ".*getBoundingBox.*IBlockState\\s*.*\\s*IBlockAccess\\s*.*\\s*BlockPos\\s*.*\\s*(switch\\s.*(\\s*[^}]*)})\\s*}";
            int boundingBoxIndex1 = 3;
            int boundingBoxIndex2 = 1;
            matcher = Pattern.compile( boundingBoxRegex1 ).matcher( fileContents );
            String boundingBox;
            if ( matcher.find() ) {
                boundingBox = matcher.group( boundingBoxIndex1 );
            }
            else {
                matcher = Pattern.compile( boundingBoxRegex2 ).matcher( fileContents );
                if ( matcher.find() ) {
                    System.err.println( "[WARN] Failed to get default bounding box from bright light file and " +
                                                "full copied bounding box will be used: " +
                                                filePath );
                    boundingBox = matcher.group( boundingBoxIndex2 );
                }
                else {
                    System.err.println(
                            "[WARN] Failed to get bounding box from bright light file and default (SQUARE) " +
                                    "bounding box will be used: " +
                                    filePath );
                    boundingBox = "return SQUARE_BOUNDING_BOX;";
                }
            }

            // Get package name
            String packageNameRegex = "package\\s(.*);";
            int packageNameIndex = 1;
            matcher = Pattern.compile( packageNameRegex ).matcher( fileContents );
            String packageName;
            if ( matcher.find() ) {
                packageName = matcher.group( packageNameIndex );
            }
            else {
                throw new Exception( "Failed to get package name from file: " + filePath );
            }

            // Get block class name
            String classNameRegex = "public\\sclass\\s(.*)\\sextends\\sElementsCitySuperMod\\.ModElement";
            int classNameIndex = 1;
            matcher = Pattern.compile( classNameRegex ).matcher( fileContents );
            String className;
            if ( matcher.find() ) {
                className = matcher.group( classNameIndex );
            }
            else {
                throw new Exception( "Failed to get class name from file: " + filePath );
            }

            // Build new class
            StringBuilder newClass = new StringBuilder();
            newClass.append( "package " +
                                     packageName +
                                     ";\n" +
                                     "\n" +
                                     "import net.minecraft.block.state.IBlockState;\n" +
                                     "import net.minecraft.util.math.AxisAlignedBB;\n" +
                                     "import net.minecraft.util.math.BlockPos;\n" +
                                     "import net.minecraft.world.IBlockAccess;\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends AbstractBrightLight\n" +
                                     "{\n" +
                                     "    @Override\n" +
                                     "    public String getBlockRegistryName() {\n" +
                                     "        return \"" +
                                     blockId +
                                     "\";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    /**\n" +
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
                                     "        " +
                                     boundingBox +
                                     "\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public int getBrightLightXOffset() {\n" +
                                     "        return " +
                                     brightLightXOffset +
                                     ";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "}\n" );

            // Write back to file
            FileUtils.writeStringToFile( file, newClass.toString() );
        }

        return true;
    }

    public static boolean upgradeRegularClass( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();

        // Define new class names
        final String[] newClassNames = new String[]{ "AbstractBlock",
                                                     "AbstractBlockRotatableNSEWUD",
                                                     "AbstractBlockRotatableNSEW" };

        // Check if class contains previous version
        final String previousVersionHeaderRegex = "@ElementsCitySuperMod\\.ModElement\\.Tag";
        boolean previousVersionHeaderFound = Pattern.compile( previousVersionHeaderRegex )
                                                    .matcher( fileContents )
                                                    .find();

        // Check if class already using new class
        boolean newClassFound = false;
        for ( String newClassName : newClassNames ) {
            if ( fileContents.contains( "extends " + newClassName ) ) {
                newClassFound = true;
                break;
            }
        }

        // Throw exception if class is unsupported (not previous version or new class)
        if ( !previousVersionHeaderFound && !newClassFound ) {
            throw new Exception( "Unsupported file: " + filePath );
        }

        // Upgrade class if needed
        if ( !newClassFound ) {
            // Check if class contains something unsupported
            final String[] unsupportedContains = new String[]{ "getCollisionBoundingBox",
                                                               "PropertyInteger",
                                                               "PropertyBool",
                                                               "PropertyEnum",
                                                               "ITileEntityProvider",
                                                               "isPassable" };
            final String[] unsupportedRegex = new String[]{};
            for ( String unsupported : unsupportedContains ) {
                if ( fileContents.contains( unsupported ) ) {
                    throw new Exception( "Unsupported code found in file: " + filePath );
                }
            }
            for ( String unsupported : unsupportedRegex ) {
                if ( Pattern.compile( unsupported ).matcher( fileContents ).find() ) {
                    throw new Exception( "Unsupported code found in file: " + filePath );
                }
            }

            // Detect if class has no rotation, NESW, or NESWUD
            Rotation rotation = Rotation.NONE;
            if ( fileContents.contains( "BlockDirectional.FACING" ) ) {
                rotation = Rotation.NESWUD;
            }
            else if ( fileContents.contains( "BlockHorizontal.FACING" ) ) {
                rotation = Rotation.NESW;
            }

            // Get block ID
            String blockIdRegex1 = "\\.setRegistryName\\(\\s*\"(\\w+)\"\\s*\\)";
            String blockIdRegex2 = "setUnlocalizedName\\(\\s*\"(\\w+)\"\\s*\\)";
            int blockIdIndex = 1;
            Matcher matcher = Pattern.compile( blockIdRegex1 ).matcher( fileContents );
            String blockId;
            if ( matcher.find() ) {
                blockId = matcher.group( blockIdIndex );
            }
            else {
                matcher = Pattern.compile( blockIdRegex2 ).matcher( fileContents );
                if ( matcher.find() ) {
                    blockId = matcher.group( blockIdIndex );
                }
                else {
                    throw new Exception( "Failed to get block ID from file: " + filePath );
                }
            }

            // Get material type
            String materialTypeRegex
                    = "public\\s+BlockCustom\\(\\s*\\)\\s*\\{\\s*super\\(\\s*([a-zA-Z_.]+)\\s*\\);[\\s\\S]*?\\}";
            int materialTypeIndex = 1;
            matcher = Pattern.compile( materialTypeRegex ).matcher( fileContents );
            String materialType;
            if ( matcher.find() ) {
                materialType = matcher.group( materialTypeIndex );
            }
            else {
                throw new Exception( "Failed to get material type from file: " + filePath );
            }

            // Get sound type
            String soundTypeRegex = ".*setSoundType\\(\\s?(.*)\\s?\\).*";
            int soundTypeIndex = 1;
            matcher = Pattern.compile( soundTypeRegex ).matcher( fileContents );
            String soundType;
            if ( matcher.find() ) {
                soundType = matcher.group( soundTypeIndex );
            }
            else {
                throw new Exception( "Failed to get sound type from file: " + filePath );
            }

            // Get harvest level
            String harvestLevelRegex = ".*setHarvestLevel\\(\\s?(.*)\\s?\\).*";
            int harvestLevelIndex = 1;
            matcher = Pattern.compile( harvestLevelRegex ).matcher( fileContents );
            String harvestLevel;
            final String defaultHarvestLevel = "\"pickaxe\", 1";
            if ( matcher.find() ) {
                harvestLevel = matcher.group( harvestLevelIndex );
            }
            else {
                System.err.println( "[WARN] Failed to get harvest level from file and default (" +
                                            defaultHarvestLevel +
                                            ") will be used: " +
                                            filePath );
                harvestLevel = defaultHarvestLevel;
            }

            // Get hardness
            String hardnessRegex = ".*setHardness\\(\\s?(.*)\\s?\\).*";
            int hardnessIndex = 1;
            matcher = Pattern.compile( hardnessRegex ).matcher( fileContents );
            String hardness;
            if ( matcher.find() ) {
                hardness = matcher.group( hardnessIndex );
            }
            else {
                throw new Exception( "Failed to get hardness from file: " + filePath );
            }

            // Get resistance
            String resistanceRegex = ".*setResistance\\(\\s?(.*)\\s?\\).*";
            int resistanceIndex = 1;
            matcher = Pattern.compile( resistanceRegex ).matcher( fileContents );
            String resistance;
            if ( matcher.find() ) {
                resistance = matcher.group( resistanceIndex );
            }
            else {
                throw new Exception( "Failed to get resistance from file: " + filePath );
            }

            // Get light level
            String lightLevelRegex = ".*setLightLevel\\(\\s?(.*)\\s?\\).*";
            int lightLevelIndex = 1;
            matcher = Pattern.compile( lightLevelRegex ).matcher( fileContents );
            String lightLevel;
            if ( matcher.find() ) {
                lightLevel = matcher.group( lightLevelIndex );
            }
            else {
                throw new Exception( "Failed to get light level from file: " + filePath );
            }

            // Get light opacity
            String lightOpacityRegex = ".*setLightOpacity\\(\\s?(.*)\\s?\\).*";
            int lightOpacityIndex = 1;
            matcher = Pattern.compile( lightOpacityRegex ).matcher( fileContents );
            String lightOpacity;
            if ( matcher.find() ) {
                lightOpacity = matcher.group( lightOpacityIndex );
            }
            else {
                throw new Exception( "Failed to get light opacity from file: " + filePath );
            }

            // Get full cube value
            String fullCubeRegex = ".*isFullCube.*(\\r|\\n|\\r\\n).*return\\s(.*);(\\r|\\n|\\r\\n).*}";
            int fullCubeIndex = 2;
            matcher = Pattern.compile( fullCubeRegex ).matcher( fileContents );
            boolean fullCube = true;
            if ( matcher.find() ) {
                fullCube = matcher.group( fullCubeIndex ).equalsIgnoreCase( "true" );
            }

            // Get opaque cube value
            String opaqueCubeRegex = ".*isOpaqueCube.*(\\r|\\n|\\r\\n).*return\\s(.*);(\\r|\\n|\\r\\n).*}";
            int opaqueCubeIndex = 2;
            matcher = Pattern.compile( opaqueCubeRegex ).matcher( fileContents );
            boolean opaqueCube = true;
            if ( matcher.find() ) {
                opaqueCube = matcher.group( opaqueCubeIndex ).equalsIgnoreCase( "true" );
            }

            // Get redstone connection value
            String redstoneConnectionRegex
                    = ".*canConnectRedstone.*IBlockState\\s*.*\\s*IBlockAccess\\s*.*\\s*BlockPos\\s*.*\\s*.*EnumFacing\\s*.*\\s*\\{\\s*return\\s(.*);\\s*}";
            int redstoneConnectionIndex = 1;
            matcher = Pattern.compile( redstoneConnectionRegex ).matcher( fileContents );
            boolean redstoneConnection = false;
            if ( matcher.find() ) {
                redstoneConnection = matcher.group( redstoneConnectionIndex ).equalsIgnoreCase( "true" );
            }

            // Get block render layer
            String renderLayerRegex = ".*getBlockLayer.*(\\r|\\n|\\r\\n).*return\\s(.*);(\\r|\\n|\\r\\n).*}";
            int renderLayerIndex = 2;
            matcher = Pattern.compile( renderLayerRegex ).matcher( fileContents );
            String renderLayer;
            if ( matcher.find() ) {
                renderLayer = matcher.group( renderLayerIndex );
            }
            else {
                renderLayer = "BlockRenderLayer.SOLID";
            }

            // Get block bounding box
            String boundingBoxRegex1
                    = ".*getBoundingBox.*IBlockState\\s*.*\\s*IBlockAccess\\s*.*\\s*BlockPos\\s*.*\\s*(switch\\s.*(\\s*[^}]*default:\\s*([^}\\r\\n]*)[^}]*)})\\s*}";
            String boundingBoxRegex2
                    = ".*getBoundingBox.*IBlockState\\s*.*\\s*IBlockAccess\\s*.*\\s*BlockPos\\s*.*\\s*(switch\\s.*(\\s*[^}]*)})\\s*}";
            int boundingBoxIndex1 = 3;
            int boundingBoxIndex2 = 1;
            matcher = Pattern.compile( boundingBoxRegex1 ).matcher( fileContents );
            String boundingBox;
            if ( matcher.find() ) {
                boundingBox = matcher.group( boundingBoxIndex1 );
            }
            else {
                matcher = Pattern.compile( boundingBoxRegex2 ).matcher( fileContents );
                if ( matcher.find() ) {
                    System.err.println( "[WARN] Failed to get default bounding box from bright light file and " +
                                                "full copied bounding box will be used: " +
                                                filePath );
                    boundingBox = matcher.group( boundingBoxIndex2 );
                }
                else {
                    System.err.println(
                            "[WARN] Failed to get bounding box from bright light file and default (SQUARE) " +
                                    "bounding box will be used: " +
                                    filePath );
                    boundingBox = "return SQUARE_BOUNDING_BOX;";
                }
            }

            // Get block class name
            String classNameRegex = "public\\sclass\\s(.*)\\sextends\\sElementsCitySuperMod\\.ModElement";
            int classNameIndex = 1;
            matcher = Pattern.compile( classNameRegex ).matcher( fileContents );
            String className;
            if ( matcher.find() ) {
                className = matcher.group( classNameIndex );
            }
            else {
                throw new Exception( "Failed to get class name from file: " + filePath );
            }

            // Get new block extends class name
            String newBlockExtendsClassName = newClassNames[ 0 ];
            if ( rotation == Rotation.NESWUD ) {
                newBlockExtendsClassName = newClassNames[ 1 ];
            }
            else if ( rotation == Rotation.NESW ) {
                newBlockExtendsClassName = newClassNames[ 2 ];
            }

            // Get package name
            String packageNameRegex = "package\\s(.*);";
            int packageNameIndex = 1;
            matcher = Pattern.compile( packageNameRegex ).matcher( fileContents );
            String packageName;
            if ( matcher.find() ) {
                packageName = matcher.group( packageNameIndex );
            }
            else {
                throw new Exception( "Failed to get package name from file: " + filePath );
            }

            // Build new class
            StringBuilder newClass = new StringBuilder();
            newClass.append( "package " +
                                     packageName +
                                     ";\n" +
                                     "\n" +
                                     "import com.micatechnologies.minecraft.csm.codeutils." +
                                     newBlockExtendsClassName +
                                     ";\n" +
                                     "import net.minecraft.block.SoundType;\n" +
                                     "import net.minecraft.block.material.Material;\n" +
                                     "import net.minecraft.block.state.IBlockState;\n" +
                                     "import net.minecraft.block.BlockDirectional;\n" +
                                     "import net.minecraft.block.BlockHorizontal;\n" +
                                     "import net.minecraft.util.BlockRenderLayer;\n" +
                                     "import net.minecraft.util.EnumFacing;\n" +
                                     "import net.minecraft.util.math.AxisAlignedBB;\n" +
                                     "import net.minecraft.util.math.BlockPos;\n" +
                                     "import net.minecraft.world.IBlockAccess;\n" +
                                     "\n" +
                                     "import javax.annotation.Nonnull;\n" +
                                     "import javax.annotation.Nullable;\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends " +
                                     newBlockExtendsClassName +
                                     "\n" +
                                     "{\n" +
                                     "    public " +
                                     className +
                                     "() {\n" +
                                     "        super( " +
                                     materialType +
                                     ", " +
                                     soundType +
                                     ", " +
                                     harvestLevel +
                                     ", " +
                                     hardness +
                                     ", " +
                                     resistance +
                                     ", " +
                                     lightLevel +
                                     ", " +
                                     lightOpacity +
                                     " );\n" +

                                     "    }\n" +
                                     "\n" +
                                     "    /**\n" +
                                     "     * Retrieves the registry name of the block.\n" +
                                     "     *\n" +
                                     "     * @return The registry name of the block.\n" +
                                     "     *\n" +
                                     "     * @since 1.0\n" +
                                     "     */\n" +
                                     "    @Override\n" +
                                     "    public String getBlockRegistryName() {\n" +
                                     "        return \"" +
                                     blockId +
                                     "\";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    /**\n" +
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
                                     "        " +
                                     boundingBox +
                                     "\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    /**\n" +
                                     "     * Retrieves whether the block is an opaque cube.\n" +
                                     "     *\n" +
                                     "     * @param state The block state.\n" +
                                     "     *\n" +
                                     "     * @return {@code true} if the block is an opaque cube, {@code false} otherwise.\n" +
                                     "     *\n" +
                                     "     * @since 1.0\n" +
                                     "     */\n" +
                                     "    @Override\n" +
                                     "    public boolean getBlockIsOpaqueCube( IBlockState state ) {\n" +
                                     "        return " +
                                     opaqueCube +
                                     ";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    /**\n" +
                                     "     * Retrieves whether the block is a full cube.\n" +
                                     "     *\n" +
                                     "     * @param state The block state.\n" +
                                     "     *\n" +
                                     "     * @return {@code true} if the block is a full cube, {@code false} otherwise.\n" +
                                     "     *\n" +
                                     "     * @since 1.0\n" +
                                     "     */\n" +
                                     "    @Override\n" +
                                     "    public boolean getBlockIsFullCube( IBlockState state ) {\n" +
                                     "        return " +
                                     fullCube +
                                     ";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    /**\n" +
                                     "     * Retrieves whether the block connects to redstone.\n" +
                                     "     *\n" +
                                     "     * @param state  the block state\n" +
                                     "     * @param access the block access\n" +
                                     "     * @param pos    the block position\n" +
                                     "     * @param facing the block facing direction\n" +
                                     "     *\n" +
                                     "     * @return {@code true} if the block connects to redstone, {@code false} otherwise.\n" +
                                     "     *\n" +
                                     "     * @since 1.0\n" +
                                     "     */\n" +
                                     "    @Override\n" +
                                     "    public boolean getBlockConnectsRedstone( IBlockState state,\n" +
                                     "                                             IBlockAccess access,\n" +
                                     "                                             BlockPos pos,\n" +
                                     "                                             @Nullable EnumFacing facing )\n" +
                                     "    {\n" +
                                     "        return " +
                                     redstoneConnection +
                                     ";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    /**\n" +
                                     "     * Retrieves the block's render layer.\n" +
                                     "     *\n" +
                                     "     * @return The block's render layer.\n" +
                                     "     *\n" +
                                     "     * @since 1.0\n" +
                                     "     */\n" +
                                     "    @Nonnull\n" +
                                     "    @Override\n" +
                                     "    public BlockRenderLayer getBlockRenderLayer() {\n" +
                                     "        return " +
                                     renderLayer +
                                     ";\n" +
                                     "    }\n" +
                                     "}\n" );

            // Write back to file
            FileUtils.writeStringToFile( file, newClass.toString() );
        }

        return true;
    }

    public enum Rotation
    {
        NONE, NESW, NESWUD
    }
}
