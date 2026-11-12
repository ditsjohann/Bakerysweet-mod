(function () {
const BK = window.BK;
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

function wrap(a, b) {
	if (a < 0) { a += 16; b += 16; }
	if (b > 16) { a -= 16; b -= 16; }
	return [a, b];
}
// Vanilla-style UVs that follow the block coordinates, so textures line up across cubes.
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

/*
 * cube spec: { name, from, to, tex: 'key' | {north:'key'|null,...,all:'key'}, tint: true|[faces], rot: [axis, angle, origin] }
 */
function build(name, textures, cubes) {
	try { ModelProject.all.slice().forEach(p => { p.saved = true; p.close(true); }); } catch (e) { }
	Formats.java_block.new();
	Project.name = name;
	const texMap = {};
	Object.entries(textures).forEach(([key, file]) => {
		const tex = new Texture({ name: key }).fromPath(BK.path.join(BK.ROOT, 'textures', file + '.png'));
		tex.add(false);
		tex.id = key;
		texMap[key] = tex;
	});
	Undo.initEdit({ elements: [], outliner: true, textures: Object.values(texMap) });
	const made = [];
	cubes.forEach((c, i) => {
		const faces = {};
		const texFor = face => {
			if (typeof c.tex === 'string') return c.tex;
			if (face in c.tex) return c.tex[face];
			return c.tex.all !== undefined ? c.tex.all : null;
		};
		FACES.forEach(face => {
			const key = texFor(face);
			const tinted = c.tint === true || (Array.isArray(c.tint) && c.tint.includes(face));
			faces[face] = key ? { texture: texMap[key].uuid, uv: (c.uv && c.uv[face]) || autoUv(face, c.from, c.to), tint: tinted ? 0 : -1 } : { texture: null };
		});
		const opts = { name: c.name || ('cube' + i), from: c.from, to: c.to, faces, autouv: 0 };
		if (c.rot) {
			const [axis, angle, origin] = c.rot;
			opts.rotation = [axis === 'x' ? angle : 0, axis === 'y' ? angle : 0, axis === 'z' ? angle : 0];
			opts.origin = origin;
		} else {
			opts.origin = [8, 8, 8];
		}
		const cube = new Cube(opts).init();
		made.push(cube);
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
	const file = BK.path.join(BK.ROOT, 'models', 'block', name + '.json');
	BK.fs.mkdirSync(BK.path.dirname(file), { recursive: true });
	BK.fs.writeFileSync(file, JSON.stringify(out, null, '\t'));
	BK.models = BK.models || [];
	BK.models.push(name);
	return out;
}

// Trim cubes for a sliced pie/cake: everything west of cutX is eaten; new west faces show the inside.
function slice(cubes, cutX, insideKey, mode) {
	const res = [];
	cubes.forEach(c => {
		if (c.to[0] <= cutX + 0.001) return;
		if (mode === 'drop' && (c.from[0] + c.to[0]) / 2 < cutX && c.to[1] - c.from[1] > 0.1) return;
		const n = JSON.parse(JSON.stringify(c));
		if (n.from[0] < cutX) {
			n.from[0] = cutX;
			if (insideKey && typeof n.tex === 'object' && !n.noInside) n.tex = Object.assign({}, n.tex, { west: insideKey });
			else if (insideKey && typeof n.tex === 'string' && !n.noInside) n.tex = { all: n.tex, west: insideKey };
			if (insideKey && !n.noInside) {
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

BK.build = build;
BK.exportModel = exportModel;
BK.slice = slice;
BK.offset = offset;
BK.autoUv = autoUv;
return 'modeler ready';
})()
