package net.lidl.lidlclient.cursor;

/** Clears queued look motion at the grab/release boundary. */
public interface CursorMouseAccess {
    void lidlclient$clearLookMotion();
    void lidlclient$releaseCursor();
    boolean lidlclient$finishCursorRelease();
}
