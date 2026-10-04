package org.highresfocusbar.client.portrait;

import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.SubPickableUnit;
import com.wurmonline.client.renderer.TilePicker;
import com.wurmonline.client.renderer.cave.CaveWallPicker;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.cell.MobileModelRenderable;
import com.wurmonline.client.renderer.cell.StaticModelRenderable;
import com.wurmonline.client.renderer.model.AnimationData;
import com.wurmonline.client.renderer.model.ModelLoadListener;
import com.wurmonline.client.renderer.model.ModelResourceLoader;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.model.collada.AbstractColladaModelData;
import com.wurmonline.client.renderer.model.collada.importer.ColladaGeometry;
import com.wurmonline.client.renderer.model.collada.importer.ColladaModel;
import com.wurmonline.client.renderer.model.collada.importer.TriangleMesh;
import com.wurmonline.client.resources.textures.IconLoader;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.client.resources.textures.Texture;
import org.highresfocusbar.client.HighResFocusBarResources;
import org.highresfocusbar.client.HighResFocusBarSettings;
import org.highreshud.client.portrait.PortraitFraming;
import org.highreshud.client.portrait.PortraitProfile;
import org.highreshud.client.portrait.PortraitProfileRepository;
import org.highreshud.client.portrait.PortraitRenderService;
import org.highreshud.client.state.CreatureRelationState;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns the single isolated model copy and off-screen target for one HUD. */
public final class FocusPortraitController {
    public enum Disposition { NEUTRAL, FRIENDLY, HOSTILE }

    private static final Logger LOG = Logger.getLogger("HighResFocusBar");
    private static volatile Field staticModelNameField;

    private final PortraitRenderService renderer;
    private final PortraitProfileRepository repository;
    private final World world;

    private PickableUnit subject;
    private long subjectId = Long.MIN_VALUE;
    private long portraitOwnerId = Long.MIN_VALUE;
    private long generation;
    private ModelResourceWrapper pendingSource;
    private ModelLoadListener pendingListener;
    private ModelResourceWrapper model;
    private AnimationData idleAnimation;
    private long idleStarted;
    private long motionStarted = System.nanoTime();
    private PortraitProfile profile = PortraitProfile.defaults("", "");
    private Texture fallbackTexture;
    private TilePortraitPreview tilePreview;
    private boolean tileSubject;
    private boolean creature;
    private boolean manualPose;
    private float manualYaw;
    private float manualPitch;
    private float zoom = 1f;

    public FocusPortraitController(int renderSize) throws IOException {
        this(renderSize, null);
    }

    public FocusPortraitController(int renderSize, World world)
            throws IOException {
        renderer = new PortraitRenderService(renderSize);
        this.world = world;
        File override = HighResFocusBarResources.resolve(
                HighResFocusBarSettings.portraitProfilesOverride);
        repository = PortraitProfileRepository.load(override);
        LOG.info("Loaded " + repository.size()
                + " bundled/override portrait profiles"
                + (override.isFile() ? "; override=" + override : ""));
    }

