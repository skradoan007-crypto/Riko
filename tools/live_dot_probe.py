"""Compile/link and render the checked-in ESSL with Mesa surfaceless EGL.
Requires Python, numpy, Pillow and libEGL; no Android/emulator/device claim.
Run from repository root: MESA_GLES_VERSION_OVERRIDE=2.0 python tools/live_dot_probe.py
"""
import ctypes as C
import hashlib
import json
import math
from pathlib import Path
import re
import sys
import numpy as np
from PIL import Image

E = C.CDLL('libEGL.so.1')
E.eglGetProcAddress.argtypes = [C.c_char_p]
E.eglGetProcAddress.restype = C.c_void_p

def egl(name, restype, args):
    f = getattr(E, name); f.restype = restype; f.argtypes = args; return f

def gl(name, restype, *args):
    addr = E.eglGetProcAddress(name.encode())
    if not addr: raise RuntimeError(name)
    return C.CFUNCTYPE(restype, *args)(addr)

I, U, F, P = C.c_int, C.c_uint, C.c_float, C.c_void_p
ints = lambda *a: (I * len(a))(*a)
get_display = C.CFUNCTYPE(P, U, P, C.POINTER(I))(E.eglGetProcAddress(b'eglGetPlatformDisplayEXT'))
display = get_display(0x31DD, None, None)
major, minor = I(), I()
assert egl('eglInitialize', U, [P, C.POINTER(I), C.POINTER(I)])(display, C.byref(major), C.byref(minor))
assert egl('eglBindAPI', U, [U])(0x30A0)
config, count = P(), I()
assert egl('eglChooseConfig', U, [P, C.POINTER(I), C.POINTER(P), I, C.POINTER(I)])(
    display, ints(0x3033,1,0x3040,4,0x3024,8,0x3023,8,0x3022,8,0x3038), C.byref(config), 1, C.byref(count))
assert count.value > 0
surface = egl('eglCreatePbufferSurface', P, [P,P,C.POINTER(I)])(display,config,ints(0x3057,960,0x3056,960,0x3038))
context = egl('eglCreateContext', P, [P,P,P,C.POINTER(I)])(display,config,None,ints(0x3098,2,0x3038))
assert egl('eglMakeCurrent',U,[P,P,P,P])(display,surface,surface,context)
get_string=gl('glGetString',C.c_char_p,U)
create_shader=gl('glCreateShader',U,U)
shader_source=gl('glShaderSource',None,U,I,C.POINTER(C.c_char_p),P)
compile_shader=gl('glCompileShader',None,U)
get_shader=gl('glGetShaderiv',None,U,U,C.POINTER(I))
get_shader_log=gl('glGetShaderInfoLog',None,U,I,P,C.c_char_p)
create_program=gl('glCreateProgram',U)
attach=gl('glAttachShader',None,U,U)
link=gl('glLinkProgram',None,U)
get_program=gl('glGetProgramiv',None,U,U,C.POINTER(I))
get_program_log=gl('glGetProgramInfoLog',None,U,I,P,C.c_char_p)
delete_shader=gl('glDeleteShader',None,U)
delete_program=gl('glDeleteProgram',None,U)

def program(vs, fs):
    p=create_program()
    for kind, text in [(0x8B31,vs),(0x8B30,fs)]:
        s=create_shader(kind); source=C.c_char_p(text.strip().encode())
        shader_source(s,1,C.byref(source),None); compile_shader(s)
        ok=I();get_shader(s,0x8B81,C.byref(ok))
        if not ok.value:
            buf=C.create_string_buffer(8192);get_shader_log(s,8192,None,buf);raise AssertionError(buf.value.decode())
        attach(p,s);delete_shader(s)
    link(p);ok=I();get_program(p,0x8B82,C.byref(ok))
    if not ok.value:
        buf=C.create_string_buffer(8192);get_program_log(p,8192,None,buf);raise AssertionError(buf.value.decode())
    return p

