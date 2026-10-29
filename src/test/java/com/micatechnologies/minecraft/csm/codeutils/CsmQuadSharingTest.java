package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.registry.RegistrySimple;
import org.junit.jupiter.api.Test;

/**
 * The other mods' pass of {@link CsmQuadSharing}, as {@code /csmclient quadsharing on} runs it
 * over live models: equal quads of the same mod become one instance, nothing crosses between
 * mods, CSM's own models and quads of another class are left alone, and no list changes size or
 * identity (the property that makes it safe while the chunk builders read the lists).
 */
class CsmQuadSharingTest {

  private static BakedQuad quad(int seed) {
    int[] data = new int[28];
    Arrays.fill(data, seed);
    return new BakedQuad(data, -1, EnumFacing.UP, null, true, DefaultVertexFormats.ITEM);
  }

  private static SimpleBakedModel model(List<BakedQuad> general) {
    Map<EnumFacing, List<BakedQuad>> faces = new EnumMap<>(EnumFacing.class);
    for (EnumFacing f : EnumFacing.values()) {
      faces.put(f, new ArrayList<>());
    }
    // No camera transforms: their class needs LWJGL, which the tests do not have.
    return new SimpleBakedModel(general, faces, true, false, null, null, ItemOverrideList.NONE);
  }

  @Test
  void sharesEqualQuadsWithinEachModOnly() {
    RegistrySimple<ModelResourceLocation, IBakedModel> registry = new RegistrySimple<>();
    List<BakedQuad> a1 = new ArrayList<>(Arrays.asList(quad(1), quad(2)));
    List<BakedQuad> a2 = new ArrayList<>(Arrays.asList(quad(1), quad(2), quad(1)));
    List<BakedQuad> b1 = new ArrayList<>(Collections.singletonList(quad(1)));
    List<BakedQuad> own1 = new ArrayList<>(Collections.singletonList(quad(1)));
    List<BakedQuad> own2 = new ArrayList<>(Collections.singletonList(quad(1)));
    registry.putObject(new ModelResourceLocation("moda:x", "normal"), model(a1));
    registry.putObject(new ModelResourceLocation("moda:y", "normal"), model(a2));
    registry.putObject(new ModelResourceLocation("modb:z", "normal"), model(b1));
    registry.putObject(new ModelResourceLocation("csm:p", "normal"), model(own1));
    registry.putObject(new ModelResourceLocation("csm:q", "normal"), model(own2));
    BakedQuad b1Before = b1.get(0);
    BakedQuad own2Before = own2.get(0);

    CsmQuadSharing.OtherModsPass pass = new CsmQuadSharing().runOtherMods(registry, true);

    // moda: five quads, two distinct; three replaced. modb: one quad, nothing to replace.
    assertEquals(6, pass.getQuads());
    assertEquals(3, pass.getReplaced());
    assertEquals(3, pass.getDistinct());
    assertEquals(2, pass.getNamespaces());
    assertEquals(3, pass.getModels());
    assertEquals("moda", pass.getMost());
    assertEquals(3, pass.getMostReplaced());
    assertEquals(3L * (40 + 16 + 4 * 28), pass.getBytes());

    assertSame(a1.get(0), a2.get(0));
    assertSame(a1.get(0), a2.get(2));
    assertSame(a1.get(1), a2.get(1));
    assertEquals(3, a2.size());
    // Never across mods, and CSM's own models are not walked by this pass.
    assertSame(b1Before, b1.get(0));
    assertNotSame(a1.get(0), b1.get(0));
    assertSame(own2Before, own2.get(0));
    assertNotSame(own1.get(0), own2.get(0));
  }

  @Test
  void leavesOtherQuadClassesAndListsAlone() {
    RegistrySimple<ModelResourceLocation, IBakedModel> registry = new RegistrySimple<>();
    BakedQuad custom = new BakedQuad(new int[28], -1, EnumFacing.UP, null, true,
        DefaultVertexFormats.ITEM) {
    };
    List<BakedQuad> plain = new ArrayList<>(Collections.singletonList(new BakedQuad(new int[28],
        -1, EnumFacing.UP, null, true, DefaultVertexFormats.ITEM)));
    List<BakedQuad> withCustom = new ArrayList<>(Collections.singletonList(custom));
    List<BakedQuad> fixed = Collections.unmodifiableList(
        Collections.singletonList(new BakedQuad(new int[28], -1, EnumFacing.UP, null, true,
            DefaultVertexFormats.ITEM)));
    registry.putObject(new ModelResourceLocation("moda:a", "normal"), model(plain));
    registry.putObject(new ModelResourceLocation("moda:b", "normal"), model(withCustom));
    registry.putObject(new ModelResourceLocation("moda:c", "normal"), model(fixed));

    CsmQuadSharing.OtherModsPass pass = new CsmQuadSharing().runOtherMods(registry, true);

    assertEquals(0, pass.getReplaced());
    assertSame(custom, withCustom.get(0));
  }

  @Test
  void aSecondPassReplacesNothing() {
    RegistrySimple<ModelResourceLocation, IBakedModel> registry = new RegistrySimple<>();
    registry.putObject(new ModelResourceLocation("moda:x", "normal"),
        model(new ArrayList<>(Arrays.asList(quad(7), quad(7), quad(7)))));
    assertEquals(2, new CsmQuadSharing().runOtherMods(registry, true).getReplaced());
    assertEquals(0, new CsmQuadSharing().runOtherMods(registry, true).getReplaced());
  }
}
