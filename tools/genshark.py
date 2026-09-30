"""Generate the app's artwork with OpenAI image generation.

Usage: python tools/genshark.py <job> [<job> ...]   (or "all", or "colours")
Base scenes are made in white from the real reference photos, then each other colour is an
edit of the white image so the scene stays identical and only the paint changes.
The key is read from the agency .env.local and never printed.
"""
import base64, io, json, os, sys, urllib.request, uuid
from concurrent.futures import ThreadPoolExecutor
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(ROOT, 'research', 'shark-reference')
OUT = os.path.join(ROOT, 'art', 'source')
MODEL = os.environ.get('SHARK_IMAGE_MODEL', 'gpt-image-2')
# OpenRouter is the second door when the OpenAI account is empty. Same prompts, references sent inline.
BACKEND = os.environ.get('SHARK_IMAGE_BACKEND', 'openai')
OR_MODEL = os.environ.get('SHARK_OR_MODEL', 'openai/gpt-image-2')
BASE = 'https://openrouter.ai/api/v1' if BACKEND == 'openrouter' else 'https://api.openai.com/v1'


def apikey():
    return key('OPENROUTER_API_KEY' if BACKEND == 'openrouter' else 'OPENAI_API_KEY')
ENV = 'C:/Users/Hodgs/Lonewolf-ai-solutions/.env.local'

FRONT = '2025_BYD_Shark_6_front.jpg'
REAR = '2025_BYD_Shark_6_rear.jpg'
SIDE = '2026_BYD_Shark_6_Dynamic_-_Melbourne_Motor_Show_-_carsales.jpg'

UTE = ('The vehicle is the BYD Shark 6 dual cab ute exactly as in the reference photos: same grille with the '
       'BYD lettering, same C shaped LED headlights and full width light bar, same bonnet, mirrors, wheels, '
       'sports bar and tub, same full width rear light bar. Keep every body detail accurate. Standard road tyres, '
       'no lift kit, no bull bar, no number plate text. Paint colour: gloss white. ')
LOOK = ('Photograph, natural available light at dusk just after sunset, dark moody sky with a band of warm light low on the horizon, '
        'the surroundings dark so the image works behind white text, but enough soft light on the ute that its paint colour reads clearly. Muted colours, no lens flare, no text, no logos other than the BYD badge, no people. '
        'Wide 16:9 frame.')

JOBS = {
    'home': ([FRONT, SIDE], UTE + 'Seen straight on from the front, centred in the frame, headlights and light bar on, '
             'parked on hard wet black sand on a wild New Zealand west coast beach, dark headland and low surf far behind. '
             'The ute fills the middle third of the frame, the left and right thirds are dark empty beach and sky. ' + LOOK),
    'gauges': ([REAR, SIDE], UTE + 'Seen straight on from behind, centred, tail light bar glowing red, parked on a gravel '
               'high country road in the South Island of New Zealand, tussock either side, dark ranges ahead under the last light. '
               'The ute fills the middle third of the frame, the left and right thirds are dark and uncluttered. ' + LOOK),
    'offroad': ([FRONT, SIDE], UTE + 'Front three quarter view, small in the frame, centred and sitting in the lower half, headlights on, '
                'stopped on the shingle bed of a braided New Zealand river with shallow water around the tyres, beech forest and steep dark hills behind. '
                'The ute takes up only the middle fifth of the frame width. Everything around it is open landscape with no bright detail. ' + LOOK),
    'towing': ([SIDE, FRONT, REAR], UTE + 'Side on view facing left, hitched to a modern dark grey twin axle off road caravan '
               'with no writing on it, parked on a gravel lakefront at Lake Pukaki style New Zealand scenery, mountains across the water. '
               'Ute and caravan together span the middle half of the frame width and sit in the lower half of the frame, seen from a little above, '
               'with open sky and mountains above and plain dark gravel in the bottom quarter. ' + LOOK),
    'fuel': ([REAR, SIDE], UTE + 'Rear three quarter view on the left half of the frame, tail lights on, parked beside a single '
             'unbranded fuel pump at a small rural New Zealand forecourt at night, wet concrete, one overhead light, dark paddocks behind. '
             'The right half of the frame is dark and empty. ' + LOOK),
    'pet': ([REAR, SIDE], UTE + 'Rear three quarter view on the left half of the frame, parked in a quiet car park beside a New Zealand '
            'beach reserve with pohutukawa trees, a border collie sitting calmly on the ground beside the rear wheel. '
            'The right half of the frame is dark and empty. ' + LOOK),
    'side': ([SIDE, FRONT], UTE + 'Exact side profile facing left, perfectly level, whole vehicle visible with clear space all '
             'round, evenly lit studio product photograph on a plain solid pure black background, no floor, no shadow, no reflection, no text.'),
    'seat': ([FRONT], 'Product photograph of a single modern black leather car front seat with a headrest and light stitching, seen straight on from the front, '
             'whole seat visible with clear space all round, evenly lit studio photograph on a plain solid pure black background, no floor, no shadow, no text, no logos.'),
    'front': ([FRONT], UTE + 'Exact straight on front view, perfectly level and symmetrical, whole vehicle visible with clear space '
              'all round, headlights on, evenly lit studio product photograph on a plain solid pure black background, no floor, no shadow, '
              'no reflection, no text.'),
}
SCENES = {
    'climate': ('Photograph looking across a still New Zealand alpine lake at deep dusk, dark ranges with a little snow, thin band of '
                'warm light on the horizon, dark sky. No vehicles, no people, no text. Dark overall so it works behind white text. Wide 16:9 frame.'),
    'cameras': ('Photograph of an empty New Zealand country road at deep dusk running through dark farmland toward hills, wet seal, '
                'very dark overall, no vehicles, no people, no text. Wide 16:9 frame.'),
}
COLOURS = {
    'black': 'gloss black',
    'grey': 'dark metallic grey',
    'blue': 'deep metallic navy blue',
    'orange': 'burnt metallic orange',
}


