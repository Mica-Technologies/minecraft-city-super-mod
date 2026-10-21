package com.micatechnologies.minecraft.csm.technology.rack;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

/**
 * Puts {@link RackBakedModel} round every world variant of every fillable rack, keeps the unit
 * sheet on the block atlas, and bakes and caches the units' quads.
 *
 * <p>Wrapped through {@link CsmBakedModelWrappers}, never by walking the model registry, so a
 * pack running VintageFix's dynamic resources draws the units too (see the mile markers'
 * {@code MileMarkerModels}, whose approach this copies). The wrapper is registered after Core's
 * glass split ({@code CsmGlassLayerModel}), so it wraps that model and adds the units in every
 * pass but the translucent one.</p>
 *
 * <p>Quads are cached by (frame, contents, facing, cell), so a room of racks filled alike bakes
 * them once; the cache is dropped at every bake, when the sheet's sprite may have moved.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public final class RackModels {

  private static final FaceBakery BAKERY = new FaceBakery();

  private static final Cache<Key, List<BakedQuad>> CACHE =
      CacheBuilder.newBuilder().maximumSize(4096).build();

  /** The unit sheet, read at the latest bake. */
  private static volatile TextureAtlasSprite sheet;

  private RackModels() {
  }

  /** Registers the handlers; called from the Technology client proxy's pre-initialization. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new Events());
    CsmBakedModelWrappers.register(RackModels::wrap);
  }

  /** The event handlers. */
  public static final class Events {

    /**
     * The rack models name the sheet already, which puts it on the atlas; this makes it
     * independent of how a model's unused texture keys are treated.
     */
    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
      event.getMap().registerSprite(new ResourceLocation(RackUnitType.SHEET));
    }

    /** Reads the sheet before any model is wrapped, and forgets quads baked on the old one. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onModelBake(ModelBakeEvent event) {
      sheet = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(RackUnitType.SHEET);
      CACHE.invalidateAll();
    }
  }

  /** The unit wrapper round a fillable rack's world model; {@code null} for any other model. */
  @Nullable
  private static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof RackBakedModel || "inventory".equals(location.getVariant())
        || !"csm".equals(location.getNamespace())) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    if (!(block instanceof AbstractBlockRack) || !((AbstractBlockRack) block).isFillable()) {
      return null;
    }
    return new RackBakedModel(model, (AbstractBlockRack) block);
  }

  private static final class Key {

    final RackFrame frame;
    final RackContents contents;
    final EnumFacing facing;
    final int cell;

    Key(RackFrame frame, RackContents contents, EnumFacing facing, int cell) {
      this.frame = frame;
      this.contents = contents;
      this.facing = facing;
      this.cell = cell;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Key)) {
        return false;
      }
      Key k = (Key) o;
      return frame == k.frame && facing == k.facing && cell == k.cell
          && contents.equals(k.contents);
    }

    @Override
    public int hashCode() {
      return Objects.hash(frame, contents, facing, cell);
    }
  }

  /**
   * The quads of one cell's units, turned to the rack's facing.
   *
   * @param frame    the rack
   * @param contents what is mounted
   * @param facing   the rack's facing
   * @param cell     the cell
   * @return the quads, shared: never modify them
   */
  static List<BakedQuad> quads(RackFrame frame, RackContents contents, EnumFacing facing,
      int cell) {
    TextureAtlasSprite s = sheet;
    if (s == null) {
      return Collections.emptyList();
    }
    Key key = new Key(frame, contents, facing, cell);
    try {
      return CACHE.get(key, () -> bake(key, s));
    } catch (ExecutionException e) {
      return Collections.emptyList();
    }
  }

  /** The y turn a facing's blockstate variant gives the model. */
  private static int degrees(EnumFacing facing) {
    switch (facing) {
      case EAST:
        return 90;
      case SOUTH:
        return 180;
      case WEST:
        return 270;
      default:
        return 0;
    }
  }

  private static final EnumFacing[] FACES = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST,
      EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN};
  private static final int[] BITS = {RackUnitBoxes.NORTH, RackUnitBoxes.SOUTH, RackUnitBoxes.EAST,
      RackUnitBoxes.WEST, RackUnitBoxes.UP, RackUnitBoxes.DOWN};

  private static List<BakedQuad> bake(Key key, TextureAtlasSprite sprite) {
    ModelRotation turn = ModelRotation.getModelRotation(0, degrees(key.facing));
    List<RackUnitBoxes.Box> boxes = RackUnitBoxes.boxes(key.frame, key.contents, key.cell);
    List<BakedQuad> out = new ArrayList<>(boxes.size() * 4);
    for (RackUnitBoxes.Box b : boxes) {
      Vector3f from = new Vector3f(b.x0, b.y0, b.z0);
      Vector3f to = new Vector3f(b.x1, b.y1, b.z1);
      for (int i = 0; i < FACES.length; i++) {
        if (!b.has(BITS[i])) {
          continue;
        }
        float[] uv = FACES[i] == EnumFacing.NORTH ? b.frontUv : b.sideUv;
        BlockPartFace face = new BlockPartFace(null, -1, "",
            new BlockFaceUV(uv.clone(), 0));
        out.add(BAKERY.makeBakedQuad(from, to, face, sprite, FACES[i], turn, null, false,
            true));
      }
    }
    return out;
  }
}
