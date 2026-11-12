package com.rikodev.music.android.dna.genome;
import java.util.Base64;
import java.security.MessageDigest;
import java.nio.ByteBuffer;
public final class DnaCode {
 private DnaCode() {}
 public static String encode(DnaGenome g){return "RIKO1."+Base64.getUrlEncoder().withoutPadding().encodeToString(DnaCodec.encodeGenome(g));}
 public static DnaGenome decode(String text){
  if(text==null||text.length()>180||!text.startsWith("RIKO1."))throw new IllegalArgumentException("Unsupported DNA code");
  return DnaCodec.decodeGenome(Base64.getUrlDecoder().decode(text.substring(6)));
 }
 private static int avg(int a,int b){return (int)(((long)a+b)/2);}
 public static double distance(DnaGenome a,DnaGenome b){
  return (Math.abs(a.getBrightnessPpm()-b.getBrightnessPpm())+Math.abs(a.getSurfaceSharpnessPpm()-b.getSurfaceSharpnessPpm())+
    Math.abs(a.getMicroDetailPpm()-b.getMicroDetailPpm())+Math.abs(a.getTransientBurstPpm()-b.getTransientBurstPpm()))/4000000.0;
 }
 public static DnaGenome breed(DnaGenome a,DnaGenome b){
  if(a.getAlgorithmVersion()!=b.getAlgorithmVersion())throw new IllegalArgumentException("Algorithm mismatch");
  if(encode(a).compareTo(encode(b))>0){DnaGenome t=a;a=b;b=t;}
  long seed;
  try{MessageDigest d=MessageDigest.getInstance("SHA-256");d.update(DnaCodec.encodeGenome(a));seed=ByteBuffer.wrap(d.digest(DnaCodec.encodeGenome(b))).getLong();}
  catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  int[] chroma=new int[12];int sum=0;for(int i=0;i<12;i++){chroma[i]=avg(a.chromaAt(i),b.chromaAt(i));sum+=chroma[i];}
  if(sum>0){ // Normalize silent/non-silent parents as well as rounding remainders.
   int total=0;for(int i=0;i<12;i++){chroma[i]=(int)((long)chroma[i]*1000000/sum);total+=chroma[i];}chroma[0]+=1000000-total;
  }
  return new DnaGenome(a.getAlgorithmVersion(),seed,new SplitMix64(seed).nextLong(),(seed&1)==0?a.getArchetype():b.getArchetype(),
   avg(a.getParticleCount(),b.getParticleCount()),avg(a.getLayerCount(),b.getLayerCount()),avg(a.getSymmetry(),b.getSymmetry()),
   avg(a.getBodyExpansionPpm(),b.getBodyExpansionPpm()),avg(a.getBrightnessPpm(),b.getBrightnessPpm()),avg(a.getSurfaceSharpnessPpm(),b.getSurfaceSharpnessPpm()),
   avg(a.getTransientBurstPpm(),b.getTransientBurstPpm()),avg(a.getRhythmMilliBpm(),b.getRhythmMilliBpm()),avg(a.getDeformationPpm(),b.getDeformationPpm()),
   avg(a.getFlowSpeedPpm(),b.getFlowSpeedPpm()),avg(a.getMicroDetailPpm(),b.getMicroDetailPpm()),avg(a.getPaletteRotationMilliDegrees(),b.getPaletteRotationMilliDegrees()),chroma);
 }
}
