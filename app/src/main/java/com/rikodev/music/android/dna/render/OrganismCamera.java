package com.rikodev.music.android.dna.render;

/** GL-thread camera state; independent of genome and audio. Angles are degrees. */
public final class OrganismCamera {
    private float yaw, pitch, zoom = 1, targetYaw, targetPitch, targetZoom = 1;
    private float vx, vy, pulse;
    public void drag(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) return;
        vx = vy = 0; targetYaw += x; targetPitch = clamp(targetPitch + y, -75, 75);
    }
    public void scale(float factor) {
        if (Float.isFinite(factor) && factor > 0) targetZoom = clamp(targetZoom * factor, .5f, 2.5f);
    }
    public void fling(float x, float y) {
        vx = Float.isFinite(x) ? clamp(x, -90, 90) : 0;
        vy = Float.isFinite(y) ? clamp(y, -90, 90) : 0;
    }
    public void cancel() { vx = vy = 0; targetYaw = yaw; targetPitch = pitch; targetZoom = zoom; }
    public void reset() { vx = vy = 0; targetYaw = targetPitch = 0; targetZoom = 1; }
    public void pulse() { pulse = 1; }
    public void step(float dt) {
        if (!Float.isFinite(dt) || dt <= 0) return;
        dt = Math.min(dt, .05f);
        targetYaw += vx * dt; targetPitch = clamp(targetPitch + vy * dt, -75, 75);
        float decay = (float)Math.exp(-5 * dt); vx *= decay; vy *= decay;
        float k = 1 - (float)Math.exp(-14 * dt);
        yaw += (targetYaw-yaw)*k; pitch += (targetPitch-pitch)*k; zoom += (targetZoom-zoom)*k;
        if (Math.abs(yaw) > 720) { float shift = ((int)(yaw/360))*360; yaw -= shift; targetYaw -= shift; }
        pulse *= (float)Math.exp(-5 * dt);
    }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getZoom() { return zoom; }
    public float getPulse() { return pulse; }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi,v)); }
}
