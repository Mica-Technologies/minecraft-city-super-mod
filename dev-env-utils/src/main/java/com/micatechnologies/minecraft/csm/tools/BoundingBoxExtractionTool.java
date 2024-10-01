package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class BoundingBoxExtractionTool {

  // Constants for 'reasonable' variant
  private static final double minBoxSideSize = 1.6;  // New minimum size for clickable space

  // Toggles for enabling/disabling rounding
  private static final boolean ENABLE_MODEL_ROUNDING = false;  // Toggle model rounding on/off (0-16 scale)
  private static final boolean ENABLE_BBOX_ROUNDING = true;  // Toggle bounding box rounding on/off (0.0-1.0 scale)

  // Thresholds for each rounding type
  private static final double modelRoundingThreshold = 0.099999;  // Threshold for model scale rounding (0-16)
  private static final double bboxRoundingThreshold = 0.05;  // Threshold for bounding box scale rounding (0.0-1.0)

  // Constants for 'rounded' variant
  private static final double[] significantValuesRounded = {-16, 0, 16, 32};

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Bounding Box Extractor Tool", args,
        (devEnvironmentPath) -> {
          final String modelFolderPathRelative = "src/main/resources/assets/csm/models/custom";
          final String modelBoundingBoxFolderPathRelative =
              "dev-env-utils/boundingBoxExtractorToolOutput";
          File modelFolder = new File(devEnvironmentPath, modelFolderPathRelative);
          File modelBoundingBoxFolder =
              new File(devEnvironmentPath, modelBoundingBoxFolderPathRelative);
          extractBoundingBoxFromModelsInFolder(modelFolder, modelBoundingBoxFolder);
        });
  }

  public static void extractBoundingBoxFromModelsInFolder(File modelFolder,
      File modelBoundingBoxFolder)
      throws IOException {
    System.out.println("Processing folder: " + modelFolder.getAbsolutePath());

    for (File modelFile : modelFolder.listFiles()) {
      if (modelFile.isDirectory()) {
        System.out.println("Entering directory: " + modelFile.getName());
        extractBoundingBoxFromModelsInFolder(modelFile,
            new File(modelBoundingBoxFolder, modelFile.getName()));
      } else if (modelFile.getName().endsWith(".json")) {
        System.out.println("Processing JSON file: " + modelFile.getName());

        try {
          // Read the JSON file
          String jsonFileContent = FileUtils.readFileToString(modelFile);
          JsonObject blockModelJson = new Gson().fromJson(jsonFileContent, JsonObject.class);
          writeCombinedBoundingBox(blockModelJson, modelBoundingBoxFolder, modelFile.getName());
        } catch (Exception e) {
          System.err.println("Failed to process JSON file: " + modelFile.getName());
        }
      } else {
        System.out.println("Skipping non-JSON file: " + modelFile.getName());
      }
    }
  }

  public static void writeCombinedBoundingBox(JsonObject blockModelJson,
      File modelBoundingBoxFolder,
      String modelName) throws IOException {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;

    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    JsonArray elements = blockModelJson.getAsJsonArray("elements");
    for (JsonElement element : elements) {
      JsonObject cuboid = element.getAsJsonObject();
      double[] from = toArray(cuboid.getAsJsonArray("from"));
      double[] to = toArray(cuboid.getAsJsonArray("to"));

      minX = Math.min(minX, from[0]);
      minY = Math.min(minY, from[1]);
      minZ = Math.min(minZ, from[2]);

      maxX = Math.max(maxX, to[0]);
      maxY = Math.max(maxY, to[1]);
      maxZ = Math.max(maxZ, to[2]);
    }

    // Write Regular Bounding Box
    File regularBoundingBoxFile = new File(modelBoundingBoxFolder, modelName);
    String regularJavaCode =
        fromJsonToJavaCode(new double[]{minX, minY, minZ}, new double[]{maxX, maxY, maxZ},false);
    FileUtils.writeStringToFile(regularBoundingBoxFile, regularJavaCode, false);

    // Write Rounded Bounding Box (Significant values -16, 0, 16, 32)
    double roundedMinX = roundToSignificantValue(minX);
    double roundedMinY = roundToSignificantValue(minY);
    double roundedMinZ = roundToSignificantValue(minZ);
    double roundedMaxX = roundToSignificantValue(maxX);
    double roundedMaxY = roundToSignificantValue(maxY);
    double roundedMaxZ = roundToSignificantValue(maxZ);

    File roundedBoundingBoxFile =
        new File(modelBoundingBoxFolder, modelName.replace(".json", "_rounded.json"));
    String roundedJavaCode = fromJsonToJavaCode(new double[]{roundedMinX, roundedMinY, roundedMinZ},
        new double[]{roundedMaxX, roundedMaxY, roundedMaxZ},false);
    FileUtils.writeStringToFile(roundedBoundingBoxFile, roundedJavaCode, false);

    // Write Reasonable Bounding Box
    double[] adjustedFrom = adjustBoxSide(minX, minY, minZ, maxX, maxY, maxZ, true);
    double[] adjustedTo = adjustBoxSide(minX, minY, minZ, maxX, maxY, maxZ, false);

    File reasonableBoundingBoxFile =
        new File(modelBoundingBoxFolder, modelName.replace(".json", "_reasonable.json"));
    String reasonableJavaCode = fromJsonToJavaCode(adjustedFrom, adjustedTo,true);
    FileUtils.writeStringToFile(reasonableBoundingBoxFile, reasonableJavaCode, false);
  }

  // Method to round to significant values (-16, 0, 16, 32)
  private static double roundToSignificantValue(double value) {
    double closestValue = significantValuesRounded[0];
    double smallestDifference = Math.abs(value - closestValue);

    for (int i = 1; i < significantValuesRounded.length; i++) {
      double difference = Math.abs(value - significantValuesRounded[i]);
      if (difference < smallestDifference) {
        smallestDifference = difference;
        closestValue = significantValuesRounded[i];
      }
    }
    return closestValue;
  }

  private static double[] adjustBoxSide(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, boolean isFrom) {
    double[] adjustedValues = new double[3];
    double[] mins = {minX, minY, minZ};
    double[] maxs = {maxX, maxY, maxZ};

    for (int i = 0; i < 3; i++) {
      double size = maxs[i] - mins[i];
      System.out.printf("Initial min[%d]: %.6f, max[%d]: %.6f, size[%d]: %.6f\n", i, mins[i], i, maxs[i], i, size);

      // Enforce minimum size before any rounding
      if (size < minBoxSideSize) {
        System.out.printf("Size smaller than minBoxSideSize[%d]: %.6f, enforcing minimum size.\n", i, minBoxSideSize);
        if (isFrom) {
          adjustedValues[i] = mins[i];
          maxs[i] = mins[i] + minBoxSideSize; // Increase the max value to ensure the minimum size
        } else {
          mins[i] = maxs[i] - minBoxSideSize; // Decrease the min value to ensure the minimum size
          adjustedValues[i] = maxs[i];
        }
        System.out.printf("After min size enforcement min[%d]: %.6f, max[%d]: %.6f\n", i, mins[i], i, maxs[i]);
      } else if (ENABLE_MODEL_ROUNDING) {
        // Apply model rounding if enabled
        double originalValue = isFrom ? mins[i] : maxs[i];
        double roundedValue = roundToNearestModel(originalValue);

        System.out.printf("Original model value[%d]: %.6f, Rounded model value[%d]: %.6f\n", i, originalValue, i, roundedValue);

        // Avoid rounding that would result in min == max
        if (isFrom && roundedValue >= maxs[i]) {
          System.out.printf("Rounding would collapse min[%d], adjusting min[%d].\n", i, i);
          adjustedValues[i] = maxs[i] - minBoxSideSize;
        } else if (!isFrom && roundedValue <= mins[i]) {
          System.out.printf("Rounding would collapse max[%d], adjusting max[%d].\n", i, i);
          adjustedValues[i] = mins[i] + minBoxSideSize;
        } else {
          // Safe to apply rounding
          System.out.printf("Applying rounded model value[%d]: %.6f\n", i, roundedValue);
          adjustedValues[i] = roundedValue;
        }
      } else {
        // If model rounding is disabled, keep the original values
        adjustedValues[i] = isFrom ? mins[i] : maxs[i];
      }

      System.out.printf("Final adjusted model min[%d]: %.6f, max[%d]: %.6f\n", i, mins[i], i, maxs[i]);
    }
    return adjustedValues;
  }

  private static double roundToNearestModel(double value) {
    double[] roundingValuesModel = {0.0, 4.0, 8.0, 12.0, 16.0};  // Model scale rounding values (0-16)
    double closestValue = roundingValuesModel[0];
    double smallestDifference = Math.abs(value - closestValue);

    for (double roundingValue : roundingValuesModel) {
      double difference = Math.abs(value - roundingValue);
      if (difference < smallestDifference && difference <= modelRoundingThreshold) {
        smallestDifference = difference;
        closestValue = roundingValue;
      }
    }
    return closestValue;
  }

  private static double roundToPrecision(double value, int decimalPlaces) {
    double scale = Math.pow(10, decimalPlaces);
    return Math.round(value * scale) / scale;
  }


  private static double[] toArray(JsonArray jsonArray) {
    return new double[]{jsonArray.get(0).getAsDouble(),
        jsonArray.get(1).getAsDouble(),
        jsonArray.get(2).getAsDouble()};
  }

  private static String fromJsonToJavaCode(double[] from, double[] to, boolean isReasonableVariant) {
    double x1 = from[0] / 16.0;
    double y1 = from[1] / 16.0;
    double z1 = from[2] / 16.0;
    double x2 = to[0] / 16.0;
    double y2 = to[1] / 16.0;
    double z2 = to[2] / 16.0;

    if (isReasonableVariant && ENABLE_BBOX_ROUNDING) {
      // Apply bounding box rounding only if this is the reasonable variant
      x1 = roundToNearestBbox(x1, x1, x2, true);
      y1 = roundToNearestBbox(y1, y1, y2, true);
      z1 = roundToNearestBbox(z1, z1, z2, true);
      x2 = roundToNearestBbox(x2, x1, x2, false);
      y2 = roundToNearestBbox(y2, y1, y2, false);
      z2 = roundToNearestBbox(z2, z1, z2, false);
    }

    // Apply precision rounding to avoid floating-point weirdness
    x1 = roundToPrecision(x1, 6);
    y1 = roundToPrecision(y1, 6);
    z1 = roundToPrecision(z1, 6);
    x2 = roundToPrecision(x2, 6);
    y2 = roundToPrecision(y2, 6);
    z2 = roundToPrecision(z2, 6);

    return String.format(
        "    /**\r\n" +
            "     * Retrieves the bounding box of the block.\r\n" +
            "     *\r\n" +
            "     * @param state  the block state\r\n" +
            "     * @param source the block access\r\n" +
            "     * @param pos    the block position\r\n" +
            "     *\r\n" +
            "     * @return The bounding box of the block.\r\n" +
            "     *\r\n" +
            "     * @since 1.0\r\n" +
            "     */\r\n" +
            "    @Override\r\n" +
            "    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {\r\n" +
            "        return new AxisAlignedBB(%f, %f, %f, %f, %f, %f);\r\n" +
            "    }", x1, y1, z1, x2, y2, z2);
  }

  private static double roundToNearestBbox(double value, double min, double max, boolean isMin) {
    double[] roundingValuesBbox = {0.0, 0.25, 0.5, 0.75, 1.0};  // Bounding box scale rounding values (can extend beyond 1.0)
    double closestValue = roundingValuesBbox[0];
    double smallestDifference = Math.abs(value - closestValue);

    for (double roundingValue : roundingValuesBbox) {
      double difference = Math.abs(value - roundingValue);
      if (difference < smallestDifference && difference <= bboxRoundingThreshold) {
        smallestDifference = difference;
        closestValue = roundingValue;
      }
    }

    // Only round if the difference is within the threshold
    if (smallestDifference <= bboxRoundingThreshold) {
      // Prevent collapse by ensuring min and max are not equal after rounding
      if (isMin && closestValue >= max) {
        // Adjust min to maintain the minimum size and avoid collapse
        return max - Math.max(minBoxSideSize / 16.0, Math.abs(max - min));  // Adjust min based on max
      } else if (!isMin && closestValue <= min) {
        // Adjust max to maintain the minimum size and avoid collapse
        return min + Math.max(minBoxSideSize / 16.0, Math.abs(max - min));  // Adjust max based on min
      }
      return closestValue;
    }

    return value;  // Return the value if rounding doesn't apply
  }

}