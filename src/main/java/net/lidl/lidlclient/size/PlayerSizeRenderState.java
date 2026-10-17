package net.lidl.lidlclient.size;

/** Carries immutable model-local axis scales from extraction into rendering. */
public interface PlayerSizeRenderState {
    PlayerSizeRules.Axes lidlclient$getAxes();
    void lidlclient$setAxes(PlayerSizeRules.Axes axes);
}