# Host evidence only: compile actual production ESSL, render synthetic spectra,
# and optionally feed a user-supplied audio fixture. No Android timing claim.
import subprocess
import argparse
parser=argparse.ArgumentParser()
parser.add_argument('--audio')
parser.add_argument('--output',default='build/host-live-dot-probe')
args=parser.parse_args()
out=Path(args.output);out.mkdir(parents=True,exist_ok=True)
source=Path('app/src/main/java/com/rikodev/music/android/dna/render/LiveDotShaders.kt').read_text()
shaders=dict(re.findall(r'const val (\w+)\s*=\s*"""(.*?)"""',source,re.S))
assert 'uTime' not in shaders['VERT']
p=program(shaders['VERT'],shaders['FRAG'])
use=gl('glUseProgram',None,U);use(p)
loc=gl('glGetUniformLocation',I,U,C.c_char_p)
u1=gl('glUniform1f',None,I,F);u2=gl('glUniform2f',None,I,F,F)
ufv=gl('glUniform1fv',None,I,I,P)
u=lambda name:loc(p,name.encode())
buffer=U();gl('glGenBuffers',None,I,C.POINTER(U))(1,C.byref(buffer))
gl('glBindBuffer',None,U,U)(0x8892,buffer)
ids=np.arange(15360,dtype=np.float32)
gl('glBufferData',None,U,C.c_ssize_t,P,U)(0x8892,ids.nbytes,ids.ctypes.data,0x88E4)
attribute=gl('glGetAttribLocation',I,U,C.c_char_p)(p,b'aId')
gl('glEnableVertexAttribArray',None,U)(attribute)
gl('glVertexAttribPointer',None,U,I,U,U,I,P)(attribute,1,0x1406,0,4,None)
gl('glEnable',None,U)(0x0BE2);gl('glBlendFunc',None,U,U)(1,1)
gl('glClearColor',None,F,F,F,F)(0,0,0,1)
viewport=gl('glViewport',None,I,I,I,I);clear=gl('glClear',None,U)
draw=gl('glDrawArrays',None,U,I,I);read=gl('glReadPixels',None,I,I,I,I,U,U,P)
get_error=gl('glGetError',U)
max_size=(F*2)();gl('glGetFloatv',None,U,P)(0x846D,max_size)
bass_end=int(math.log(250/40)/math.log(44100/2/40)*40)
def render(fast,trail=None,w=720,h=720,points=9216,palette=0,seed=0):
    fast=np.asarray(fast,dtype=np.float32);trail=fast if trail is None else np.asarray(trail,dtype=np.float32)
    viewport(0,0,w,h);clear(0x4000)
    u2(u('uViewport'),w,h);u1(u('uColumns'),points/48)
    u1(u('uPointSize'),min(6,max(1.6,min(w,h)/260)))
    u1(u('uMaxPointSize'),max_size[1]);u1(u('uPalette'),palette)
    u1(u('uGain'),1.5);u1(u('uSeed'),seed)
    u1(u('uBass'),min(1,float((fast[:bass_end]**4).sum()**.25)))
    ufv(u('uBands[0]'),40,fast.ctypes.data);ufv(u('uTrail[0]'),40,trail.ctypes.data)
    draw(0,0,points)
    pixels=np.zeros((h,w,4),np.uint8);read(0,0,w,h,0x1908,0x1401,pixels.ctypes.data)
    assert get_error()==0
    return pixels[::-1,:,:3].copy()
zero=np.zeros(40,np.float32)
quiet=render(zero)
assert np.array_equal(quiet,render(zero)), 'silence must not animate'
constant=np.linspace(.03,.4,40,dtype=np.float32)
assert np.array_equal(render(constant),render(constant)), 'constant input must not animate'
one=zero.copy();one[24]=.7
localized=render(one)
changed=np.any(localized!=quiet,axis=2)
assert 0 < changed.sum() < 720*720*.10, 'single mid-band must act locally'
bass=zero.copy();bass[:bass_end]=.5
loud=render(bass)
assert np.abs(loud.astype(int)-quiet.astype(int)).sum()>10000
assert render(constant*.2).sum()<render(constant).sum(), 'quiet input must stay dimmer'
for count in (3840,9216,15360):
    for w,h in ((360,800),(800,360),(720,720)):
        pixels=render(np.ones(40,np.float32),w=w,h=h,points=count)
        assert pixels[0].max()==0 and pixels[-1].max()==0
        assert pixels[:,0].max()==0 and pixels[:,-1].max()==0
