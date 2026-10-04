package org.highresfocusbar.client.portrait;

import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.TilePicker;
import com.wurmonline.client.renderer.cave.CaveWallPicker;
import com.wurmonline.client.renderer.model.ModelResourceLoader;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.terrain.TerrainTexture;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.mesh.FieldData;
import com.wurmonline.mesh.GrassData;
import com.wurmonline.mesh.Tiles;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves selected surface tiles and cave walls into the resources used by
 * Wurm's world renderer. Pickers expose generic item-atlas icon IDs, so a
 * useful Select Bar preview has to consult the corresponding terrain buffer.
 */
public final class TilePortraitPreview {
    public enum OverlayKind { NONE, GRASS, CROP }

    private static final Logger LOG = Logger.getLogger("HighResSelectBar");
    private static final int ATLAS_COLUMNS = 2;
    private static final int ATLAS_ROWS = 4;

    private final long subjectId;
    private final int tileTypeId;
    private final byte tileData;
    private final String seasonAppendix;
    private final Texture groundTexture;
    private final Texture overlayTexture;
    private final OverlayKind overlayKind;
    private final float overlayU;
    private final float overlayV;
    private final float overlayWidth;
    private final float overlayHeight;
    private final String modelMapping;
    private final ModelResourceWrapper modelSource;
    private final boolean caveSubject;
    private final int caveX;
    private final int caveY;
    private final int caveWallSide;

    private TilePortraitPreview(long subjectId, Tiles.Tile tile, byte tileData,
                                String seasonAppendix, Texture groundTexture,
                                Texture overlayTexture,
                                OverlayKind overlayKind, float[] overlayUv,
                                String modelMapping,
                                ModelResourceWrapper modelSource,
                                boolean caveSubject, int caveX, int caveY,
                                int caveWallSide) {
        this.subjectId = subjectId;
        this.tileTypeId = tile.getIntId();
        this.tileData = tileData;
        this.seasonAppendix = clean(seasonAppendix);
        this.groundTexture = groundTexture;
        this.overlayTexture = overlayTexture;
        this.overlayKind = overlayKind;
        this.overlayU = overlayUv[0];
        this.overlayV = overlayUv[1];
        this.overlayWidth = overlayUv[2];
        this.overlayHeight = overlayUv[3];
        this.modelMapping = clean(modelMapping);
        this.modelSource = modelSource;
        this.caveSubject = caveSubject;
        this.caveX = caveX;
        this.caveY = caveY;
        this.caveWallSide = caveWallSide;
    }

    public static TilePortraitPreview resolve(World world,
                                              PickableUnit subject) {
        if (world == null || subject == null) return null;
        if (subject instanceof CaveWallPicker) {
            return resolveCaveWall(world, (CaveWallPicker) subject);
        }
        if (!(subject instanceof TilePicker)) return null;
        try {
            long id = subject.getId();
            int tileX = Tiles.decodeTileX(id);
            int tileY = Tiles.decodeTileY(id);
            Tiles.Tile tile = world.getNearTerrainBuffer()
                    .getTileType(tileX, tileY);
            if (tile == null) return null;
            byte data = world.getNearTerrainBuffer().getData(tileX, tileY);
            String season = seasonAppendix(world);
            Texture ground = TerrainTexture.getTileTexture(world, tile);

            Texture overlay = null;
            OverlayKind overlayKind = OverlayKind.NONE;
            float[] overlayUv = atlasUvForCell(0);
            String modelMapping = "";
            ModelResourceWrapper modelSource = null;

            if (tile.isTree() || tile.isBush()) {
                String base = tile.getModelResourceName(data);
                ResolvedModel resolved = resolveSeasonalModel(base, season);
                modelMapping = resolved.mapping;
                modelSource = resolved.source;
            } else if (tile == Tiles.Tile.TILE_FIELD
                    || tile == Tiles.Tile.TILE_FIELD2) {
                int fieldType = FieldData.getType(tile, data);
                int age = FieldData.getAge(data);
                String specialModel = fieldModelMapping(fieldType, age);
                if (!specialModel.isEmpty()) {
                    ResolvedModel resolved = resolveModel(specialModel);
                    modelMapping = resolved.mapping;
                    modelSource = resolved.source;
                } else {
                    overlay = texture(FieldData.getModelResourceName(fieldType));
                    overlayKind = OverlayKind.CROP;
                    overlayUv = atlasUvForCell(age);
                }
            } else if (tile.isGrass()) {
                overlay = texture("img.texture.grass2" + season);
                overlayKind = OverlayKind.GRASS;
                overlayUv = atlasUvForCell(GrassData.getFlowerType(data));
            }

            return new TilePortraitPreview(id, tile, data, season, ground,
                    overlay, overlayKind, overlayUv, modelMapping, modelSource,
                    false, 0, 0, -1);
        } catch (Throwable error) {
            LOG.log(Level.FINE, "Unable to resolve selected terrain preview",
                    error);
            return null;
        }
    }

