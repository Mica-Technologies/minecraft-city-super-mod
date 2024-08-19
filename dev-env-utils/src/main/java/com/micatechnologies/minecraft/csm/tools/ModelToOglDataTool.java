package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class ModelToOglDataTool {

  private static final String[] MODEL_FILES_TO_CONVERT = {
      "src/main/resources/assets/csm/models/block/trafficsignals/visors/visor_cap.json",
      "src/main/resources/assets/csm/models/block/trafficsignals/visors/visor_circle.json",
      "src/main/resources/assets/csm/models/block/trafficsignals/visors/visor_louvered_vertical.json",
      "src/main/resources/assets/csm/models/block/trafficsignals/visors/visor_louvered_horizontal.json",
      "src/main/resources/assets/csm/models/block/trafficsignals/visors/visor_louvered_both.json",
      "src/main/resources/assets/csm/models/block/trafficsignals/visors/visor_none.json",
      "src/main/resources/assets/csm/models/block/trafficsignals/visors/visor_tunnel.json"
  };

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Model to OpenGL Data Converter", args,
        (devEnvironmentPath) -> {
          final String outputFolder = "dev-env-utils/openGlData/";
          for (String modelFilePath : MODEL_FILES_TO_CONVERT) {
            File modelFile = new File(devEnvironmentPath, modelFilePath);
            if (!modelFile.exists()) {
              System.err.println("Model file not found: " + modelFile.getAbsolutePath());
              continue;
            }

            String outputFileName = modelFile.getName().replace(".json", ".ogldata");
            File outputFile = new File(devEnvironmentPath, outputFolder + outputFileName);

            convertModelToOglData(modelFile, outputFile);
            System.out.println("Converted: " + modelFile.getName() + " -> " + outputFileName);
          }
        });
  }

  private static void convertModelToOglData(File modelFile, File outputFile) {
    try {
      String jsonString = FileUtils.readFileToString(modelFile, "UTF-8");
      JsonObject root = new Gson().fromJson(jsonString, JsonObject.class);
      JsonArray elements = root.getAsJsonArray("elements");

      List<String> boxLines = new ArrayList<>();
      for (JsonElement el : elements) {
        JsonObject obj = el.getAsJsonObject();
        float[] from = getFloatArray(obj.getAsJsonArray("from"));
        float[] to = getFloatArray(obj.getAsJsonArray("to"));

        String line = String.format("    new Box(new float[]{%.2ff, %.2ff, %.2ff}, new float[]{%.2ff, %.2ff, %.2ff})",
            from[0], from[1], from[2], to[0], to[1], to[2]);
        boxLines.add(line);
      }

      StringBuilder result = new StringBuilder();
      result.append("Arrays.asList(\n");
      for (int i = 0; i < boxLines.size(); i++) {
        result.append(boxLines.get(i));
        if (i < boxLines.size() - 1) {
          result.append(",");
        }
        result.append("\n");
      }
      result.append(");\n");

      FileUtils.writeStringToFile(outputFile, result.toString(), "UTF-8");

    } catch (IOException e) {
      System.err.println("Error processing " + modelFile.getName() + ": " + e.getMessage());
    }
  }

  private static float[] getFloatArray(JsonArray jsonArray) {
    float[] arr = new float[3];
    for (int i = 0; i < 3; i++) {
      arr[i] = jsonArray.get(i).getAsFloat();
    }
    return arr;
  }
}
