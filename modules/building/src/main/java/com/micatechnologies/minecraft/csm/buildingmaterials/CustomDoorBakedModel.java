package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Half;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Hinge;
import com.micatechnologies.minecraft.csm.buildingmaterials.CustomDoorGeometry.Box;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

/**
 * The custom door's baked model: the shape from {@link CustomDoorGeometry}, each box faced with the
 * sprite of the material it wears, baked into the chunk like any block's.
 *
 * <p>The settings arrive through the block's extended state. The quads for one combination -- the
 * settings, half, facing, hinge, open or shut, paired, render pass -- are baked once and cached, so
 * a street of identical doors bakes them once. A material is drawn in the pass its own block draws
 * in: glass in the translucent pass, everything else in the cutout pass. A door that is moving
 * ({@link BlockCustomDoor#HIDDEN}) draws nothing here; the moving-door renderer draws it.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class CustomDoorBakedModel implements IBakedModel {

  private static final FaceBakery BAKERY = new FaceBakery();

  private final Cache<Key, List<BakedQuad>> cache =
      CacheBuilder.newBuilder().maximumSize(4096).build();

  /** One combination the quads depend on. */
  private static final class Key {

    final CustomDoorSettings settings;
    final Half half;
    final EnumFacing facing;
    final Hinge hinge;
    final boolean open;
    final boolean paired;
    final BlockRenderLayer layer;

    Key(CustomDoorSettings settings, Half half, EnumFacing facing, Hinge hinge, boolean open,
        boolean paired, BlockRenderLayer layer) {
      this.settings = settings;
      this.half = half;
      this.facing = facing;
      this.hinge = hinge;
      this.open = open;
      this.paired = paired;
      this.layer = layer;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Key)) {
        return false;
      }
      Key k = (Key) o;
      return open == k.open && paired == k.paired && half == k.half && facing == k.facing
          && hinge == k.hinge && layer == k.layer && settings.equals(k.settings);
    }

    @Override
    public int hashCode() {
      return Objects.hash(settings, half, facing, hinge, open, paired, layer);
    }
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    if (side != null || !(state instanceof IExtendedBlockState)) {
      return Collections.emptyList();
    }
    IExtendedBlockState ext = (IExtendedBlockState) state;
    if (Boolean.TRUE.equals(ext.getValue(BlockCustomDoor.HIDDEN))) {
      return Collections.emptyList();
    }
    CustomDoorSettings settings = ext.getValue(BlockCustomDoor.SETTINGS);
    return quads(settings == null ? CustomDoorSettings.DEFAULT : settings,
        state.getValue(BlockBuildingDoor.HALF), state.getValue(BlockBuildingDoor.FACING),
        state.getValue(BlockBuildingDoor.HINGE), state.getValue(BlockBuildingDoor.OPEN),
        Boolean.TRUE.equals(ext.getValue(BlockCustomDoor.PAIRED)),
        MinecraftForgeClient.getRenderLayer());
  }

  /**
   * The quads for one combination; {@code layer} null for every pass at once (the moving-door
   * renderer).
   *
   * @since 1.0
   */
  public List<BakedQuad> quads(CustomDoorSettings settings, Half half, EnumFacing facing,
      Hinge hinge, boolean open, boolean paired, @Nullable BlockRenderLayer layer) {
    Key key = new Key(settings, half, facing, hinge, open, paired, layer);
    try {
      return cache.get(key, () -> bake(key));
    } catch (ExecutionException e) {
      return Collections.emptyList();
    }
  }

  private static BlockRenderLayer layerOf(IBlockState material) {
    return material.getBlock().getRenderLayer() == BlockRenderLayer.TRANSLUCENT
        ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT_MIPPED;
  }

  private static List<BakedQuad> bake(Key k) {
    List<Box> boxes = k.open
        ? CustomDoorGeometry.open(k.settings.movement(), k.half, k.hinge, k.paired)
        : CustomDoorGeometry.closed(k.settings.movement(), k.half, k.hinge);
    ModelRotation rotation = ModelRotation.getModelRotation(0,
        (int) ((k.facing.getHorizontalAngle() + 180) % 360));
    List<BakedQuad> out = new ArrayList<>();
    for (Box b : boxes) {
      IBlockState material = b.part == CustomDoorGeometry.Part.FRAME ? k.settings.frame()
          : b.part == CustomDoorGeometry.Part.UPPER ? k.settings.upper() : k.settings.lower();
      if (k.layer != null && layerOf(material) != k.layer) {
        continue;
      }
      TextureAtlasSprite sprite = CustomDoorMaterials.sprite(material);
      Vector3f from = new Vector3f(b.x0, b.y0, b.z0);
      Vector3f to = new Vector3f(b.x1, b.y1, b.z1);
      for (EnumFacing face : EnumFacing.values()) {
        BlockPartFace part = new BlockPartFace(null, -1, "", new BlockFaceUV(uv(face, b), 0));
        out.add(BAKERY.makeBakedQuad(from, to, part, sprite, face, rotation, null, false, true));
      }
    }
    return out;
  }

  /** The UV Minecraft would take from a face's position, fitted into the sprite. */
  private static float[] uv(EnumFacing face, Box b) {
    float u0;
    float v0;
    float u1;
    float v1;
    switch (face) {
      case DOWN:
        u0 = b.x0;
        v0 = 16 - b.z1;
        u1 = b.x1;
        v1 = 16 - b.z0;
        break;
      case UP:
        u0 = b.x0;
        v0 = b.z0;
        u1 = b.x1;
        v1 = b.z1;
        break;
      case NORTH:
        u0 = 16 - b.x1;
        v0 = 16 - b.y1;
        u1 = 16 - b.x0;
        v1 = 16 - b.y0;
        break;
      case SOUTH:
        u0 = b.x0;
        v0 = 16 - b.y1;
        u1 = b.x1;
        v1 = 16 - b.y0;
        break;
      case WEST:
        u0 = b.z0;
        v0 = 16 - b.y1;
        u1 = b.z1;
        v1 = 16 - b.y0;
        break;
      default:
        u0 = 16 - b.z1;
        v0 = 16 - b.y1;
        u1 = 16 - b.z0;
        v1 = 16 - b.y0;
        break;
    }
    float[] u = fit(u0, u1);
    float[] v = fit(v0, v1);
    return new float[]{u[0], v[0], u[1], v[1]};
  }

  /**
   * A span shifted by whole blocks into 0..16 if it fits there, else clamped: a span past the cell
   * (a door slid a block over) must not sample the neighbouring sprites on the atlas.
   */
  private static float[] fit(float a, float b) {
    float lo = Math.min(a, b);
    float hi = Math.max(a, b);
    float k = (float) Math.floor(lo / 16) * 16;
    if (hi - k <= 16) {
      lo -= k;
      hi -= k;
    } else {
      lo = Math.max(0, Math.min(16, lo));
      hi = Math.max(0, Math.min(16, hi));
    }
    return new float[]{lo, hi};
  }

  @Override
  public boolean isAmbientOcclusion() {
    return true;
  }

  @Override
  public boolean isGui3d() {
    return true;
  }

  @Override
  public boolean isBuiltInRenderer() {
    return false;
  }

  @Override
  @Nonnull
  public TextureAtlasSprite getParticleTexture() {
    return CustomDoorMaterials.sprite(CustomDoorSettings.DEFAULT.frame());
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public ItemCameraTransforms getItemCameraTransforms() {
    return ItemCameraTransforms.DEFAULT;
  }

  @Override
  @Nonnull
  public ItemOverrideList getOverrides() {
    return ItemOverrideList.NONE;
  }
}
