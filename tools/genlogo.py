"""App icon options for Lonewolf Shark, made through OpenRouter with the real Shark 6 as reference.
Usage: python tools/genlogo.py   (writes art/logo/option_*.png)
"""
import base64, json, os, sys, urllib.request
from concurrent.futures import ThreadPoolExecutor

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(ROOT, 'research', 'shark-reference')
OUT = os.path.join(ROOT, 'art', 'logo')
ENV = 'C:/Users/Hodgs/Lonewolf-ai-solutions/.env.local'

BASE = ('App icon for a car companion app. Square, a single bold image that still reads at 96 pixels. '
        'The vehicle is the BYD Shark 6 dual cab ute exactly as in the reference photos: the wide grille with the BYD lettering, '
        'the C shaped LED headlights and the full width light bar. No text, no letters other than the BYD badge on the grille, '
        'no border, no rounded corners drawn in, no watermark. ')
OPTIONS = {
    'a': BASE + 'Straight on front view of the ute, low and wide, headlights and light bar glowing electric cyan, '
               'on a deep black to navy background with a subtle cyan glow behind it. Clean, modern, premium.',
    'b': BASE + 'Front three quarter view of the ute at night, headlights on, the silhouette picked out by a thin electric cyan rim light, '
               'deep black background. Dramatic and minimal.',
    'c': BASE + 'Simplified graphic emblem: the front of the ute drawn as bold flat shapes, grille and light bar in electric cyan, '
               'inside a dark navy circle, with a subtle shark fin shape rising behind the roof line. Flat vector style.',
    'd': BASE + 'Straight on front view of the ute, headlights and light bar glowing warm white, grille lit from below in red like the '
               'Shark cabin trim, black background. Bold and aggressive.',
}


def key():
    for line in open(ENV, encoding='utf-8'):
        if line.startswith('OPENROUTER_API_KEY'):
            return line.split('=', 1)[1].strip().strip('"')
    raise SystemExit('no OPENROUTER_API_KEY')


def ref(name):
    return {'type': 'image_url', 'image_url': {'url': 'data:image/jpeg;base64,' + base64.b64encode(open(os.path.join(REF, name), 'rb').read()).decode()}}


def run(k):
    body = {'model': 'openai/gpt-image-2', 'prompt': OPTIONS[k], 'size': '1024x1024', 'quality': 'high', 'output_format': 'png',
            'input_references': [ref('2025_BYD_Shark_6_front.jpg'), ref('2026_BYD_Shark_6_Dynamic_-_Melbourne_Motor_Show_-_carsales.jpg')]}
    req = urllib.request.Request('https://openrouter.ai/api/v1/images', data=json.dumps(body).encode(),
                                 headers={'Authorization': 'Bearer ' + key(), 'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=600) as r:
            data = json.load(r)
        os.makedirs(OUT, exist_ok=True)
        open(os.path.join(OUT, 'option_%s.png' % k), 'wb').write(base64.b64decode(data['data'][0]['b64_json']))
        print(k, 'ok', flush=True)
    except urllib.error.HTTPError as e:
        print(k, 'FAILED', e.read().decode()[:300], flush=True)


if __name__ == '__main__':
    with ThreadPoolExecutor(4) as ex:
        list(ex.map(run, sys.argv[1:] or list(OPTIONS)))
