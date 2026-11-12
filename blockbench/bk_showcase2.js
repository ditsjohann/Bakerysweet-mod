(function () {
// Loads several exported models side by side into one free-format Blockbench scene for review.
const ART = window.ART, fs = ART.fs, path = ART.path;
const list = window.SHOWCASE;
try { ModelProject.all.slice().forEach(p => { p.saved = true; p.close(true); }); } catch (e) { }
Formats.free.new();
Project.name = 'bakery_showcase';
const cache = {};
const tex = ref => {
	const file = ref.replace('bakery:', '');
	if (!cache[file]) {
		const t = new Texture({ name: file }).fromPath(path.join(ART.ROOT, 'textures', file + '.png'));
		t.add(false);
		cache[file] = t;
	}
	return cache[file];
};
const made = [];
Undo.initEdit({ elements: [], outliner: true, textures: [] });
list.forEach(([name, ox, oz]) => {
	const json = JSON.parse(fs.readFileSync(path.join(ART.ROOT, 'models', 'block', name + '.json'), 'utf8'));
	(json.elements || []).forEach(el => {
		const faces = {};
		Object.entries(el.faces).forEach(([f, d]) => {
			const ref = json.textures[d.texture.slice(1)];
			const t = tex(ref);
			const sx = t.width / 16, sy = t.height / 16;
			faces[f] = { uv: [d.uv[0] * sx, d.uv[1] * sy, d.uv[2] * sx, d.uv[3] * sy], texture: t.uuid };
		});
		['north', 'south', 'east', 'west', 'up', 'down'].forEach(f => { if (!faces[f]) faces[f] = { texture: null }; });
		const r = el.rotation || { angle: 0, axis: 'y', origin: [8, 8, 8] };
		const rotation = [0, 0, 0]; rotation['xyz'.indexOf(r.axis)] = r.angle;
		made.push(new Cube({ name: name, autouv: 0, faces, rotation,
			from: [el.from[0] + ox, el.from[1], el.from[2] + oz], to: [el.to[0] + ox, el.to[1], el.to[2] + oz],
			origin: [r.origin[0] + ox, r.origin[1], r.origin[2] + oz] }).init());
	});
});
Undo.finishEdit('Showcase', { elements: made, outliner: true });
Canvas.updateAll();
return made.length;
})()
