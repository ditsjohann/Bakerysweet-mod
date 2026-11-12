(function () {
// Fourth wave: soft, 32x32 bark, log ends and stripped logs for all seven fruit woods (leaves: bk_leaves5.js).
// Shading is gentle (half-steps between ramp colours, no near-black), and every texture tiles seamlessly.
const ART = window.ART, T = ART.T, rnd = ART.rnd;
const W = 32;
const B = n => 'block/' + n;
const TAU = Math.PI * 2;
const R = {};
const ramp = base => R[base] || (R[base] = ART.ramp(base).map(c => [c.r, c.g, c.b]));
const hex = c => '#' + c.map(v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')).join('');
// Soft shade: v runs 0..5 along the colour's ramp and is rounded to half steps, which blend two neighbours.
const sh = (base, v) => {
	v = Math.max(0, Math.min(5, Math.round(v * 2) / 2));
	const r = ramp(base), i = Math.floor(v);
	if (v === i) return hex(r[i]);
	return hex(r[i].map((c, k) => (c + r[i + 1][k]) / 2));
};
const prng = seed => () => {
	seed = (seed + 0x6D2B79F5) | 0;
	let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
	t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
	return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};
const wrap = v => ((v % W) + W) % W;
const wrapD = d => { d = wrap(d); return d > W / 2 ? d - W : d; };
// Smooth value noise that wraps at the texture edge (cell must divide 32).
const vnoise = (x, y, cell, seed) => {
	const n = W / cell, gx = Math.floor(x / cell), gy = Math.floor(y / cell);
	const fx = x / cell - gx, fy = y / cell - gy, s = t => t * t * (3 - 2 * t);
	const at = (i, j) => rnd(((i % n) + n) % n, ((j % n) + n) % n, seed);
	const a = at(gx, gy), b = at(gx + 1, gy), c = at(gx, gy + 1), d = at(gx + 1, gy + 1);
	return (a + (b - a) * s(fx)) * (1 - s(fy)) + (c + (d - c) * s(fx)) * s(fy);
};
const tex = fn => T(W, W).fill(fn);

// ================= Bark =================
// Rounded vertical plates between soft, meandering fissures, lit gently from the left.
function plates(o) {
	const r = prng(o.seed);
	const fis = [];
	for (let i = 0; i < o.n; i++) fis.push({ x0: i * W / o.n + r() * 2, a: 0.8 + r() * o.amp, p: r() * TAU, b: r() * o.amp * 0.5, q: r() * TAU });
	const fx = (f, y) => f.x0 + f.a * Math.sin(y * TAU / W + f.p) + f.b * Math.sin(y * TAU * 2 / W + f.q);
	const breaks = fis.map(() => { const s = []; for (let k = 0; k < (o.breaks || 0); k++) s.push(Math.floor(r() * W)); return s; });
	return (x, y) => {
		let dl = 99, dr = 99, plate = 0;
		fis.forEach((f, i) => {
			const d = wrapD(x + 0.5 - fx(f, y + 0.5));
			if (d >= 0 && d < dl) { dl = d; plate = i; }
			if (d < 0 && -d < dr) dr = -d;
		});
		const t = dl / (dl + dr);
		const crack = o.crack * (0.7 + 0.6 * vnoise(x, y, 8, o.seed + 1));
		if (dl < crack) return { v: 1.5, plate, t };
		if (dl < crack + 1) return { v: 2.5, plate, t };
		const br = breaks[plate], cut = 0.3 + 0.4 * rnd(plate, y, o.seed + 7);
		if (br.includes(y) && t < cut) return { v: 1.5, plate, t };
		if (br.includes(wrap(y - 1)) && t < cut) return { v: 4, plate, t };
		let v = 2.7 + 1.3 * Math.pow(Math.sin(Math.PI * Math.min(1, t * 1.1)), 0.6) - 0.6 * t;
		if (dr < 1.2) v -= 0.5;
		v += (vnoise(x, y, 4, o.seed + 2) - 0.5) * 0.9;
		return { v, plate, t };
	};
}
function platedBark(o) {
	const p = plates(o);
	return tex((x, y) => {
		const q = p(x, y);
		if (o.lichen && q.v > 2.6 && vnoise(x, y, 4, o.seed + 9) > 1 - o.lichen[1] && rnd(x, y, o.seed + 10) < 0.8) {
			return sh(o.lichen[0], 3 + (rnd(x, y, o.seed + 11) < 0.3 ? 0.5 : 0) - (rnd(x, y, o.seed + 12) < 0.2 ? 0.5 : 0));
		}
		const col = o.alt && rnd(q.plate, Math.floor(y / 8), o.seed) < o.alt[1] ? o.alt[0] : o.base;
		return sh(q.v < 2 ? (o.dark || col) : col, q.v);
	});
}

// Palm: soft stacked rings with a lip of light on top, fibres trailing down, and a hairy fringe in each crease.
function palmBark() {
	const base = '#b08d68', dark = '#7a5c44', fibre = '#dcc49a';
	return tex((x, y) => {
		const wave = Math.round(1.2 * Math.sin(x * TAU / W + 0.6) + 0.6 * Math.sin(x * TAU * 2 / W));
		const yy = y + wave, ring = Math.floor(wrap(yy) / 8), ry = wrap(yy) % 8;
		if (ry === 0) return rnd(x, ring, 21) < 0.3 ? sh(fibre, 2.5) : sh(dark, 2);
		if (ry === 1) return rnd(x, ring, 22) < 0.25 ? sh(fibre, 3) : sh(base, 4.5);
		const strand = rnd(x, ring, 25) < 0.22 && ry > 2 + Math.floor(rnd(x, ring, 26) * 3);
		let v = 4 - (ry - 1) * 0.3 + (vnoise(x, y, 4, 27) - 0.5) * 0.6;
		if (strand) return sh(fibre, v - 0.5);
		return sh(base, v);
	});
}

// Cherry: smooth rosy bark with soft horizontal bands and pale lenticel dashes.
function cherryBark() {
	const base = '#84504a', len = '#dcae9a';
	const r = prng(31), dashes = [];
	for (let i = 0; i < 16; i++) dashes.push({ x: Math.floor(r() * W), y: Math.floor(r() * W), w: 3 + Math.floor(r() * 5) });
	return tex((x, y) => {
		for (const d of dashes) {
			const dx = wrap(x - d.x);
			if (dx < d.w && y === d.y) return sh(len, dx === 0 || dx === d.w - 1 ? 2.5 : 3);
			if (dx < d.w && y === wrap(d.y + 1)) return sh(base, 2.5);
		}
		const band = vnoise(x, y, 16, 32) * 0.4 + Math.sin(y * TAU * 3 / W + vnoise(x, y, 8, 33) * 2) * 0.35;
		return sh(base, 3.1 + band + (vnoise(x, y, 4, 34) - 0.5) * 0.5);
	});
}

// Plum: lavender-grey plates with papery strips curling off.
function plumBark() {
	const base = '#76627c', peel = '#c4aec6';
	const p = plates({ n: 6, amp: 1.5, crack: 0.8, seed: 42 });
	const r = prng(41), curls = [];
	for (let i = 0; i < 9; i++) curls.push({ x: Math.floor(r() * W), y: Math.floor(r() * W), w: 3 + Math.floor(r() * 4) });
	return tex((x, y) => {
		for (const c of curls) {
			const dx = wrap(x - c.x);
			if (dx < c.w && y === c.y) return sh(peel, dx === 0 ? 2.5 : 3.5);
			if (dx < c.w && y === wrap(c.y + 1)) return sh(peel, 2);
			if (dx < c.w && y === wrap(c.y + 2)) return sh(base, 2);
		}
		return sh(base, p(x, y).v);
	});
}

const BARK = {
	palm: palmBark,
	citrus: () => platedBark({ base: '#9c977a', dark: '#77735c', n: 6, amp: 2.2, crack: 0.7, seed: 51, lichen: ['#cfd8a2', 0.12] }),
	blossom: cherryBark,
	orangewood: () => platedBark({ base: '#958468', dark: '#6e604a', n: 7, amp: 1.6, crack: 0.6, seed: 52, alt: ['#a09274', 0.35] }),
	peachwood: () => platedBark({ base: '#93604c', dark: '#6a3e30', n: 5, amp: 3.0, crack: 1.1, breaks: 3, seed: 53 }),
	mangowood: () => platedBark({ base: '#7c7060', dark: '#564c40', n: 6, amp: 1.8, crack: 1.0, breaks: 4, seed: 54, lichen: ['#bccaa4', 0.1] }),
	plumwood: plumBark
};
const BARK_BASE = { palm: '#b08d68', citrus: '#9c977a', blossom: '#84504a', orangewood: '#958468', peachwood: '#93604c', mangowood: '#7c7060', plumwood: '#76627c' };
const PLANK = { palm: '#e8cfa6', citrus: '#f3e2a2', blossom: '#f2b6c6', orangewood: '#eaa868', peachwood: '#f2c3a6', mangowood: '#e4bb62', plumwood: '#b896c2' };

// Log end: a soft bark rim, a pale band of sapwood, gently wobbling rings, a small heart and a short crack.
function logTop(name, stripped) {
	const bark = BARK_BASE[name], wood = PLANK[name], r = prng(name.length * 13 + (stripped ? 7 : 0));
	const crackAng = Math.floor(r() * 8) * TAU / 8, ph = r() * TAU;
	return tex((x, y) => {
		const dx = x - 15.5, dy = y - 15.5;
		const m = Math.max(Math.abs(dx), Math.abs(dy)), h = Math.hypot(dx, dy);
		if (m > 14) return stripped ? sh(wood, 2.5) : sh(bark, 3 + (vnoise(x, y, 4, 61) - 0.5) * 1.2);
		if (m > 13) return stripped ? sh(wood, 3) : sh(bark, 2);
		const d = 0.6 * m + 0.4 * h + 0.6 * Math.sin(Math.atan2(dy, dx) * 3 + ph) * (h / 14);
		if (d > 12) return sh(wood, 4);
		const along = dx * Math.cos(crackAng) + dy * Math.sin(crackAng), across = Math.abs(-dx * Math.sin(crackAng) + dy * Math.cos(crackAng));
		if (along > 1.5 && along < 7 && across < 0.6) return sh(wood, 1.5);
		if (d < 1.6) return sh(wood, 2);
		const ring = Math.floor(d / 2.2);
		return sh(wood, (ring % 2 ? 2.5 : 3.5) + (d % 2.2 < 0.7 && ring % 2 === 0 ? -0.5 : 0) + (vnoise(x, y, 4, 63) - 0.5) * 0.5);
	});
}

// Stripped side: soft, long, wavy grain over the plank colour.
function strippedSide(name) {
	const wood = PLANK[name], r = prng(name.length * 7 + 3);
	const lines = [];
	for (let i = 0; i < 7; i++) lines.push({ x0: i * W / 7 + r() * 3, a: 0.8 + r() * 1.8, p: r() * TAU });
	return tex((x, y) => {
		for (const g of lines) {
			const d = Math.abs(wrapD(x + 0.5 - (g.x0 + g.a * Math.sin(y * TAU / W + g.p))));
			if (d < 0.5) return sh(wood, 2.5);
			if (d < 1.5) return sh(wood, 3.5);
		}
		return sh(wood, 3 + (vnoise(x, y, 4, 64) - 0.5) * 0.8);
	});
}

Object.keys(PLANK).forEach(name => {
	BARK[name]().save(B(name + '_log'));
	logTop(name, false).save(B(name + '_log_top'));
	strippedSide(name).save(B('stripped_' + name + '_log'));
	logTop(name, true).save(B('stripped_' + name + '_log_top'));
});
return Object.keys(PLANK).length * 4 + ' soft 32x32 wood textures';
})()
