package com.rikodev.music.android.dna.analysis;
/** Allocation-free sampling; media time is independent of the display monotonic clock. */
public final class DnaTimelineSampler {
 private DnaTimelineSampler() {}
 public static float sample(DnaTimeline t,long us,int column) {
  if(t==null||t.getFrameCount()==0||us<t.getStartUs())return 0;
  us-=t.getStartUs();long raw=us/DnaTimeline.STEP_US;
  int frame=(int)Math.min(raw,t.getFrameCount()-1);
  int next=Math.min(frame+1,t.getFrameCount()-1);
  float f=(us%DnaTimeline.STEP_US)/(float)DnaTimeline.STEP_US;
  return (t.valueAt(frame,column)*(1-f)+t.valueAt(next,column)*f)/1000000f;
 }
 public static long mediaTimeUs(long positionMs,float speed,long anchorNanos,long displayNanos,int offsetMs) {
  if(!Float.isFinite(speed)||speed<0)speed=0;
  double elapsed=Math.max(0,Math.min(250,(displayNanos-anchorNanos)/1000000.0));
  return (long)Math.max(0,(positionMs+elapsed*speed-offsetMs)*1000);
 }
}