    public synchronized boolean setSubject(PickableUnit next) {
        if ((next instanceof TilePicker || next instanceof CaveWallPicker)
                && world != null) {
            return setTileSubject(next);
        }
        PickableUnit portraitUnit = portraitUnit(next);
        long nextId = next == null ? Long.MIN_VALUE : next.getId();
        long nextPortraitOwnerId = portraitUnit == null
                ? Long.MIN_VALUE : portraitUnit.getId();
        ModelResourceWrapper actualSource = portraitUnit instanceof CellRenderable
                ? ((CellRenderable) portraitUnit).getModelWrapper() : null;
        String nextMapping = portraitUnit == null ? "" : mapping(portraitUnit);
        boolean nextCreature = portraitUnit instanceof CreatureCellRenderable
                && !((CreatureCellRenderable) portraitUnit).isItem();
        ModelResourceWrapper nextSource = portraitSource(
                actualSource, nextMapping, nextCreature);
        if (next != null && subjectId == nextId
                && portraitOwnerId == nextPortraitOwnerId && !tileSubject) {
            // Wurm may replace the renderable instance while retaining the
            // same selected/target id. Keep the independent model copy and
            // its looping idle instead of resetting the portrait.
            subject = next;
            if (fallbackTexture == null) {
                fallbackTexture = resolveFallback(
                        portraitUnit, nextMapping, profile);
            }
            if ((model == null || !model.isLoaded())
                    && nextSource != null && nextSource != pendingSource
                    && nextSource != ModelResourceLoader.getBrokenModel()) {
                generation++;
                clearPendingListener();
                loadIndependent(nextSource, generation);
            }
            return false;
        }
        if (subject == next && subjectId == nextId) return false;
        subject = next;
        subjectId = nextId;
        portraitOwnerId = nextPortraitOwnerId;
        generation++;
        clearPendingListener();
        clearIdleAnimation();
        model = null;
        fallbackTexture = null;
        tilePreview = null;
        tileSubject = false;
        creature = nextCreature;
        manualPose = false;
        motionStarted = System.nanoTime();

        if (next == null) {
            profile = PortraitProfile.defaults("", "");
            return true;
        }

        ModelResourceWrapper source = actualSource;
        String mapping = nextMapping;
        String resourcePath = source == null || source.getResourceUrl() == null
                ? "" : source.getResourceUrl().getFilePath();
        PortraitProfile saved = repository.resolveForSubject(
                mapping, resourcePath, creature);
        profile = saved == null
                ? PortraitProfile.defaults(mapping, resourcePath) : saved;
        if (!creature && saved == null) {
            profile.mode = PortraitProfile.Mode.FULL_MODEL;
            profile.anchor = PortraitProfile.Anchor.BOUNDS;
            profile.yawMin = -180f;
            profile.yawMax = 180f;
        }
        profile.normalize();
        manualYaw = profile.yaw;
        manualPitch = profile.pitch;
        zoom = profile.zoom;
        fallbackTexture = resolveFallback(portraitUnit, mapping, profile);

        ModelResourceWrapper portraitSource = portraitSource(
                source, mapping, creature);
        if (portraitSource != null
                && portraitSource != ModelResourceLoader.getBrokenModel()) {
            loadIndependent(portraitSource, generation);
        }
        return true;
    }

    private boolean setTileSubject(PickableUnit next) {
        long nextId = next.getId();
        if (subjectId == nextId && tilePreview != null
                && tilePreview.stillMatches(world, next)) {
            subject = next;
            return false;
        }

        TilePortraitPreview resolved = TilePortraitPreview.resolve(world, next);
        subject = next;
        subjectId = nextId;
        portraitOwnerId = nextId;
        generation++;
        clearPendingListener();
        clearIdleAnimation();
        model = null;
        tilePreview = resolved;
        tileSubject = true;
        fallbackTexture = resolved == null
                ? null : resolved.getGroundTexture();
        creature = false;
        manualPose = false;
        motionStarted = System.nanoTime();

        String mapping = resolved == null ? ""
                : resolved.getModelMapping();
        ModelResourceWrapper source = resolved == null ? null
                : resolved.getModelSource();
        String resourcePath = source == null || source.getResourceUrl() == null
                ? "" : source.getResourceUrl().getFilePath();
        PortraitProfile saved = repository.resolveForSubject(
                mapping, resourcePath, false);
        profile = saved == null
                ? PortraitProfile.defaults(mapping, resourcePath) : saved;
        profile.mode = PortraitProfile.Mode.FULL_MODEL;
        if (saved == null) {
            profile.anchor = PortraitProfile.Anchor.BOUNDS;
            profile.yawMin = -180f;
            profile.yawMax = 180f;
        }
        profile.normalize();
        manualYaw = profile.yaw;
        manualPitch = profile.pitch;
        zoom = profile.zoom;

        if (source != null && source != ModelResourceLoader.getBrokenModel()) {
            loadIndependent(source, generation);
        }
        return true;
    }

    private static ModelResourceWrapper portraitSource(
            ModelResourceWrapper actual, String mapping, boolean creature) {
        // The live wrapper is the only reliable discriminator for champion
        // and custom-server variants whose modelName can still say "human".
        // loadIndependent() requests a separate copy and strips mount meshes,
        // so using it does not leak the mutable world model into the portrait.
        if (creature && actual != null
                && actual != ModelResourceLoader.getBrokenModel()) {
            return actual;
        }
        if (mapping == null || mapping.isEmpty()) return actual;
        try {
            // Live creature copies are mutated by MountItems, while live item
            // copies may be replaced without changing the server id. Resolve
            // the real model mapping again and ask the loader for a clean,
            // independent portrait copy in both cases.
            ModelResourceWrapper clean = ModelResourceLoader.getModel(mapping);
            return clean == null || clean == ModelResourceLoader.getBrokenModel()
                    ? actual : clean;
        } catch (Throwable error) {
            return actual;
        }
    }

