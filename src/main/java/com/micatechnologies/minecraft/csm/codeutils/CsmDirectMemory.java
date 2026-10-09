package com.micatechnologies.minecraft.csm.codeutils;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/**
 * Direct (off-heap) memory: how much is in use, and freeing a buffer now rather than when a
 * garbage collection gets round to it.
 *
 * <p>A direct buffer's memory is returned only when the collector finds its {@link ByteBuffer}
 * unreachable. Running short, the JVM asks for a full collection to find some; a modpack's
 * {@code -XX:+DisableExplicitGC} turns that request off, and the game dies of "Direct buffer
 * memory" with gigabytes of dead buffers waiting. A buffer CSM knows to be dead is better freed
 * outright.</p>
 *
 * <p>Freeing goes through the buffer's cleaner on Java 8 ({@code sun.nio.ch.DirectBuffer}) and
 * {@code sun.misc.Unsafe.invokeCleaner} on Java 9 and later, both found by reflection. Where
 * neither works the buffer is simply dropped, as before. <b>Free only a buffer nothing can touch
 * again:</b> reading freed memory crashes the JVM.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public final class CsmDirectMemory {

  @Nullable
  private static final BufferPoolMXBean DIRECT_POOL = findDirectPool();

  /** Java 9+: {@code Unsafe.invokeCleaner(ByteBuffer)} and the {@code Unsafe} it is called on. */
  @Nullable
  private static final Method INVOKE_CLEANER;
  @Nullable
  private static final Object UNSAFE;

  static {
    Method invoke = null;
    Object unsafe = null;
    try {
      Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      invoke = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
      Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      unsafe = theUnsafe.get(null);
    } catch (Throwable java8) {
      invoke = null;
      unsafe = null;
    }
    INVOKE_CLEANER = invoke;
    UNSAFE = unsafe;
  }

  private CsmDirectMemory() {
  }

  @Nullable
  private static BufferPoolMXBean findDirectPool() {
    try {
      for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
        if ("direct".equals(pool.getName())) {
          return pool;
        }
      }
    } catch (Throwable ignored) {
      // Reported as 0
    }
    return null;
  }

  /** Bytes of direct memory in use, or 0 if the JVM does not say. */
  public static long used() {
    return DIRECT_POOL == null ? 0 : DIRECT_POOL.getMemoryUsed();
  }

  /** How many direct buffers exist, live or awaiting collection, or 0 if the JVM does not say. */
  public static long count() {
    return DIRECT_POOL == null ? 0 : DIRECT_POOL.getCount();
  }

  /**
   * Frees a direct buffer's memory now. The buffer, and every view made from it, must never be
   * used again.
   *
   * @param buffer a dead direct buffer; a heap buffer or a view is ignored
   *
   * @return whether the memory was freed (false means it is left to the collector)
   */
  public static boolean free(@Nullable ByteBuffer buffer) {
    if (buffer == null || !buffer.isDirect()) {
      return false;
    }
    try {
      if (INVOKE_CLEANER != null) {
        INVOKE_CLEANER.invoke(UNSAFE, buffer);
        return true;
      }
      Method cleanerMethod = buffer.getClass().getMethod("cleaner");
      cleanerMethod.setAccessible(true);
      Object cleaner = cleanerMethod.invoke(buffer);
      if (cleaner == null) {
        return false; // A view or a slice: its memory belongs to another buffer
      }
      Method clean = cleaner.getClass().getMethod("clean");
      clean.setAccessible(true);
      clean.invoke(cleaner);
      return true;
    } catch (Throwable notFreed) {
      return false;
    }
  }
}
