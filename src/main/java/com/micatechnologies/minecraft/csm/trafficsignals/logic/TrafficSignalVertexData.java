package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper.Box;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrafficSignalVertexData {
  // New: Generate optimized circle visor (segmented ring)
  public static List<Box> getOptimizedCircleVisor() {
    List<Box> boxes = new ArrayList<>();
    int segments = 16; // Fewer than original; adjust for smoothness
    float radius = 6.0f; // Approx radius of bulb
    float thickness = 0.4f; // Visor thickness
    float depth = 2.0f; // Visor protrusion

    float centerX = 8.0f, centerY = 6.0f, startZ = 11.0f; // Aligned to bulb

    for (int i = 0; i < segments; i++) {
      float angle1 = (float) (2 * Math.PI * i / segments);
      float angle2 = (float) (2 * Math.PI * (i + 1) / segments);

      float x1 = centerX + radius * (float) Math.cos(angle1);
      float y1 = centerY + radius * (float) Math.sin(angle1);
      float x2 = centerX + radius * (float) Math.cos(angle2);
      float y2 = centerY + radius * (float) Math.sin(angle2);

      // Outer quad for thickness
      boxes.add(new Box(new float[]{Math.min(x1, x2), Math.min(y1, y2), startZ},
          new float[]{Math.max(x1, x2), Math.max(y1, y2), startZ + depth}));
      // Add inner if needed for louvered, etc.
    }
    return boxes;
  }

  /**
   * Generates an optimized circular perimeter for a visor, suitable for a triangle fan.
   * The perimeter is offset along the Z-axis by the specified depth to create a 3D effect.
   *
   * @param centerX The X-coordinate of the circle's center.
   * @param centerY The Y-coordinate of the circle's center.
   * @param centerZ The Z-coordinate of the circle's center (base plane).
   * @param radius The radius of the circle.
   * @param depth The depth along the Z-axis to extend the perimeter.
   * @param segments The number of segments/points to approximate the circle (e.g., 16 for smoothness).
   * @return A list of float arrays, each containing [x, y, z] coordinates of the perimeter points.
   */
  public static List<float[]> getOptimizedCircleVisorPerimeter(float centerX, float centerY, float centerZ, float radius, float depth, int segments) {
    List<float[]> perimeter = new ArrayList<>();

    // Ensure at least 3 segments for a valid circle
    if (segments < 3) segments = 3;

    // Calculate the angle increment per segment
    float angleIncrement = (float) (2.0 * Math.PI / segments);

    // Generate points, offset by depth along Z-axis
    for (int i = 0; i < segments; i++) {
      float angle = i * angleIncrement;
      float x = centerX + radius * (float) Math.cos(angle);
      float y = centerY + radius * (float) Math.sin(angle);
      float z = centerZ + depth; // Extend forward along Z
      perimeter.add(new float[]{x, y, z});
    }

    return perimeter;
  }
  // New: Similar for tunnel (longer depth, perhaps tapered)
  public static List<Box> getOptimizedTunnelVisor() {
    List<Box> boxes = new ArrayList<>();
    int segments = 16;
    float radius = 6.0f;
    float depth = 9.0f; // Longer for tunnel

    // Similar loop as above, but extend Z deeper
    // ... (adapt the circle logic, but with z1 = 2.0f, z2 = 11.0f or your original range)

    return boxes;
  }
  public static final List<Box> TUNNEL_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f})
  );


  public static final List<Box> NONE_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 10.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 10.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 10.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 10.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 10.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 10.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 10.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 10.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 10.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 10.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 10.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 10.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 10.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 10.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 10.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 10.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 10.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 10.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 10.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 10.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 10.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 10.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 10.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 10.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 10.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 10.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 10.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 10.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 10.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 10.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 10.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 10.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 10.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 10.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 10.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 10.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 10.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 10.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 10.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 10.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 10.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 10.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 10.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 10.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 10.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 10.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 10.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 10.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 10.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 10.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 10.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f})
  );



  public static final List<Box> VERTICAL_LOUVERED_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{3.15f, 4.10f, 2.00f}, new float[]{3.20f, 8.10f, 11.00f}),
      new Box(new float[]{12.80f, 4.10f, 2.00f}, new float[]{12.85f, 8.10f, 11.00f}),
      new Box(new float[]{11.80f, 2.50f, 2.00f}, new float[]{11.85f, 9.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.90f, 2.00f}, new float[]{10.85f, 10.30f, 11.00f}),
      new Box(new float[]{9.80f, 1.30f, 2.00f}, new float[]{9.85f, 10.90f, 11.00f}),
      new Box(new float[]{8.70f, 0.90f, 2.00f}, new float[]{8.75f, 11.30f, 11.00f}),
      new Box(new float[]{7.25f, 0.90f, 2.00f}, new float[]{7.30f, 11.30f, 11.00f}),
      new Box(new float[]{6.15f, 1.30f, 2.00f}, new float[]{6.20f, 10.90f, 11.00f}),
      new Box(new float[]{5.15f, 1.90f, 2.00f}, new float[]{5.20f, 10.30f, 11.00f}),
      new Box(new float[]{4.15f, 2.50f, 2.00f}, new float[]{4.20f, 9.70f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 10.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 10.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 10.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 10.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 10.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 10.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f})
  );

  public static final List<Box> HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{6.00f, 10.80f, 2.00f}, new float[]{10.00f, 10.85f, 11.00f}),
      new Box(new float[]{4.30f, 9.80f, 2.00f}, new float[]{11.70f, 9.85f, 11.00f}),
      new Box(new float[]{3.50f, 8.80f, 2.00f}, new float[]{12.50f, 8.85f, 11.00f}),
      new Box(new float[]{3.10f, 7.80f, 2.00f}, new float[]{12.90f, 7.85f, 11.00f}),
      new Box(new float[]{2.80f, 6.80f, 2.00f}, new float[]{13.20f, 6.85f, 11.00f}),
      new Box(new float[]{2.80f, 5.80f, 2.00f}, new float[]{13.20f, 5.85f, 11.00f}),
      new Box(new float[]{2.90f, 4.80f, 2.00f}, new float[]{13.10f, 4.85f, 11.00f}),
      new Box(new float[]{3.30f, 3.80f, 2.00f}, new float[]{12.70f, 3.85f, 11.00f}),
      new Box(new float[]{4.00f, 2.80f, 2.00f}, new float[]{12.00f, 2.85f, 11.00f}),
      new Box(new float[]{5.20f, 1.80f, 2.00f}, new float[]{10.80f, 1.85f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 10.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 10.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 10.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 10.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 10.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 10.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f})
  );

  public static final List<Box> BOTH_LOUVERED_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{6.00f, 10.80f, 2.00f}, new float[]{10.00f, 10.85f, 11.00f}),
      new Box(new float[]{4.30f, 9.80f, 2.00f}, new float[]{11.70f, 9.85f, 11.00f}),
      new Box(new float[]{3.50f, 8.80f, 2.00f}, new float[]{12.50f, 8.85f, 11.00f}),
      new Box(new float[]{3.10f, 7.80f, 2.00f}, new float[]{12.90f, 7.85f, 11.00f}),
      new Box(new float[]{2.80f, 6.80f, 2.00f}, new float[]{13.20f, 6.85f, 11.00f}),
      new Box(new float[]{2.80f, 5.80f, 2.00f}, new float[]{13.20f, 5.85f, 11.00f}),
      new Box(new float[]{2.90f, 4.80f, 2.00f}, new float[]{13.10f, 4.85f, 11.00f}),
      new Box(new float[]{3.30f, 3.80f, 2.00f}, new float[]{12.70f, 3.85f, 11.00f}),
      new Box(new float[]{4.00f, 2.80f, 2.00f}, new float[]{12.00f, 2.85f, 11.00f}),
      new Box(new float[]{5.20f, 1.80f, 2.00f}, new float[]{10.80f, 1.85f, 11.00f}),
      new Box(new float[]{3.15f, 4.10f, 2.00f}, new float[]{3.20f, 8.10f, 11.00f}),
      new Box(new float[]{4.15f, 2.50f, 2.00f}, new float[]{4.20f, 9.70f, 11.00f}),
      new Box(new float[]{5.15f, 1.90f, 2.00f}, new float[]{5.20f, 10.30f, 11.00f}),
      new Box(new float[]{6.15f, 1.30f, 2.00f}, new float[]{6.20f, 10.90f, 11.00f}),
      new Box(new float[]{7.25f, 0.90f, 2.00f}, new float[]{7.30f, 11.30f, 11.00f}),
      new Box(new float[]{8.70f, 0.90f, 2.00f}, new float[]{8.75f, 11.30f, 11.00f}),
      new Box(new float[]{9.80f, 1.30f, 2.00f}, new float[]{9.85f, 10.90f, 11.00f}),
      new Box(new float[]{10.80f, 1.90f, 2.00f}, new float[]{10.85f, 10.30f, 11.00f}),
      new Box(new float[]{11.80f, 2.50f, 2.00f}, new float[]{11.85f, 9.70f, 11.00f}),
      new Box(new float[]{12.80f, 4.10f, 2.00f}, new float[]{12.85f, 8.10f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 10.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 10.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 10.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 10.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 10.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 10.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f})
  );


  public static final List<Box> CIRCLE_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 2.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 2.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 2.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 2.00f}, new float[]{10.80f, 1.70f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 2.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 2.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 2.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 2.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 2.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f})
  );




  public static final List<Box> CAP_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 7.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 7.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 7.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 7.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 7.50f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 7.50f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 7.50f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 7.50f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 8.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 8.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 8.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 8.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 5.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 5.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 8.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 8.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 8.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 8.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 5.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 5.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 6.10f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 3.00f}, new float[]{13.60f, 6.10f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 3.00f}, new float[]{2.80f, 6.10f, 11.00f}),
      new Box(new float[]{13.20f, 6.10f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f})
  );

  public static final List<Box> SIGNAL_BODY_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{4.00f, 0.00f, 15.80f}, new float[]{12.00f, 12.00f, 16.00f}),
      new Box(new float[]{3.80f, 0.00f, 15.60f}, new float[]{12.20f, 12.00f, 15.80f}),
      new Box(new float[]{3.60f, 0.00f, 15.40f}, new float[]{12.40f, 12.00f, 15.60f}),
      new Box(new float[]{3.40f, 0.00f, 15.20f}, new float[]{12.60f, 12.00f, 15.40f}),
      new Box(new float[]{3.20f, 0.00f, 15.00f}, new float[]{12.80f, 12.00f, 15.20f}),
      new Box(new float[]{3.00f, 0.00f, 14.80f}, new float[]{13.00f, 12.00f, 15.00f}),
      new Box(new float[]{2.80f, 0.00f, 14.60f}, new float[]{13.20f, 12.00f, 14.80f}),
      new Box(new float[]{2.60f, 0.00f, 14.40f}, new float[]{13.40f, 12.00f, 14.60f}),
      new Box(new float[]{2.40f, 0.00f, 14.20f}, new float[]{13.60f, 12.00f, 14.40f}),
      new Box(new float[]{2.20f, 0.00f, 14.00f}, new float[]{13.80f, 12.00f, 14.20f}),
      new Box(new float[]{2.00f, 0.00f, 11.00f}, new float[]{14.00f, 12.00f, 14.00f}),
      new Box(new float[]{1.80f, 1.20f, 10.80f}, new float[]{2.40f, 1.60f, 11.50f}),
      new Box(new float[]{1.80f, 10.20f, 10.80f}, new float[]{2.40f, 10.60f, 11.50f})
  );

  public static final List<Box> SIGNAL_DOOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.00f, 0.00f, 10.75f}, new float[]{14.00f, 12.00f, 11.00f})
  );
}