    public boolean stillMatches(World world, PickableUnit subject) {
        if (world == null || subject == null
                || subject.getId() != subjectId) return false;
        if (caveSubject) return caveWallStillMatches(world, subject);
        if (!(subject instanceof TilePicker)) return false;
        try {
            int tileX = Tiles.decodeTileX(subjectId);
            int tileY = Tiles.decodeTileY(subjectId);
            Tiles.Tile tile = world.getNearTerrainBuffer()
                    .getTileType(tileX, tileY);
            return tile != null
                    && tile.getIntId() == tileTypeId
                    && world.getNearTerrainBuffer().getData(tileX, tileY)
                    == tileData
                    && Objects.equals(seasonAppendix,
                    seasonAppendix(world));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static TilePortraitPreview resolveCaveWall(
            World world, CaveWallPicker subject) {
        try {
            int tileX = subject.getXNeighbor();
            int tileY = subject.getYNeighbor();
            Tiles.Tile tile = world.getCaveBuffer().getTileType(tileX, tileY);
            if (tile == null) return null;
            Texture wall = texture(textureMappingForCaveTile(tile));
            return new TilePortraitPreview(subject.getId(), tile, (byte) 0,
                    "", wall, null, OverlayKind.NONE, atlasUvForCell(0),
                    "", null, true, tileX, tileY, subject.getWallId());
        } catch (Throwable error) {
            LOG.log(Level.FINE, "Unable to resolve selected cave preview",
                    error);
            return null;
        }
    }

    private boolean caveWallStillMatches(World world, PickableUnit subject) {
        if (!(subject instanceof CaveWallPicker)) return false;
        CaveWallPicker wall = (CaveWallPicker) subject;
        if (wall.getXNeighbor() != caveX || wall.getYNeighbor() != caveY
                || wall.getWallId() != caveWallSide) return false;
        try {
            Tiles.Tile tile = world.getCaveBuffer().getTileType(caveX, caveY);
            return tile != null && tile.getIntId() == tileTypeId;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static String textureMappingForCaveTile(Tiles.Tile tile) {
        return tile == null ? "" : clean(tile.getTextureResource());
    }

    public Texture getGroundTexture() {
        return groundTexture;
    }

    public Texture getOverlayTexture() {
        return overlayTexture;
    }

    public OverlayKind getOverlayKind() {
        return overlayKind;
    }

    public float getOverlayU() {
        return overlayU;
    }

    public float getOverlayV() {
        return overlayV;
    }

    public float getOverlayWidth() {
        return overlayWidth;
    }

    public float getOverlayHeight() {
        return overlayHeight;
    }

    public String getModelMapping() {
        return modelMapping;
    }

    public ModelResourceWrapper getModelSource() {
        return modelSource;
    }

    static float[] atlasUvForCell(int cell) {
        int clamped = Math.max(0,
                Math.min(ATLAS_COLUMNS * ATLAS_ROWS - 1, cell));
        float width = 1f / ATLAS_COLUMNS;
        float height = 1f / ATLAS_ROWS;
        return new float[]{(clamped % ATLAS_COLUMNS) * width,
                (clamped / ATLAS_COLUMNS) * height, width, height};
    }

    static String fieldModelMapping(int fieldType, int age) {
        String crop;
        if (fieldType == FieldData.PUMPKIN) crop = "pumpkin";
        else if (fieldType == FieldData.CABBAGE) crop = "cabbage";
        else return "";
        int clampedAge = Math.max(0, Math.min(7, age));
        int modelStage = Math.max(0, (clampedAge - 1) / 2);
        return "model.terrain.farm." + crop + (modelStage + 1);
    }

    private static ResolvedModel resolveSeasonalModel(String base,
                                                       String season) {
        if (base == null || base.isEmpty()) return ResolvedModel.NONE;
        if (season != null && !season.isEmpty()) {
            ResolvedModel seasonal = resolveModel(base + season);
            if (seasonal.source != null) return seasonal;
        }
        return resolveModel(base);
    }

    private static ResolvedModel resolveModel(String mapping) {
        if (mapping == null || mapping.isEmpty()) return ResolvedModel.NONE;
        try {
            ModelResourceWrapper source = ModelResourceLoader.getModel(mapping);
            if (source != null && source != ModelResourceLoader.getBrokenModel()) {
                return new ResolvedModel(mapping, source);
            }
        } catch (Throwable error) {
            LOG.log(Level.FINE, "Unable to resolve terrain model " + mapping,
                    error);
        }
        return ResolvedModel.NONE;
    }

    private static Texture texture(String mapping) {
        if (mapping == null || mapping.isEmpty()) return null;
        try {
            return ResourceTextureLoader.getNearestTexture(mapping);
        } catch (Throwable error) {
            LOG.log(Level.FINE, "Unable to resolve terrain texture " + mapping,
                    error);
            return null;
        }
    }

    private static String seasonAppendix(World world) {
        String appendix = world.getSeasonManager().getSeasonAppendix();
        return clean(appendix);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ResolvedModel {
        private static final ResolvedModel NONE =
                new ResolvedModel("", null);

        private final String mapping;
        private final ModelResourceWrapper source;

        private ResolvedModel(String mapping, ModelResourceWrapper source) {
            this.mapping = mapping;
            this.source = source;
        }
    }
}
