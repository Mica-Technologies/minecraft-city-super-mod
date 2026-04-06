package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper.Box;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrafficSignalVertexData}, verifying that all static vertex data lists are
 * well-formed and that box coordinates are valid (from < to in each dimension).
 */
class TrafficSignalVertexDataTest {

  // region: Non-null and non-empty checks

  @Test
  void tunnelVisorDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.TUNNEL_VISOR_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.TUNNEL_VISOR_VERTEX_DATA.isEmpty());
  }

  @Test
  void noneVisorDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.NONE_VISOR_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.NONE_VISOR_VERTEX_DATA.isEmpty());
  }

  @Test
  void verticalLouveredVisorDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_VERTEX_DATA.isEmpty());
  }

  @Test
  void horizontalLouveredVisorDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA.isEmpty());
  }

  @Test
  void bothLouveredVisorDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.BOTH_LOUVERED_VISOR_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.BOTH_LOUVERED_VISOR_VERTEX_DATA.isEmpty());
  }

  @Test
  void circleVisorDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.CIRCLE_VISOR_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.CIRCLE_VISOR_VERTEX_DATA.isEmpty());
  }

  @Test
  void capVisorDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.CAP_VISOR_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.CAP_VISOR_VERTEX_DATA.isEmpty());
  }

  @Test
  void signalBodyDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA.isEmpty());
  }

  @Test
  void signalDoorDataIsNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.SIGNAL_DOOR_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.SIGNAL_DOOR_VERTEX_DATA.isEmpty());
  }

  // endregion

  // region: 8-inch scaled data non-null checks

  @Test
  void eightInchVariantsAreNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.SIGNAL_BODY_8INCH_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.SIGNAL_BODY_8INCH_VERTEX_DATA.isEmpty());
    assertNotNull(TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA.isEmpty());
    assertNotNull(TrafficSignalVertexData.TUNNEL_VISOR_8INCH_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.TUNNEL_VISOR_8INCH_VERTEX_DATA.isEmpty());
  }

  // endregion

  // region: 4-inch scaled data non-null checks

  @Test
  void fourInchVariantsAreNonNullAndNonEmpty() {
    assertNotNull(TrafficSignalVertexData.SIGNAL_BODY_4INCH_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.SIGNAL_BODY_4INCH_VERTEX_DATA.isEmpty());
    assertNotNull(TrafficSignalVertexData.CIRCLE_VISOR_4INCH_VERTEX_DATA);
    assertFalse(TrafficSignalVertexData.CIRCLE_VISOR_4INCH_VERTEX_DATA.isEmpty());
  }

  // endregion

  // region: Box coordinate validity (from < to)

  @Test
  void allBoxesInAllStaticFieldsHaveValidCoordinates() throws IllegalAccessException {
    List<String> violations = new ArrayList<>();

    for (Field field : TrafficSignalVertexData.class.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())
          && Modifier.isPublic(field.getModifiers())
          && List.class.isAssignableFrom(field.getType())) {
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Box> boxes = (List<Box>) field.get(null);
        if (boxes == null) {
          continue;
        }
        for (int i = 0; i < boxes.size(); i++) {
          Box box = boxes.get(i);
          for (int dim = 0; dim < 3; dim++) {
            if (box.from[dim] > box.to[dim]) {
              String dimName = dim == 0 ? "X" : dim == 1 ? "Y" : "Z";
              violations.add(String.format("%s[%d] %s: from=%.2f > to=%.2f",
                  field.getName(), i, dimName, box.from[dim], box.to[dim]));
            }
          }
        }
      }
    }

    assertTrue(violations.isEmpty(),
        "Found boxes with from > to:\n" + String.join("\n", violations));
  }

  // endregion

  // region: Circle visor generation

  @Test
  void optimizedCircleVisorProducesExpectedSegments() {
    List<Box> circleVisor = TrafficSignalVertexData.getOptimizedCircleVisor();
    assertNotNull(circleVisor);
    // Method uses 16 segments, so should produce 16 boxes
    assertEquals(16, circleVisor.size(),
        "getOptimizedCircleVisor should produce exactly 16 segments");
  }

  @Test
  void optimizedCircleVisorBoxesHavePositiveDepth() {
    List<Box> circleVisor = TrafficSignalVertexData.getOptimizedCircleVisor();
    for (int i = 0; i < circleVisor.size(); i++) {
      Box box = circleVisor.get(i);
      // Z dimension represents depth (visor protrusion)
      assertTrue(box.to[2] > box.from[2],
          "Circle visor segment " + i + " should have positive Z depth");
    }
  }

  // endregion

  // region: Horizontal rotation produces same count

  @Test
  void horizontalBodyHasSameBoxCountAsStandard() {
    assertEquals(
        TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA.size(),
        TrafficSignalVertexData.SIGNAL_BODY_HORIZONTAL_VERTEX_DATA.size(),
        "Horizontal body should have same number of boxes as standard body");
  }

  @Test
  void horizontalDoorHasSameBoxCountAsStandard() {
    assertEquals(
        TrafficSignalVertexData.SIGNAL_DOOR_VERTEX_DATA.size(),
        TrafficSignalVertexData.SIGNAL_DOOR_HORIZONTAL_VERTEX_DATA.size(),
        "Horizontal door should have same number of boxes as standard door");
  }

  // endregion

  // region: Scaled variants preserve box count

  @Test
  void eightInchBodyHasSameBoxCountAsStandard() {
    assertEquals(
        TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA.size(),
        TrafficSignalVertexData.SIGNAL_BODY_8INCH_VERTEX_DATA.size(),
        "8-inch body should have same box count as 12-inch body");
  }

  @Test
  void fourInchBodyHasSameBoxCountAsStandard() {
    assertEquals(
        TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA.size(),
        TrafficSignalVertexData.SIGNAL_BODY_4INCH_VERTEX_DATA.size(),
        "4-inch body should have same box count as 12-inch body");
  }

  // endregion
}
