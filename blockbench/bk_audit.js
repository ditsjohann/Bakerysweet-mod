(function () {
// Audits every block model for things that render as glitches in game.
const fs = require('fs'), path = require('path');
const ROOT = 'C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/src/main/resources/assets/bakery/';
const dir = ROOT + 'models/block/';
const issues = [];
const DIRS = { north: ['z', 0], south: ['z', 1], west: ['x', 0], east: ['x', 1], down: ['y', 0], up: ['y', 1] };
const AX = { x: 0, y: 1, z: 2 };
function texExists(ref) {
	if (!ref || ref.startsWith('#')) return true;
	const [ns, p] = ref.includes(':') ? ref.split(':') : ['minecraft', ref];
	if (ns !== 'bakery') return true;
	return fs.existsSync(ROOT + 'textures/' + p + '.png');
}
fs.readdirSync(dir).filter(f => f.endsWith('.json')).forEach(f => {
	const json = JSON.parse(fs.readFileSync(dir + f, 'utf8'));
	const name = f.replace('.json', '');
	Object.entries(json.textures || {}).forEach(([k, v]) => { if (!texExists(v)) issues.push(name + ': missing texture ' + k + ' -> ' + v); });
	const els = json.elements || [];
	els.forEach((el, i) => {
		Object.entries(el.faces || {}).forEach(([face, d]) => {
			if (d.uv && d.uv.some(v => v < -0.001 || v > 16.001)) issues.push(name + ': element ' + (el.name || i) + ' ' + face + ' uv out of range ' + JSON.stringify(d.uv));
			const key = d.texture && d.texture.slice(1);
			if (key && json.textures && !(key in json.textures)) issues.push(name + ': element ' + (el.name || i) + ' uses undefined #' + key);
		});
		el.from.concat(el.to).forEach(v => { if (v < -16 || v > 32) issues.push(name + ': element ' + (el.name || i) + ' outside -16..32'); });
	});
	// Coplanar overlapping faces facing the same way (z-fighting), only for unrotated elements.
	for (let a = 0; a < els.length; a++) for (let b = a + 1; b < els.length; b++) {
		const A = els[a], B = els[b];
		if ((A.rotation && A.rotation.angle) || (B.rotation && B.rotation.angle)) continue;
		Object.entries(DIRS).forEach(([face, [axis, side]]) => {
			if (!A.faces[face] || !B.faces[face]) return;
			const k = AX[axis];
			const pa = side ? A.to[k] : A.from[k], pb = side ? B.to[k] : B.from[k];
			if (Math.abs(pa - pb) > 0.001) return;
			const others = [0, 1, 2].filter(i => i !== k);
			const overlap = others.every(i => Math.min(A.to[i], B.to[i]) - Math.max(A.from[i], B.from[i]) > 0.01);
			if (overlap) issues.push(name + ': z-fighting ' + face + ' between ' + (A.name || a) + ' and ' + (B.name || b));
		});
	}
});
return issues.length + ' issues\n' + issues.slice(0, 120).join('\n');
})()
