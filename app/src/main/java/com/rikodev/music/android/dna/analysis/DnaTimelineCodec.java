package com.rikodev.music.android.dna.analysis;
import java.io.*;
public final class DnaTimelineCodec {
 private static final int MDT1=0x4d445431, MDT2=0x4d445432;
 private DnaTimelineCodec() {}
 public static void write(DnaTimeline t, OutputStream stream) throws IOException {
  // Preserve the original encoding byte-for-byte for zero-origin profiles.
  DataOutputStream out=new DataOutputStream(stream);boolean offset=t.getStartUs()!=0;
  out.writeInt(offset?MDT2:MDT1);out.writeLong(t.getDurationUs());
  if(offset)out.writeLong(t.getStartUs());out.writeInt(t.getFrameCount());
  for(int i=0;i<t.getFrameCount();i++)for(int c=0;c<DnaTimeline.WIDTH;c++)out.writeInt(t.valueAt(i,c));out.flush();
 }
 public static DnaTimeline read(InputStream stream) throws IOException {
  DataInputStream in=new DataInputStream(stream);int version=in.readInt();
  if(version!=MDT1&&version!=MDT2)throw new IOException("Timeline version");
  long duration=in.readLong(),start=version==MDT2?in.readLong():0;int count=in.readInt();
  try{DnaTimeline.validateBounds(start,duration,count);}catch(IllegalArgumentException error){throw new IOException("Timeline bounds",error);}
  int[] values=new int[count*DnaTimeline.WIDTH];
  for(int i=0;i<values.length;i++){int v=in.readInt();if(v<0||v>(i%DnaTimeline.WIDTH==DnaTimeline.CENTROID?11025000:1000000))throw new IOException("Feature range");values[i]=v;}
  if(in.read()!=-1)throw new IOException("Trailing bytes");return new DnaTimeline(values,start,duration);
 }
}
