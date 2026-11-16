"""
The mod icon (32x32): a little pastel bakery shopfront. A wooden sign with a loaf, a pink-and-cream scalloped
awning, a glowing shop window with cake, a cupcake, bread and a croissant, a flower box, a door with a heart, and a
cobbled step, inside a rounded pink frame.
Run: python3 blockbench/gen_icon.py
"""
import math, os, random, sys
from PIL import Image
S=32
def hx(c): c=c.lstrip('#'); return tuple(int(c[i:i+2],16) for i in (0,2,4))+(255,)
im=Image.new('RGBA',(S,S),(0,0,0,0))
def put(x,y,c):
    if 0<=x<S and 0<=y<S: im.putpixel((x,y),hx(c) if isinstance(c,str) else c)
def get(x,y): return im.getpixel((x,y))
rnd=random.Random(11)
R=5
def inbadge(x,y,inset=0):
    r=R-inset
    dx=max(r+inset-x, x-(S-1-r-inset),0); dy=max(r+inset-y, y-(S-1-r-inset),0)
    return math.hypot(dx,dy)<=r+0.3 and inset<=x<=S-1-inset and inset<=y<=S-1-inset

# sky: soft pastel gradient with a few tiny sparkles
for y in range(S):
    for x in range(S):
        if inbadge(x,y):
            t=y/S
            put(x,y,(int(255-10*t),int(236+4*t),int(242-8*t),255))
for (x,y) in ((4,3),(27,4),(25,2)): put(x,y,'#ffffff')

# wall: warm cream-pink bricks with varied tones
for y in range(12,28):
    for x in range(2,30):
        row=(y-12)//3; off=(row%2)*3
        if (y-12)%3==2 or (x+off)%6==0: c='#efc0cc'
        else:
            c=['#fbd8e2','#f8d0dc','#fcdde6'][rnd.randrange(3)]
        put(x,y,c)
# side shading of the building edges
for y in range(12,28): put(2,y,'#f4c4d2'); put(29,y,'#e8b0c0')

# cornice + sign
for x in range(2,30): put(x,4,'#8a5038'); put(x,3,'#b0704c')
for y in range(1,5):
    for x in range(10,22):
        edge=x in(10,21) or y in(1,4)
        put(x,y,'#8a5038' if edge else '#fff4e2')
# tiny loaf on the sign, a heart either side
for (x,y,c) in ((14,2,'#f2c070'),(15,2,'#ffe0a0'),(16,2,'#f2c070'),(17,2,'#f2c070'),(13,3,'#c07830'),(14,3,'#d8903c'),(15,3,'#a86020'),
                (16,3,'#d8903c'),(17,3,'#a86020'),(18,3,'#c07830')):
    put(x,y,c)
put(12,2,'#f27aa4'); put(19,2,'#f27aa4'); put(12,3,'#d0507e'); put(19,3,'#d0507e')

