package org.highresfocusbar.client;

import com.wurmonline.client.renderer.Color;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.SubPickableUnit;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.backend.RenderState;
import com.wurmonline.client.resources.textures.Texture;

import java.util.List;

/**
 * Delegates the stock picked-object render pass while replacing only its
 * outline colour. This keeps the effect compatible with creatures, corpses,
 * ground items, and any other PickableUnit implementation.
 */
final class SelectedOutlinePickable implements PickableUnit {
    private static volatile SelectedOutlinePickable cached;

    private final PickableUnit delegate;

    private SelectedOutlinePickable(PickableUnit delegate) {
        this.delegate = delegate;
    }

    static PickableUnit wrap(PickableUnit unit) {
        if (unit == null) return null;
        SelectedOutlinePickable current = cached;
        if (current == null || current.delegate != unit) {
            current = new SelectedOutlinePickable(unit);
            cached = current;
        }
        return current;
    }

    static PickableUnit unwrap(PickableUnit unit) {
        return unit instanceof SelectedOutlinePickable
                ? ((SelectedOutlinePickable) unit).delegate : unit;
    }

    @Override
    public void getHoverDescription(PickData pickData) {
        delegate.getHoverDescription(pickData);
    }

    @Override
    public String getHoverName() {
        return delegate.getHoverName();
    }

    @Override
    public void renderPicked(Queue queue, RenderState state, Color color) {
        delegate.renderPicked(queue, state, color);
    }

    @Override
    public long getId() {
        return delegate.getId();
    }

    @Override
    public Color getOutlineColor() {
        double seconds = System.nanoTime() / 1_000_000_000.0;
        float warmWave = (float) ((Math.sin(seconds * 2.8) + 1.0) * 0.5);
        float shimmer = (float) ((Math.sin(seconds * 6.7 + 0.9) + 1.0) * 0.5);
        return new Color(1.0f,
                0.76f + warmWave * 0.22f,
                0.065f + shimmer * 0.15f,
                0.42f + shimmer * 0.18f);
    }

    @Override
    public void pick(Queue queue, boolean close) {
        delegate.pick(queue, close);
    }

    @Override
    public boolean targetMatches(int targetType) {
        return delegate.targetMatches(targetType);
    }

    @Override
    public List<SubPickableUnit> getSubPickableUnitList() {
        return delegate.getSubPickableUnitList();
    }

    @Override
    public Texture getIconTexture() {
        return delegate.getIconTexture();
    }

    @Override
    public short getIconId() {
        return delegate.getIconId();
    }

    @Override
    public void preparePick() {
        delegate.preparePick();
    }
}
