(function () {
// Café customers: twelve player-format skins (64x64). The first eight use slim arms.
// Big sparkly eyes, rosy cheeks, pastel outfits, bows and buns. Runs after bk_art.js.
const ART = window.ART, T = ART.T, rnd = ART.rnd;
const R = {};
const ramp = base => R[base] || (R[base] = ART.ramp(base).map(ART.toHex));
const lv = (base, i) => ramp(base)[Math.max(0, Math.min(5, Math.round(i)))];

// Every face of every box: [u, v, w, h] for a box of size (w, h, d) at (u, v): top, bottom, right, front, left, back.
function faces(u, v, w, h, d) {
	return {
		top: [u + d, v, w, d], bottom: [u + d + w, v, w, d],
		right: [u, v + d, d, h], front: [u + d, v + d, w, h], left: [u + d + w, v + d, d, h], back: [u + d + w + d, v + d, w, h]
	};
}

const LOOKS = [
	// 0: strawberry girl - long pink hair, strawberry dress, red bow
	{ skin: '#ffe2d2', hair: '#ff9fc0', style: 'long', eye: '#7a3a5a', top: '#ff8fb3', pattern: 'dots', patternColor: '#ffffff', bottom: 'dress', shoes: '#ffffff', bow: '#e8304e' },
	// 1: mint bob, white blouse and mint skirt
	{ skin: '#ffe6d8', hair: '#8a5a3a', style: 'bob', eye: '#3a6a5a', top: '#ffffff', collar: '#9fdcc2', pattern: 'none', bottom: 'skirt', skirt: '#9fdcc2', shoes: '#8a5a3a', bow: '#9fdcc2' },
	// 2: blonde space buns, lilac dress with a white collar
	{ skin: '#fde3d4', hair: '#ffe08a', style: 'buns', eye: '#6a5aa8', top: '#c8a6f2', collar: '#ffffff', pattern: 'none', bottom: 'dress', shoes: '#f7a8c6' },
	// 3: long black hair, sunny yellow sundress, flower clip
	{ skin: '#e9b98f', hair: '#2e2430', style: 'long', eye: '#4a2a1a', top: '#ffe27a', pattern: 'flowers', patternColor: '#ffffff', bottom: 'dress', shoes: '#ffffff', flower: '#ff8fb3' },
	// 4: ginger braids and freckles, blue gingham
	{ skin: '#fde6d6', hair: '#e07a3a', style: 'braids', eye: '#3a7a5a', top: '#9ccaf2', pattern: 'gingham', patternColor: '#ffffff', bottom: 'dress', shoes: '#c06a3a', freckles: true },
	// 5: lavender waves, pink cardigan, plaid skirt
	{ skin: '#fff0e8', hair: '#c8a6f2', style: 'long', eye: '#8a4a8a', top: '#ffc0d3', pattern: 'buttons', patternColor: '#ffffff', bottom: 'skirt', skirt: '#9a7ad0', skirtPattern: 'plaid', shoes: '#5a4a62', bow: '#ffffff' },
	// 6: curly puffs, coral top, denim skirt, gold earrings
	{ skin: '#8a5a3e', hair: '#2a1a14', style: 'puffs', eye: '#2a1a14', top: '#ff9a8a', pattern: 'heart', patternColor: '#ffffff', bottom: 'skirt', skirt: '#7aa6d8', shoes: '#ffffff', earrings: '#ffd84a' },
	// 7: chestnut ponytail, strawberry overalls
	{ skin: '#ffe0cc', hair: '#9a5a32', style: 'ponytail', eye: '#5a3a2a', top: '#ffffff', pattern: 'none', bottom: 'overalls', overalls: '#ff6f8f', shoes: '#ffffff', bow: '#ff6f8f' },
	// 8: boy, brown hair, striped tee, khakis
	{ skin: '#ffe0cc', hair: '#6a4026', style: 'short', eye: '#3a5a8a', top: '#9ccaf2', pattern: 'stripes', patternColor: '#ffffff', bottom: 'pants', pants: '#d8c09a', shoes: '#ffffff' },
	// 9: boy, black hair, green hoodie, jeans
	{ skin: '#d9a57a', hair: '#1e1a20', style: 'short', eye: '#2a1a14', top: '#8fd9b6', pattern: 'hoodie', patternColor: '#ffffff', bottom: 'pants', pants: '#6a8ac8', shoes: '#f4f4f4' },
	// 10: blonde, cream knit sweater, brown trousers, round glasses
	{ skin: '#ffe8d8', hair: '#f0d070', style: 'short', eye: '#4a6a3a', top: '#f6ead2', pattern: 'knit', patternColor: '#e6d2b2', bottom: 'pants', pants: '#8a6a4a', shoes: '#5a3a2a', glasses: true },
	// 11: grandma: silver bun, pink cardigan, lilac skirt, glasses
	{ skin: '#ffe6dc', hair: '#e4e0ea', style: 'granny', eye: '#5a4a62', top: '#ffb3c7', pattern: 'buttons', patternColor: '#ffffff', bottom: 'skirt', skirt: '#c8a6f2', shoes: '#8a5a6a', glasses: true }
];

function paint(look, slim) {
	const t = T(64, 64);
	const S = (x, y, c) => { if (c) t.set(x, y, c); };
	const fill = (r, fn) => { const [u, v, w, h] = r; for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) S(u + x, v + y, fn(x, y, w, h)); };
	const n = (x, y, s) => rnd(x, y, s) < 0.18 ? (rnd(x, y, s + 1) < 0.5 ? -1 : 1) : 0;
	const skin = (x, y) => lv(look.skin, 3 + (n(x, y, 7) > 0 ? 0 : 0));
	const hair = (x, y) => lv(look.hair, 3 + ((x + y) % 3 === 0 ? 1 : 0) + (y % 4 === 3 ? -1 : 0));

	// ---------------- head ----------------
	const head = faces(0, 0, 8, 8, 8);
	fill(head.top, hair);
	fill(head.bottom, skin);
	fill(head.back, hair);
	fill(head.right, (x, y) => (y < 3 || x < 3 || (look.style === 'long' || look.style === 'braids' || look.style === 'bob' || look.style === 'ponytail' || look.style === 'granny')) ? (y >= 5 && x >= 5 && look.style !== 'long' ? skin(x, y) : hair(x, y)) : skin(x, y));
	fill(head.left, (x, y) => (y < 3 || x > 4 || (look.style === 'long' || look.style === 'braids' || look.style === 'bob' || look.style === 'ponytail' || look.style === 'granny')) ? (y >= 5 && x <= 2 && look.style !== 'long' ? skin(x, y) : hair(x, y)) : skin(x, y));
	fill(head.front, (x, y) => {
		// fringe
		const fringe = y === 0 || y === 1 || (y === 2 && (x === 0 || x === 7 || x === 2 || x === 5));
		if (fringe) return hair(x, y);
		if ((x === 0 || x === 7) && y < 7 && look.style !== 'short') return hair(x, y);
		// eyes: big, two pixels tall, with a sparkle
		if ((x === 1 || x === 2 || x === 5 || x === 6) && (y === 4 || y === 5)) {
			if (y === 4 && (x === 1 || x === 5)) return '#ffffff';
			if (y === 4) return '#2a1a24';
			return x === 2 || x === 6 ? '#2a1a24' : look.eye;
		}
		if ((x === 1 || x === 2 || x === 5 || x === 6) && y === 3) return look.glasses ? '#5a4a52' : null;
		// rosy cheeks + tiny smile
		if ((x === 1 || x === 6) && y === 6) return look.freckles ? lv('#d0805a', 3) : lv('#ff9fb0', 3);
		if (look.freckles && (x === 2 || x === 5) && y === 6) return lv('#d0805a', 3);
		if ((x === 3 || x === 4) && y === 6) return x === 3 ? lv('#e0607a', 3) : lv('#e0607a', 4);
		return skin(x, y);
	});
	if (look.glasses) {
		[[0, 4], [3, 4], [4, 4], [7, 4]].forEach(([x, y]) => S(8 + x, 8 + y, '#5a4a52'));
		[[1, 3], [2, 3], [5, 3], [6, 3]].forEach(([x, y]) => S(8 + x, 8 + y, '#5a4a52'));
	}
	// head eyes fix: fill the eyebrow row with skin where glasses aren't
	for (const x of [1, 2, 5, 6]) if (!look.glasses) S(8 + x, 8 + 3, lv(look.skin, 3));

	// ---------------- hat layer: hair volume and accessories ----------------
	const hat = faces(32, 0, 8, 8, 8);
	fill(hat.top, (x, y) => look.style === 'short' && (x + y) % 4 === 0 ? hair(x, y) : look.style === 'short' ? null : hair(x, y));
	fill(hat.front, (x, y) => y === 0 || (y === 1 && x % 2 === 0) ? hair(x, y) : null);
	const longSides = (x, y, w) => (look.style === 'long' || look.style === 'braids' || look.style === 'granny' || look.style === 'ponytail') ? hair(x, y) : (y < 4 ? hair(x, y) : null);
	fill(hat.right, longSides);
	fill(hat.left, longSides);
	fill(hat.back, (x, y) => hair(x, y));
	// bows, flowers, buns
	if (look.bow) {
		[[1, 0], [2, 0], [0, 1], [1, 1], [2, 1], [3, 1], [1, 2], [2, 2]].forEach(([x, y]) => S(hat.top[0] + 4 + x - 2, hat.top[1] + y + 1, lv(look.bow, y === 0 ? 4 : 3)));
		[[5, 0], [6, 0], [6, 1], [7, 1]].forEach(([x, y]) => S(hat.left[0] + x, hat.left[1] + y, lv(look.bow, 3)));
	}
	if (look.flower) {
		[[6, 1], [7, 2], [6, 3], [5, 2]].forEach(([x, y]) => S(hat.front[0] + x, hat.front[1] + y, look.flower));
		S(hat.front[0] + 6, hat.front[1] + 2, '#ffe27a');
	}
	if (look.style === 'buns' || look.style === 'puffs' || look.style === 'granny') {
		const c = look.style === 'granny' ? [[3, 2], [4, 2], [3, 3], [4, 3], [2, 3], [5, 3]] : [[0, 0], [1, 0], [0, 1], [1, 1], [6, 0], [7, 0], [6, 1], [7, 1]];
		c.forEach(([x, y]) => S(hat.top[0] + x, hat.top[1] + y, lv(look.hair, 4)));
	}
	if (look.earrings) {
		S(head.right[0] + 4, head.right[1] + 6, look.earrings);
		S(head.left[0] + 3, head.left[1] + 6, look.earrings);
	}

	// ---------------- body ----------------
	const body = faces(16, 16, 8, 12, 4);
	const topColor = (x, y, side) => {
		let c = lv(look.top, 3 + (y === 0 ? 1 : 0));
		const p = look.pattern;
		if (p === 'dots' && (x + y * 3) % 5 === 0 && y > 0) c = look.patternColor;
		if (p === 'stripes' && y % 3 === 1) c = look.patternColor;
		if (p === 'gingham' && (x % 2 === 0 || y % 2 === 0)) c = x % 2 === 0 && y % 2 === 0 ? lv(look.top, 2) : lv(look.top, 4);
		if (p === 'flowers' && (x * 3 + y * 5) % 9 === 0) c = look.patternColor;
		if (p === 'knit' && (x + y) % 2 === 0) c = look.patternColor;
		if (p === 'buttons' && side === 'front' && (x === 3 || x === 4) && y % 3 === 1) c = look.patternColor;
		if (p === 'heart' && side === 'front' && [[3, 3], [5, 3], [2, 4], [3, 4], [4, 4], [5, 4], [6, 4], [3, 5], [4, 5], [5, 5], [4, 6]].some(([a, b]) => a === x && b === y)) c = look.patternColor;
		if (p === 'hoodie' && side === 'front' && y >= 6 && y <= 8 && x >= 2 && x <= 5) c = lv(look.top, 2);
		return c;
	};
	const lower = (x, y, side) => {
		// rows 8-11 of the body: skirt / dress / overalls / pants waist
		if (look.bottom === 'dress') return y === 11 ? lv(look.top, 2) : topColor(x, y, side);
		if (look.bottom === 'skirt') {
			let c = lv(look.skirt, 3 + (y === 8 ? -1 : 0));
			if (look.skirtPattern === 'plaid' && (x % 3 === 0 || y % 3 === 0)) c = lv(look.skirt, 1);
			return c;
		}
		if (look.bottom === 'overalls') return lv(look.overalls, 3);
		return lv(look.pants, 3 + (y === 8 ? -1 : 0));
	};
	for (const side of ['front', 'back', 'right', 'left']) {
		fill(body[side], (x, y) => {
			if (y >= 8) return lower(x, y, side);
			if (look.bottom === 'overalls' && side === 'front' && y >= 3 && x >= 2 && x <= 5) return y === 3 && (x === 2 || x === 5) ? '#ffe27a' : lv(look.overalls, 3);
			if (look.bottom === 'overalls' && (x === 1 || x === 6) && (side === 'front' || side === 'back')) return lv(look.overalls, 3);
			if (look.collar && side === 'front' && y === 0) return look.collar;
			if (side === 'front' && y === 0 && (x === 3 || x === 4)) return lv(look.skin, 3);
			return topColor(x, y, side);
		});
	}
	fill(body.top, (x, y) => topColor(x, y, 'top'));
	fill(body.bottom, (x, y) => lower(x, 11, 'bottom'));
	// long hair down the back (jacket overlay back)
	const jacket = faces(16, 32, 8, 12, 4);
	const hairRows = look.style === 'long' ? 6 : look.style === 'braids' || look.style === 'ponytail' ? 5 : 0;
	if (hairRows) fill(jacket.back, (x, y) => {
		if (look.style === 'braids' && !(x <= 1 || x >= 6)) return null;
		if (look.style === 'ponytail' && !(x >= 3 && x <= 4)) return null;
		return y < hairRows ? hair(x, y + 3) : null;
	});
	// skirts flare a little: overlay on the lower body
	if (look.bottom === 'skirt' || look.bottom === 'dress') {
		for (const side of ['front', 'back', 'right', 'left']) fill(jacket[side], (x, y) => (y >= 9 ? (y === 11 ? '#ffffff' : lower(x, y, side)) : null));
	}

	// ---------------- arms ----------------
	const aw = slim ? 3 : 4;
	const arm = (u, v) => faces(u, v, aw, 12, 4);
	const sleeve = look.pattern === 'hoodie' || look.pattern === 'knit' || look.style === 'granny' || look.pattern === 'buttons' ? 9 : 4;
	const armFill = (x, y, side) => {
		if (y >= 11) return lv(look.skin, 3);
		if (y < sleeve) return y === sleeve - 1 && sleeve === 4 ? '#ffffff' : topColor(x, y, side);
		return lv(look.skin, 3 + (side === 'back' ? -1 : 0));
	};
	for (const [u, v] of [[40, 16], [32, 48]]) {
		const f = arm(u, v);
		for (const side of ['front', 'back', 'right', 'left']) fill(f[side], (x, y) => armFill(x, y, side));
		fill(f.top, (x, y) => topColor(x, y, 'top'));
		fill(f.bottom, () => lv(look.skin, 3));
	}

	// ---------------- legs ----------------
	const legFill = (x, y, side) => {
		if (y >= 10) return lv(look.shoes, 3 + (y === 10 ? 1 : 0));
		if (look.bottom === 'pants') return lv(look.pants, 3 + (side === 'back' ? -1 : 0));
		if (look.bottom === 'overalls') return y < 6 ? lv(look.overalls, 3) : lv(look.skin, 3);
		if (y < 3) return lower(x, 8 + y, side);
		if (y === 9 && look.bottom !== 'pants') return '#ffffff';
		return lv(look.skin, 3);
	};
	for (const [u, v] of [[0, 16], [16, 48]]) {
		const f = faces(u, v, 4, 12, 4);
		for (const side of ['front', 'back', 'right', 'left']) fill(f[side], (x, y) => legFill(x, y, side));
		fill(f.top, (x, y) => lower(x, 8, 'top'));
		fill(f.bottom, () => lv(look.shoes, 2));
	}
	return t;
}

LOOKS.forEach((look, i) => paint(look, i < 8).save('entity/customer/customer_' + i));
return window.ART.saved.length + ' skins saved';
})()
