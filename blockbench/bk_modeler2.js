(function () {
// Builds Java block models as real Blockbench projects and exports them.
const ART = window.ART;
const fs = ART.fs, path = ART.path, ROOT = ART.ROOT;
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];
const MB = window.MB = window.MB || {};
MB.models = [];

// Keep a UV span inside 0..16 even when the element crosses a block edge (otherwise the texture smears).
function wrap(a, b) {
	const len = Math.min(16, b - a);
	let start = ((a % 16) + 16) % 16;
	if (start + len > 16.0001) start = 16 - len;
	return [start, start + len];
}

// Two faces in the same plane flicker in game. Push the smaller face a hair inward so the bigger one wins.
const FACE_AXES = { north: [2, 0], south: [2, 1], west: [0, 0], east: [0, 1], down: [1, 0], up: [1, 1] };
function hasFace(c, face) {
	if (typeof c.tex === 'string') return true;
	if (face in c.tex) return !!c.tex[face];
	return c.tex.all != null;
}
function dezfight(cubes) {
	const EPS = 0.015;
	for (let a = 0; a < cubes.length; a++) for (let b = 0; b < cubes.length; b++) {
		if (a === b) continue;
		const A = cubes[a], B = cubes[b];
		if (A.rot || B.rot) continue;
		Object.entries(FACE_AXES).forEach(([face, [k, side]]) => {
			if (!hasFace(A, face) || !hasFace(B, face)) return;
			const pa = side ? A.to[k] : A.from[k], pb = side ? B.to[k] : B.from[k];
			if (Math.abs(pa - pb) > 0.001) return;
			const others = [0, 1, 2].filter(i => i !== k);
			if (!others.every(i => Math.min(A.to[i], B.to[i]) - Math.max(A.from[i], B.from[i]) > 0.01)) return;
			const area = c => others.reduce((s, i) => s * (c.to[i] - c.from[i]), 1);
			// Move the smaller one (ties: the later one) inward.
			if (area(B) > area(A) || (area(B) === area(A) && b < a)) return;
			const thick = B.to[k] - B.from[k];
			// Each element gets its own nudge so two nudged faces never land in the same plane again.
			const eps = EPS * (1 + (b % 9) * 0.37);
			if (thick <= eps * 2) return;
			if (side) B.to[k] -= eps; else B.from[k] += eps;
		});
	}
	return cubes;
}
// Vanilla-style UVs that follow block coordinates so textures line up across cubes.
function autoUv(face, f, t) {
	let u, v;
	switch (face) {
		case 'north': u = [16 - t[0], 16 - f[0]]; v = [16 - t[1], 16 - f[1]]; break;
		case 'south': u = [f[0], t[0]]; v = [16 - t[1], 16 - f[1]]; break;
		case 'west': u = [f[2], t[2]]; v = [16 - t[1], 16 - f[1]]; break;
		case 'east': u = [16 - t[2], 16 - f[2]]; v = [16 - t[1], 16 - f[1]]; break;
		case 'up': u = [f[0], t[0]]; v = [f[2], t[2]]; break;
		case 'down': u = [f[0], t[0]]; v = [16 - t[2], 16 - f[2]]; break;
	}
	u = wrap(u[0], u[1]); v = wrap(v[0], v[1]);
	return [u[0], v[0], u[1], v[1]];
}
// UVs for 64px textures on the 3x3x3 oven: one texel per model unit over the -16..32 range.
function bigUv(face, f, t) {
	let u, v;
	switch (face) {
		case 'north': u = [32 - t[0], 32 - f[0]]; v = [32 - t[1], 32 - f[1]]; break;
		case 'south': u = [f[0] + 16, t[0] + 16]; v = [32 - t[1], 32 - f[1]]; break;
		case 'west': u = [f[2] + 16, t[2] + 16]; v = [32 - t[1], 32 - f[1]]; break;
		case 'east': u = [32 - t[2], 32 - f[2]]; v = [32 - t[1], 32 - f[1]]; break;
		case 'up': u = [f[0] + 16, t[0] + 16]; v = [f[2] + 16, t[2] + 16]; break;
		case 'down': u = [f[0] + 16, t[0] + 16]; v = [32 - t[2], 32 - f[2]]; break;
	}
	return [u[0] / 4, v[0] / 4, u[1] / 4, v[1] / 4];
}

/*
 * textures: { key: 'block/file' | { file, big: true } }
 * cube: { name, from, to, tex: 'key' | { all, north, ... (null hides a face) }, tint: true | [faces],
 *         rot: [axis, angle, origin], rescale, uv: { face: [u1,v1,u2,v2] }, uvAll: [..], shade: false }
 */
function build(name, textures, cubes) {
	try { ModelProject.all.slice().forEach(p => { p.saved = true; p.close(true); }); } catch (e) { }
	Formats.java_block.new();
	Project.name = name;
	const texMap = {}, big = {};
	Object.entries(textures).forEach(([key, spec]) => {
		const file = typeof spec === 'string' ? spec : spec.file;
		if (typeof spec === 'object' && spec.big) big[key] = true;
		const tex = new Texture({ name: key }).fromPath(path.join(ROOT, 'textures', file + '.png'));
		tex.add(false);
		tex.id = key;
		texMap[key] = tex;
	});
	Undo.initEdit({ elements: [], outliner: true, textures: Object.values(texMap) });
	const made = [];
	cubes = dezfight(JSON.parse(JSON.stringify(cubes)));
	cubes.forEach((c, i) => {
		const faces = {};
		const texFor = face => {
			if (typeof c.tex === 'string') return c.tex;
			if (face in c.tex) return c.tex[face];
			return c.tex.all !== undefined ? c.tex.all : null;
		};
		FACES.forEach(face => {
			const key = texFor(face);
			if (!key) { faces[face] = { texture: null }; return; }
			if (!texMap[key]) throw new Error(name + ': missing texture key ' + key);
			const tinted = c.tint === true || (Array.isArray(c.tint) && c.tint.includes(face));
			const uv = (c.uv && c.uv[face]) || c.uvAll || (big[key] ? bigUv(face, c.from, c.to) : autoUv(face, c.from, c.to));
			faces[face] = { texture: texMap[key].uuid, uv, tint: tinted ? 0 : -1 };
		});
		const opts = { name: c.name || ('cube' + i), from: c.from, to: c.to, faces, autouv: 0 };
		if (c.rot) {
			const [axis, angle, origin] = c.rot;
			opts.rotation = [axis === 'x' ? angle : 0, axis === 'y' ? angle : 0, axis === 'z' ? angle : 0];
			opts.origin = origin;
		} else {
			opts.origin = [8, 8, 8];
		}
		if (c.rescale) opts.rescale = true;
		if (c.shade === false) opts.shade = false;
		made.push(new Cube(opts).init());
	});
	Undo.finishEdit('Build ' + name, { elements: made, outliner: true });
	Canvas.updateAll();
	return made.length;
}

function exportModel(name, extra) {
	const json = JSON.parse(Codecs.java_block.compile());
	delete json.credit;
	if (json.textures && !json.textures.particle) json.textures.particle = Object.values(json.textures)[0];
	const out = Object.assign({ credit: 'Made with Blockbench - Sweetheart Bakery' }, extra || {}, json);
	const file = path.join(ROOT, 'models', 'block', name + '.json');
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, JSON.stringify(out, null, '\t'));
	MB.models.push(name);
	return out;
}

