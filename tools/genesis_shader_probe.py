"""Compile/link and render the checked-in ESSL with Mesa surfaceless EGL.
Requires Python, numpy, Pillow and libEGL; no Android/emulator/device claim.
Run from repository root: python tools/genesis_shader_probe.py
"""
import ctypes as C
import hashlib
import json
import math
from pathlib import Path
import re
import sys
import subprocess
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
old=sources('MdnaShaders.kt');genesis=sources('GenesisShaders.kt')
pairs=[('SCENE_VERT','SCENE_FRAG'),('HALO_VERT','SPRITE_SOFT_FRAG'),('TERRAIN_VERT','SPRITE_SOFT_FRAG'),
       ('LENS_VERT','SPRITE_SOFT_FRAG'),('POST_VERT','BRIGHT_FRAG'),('POST_VERT','BLUR_FRAG'),('POST_VERT','COMPOSITE_FRAG')]
for v,f in pairs: delete_program(program(old[v],old[f]))
p=program(genesis['VERT'],genesis['FRAG'])
use=gl('glUseProgram',None,U);use(p)
uniform=gl('glGetUniformLocation',I,U,C.c_char_p)
u1=gl('glUniform1f',None,I,F);u3=gl('glUniform3f',None,I,F,F,F)
ufv=gl('glUniform1fv',None,I,I,P);um=gl('glUniformMatrix4fv',None,I,I,U,P)
u=lambda name: uniform(p,name.encode())
gen_vao=gl('glGenVertexArrays',None,I,C.POINTER(U));vao=U();gen_vao(1,C.byref(vao))
gl('glBindVertexArray',None,U)(vao)
viewport=gl('glViewport',None,I,I,I,I);clear=gl('glClear',None,U)
gl('glClearColor',None,F,F,F,F)(0,0,0,1)
gl('glEnable',None,U)(0x0BE2);gl('glBlendFunc',None,U,U)(1,1)
draw_arrays=gl('glDrawArrays',None,U,I,I);read=gl('glReadPixels',None,I,I,I,I,U,U,P)
get_error=gl('glGetError',U)

def render(t=3.0,bass=.35,mid=.32,treble=.18,pulse=.15,w=960,h=960,tier=1.0):
    viewport(0,0,w,h);clear(0x4000)
    aspect=w/h; tan=math.tan(math.radians(21)); z=1.08/(tan*min(1,aspect)*.92)
    near,far=.1,40
    proj=np.array([[1/(aspect*tan),0,0,0],[0,1/tan,0,0],[0,0,-(far+near)/(far-near),-2*far*near/(far-near)],[0,0,-1,0]])
    view=np.eye(4);view[2,3]=-z
    yaw=t*.08;pitch=math.radians(14);c,s=math.cos(yaw),math.sin(yaw);d,e=math.cos(pitch),math.sin(pitch)
    rot=np.array([[c,0,s,0],[0,1,0,0],[-s,0,c,0],[0,0,0,1]])@np.array([[1,0,0,0],[0,d,-e,0],[0,e,d,0],[0,0,0,1]])
    mvp=np.ascontiguousarray((proj@view@rot).T,dtype=np.float32)
    um(u('uMvp'),1,0,mvp.ctypes.data);u3(u('uCamDir'),*rot[2,:3])
    for k,v in dict(uTime=t,uBass=bass,uMid=mid,uTreble=treble,uPulse=pulse,uPointsPerLayer=18432*tier,uPointScale=2.5,uGain=min(1.3,math.sqrt(1/tier))).items(): u1(u(k),v)
    bands=np.array([bass]*11+[mid]*18+[treble]*11,dtype=np.float32);ufv(u('uBands'),40,bands.ctypes.data)
    u1(u('uLine'),1)
    for layer in range(2): draw_arrays(3,int(layer*18432*tier),int(18432*tier))
    u1(u('uLine'),0)
    draw_arrays(0,0,int(18432*tier*2));assert get_error()==0
    pixels=np.empty((h,w,4),np.uint8);read(0,0,w,h,0x1908,0x1401,pixels.ctypes.data)
    image=pixels[::-1,:,:3].copy()
    ys,xs=np.where(image.astype(np.int32).sum(axis=2)>12)
    return image,dict(hash=hashlib.sha256(image.tobytes()).hexdigest(),lit=len(xs),bounds=[int(xs.min()),int(ys.min()),int(xs.max()),int(ys.max())])

_,base=render(bass=0,mid=0,treble=0,pulse=0)
_,bass=render(bass=.9,mid=0,treble=0,pulse=0)
_,mid=render(bass=0,mid=.9,treble=0,pulse=0)
_,treble=render(bass=0,mid=0,treble=.9,pulse=0)
assert len({base['hash'],bass['hash'],mid['hash'],treble['hash']})==4
assert base['hash']==render(bass=0,mid=0,treble=0,pulse=0)[1]['hash']
frames=[]
for w,h in [(480,800),(960,540),(960,960)]:
    for q in [.3125,1,2.5]:
        _,f=render(bass=1,mid=1,treble=1,pulse=1,w=w,h=h,tier=q)
        x,y,xx,yy=f['bounds'];assert f['lit']>500 and x>0 and y>0 and xx<w-1 and yy<h-1
        frames.append(dict(w=w,h=h,quality=q,**f))
report=dict(shader_pairs_linked=8,independent_band_response=True,fixed_inputs_reproducible=True,
            frames=frames,renderer=get_string(0x1F01).decode(),gl_version=get_string(0x1F02).decode(),
            scope='Actual checked-in ESSL via Mesa surfaceless EGL; no Android UI, AGSL, bloom or physical-device timing validation.')
out=Path('docs/mdna/genesis/evidence');out.mkdir(parents=True,exist_ok=True)
Image.fromarray(render()[0]).save(out/'genesis-orb-shader.png')
(out/'shader-probe.json').write_text(json.dumps(report,indent=2)+'\n')
if '--video' in sys.argv:
    # Inspection clip only; this is not the Android P7 export feature.
    encoder=subprocess.Popen(['ffmpeg','-y','-loglevel','error','-f','rawvideo','-pix_fmt','rgb24',
        '-s','640x640','-r','30','-i','-','-an','-c:v','libx264','-crf','18','-pix_fmt','yuv420p',
        '-movflags','+faststart',str(out/'genesis-orb-demo.mp4')],stdin=subprocess.PIPE)
    for frame in range(120):
        t=frame/30
        kick=math.exp(-(t*2%1)*7)
        hat=math.exp(-((t*2+.5)%1)*14)
        image,_=render(t=t,bass=.12+.65*kick,mid=.26+.16*math.sin(t*2),treble=.08+.40*hat,pulse=.5*kick,w=640,h=640)
        encoder.stdin.write(image.tobytes())
    encoder.stdin.close()
    assert encoder.wait()==0
    report['inspection_clip']='4 seconds, 30 fixed steps/s, silent synthetic modulation. Not a measured device frame rate.'
    (out/'shader-probe.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
delete_program(p)
egl('eglMakeCurrent',U,[P,P,P,P])(display,None,None,None)
egl('eglDestroyContext',U,[P,P])(display,context)
egl('eglDestroySurface',U,[P,P])(display,surface)
egl('eglTerminate',U,[P])(display)
