package com.micatechnologies.minecraft.csm;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchUpgradeClassConverter
{
    public static void main( String[] args ) {
        System.out.println( "Running CSM Class Converter..." );

        // Define upgrade path
        final String upgradePath
                = "E:\\source\\repos\\minecraft-city-super-mod\\src\\main\\java\\com\\micatechnologies\\minecraft\\csm\\technology";
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
        else if ( checkTrafficSignalClass( file, fileContents ) ) {
            return upgradeTrafficSignalClass( file, fileContents );
        }
        else if ( checkFireAlarmClassA( file, fileContents ) ) {
            return upgradeFireAlarmClassA( file, fileContents );
        }
        else if ( checkFireAlarmClassB( file, fileContents ) ) {
            return upgradeFireAlarmClassB( file, fileContents );
        }
        else if ( checkFireAlarmClassC( file, fileContents ) ) {
            return upgradeFireAlarmClassC( file, fileContents );
        }
        else if ( checkFireAlarmClassD( file, fileContents ) ) {
            return upgradeFireAlarmClassD( file, fileContents );
        }
        else {
            return upgradeRegularClass( file, fileContents );
        }
    }

    public static boolean checkFireAlarmClassA( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();
        return fileContents.contains( "extends AbstractBlockFireAlarmDetector" );
    }

    public static boolean checkFireAlarmClassB( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();
        return fileContents.contains( "extends AbstractBlockFireAlarmActivator" );
    }

    public static boolean checkFireAlarmClassC( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();
        return fileContents.contains( "extends AbstractBlockFireAlarmSounderVoiceEvac" );
    }

    public static boolean checkFireAlarmClassD( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();
        return fileContents.contains( "extends AbstractBlockFireAlarmSounder" );
    }

    public static boolean upgradeFireAlarmClassA( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();

        // Check if class contains previous version
        final String previousVersionHeaderRegex = "@ElementsCitySuperMod\\.ModElement\\.Tag";
        boolean previousVersionHeaderFound = Pattern.compile( previousVersionHeaderRegex )
                                                    .matcher( fileContents )
                                                    .find();

        if ( previousVersionHeaderFound ) {
            // Get block ID
            String blockIdRegex = "public\\sstatic\\sfinal\\sString\\sblockRegistryName\\s=\\s\"(.*)\";";
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

            // Get onFire method
            String onFireRegex
                    = "public\\svoid\\sonFire\\(\\s?World\\s.*,\\s?BlockPos\\s.*,\\s?IBlockState\\s.*\\s?\\)\\s?\\{(\\s*[^}]*)}";
            int onFireIndex = 1;
            matcher = Pattern.compile( onFireRegex ).matcher( fileContents );
            String onFire;
            if ( matcher.find() ) {
                onFire = matcher.group( onFireIndex );
            }
            else {
                throw new Exception( "Failed to get on fire method from file: " + filePath );
            }

            // Build new class
            StringBuilder newClass = new StringBuilder();
            newClass.append( "package " +
                                     packageName +
                                     ";\n" +
                                     "\n" +
                                     "import net.minecraft.block.state.IBlockState;\n" +
                                     "import net.minecraft.util.math.BlockPos;\n" +
                                     "import net.minecraft.world.World;\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends AbstractBlockFireAlarmDetector\n" +
                                     "{\n" +
                                     "    @Override\n" +
                                     "    public String getBlockRegistryName() {\n" +
                                     "        return \"" +
                                     blockId +
                                     "\";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public void onFire( World world, BlockPos blockPos, IBlockState blockState ) {\n" +
                                     onFire +
                                     "    }\n" +
                                     "}\n" );

            // Write back to file
            FileUtils.writeStringToFile( file, newClass.toString() );
        }

        return true;
    }

    public static boolean upgradeFireAlarmClassB( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();

        // Check if class contains previous version
        final String previousVersionHeaderRegex = "@ElementsCitySuperMod\\.ModElement\\.Tag";
        boolean previousVersionHeaderFound = Pattern.compile( previousVersionHeaderRegex )
                                                    .matcher( fileContents )
                                                    .find();

        if ( previousVersionHeaderFound ) {
            // Get block ID
            String blockIdRegex = "public\\sstatic\\sfinal\\sString\\sblockRegistryName\\s=\\s\"(.*)\";";
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

            // Get onTick method
            String onTickRegex
                    = "public\\svoid\\sonTick\\(\\s?World\\s.*,\\s?BlockPos\\s.*,\\s?IBlockState\\s.*\\s?\\)\\s?\\{(\\s*[^}]*)}";
            int onTickIndex = 1;
            matcher = Pattern.compile( onTickRegex ).matcher( fileContents );
            String onTick;
            if ( matcher.find() ) {
                onTick = matcher.group( onTickIndex );
            }
            else {
                throw new Exception( "Failed to get on tick method from file: " + filePath );
            }

            // Get getBlockTickRate() method
            String getBlockTickRateRegex = "public\\sint\\sgetBlockTickRate\\(\\s?\\)\\s?\\{\\s*return\\s(.*);\\s*}";
            int getBlockTickRateIndex = 1;
            matcher = Pattern.compile( getBlockTickRateRegex ).matcher( fileContents );
            String getBlockTickRate;
            if ( matcher.find() ) {
                getBlockTickRate = matcher.group( getBlockTickRateIndex );
            }
            else {
                throw new Exception( "Failed to get getBlockTickRate method from file: " + filePath );
            }

            // Parse Java file
            CompilationUnit parseResult = StaticJavaParser.parse( file );
            if ( parseResult == null ) {
                throw new Exception( "Failed to parse Java file: " + filePath );
            }

            // Get on block activated method
            boolean hasOnBlockActivated = false;
            AtomicReference< String > onBlockActivatedMethodSignature = new AtomicReference<>( null );
            AtomicReference< String > onBlockActivatedMethodBody = new AtomicReference<>( null );
            if ( fileContents.contains( "onBlockActivated" ) ) {
                hasOnBlockActivated = true;
                parseResult.findAll( MethodDeclaration.class ).forEach( methodDeclaration -> {
                    if ( methodDeclaration.getNameAsString().equals( "onBlockActivated" ) ) {
                        onBlockActivatedMethodSignature.set(
                                "@SideOnly( Side.CLIENT )\n@Override\n" + methodDeclaration.getDeclarationAsString() );
                        methodDeclaration.getBody()
                                         .ifPresent(
                                                 blockStmt -> onBlockActivatedMethodBody.set( blockStmt.toString() ) );
                    }
                } );
            }
            if ( hasOnBlockActivated && onBlockActivatedMethodSignature.get() == null ) {
                throw new IllegalAccessException( "onBlockActivatedMethod signature is null" );
            }
            if ( hasOnBlockActivated && onBlockActivatedMethodBody.get() == null ) {
                throw new IllegalAccessException( "onBlockActivatedMethod body is null" );
            }
            String onBlockActivatedMethod = null;
            if ( hasOnBlockActivated ) {
                onBlockActivatedMethod = onBlockActivatedMethodSignature.get() +
                        "\n" +
                        onBlockActivatedMethodBody.get();
            }

            // Build new class
            StringBuilder newClass = new StringBuilder();
            newClass.append( "package " +
                                     packageName +
                                     ";\n" +
                                     "\n" +
                                     "import net.minecraft.block.state.IBlockState;\n" +
                                     "import net.minecraft.entity.player.EntityPlayer;\n" +
                                     "import net.minecraft.util.EnumFacing;\n" +
                                     "import net.minecraft.util.EnumHand;\n" +
                                     "import net.minecraft.util.math.BlockPos;\n" +
                                     "import net.minecraft.util.text.TextComponentString;\n" +
                                     "import net.minecraft.world.World;\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends AbstractBlockFireAlarmActivator\n" +
                                     "{\n" +
                                     "\n" +
                                     ( hasOnBlockActivated ? "\n" + onBlockActivatedMethod + "\n" : "\n" ) +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public String getBlockRegistryName() {\n" +
                                     "        return \"" +
                                     blockId +
                                     "\";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public int getBlockTickRate() {\n" +
                                     "        return " +
                                     getBlockTickRate +
                                     ";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public void onTick( World world, BlockPos blockPos, IBlockState blockState ) {\n" +
                                     "        " +
                                     onTick +
                                     "    }\n" +
                                     "}\n" );

            // Write back to file
            FileUtils.writeStringToFile( file, newClass.toString() );
        }

        return true;
    }

    public static boolean upgradeFireAlarmClassC( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();

        // Check if class contains previous version
        final String previousVersionHeaderRegex = "@ElementsCitySuperMod\\.ModElement\\.Tag";
        boolean previousVersionHeaderFound = Pattern.compile( previousVersionHeaderRegex )
                                                    .matcher( fileContents )
                                                    .find();

        if ( previousVersionHeaderFound ) {
            // Get block ID
            String blockIdRegex = "public\\sstatic\\sfinal\\sString\\sblockRegistryName\\s=\\s\"(.*)\";";
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
                                     "import net.minecraft.block.state.IBlockState;\n" +
                                     "import net.minecraft.util.math.BlockPos;\n" +
                                     "import net.minecraft.world.World;\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends AbstractBlockFireAlarmSounderVoiceEvac\n" +
                                     "{\n" +
                                     "    @Override\n" +
                                     "    public String getBlockRegistryName() {\n" +
                                     "        return \"" +
                                     blockId +
                                     "\";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "}\n" );

            // Write back to file
            FileUtils.writeStringToFile( file, newClass.toString() );
        }

        return true;
    }

    public static boolean upgradeFireAlarmClassD( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();

        // Check if class contains previous version
        final String previousVersionHeaderRegex = "@ElementsCitySuperMod\\.ModElement\\.Tag";
        boolean previousVersionHeaderFound = Pattern.compile( previousVersionHeaderRegex )
                                                    .matcher( fileContents )
                                                    .find();

        if ( previousVersionHeaderFound ) {
            // Get block ID
            String blockIdRegex = "public\\sstatic\\sfinal\\sString\\sblockRegistryName\\s=\\s\"(.*)\";";
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

            // Get getSoundResourceName method
            String getSoundResourceNameRegex
                    = "public\\sString\\sgetSoundResourceName\\(\\s?IBlockState\\s.*\\s?\\)\\s?\\{(\\s*[^}]*)}";
            int getSoundResourceNameIndex = 1;
            matcher = Pattern.compile( getSoundResourceNameRegex ).matcher( fileContents );
            String getSoundResourceName;
            if ( matcher.find() ) {
                getSoundResourceName = matcher.group( getSoundResourceNameIndex );
                if ( getSoundResourceName.contains( "if" ) ) {
                    throw new Exception( "getSoundResourceName contains dynamic sound selection logic and requires " +
                                                 "manual conversion!" );
                }
            }
            else {
                throw new Exception( "Failed to get getSoundResourceName method from file: " + filePath );
            }

            // Get getSoundTickLen method
            String getSoundTickLenRegex
                    = "public\\sint\\sgetSoundTickLen\\(\\s?IBlockState\\s.*\\s?\\)\\s?\\{(\\s*[^}]*)}";
            int getSoundTickLenIndex = 1;
            matcher = Pattern.compile( getSoundTickLenRegex ).matcher( fileContents );
            String getSoundTickLen;
            if ( matcher.find() ) {
                getSoundTickLen = matcher.group( getSoundTickLenIndex );
                if ( getSoundTickLen.contains( "if" ) ) {
                    throw new Exception( "getSoundTickLen contains dynamic sound selection logic and requires manual " +
                                                 "conversion!" );
                }
            }
            else {
                throw new Exception( "Failed to get getSoundTickLen method from file: " + filePath );
            }

            // Build new class
            StringBuilder newClass = new StringBuilder();
            newClass.append( "package " +
                                     packageName +
                                     ";\n" +
                                     "\n" +
                                     "import net.minecraft.block.state.IBlockState;\n" +
                                     "import net.minecraft.util.math.BlockPos;\n" +
                                     "import net.minecraft.world.World;\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends AbstractBlockFireAlarmSounder\n" +
                                     "{\n" +
                                     "    @Override\n" +
                                     "    public String getBlockRegistryName() {\n" +
                                     "        return \"" +
                                     blockId +
                                     "\";\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public String getSoundResourceName( IBlockState blockState ) {\n" +
                                     "        " +
                                     getSoundResourceName +
                                     "\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public int getSoundTickLen( IBlockState blockState ) {\n" +
                                     "        " +
                                     getSoundTickLen +
                                     "\n" +
                                     "    }\n" +
                                     "}\n" );

            // Write back to file
            FileUtils.writeStringToFile( file, newClass.toString() );
        }

        return true;
    }

    public static boolean checkTrafficSignalClass( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();
        return fileContents.contains( "extends AbstractBlockControllableSignal" );
    }

    public static boolean upgradeTrafficSignalClass( File file, String fileContents ) throws Exception {
        final String filePath = file.getPath();

        // Check if class contains previous version
        final String previousVersionHeaderRegex = "@ElementsCitySuperMod\\.ModElement\\.Tag";
        boolean previousVersionHeaderFound = Pattern.compile( previousVersionHeaderRegex )
                                                    .matcher( fileContents )
                                                    .find();

        if ( previousVersionHeaderFound ) {
            // Get block ID
            String blockIdRegex1 = "setTranslationKey\\(\\s*\"(\\w+)\"\\s*\\)";
            String blockIdRegex2 = "setRegistryName\\(\\s*\"(\\w+)\"\\s*\\)";
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

            // Get block signal side
            String signalSideRegex
                    = "public\\sSIGNAL_SIDE\\sgetSignalSide\\(\\s?World\\s.*,\\s?BlockPos\\s.*\\s?\\)\\s?\\{\\s*(.*)\\s*}";
            int signalSideIndex = 1;
            matcher = Pattern.compile( signalSideRegex ).matcher( fileContents );
            String signalSide;
            if ( matcher.find() ) {
                signalSide = matcher.group( signalSideIndex );
            }
            else {
                throw new Exception( "Failed to get signal side from file: " + filePath );
            }

            // Get block does flash
            String doesFlashRegex = "public\\sboolean\\sdoesFlash\\(\\s?\\)\\s?\\{\\s*(.*)\\s*\\}";
            int doesFlashIndex = 1;
            matcher = Pattern.compile( doesFlashRegex ).matcher( fileContents );
            String doesFlash;
            if ( matcher.find() ) {
                doesFlash = matcher.group( doesFlashIndex );
            }
            else {
                throw new Exception( "Failed to get does flash from file: " + filePath );
            }

            // Build new class
            StringBuilder newClass = new StringBuilder();
            newClass.append( "package " +
                                     packageName +
                                     ";\n" +
                                     "\n" +
                                     "import com.micatechnologies.minecraft.csm.tabs.CsmTabTrafficSignals;\n" +
                                     "import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;\n" +
                                     "import net.minecraft.block.Block;\n" +
                                     "import net.minecraft.block.SoundType;\n" +
                                     "import net.minecraft.block.material.Material;\n" +
                                     "import net.minecraft.client.renderer.block.model.ModelResourceLocation;\n" +
                                     "import net.minecraft.item.Item;\n" +
                                     "import net.minecraft.item.ItemBlock;\n" +
                                     "import net.minecraft.util.EnumFacing;\n" +
                                     "import net.minecraft.util.math.BlockPos;\n" +
                                     "import net.minecraft.world.World;\n" +
                                     "import net.minecraftforge.client.event.ModelRegistryEvent;\n" +
                                     "import net.minecraftforge.client.model.ModelLoader;\n" +
                                     "import net.minecraftforge.fml.common.registry.GameRegistry;\n" +
                                     "import net.minecraftforge.fml.relauncher.Side;\n" +
                                     "import net.minecraftforge.fml.relauncher.SideOnly;\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends AbstractBlockControllableSignal\n" +
                                     "{\n" +
                                     "    public " +
                                     className +
                                     "() {\n" +
                                     "        super( Material.ROCK );\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public SIGNAL_SIDE getSignalSide( World world, BlockPos blockPos ) {\n" +
                                     "        " +
                                     signalSide +
                                     "\n" +
                                     "    }\n" +
                                     "\n" +
                                     "    @Override\n" +
                                     "    public boolean doesFlash() {\n" +
                                     "        " +
                                     doesFlash +
                                     "\n" +
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
                                     "}\n" );

            // Write back to file
            FileUtils.writeStringToFile( file, newClass.toString() );
        }

        return true;
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
            final String[] unsupportedContains = new String[]{ "PropertyInteger",
                                                               "PropertyBool",
                                                               "PropertyEnum",
                                                               "extends Item" };
            final String[] unsupportedRegex = new String[]{};
            for ( String unsupported : unsupportedContains ) {
                if ( fileContents.contains( unsupported ) ) {
                    throw new Exception( "Unsupported code (" + unsupported + ") found in file: " + filePath );
                }
            }
            for ( String unsupported : unsupportedRegex ) {
                if ( Pattern.compile( unsupported ).matcher( fileContents ).find() ) {
                    throw new Exception( "Unsupported code (Regex: " + unsupported + ") found in file: " + filePath );
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
            String blockIdRegex2 = "setTranslationKey\\(\\s*\"(\\w+)\"\\s*\\)";
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
                    System.err.println( "[WARN] Failed to get default bounding box from block file and " +
                                                "full copied bounding box will be used: " +
                                                filePath );
                    boundingBox = matcher.group( boundingBoxIndex2 );
                }
                else {
                    System.err.println( "[WARN] Failed to get bounding box from block file and default (SQUARE) " +
                                                "bounding box will be used: " +
                                                filePath );
                    boundingBox = "return SQUARE_BOUNDING_BOX;";
                }
            }

            // Get block isPassable
            boolean hasIsPassable = false;
            String isPassable = null;
            if ( fileContents.contains( "isPassable" ) ) {
                hasIsPassable = true;
                String isPassableRegex
                        = "\\s*@Override\\s*public\\sboolean\\sisPassable\\(\\s?IBlockAccess\\s.*,\\s?BlockPos\\s.*\\)\\s?\\{\\s*(.*)\\s*}";
                matcher = Pattern.compile( isPassableRegex ).matcher( fileContents );
                if ( matcher.find() ) {
                    isPassable = matcher.group();
                }
                else {
                    throw new Exception( "Failed to get isPassable from file: " + filePath );
                }
            }
            if ( hasIsPassable && isPassable == null ) {
                throw new IllegalAccessException( "isPassable is null" );
            }

            // Get block collision bounding box
            boolean hasCollisionBoundingBox = false;
            String collisionBoundingBox = null;
            if ( fileContents.contains( "getCollisionBoundingBox" ) ) {
                hasCollisionBoundingBox = true;
                String collisionBoundingBoxRegex
                        = "\\s*@Override\\s*.*\\s*public\\sAxisAlignedBB\\sgetCollisionBoundingBox\\(\\s?IBlockState\\s.*,\\s?IBlockAccess\\s.*,\\s?BlockPos\\s.*\\s?\\)\\s?\\{\\s*(.*)\\s*}";
                matcher = Pattern.compile( collisionBoundingBoxRegex ).matcher( fileContents );
                if ( matcher.find() ) {
                    collisionBoundingBox = matcher.group();
                }
                else {
                    throw new Exception( "Failed to get collision bounding box from file: " + filePath );
                }
            }
            if ( hasCollisionBoundingBox && collisionBoundingBox == null ) {
                throw new IllegalAccessException( "collisionBoundingBox is null" );
            }

            // Get tile entity class name
            boolean hasTileEntity = false;
            String tileEntityClassName = null;
            int tileEntityClassNameIndex = 2;
            if ( fileContents.contains( "ITileEntityProvider" ) ) {
                hasTileEntity = true;
                String tileEntityCreateMethodRegex
                        = "\\s*(@Nullable)?\\s*@Override\\s*public\\sTileEntity\\screateNewTileEntity\\(\\s?World\\s.*,\\s?int\\s.*\\s?\\)\\s?\\{\\s*return\\snew\\s(.*)\\(\\);\\s*}";
                matcher = Pattern.compile( tileEntityCreateMethodRegex ).matcher( fileContents );
                if ( matcher.find() ) {
                    tileEntityClassName = matcher.group( tileEntityClassNameIndex );
                }
                else {
                    throw new Exception( "Failed to get tile entity class name from file: " + filePath );
                }
            }
            if ( hasTileEntity && tileEntityClassName == null ) {
                throw new IllegalAccessException( "tileEntityClassName is null" );
            }
            String tileEntityMethod = null;
            if ( hasTileEntity ) {
                tileEntityMethod = "    /**\n" +
                        "         * Gets the tile entity class for the block.\n" +
                        "         *\n" +
                        "         * @return the tile entity class for the block\n" +
                        "         *\n" +
                        "         * @since 1.0\n" +
                        "         */\n" +
                        "        @Override\n" +
                        "        public Class< ? extends TileEntity > getTileEntityClass() {\n" +
                        "            return " +
                        tileEntityClassName +
                        ".class;\n" +
                        "        }";
            }

            // Parse Java file
            CompilationUnit parseResult = StaticJavaParser.parse( file );
            if ( parseResult == null ) {
                throw new Exception( "Failed to parse Java file: " + filePath );
            }

            // Get neighbor changed method
            boolean hasNeighborChanged = false;
            AtomicReference< String > neighborChangedMethodSignature = new AtomicReference<>( null );
            AtomicReference< String > neighborChangedMethodBody = new AtomicReference<>( null );
            if ( fileContents.contains( "neighborChanged" ) ) {
                hasNeighborChanged = true;
                parseResult.findAll( MethodDeclaration.class ).forEach( methodDeclaration -> {
                    if ( methodDeclaration.getNameAsString().equals( "neighborChanged" ) ) {
                        neighborChangedMethodSignature.set(
                                "@Override\n" + methodDeclaration.getDeclarationAsString() );
                        methodDeclaration.getBody()
                                         .ifPresent(
                                                 blockStmt -> neighborChangedMethodBody.set( blockStmt.toString() ) );
                    }
                } );
            }
            if ( hasNeighborChanged && neighborChangedMethodSignature.get() == null ) {
                throw new IllegalAccessException( "neighborChangedMethod signature is null" );
            }
            if ( hasNeighborChanged && neighborChangedMethodBody.get() == null ) {
                throw new IllegalAccessException( "neighborChangedMethod body is null" );
            }
            String neighborChangedMethod = null;
            if ( hasNeighborChanged ) {
                neighborChangedMethod = neighborChangedMethodSignature.get() + "\n" + neighborChangedMethodBody.get();
            }

            // Get on block activated method
            boolean hasOnBlockActivated = false;
            AtomicReference< String > onBlockActivatedMethodSignature = new AtomicReference<>( null );
            AtomicReference< String > onBlockActivatedMethodBody = new AtomicReference<>( null );
            if ( fileContents.contains( "onBlockActivated" ) ) {
                hasOnBlockActivated = true;
                parseResult.findAll( MethodDeclaration.class ).forEach( methodDeclaration -> {
                    if ( methodDeclaration.getNameAsString().equals( "onBlockActivated" ) ) {
                        onBlockActivatedMethodSignature.set(
                                "@SideOnly( Side.CLIENT )\n@Override\n" + methodDeclaration.getDeclarationAsString() );
                        methodDeclaration.getBody()
                                         .ifPresent(
                                                 blockStmt -> onBlockActivatedMethodBody.set( blockStmt.toString() ) );
                    }
                } );
            }
            if ( hasOnBlockActivated && onBlockActivatedMethodSignature.get() == null ) {
                throw new IllegalAccessException( "onBlockActivatedMethod signature is null" );
            }
            if ( hasOnBlockActivated && onBlockActivatedMethodBody.get() == null ) {
                throw new IllegalAccessException( "onBlockActivatedMethod body is null" );
            }
            String onBlockActivatedMethod = null;
            if ( hasOnBlockActivated ) {
                onBlockActivatedMethod = onBlockActivatedMethodSignature.get() +
                        "\n" +
                        onBlockActivatedMethodBody.get();
            }

            // Get can provide power method
            boolean hasCanProvidePower = false;
            AtomicReference< String > canProvidePowerMethodSignature = new AtomicReference<>( null );
            AtomicReference< String > canProvidePowerMethodBody = new AtomicReference<>( null );
            if ( fileContents.contains( "canProvidePower" ) ) {
                hasCanProvidePower = true;
                parseResult.findAll( MethodDeclaration.class ).forEach( methodDeclaration -> {
                    if ( methodDeclaration.getNameAsString().equals( "canProvidePower" ) ) {
                        canProvidePowerMethodSignature.set( "@Override\n@ParametersAreNonnullByDefault\n" +
                                                                    methodDeclaration.getDeclarationAsString() );
                        methodDeclaration.getBody()
                                         .ifPresent(
                                                 blockStmt -> canProvidePowerMethodBody.set( blockStmt.toString() ) );
                    }
                } );
            }
            if ( hasCanProvidePower && canProvidePowerMethodSignature.get() == null ) {
                throw new IllegalAccessException( "canProvidePowerMethod signature is null" );
            }
            if ( hasCanProvidePower && canProvidePowerMethodBody.get() == null ) {
                throw new IllegalAccessException( "canProvidePowerMethod body is null" );
            }
            String canProvidePowerMethod = null;
            if ( hasCanProvidePower ) {
                canProvidePowerMethod = canProvidePowerMethodSignature.get() + "\n" + canProvidePowerMethodBody.get();
            }

            // Get add information method
            boolean hasAddInformation = false;
            AtomicReference< String > addInformationMethodSignature = new AtomicReference<>( null );
            AtomicReference< String > addInformationMethodBody = new AtomicReference<>( null );
            if ( fileContents.contains( "addInformation" ) ) {
                hasAddInformation = true;
                parseResult.findAll( MethodDeclaration.class ).forEach( methodDeclaration -> {
                    if ( methodDeclaration.getNameAsString().equals( "addInformation" ) ) {
                        addInformationMethodSignature.set( "@Override\n@ParametersAreNonnullByDefault\n" +
                                                                   methodDeclaration.getDeclarationAsString() );
                        methodDeclaration.getBody()
                                         .ifPresent(
                                                 blockStmt -> addInformationMethodBody.set( blockStmt.toString() ) );
                    }
                } );
            }
            if ( hasAddInformation && addInformationMethodSignature.get() == null ) {
                throw new IllegalAccessException( "addInformationMethod signature is null" );
            }
            if ( hasAddInformation && addInformationMethodBody.get() == null ) {
                throw new IllegalAccessException( "addInformationMethod body is null" );
            }
            String addInformationMethod = null;
            if ( hasAddInformation ) {
                addInformationMethod = addInformationMethodSignature.get() + "\n" + addInformationMethodBody.get();
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

            // Check if contains raw mod import
            boolean hasRawCsmModImport = fileContents.contains( "import com.micatechnologies.minecraft.csm.Csm;" );

            // Build new class
            StringBuilder newClass = new StringBuilder();
            newClass.append( "package " +
                                     packageName +
                                     ";\n" +
                                     "\n" +
                                     ( hasRawCsmModImport ? "import com.micatechnologies.minecraft.csm.Csm;\n" : "" ) +
                                     "import com.micatechnologies.minecraft.csm.codeutils." +
                                     newBlockExtendsClassName +
                                     ";\n" +
                                     ( hasTileEntity ?
                                       "import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;\n" :
                                       "" ) +
                                     "import net.minecraft.block.SoundType;\n" +
                                     "import net.minecraft.block.material.Material;\n" +
                                     "import net.minecraft.block.state.IBlockState;\n" +
                                     ( hasTileEntity ? "import net.minecraft.tileentity.TileEntity;\n" : "" ) +
                                     ( hasNeighborChanged ? "import net.minecraft.block.Block;\n" : "" ) +
                                     "import net.minecraft.block.BlockDirectional;\n" +
                                     "import net.minecraft.block.BlockHorizontal;\n" +
                                     "import net.minecraft.util.BlockRenderLayer;\n" +
                                     "import net.minecraft.util.EnumFacing;\n" +
                                     ( hasOnBlockActivated ? "import net.minecraft.util.EnumHand;\n" : "" ) +
                                     ( hasOnBlockActivated ?
                                       "import net.minecraft.entity.player.EntityPlayer;\n" :
                                       "" ) +
                                     "import net.minecraft.util.math.AxisAlignedBB;\n" +
                                     "import net.minecraft.util.math.BlockPos;\n" +
                                     "import net.minecraft.world.IBlockAccess;\n" +
                                     "import net.minecraftforge.fml.relauncher.Side;\n" +
                                     "import net.minecraftforge.fml.relauncher.SideOnly;\n" +
                                     ( hasTileEntity ? "import net.minecraft.world.World;\n" : "" ) +
                                     "\n" +
                                     "import javax.annotation.Nonnull;\n" +
                                     "import javax.annotation.Nullable;\n" +
                                     "\n" +
                                     "public class " +
                                     className +
                                     " extends " +
                                     newBlockExtendsClassName +
                                     ( hasTileEntity ? " implements ICsmTileEntityProvider" : "" ) +
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
                                     "    }" +
                                     ( hasIsPassable ? isPassable : "\n" ) +
                                     ( hasCollisionBoundingBox ? collisionBoundingBox : "\n" ) +
                                     ( hasTileEntity ? "\n" + tileEntityMethod + "\n" : "\n" ) +
                                     ( hasNeighborChanged ? "\n" + neighborChangedMethod + "\n" : "\n" ) +
                                     ( hasOnBlockActivated ? "\n" + onBlockActivatedMethod + "\n" : "\n" ) +
                                     ( hasCanProvidePower ? "\n" + canProvidePowerMethod + "\n" : "\n" ) +
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
