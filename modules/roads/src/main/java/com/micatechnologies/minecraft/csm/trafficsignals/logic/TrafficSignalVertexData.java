package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper.Box;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrafficSignalVertexData {

  // Extra downward tilt (degrees) applied to horizontal louver slats on top of the visor tilt.
  // Combined with the 9° visor tilt and the ~6° gap/depth ratio, this yields a visibility
  // cutoff around 23° from horizontal — blocking signals beyond ~20 blocks when mounted
  // 10 blocks above road level.
  // Public because the renderer aims a programmed louver by adjusting FROM this authored value.
  public static final float HORIZONTAL_LOUVER_EXTRA_TILT = 15.0f;
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
      new Box(new float[]{3.15f, 4.10f, 2.00f}, new float[]{3.20f, 8.10f, 11.00f}, true),
      new Box(new float[]{12.80f, 4.10f, 2.00f}, new float[]{12.85f, 8.10f, 11.00f}, true),
      new Box(new float[]{11.80f, 2.50f, 2.00f}, new float[]{11.85f, 9.70f, 11.00f}, true),
      new Box(new float[]{10.80f, 1.90f, 2.00f}, new float[]{10.85f, 10.30f, 11.00f}, true),
      new Box(new float[]{9.80f, 1.30f, 2.00f}, new float[]{9.85f, 10.90f, 11.00f}, true),
      new Box(new float[]{8.70f, 0.90f, 2.00f}, new float[]{8.75f, 11.30f, 11.00f}, true),
      new Box(new float[]{7.25f, 0.90f, 2.00f}, new float[]{7.30f, 11.30f, 11.00f}, true),
      new Box(new float[]{6.15f, 1.30f, 2.00f}, new float[]{6.20f, 10.90f, 11.00f}, true),
      new Box(new float[]{5.15f, 1.90f, 2.00f}, new float[]{5.20f, 10.30f, 11.00f}, true),
      new Box(new float[]{4.15f, 2.50f, 2.00f}, new float[]{4.20f, 9.70f, 11.00f}, true),
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
      // Horizontal louver slats — each slat is split into Z segments with X width matched
      // to the visor circle at the tilted Y position at that depth. Upper slats widen toward
      // the front (tilt drops them toward wider visor center). Lower slats narrow toward the
      // front (tilt drops them toward narrower visor bottom). Back segments clip into the
      // opaque visor walls (invisible); front segments fit the visor profile at dropped Y.
      // Y=10.8: back (narrow, top of visor) → mid → front (wide, drops to Y≈8.4)
      // X ranges matched to visor shell inner wall at tilted Y for each Z segment.
      new Box(new float[]{6.00f, 10.80f, 9.00f}, new float[]{10.00f, 10.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{4.30f, 10.80f, 5.00f}, new float[]{11.70f, 10.85f, 9.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.50f, 10.80f, 2.00f}, new float[]{12.50f, 10.85f, 5.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=9.8: back → front (drops to Y≈7.4)
      new Box(new float[]{4.30f, 9.80f, 7.00f}, new float[]{11.70f, 9.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.10f, 9.80f, 2.00f}, new float[]{12.90f, 9.85f, 7.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=8.8: back → front (drops to Y≈6.4)
      new Box(new float[]{3.50f, 8.80f, 7.00f}, new float[]{12.50f, 8.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{2.80f, 8.80f, 2.00f}, new float[]{13.20f, 8.85f, 7.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=7.8: back → front (drops to Y≈5.4)
      new Box(new float[]{3.10f, 7.80f, 7.00f}, new float[]{12.90f, 7.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{2.80f, 7.80f, 2.00f}, new float[]{13.20f, 7.85f, 7.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=6.8: single segment (near center, visor widest here)
      new Box(new float[]{2.80f, 6.80f, 2.00f}, new float[]{13.20f, 6.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=5.8: back (connects to wall) + front (narrowed for dropped Y≈4.2)
      new Box(new float[]{2.80f, 5.80f, 8.00f}, new float[]{13.20f, 5.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.30f, 5.80f, 5.00f}, new float[]{12.70f, 5.85f, 8.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=4.8: back + front
      new Box(new float[]{2.90f, 4.80f, 8.00f}, new float[]{13.10f, 4.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.50f, 4.80f, 5.00f}, new float[]{12.50f, 4.85f, 8.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=3.8: back + front
      new Box(new float[]{3.30f, 3.80f, 8.00f}, new float[]{12.70f, 3.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{4.20f, 3.80f, 5.00f}, new float[]{11.80f, 3.85f, 8.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=2.8: back + front
      new Box(new float[]{4.00f, 2.80f, 9.00f}, new float[]{12.00f, 2.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{4.80f, 2.80f, 7.00f}, new float[]{11.20f, 2.85f, 9.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      // Y=1.8: single short segment
      new Box(new float[]{5.20f, 1.80f, 9.00f}, new float[]{10.80f, 1.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
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
      // Horizontal louver slats — same Z-segmented layout as HORIZONTAL_LOUVERED list
      new Box(new float[]{6.00f, 10.80f, 9.00f}, new float[]{10.00f, 10.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{4.30f, 10.80f, 5.00f}, new float[]{11.70f, 10.85f, 9.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.50f, 10.80f, 2.00f}, new float[]{12.50f, 10.85f, 5.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{4.30f, 9.80f, 7.00f}, new float[]{11.70f, 9.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.10f, 9.80f, 2.00f}, new float[]{12.90f, 9.85f, 7.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.50f, 8.80f, 7.00f}, new float[]{12.50f, 8.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{2.80f, 8.80f, 2.00f}, new float[]{13.20f, 8.85f, 7.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.10f, 7.80f, 7.00f}, new float[]{12.90f, 7.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{2.80f, 7.80f, 2.00f}, new float[]{13.20f, 7.85f, 7.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{2.80f, 6.80f, 2.00f}, new float[]{13.20f, 6.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{2.80f, 5.80f, 8.00f}, new float[]{13.20f, 5.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.30f, 5.80f, 5.00f}, new float[]{12.70f, 5.85f, 8.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{2.90f, 4.80f, 8.00f}, new float[]{13.10f, 4.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.50f, 4.80f, 5.00f}, new float[]{12.50f, 4.85f, 8.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.30f, 3.80f, 8.00f}, new float[]{12.70f, 3.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{4.20f, 3.80f, 5.00f}, new float[]{11.80f, 3.85f, 8.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{4.00f, 2.80f, 9.00f}, new float[]{12.00f, 2.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{4.80f, 2.80f, 7.00f}, new float[]{11.20f, 2.85f, 9.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{5.20f, 1.80f, 9.00f}, new float[]{10.80f, 1.85f, 11.00f}, true, HORIZONTAL_LOUVER_EXTRA_TILT),
      new Box(new float[]{3.15f, 4.10f, 2.00f}, new float[]{3.20f, 8.10f, 11.00f}, true),
      new Box(new float[]{4.15f, 2.50f, 2.00f}, new float[]{4.20f, 9.70f, 11.00f}, true),
      new Box(new float[]{5.15f, 1.90f, 2.00f}, new float[]{5.20f, 10.30f, 11.00f}, true),
      new Box(new float[]{6.15f, 1.30f, 2.00f}, new float[]{6.20f, 10.90f, 11.00f}, true),
      new Box(new float[]{7.25f, 0.90f, 2.00f}, new float[]{7.30f, 11.30f, 11.00f}, true),
      new Box(new float[]{8.70f, 0.90f, 2.00f}, new float[]{8.75f, 11.30f, 11.00f}, true),
      new Box(new float[]{9.80f, 1.30f, 2.00f}, new float[]{9.85f, 10.90f, 11.00f}, true),
      new Box(new float[]{10.80f, 1.90f, 2.00f}, new float[]{10.85f, 10.30f, 11.00f}, true),
      new Box(new float[]{11.80f, 2.50f, 2.00f}, new float[]{11.85f, 9.70f, 11.00f}, true),
      new Box(new float[]{12.80f, 4.10f, 2.00f}, new float[]{12.85f, 8.10f, 11.00f}, true),
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

  // --- Scaling utility for generating 8-inch section data from 12-inch ---

  private static final float SCALE_8_INCH = 8.0f / 12.0f; // 0.667
  private static final float CENTER_X = 8.0f;
  private static final float CENTER_Y = 6.0f;

  /**
   * Rotates a list of boxes 90° around the Z axis by swapping X and Y coordinates
   * relative to the section center (8, 6). Used for horizontal signal body/door geometry
   * where the back taper should be top-to-bottom instead of left-to-right.
   */
  private static List<Box> rotateBoxes90Z(List<Box> source) {
    List<Box> result = new ArrayList<>();
    for (Box box : source) {
      // Swap X↔Y relative to center: newX = centerX + (oldY - centerY), newY = centerY + (oldX - centerX)
      float x1 = CENTER_X + (box.from[1] - CENTER_Y);
      float y1 = CENTER_Y + (box.from[0] - CENTER_X);
      float x2 = CENTER_X + (box.to[1] - CENTER_Y);
      float y2 = CENTER_Y + (box.to[0] - CENTER_X);
      result.add(new Box(
          new float[]{Math.min(x1, x2), Math.min(y1, y2), box.from[2]},
          new float[]{Math.max(x1, x2), Math.max(y1, y2), box.to[2]}));
    }
    return result;
  }

  /**
   * Scales a list of boxes by scaling X and Y coordinates relative to the section center (8, 6).
   * Z coordinates are preserved (appropriate for body/door geometry where back taper depth is
   * consistent across sizes).
   */
  private static List<Box> scaleBoxes(List<Box> source, float scale) {
    return scaleBoxes(source, scale, false);
  }

  /**
   * Scales a list of boxes by scaling X and Y coordinates relative to the section center (8, 6).
   * When scaleZ is true, Z coordinates are also scaled relative to the body face (Z=11), which
   * keeps visor depth proportional to the section size.
   */
  private static final float CENTER_Z_VISOR = 11.0f;

  private static List<Box> scaleBoxes(List<Box> source, float scale, boolean scaleZ) {
    List<Box> result = new ArrayList<>();
    for (Box box : source) {
      float x1 = CENTER_X + (box.from[0] - CENTER_X) * scale;
      float y1 = CENTER_Y + (box.from[1] - CENTER_Y) * scale;
      float x2 = CENTER_X + (box.to[0] - CENTER_X) * scale;
      float y2 = CENTER_Y + (box.to[1] - CENTER_Y) * scale;
      float z1 = scaleZ ? CENTER_Z_VISOR + (box.from[2] - CENTER_Z_VISOR) * scale : box.from[2];
      float z2 = scaleZ ? CENTER_Z_VISOR + (box.to[2] - CENTER_Z_VISOR) * scale : box.to[2];
      result.add(new Box(new float[]{x1, y1, z1}, new float[]{x2, y2, z2}, box.innerOnly, box.extraTiltDegrees));
    }
    return result;
  }

  // --- 12-inch section data (standard) ---

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

  // --- Horizontal section data (body/door rotated 90° around Z) ---

  public static final List<Box> SIGNAL_BODY_HORIZONTAL_VERTEX_DATA =
      rotateBoxes90Z(SIGNAL_BODY_VERTEX_DATA);
  public static final List<Box> SIGNAL_DOOR_HORIZONTAL_VERTEX_DATA =
      rotateBoxes90Z(SIGNAL_DOOR_VERTEX_DATA);

  // --- 8-inch section data (scaled from 12-inch) ---

  public static final List<Box> SIGNAL_BODY_8INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_BODY_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> SIGNAL_DOOR_8INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_DOOR_VERTEX_DATA, SCALE_8_INCH, true);

  public static final List<Box> CIRCLE_VISOR_8INCH_VERTEX_DATA =
      scaleBoxes(CIRCLE_VISOR_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> TUNNEL_VISOR_8INCH_VERTEX_DATA =
      scaleBoxes(TUNNEL_VISOR_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> CAP_VISOR_8INCH_VERTEX_DATA =
      scaleBoxes(CAP_VISOR_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> BOTH_LOUVERED_VISOR_8INCH_VERTEX_DATA =
      scaleBoxes(BOTH_LOUVERED_VISOR_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> VERTICAL_LOUVERED_VISOR_8INCH_VERTEX_DATA =
      scaleBoxes(VERTICAL_LOUVERED_VISOR_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> HORIZONTAL_LOUVERED_VISOR_8INCH_VERTEX_DATA =
      scaleBoxes(HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> NONE_VISOR_8INCH_VERTEX_DATA =
      scaleBoxes(NONE_VISOR_VERTEX_DATA, SCALE_8_INCH, true);

  // --- 4-inch section data (scaled from 12-inch) ---

  private static final float SCALE_4_INCH = 4.0f / 12.0f; // 0.333

  public static final List<Box> SIGNAL_BODY_4INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_BODY_VERTEX_DATA, SCALE_4_INCH, true);
  public static final List<Box> SIGNAL_DOOR_4INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_DOOR_VERTEX_DATA, SCALE_4_INCH, true);

  public static final List<Box> CIRCLE_VISOR_4INCH_VERTEX_DATA =
      scaleBoxes(CIRCLE_VISOR_VERTEX_DATA, SCALE_4_INCH, true);
  public static final List<Box> TUNNEL_VISOR_4INCH_VERTEX_DATA =
      scaleBoxes(TUNNEL_VISOR_VERTEX_DATA, SCALE_4_INCH, true);
  public static final List<Box> CAP_VISOR_4INCH_VERTEX_DATA =
      scaleBoxes(CAP_VISOR_VERTEX_DATA, SCALE_4_INCH, true);
  public static final List<Box> BOTH_LOUVERED_VISOR_4INCH_VERTEX_DATA =
      scaleBoxes(BOTH_LOUVERED_VISOR_VERTEX_DATA, SCALE_4_INCH, true);
  public static final List<Box> VERTICAL_LOUVERED_VISOR_4INCH_VERTEX_DATA =
      scaleBoxes(VERTICAL_LOUVERED_VISOR_VERTEX_DATA, SCALE_4_INCH, true);
  public static final List<Box> HORIZONTAL_LOUVERED_VISOR_4INCH_VERTEX_DATA =
      scaleBoxes(HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA, SCALE_4_INCH, true);
  public static final List<Box> NONE_VISOR_4INCH_VERTEX_DATA =
      scaleBoxes(NONE_VISOR_VERTEX_DATA, SCALE_4_INCH, true);

  // --- Bubbled (Eagle-style) section housing ---

  /**
   * Builds the bubbled (Eagle-style) section housing: a deep rear lobe that lives strictly
   * <em>behind</em> the 12x12 face (x 2..14, y 0..12, face plane z=11), so the signal stays a
   * clean rectangle from the front. The lobe's back depth follows a circular dome along the
   * section's height — deepest at the belly, pinching to a shallow seam flange at the section's
   * top/bottom edges, which is what produces the classic Eagle "stacked bubbles" waist where
   * sections meet — and each height band steps narrower toward the back so the lobe also rounds
   * off in plan view. Shares the door frame plane and hinge hardware with the standard housing;
   * doors, visors, bulbs, and mounts are untouched.
   */
  private static List<Box> buildBubbledBody() {
    final float faceZ = 11.0f;       // door frame plane; nothing renders in front of it
    final float half = 6.0f;         // face half-extent (12x12 section) — never exceeded
    final float edgeDepth = 1.6f;    // depth remaining at the seam flange (top/bottom edges)
    final float bellyDepth = 7.6f;   // depth at the section's belly (the bubble apex)
    // Half-band edges outward from the section centerline (y=6); mirrored below center.
    final float[] bandEdges = {0.0f, 1.0f, 2.4f, 3.8f, 4.8f, 5.6f, 6.0f};
    // Rear taper steps per band: {fraction of the band's depth, x half-extent} — the lobe
    // steps in from full width toward a narrower back so it reads rounded from above too.
    final float[][] taper = {{0.60f, 6.0f}, {0.88f, 5.1f}, {1.00f, 3.9f}};

    // Assemble the height bands: the center band straddles the centerline once; the rest mirror.
    List<float[]> bands = new ArrayList<>(); // {y1, y2, outerOffsetFromCenter}
    for (int i = 0; i < bandEdges.length - 1; i++) {
      float o1 = bandEdges[i];
      float o2 = bandEdges[i + 1];
      if (i == 0) {
        bands.add(new float[]{CENTER_Y - o2, CENTER_Y + o2, o2});
      } else {
        bands.add(new float[]{CENTER_Y + o1, CENTER_Y + o2, o2});
        bands.add(new float[]{CENTER_Y - o2, CENTER_Y - o1, o2});
      }
    }

    List<Box> boxes = new ArrayList<>();
    for (float[] band : bands) {
      float y1 = band[0];
      float y2 = band[1];
      // Depth at the band's outer edge, inscribed in the dome curve (circle in the y-z plane).
      float t = band[2] / half;
      float depth = edgeDepth
          + (bellyDepth - edgeDepth) * (float) Math.sqrt(Math.max(0.0f, 1.0f - t * t));
      float zStart = faceZ;
      for (float[] step : taper) {
        float zEnd = faceZ + depth * step[0];
        boxes.add(new Box(new float[]{CENTER_X - step[1], y1, zStart},
            new float[]{CENTER_X + step[1], y2, zEnd}));
        zStart = zEnd;
      }
    }
    // Same door-hinge hardware as the standard housing so the styles read as siblings.
    boxes.add(new Box(new float[]{1.80f, 1.20f, 10.80f}, new float[]{2.40f, 1.60f, 11.50f}));
    boxes.add(new Box(new float[]{1.80f, 10.20f, 10.80f}, new float[]{2.40f, 10.60f, 11.50f}));
    return boxes;
  }

  public static final List<Box> SIGNAL_BODY_BUBBLED_VERTEX_DATA = buildBubbledBody();
  public static final List<Box> SIGNAL_BODY_BUBBLED_HORIZONTAL_VERTEX_DATA =
      rotateBoxes90Z(SIGNAL_BODY_BUBBLED_VERTEX_DATA);
  public static final List<Box> SIGNAL_BODY_BUBBLED_8INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_BODY_BUBBLED_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> SIGNAL_BODY_BUBBLED_4INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_BODY_BUBBLED_VERTEX_DATA, SCALE_4_INCH, true);

  // --- PV (programmable visibility) section housing ---

  /**
   * How deep the PV housing runs behind the door plane, in model units for a 12-inch section: as
   * deep as the section is tall, which is what the photographs of 3M and McCain heads show. The
   * rear therefore sits at z = 23, seven units past the block's back face; a deep casting really
   * does reach into whatever is behind it, and the render box already covers a block each way.
   */
  public static final float PV_BODY_DEPTH = 12.0f;

  /** The door plane every housing style starts at. */
  public static final float BODY_FACE_Z = 11.0f;

  /** The PV housing's rear plane for a 12-inch section. */
  public static final float PV_BODY_REAR_Z = BODY_FACE_Z + PV_BODY_DEPTH;

  /**
   * Builds the PV housing: a squared-off box as deep as it is tall, with three shallow horizontal
   * grooves along each side running front to back and a two-step chamfer on the rear edges --
   * the silhouette of an optically programmed head seen from the side. The face plane, door and
   * hinge hardware match the standard housing so the styles read as siblings on one head.
   *
   * <p>Built as touching, never overlapping, boxes: the grooves are bands inset from the sides,
   * not cuts, and the chamfer is two successively narrower rear slices. Grooves sit at a quarter,
   * half and three quarters of the height, clear of the hinge hardware at the top and bottom.</p>
   *
   * <p>Two lists come out of one layout. The signal renderer draws with world light and no
   * directional shading, so a recess that is only geometry is invisible on a flat-coloured
   * housing; the grooves and the chamfer are therefore returned separately and drawn a shade
   * darker than the body, which is what makes them read as a recess and a bevel.</p>
   *
   * @param shadedParts false for the body proper (front band, full-width bands, hinges), true
   *                    for the parts drawn darker (groove bands, chamfer slices)
   */
  private static List<Box> buildPvBody(boolean shadedParts) {
    final float faceZ = BODY_FACE_Z;
    final float rearZ = PV_BODY_REAR_Z;
    final float frontBand = 1.0f;     // plain band behind the door frame, before the grooves start
    final float grooveHalf = 0.35f;   // half-height of a groove
    final float grooveInset = 0.3f;   // how far a groove sits in from the side
    final float chamferStep = 0.4f;   // each of the two rear chamfer slices, in z and in inset
    final float left = 2.0f;
    final float right = 14.0f;
    final float bottom = 0.0f;
    final float top = 12.0f;
    final float[] grooveCentres = {3.0f, 6.0f, 9.0f};

    List<Box> body = new ArrayList<>();
    List<Box> shaded = new ArrayList<>();
    // Plain band directly behind the door frame.
    body.add(new Box(new float[]{left, bottom, faceZ}, new float[]{right, top, faceZ + frontBand}));

    // Ribbed body: full-width bands alternating with inset groove bands, up the height.
    float ribbedFrom = faceZ + frontBand;
    float ribbedTo = rearZ - 2 * chamferStep;
    float y = bottom;
    for (float centre : grooveCentres) {
      float grooveBottom = centre - grooveHalf;
      float grooveTop = centre + grooveHalf;
      body.add(new Box(new float[]{left, y, ribbedFrom}, new float[]{right, grooveBottom, ribbedTo}));
      shaded.add(new Box(new float[]{left + grooveInset, grooveBottom, ribbedFrom},
          new float[]{right - grooveInset, grooveTop, ribbedTo}));
      y = grooveTop;
    }
    body.add(new Box(new float[]{left, y, ribbedFrom}, new float[]{right, top, ribbedTo}));

    // Rear chamfer: two slices, each a step further in on every edge.
    shaded.add(new Box(
        new float[]{left + chamferStep, bottom + chamferStep, ribbedTo},
        new float[]{right - chamferStep, top - chamferStep, ribbedTo + chamferStep}));
    shaded.add(new Box(
        new float[]{left + 2 * chamferStep, bottom + 2 * chamferStep, ribbedTo + chamferStep},
        new float[]{right - 2 * chamferStep, top - 2 * chamferStep, rearZ}));

    // Same door-hinge hardware as the standard housing.
    body.add(new Box(new float[]{1.80f, 1.20f, 10.80f}, new float[]{2.40f, 1.60f, 11.50f}));
    body.add(new Box(new float[]{1.80f, 10.20f, 10.80f}, new float[]{2.40f, 10.60f, 11.50f}));
    return shadedParts ? shaded : body;
  }

  public static final List<Box> SIGNAL_BODY_PV_VERTEX_DATA = buildPvBody(false);
  public static final List<Box> SIGNAL_BODY_PV_HORIZONTAL_VERTEX_DATA =
      rotateBoxes90Z(SIGNAL_BODY_PV_VERTEX_DATA);
  public static final List<Box> SIGNAL_BODY_PV_8INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_BODY_PV_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> SIGNAL_BODY_PV_4INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_BODY_PV_VERTEX_DATA, SCALE_4_INCH, true);

  /** The PV housing's grooves and rear chamfer, drawn a shade darker than the body. */
  public static final List<Box> SIGNAL_BODY_PV_SHADE_VERTEX_DATA = buildPvBody(true);
  public static final List<Box> SIGNAL_BODY_PV_SHADE_HORIZONTAL_VERTEX_DATA =
      rotateBoxes90Z(SIGNAL_BODY_PV_SHADE_VERTEX_DATA);
  public static final List<Box> SIGNAL_BODY_PV_SHADE_8INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_BODY_PV_SHADE_VERTEX_DATA, SCALE_8_INCH, true);
  public static final List<Box> SIGNAL_BODY_PV_SHADE_4INCH_VERTEX_DATA =
      scaleBoxes(SIGNAL_BODY_PV_SHADE_VERTEX_DATA, SCALE_4_INCH, true);

  /**
   * The part of a housing drawn a shade darker than the rest, so recesses read under the flat
   * lighting the signal renderer uses, or {@code null} for a style that has none.
   *
   * @param style       the section's housing style
   * @param horizontal  whether the head is in horizontal orientation
   * @param sectionSize the section size in inches: 12, 8 or 4
   *
   * @return the shaded boxes, or null
   */
  public static List<Box> resolveBodyShadeData(TrafficSignalBodyStyle style, boolean horizontal,
      int sectionSize) {
    if (style != TrafficSignalBodyStyle.PV) {
      return null;
    }
    if (horizontal) return SIGNAL_BODY_PV_SHADE_HORIZONTAL_VERTEX_DATA;
    return selectVisorData(SIGNAL_BODY_PV_SHADE_VERTEX_DATA,
        SIGNAL_BODY_PV_SHADE_8INCH_VERTEX_DATA, SIGNAL_BODY_PV_SHADE_4INCH_VERTEX_DATA,
        sectionSize);
  }

  /**
   * Resolves the housing geometry for a body style, orientation and section size.
   *
   * <p>Horizontal heads are 12-inch only, as the door and body data for them are; the size is
   * ignored for them, matching how the renderer has always chosen.</p>
   *
   * @param style       the section's housing style
   * @param horizontal  whether the head is in horizontal orientation
   * @param sectionSize the section size in inches: 12, 8 or 4
   *
   * @return the body boxes, never null
   */
  public static List<Box> resolveBodyData(TrafficSignalBodyStyle style, boolean horizontal,
      int sectionSize) {
    switch (style) {
      case BUBBLED:
        if (horizontal) return SIGNAL_BODY_BUBBLED_HORIZONTAL_VERTEX_DATA;
        return selectVisorData(SIGNAL_BODY_BUBBLED_VERTEX_DATA,
            SIGNAL_BODY_BUBBLED_8INCH_VERTEX_DATA, SIGNAL_BODY_BUBBLED_4INCH_VERTEX_DATA,
            sectionSize);
      case PV:
        if (horizontal) return SIGNAL_BODY_PV_HORIZONTAL_VERTEX_DATA;
        return selectVisorData(SIGNAL_BODY_PV_VERTEX_DATA, SIGNAL_BODY_PV_8INCH_VERTEX_DATA,
            SIGNAL_BODY_PV_4INCH_VERTEX_DATA, sectionSize);
      case STANDARD:
      default:
        if (horizontal) return SIGNAL_BODY_HORIZONTAL_VERTEX_DATA;
        return selectVisorData(SIGNAL_BODY_VERTEX_DATA, SIGNAL_BODY_8INCH_VERTEX_DATA,
            SIGNAL_BODY_4INCH_VERTEX_DATA, sectionSize);
    }
  }

  /**
   * Where a section's housing ends at the back, in model units, for a style and size: the plane
   * the mount hardware bolts to. Standard and bubbled housings report the block's back face, as
   * the mounts have always assumed; a PV housing reports its own deeper rear, scaled with the
   * section like its geometry is.
   *
   * @param style       the section's housing style
   * @param sectionSize the section size in inches
   *
   * @return the rear plane's z
   */
  public static float bodyRearZ(TrafficSignalBodyStyle style, int sectionSize) {
    if (style == TrafficSignalBodyStyle.PV) {
      return BODY_FACE_Z + PV_BODY_DEPTH * (sectionSize / 12.0f);
    }
    return 16.0f;
  }

  /** Picks the 12-, 8- or 4-inch variant of a visor for the given section size. */
  private static List<Box> selectVisorData(List<Box> data12, List<Box> data8, List<Box> data4,
      int sectionSize) {
    if (sectionSize <= 4) {
      return data4;
    }
    if (sectionSize <= 8) {
      return data8;
    }
    return data12;
  }

  /**
   * Resolves the visor geometry for a visor type at a section size, or {@code null} if the type
   * has none.
   *
   * <p>This lives here rather than in a renderer because more than one thing wears a signal
   * visor: the signal heads and the school zone beacons both select from this same set, and a
   * mapping kept privately by one of them is a mapping the other silently gets wrong.
   *
   * <p>The two Barlo types have no geometry of their own — they borrow the tunnel and circle
   * shells and are told apart by the strobe the signal renderer draws over them. A caller that
   * does not draw that strobe should not offer them, which is why the beacons do not.
   */
  public static List<Box> resolveVisorData(TrafficSignalVisorType visorType, int sectionSize) {
    switch (visorType) {
      case CIRCLE:
        return selectVisorData(CIRCLE_VISOR_VERTEX_DATA, CIRCLE_VISOR_8INCH_VERTEX_DATA,
            CIRCLE_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case TUNNEL:
        return selectVisorData(TUNNEL_VISOR_VERTEX_DATA, TUNNEL_VISOR_8INCH_VERTEX_DATA,
            TUNNEL_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case CUTAWAY:
        return selectVisorData(CAP_VISOR_VERTEX_DATA, CAP_VISOR_8INCH_VERTEX_DATA,
            CAP_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case BOTH_LOUVERED:
        return selectVisorData(BOTH_LOUVERED_VISOR_VERTEX_DATA,
            BOTH_LOUVERED_VISOR_8INCH_VERTEX_DATA, BOTH_LOUVERED_VISOR_4INCH_VERTEX_DATA,
            sectionSize);
      case VERTICAL_LOUVERED:
        return selectVisorData(VERTICAL_LOUVERED_VISOR_VERTEX_DATA,
            VERTICAL_LOUVERED_VISOR_8INCH_VERTEX_DATA,
            VERTICAL_LOUVERED_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case HORIZONTAL_LOUVERED:
        return selectVisorData(HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA,
            HORIZONTAL_LOUVERED_VISOR_8INCH_VERTEX_DATA,
            HORIZONTAL_LOUVERED_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case BARLO:
        return selectVisorData(TUNNEL_VISOR_VERTEX_DATA, TUNNEL_VISOR_8INCH_VERTEX_DATA,
            TUNNEL_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case BARLO_VERTICAL:
        return selectVisorData(CIRCLE_VISOR_VERTEX_DATA, CIRCLE_VISOR_8INCH_VERTEX_DATA,
            CIRCLE_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case PROGRAMMABLE:
        // The full-tube shell of a programmed head; what makes it programmable is the mask the
        // signal renderer draws over the lens, not the visor.
        return selectVisorData(CIRCLE_VISOR_VERTEX_DATA, CIRCLE_VISOR_8INCH_VERTEX_DATA,
            CIRCLE_VISOR_4INCH_VERTEX_DATA, sectionSize);
      case NONE:
        return selectVisorData(NONE_VISOR_VERTEX_DATA, NONE_VISOR_8INCH_VERTEX_DATA,
            NONE_VISOR_4INCH_VERTEX_DATA, sectionSize);
      default:
        return null;
    }
  }
}