function make(name, textures, cubes, extra) {
	build(name, textures, cubes);
	return exportModel(name, extra === undefined ? { parent: 'block/block' } : extra);
}

// Trim cubes for a sliced pie/cake: everything west of cutX is eaten; new west faces show the inside.
function slice(cubes, cutX, insideKey, mode) {
	const res = [];
	cubes.forEach(c => {
		if (c.to[0] <= cutX + 0.001) return;
		if (mode === 'drop' && (c.from[0] + c.to[0]) / 2 < cutX) return;
		const n = JSON.parse(JSON.stringify(c));
		if (n.from[0] < cutX) {
			if (n.rot) return;
			n.from[0] = cutX;
			if (insideKey && !n.noInside) {
				n.tex = typeof n.tex === 'string' ? { all: n.tex, west: insideKey } : Object.assign({}, n.tex, { west: insideKey });
				if (n.tint === true) n.tint = ['north', 'south', 'east', 'up', 'down'];
				else if (Array.isArray(n.tint)) n.tint = n.tint.filter(f => f !== 'west');
			}
		}
		res.push(n);
	});
	return res;
}

function offset(cubes, dx, dy, dz) {
	return cubes.map(c => {
		const n = JSON.parse(JSON.stringify(c));
		n.from = [c.from[0] + dx, c.from[1] + dy, c.from[2] + dz];
		n.to = [c.to[0] + dx, c.to[1] + dy, c.to[2] + dz];
		if (n.rot) n.rot[2] = [c.rot[2][0] + dx, c.rot[2][1] + dy, c.rot[2][2] + dz];
		return n;
	});
}

function writeJson(rel, obj) {
	const file = path.join(ROOT, rel);
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, JSON.stringify(obj, null, '\t'));
}

Object.assign(MB, { build, exportModel, make, slice, offset, autoUv, writeJson });
return 'modeler2 ready';
})()
