package org.highreshud.client.portrait;

import com.wurmonline.client.GameCrashedException;
import com.wurmonline.client.job.Job;
import com.wurmonline.client.job.JobCompletionCallback;
import com.wurmonline.client.job.JobManager;
import com.wurmonline.client.renderer.Color;
import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.ModelRenderMode;
import com.wurmonline.client.renderer.backend.Offscreen;
import com.wurmonline.client.renderer.backend.Pipeline;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.backend.RenderState;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.model.AnimationData;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import com.wurmonline.client.renderer.model.collada.importer.TriangleMesh;
import com.wurmonline.client.renderer.shaders.StateUniformManager;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.client.util.GLHelper;
import org.highreshud.client.portrait.PortraitRenderKey;
import org.highreshud.client.portrait.SharedPortraitChannel;
import org.highreshud.client.portrait.SharedPortraitCoordinator;

import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Independent model-to-texture renderer following Wurm's PaperDoll job flow. */
public final class PortraitRenderService implements Job, JobCompletionCallback,
        SharedPortraitChannel {
    private static final Logger LOG = Logger.getLogger("HighResHud.Portraits");

    private final int renderSize;
    private final Offscreen offscreen = new Offscreen();
    private final Pipeline pipeline = new Pipeline(offscreen);
    private final Queue queue = new Queue(128, true);
    private final StudioLightManager lightManager = new StudioLightManager();
    private final RenderState renderState = new RenderState();
    private volatile PortraitAnimationDriver animationDriver;
    private volatile boolean animationDriverUnavailable;

    private volatile ModelResourceWrapper requestedModel;
    private volatile long requestedSubjectId = Long.MIN_VALUE;
    private volatile PortraitProfile requestedProfile;
    private volatile AnimationData requestedAnimation;
    private volatile float requestedAnimationTime;
    private volatile boolean shouldRender;
    private volatile int jobToken = -1;
    private volatile boolean jobDone;
    private volatile long animationFrames;
    private volatile String animationError = "";
    private volatile SharedPortraitChannel sharedFrom;

    public PortraitRenderService(int renderSize) {
        this.renderSize = renderSize;
        if (GLHelper.useDeferredShading()) pipeline.setUseModern(true);
        pipeline.addQueue(Queue.QUEUE_OBJECTS, queue);
    }

    public void requestRender(ModelResourceWrapper model,
                              PortraitProfile profile) {
        requestRender(model == null ? Long.MIN_VALUE
                : System.identityHashCode(model), model, profile, null, 0f);
    }

    public void requestRender(ModelResourceWrapper model,
                              PortraitProfile profile,
                              AnimationData animation,
                              float normalizedAnimationTime) {
        requestRender(model == null ? Long.MIN_VALUE
                        : System.identityHashCode(model),
                model, profile, animation, normalizedAnimationTime);
    }

    public void requestRender(long subjectId, ModelResourceWrapper model,
                              PortraitProfile profile,
                              AnimationData animation,
                              float normalizedAnimationTime) {
        if (model == null || profile == null || !model.isLoaded()) return;
        requestedSubjectId = subjectId;
        requestedModel = model;
        requestedProfile = profile.copy();
        requestedAnimation = animation;
        requestedAnimationTime = normalizedAnimationTime;
        shouldRender = true;
    }

    public void beginRender() {
        if (!shouldRender || jobToken != -1) return;
        SharedPortraitCoordinator.request(this);
    }

    public void endRender() {
        // The unified coordinator completes every unique target exactly once.
    }

    @Override
    public PortraitRenderKey renderKey() {
        ModelResourceWrapper model = requestedModel;
        String resource = model == null || model.getResourceUrl() == null
                ? "" : model.getResourceUrl().getFilePath();
        PortraitProfile profile = requestedProfile;
        return new PortraitRenderKey(requestedSubjectId, renderSize, resource,
                profile == null ? "" : profile.renderKey());
    }

    @Override
    public void scheduleOwnRender() {
        if (!shouldRender || jobToken != -1) return;
        jobDone = false;
        jobToken = JobManager.getInstance().nextToken();
        JobManager.getInstance().schedule(jobToken, this, null, this);
    }

    @Override
    public void finishOwnRender() {
        if (jobToken == -1) return;
        while (!jobDone) JobManager.getInstance().yield();
        jobToken = -1;

        if (offscreen.getWidth() != renderSize
                || offscreen.getHeight() != renderSize) {
            offscreen.init(renderSize, renderSize,
                    true, false, false, true, false);
        }
        if (GLHelper.useDeferredShading()) {
            FloatBuffer sun = StateUniformManager.get().getSunData();
            sun.rewind();
            lightManager.addSunLight(sun);
            StateUniformManager.get().bumpSunState();
        }
        pipeline.flush();
        queue.clear();
    }

    @Override
    public Texture ownTexture() {
        return offscreen.getTexture();
    }

    @Override
    public void shareFrom(SharedPortraitChannel leader) {
        sharedFrom = leader;
        if (leader != null) shouldRender = false;
    }

    @Override
    public void execute(Object context) {
        if (!shouldRender) return;
        shouldRender = false;
        ModelResourceWrapper model = requestedModel;
        PortraitProfile profile = requestedProfile;
        AnimationData animation = requestedAnimation;
        float animationTime = requestedAnimationTime;
        if (model == null || profile == null || !model.isLoaded()) return;

        PortraitAnimationDriver driver = animation == null
                ? null : animationDriver();
        if (animation != null && driver != null) {
            try {
                driver.apply(model, animation,
                        animationTime);
                animationFrames++;
                animationError = "";
            } catch (Throwable error) {
                String failure = error.getClass().getSimpleName();
                if (!failure.equals(animationError)) {
                    LOG.log(Level.WARNING,
                            "Unable to calculate portrait idle animation", error);
                }
                animationError = failure;
            }
        }

        pipeline.setViewport(0, 0, renderSize, renderSize);
        pipeline.clear(true, true, Color.ZERO, 1f);
        queue.getProjectionMatrix().perspectiveProjection(1f,
                profile.fov, 0.01f, 10_000f);
        queue.setViewMatrix(Matrix.identityMatrix);
        queue.setPrimaryLightManager(lightManager);
        queue.setNoFog();

        if (model.getModelData() instanceof ColladaModelData) {
            ((ColladaModelData) model.getModelData()).setShouldBeRendered(true);
        }
        Matrix modelMatrix = PortraitFraming.modelMatrix(model, profile, 1f);
        model.render(queue, modelMatrix, null, null, null, null,
                renderState, ModelRenderMode.RENDER_NORMAL, 0, 0f,
                TriangleMesh.LODLevel.NONE);
    }

    private PortraitAnimationDriver animationDriver() {
        PortraitAnimationDriver result = animationDriver;
        if (result != null || animationDriverUnavailable) return result;
        synchronized (this) {
            result = animationDriver;
            if (result != null || animationDriverUnavailable) return result;
            try {
                result = new PortraitAnimationDriver();
                animationDriver = result;
            } catch (Throwable error) {
                animationDriverUnavailable = true;
                animationError = error.getClass().getName() + ": "
                        + String.valueOf(error.getMessage());
                LOG.log(Level.WARNING,
                        "Idle animation unavailable; live 3D portrait remains enabled: "
                                + animationError, error);
            }
            return result;
        }
    }

    public Texture getTexture() {
        SharedPortraitChannel leader = sharedFrom;
        return leader == null ? offscreen.getTexture() : leader.ownTexture();
    }

    public int getRenderSize() {
        return renderSize;
    }

    public long getAnimationFrames() {
        return animationFrames;
    }

    public String getAnimationError() {
        return animationError;
    }

    public void resetAnimationDiagnostics() {
        animationFrames = 0L;
        animationError = "";
    }

    @Override
    public void onJobComplete(int token) {
        if (token != jobToken) {
            throw GameCrashedException.forFailure(
                    "Unknown job in Portrait Studio callback");
        }
        jobDone = true;
    }
}
