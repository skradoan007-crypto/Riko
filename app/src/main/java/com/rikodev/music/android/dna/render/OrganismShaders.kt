package com.rikodev.music.android.dna.render

/** Shared ESSL geometry program; archetype selects topology, genome selects the organism. */
internal object OrganismShaders {
    const val VERT = """
#version 300 es
precision highp float;
precision highp int;
uniform mat4 uMvp;
uniform float uTime, uBass, uMid, uTreble, uPulse, uBurst, uEvolution, uBaseLayers;
uniform float uPointsPerLayer, uPointScale, uGain, uSeed, uFamily;
uniform vec4 uDna, uTraits;
uniform vec3 uCamDir;
uniform float uBands[40];
uniform float uChroma[12];
out vec3 vColor;
out float vPin;
const float PI=3.14159265359;
const float TAU=6.28318530718;
float spectrum(float x){
 float pos=clamp(x,0.0,1.0)*38.999;
 int index=int(pos);
 return mix(uBands[index],uBands[index+1],fract(pos));
}
void main(){
 float id=mod(float(gl_VertexID),uPointsPerLayer);
 float layer=floor(float(gl_VertexID)/uPointsPerLayer);
 float f=(id+0.5)/uPointsPerLayer;
 float cols=uPointsPerLayer/96.0;
 float a=mod(id,cols)/cols*TAU;
 // Geometry is a function of spectrum, never elapsed wall time.
 float t=0.0;
 float phase=uSeed*TAU;
 float fold=sin(a*uTraits.x+f*PI*(3.0+uDna.z*3.0)+t+phase);
 float sector=abs(sin(a*.5));
 float local=spectrum(sector);
 float fine=spectrum(fract(f*3.0));
 vec3 p;
 if(uFamily<0.5){
  float theta=f*PI;
  float r=(0.78-layer*0.045)*(1.0+uBass*(.18+uDna.x*.25)+uPulse*(.06+uBurst*.06));
  r+=sin(theta)*fold*(0.012+uMid*uDna.w*.10);
  r+=local*(.12+.14*uDna.w)*(0.45+0.55*sin(theta));
  p=vec3(sin(theta)*cos(a),cos(theta),sin(theta)*sin(a))*r;
 }else if(uFamily<1.5){
  // Serpentine rows keep a single continuous ribbon per layer (one draw call).
  float across=mod(id,cols)/max(cols-1.0,1.0);
  float x=mix(across,1.0-across,mod(floor(id/cols),2.0))*2.0-1.0;
  float z=f*2.0-1.0;
  float y=sin(x*(3.0+uTraits.x)+t+phase)*cos(z*4.0-t)*(.015+uMid*.12);
  y+=sin(length(vec2(x,z))*14.0-t*2.0)*(.008+uBass*.09+uPulse*uBurst*.035);
  y+=spectrum(across)*(.28+.2*uDna.w);
  p=vec3(x,y+layer*.035,z)*.85;
 }else{
  float y=1.0-2.0*f;
  float angle=id*2.39996323+phase;
  float r=sqrt(max(0.0,1.0-y*y));
  float ribbon=.65+.12*sin(angle*.07+y*6.0+t+layer);
  p=vec3(cos(angle)*r,y,sin(angle)*r)*ribbon;
  p+=vec3(sin(y*7.0+t),cos(angle+y*4.0-t),sin(y*5.0-t))*(.015+uMid*.10+local*.08);
  p*=1.0+uBass*.16+fine*.12;
  p+=normalize(p+vec3(.001))*uPulse*uBurst*.09*exp(-12.0*(y-uSeed)*(y-uSeed));
 }
 p+=normalize(p+vec3(.001))*uEvolution*.025*sin(a*7.0+f*31.0+t*.5);
 p+=normalize(p+vec3(.001))*sin(a*19.0+f*43.0+t)*uTreble*uTraits.z*.012;
 gl_Position=uMvp*vec4(p,1.0);
 float selector=fract(f*.75 + .12*sin(a+phase) + uSeed*.2);
 float cumulative=0.0;
 float pitch=0.0;
 for(int i=0;i<12;i++){cumulative+=uChroma[i];if(selector>cumulative)pitch=float(i+1);}
 // A contiguous cyan/blue/violet palette; chroma controls its spatial balance.
 float hue=.48 + .32*(pitch/11.0) + .08*sin(uTraits.w*TAU);
 vec3 color=.5+.5*cos(TAU*(hue+vec3(0.0,.67,.33)));
 float facing=dot(normalize(p),uCamDir);
 float rim=pow(1.0-abs(facing),2.0);
 float light=(.13+.30*uDna.y+local*.65+uTreble*.16)*(0.35+rim*.8)*mix(.35,1.0,smoothstep(-.15,.2,facing));
 vColor=mix(color,vec3(.8,.9,1.0),.18)*light*uGain/(1.0+layer*.55);
 vColor*=clamp(uBaseLayers+uEvolution*4.0-layer,0.0,1.0);
 vPin=1.0;
 gl_PointSize=clamp(uPointScale*(1.1+uTraits.z)*3.0/max(gl_Position.w,.1),1.0,4.0);
}
"""
    const val FRAG = """
#version 300 es
precision highp float;
in vec3 vColor;
in float vPin;
uniform float uLine;
out vec4 fragColor;
void main(){
 if(uLine>.5){fragColor=vec4(vColor*.48,1.0);return;}
 vec2 p=gl_PointCoord*2.0-1.0;
 float d=dot(p,p);if(d>1.0)discard;
 fragColor=vec4(vColor*exp(-d*3.5)*.55,1.0);
}
"""
}
