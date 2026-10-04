package org.highreshud.client.portrait;

import com.wurmonline.client.renderer.model.AnimationData;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.model.collada.ColladaAnimationJob;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import com.wurmonline.client.renderer.model.collada.importer.ColladaModel;
import com.wurmonline.client.renderer.model.collada.importer.TriangleMesh;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Advances an isolated preview model without registering a fake world cell. */
final class PortraitAnimationDriver {
    private final ColladaAnimationJob colladaJob = new ColladaAnimationJob();
    private final Method applyColladaAnimations;

    PortraitAnimationDriver() {
        try {
            applyColladaAnimations = ColladaAnimationJob.class
                    .getDeclaredMethod("applyAnimations",
                            ColladaModelData.class, ColladaModel.class,
                            boolean.class, boolean.class, boolean.class,
                            TriangleMesh.LODLevel.class);
            applyColladaAnimations.setAccessible(true);
        } catch (ReflectiveOperationException | SecurityException error) {
            throw new IllegalStateException(
                    "Wurm animation pipeline contract changed", error);
        }
    }

    void apply(ModelResourceWrapper wrapper, AnimationData animation,
               float normalizedTime) throws ReflectiveOperationException {
        animation.apply(normalizedTime);
        if (!(wrapper.getModelData() instanceof ColladaModelData)) return;

        ColladaModelData data = (ColladaModelData) wrapper.getModelData();
        try {
            // The three flags are walking, combat-target and player-body
            // special handling. Portrait copies need none of them.
            applyColladaAnimations.invoke(colladaJob, data, data.getModel(),
                    false, false, false, TriangleMesh.LODLevel.NONE);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) throw (Error) cause;
            throw new ReflectiveOperationException(cause);
        }
    }
}
