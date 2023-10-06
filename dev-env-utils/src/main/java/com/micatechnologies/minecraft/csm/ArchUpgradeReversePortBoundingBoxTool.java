package com.micatechnologies.minecraft.csm;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchUpgradeReversePortBoundingBoxTool {
    private static final String NEW_BOUNDING_BOX_PATTERN = "@Override\\s+public AxisAlignedBB getBlockBoundingBox\\( IBlockState state, IBlockAccess source, BlockPos pos \\) \\{\\s+return new AxisAlignedBB\\(.*?\\);\\s+\\}";
    private static final Pattern OLD_BOUNDING_BOX_NORTH_PATTERN = Pattern.compile("case NORTH:\\s+return new AxisAlignedBB\\((.*?);");

    public static void main(String[] args) throws IOException {
        System.out.println("Running CSM Reverse Port Bounding Box Tool...");

        String upgradeRootFolderPath = "E:\\source\\repos\\minecraft-city-super-mod\\";
        String liveCodeFolderPath = "src\\main\\java\\com\\micatechnologies\\minecraft\\csm";
        String oldCodeFolderPath = "old\\src\\main\\java\\com\\micatechnologies\\minecraft\\csm";
        String liveCodeFolderFullPath = upgradeRootFolderPath + liveCodeFolderPath;
        String oldCodeFolderFullPath = upgradeRootFolderPath + oldCodeFolderPath;
        File liveCodeFolder = new File(liveCodeFolderFullPath);
        File oldCodeFolder = new File(oldCodeFolderFullPath);

        updateBoundingBoxes(liveCodeFolder, oldCodeFolder);

        System.out.println("Finished Running CSM Reverse Port Bounding Box Tool.");
    }

    public static void updateBoundingBoxes(File liveCodeFolder, File oldCodeFolder) throws IOException {
        System.out.println("Processing folder: " + liveCodeFolder.getAbsolutePath());

        for (File liveFile : liveCodeFolder.listFiles()) {
            if (liveFile.isDirectory()) {
                System.out.println("Entering directory: " + liveFile.getName());
                updateBoundingBoxes(liveFile, new File(oldCodeFolder, liveFile.getName()));
            } else if (liveFile.getName().endsWith(".java")) {
                System.out.println("Processing Java file: " + liveFile.getName());

                File oldFile = new File(oldCodeFolder, liveFile.getName());
                if (oldFile.exists()) {
                    System.out.println("Found matching old code file: " + oldFile.getName());

                    Path livePath = liveFile.toPath();
                    Path oldPath = oldFile.toPath();

                    String liveContent = new String(Files.readAllBytes(livePath));
                    String oldContent = new String(Files.readAllBytes(oldPath));

                    if (liveContent.contains("getBlockBoundingBox")) {
                        System.out.println("Found new bounding box method in: " + liveFile.getName());

                        Matcher matcher = OLD_BOUNDING_BOX_NORTH_PATTERN.matcher(oldContent);
                        if (matcher.find()) {
                            System.out.println("Found old NORTH bounding box code. Updating...");

                            String oldNorthBoundingBox = matcher.group(1).trim();
                            if (oldNorthBoundingBox.endsWith(")")) {
                                oldNorthBoundingBox = oldNorthBoundingBox.substring(0, oldNorthBoundingBox.length() - 1);
                            }

                            liveContent = liveContent.replaceAll(NEW_BOUNDING_BOX_PATTERN, "@Override\n    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {\n        return new AxisAlignedBB(" + oldNorthBoundingBox + ");\n    }");
                            Files.write(livePath, liveContent.getBytes());

                            System.out.println("Updated " + liveFile.getName());
                        } else {
                            System.out.println("Did not find old NORTH bounding box code in: " + oldFile.getName());
                        }
                    } else {
                        System.out.println("Did not find new bounding box method in: " + liveFile.getName());
                    }
                } else {
                    System.out.println("No matching old code file for: " + liveFile.getName());
                }
            } else {
                System.out.println("Skipping non-Java file: " + liveFile.getName());
            }
        }
    }
}
