package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import com.micatechnologies.minecraft.csm.codeutils.CsmGlassLayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

/**
 * A glazing window's model ({@link BlockGlazingWindow}): every face is baked here from the boxes
 * {@link WindowLayout} lays out for the window's shape, which arrives packed in
 * {@link BlockGlazingWindow#SHAPE}. The blockstate's own model is empty but for its particle
 * texture.
 *
 * <p>Quads are cached by finish and shape, so a facade of like windows bakes each kind once and
 * nothing runs per frame. The glass goes in the translucent pass and the frame and stone in the
 * cutout pass: this wrapper is registered after Core's glass split ({@code CsmGlassLayerModel}),
 * so it splits its own quads by the same rule ({@link CsmGlassLayer#belongsIn}). Put in place
 * through {@link CsmBakedModelWrappers}, so a pack running VintageFix's dynamic bakes draws the
 * windows too.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public class GlazingWindowBakedModel extends BakedModelWrapper<IBakedModel> {

  static final String GLASS = "csm:blocks/glazing/glass_clear";
  static final String STONE = "csm:blocks/glazing/window_stone";
  static final String FRAME = "csm:blocks/glazing/window_frame_";

  private static final FaceBakery BAKERY = new FaceBakery();

  /** The finishes, SHARED with gen_glazing_windows.FINISHES; the index is in the cache key. */
  static final String[] FINISHES = {"bronze", "white", "black", "oak", "walnut", "darkoak",
      "aluminium"};

  private static final Cache<Long, Quads> CACHE =
      CacheBuilder.newBuilder().maximumSize(4096).build();

  private final String finish;
  private final int finishIndex;

  GlazingWindowBakedModel(IBakedModel original, String finish) {
    super(original);
    this.finish = finish;
    this.finishIndex = java.util.Arrays.asList(FINISHES).indexOf(finish) + 1;
  }

  /** The baked quads of one shape, split by pass. */
  private static final class Quads {

    final List<BakedQuad> all;
    final List<BakedQuad> cutout;
    final List<BakedQuad> glass;

    Quads(List<BakedQuad> all) {
      this.all = all;
      List<BakedQuad> c = new ArrayList<>();
      List<BakedQuad> g = new ArrayList<>();
      for (BakedQuad q : all) {
        (CsmGlassLayer.isGlass(q.getSprite().getIconName()) ? g : c).add(q);
      }
      this.cutout = c;
      this.glass = g;
    }
  }

  /** Registers the sprites and the wrapper; the Building client proxy's pre-initialisation. */
  static void register() {
    MinecraftForge.EVENT_BUS.register(new Object() {
      @SubscribeEvent
      public void onStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation(GLASS));
        event.getMap().registerSprite(new ResourceLocation(STONE));
        for (String f : FINISHES) {
          event.getMap().registerSprite(new ResourceLocation(FRAME + f));
        }
        CACHE.invalidateAll();
      }
    });
    CsmBakedModelWrappers.register(GlazingWindowBakedModel::wrap);
    GlazingWindowRenderUpdater.register();
  }

  /** Wraps every world variant of every window; the item model is a flat icon, left alone. */
  @Nullable
  static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof GlazingWindowBakedModel || "inventory".equals(location.getVariant())
        || !location.getPath().startsWith("glass_window_")) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    if (!(block instanceof BlockGlazingWindow)) {
      return null;
    }
    return new GlazingWindowBakedModel(model, ((BlockGlazingWindow) block).getFinish());
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    if (side != null || !(state instanceof IExtendedBlockState)) {
      return originalModel.getQuads(state, side, rand);
    }
    Long shape = ((IExtendedBlockState) state).getValue(BlockGlazingWindow.SHAPE);
    if (shape == null) {
      return originalModel.getQuads(state, side, rand);
    }
    long key = shape | (long) finishIndex << 40;
    Quads quads;
    try {
      quads = CACHE.get(key, () -> new Quads(bake(shape, finish)));
    } catch (ExecutionException e) {
      return Collections.emptyList();
    }
    BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
    if (layer == null) {
      return quads.all;
    }
    return layer == BlockRenderLayer.TRANSLUCENT ? quads.glass : quads.cutout;
  }

  // --- baking -------------------------------------------------------------------------------

  private static List<BakedQuad> bake(long packed, String finish) {
    WindowLayout.Shape shape = WindowLayout.Shape.unpack(packed);
    TextureAtlasSprite glass = sprite(GLASS);
    TextureAtlasSprite frame = sprite(FRAME + finish);
    TextureAtlasSprite stone = sprite(STONE);
    // the blockstate turn for the facing: north 0, east 90, south 180, west 270
    int turn = ((shape.facing + 2) & 3) * 90;
    ModelRotation rotation = ModelRotation.getModelRotation(0, turn);
    List<BakedQuad> out = new ArrayList<>();
    for (WindowLayout.Box b : WindowLayout.boxes(shape)) {
      TextureAtlasSprite tex = b.part == WindowLayout.GLASS ? glass
          : b.part == WindowLayout.STONE ? stone : frame;
      BlockPartRotation partRotation = b.rotated
          ? new BlockPartRotation(new Vector3f(b.ox / 16f, 0.5f, b.oz / 16f), EnumFacing.Axis.Y,
          45f, false) : null;
      Vector3f from = new Vector3f(b.x0, b.y0, b.z0);
      Vector3f to = new Vector3f(b.x1, b.y1, b.z1);
      for (EnumFacing face : EnumFacing.VALUES) {
        if ((b.faces & 1 << face.getIndex()) == 0) {
          continue;
        }
        BlockPartFace part = new BlockPartFace(null, -1, "",
            new BlockFaceUV(uv(b, face), 0));
        out.add(BAKERY.makeBakedQuad(from, to, part, tex, face, rotation, partRotation, false,
            true));
      }
    }
    return out;
  }

  private static TextureAtlasSprite sprite(String name) {
    return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
  }

  /**
   * The face's UVs as a model without UVs gets them (by position), each range wrapped into the
   * sprite: a box reaching past the cell (a sill's horn) would otherwise sample the neighbouring
   * sprite on the atlas.
   */
  static float[] uv(WindowLayout.Box b, EnumFacing face) {
    float[] r;
    switch (face) {
      case DOWN:
        r = new float[]{b.x0, 16 - b.z1, b.x1, 16 - b.z0};
        break;
      case UP:
        r = new float[]{b.x0, b.z0, b.x1, b.z1};
        break;
      case NORTH:
        r = new float[]{16 - b.x1, 16 - b.y1, 16 - b.x0, 16 - b.y0};
        break;
      case SOUTH:
        r = new float[]{b.x0, 16 - b.y1, b.x1, 16 - b.y0};
        break;
      case WEST:
        r = new float[]{b.z0, 16 - b.y1, b.z1, 16 - b.y0};
        break;
      default:
        r = new float[]{16 - b.z1, 16 - b.y1, 16 - b.z0, 16 - b.y0};
        break;
    }
    wrap(r, 0);
    wrap(r, 1);
    return r;
  }

  private static void wrap(float[] r, int i) {
    float len = Math.min(r[i + 2] - r[i], 16f);
    float a = r[i] % 16f;
    if (a < 0) {
      a += 16f;
    }
    if (a + len > 16f) {
      a = 16f - len;
    }
    r[i] = a;
    r[i + 2] = a + len;
  }
}
