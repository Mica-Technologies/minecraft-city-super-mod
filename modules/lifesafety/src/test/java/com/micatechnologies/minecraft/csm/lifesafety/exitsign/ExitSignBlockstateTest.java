package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Legend;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.properties.IProperty;
import org.junit.jupiter.api.Test;

/**
 * Holds the generated exit sign assets (gen_exit_signs.py) to the Java specs, which the generator
 * repeats as its STYLES catalogue: a multipart condition naming a property the block does not
 * have, or a value it does not offer, fails the whole blockstate at load, and an item icon the
 * block asks for that was never written shows the missing-model cube.
 */
class ExitSignBlockstateTest {

  /** Every exit sign block, with its registry name. */
  private static final Map<String, ExitSignSpec> MODELLED = new HashMap<>();

  static {
    MODELLED.put("exit_sign_traditional_flat", BlockExitSignTraditionalFlat.SPEC);
    MODELLED.put("exit_sign_traditional_rounded", BlockExitSignTraditionalRounded.SPEC);
    MODELLED.put("exit_sign_combo_compact", BlockExitSignCombo.SPEC);
    MODELLED.put("exit_sign_diecast", BlockExitSignDieCast.SPEC);
    MODELLED.put("exit_sign_vandal_resistant", BlockExitSignVandalResistant.SPEC);
    MODELLED.put("exit_sign_photoluminescent", BlockExitSignPhotoluminescent.SPEC);
    MODELLED.put("exit_sign_explosion_proof", BlockExitSignExplosionProof.SPEC);
  }

  private static final List<String> FACINGS = Arrays.asList("north", "east", "south", "west");

  @Test
  void everyConditionNamesAnOfferedValue() throws Exception {
    for (Map.Entry<String, ExitSignSpec> e : MODELLED.entrySet()) {
      ExitSignSpec spec = e.getValue();
      Map<String, IProperty<?>> properties = new HashMap<>();
      for (IProperty<?> p : spec.properties()) {
        properties.put(p.getName(), p);
      }
      JsonObject state = read("assets/csm/blockstates/" + e.getKey() + ".json");
      int parts = 0;
      for (JsonElement part : state.getAsJsonArray("multipart")) {
        parts++;
        for (Map.Entry<String, JsonElement> condition :
            part.getAsJsonObject().getAsJsonObject("when").entrySet()) {
          String key = condition.getKey();
          for (String value : condition.getValue().getAsString().split("\\|")) {
            if (key.equals("facing")) {
              assertTrue(FACINGS.contains(value), e.getKey() + " facing " + value);
            } else if (key.equals("powered")) {
              assertTrue(spec.isMainsPowered(), e.getKey() + " has no powered property");
            } else {
              IProperty<?> property = properties.get(key);
              assertNotNull(property, e.getKey() + " has no property " + key);
              assertTrue(property.parseValue(value).isPresent(),
                  e.getKey() + " does not offer " + key + "=" + value);
            }
          }
        }
      }
      assertTrue(parts > 0, e.getKey() + " has no parts");
    }
  }

  @Test
  void everyIconTheBlockAsksForExists() {
    for (Map.Entry<String, ExitSignSpec> e : MODELLED.entrySet()) {
      ExitSignSpec spec = e.getValue();
      for (Legend legend : spec.getLegends()) {
        for (Letters letters : spec.getLetterColours()) {
          for (Housing housing : spec.getHousings()) {
            for (Heads heads : spec.getHeadTypes()) {
              // the same name AbstractBlockExitSign.getItemModelName builds
              String name = e.getKey() + "_" + legend.getName() + "_" + letters.getName() + "_"
                  + housing.getName() + "_" + heads.getName();
              assertNotNull(getClass().getClassLoader()
                      .getResource("assets/csm/models/item/" + name + ".json"),
                  "missing item model " + name);
            }
          }
        }
      }
    }
  }

  private static JsonObject read(String path) throws Exception {
    InputStream in = ExitSignBlockstateTest.class.getClassLoader().getResourceAsStream(path);
    assertNotNull(in, path + " is not on the test classpath");
    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      return new JsonParser().parse(reader).getAsJsonObject();
    }
  }
}