    private void loadIndependent(final ModelResourceWrapper source,
                                 final long expectedGeneration) {
        pendingSource = source;
        ModelLoadListener listener = new ModelLoadListener() {
            @Override
            public boolean wantNewModelCopy() {
                return true;
            }

            @Override
            public void modelLoaded(ModelResourceWrapper loaded) {
                synchronized (FocusPortraitController.this) {
                    if (generation != expectedGeneration || subject == null) return;
                    model = loaded;
                    stripDefaultMountEquipment(loaded);
                    pendingSource = null;
                    pendingListener = null;
                    prepareIdle(loaded);
                }
            }
        };
        pendingListener = listener;
        source.addLoadListener(listener);
    }

    private static void stripDefaultMountEquipment(ModelResourceWrapper loaded) {
        try {
            if (!(loaded.getModelData() instanceof AbstractColladaModelData)) return;
            ColladaModel collada = ((AbstractColladaModelData)
                    loaded.getModelData()).getModel();
            if (collada == null || !collada.getCanHaveMountItems()
                    || collada.getGeometryArray() == null) return;
            for (ColladaGeometry geometry : collada.getGeometryArray()) {
                if (geometry == null || geometry.getTriangleMeshArray() == null) {
                    continue;
                }
                for (TriangleMesh mesh : geometry.getTriangleMeshArray()) {
                    if (mesh == null) continue;
                    String name = mesh.getName() == null ? "" : mesh.getName();
                    String lower = name.toLowerCase(java.util.Locale.ROOT);
                    mesh.enableRender = lower.contains("bodymesh")
                            || lower.contains("bodylavamesh");
                    if (!mesh.enableRender) mesh.setOverridedTexture(null);
                }
            }
        } catch (Throwable error) {
            LOG.log(Level.FINE,
                    "Unable to normalize portrait mount equipment", error);
        }
    }

    public synchronized void requestPreview() {
        ModelResourceWrapper current = model;
        if (current == null || !current.isLoaded()) return;
        PortraitProfile renderProfile = motionProfile();
        renderer.requestRender(subjectId, current, renderProfile,
                idleAnimation, idleFrame());
    }

    private PortraitProfile motionProfile() {
        PortraitProfile result = profile.copy();
        // Studio allows an authored composition to start outside the optional
        // interactive zoom range. Clamping here silently enlarged profiles
        // such as pig (0.229 -> 0.65) and hen (0.404 -> 0.65).
        result.zoom = Math.max(0.01f, zoom);
        if (manualPose || result.autoTurnSpeed <= 0f) {
            result.yaw = manualYaw;
            result.pitch = manualPitch;
            return result;
        }

        float seconds = (System.nanoTime() - motionStarted) / 1_000_000_000f;
        float yawAmplitude = Math.max(0f,
                Math.min(result.yaw - result.yawMin,
                        result.yawMax - result.yaw));
        float yawPhase = yawAmplitude < 0.001f ? 0f
                : seconds * result.autoTurnSpeed / yawAmplitude;
        result.yaw = clamp(result.yaw
                        + (float) Math.sin(yawPhase) * yawAmplitude,
                result.yawMin, result.yawMax);

        float pitchAmplitude = Math.max(0f, Math.min(4f,
                Math.min(result.pitch - result.pitchMin,
                        result.pitchMax - result.pitch)));
        result.pitch = clamp(result.pitch
                        + (float) Math.sin(yawPhase * 0.43f) * pitchAmplitude,
                result.pitchMin, result.pitchMax);
        return result;
    }

    public synchronized boolean rotate(float deltaX, float deltaY) {
        if (subject == null || !profile.mouseRotation) return false;
        if (!manualPose) {
            manualYaw = motionProfile().yaw;
            manualPitch = motionProfile().pitch;
        }
        manualPose = true;
        manualYaw = clamp(manualYaw + deltaX * 0.55f,
                profile.yawMin, profile.yawMax);
        manualPitch = clamp(manualPitch + deltaY * 0.45f,
                profile.pitchMin, profile.pitchMax);
        return true;
    }

    public synchronized boolean zoom(int wheelDelta) {
        if (subject == null || wheelDelta == 0) return false;
        float minimum = Math.min(profile.zoomMin, profile.zoom);
        float maximum = Math.max(profile.zoomMax, profile.zoom);
        float step = Math.max(0.03f, (maximum - minimum) * 0.06f);
        zoom = clamp(zoom + (wheelDelta < 0 ? step : -step),
                minimum, maximum);
        return true;
    }

    public synchronized void resumeMotion() {
        manualPose = false;
        motionStarted = System.nanoTime();
    }