Image.fromarray(quiet).save(out/'silence.png')
Image.fromarray(localized).save(out/'single-mid-band.png')
Image.fromarray(render(constant)).save(out/'constant-spectrum.png')
# Same time constants as BandFollower calls in LiveDotRenderer; this is a
# host reference DSP driver, not a test of the Android tap or output latency.
def follow(state,target,dt,attack,release):
    k=np.where(target>state,1-np.exp(-dt/attack),1-np.exp(-dt/release))
    state += (target-state)*k
fast=constant.copy();trail=constant.copy()
for _ in range(180):
    follow(fast,zero,1/60,.018,.115);follow(trail,zero,1/60,.09,.24)
assert fast.max()<1e-9 and trail.max()<2e-6, 'pause/missing PCM must settle'
# Build a diagnostic clip: silence, bass pulses, mid sweeps, high transients,
# then silence. The sound and spectra are derived from these same samples.
rate=44100;seconds=10;fps=60;sample_time=np.arange(rate*seconds)/rate
pcm=np.zeros(len(sample_time),np.float32)
m=(sample_time>=1)&(sample_time<4)
t=sample_time[m]-1
pcm[m]=.6*np.maximum(0,np.sin(2*np.pi*2*t))**4*np.sin(2*np.pi*90*t)
m=(sample_time>=4)&(sample_time<7);t=sample_time[m]-4
pcm[m]=.24*np.sin(2*np.pi*(400*t+400*t*t))*(.5+.5*np.sin(2*np.pi*3*t))
m=(sample_time>=7)&(sample_time<9);t=sample_time[m]-7
pcm[m]=.25*np.maximum(0,np.sin(2*np.pi*5*t))**6*np.sin(2*np.pi*6500*t)
name='synthetic-audio-preview'
if args.audio:
    pcm=np.frombuffer(subprocess.check_output(['ffmpeg','-v','error','-i',args.audio,'-vn','-ac','1','-ar',str(rate),'-t','12','-f','f32le','-']),np.float32)
    seconds=len(pcm)/rate;name='reference-audio-preview'
audio_path=out/'preview-input.f32';audio_path.write_bytes(pcm.tobytes())
video=out/(name+'.mp4')
encoder=subprocess.Popen(['ffmpeg','-y','-v','error','-f','rawvideo','-pix_fmt','rgb24','-s','720x720','-r',str(fps),'-i','-',
    '-f','f32le','-ar',str(rate),'-ac','1','-i',str(audio_path),'-c:v','libx264','-preset','veryfast','-crf','19','-pix_fmt','yuv420p','-c:a','aac','-shortest','-movflags','+faststart',str(video)],stdin=subprocess.PIPE)
n=1024;window=np.hanning(n);energy=(window**2).sum()
freq=np.arange(1,n//2)*rate/n
bins=np.clip((np.log(np.maximum(40,freq)/40)/math.log(rate/2/40)*40).astype(int),0,39)
fast=zero.copy();trail=zero.copy();snapshots=[]
for frame in range(int(seconds*fps)):
    end=int((frame+1)*rate/fps);segment=pcm[max(0,end-n):end]
    block=np.zeros(n);block[:len(segment)]=segment
    spectrum=np.fft.rfft(block*window)
    power=np.bincount(bins,weights=np.abs(spectrum[1:n//2])**2,minlength=40)
    target=np.minimum(1,(2*power/(n*energy))**.25).astype(np.float32)
    follow(fast,target,1/fps,.018,.115);follow(trail,target,1/fps,.09,.24)
    pixels=render(fast,trail)
    encoder.stdin.write(pixels.tobytes())
    if frame in (90,180,330,450): snapshots.append(pixels)
encoder.stdin.close();assert encoder.wait()==0
audio_path.unlink()
Image.fromarray(np.concatenate(snapshots[:2],axis=1)).save(out/(name+'.jpg'))
report={'result':'PASS','scope':'Host Mesa ESSL only; not Android, not AV-sync or device performance',
    'checks':['ESSL100 compile/link','silence stable','constant signal stable','local mid-band response','bass expansion',
    'amplitude ordering','portrait and landscape at all three densities','no clipping at full input','pause decay reference'],
    'gl':get_string(0x1F02).decode(),'renderer':get_string(0x1F01).decode(),
    'shader_sha256':hashlib.sha256(source.encode()).hexdigest(),'localized_changed_pixels':int(changed.sum())}
(out/'probe-result.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
print(video)
