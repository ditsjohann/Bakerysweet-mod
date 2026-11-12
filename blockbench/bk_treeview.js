(function () {
// Rebuilds real grown trees (from the game test's tree_dump.json) as cubes in Blockbench, with the mod's textures.
const fs = require('fs');
const ROOT = 'C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/';
const TEX = ROOT + 'src/main/resources/assets/bakery/textures/block/';
const dump = JSON.parse(fs.readFileSync(ROOT + 'build/gametest/tree_dump.json', 'utf8'));
// Keys are "<wood>_<variant>"; trees stand in one row along x, each wood's variants side by side.
const names = Object.keys(dump).filter(k => !window.TREES || window.TREES.includes(k.replace(/_\d+$/, '')));
const spacing = window.TREE_SPACING || 11;
const woodsSeen = [];
try { ModelProject.all.slice().forEach(p => { p.saved = true; p.close(true); }); } catch (e) { }
Formats.free.new();
Project.name = 'tree_preview';
const cache = {};
const tex = file => {
	if (!cache[file]) {
		const path = TEX + file + '.png';
		if (!fs.existsSync(path)) return null;
		const t = new Texture({ name: file }).fromPath(path);
		t.add(false);
		cache[file] = t;
	}
	return cache[file];
};
const vanilla = { sand: '#dbcf9f', grass_block: '#6aa84f' };
const made = [];
Undo.initEdit({ elements: [], outliner: true, textures: [] });
names.forEach(name => {
	const wood = name.replace(/_\d+$/, ''), variant = +(name.match(/_(\d+)$/) || [0, 0])[1];
	if (!woodsSeen.includes(wood)) woodsSeen.push(wood);
	const ox = (woodsSeen.indexOf(wood) * 3 + variant) * spacing * 16, oz = 0;
	(dump[name] || []).forEach(([x, y, z, id]) => {
		const from = [x * 16 + ox, y * 16, z * 16 + oz], to = [x * 16 + 16 + ox, y * 16 + 16, z * 16 + 16 + oz];
		let faces;
		if (id.endsWith('_log') || id.endsWith('_wood')) {
			const side = tex(id), top = tex(id + '_top') || side;
			faces = { north: side, south: side, east: side, west: side, up: top, down: top };
		} else if (id.endsWith('_leaves')) {
			const l = tex(id);
			faces = { north: l, south: l, east: l, west: l, up: l, down: l };
		} else if (id in vanilla) {
			return;
		} else {
			const fruit = { coconut: 'coconut_shell', lemon: 'lemon_peel', cherries: 'fruit_cherry', orange: 'fruit_orange', peach: 'fruit_peach', mango: 'fruit_mango', plum: 'fruit_plum' }[id];
			if (!fruit) return;
			const f = tex(fruit);
			from[0] += 5; from[1] += 6; from[2] += 5; to[0] -= 5; to[1] -= 3; to[2] -= 5;
			faces = { north: f, south: f, east: f, west: f, up: f, down: f };
		}
		const f = {};
		Object.entries(faces).forEach(([k, t]) => f[k] = t ? { texture: t.uuid, uv: [0, 0, 16, 16] } : { texture: null });
		made.push(new Cube({ name: id, from, to, faces: f, autouv: 0, origin: [8, 8, 8] }).init());
	});
});
Undo.finishEdit('Tree preview', { elements: made, outliner: true });
Canvas.updateAll();
return made.length + ' blocks';
})()
