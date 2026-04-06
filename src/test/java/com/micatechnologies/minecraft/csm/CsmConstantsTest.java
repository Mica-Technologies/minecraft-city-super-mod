package com.micatechnologies.minecraft.csm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CsmConstantsTest {

  @Test
  void modNamespaceIsCsm() {
    assertEquals("csm", CsmConstants.MOD_NAMESPACE);
  }

  @Test
  void modNameIsNonNullAndNonEmpty() {
    assertNotNull(CsmConstants.MOD_NAME);
    assertFalse(CsmConstants.MOD_NAME.isEmpty());
  }

  @Test
  void modVersionIsNonNullAndNonEmpty() {
    assertNotNull(CsmConstants.MOD_VERSION);
    assertFalse(CsmConstants.MOD_VERSION.isEmpty());
  }

  @Test
  void modNamespaceIsNonNullAndNonEmpty() {
    assertNotNull(CsmConstants.MOD_NAMESPACE);
    assertFalse(CsmConstants.MOD_NAMESPACE.isEmpty());
  }
}
