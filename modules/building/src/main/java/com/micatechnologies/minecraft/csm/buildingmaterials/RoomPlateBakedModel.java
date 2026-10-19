package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
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
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

/**
 * A room plate's model: the blank plate its blockstate draws, with the number and name its tile
 * entity holds added as quads from the glyph sheet, baked into the chunk mesh.
 *
 * <p>The legend arrives through {@link BlockRoomPlate#LEGEND}, is laid out by
 * {@link RoomPlateGlyphs} in the plate's own model space (the wall to the north) and baked by the
 * vanilla {@link FaceBakery} under the same y turn the blockstate gives that facing's variant, so
 * plate and legend cannot disagree. Quads are cached by legend: a ward of plates bakes each
 * number once, and nothing runs per frame.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class RoomPlateBakedModel extends BakedModelWrapper<IBakedModel> {

  private static final String SHEET = "csm:blocks/wallfinish/room_plate_glyphs";
  private static final FaceBakery BAKERY = new FaceBakery();

  private final ModelRotation rotation;
  private final Cache<Integer, List<BakedQuad>> cache =
      CacheBuilder.newBuilder().maximumSize(1024).build();

  RoomPlateBakedModel(IBakedModel original, ModelRotation rotation) {
    super(original);
    this.rotation = rotation;
  }

  /** Registers the glyph sheet with the block atlas and the wrapper with Core. */
  static void register() {
    MinecraftForge.EVENT_BUS.register(new Object() {
      @SubscribeEvent
      public void onStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation(SHEET));
      }
    });
    com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers.register(
        RoomPlateBakedModel::wrap);
  }

  /**
   * Wraps a room plate's world model, by blockstate variant; the item model already carries a
   * sample legend.
   */
  @Nullable
  static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof RoomPlateBakedModel || !"csm".equals(location.getNamespace())
        || !location.getPath().startsWith("room_plate_")
        || !location.getVariant().startsWith("facing=")) {
      return null;
    }
    String side = location.getVariant().substring("facing=".length());
    EnumFacing facing = EnumFacing.byName(side);
    if (facing == null || facing.getAxis() == EnumFacing.Axis.Y) {
      return null;
    }
    // The blockstate's turn for each facing: north 0, east 90, south 180, west 270.
    int turn = (facing.getHorizontalIndex() * 90 + 180) % 360;
    return new RoomPlateBakedModel(model, ModelRotation.getModelRotation(0, turn));
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    List<BakedQuad> base = originalModel.getQuads(state, side, rand);
    if (side != null || !(state instanceof IExtendedBlockState)) {
      return base;
    }
    Integer legend = ((IExtendedBlockState) state).getValue(BlockRoomPlate.LEGEND);
    if (legend == null) {
      return base;
    }
    List<BakedQuad> overlay;
    try {
      overlay = cache.get(legend, () -> bake(legend));
    } catch (ExecutionException e) {
      overlay = Collections.emptyList();
    }
    if (overlay.isEmpty()) {
      return base;
    }
    List<BakedQuad> out = new ArrayList<>(base.size() + overlay.size());
    out.addAll(base);
    out.addAll(overlay);
    return out;
  }

  private List<BakedQuad> bake(int legend) {
    TextureAtlasSprite sheet = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite(SHEET);
    List<BakedQuad> out = new ArrayList<>();
    int number = Math.floorDiv(legend, 16);
    int name = Math.floorMod(legend, 16);
    float z = RoomPlateGlyphs.LEGEND_Z;
    for (RoomPlateGlyphs.Piece piece : RoomPlateGlyphs.pieces(number, name)) {
      // The plate's face looks south in model space (the wall to the north), just off its face.
      BlockPartFace face = new BlockPartFace(EnumFacing.SOUTH, -1, "",
          new BlockFaceUV(piece.uv, 0));
      out.add(BAKERY.makeBakedQuad(new Vector3f(piece.x0, piece.y0, z),
          new Vector3f(piece.x1, piece.y1, z), face, sheet, EnumFacing.SOUTH, rotation,
          null, false, true));
    }
    return out;
  }
}