    public synchronized float[] crop() {
        return new float[]{profile.cropX, profile.cropY,
                profile.cropWidth, profile.cropHeight};
    }

    public synchronized Disposition disposition() {
        if (!(subject instanceof CreatureCellRenderable)
                || ((CreatureCellRenderable) subject).isItem()) {
            return Disposition.NEUTRAL;
        }
        return CreatureRelationState.isHostile(
                (CreatureCellRenderable) subject)
                ? Disposition.HOSTILE : Disposition.FRIENDLY;
    }

    public synchronized Texture fallbackTexture() {
        return fallbackTexture;
    }

    public synchronized TilePortraitPreview tilePreview() {
        return tilePreview;
    }

    public Texture texture() {
        return renderer.getTexture();
    }

    public synchronized boolean hasLiveModel() {
        return model != null && model.isLoaded();
    }

    public void beginRender() {
        renderer.beginRender();
    }

    public void endRender() {
        renderer.endRender();
    }

    public int profileCount() {
        return repository.size();
    }

    private void prepareIdle(ModelResourceWrapper loaded) {
        // A loader is allowed to notify the same independent copy again.
        // Detach the previous registration so one portrait never advances
        // two identical idle tracks at once.
        clearIdleAnimation();
        try {
            AnimationData idle = loaded.getAnimation("idle");
            if (idle == null) idle = loaded.getAnimation("Idle");
            if (idle != null) {
                idle.addToModel();
                idle.apply(0f);
                idleAnimation = idle;
                idleStarted = System.nanoTime();
            }
        } catch (Throwable error) {
            LOG.log(Level.FINE, "Portrait model has no usable idle animation", error);
        }
    }

    private float idleFrame() {
        AnimationData idle = idleAnimation;
        if (idle == null) return 0f;
        float elapsed = (System.nanoTime() - idleStarted) / 1_000_000_000f;
        return PortraitFraming.idleFraction(elapsed, idle.getLength(),
                idle.shouldLoop());
    }

    private void clearIdleAnimation() {
        AnimationData idle = idleAnimation;
        idleAnimation = null;
        if (idle == null) return;
        try {
            idle.removeFromModel();
        } catch (Throwable error) {
            LOG.log(Level.FINE, "Unable to detach previous portrait idle", error);
        }
    }

    private void clearPendingListener() {
        if (pendingSource != null && pendingListener != null) {
            pendingSource.removeLoadListener(pendingListener);
        }
        pendingSource = null;
        pendingListener = null;
    }

    public static String mapping(PickableUnit unit) {
        if (unit instanceof MobileModelRenderable) {
            MobileModelRenderable mobile = (MobileModelRenderable) unit;
            return mobile.getModelName() == null ? ""
                    : mobile.getModelName().toString();
        }
        if (unit instanceof StaticModelRenderable) {
            try {
                Field field = staticModelNameField;
                if (field == null) {
                    field = StaticModelRenderable.class
                            .getDeclaredField("modelName");
                    field.setAccessible(true);
                    staticModelNameField = field;
                }
                Object value = field.get(unit);
                return value == null ? "" : value.toString();
            } catch (Throwable error) {
                LOG.log(Level.FINE,
                        "Unable to resolve static portrait model mapping", error);
            }
        }
        return "";
    }

    /**
     * Sub-selection units represent a mesh or a terrain corner and deliberately
     * expose no portrait icon of their own. Render their owning world object
     * while keeping the original unit selected for actions and identity.
     */
    static PickableUnit portraitUnit(PickableUnit unit) {
        if (unit instanceof SubPickableUnit) {
            PickableUnit parent = ((SubPickableUnit) unit).getParent();
            if (parent != null) return parent;
        }
        return unit;
    }

    private static Texture resolveFallback(PickableUnit unit, String mapping,
                                           PortraitProfile profile) {
        try {
            if (!profile.fallbackIcon.isEmpty()) {
                Texture texture = ResourceTextureLoader.getNearestTexture(
                        profile.fallbackIcon);
                if (texture != null) return texture;
            }
            Texture direct = unit.getIconTexture();
            if (direct != null) return direct;
            short iconId = unit.getIconId();
            if (iconId >= 0) {
                Texture texture = IconLoader.getIcon(Short.valueOf(iconId));
                if (texture != null) return texture;
            }
            if (unit instanceof CreatureCellRenderable && !mapping.isEmpty()) {
                return ResourceTextureLoader.getNearestTexture("icon." + mapping);
            }
        } catch (Throwable error) {
            LOG.log(Level.FINE, "Unable to resolve portrait fallback icon", error);
        }
        return null;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
