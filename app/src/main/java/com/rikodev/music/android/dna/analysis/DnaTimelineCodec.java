package com.rikodev.music.android.dna.analysis;
import java.io.*;
public final class DnaTimelineCodec {
 private DnaTimelineCodec() {}
 public static void write(DnaTimeline t, OutputStream stream) throws IOException {
  DataOutputStream out=new DataOutputStream(stream);out.writeInt(0x4d445431);out.writeLong(t.getDurationUs());out.writeInt(t.getFrameCount());
  for(int i=0;i<t.getFrameCount();i++)for(int c=0;c<DnaTimeline.WIDTH;c++)out.writeInt(t.valueAt(i,c));out.flush();
 }
 public static DnaTimeline read(InputStream stream) throws IOException {
  DataInputStream in=new DataInputStream(stream);if(in.readInt()!=0x4d445431)throw new IOException("Timeline version");
  long duration=in.readLong();int count=in.readInt();
  if(duration<0||duration>21600000000L||count<1||count>216001)throw new IOException("Timeline bounds");
  int[] values=new int[count*DnaTimeline.WIDTH];
  for(int i=0;i<values.length;i++){int v=in.readInt();if(v<0||v>(i%DnaTimeline.WIDTH==DnaTimeline.CENTROID?11025000:1000000))throw new IOException("Feature range");values[i]=v;}
  if(in.read()!=-1)throw new IOException("Trailing bytes");return new DnaTimeline(values,duration);
 }
}
