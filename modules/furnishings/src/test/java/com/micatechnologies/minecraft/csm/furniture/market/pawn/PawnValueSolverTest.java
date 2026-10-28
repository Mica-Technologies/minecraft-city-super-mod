package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Input;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Recipe;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Result;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Source;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The pawn shop's recipe walk: values come from the cheapest way to make a thing, loops settle,
 * fixed values never move, and inputs nothing makes get the default.
 */
class PawnValueSolverTest {

  private static final double E = 1e-9;

  private static Recipe r(String out, int count, double extra, Input... inputs) {
    return new Recipe(out, count, inputs, extra, "crafting");
  }

  private static Input in(String key, int count) {
    return new Input(key, count);
  }

  @Test
  void dividesByOutputCountAndAddsSmeltCost() {
    Map<String, Double> fixed = new HashMap<>();
    fixed.put("mc:log", 0.4);
    fixed.put("mc:iron_ore@0", 2.0);
    List<Recipe> recipes = Arrays.asList(
        r("mc:planks@0", 4, 0, in("mc:log", 1)),
        r("mc:stick@0", 4, 0, in("mc:planks", 2)),
        r("mc:iron_ingot@0", 1, 0.05, in("mc:iron_ore@0", 1)),
        r("mc:iron_pickaxe@0", 1, 0, in("mc:iron_ingot@0", 3), in("mc:stick@0", 2)));
    Result res = PawnValueSolver.solve(fixed, recipes, 1.0);
    assertEquals(0.1, res.value("mc:planks@0"), E);
    assertEquals(0.05, res.value("mc:stick@0"), E);
    assertEquals(2.05, res.value("mc:iron_ingot@0"), E);
    assertEquals(3 * 2.05 + 2 * 0.05, res.value("mc:iron_pickaxe@0"), E);
    assertEquals(Source.RECIPE, res.source("mc:iron_pickaxe@0"));
    // A key fixed for every subtype prices each one.
    assertEquals(0.4, res.value("mc:log@2"), E);
    assertEquals(Source.FIXED, res.source("mc:log@2"));
  }

  @Test
  void cheapestRecipeAndCheapestAlternativeWin() {
    Map<String, Double> fixed = new HashMap<>();
    fixed.put("a@0", 5.0);
    fixed.put("b@0", 1.0);
    fixed.put("c@0", 3.0);
    List<Recipe> recipes = Arrays.asList(
        r("x@0", 1, 0, in("a@0", 1)),
        r("x@0", 1, 0, new Input(new String[]{"c@0", "b@0"}, new String[2], 2)));
    Result res = PawnValueSolver.solve(fixed, recipes, 1.0);
    assertEquals(2.0, res.value("x@0"), E);
  }

  @Test
  void blockAndIngotLoopSettles() {
    Map<String, Double> fixed = new HashMap<>();
    fixed.put("ore@0", 2.0);
    List<Recipe> recipes = Arrays.asList(
        r("ingot@0", 1, 0, in("ore@0", 1)),
        r("block@0", 1, 0, in("ingot@0", 9)),
        r("ingot@0", 9, 0, in("block@0", 1)),
        r("nugget@0", 9, 0, in("ingot@0", 1)),
        r("ingot@0", 1, 0, in("nugget@0", 9)));
    Result res = PawnValueSolver.solve(fixed, recipes, 1.0);
    assertEquals(2.0, res.value("ingot@0"), E);
    assertEquals(18.0, res.value("block@0"), E);
    assertEquals(2.0 / 9, res.value("nugget@0"), E);
    assertTrue(res.getPasses() < 10);
  }

  @Test
  void lossyLoopDriftsDownAndStops() {
    // x makes two y and one y makes one x: each lap halves both. It must stop, low.
    Map<String, Double> fixed = new HashMap<>();
    fixed.put("seed@0", 8.0);
    List<Recipe> recipes = Arrays.asList(
        r("x@0", 1, 0, in("seed@0", 1)),
        r("y@0", 2, 0, in("x@0", 1)),
        r("x@0", 1, 0, in("y@0", 1)));
    Result res = PawnValueSolver.solve(fixed, recipes, 1.0);
    assertTrue(res.value("x@0") < 0.01);
    assertTrue(res.getPasses() <= 2 * PawnValueSolver.MAX_PASSES);
  }

  @Test
  void fixedValuesNeverMove() {
    Map<String, Double> fixed = new HashMap<>();
    fixed.put("gem@0", 25.0);
    fixed.put("dust@0", 0.01);
    List<Recipe> recipes = Arrays.asList(r("gem@0", 1, 0, in("dust@0", 1)));
    Result res = PawnValueSolver.solve(fixed, recipes, 1.0);
    assertEquals(25.0, res.value("gem@0"), E);
    assertEquals(Source.FIXED, res.source("gem@0"));
  }

  @Test
  void containerRefundIsSubtracted() {
    Map<String, Double> fixed = new HashMap<>();
    fixed.put("milk_bucket@0", 6.5);
    fixed.put("bucket@0", 6.0);
    fixed.put("sugar@0", 0.1);
    Input milk = new Input(new String[]{"milk_bucket@0"}, new String[]{"bucket@0"}, 1);
    List<Recipe> recipes = Arrays.asList(r("cake@0", 1, 0, milk, in("sugar@0", 1)));
    Result res = PawnValueSolver.solve(fixed, recipes, 1.0);
    assertEquals(0.6, res.value("cake@0"), E);
  }

  @Test
  void inputNothingMakesGetsTheDefault() {
    List<Recipe> recipes = Arrays.asList(
        r("gadget@0", 1, 0, in("othermod:ore@0", 2), in("othermod:gem", 1)));
    Result res = PawnValueSolver.solve(new HashMap<>(), recipes, 1.5);
    assertEquals(1.5, res.value("othermod:ore@0"), E);
    assertEquals(Source.DEFAULT, res.source("othermod:ore@0"));
    assertEquals(Source.DEFAULT, res.source("othermod:gem@3"));
    assertEquals(4.5, res.value("gadget@0"), E);
  }

  @Test
  void noDefaultLeavesUnpricedAlone() {
    List<Recipe> recipes = Arrays.asList(r("gadget@0", 1, 0, in("othermod:ore@0", 1)));
    Result res = PawnValueSolver.solve(new HashMap<>(), recipes, 0);
    assertTrue(Double.isNaN(res.value("gadget@0")));
    assertNull(res.source("gadget@0"));
  }

  @Test
  void explainFollowsTheCheapestChain() {
    Map<String, Double> fixed = new HashMap<>();
    fixed.put("mc:log", 0.4);
    List<Recipe> recipes = new ArrayList<>();
    recipes.add(r("mc:planks@0", 4, 0, in("mc:log", 1)));
    recipes.add(r("mc:chest@0", 1, 0, in("mc:planks", 8)));
    Result res = PawnValueSolver.solve(fixed, recipes, 1.0);
    List<String> lines = PawnValueSolver.explain(res, "mc:chest@0", 6);
    assertTrue(lines.get(0).startsWith("mc:chest@0 = 0.8000"), lines.toString());
    assertTrue(lines.toString().contains("mc:log"), lines.toString());
  }

  @Test
  void defaultsFileParsesAndBlocksBedrock() {
    PawnValues values = PawnValues.defaults();
    assertTrue(values.getRaw().size() > 100);
    assertEquals(25.0, values.getRaw().get("minecraft:diamond"), E);
    assertTrue(values.isBlocked("minecraft:bedrock@0"));
    assertTrue(values.isBlocked("minecraft:spawn_egg@0"));
    assertFalse(values.isBlocked("minecraft:diamond@0"));
  }
}