def key(name='OPENAI_API_KEY'):
    for line in open(ENV, encoding='utf-8'):
        if line.startswith(name):
            return line.split('=', 1)[1].strip().strip('"').strip()
    raise SystemExit('no key ' + name)


def openrouter(name, prompt, files, size):
    """OpenRouter's unified image API. References go in as data URLs, same prompt as the OpenAI door."""
    refs = []
    for f in files:
        mime = 'image/png' if f.endswith('.png') else 'image/jpeg'
        refs.append({'type': 'image_url', 'image_url': {'url': 'data:%s;base64,%s' % (mime, base64.b64encode(open(f, 'rb').read()).decode())}})
    body = {'model': OR_MODEL, 'prompt': prompt, 'size': size, 'quality': 'high', 'output_format': 'png'}
    if refs:
        body['input_references'] = refs
    resp = post('https://openrouter.ai/api/v1/images', json.dumps(body).encode(), {'Authorization': 'Bearer ' + key('OPENROUTER_API_KEY'), 'Content-Type': 'application/json'})
    save(resp, name)


def post(url, body, headers):
    req = urllib.request.Request(url, data=body, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=600) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        raise RuntimeError(e.read().decode()[:600])


def multipart(fields, files):
    b = uuid.uuid4().hex
    buf = io.BytesIO()
    for k, v in fields.items():
        buf.write(('--%s\r\nContent-Disposition: form-data; name="%s"\r\n\r\n%s\r\n' % (b, k, v)).encode())
    for path in files:
        mime = 'image/png' if path.endswith('.png') else 'image/jpeg'
        buf.write(('--%s\r\nContent-Disposition: form-data; name="image[]"; filename="%s"\r\nContent-Type: %s\r\n\r\n'
                   % (b, os.path.basename(path), mime)).encode())
        buf.write(open(path, 'rb').read()); buf.write(b'\r\n')
    buf.write(('--%s--\r\n' % b).encode())
    return buf.getvalue(), 'multipart/form-data; boundary=' + b


def save(resp, name):
    os.makedirs(OUT, exist_ok=True)
    data = base64.b64decode(resp['data'][0]['b64_json'])
    path = os.path.join(OUT, name + '.png')
    open(path, 'wb').write(data)
    print(name, Image.open(path).size, resp.get('usage', {}).get('total_tokens'), flush=True)


def edit(name, prompt, files, size='1536x1024'):
    if os.path.exists(os.path.join(OUT, name + '.png')) and '--force' not in sys.argv:
        print(name, 'exists'); return
    if BACKEND == 'openrouter':
        return openrouter(name, prompt, files, size)
    body, ctype = multipart({'model': MODEL, 'prompt': prompt, 'size': size, 'quality': 'high'}, files)
    save(post('https://api.openai.com/v1/images/edits', body, {'Authorization': 'Bearer ' + key(), 'Content-Type': ctype}), name)


def text(name, prompt, size='1536x1024'):
    if os.path.exists(os.path.join(OUT, name + '.png')) and '--force' not in sys.argv:
        print(name, 'exists'); return
    if BACKEND == 'openrouter':
        return openrouter(name, prompt, [], size)
    body = json.dumps({'model': MODEL, 'prompt': prompt, 'size': size, 'quality': 'high'}).encode()
    save(post('https://api.openai.com/v1/images/generations', body, {'Authorization': 'Bearer ' + key(), 'Content-Type': 'application/json'}), name)


def run(job):
    try:
        if job in JOBS:
            refs, prompt = JOBS[job]
            edit(job + '_white', prompt, [os.path.join(REF, r) for r in refs])
        elif job in SCENES:
            text(job, SCENES[job])
        elif '_' in job:
            base, colour = job.rsplit('_', 1)
            edit(job, 'Change only the paint colour of the ute body to %s. Keep the vehicle shape, badges, lights, wheels, '
                      'black plastic trim, the scene, the framing and the lighting exactly the same. Change nothing else.' % COLOURS[colour],
                 [os.path.join(OUT, base + '_white.png')])
    except Exception as e:
        print(job, 'FAILED', str(e)[:400], flush=True)


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if args == ['all']:
        args = list(JOBS) + list(SCENES)
    elif args == ['colours']:
        args = ['%s_%s' % (j, c) for j in JOBS if j != 'seat' for c in COLOURS]
    with ThreadPoolExecutor(4) as ex:
        list(ex.map(run, args))
