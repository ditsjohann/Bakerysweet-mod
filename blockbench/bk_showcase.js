(function () {
const BK = window.BK;
const names = BK.showcaseNames;
try { ModelProject.all.slice().forEach(p => { p.saved = true; p.close(true); }); } catch (e) { }
Formats.free.new();
Project.name = 'bakery_showcase';
const texCache = {};
function tex(ref) {
	const file = ref.replace('bakery:', '');
	if (!texCache[file]) {
		const t = new Texture({ name: file }).fromPath(BK.path.join(BK.ROOT, 'textures', file + '.png'));
		t.add(false);
		texCache[file] = t;
	}
	return texCache[file];
}
const made = [];
Undo.initEdit({ elements: [], outliner: true, textures: [] });
names.forEach((entry, i) => {
	const [name, layer] = Array.isArray(entry) ? entry : [entry, null];
	const json = JSON.parse(BK.fs.readFileSync(BK.path.join(BK.ROOT, 'models', 'block', name + '.json'), 'utf8'));
	const col = i % BK.showcaseCols, row = Math.floor(i / BK.showcaseCols);
	const ox = col * 20 - 40, oz = row * 20 - 20;
	json.elements.forEach(el => {
		const faces = {};
		Object.entries(el.faces).forEach(([f, d]) => {
			const ref = json.textures[d.texture.slice(1)];
			faces[f] = { uv: d.uv, texture: tex(ref).uuid };
		});
		['north', 'south', 'east', 'west', 'up', 'down'].forEach(f => { if (!faces[f]) faces[f] = { texture: null }; });
		const r = el.rotation || { angle: 0, axis: 'y', origin: [8, 8, 8] };
		const rotation = [0, 0, 0]; rotation['xyz'.indexOf(r.axis)] = r.angle;
		const cube = new Cube({
			name: name + ':' + (el.name || 'cube'), autouv: 0, faces, rotation,
			from: [el.from[0] + ox, el.from[1], el.from[2] + oz], to: [el.to[0] + ox, el.to[1], el.to[2] + oz],
			origin: [r.origin[0] + ox, r.origin[1], r.origin[2] + oz]
		}).init();
		made.push(cube);
	});
});
Undo.finishEdit('Showcase', { elements: made, outliner: true });
Canvas.updateAll();
return made.length;
})()
