package org.highreshud.client.portrait;

import java.util.Objects;

/** Compatibility key: only genuinely interchangeable renders may share it. */
public final class PortraitRenderKey {
    private final long subjectId;
    private final int renderSize;
    private final String modelResource;
    private final String profile;

    public PortraitRenderKey(long subjectId, int renderSize,
                             String modelResource, String profile) {
        this.subjectId = subjectId;
        this.renderSize = renderSize;
        this.modelResource = modelResource == null ? "" : modelResource;
        this.profile = profile == null ? "" : profile;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PortraitRenderKey)) return false;
        PortraitRenderKey key = (PortraitRenderKey) other;
        return subjectId == key.subjectId && renderSize == key.renderSize
                && modelResource.equals(key.modelResource)
                && profile.equals(key.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjectId, renderSize, modelResource, profile);
    }
}
