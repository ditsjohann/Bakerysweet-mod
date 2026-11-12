(function () {
// Seventh wave, "Cozy Café": cake surfaces, animated chocolate, the oven gauge, 26 pieces of cute decor,
// fancy cake toppers, molds, the bellows, and prettier crates and cupcakes. Runs after bk_art.js.
const ART = window.ART, A = ART.A, T = ART.T, M = ART.M, rnd = ART.rnd;
const B = n => 'block/' + n;
const I = n => 'item/' + n;
const R = {};
const ramp = base => R[base] || (R[base] = ART.ramp(base).map(ART.toHex));
const lv = (base, i) => ramp(base)[Math.max(0, Math.min(5, Math.round(i)))];
const N = o => Object.assign({ neutral: true }, o || {});
const wob = (x, y, s, p) => (rnd(x, y, s) < (p || 0.18) ? (rnd(x, y, s + 3) < 0.5 ? -1 : 1) : 0);
const fs = ART.fs;
const TEX = ART.ROOT + 'textures/';
const anim = (rel, frametime) => fs.writeFileSync(TEX + rel + '.png.mcmeta', JSON.stringify({ animation: { frametime, interpolate: true } }, null, '\t') + '\n');

// ============================================================ cake surfaces (white, tinted in game)
T().fill((x, y) => {
	const r = rnd(x, y, 101);
	if (r < 0.05) return '#ffffff';
	return lv('#f4f1f2', 3 + (r > 0.85 ? -1 : 0) + ((x * 7 + y * 3) % 11 === 0 ? 1 : 0));
}).save(B('cake_frosting'));
// Sponge side: a cream band at the top edge, airy crumb, a jam/cream filling line, golden base.
T().fill((x, y) => {
	if (y <= 10) return lv('#f6f2f2', 3 + wob(x, y, 102, 0.1));
	if (y === 11) return x % 3 === 0 ? '#ffffff' : lv('#f8f4f4', 4);
	if (y === 13) return lv('#fbf6f6', 4 + wob(x, y, 103, 0.2));
	if (y === 15) return lv('#e9dccb', 2);
	const pore = rnd(x, y, 104) < 0.22;
	return pore ? lv('#e8dccb', 2) : lv('#f3eadf', 3 + wob(x, y, 105, 0.15));
}).save(B('cake_sponge'));
T().fill((x, y) => {
	const d = Math.hypot(x - 5, y - 5);
	return lv('#f7f5f6', d < 3 ? 5 : d < 6 ? 4 : 3);
}).save(B('cake_piping'));

// ============================================================ animated chocolate (fountain)
{
	const flow = T(16, 64);
	flow.fill((x, y) => {
		const f = Math.floor(y / 16), ly = y % 16;
		// streaks that slide down a little each frame
		const s = (ly - f * 4 + 64) % 16;
		const streak = (x * 5 + Math.floor(s / 3)) % 7;
		const c = streak === 0 ? lv('#7a4a2e', 4) : streak === 3 ? lv('#7a4a2e', 2) : lv('#7a4a2e', 3);
		return (x === 0 || x === 15) && s % 5 === 0 ? lv('#7a4a2e', 5) : c;
	}).save(B('fountain/chocolate_flow'));
	anim(B('fountain/chocolate_flow'), 4);
	const pool = T(16, 48);
	pool.fill((x, y) => {
		const f = Math.floor(y / 16), ly = y % 16;
		const a = Math.sin((x + f * 2) * 0.7) + Math.sin((ly - f) * 0.6 + x * 0.3);
		if (a > 1.45) return lv('#8a5636', 5);
		if (a > 0.9) return lv('#8a5636', 4);
		if (a < -1.3) return lv('#8a5636', 2);
		return lv('#8a5636', 3);
	}).save(B('fountain/chocolate_pool'));
	anim(B('fountain/chocolate_pool'), 6);
}
// Bubbling jam (tinted): white base with darker swirls, animated.
{
	const jam = T(16, 64);
	jam.fill((x, y) => {
		const f = Math.floor(y / 16), ly = y % 16;
		const b = Math.hypot(((x + f * 3) % 8) - 4, ((ly + f * 2) % 8) - 4);
		if (b < 1.2 && f % 2 === 0) return '#ffffff';
		const a = Math.sin(x * 0.8 + f) + Math.cos(ly * 0.7 - f * 0.6);
		return a > 1.2 ? '#f2f2f2' : a < -1.1 ? '#c8c8c8' : '#dedede';
	}).save(B('jam_surface'));
	anim(B('jam_surface'), 5);
}

// ============================================================ decor materials
// Soft fabrics: smooth, with only a few fluffy highlights; velvet gets gentle diamond tufting with tiny buttons.
const fabric = (name, base, opts) => T().fill((x, y) => {
	opts = opts || {};
	let l = 3 + (rnd(x, y, 110 + base.length) < 0.05 ? 1 : 0);
	if (opts.tuft) {
		const dx = (x + (Math.floor(y / 8) % 2) * 4) % 8, dy = y % 8;
		if (dx === 0 && dy === 0) return lv(base, 2);
		const d = Math.abs(dx - 4) + Math.abs(dy - 4);
		if (d <= 1) l += 1;
		else if (d === 4) l -= 0.5;
	}
	return lv(base, l);
}).save(B(name));
fabric('velvet_pink', '#f5a8c4', { tuft: true });
fabric('velvet_mint', '#9fdcc2', { tuft: true });
fabric('plush_white', '#fbf6f4');
fabric('plush_brown', '#c9936a');
fabric('plush_pink', '#ffc0d3');
const painted = (name, base) => T().fill((x, y) => {
	const plank = Math.floor(y / 4);
	if (y % 4 === 3) return lv(base, 2);
	const grain = (x + plank * 5) % 9 === 0 ? -1 : 0;
	return lv(base, 3 + grain + (y % 4 === 0 ? 1 : 0) + wob(x, y, 120, 0.06));
}).save(B(name));
painted('wood_pink', '#f2b3c8');
painted('wood_mint', '#a7dcc4');
painted('wood_cream', '#f7ead8');
painted('wood_lilac', '#d2bdf0');
T().fill((x, y) => lv('#fbf7f4', 3 + (x + y < 6 ? 1 : 0) + wob(x, y, 121, 0.08))).save(B('ceramic_white'));
T().fill((x, y) => lv('#f7b6cf', 3 + (x + y < 6 ? 1 : 0) + wob(x, y, 122, 0.08))).save(B('ceramic_pink'));
T().fill((x, y) => lv('#f0c45a', 3 + ((x * 3 + y) % 7 === 0 ? 1 : 0) + (y > 12 ? -1 : 0))).save(B('metal_gold'));
T().fill((x, y) => lv('#ee97b5', 3 + ((x * 3 + y) % 7 === 0 ? 1 : 0) + (y > 12 ? -1 : 0))).save(B('metal_pink'));
T().fill((x, y) => lv('#fafafa', 3 + ((x * 3 + y) % 7 === 0 ? 1 : 0) + (y > 12 ? -1 : 0))).save(B('metal_white'));
T().fill((x, y) => lv('#efe6dc', 3 + (y < 5 ? 0.5 : 0) + (rnd(x, y, 124) < 0.05 ? -1 : 0) + (rnd(x, y, 123) < 0.05 ? 1 : 0))).save(B('stone_cream'));

// Floral porcelain: white with tiny pink roses and green leaves.
T().fill((x, y) => {
	const cx = (x + (Math.floor(y / 6) % 2) * 3) % 6, cy = y % 6;
	if (cx >= 1 && cx <= 2 && cy >= 1 && cy <= 2) return cx === 1 && cy === 1 ? '#ff9fbd' : '#f07aa0';
	if (cx === 3 && cy === 2) return '#8fcf8a';
	return lv('#fdfaf8', 3 + (y === 0 ? 1 : 0));
}).save(B('porcelain_floral'));

// Macaron pouf: shell with little "feet", and cream.
T().fill((x, y) => (y >= 13 ? lv('#f7a8c6', 3 + (x % 2 ? 1 : -1) + wob(x, y, 125, 0.2)) : lv('#f7a8c6', 3 + (y < 2 ? 1 : 0) + wob(x, y, 126, 0.08)))).save(B('macaron_shell'));
T().fill((x, y) => lv('#fff3e2', 3 + wob(x, y, 127, 0.15))).save(B('macaron_cream'));

// Heart mirror (cutout): pink gilded frame, pale blue glass with a shine stroke.
{
	const outer = M.heart(8, 8.2, 7.2), inner = M.heart(8, 8.2, 5.4);
	A().add(outer, '#f3a6c1', { shine: 2, profile: 'soft' })
		.add(inner, '#cfe8f5', N({ profile: 'flat', flatLvl: 3, outline: false }))
		.dec(M.ln(5, 5, 7.5, 3.6, 1.1), '#ffffff', N({ lvl: 5 }))
		.dec(M.ln(4.5, 7.5, 5.2, 6.8, 0.9), '#ffffff', N({ lvl: 5 }))
		.save(B('heart_mirror'));
}
// Neon heart sign: a glowing tube heart with a white-hot core, on nothing (cutout).
{
	const ring = (x, y) => M.heart(8, 8.6, 6.6)(x, y) && !M.heart(8, 8.6, 5.0)(x, y);
	const core = (x, y) => M.heart(8, 8.6, 6.0)(x, y) && !M.heart(8, 8.6, 5.5)(x, y);
	T().fill((x, y) => {
		const px = x + 0.5, py = y + 0.5;
		if (core(px, py)) return '#fff2f8';
		if (ring(px, py)) return '#ff6fa8';
		// little sparkle stars
		if ((x === 2 && y === 2) || (x === 13 && y === 3) || (x === 14 && y === 12)) return '#fff6a8';
		return null;
	}).save(B('neon_heart'));
	T().fill((x, y) => lv('#5a4a62', 3 + wob(x, y, 128, 0.1) + ((x === 0 || x === 15 || y === 0 || y === 15) ? -1 : 0))).save(B('neon_back'));
}
// Plush faces
T().fill((x, y) => {
	if ((x === 4 || x === 11) && (y === 7 || y === 8)) return y === 7 && x === 4 || y === 7 && x === 11 ? '#3a2a35' : '#3a2a35';
	if ((x === 4 || x === 11) && y === 6) return '#ffffff';
	if ((x === 7 || x === 8) && y === 9) return '#ff8fb0';
	if ((x === 2 || x === 3 || x === 12 || x === 13) && y === 10) return '#ffc6d6';
	return lv('#fbf6f4', 3 + wob(x, y, 129, 0.1));
}).save(B('bunny_face'));
T().fill((x, y) => {
	if ((x === 4 || x === 11) && (y === 6 || y === 7)) return '#2e2024';
	if (x >= 6 && x <= 9 && y >= 8 && y <= 11) return (x === 7 || x === 8) && y === 8 ? '#3a2a24' : lv('#f2dcc4', 3);
	if ((x === 2 || x === 13) && y === 9) return '#f4a7b9';
	return lv('#c9936a', 3 + wob(x, y, 130, 0.12));
}).save(B('bear_face'));

// Tulips (cross sprite): three tulips in pink, red and yellow on green stems.
{
	const a = A();
	[[4, 3, '#ff8fb3'], [8.5, 1.6, '#ff5a72'], [12, 4, '#ffd36b']].forEach(([x, y, c]) => {
		a.ln(x, y + 2, x + (x < 8 ? 1 : -1) * 0.5, 16, 0.9, '#6fbf5a', N({ outline: false }));
		a.pg([[x - 2, y], [x - 1, y + 3.6], [x + 1, y + 3.6], [x + 2, y], [x + 1, y + 1.3], [x, y - 0.4], [x - 1, y + 1.3]], c, { shine: 1 });
	});
	a.el(3, 12, 1.5, 3, '#7ccc66', { rot: -25 }).el(13, 11.5, 1.5, 3, '#7ccc66', { rot: 25 });
	a.save(B('tulips'));
}
// Window-box flowers (cross sprite): a bushy row of little blooms.
{
	const a = A();
	a.add((x, y) => y > 8 + Math.sin(x * 1.3) * 1.5, '#7ccc66', { profile: 'soft' });
	[[2, 6, '#ff8fb3'], [5, 4, '#ffffff'], [8, 6, '#c8a6f2'], [11, 4, '#ff8fb3'], [14, 6, '#ffd36b'], [3.5, 9, '#ffd36b'], [9.5, 9, '#ff6f91'], [12.5, 9, '#ffffff']]
		.forEach(([x, y, c]) => a.el(x, y, 1.7, 1.6, c, { shine: 1 }).dec(M.pts([[Math.floor(x), Math.floor(y)]]), '#ffe27a', N({ lvl: 4 })));
	a.save(B('box_flowers'));
}
// Trailing ivy for the hanging basket.
{
	const a = A();
	for (let i = 0; i < 4; i++) {
		const x = 2 + i * 4;
		a.add((px, py) => Math.abs(px - (x + Math.sin(py * 0.6 + i) * 1.2)) < 0.6 && py > 1 && py < 10 + i * 1.5, '#6fbf5a', N({ outline: false }));
		for (let k = 0; k < 3; k++) a.el(x + Math.sin(k * 2 + i) * 1.4, 3 + k * 3.5, 1.2, 0.9, '#8fd97a', { rot: 30 });
	}
	[[3, 2, '#ff8fb3'], [8, 1.5, '#ffffff'], [13, 2.5, '#ff8fb3']].forEach(([x, y, c]) => a.el(x, y, 1.6, 1.4, c, { shine: 1 }));
	a.save(B('trailing_flowers'));
}

// Lace curtains (cutout): white lace drapes gathered by pink tie-backs.
T().fill((x, y) => {
	const gather = y >= 8 && y <= 9;
	const left = x < 6 - Math.max(0, y - 9) * 0.6 + (y < 8 ? 2 : 0), right = x > 9 + Math.max(0, y - 9) * 0.6 - (y < 8 ? 2 : 0);
	if (y <= 1) return y === 0 ? lv('#e8b8c8', 3) : '#ffffff';
	if (!(left || right)) return null;
	if (gather && (x === 3 || x === 4 || x === 11 || x === 12)) return '#ff8fb3';
	const hole = (x + y * 2) % 4 === 0 && y % 2 === 0;
	if (hole) return null;
	return (x + y) % 3 === 0 ? '#f6eef2' : '#ffffff';
}).save(B('lace_curtains'));
// Star string lights
{
	const a = A();
	a.add((x, y) => Math.abs(y - (3 + Math.sin((x / 16) * Math.PI) * 4)) < 0.55, '#a89070', N({ outline: false, profile: 'flat' }));
	[[2.5, 6.6], [6, 8.4], [10, 8.6], [13.5, 6.8]].forEach(([x, y], i) => a.star(x, y, 2.2, 1.0, ['#ffe27a', '#fff3b0', '#ffd36b', '#fff3b0'][i], { shine: 1, outline: false }));
	a.save(B('star_lights'));
}
// Bow wallpaper: cream with soft pink stripes and little bows.
T().fill((x, y) => {
	const ox = (Math.floor(y / 8) % 2) * 4, bx = (x + ox) % 8, by = y % 8;
	const bow = (by === 3 && (bx === 2 || bx === 6)) || (by === 2 && (bx === 1 || bx === 7)) || (by === 4 && (bx === 1 || bx === 7)) || (by === 3 && bx === 4) ||
		(by === 3 && (bx === 1 || bx === 3 || bx === 5 || bx === 7)) || (by === 5 && (bx === 3 || bx === 5));
	if (bow) return bx === 4 ? '#e0628c' : '#f78fb3';
	return x % 8 < 1 ? lv('#fbd8e4', 3) : lv('#fff8f2', 3 + wob(x, y, 131, 0.04));
}).save(B('bow_wallpaper'));

// Cash register: pastel pink body, key grid, mint display, gold trim.
T().fill((x, y) => lv('#f4a9c3', 3 + (y < 2 ? 1 : 0) + (y > 13 ? -1 : 0) + wob(x, y, 132, 0.06))).save(B('register_body'));
T().fill((x, y) => {
	if (x % 3 === 0 || y % 3 === 0) return lv('#e48bab', 2);
	const k = (Math.floor(x / 3) + Math.floor(y / 3)) % 4;
	return ['#ffffff', '#fff3c4', '#ffffff', '#d7f2e6'][k];
}).save(B('register_keys'));
T().fill((x, y) => {
	if (x === 0 || x === 15 || y === 0 || y === 15) return lv('#f0c45a', 3);
	if (M.heart(8, 8.2, 3.2)(x + 0.5, y + 0.5)) return '#ff6f91';
	return lv('#bfeedd', 3 + (y < 4 ? 1 : 0));
}).save(B('register_display'));
// Tip jar label + coins
T().fill((x, y) => {
	const heart = M.heart(8, 8, 3.4)(x + 0.5, y + 0.5);
	if (heart) return '#ff6f91';
	return lv('#fff7f0', 3);
}).save(B('tip_label'));
T().fill((x, y) => {
	const cx = (x % 5) - 2, cy = (y % 4) - 1.5;
	if (cx * cx + cy * cy < 3) return rnd(x, y, 133) < 0.5 ? lv('#ffc2d6', 4) : lv('#ff8fb3', 3);
	return lv('#e889ab', 2);
}).save(B('coin_pile'));
T().fill((x, y) => (x === 0 || x === 15 ? '#ffffffb0' : (x === 3 && y > 3 && y < 13) ? '#ffffffd0' : null)).save(B('jar_glass'));

// OPEN sign: cream board, pink pixel letters, hearts in the corners.
{
	const FONT = {
		O: ['XXX', 'X.X', 'X.X', 'X.X', 'XXX'], P: ['XXX', 'X.X', 'XXX', 'X..', 'X..'],
		E: ['XX', 'X.', 'XX', 'X.', 'XX'], N: ['X.X', 'XXX', 'XXX', 'X.X', 'X.X']
	};
	const word = 'OPEN';
	T().fill((x, y) => {
		if (y < 4 || y > 12) return null;
		if (x === 0 || x === 15 || y === 4 || y === 12) return lv('#e48bab', 3);
		const ly = y - 6;
		if (ly >= 0 && ly < 5) {
			let lx = x - 1;
			for (const ch of word) {
				const g = FONT[ch], w = g[0].length;
				if (lx >= 0 && lx < w) { if (g[ly][lx] === 'X') return '#e0407a'; break; }
				lx -= w + 1;
			}
		}
		return lv('#fff6ea', 3 + wob(x, y, 134, 0.05));
	}).save(B('open_sign'));
}
// Chalkboard with a pink frame, a doodled cupcake, hearts and squiggles.
T().fill((x, y) => {
	if (x === 0 || x === 15 || y === 0 || y === 15) return lv('#f2a6c0', 3 + (y === 0 ? 1 : 0));
	const cup = (x >= 5 && x <= 10 && y >= 9 && y <= 12 && (x === 5 || x === 10 || y === 12 || (x + y) % 2 === 0));
	const frost = Math.hypot(x - 7.5, y - 7.5) < 3 && y <= 8 && y >= 5;
	const heart = M.heart(12, 3.6, 1.6)(x + 0.5, y + 0.5) || M.heart(3.5, 4, 1.5)(x + 0.5, y + 0.5);
	if (frost) return y === 5 ? '#ffffff' : '#ffc0d6';
	if (cup) return '#f4f0e6';
	if (heart) return '#ff8fb3';
	if (y === 13 && x >= 3 && x <= 12 && x % 2 === 0) return '#bfe8dc';
	return lv('#3f4a48', 3 + wob(x, y, 135, 0.15));
}).save(B('chalkboard'));
// Heart lantern glass: warm glowing pink heart.
T().fill((x, y) => {
	const h = M.heart(8, 8.4, 6.6)(x + 0.5, y + 0.5);
	if (!h) return lv('#ee97b5', 2);
	const d = Math.hypot(x - 7.5, y - 7);
	return d < 2.5 ? '#fff8e8' : d < 4.5 ? '#ffe2a8' : '#ffc0a8';
}).save(B('heart_lantern'));
// Parasol stripes
T().fill((x, y) => (Math.floor(x / 4) % 2 === 0 ? lv('#f7a8c6', 3 + (y < 2 ? 1 : 0)) : lv('#ffffff', 3 + (y < 2 ? 1 : 0)))).save(B('parasol'));
// Terracotta pot (pastel peach) with a white heart
T().fill((x, y) => {
	if (y < 2) return lv('#f2b29a', 4);
	if (M.heart(8, 8, 2.6)(x + 0.5, y + 0.5)) return '#ffffff';
	return lv('#f2b29a', 3 + wob(x, y, 136, 0.1) + (y > 13 ? -1 : 0));
}).save(B('flower_pot_peach'));
T().fill((x, y) => lv('#7a5a44', 3 + wob(x, y, 137, 0.3))).save(B('soil'));
// Wicker basket
T().fill((x, y) => {
	const w = (x + (y % 2) * 2) % 4 < 2;
	return lv('#e2b97f', 3 + (w ? 1 : -1) + (y % 4 === 0 ? -1 : 0));
}).save(B('wicker'));
// Mailbox
T().fill((x, y) => lv('#f49ab8', 3 + (y < 2 ? 1 : 0) + wob(x, y, 138, 0.06))).save(B('mailbox'));
T().fill((x, y) => {
	if (M.heart(8, 7.6, 3.6)(x + 0.5, y + 0.5)) return '#ffffff';
	return lv('#f49ab8', 3 + wob(x, y, 139, 0.06));
}).save(B('mailbox_front'));
// Welcome mat: coir with a pink scalloped border and a heart.
T().fill((x, y) => {
	if (x === 0 || x === 15 || y === 0 || y === 15) return lv('#f49ab8', 3);
	if (M.heart(8, 8.4, 4)(x + 0.5, y + 0.5)) return lv('#ff7fa6', 3 + (y < 7 ? 1 : 0));
	return lv('#d8b47e', 3 + ((x + y * 2) % 3 === 0 ? -1 : 0) + wob(x, y, 140, 0.2));
}).save(B('welcome_mat'));
// Pastry cart display: a glass front full of pastries.
T().fill((x, y) => {
	if (y < 2 || y > 13 || x === 0 || x === 15) return lv('#f7ead8', 3);
	const row = y < 8;
	const cx = x % 5, cy = (y - (row ? 3 : 9));
	if (cy >= 0 && cy <= 3 && cx >= 1 && cx <= 3) {
		const kind = (Math.floor(x / 5) + (row ? 0 : 1)) % 3;
		return kind === 0 ? (cy === 0 ? '#ffffff' : cy === 1 ? '#ff9fbd' : '#e0a060') : kind === 1 ? (cy < 2 ? '#7a4a2e' : '#d99a5a') : (cy === 0 ? '#ffe27a' : '#f0c080');
	}
	return (x + y) % 7 === 0 ? '#ffffffc0' : '#dff2fa80';
}).save(B('pastry_display'));
// Donut wall: a mint pegboard with donuts hanging on pegs.
T().fill((x, y) => {
	const cx = x % 8 - 3.5, cy = (y % 8) - 3.5;
	const d = Math.hypot(cx, cy);
	if (d < 3.4 && d > 1.2) {
		const k = (Math.floor(x / 8) + Math.floor(y / 8) * 2) % 3;
		const icing = ['#ff9fbd', '#7a4a2e', '#c8a6f2'][k];
		if (cy < 0.6) return rnd(x, y, 141) < 0.15 ? ['#ffffff', '#ffe27a', '#7ec8f2'][Math.floor(rnd(x, y, 142) * 3)] : icing;
		return '#e0a868';
	}
	if (x % 4 === 1 && y % 4 === 1) return lv('#9fdcc2', 1);
	return lv('#b7e6d2', 3 + wob(x, y, 143, 0.05));
}).save(B('donut_wall'));
// Mini cakes for the cake shelf (side view of little layer cakes on a strip).
T().fill((x, y) => {
	const k = Math.floor(x / 4) % 4, lx = x % 4;
	const colors = ['#ff9fbd', '#fff3e2', '#c8a6f2', '#9fdcc2'];
	if (lx === 3) return null;
	if (y < 4) return null;
	if (y === 4) return '#ffffff';
	if (y === 7 || y === 10) return '#fff8f0';
	if (y > 12) return null;
	return lv(colors[k], 3 + (lx === 0 ? 1 : 0));
}).save(B('mini_cakes'));
// Teapot lid knob / cup rim use ceramic textures; tea inside cups:
T().fill((x, y) => lv('#c98a52', 3 + wob(x, y, 144, 0.2))).save(B('tea'));

// ============================================================ crates (cuter): pastel slats with a heart stencil
const crate = (name, slat, stencil) => {
	T().fill((x, y) => {
		if (y === 0 || y === 15 || x === 0 || x === 15) return lv(slat, 2);
		if (y % 5 === 0) return lv(slat, 2);
		if (M.heart(8, 8.2, 3)(x + 0.5, y + 0.5)) return stencil;
		return lv(slat, 3 + ((x + Math.floor(y / 5) * 3) % 7 === 0 ? -1 : 0) + (y % 5 === 1 ? 1 : 0));
	}).save(B(name));
};
crate('crate_strawberry', '#f6dcc0', '#ff6f91');
crate('crate_lemon', '#f8e8b8', '#f2b400');
crate('crate_coconut', '#e8d4bc', '#8a5a3a');
// Fruit piles as seen from above
T().fill((x, y) => {
	const cx = (x + (Math.floor(y / 4) % 2) * 2) % 4 - 1.5, cy = (y % 4) - 1.5;
	if (cx * cx + cy * cy < 2.6) return cy < -0.5 && cx < 0 ? '#ff9fb0' : rnd(x, y, 145) < 0.2 ? '#ffe9a0' : '#ee3d5a';
	return (x + y) % 3 === 0 ? '#5fb84a' : '#c02a46';
}).save(B('pile_strawberry'));
T().fill((x, y) => {
	const cx = (x + (Math.floor(y / 5) % 2) * 2) % 5 - 2, cy = (y % 5) - 2;
	if (cx * cx + cy * cy < 4.2) return cy < -0.8 && cx < 0 ? '#fff7b0' : '#ffd84a';
	return '#d6a800';
}).save(B('pile_lemon'));
T().fill((x, y) => {
	const cx = (x + (Math.floor(y / 6) % 2) * 3) % 6 - 2.5, cy = (y % 6) - 2.5;
	const d = cx * cx + cy * cy;
	if (d < 1.4) return '#fbf6ee';
	if (d < 6.3) return rnd(x, y, 146) < 0.3 ? '#6a4026' : '#8a5a3a';
	return '#5a3420';
}).save(B('pile_coconut'));
// Whole coconut shell (for the 3D coconuts on top): hairy brown with three dark "eyes" on top.
T().fill((x, y) => {
	if ((x === 6 || x === 9) && y === 5 || (x === 7 || x === 8) && y === 8) return '#3a2214';
	return lv('#8a5a3a', 3 + (rnd(x, y, 147) < 0.35 ? (rnd(x, y, 148) < 0.5 ? -1 : 1) : 0));
}).save(B('coconut_shell'));
T().fill((x, y) => lv('#ffd84a', 3 + (x < 6 && y < 6 ? 1 : 0) + wob(x, y, 149, 0.15))).save(B('lemon_skin'));

// ============================================================ cupcakes (placed): swirly frosting, striped liner
T().fill((x, y) => {
	const stripe = x % 3 === 0;
	return lv(stripe ? '#e48bab' : '#f8c3d6', 3 + (y < 6 ? 1 : 0) + (y === 15 ? -1 : 0));
}).save(B('cupcake_wrapper'));
T().fill((x, y) => {
	// a spiral swirl: slightly lighter ribbons
	const a = Math.atan2(y - 7.5, x - 7.5), r = Math.hypot(x - 7.5, y - 7.5);
	const band = Math.floor((r + a * 1.6 + 20) / 1.8) % 2 === 0;
	return lv('#f6f2f2', 3 + (band ? 1 : 0) + (rnd(x, y, 150) < 0.06 ? 1 : 0));
}).save(B('frosting'));
T().fill((x, y) => {
	const r = rnd(x, y, 151);
	if (r < 0.12) return ['#ff6f91', '#7ec8f2', '#ffe27a', '#a8e6cf', '#c8a6f2'][Math.floor(rnd(x, y, 152) * 5)];
	return null;
}).save(B('sprinkle_overlay'));

// ============================================================ items: bellows, molds
A().pg([[3, 4], [11, 2], [13, 8], [11, 14], [3, 12]], '#c9936a', { profile: 'soft', grain: 0.1 })
	.dec(M.and(M.pg([[3, 4], [11, 2], [13, 8], [11, 14], [3, 12]]), (x, y) => Math.floor(x) % 3 === 0), '#9a6a48', { shift: -1 })
	.heart(7.4, 8.2, 2.2, '#ff8fb3', { outline: false })
	.ln(13, 8, 15.5, 8, 1.4, '#f0c45a', { shine: 1 })
	.ln(1, 8, 3, 8, 1.6, '#f2b3c8')
	.save(I('bellows'));
A().el(8, 8.5, 6.5, 4.5, '#d8dde4', N({ shine: 2 })).el(8, 8.5, 4.6, 2.8, '#b8c0ca', N({ profile: 'flat' }))
	.save(I('round_cake_mold'));
A().rc(2, 4, 13, 12, '#d8dde4', N({ shine: 2 })).rc(4, 6, 11, 10, '#b8c0ca', N({ profile: 'flat' }))
	.save(I('square_cake_mold'));
A().heart(8, 8.5, 6.4, '#f4b8cc', { shine: 2 }).heart(8, 8.5, 4.4, '#e48bab', { profile: 'flat' })
	.save(I('heart_cake_mold'));

// ============================================================ topper icons (and textures for their 3D models)
A().heart(8, 8.4, 5.6, '#ff7fa6', { shine: 2 }).save(I('fondant_heart'));
A().star(8, 8.6, 7, 3.2, '#ffd84a', { shine: 2 }).save(I('fondant_star'));
A().el(8, 10.5, 4.2, 3.8, '#fbf6f4', { shine: 1 }).el(8, 6, 3.2, 2.9, '#fbf6f4')
	.el(6.4, 2.6, 1, 2.6, '#fbf6f4', { rot: -10 }).el(9.6, 2.6, 1, 2.6, '#fbf6f4', { rot: 10 })
	.dots([[7, 6], [9, 6]], '#3a2a35', { lvl: 0 }).dots([[8, 7]], '#ff8fb0', { lvl: 3 })
	.save(I('fondant_bunny'));
{
	const a = A();
	for (let i = 0; i < 5; i++) {
		const ang = -Math.PI / 2 + i * Math.PI * 2 / 5;
		a.el(8 + Math.cos(ang) * 3.4, 8 + Math.sin(ang) * 3.4, 2.8, 2.8, '#ffb3cc', { shine: i === 0 ? 1 : 0 });
	}
	a.el(8, 8, 2, 2, '#ffe27a', { shine: 1 }).save(I('sugar_flower'));
}
A().el(8, 10.4, 6, 2.4, '#c8a6f2', { shine: 1 }).rc(3, 7, 12, 8, '#fff3e2', N({ profile: 'flat' })).el(8, 5.6, 6, 2.4, '#c8a6f2', { shine: 2 })
	.save(I('mini_macaron'));
A().el(5, 6, 1.8, 1.8, '#ffffff', N({ shine: 1 })).el(10, 5, 1.8, 1.8, '#ffffff', N({ shine: 1 })).el(7.5, 10, 1.8, 1.8, '#fff0f6', N({ shine: 1 }))
	.el(12, 10.5, 1.8, 1.8, '#ffffff', N({ shine: 1 })).el(4, 12, 1.5, 1.5, '#fff0f6', N({ shine: 1 }))
	.save(I('sugar_pearls'));
A().add((x, y) => Math.abs(Math.sin((x - 2) * 0.9) * 3 - (y - 8)) < 1.6 && x > 2 && x < 14, '#7a4a2e', { shine: 1 })
	.save(I('chocolate_curl'));
A().el(5, 6.5, 3.6, 4.2, '#9fd8f2', { rot: -20, shine: 1 }).el(11, 6.5, 3.6, 4.2, '#9fd8f2', { rot: 20, shine: 1 })
	.el(5.8, 11.5, 2.4, 2.6, '#c8a6f2', { rot: 20 }).el(10.2, 11.5, 2.4, 2.6, '#c8a6f2', { rot: -20 })
	.ln(8, 4, 8, 13, 1.3, '#5a4a62')
	.save(I('sugar_butterfly'));
A().pg([[2, 13], [2, 6], [5, 9], [8, 4], [11, 9], [14, 6], [14, 13]], '#ffd84a', { shine: 2 })
	.dots([[8, 10], [4, 11], [12, 11]], '#ff6f91', { lvl: 4 })
	.save(I('golden_crown'));
A().ln(3, 15, 3, 2, 1, '#e9d9cf').pg([[3.5, 2.5], [14, 4.5], [3.5, 8.5]], '#ff8fb3', { shine: 1 })
	.dots([[6, 5], [9, 5]], '#ffffff', { lvl: 5 })
	.save(I('cake_flag'));

return window.ART.saved.length + ' textures saved';
})()
