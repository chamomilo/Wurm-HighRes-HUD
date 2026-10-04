package org.highreshud.core;

/** Stable service container shared through the mod launcher's class loader. */
public final class HighResHudCore {
    private final LifecycleCoordinator lifecycle = new LifecycleCoordinator();
    private final ActionBus actions = new ActionBus();
    private final MessageRouter messages = new MessageRouter();
    private final InputRouter input = new InputRouter();
    private final ExamineBroker examines = new ExamineBroker();

    public LifecycleCoordinator lifecycle() { return lifecycle; }
    public ActionBus actions() { return actions; }
    public MessageRouter messages() { return messages; }
    public InputRouter input() { return input; }
    public ExamineBroker examines() { return examines; }
}
