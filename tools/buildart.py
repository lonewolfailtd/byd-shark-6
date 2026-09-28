"""Turn art/source PNGs into the app's drawables.

Backgrounds are cropped to 16:9 and sized 1920x1080. The two dial pictures have their black
studio background made see through and are trimmed to the ute.
"""
import glob, os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'art', 'source')
RES = os.path.join(ROOT, 'app', 'app', 'src', 'main', 'res', 'drawable-nodpi')
os.makedirs(RES, exist_ok=True)


def background(path, name):
    im = Image.open(path).convert('RGB')
    w, h = im.size
    th = int(w * 9 / 16)
    top = int((h - th) * 0.55)
    im = im.crop((0, top, w, top + th)).resize((1920, 1080), Image.LANCZOS)
    im.save(os.path.join(RES, name + '.webp'), 'WEBP', quality=86, method=6)


def cutout(path, name):
    im = Image.open(path).convert('RGB')
    w, h = im.size
    mask = Image.new('L', (w + 2, h + 2), 0)
    work = im.copy()
    key = (255, 0, 255)
    # Flood the studio black from every edge; what is left is the ute.
    for x in range(0, w, 8):
        for y in (0, h - 1):
            if sum(work.getpixel((x, y))) < 40: ImageDraw.floodfill(work, (x, y), key, thresh=26)
    for y in range(0, h, 8):
        for x in (0, w - 1):
            if sum(work.getpixel((x, y))) < 40: ImageDraw.floodfill(work, (x, y), key, thresh=26)
    alpha = Image.new('L', (w, h), 255)
    px = work.load(); ap = alpha.load()
    for y in range(h):
        for x in range(w):
            if px[x, y] == key: ap[x, y] = 0
    alpha = alpha.filter(ImageFilter.GaussianBlur(1.2))
    out = im.convert('RGBA'); out.putalpha(alpha)
    box = alpha.point(lambda v: 255 if v > 40 else 0).getbbox()
    out = out.crop(box)
    out.thumbnail((900, 900), Image.LANCZOS)
    out.save(os.path.join(RES, name + '.webp'), 'WEBP', quality=90, method=6)
    return out.size


for f in sorted(glob.glob(os.path.join(SRC, '*.png'))):
    base = os.path.splitext(os.path.basename(f))[0]
    if base.startswith('side_') or base.startswith('front_'):
        print('dial_' + base, cutout(f, 'dial_' + base))
    else:
        background(f, 'bg_' + base)
        print('bg_' + base)
