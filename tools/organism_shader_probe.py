"""Compile/link and render the checked-in ESSL with Mesa surfaceless EGL.
Requires Python, numpy, Pillow and libEGL; no Android/emulator/device claim.
Run from repository root: python tools/organism_shader_probe.py
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
    display, ints(0x3033,1,0x3040,0x40,0x3024,8,0x3023,8,0x3022,8,0x3038), C.byref(config), 1, C.byref(count))
assert count.value > 0
surface = egl('eglCreatePbufferSurface', P, [P,P,C.POINTER(I)])(display,config,ints(0x3057,960,0x3056,960,0x3038))
context = egl('eglCreateContext', P, [P,P,P,C.POINTER(I)])(display,config,None,ints(0x3098,3,0x3038))
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

root=Path('app/src/main/java/com/rikodev/music/android/dna/render')
def sources(file):
    return dict(re.findall(r'const val (\w+)(?:: String)?\s*=\s*"""(.*?)"""', (root/file).read_text(), re.S))
old=sources('MdnaShaders.kt');genesis=sources('OrganismShaders.kt')
pairs=[('SCENE_VERT','SCENE_FRAG'),('HALO_VERT','SPRITE_SOFT_FRAG'),('TERRAIN_VERT','SPRITE_SOFT_FRAG'),
       ('LENS_VERT','SPRITE_SOFT_FRAG'),('POST_VERT','BRIGHT_FRAG'),('POST_VERT','BLUR_FRAG'),('POST_VERT','COMPOSITE_FRAG')]
for v,f in pairs: delete_program(program(old[v],old[f]))
p=program(genesis['VERT'],genesis['FRAG'])
use=gl('glUseProgram',None,U);use(p)
uniform=gl('glGetUniformLocation',I,U,C.c_char_p)
u1=gl('glUniform1f',None,I,F);u3=gl('glUniform3f',None,I,F,F,F)
ufv=gl('glUniform1fv',None,I,I,P);um=gl('glUniformMatrix4fv',None,I,I,U,P)
u=lambda name: uniform(p,name.encode())
u4=gl('glUniform4f',None,I,F,F,F,F)
family=0
variant=0

gen_vao=gl('glGenVertexArrays',None,I,C.POINTER(U));vao=U();gen_vao(1,C.byref(vao))
gl('glBindVertexArray',None,U)(vao)
viewport=gl('glViewport',None,I,I,I,I);clear=gl('glClear',None,U)
gl('glClearColor',None,F,F,F,F)(0,0,0,1)
gl('glEnable',None,U)(0x0BE2);gl('glBlendFunc',None,U,U)(1,1)
draw_arrays=gl('glDrawArrays',None,U,I,I);read=gl('glReadPixels',None,I,I,I,I,U,U,P)
get_error=gl('glGetError',U)

def render(t=3.0,bass=.35,mid=.32,treble=.18,pulse=.15,w=960,h=960,tier=1.0):
    viewport(0,0,w,h);clear(0x4000)
    u1(u('uBurst'),.6);u1(u('uFamily'),family);u1(u('uSeed'),.23+variant*.37)
    u4(u('uDna'),.3,.7,.4,.3);u4(u('uTraits'),6,1,.65,.52+variant*.15)
    chroma=np.array([1/12]*12,dtype=np.float32);ufv(u('uChroma'),12,chroma.ctypes.data)
    aspect=w/h; tan=math.tan(math.radians(21)); z=1.35/(tan*min(1,aspect)*.92)
    near,far=.1,40
    proj=np.array([[1/(aspect*tan),0,0,0],[0,1/tan,0,0],[0,0,-(far+near)/(far-near),-2*far*near/(far-near)],[0,0,-1,0]])
    view=np.eye(4);view[2,3]=-z
    yaw=t*.08;pitch=math.radians(35 if family==1 else 14);c,s=math.cos(yaw),math.sin(yaw);d,e=math.cos(pitch),math.sin(pitch)
    rot=np.array([[c,0,s,0],[0,1,0,0],[-s,0,c,0],[0,0,0,1]])@np.array([[1,0,0,0],[0,d,-e,0],[0,e,d,0],[0,0,0,1]])
    mvp=np.ascontiguousarray((proj@view@rot).T,dtype=np.float32)
    um(u('uMvp'),1,0,mvp.ctypes.data);u3(u('uCamDir'),*rot[2,:3])
    count = int(min(32736,18432*tier))//96*96
    for k,v in dict(uTime=t,uBass=bass,uMid=mid,uTreble=treble,uPulse=pulse,uPointsPerLayer=count,uPointScale=2.5,uGain=min(1.3,math.sqrt(1/tier))).items(): u1(u(k),v)
    bands=np.array([bass]*11+[mid]*18+[treble]*11,dtype=np.float32);ufv(u('uBands'),40,bands.ctypes.data)
    u1(u('uLine'),1)
    if family in (0,1):
        for layer in range(2): draw_arrays(3,layer*count,count)
    u1(u('uLine'),0)
    draw_arrays(0,0,count*2);assert get_error()==0
    pixels=np.empty((h,w,4),np.uint8);read(0,0,w,h,0x1908,0x1401,pixels.ctypes.data)
    image=pixels[::-1,:,:3].copy()
    ys,xs=np.where(image.astype(np.int32).sum(axis=2)>12)
    return image,dict(hash=hashlib.sha256(image.tobytes()).hexdigest(),lit=len(xs),bounds=[int(xs.min()),int(ys.min()),int(xs.max()),int(ys.max())])

out=Path('docs/mdna/genesis/evidence');out.mkdir(parents=True,exist_ok=True)
records=[]
for family in range(3):
 for variant in range(2):
  image, base=render(w=640,h=640)
  assert base['lit']>1000
  assert base['hash']==render(w=640,h=640)[1]['hash']
  for band in ['bass','mid','treble']:
   assert base['hash']!=render(w=640,h=640,**{band:.9})[1]['hash'], band
  Image.fromarray(image).save(out/f'p4-family-{family}-variant-{variant}.png')
  records.append(dict(family=family,variant=variant,**base))
assert len({r['hash'] for r in records})==6
compatibility=[]
for family in range(3):
 for w,h in [(375,812),(812,375)]:
  for tier in [.3125,1.,2.5]:
   _, case=render(w=w,h=h,tier=tier,bass=1,mid=1,treble=1,pulse=1)
   left,top,right,bottom=case['bounds']
   assert case['lit']>100 and left>0 and top>0 and right<w-1 and bottom<h-1
   compatibility.append(dict(family=family,width=w,height=h,tier=tier,**case))
(out/'p4-shader-probe.json').write_text(json.dumps(dict(renderer=get_string(0x1F01).decode(),cases=records,compatibility=compatibility,scope='Actual ESSL on Mesa; not Android UI/device performance'),indent=2))
print('PASS: three archetypes, two variants each; deterministic ESSL, independent band responses; 18 aspect/quality stress cases')
if '--animation' in sys.argv:
 family=0;variant=0
 frames=[]
 for frame in range(72):
  t=frame/24.0
  beat=math.exp(-((t*2)%1)*7)
  pixels,_=render(t=t,bass=.18+.65*beat,mid=.3+.2*math.sin(t*3),
                  treble=.15+.45*max(0,math.sin(t*13)),pulse=beat,w=480,h=480)
  frames.append(Image.fromarray(pixels))
 frames[0].save(out/'p4-orb-synthetic-preview.gif',save_all=True,append_images=frames[1:],
                duration=42,loop=0,optimize=False)
 print('Saved synthetic-input ESSL animation; not Android playback capture')
delete_program(p)
egl('eglMakeCurrent',U,[P,P,P,P])(display,None,None,None)
egl('eglDestroyContext',U,[P,P])(display,context)
egl('eglDestroySurface',U,[P,P])(display,surface)
egl('eglTerminate',U,[P])(display)
