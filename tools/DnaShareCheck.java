package com.rikodev.music.android.dna.analysis;
import com.rikodev.music.android.dna.genome.*;
import java.io.*;
public class DnaShareCheck {
 public static void main(String[] args)throws Exception {
  DnaFeatures f=DnaFeatures.fromNormalized(.5,.3,.2,2000,.3,120,.8,20,new double[12]);
  DnaGenome a=DnaGenomeMapper.map(f,1,DnaAlgorithmVersion.GENESIS_V1),b=DnaGenomeMapper.map(f,2,DnaAlgorithmVersion.GENESIS_V1);
  if(!a.equals(DnaCode.decode(DnaCode.encode(a))))throw new AssertionError();
  if(!DnaCode.breed(a,b).equals(DnaCode.breed(b,a)))throw new AssertionError();
  if(DnaCode.distance(a,a)!=0)throw new AssertionError();
  try{DnaCode.decode("RIKO2.bad");throw new AssertionError();}catch(IllegalArgumentException expected){}
  DnaTimeline t=new DnaTimeline(new int[190],1000000);ByteArrayOutputStream out=new ByteArrayOutputStream();DnaTimelineCodec.write(t,out);
  if(DnaTimelineCodec.read(new ByteArrayInputStream(out.toByteArray())).getFrameCount()!=10)throw new AssertionError();
  int[] ramp=new int[38];ramp[19]=1000000;DnaTimeline rt=new DnaTimeline(ramp,200000);
  if(Math.abs(DnaTimelineSampler.sample(rt,50000,0)-.5f)>.00001)throw new AssertionError("interpolation");
  if(DnaTimelineSampler.sample(rt,-100,0)!=0||DnaTimelineSampler.sample(rt,Long.MAX_VALUE,0)!=1)throw new AssertionError("bounds");
  if(DnaTimelineSampler.mediaTimeUs(1000,2,1000000000L,1100000000L,50)!=1150000)throw new AssertionError("speed/offset");
  if(DnaTimelineSampler.mediaTimeUs(1000,0,1000000000L,1100000000L,0)!=1000000)throw new AssertionError("paused");
  out.write(1);try{DnaTimelineCodec.read(new ByteArrayInputStream(out.toByteArray()));throw new AssertionError();}catch(IOException expected){}
  System.out.println("PASS DNA code roundtrip/version rejection, symmetric offspring, comparison, timeline roundtrip/trailing-data rejection, interpolation, clock speed/offset/pause");
 }
}