# awning
for y in range(5,10):
    for x in range(1,31):
        stripe=((x-1)//4)%2==0
        if stripe: c=['#d04c80','#f06a98','#f27aa4','#f27aa4','#ee6896'][y-5]
        else: c=['#ead6d0','#fff6f2','#fffaf6','#fffaf6','#f8ece8'][y-5]
        put(x,y,c)
# scalloped edge, one round lobe per stripe
# shadow under the awning first, then the round lobes over it
for x in range(2,30):
    for y in (12,13):
        if get(x,y)[3]: put(x,y,'#ecb4c4' if y==12 else '#f4c8d4')
for i in range(8):
    stripe=i%2==0
    x0=1+i*4
    for (dx,dy) in ((0,10),(1,10),(2,10),(3,10),(0,11),(1,11),(2,11),(3,11),(1,12),(2,12)):
        if dy==10: c='#f06a98' if stripe else '#fff6f2'
        elif dy==11: c=('#e05a8a' if dx in (1,2) else '#c84e7c') if stripe else ('#f6eae6' if dx in (1,2) else '#dccac4')
        else: c='#b8466e' if stripe else '#d4c0ba'
        put(x0+dx,dy,c)

# window
x0,x1,y0,y1=4,18,14,24
for y in range(y0,y1+1):
    for x in range(x0,x1+1):
        if x in(x0,x1) or y in(y0,y1): put(x,y,'#c8844c' if (x==x0 or y==y0) else '#8a5434')
        else:
            t=(y-y0)/(y1-y0)
            put(x,y,(255,int(242-26*t),int(196-40*t),255))
# shelves
for x in range(x0+1,x1): put(x,19,'#c8925c'); put(x,23,'#c8925c')
# top shelf: cake (left) and cupcake (right)
for y in range(16,19):
    for x in range(6,10): put(x,y,'#f7a8c4' if y==16 else '#fff2e4' if y==17 else '#f0c896')
put(7,15,'#e8405a'); put(8,15,'#ffffff')
for (x,y,c) in ((13,16,'#ffc2d6'),(14,15,'#ffc2d6'),(15,16,'#ffc2d6'),(14,16,'#ffe0ea'),(13,17,'#c8925c'),(14,17,'#e0aa70'),(15,17,'#c8925c'),(13,18,'#c8925c'),(14,18,'#e0aa70'),(15,18,'#c8925c'),(14,14,'#e8405a')):
    put(x,y,c)
# bottom shelf: loaf and croissant
for y in range(20,23):
    for x in range(6,11):
        if y==20 and x in (6,10): continue
        put(x,y,'#f0b860' if y==20 else '#d8903c' if y==21 else '#b87030')
put(7,21,'#a86020'); put(9,21,'#a86020'); put(8,20,'#ffe0a0')
for (x,y,c) in ((12,22,'#c88038'),(13,21,'#e8a850'),(14,20,'#f2c070'),(15,21,'#e8a850'),(16,22,'#c88038'),(14,21,'#d08c40'),(13,22,'#b87030'),(15,22,'#b87030'),(14,22,'#d08c40')):
    put(x,y,c)
# glass shine
put(x0+2,y0+3,'#ffffff'); put(x0+3,y0+2,'#ffffff'); put(x1-2,y0+6,'#fffaf0')

# flower box
for x in range(4,19): put(x,25,'#a8683e'); put(x,26,'#7a4a2e')
for x in range(4,19,2):
    put(x,24,'#6ab04a')
for x in (5,9,13,17): put(x,24,'#f27aa4')
for x in (7,11,15): put(x,24,'#ffffff')

# door with a round window and a little heart sign
for y in range(14,28):
    for x in range(21,28):
        edge=x in(21,27) or y==14
        put(x,y,'#6a3e28' if edge else ('#a8683e' if (x+y)%5 else '#9a5c36'))
put(21,14,'#fbd8e2'); put(27,14,'#fbd8e2')
for (x,y) in ((23,16),(24,16),(25,16),(23,17),(24,17),(25,17),(24,18)):
    put(x,y,'#ffe8b8')
put(23,16,'#fff8e0')
for (x,y) in ((22,20),(23,20),(25,20),(26,20),(22,21),(23,21),(24,21),(25,21),(26,21),(23,22),(24,22),(25,22),(24,23)):
    put(x,y,'#f06a98')
put(24,20,'#a8683e')
put(26,24,'#f2c94c'); put(26,25,'#c89a30')

# step / cobbles
for x in range(2,30):
    put(x,28,'#d8c0b0' if (x//3)%2 else '#e8d4c4')
    put(x,29,'#b8a090')
for y in (28,29):
    for x in range(21,28): put(x,y,'#c8a888' if y==28 else '#a88a70')

# badge border
for y in range(S):
    for x in range(S):
        if inbadge(x,y) and not inbadge(x,y,1):
            put(x,y,'#c8507e')
        elif not inbadge(x,y):
            im.putpixel((x,y),(0,0,0,0))
out=sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/icon.png")
im.save(out)
print("icon written")
