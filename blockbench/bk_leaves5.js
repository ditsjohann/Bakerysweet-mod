(function () {
// Leaves, drawn like real foliage: little twigs of hand-shaped leaves (pointed tip, midrib, one half in light
// and one in shade, a darker rim on the shadow side) overlapping each other. The sky only peeks through the
// small gaps left between leaves. Runs after bk_wood4.js and replaces its leaves. All textures are 32x32 and tile.
const ART = window.ART, rnd = ART.rnd;
const W = 32, TAU = Math.PI * 2;
const R = {};
const ramp = base => R[base] || (R[base] = ART.ramp(base).map(c => [c.r, c.g, c.b]));
const hex = c => '#' + c.map(v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')).join('');
const sh = (base, v) => {
	v = Math.max(0, Math.min(5, Math.round(v * 2) / 2));
	const r = ramp(base), i = Math.floor(v);
	return v === i ? hex(r[i]) : hex(r[i].map((c, k) => (c + r[i + 1][k]) / 2));
};
const prng = seed => () => {
	seed = (seed + 0x6D2B79F5) | 0;
	let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
	t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
	return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};
const wrap = v => ((v % W) + W) % W;
const idx = (x, y) => wrap(y) * W + wrap(x);
const pick = (list, v) => { let acc = 0; for (const [c, w] of list) { acc += w; if (v < acc) return c; } return list[list.length - 1][0]; };

function sheet(seed) {
	return { px: new Array(W * W).fill(null), cov: new Array(W * W).fill(false), rim: new Array(W * W).fill(false), r: prng(seed), seed };
}
function put(s, x, y, c, rim) {
	s.px[idx(x, y)] = c;
	s.cov[idx(x, y)] = true;
	s.rim[idx(x, y)] = !!rim;
}

// One leaf growing from (bx, by) towards angle a. o.point shapes the outline: below 1 is round, above 1 is slim.
function leaf(s, bx, by, a, L, wd, col, o) {
	o = o || {};
	const c = Math.cos(a), sn = Math.sin(a);
	// The half whose outward side faces the light (upper left) is the bright half.
	const lit = 0.6 * sn - 0.8 * c > 0 ? 1 : -1;
	const ext = Math.ceil(L) + 2, shade = o.shade || 0;
	for (let oy = -ext; oy <= ext; oy++) for (let ox = -ext; ox <= ext; ox++) {
		const X = Math.round(bx) + ox, Y = Math.round(by) + oy;
		const dx = X + 0.5 - bx, dy = Y + 0.5 - by;
		const u = dx * c + dy * sn, v = -dx * sn + dy * c;
		if (u < 0 || u > L) continue;
		const t = u / L;
		const half = (wd / 2) * Math.pow(Math.sin(Math.PI * Math.min(t, 0.999) * (o.round ? 0.85 : 1) + (o.round ? Math.PI * 0.15 * t : 0)), o.point || 1);
		if (Math.abs(v) > half + 0.2) continue;
		if (o.serrate && Math.abs(v) > half - 0.7 && Math.floor(u) % 2 === 0 && t > 0.15) continue;
		const bright = v * lit >= 0;
		let l = bright ? 3.5 : 2.5;
		const rim = Math.abs(v) > half - 0.75 || t > 0.85;
		if (rim) l += bright ? 0.5 : -0.5;
		if (wd >= 3 && Math.abs(v) < 0.5 && t > 0.1 && t < 0.8) l = bright ? 4 : 3;
		if (o.gloss && bright && t > 0.3 && t < 0.6 && Math.abs(Math.abs(v) - half * 0.5) < 0.5) l = 4.5;
		if (t > 0.9) l -= 0.5;
		put(s, X, Y, sh(col, Math.max(1, Math.min(5, l + shade))), rim);
	}
}

// A twig: n leaves fanning out from one point; leaves pointing up sit behind, leaves pointing down in front.
function twig(s, cx, cy, n, spec, droop) {
	const r = s.r, a0 = r() * TAU, angles = [];
	for (let i = 0; i < n; i++) {
		let a = a0 + i * TAU / n + (r() - 0.5) * 0.7;
		if (droop) a = Math.PI / 2 + (i - (n - 1) / 2) * 0.55 + (r() - 0.5) * 0.4;
		angles.push(a);
	}
	angles.sort((p, q) => Math.sin(p) - Math.sin(q));
	angles.forEach(a => {
		const L = spec.len * (0.8 + r() * 0.4), wd = spec.wid * (0.85 + r() * 0.3);
		const shade = (Math.sin(a) < -0.4 ? -0.5 : 0) + (r() < 0.25 ? -0.5 : 0);
		leaf(s, cx + Math.cos(a), cy + Math.sin(a), a, L, wd, pick(spec.colors, r()), { point: spec.point, round: spec.round, serrate: spec.serrate, gloss: spec.gloss, shade });
	});
}

// Twigs until `cover` of the sheet is leaf. Each new twig goes to the emptiest of a dozen random spots, so
// the leftover gaps end up small and evenly spread instead of pooling into big holes.
function foliage(s, spec, cover, droop) {
	const r = s.r;
	const empty = (x, y) => {
		let n = 0;
		for (let oy = -5; oy <= 5; oy++) for (let ox = -5; ox <= 5; ox++) if (ox * ox + oy * oy <= 25 && !s.cov[idx(Math.floor(x) + ox, Math.floor(y) + oy)]) n++;
		return n;
	};
	let guard = 0;
	while (s.cov.filter(Boolean).length < W * W * cover && guard++ < 200) {
		let best = null, bestN = -1;
		for (let k = 0; k < 12; k++) {
			const x = r() * W, y = r() * W, n = empty(x, y);
			if (n > bestN) { best = [x, y]; bestN = n; }
		}
		twig(s, best[0], best[1], spec.n + Math.floor(r() * 2), spec, droop);
	}
}
// A round flower with five lobed petals, lit from the upper left, with a coloured heart.
function flower(s, cx, cy, rad, col, heart) {
	const rot = s.r() * TAU, ext = Math.ceil(rad) + 1;
	for (let oy = -ext; oy <= ext; oy++) for (let ox = -ext; ox <= ext; ox++) {
		const X = Math.round(cx) + ox, Y = Math.round(cy) + oy;
		const dx = X + 0.5 - cx, dy = Y + 0.5 - cy, d = Math.hypot(dx, dy);
		const edge = rad * (0.4 + 0.6 * Math.pow(Math.abs(Math.cos(2.5 * (Math.atan2(dy, dx) - rot))), 0.7));
		if (d > edge) continue;
		if (d < rad * 0.28) { put(s, X, Y, heart); continue; }
		let l = 3 + d / rad * 1.2 - (dx * 0.6 + dy * 0.8) / rad * 0.6;
		if (d > edge - 0.8) l -= 0.5;
		put(s, X, Y, sh(col, Math.max(2, Math.min(4.5, l))));
	}
}
function flowers(s, n, rad, col, heart) {
	for (let i = 0; i < n; i++) flower(s, s.r() * W, s.r() * W, rad * (0.85 + s.r() * 0.3), col, heart);
}
function buds(s, n, col) {
	for (let i = 0; i < n; i++) {
		const x = Math.floor(s.r() * W), y = Math.floor(s.r() * W);
		put(s, x, y, sh(col, 3.5));
		put(s, x + 1, y, sh(col, 3));
		put(s, x, y + 1, sh(col, 2.5));
	}
}

// Palm: two fronds crossing the tile, one sweeping each way: a pale stem with long slim leaflets on both
// sides, angled forward and drooping. A stem running 4 across for every 1 down closes on itself after one
// tile, so it tiles seamlessly.
function fronds(s, spec) {
	const r = s.r, len = Math.hypot(W, W / 4);
	[[1, 0], [-1, 16]].forEach(([dir, y0]) => {
		const a = Math.atan2(1, 4 * dir), c = Math.cos(a), sn = Math.sin(a);
		const x0 = r() * W, steps = 12;
		for (let k = 0; k < steps; k++) {
			const t = (k / steps) * len, x = x0 + c * t, y = y0 + sn * t;
			[-1, 1].forEach(side => {
				// Leaflets lean forward along the stem and hang down a little.
				const la = a + side * (0.7 + r() * 0.3) * dir + 0.3 * dir * (side > 0 ? 1 : -0.3);
				leaf(s, x, y, la, spec.len * (0.75 + r() * 0.4), spec.wid, pick(spec.colors, r()), { point: 1.3, shade: side < 0 ? -0.5 : 0 });
			});
		}
		for (let t = 0; t < len; t += 0.5) put(s, Math.floor(x0 + c * t), Math.floor(y0 + sn * t), sh(spec.stem, 3));
	});
}
// The gaps between leaves are see-through, except right under and beside a leaf (light comes from the upper
// left), where a one-pixel shadow gives the clump depth. So the holes are the natural spaces between leaves.
function finish(s, shadow) {
	const out = ART.T(W, W);
	out.fill((x, y) => {
		const i = idx(x, y);
		if (s.cov[i]) return s.px[i];
		const shaded = s.cov[idx(x, y - 1)] || (s.cov[idx(x - 1, y)] && rnd(x, y, s.seed) < 0.5);
		return shaded ? sh(shadow, 1.5) : null;
	});
	return out;
}
const B = n => 'block/' + n + '_leaves';
const LEAVES = {
	palm: () => {
		const s = sheet(201);
		fronds(s, { len: 11, wid: 1.7, colors: [['#5c9e44', 0.5], ['#72b256', 0.4], ['#9cc062', 0.1]], stem: '#c2b870' });
		return finish(s, '#3f6e36');
	},
	citrus: () => {
		const s = sheet(202);
		foliage(s, { n: 5, len: 7, wid: 4.6, point: 0.7, round: true, gloss: true, colors: [['#4f9a46', 0.55], ['#64ae54', 0.45]] }, 0.74);
		flowers(s, 5, 2.4, '#fffaf0', '#f2d048');
		return finish(s, '#2f6634');
	},
	blossom: () => {
		const s = sheet(203);
		foliage(s, { n: 4, len: 6, wid: 3.6, point: 0.9, colors: [['#7fbc58', 0.6], ['#94cc6a', 0.4]] }, 0.45);
		flowers(s, 11, 4.4, '#f7a8c6', '#ffe08a');
		flowers(s, 8, 3.8, '#ffd2e2', '#ffe08a');
		buds(s, 6, '#e8789c');
		return finish(s, '#c47a94');
	},
	orangewood: () => {
		const s = sheet(204);
		foliage(s, { n: 5, len: 8, wid: 4.4, point: 0.85, gloss: true, colors: [['#3c7f45', 0.6], ['#4b9150', 0.4]] }, 0.76);
		flowers(s, 6, 2.3, '#ffffff', '#f4c04a');
		return finish(s, '#24522e');
	},
	peachwood: () => {
		const s = sheet(205);
		foliage(s, { n: 5, len: 10, wid: 3, point: 1.4, colors: [['#80ba56', 0.5], ['#98ca68', 0.4], ['#b4d678', 0.1]] }, 0.7);
		flowers(s, 4, 2.2, '#ffb6cc', '#e8708f');
		buds(s, 7, '#f490ac');
		return finish(s, '#4e7e3c');
	},
	mangowood: () => {
		const s = sheet(206);
		foliage(s, { n: 5, len: 11, wid: 3.6, point: 1.2, gloss: true, colors: [['#327a40', 0.5], ['#448c48', 0.38], ['#8ec05a', 0.12]] }, 0.74, true);
		return finish(s, '#1f4c2a');
	},
	plumwood: () => {
		const s = sheet(207);
		foliage(s, { n: 5, len: 7.5, wid: 4.2, point: 0.9, colors: [['#8a4f80', 0.55], ['#a8699c', 0.45]] }, 0.74);
		flowers(s, 6, 2.2, '#f4dcf4', '#fff0b8');
		return finish(s, '#43223f');
	}
};
Object.keys(LEAVES).forEach(n => LEAVES[n]().save(B(n)));
return Object.keys(LEAVES).length + ' leaf textures';
})()
