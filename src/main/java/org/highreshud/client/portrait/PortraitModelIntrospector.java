package org.highreshud.client.portrait;

import com.wurmonline.client.renderer.model.AbstractModelData;
import com.wurmonline.client.renderer.model.ModelDataBounds;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.model.collada.AbstractColladaModelData;
import com.wurmonline.client.renderer.model.collada.animation.Joint;
import com.wurmonline.client.renderer.model.collada.importer.ColladaModel;
import com.wurmonline.client.renderer.model.collada.math.Matrix4f;
import com.wurmonline.math.Vector;
import com.wurmonline.math.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PortraitModelIntrospector {
    public static final class Snapshot {
        public final ModelDataBounds bounds;
        public final List<String> joints;
        public final String resolvedJoint;
        public final float anchorX;
        public final float anchorH;
        public final float anchorY;
        public final boolean boundsFallback;

        Snapshot(ModelDataBounds bounds, List<String> joints,
                 String resolvedJoint, float anchorX, float anchorH,
                 float anchorY, boolean boundsFallback) {
            this.bounds = bounds;
            this.joints = Collections.unmodifiableList(joints);
            this.resolvedJoint = resolvedJoint;
            this.anchorX = anchorX;
            this.anchorH = anchorH;
            this.anchorY = anchorY;
            this.boundsFallback = boundsFallback;
        }
    }

    private PortraitModelIntrospector() {
    }

    public static Snapshot inspect(ModelResourceWrapper wrapper,
                                   PortraitProfile profile) {
        if (wrapper == null || !wrapper.isLoaded() || wrapper.getBounds() == null) {
            return new Snapshot(new ModelDataBounds(-0.5f, 0f, -0.5f,
                    0.5f, 1f, 0.5f), Collections.emptyList(), "",
                    0f, 0.8f, 0f, true);
        }

        ModelDataBounds bounds = wrapper.getBounds();
        AbstractModelData data = wrapper.getModelData();
        ColladaModel model = null;
        if (data instanceof AbstractColladaModelData) {
            model = ((AbstractColladaModelData) data).getModel();
        }

        Set<String> names = new LinkedHashSet<>();
        if (model != null) {
            names.addAll(model.getJoints().keySet());
            names.addAll(model.getModelsOffsetToNullList().keySet());
        }
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);

        float centerX = midpoint(bounds.getX0(), bounds.getX1());
        float centerY = midpoint(bounds.getY0(), bounds.getY1());
        float height = Math.max(0.001f, bounds.getH1() - bounds.getH0());
        float factor;
        if (profile.mode == PortraitProfile.Mode.FULL_MODEL
                || profile.anchor == PortraitProfile.Anchor.BOUNDS) {
            factor = 0.5f;
        } else if (profile.mode == PortraitProfile.Mode.BUST) {
            factor = 0.72f;
        } else {
            factor = 0.84f;
        }
        float fallbackH = bounds.getH0() + height * factor;

        String selected = resolveJoint(profile, sorted);
        float[] point = selected.isEmpty() ? null
                : jointPoint(data, model, selected, bounds,
                centerX, fallbackH, centerY);
        if (point != null) {
            return new Snapshot(bounds, sorted, selected,
                    point[0], point[1], point[2], false);
        }

        return new Snapshot(bounds, sorted, "", centerX,
                fallbackH, centerY, true);
    }

    static String resolveJoint(PortraitProfile profile, List<String> names) {
        if (profile.anchor == PortraitProfile.Anchor.BOUNDS || names.isEmpty()) {
            return "";
        }
        if (profile.anchor == PortraitProfile.Anchor.CUSTOM) {
            return exactIgnoreCase(profile.joint, names);
        }

        List<String> tokens;
        switch (profile.anchor) {
            case HEAD: tokens = Collections.singletonList("head"); break;
            case JAW: tokens = Collections.singletonList("jaw"); break;
            case NECK: tokens = Collections.singletonList("neck"); break;
            case AUTO:
            default:
                tokens = Arrays.asList("head", "jaw", "neck", "face", "skull");
                break;
        }
        for (String token : tokens) {
            String best = bestTokenMatch(token, names);
            if (!best.isEmpty()) return best;
        }
        return "";
    }

    private static String exactIgnoreCase(String wanted, List<String> names) {
        for (String name : names) if (name.equalsIgnoreCase(wanted)) return name;
        return "";
    }

    private static String bestTokenMatch(String token, List<String> names) {
        String normalizedToken = normalize(token);
        String best = "";
        int bestScore = Integer.MIN_VALUE;
        for (String name : names) {
            String normalized = normalize(name);
            int score;
            if (normalized.equals(normalizedToken)) score = 1000;
            else if (normalized.endsWith(normalizedToken)) score = 800;
            else if (normalized.startsWith(normalizedToken)) score = 700;
            else if (normalized.contains(normalizedToken)) score = 500;
            else continue;
            score -= normalized.length();
            if (score > bestScore) {
                best = name;
                bestScore = score;
            }
        }
        return best;
    }

    private static float[] jointPoint(AbstractModelData data,
                                      ColladaModel model, String name,
                                      ModelDataBounds bounds,
                                      float expectedX, float expectedH,
                                      float expectedY) {
        List<float[]> points = new ArrayList<>();
        if (model != null) {
            Joint joint = model.getJoint(name);
            if (joint != null) {
                Matrix4f[] candidates = new Matrix4f[]{
                        joint.getForwardKinematicsMatrixTotal(),
                        joint.getAnimationMatrix(),
                        joint.getBindMatrix(),
                        joint.getOriginalBindMatrix()
                };
                for (Matrix4f matrix : candidates) {
                    if (matrix == null) continue;
                    Vector3f value = matrix.toTranslationVector();
                    float[] point = new float[]{value.x, value.y, value.z};
                    if (plausible(point, bounds)) points.add(point);
                }
            }
        }
        if (data != null && data.hasNull(name)) {
            Vector offset = data.getNullOffset(name, new Vector());
            if (offset != null) {
                float[] point = new float[]{offset.x(), offset.y(), offset.z()};
                if (plausible(point, bounds)) points.add(point);
            }
        }
        return closestPoint(points, bounds, expectedX, expectedH, expectedY);
    }

    static float[] closestPoint(List<float[]> points, ModelDataBounds bounds,
                                float expectedX, float expectedH,
                                float expectedY) {
        float xSpan = Math.max(0.001f, bounds.getX1() - bounds.getX0());
        float hSpan = Math.max(0.001f, bounds.getH1() - bounds.getH0());
        float ySpan = Math.max(0.001f, bounds.getY1() - bounds.getY0());
        float[] best = null;
        float bestScore = Float.POSITIVE_INFINITY;
        for (float[] point : points) {
            float dx = (point[0] - expectedX) / xSpan;
            float dh = (point[1] - expectedH) / hSpan;
            float dy = (point[2] - expectedY) / ySpan;
            float score = dx * dx + dh * dh + dy * dy;
            if (score < bestScore) {
                best = point;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean plausible(float[] point, ModelDataBounds bounds) {
        for (float value : point) if (!Float.isFinite(value)) return false;
        float span = Math.max(0.001f, Math.max(
                bounds.getH1() - bounds.getH0(), Math.max(
                        bounds.getX1() - bounds.getX0(),
                        bounds.getY1() - bounds.getY0())));
        float margin = span * 2f;
        return point[0] >= bounds.getX0() - margin
                && point[0] <= bounds.getX1() + margin
                && point[1] >= bounds.getH0() - margin
                && point[1] <= bounds.getH1() + margin
                && point[2] >= bounds.getY0() - margin
                && point[2] <= bounds.getY1() + margin;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace("_", "").replace("-", "").replace(" ", "");
    }

    private static float midpoint(float a, float b) {
        return (a + b) * 0.5f;
    }
}
