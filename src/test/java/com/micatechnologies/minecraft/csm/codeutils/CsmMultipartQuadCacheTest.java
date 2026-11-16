package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;

/**
 * {@link CsmMultipartQuadCache.Cached} asks the model it wraps once per state and side, and hands
 * the same answer back after.
 */
class CsmMultipartQuadCacheTest {

  @Test
  void asksOncePerStateAndSide() {
    Map<String, Integer> calls = new HashMap<>();
    List<BakedQuad> general = new ArrayList<>();
    general.add(null);
    IBakedModel model = (IBakedModel) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{IBakedModel.class}, (proxy, method, args) -> {
          if ("getQuads".equals(method.getName())) {
            String key = System.identityHashCode(args[0]) + "/" + args[1];
            calls.merge(key, 1, Integer::sum);
            return args[1] == null ? new ArrayList<>(general) : Collections.emptyList();
          }
          return null;
        });
    IBlockState a = state();
    IBlockState b = state();
    CsmMultipartQuadCache.Cached cached = new CsmMultipartQuadCache.Cached(model);
    List<BakedQuad> first = cached.getQuads(a, null, 1L);
    for (int i = 0; i < 5; i++) {
      assertSame(first, cached.getQuads(a, null, i));
      for (EnumFacing side : EnumFacing.values()) {
        assertEquals(0, cached.getQuads(a, side, i).size());
        cached.getQuads(b, side, i);
      }
    }
    assertEquals(1, first.size());
    assertEquals(13, calls.size(), "a: 7 sides, b: 6 sides");
    calls.values().forEach(n -> assertEquals(1, (int) n));
    // no state: always asked, never kept
    cached.getQuads(null, null, 0L);
    cached.getQuads(null, null, 0L);
    assertEquals(2, (int) calls.get(System.identityHashCode(null) + "/null"));
  }

  private static IBlockState state() {
    return (IBlockState) Proxy.newProxyInstance(CsmMultipartQuadCacheTest.class.getClassLoader(),
        new Class<?>[]{IBlockState.class}, (proxy, method, args) -> {
          switch (method.getName()) {
            case "hashCode":
              return System.identityHashCode(proxy);
            case "equals":
              return proxy == args[0];
            default:
              return null;
          }
        });
  }
}
