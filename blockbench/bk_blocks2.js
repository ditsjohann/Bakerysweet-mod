(function () {
const ART = window.ART, A = ART.A, T = ART.T, M = ART.M, rnd = ART.rnd;
const B = n => 'block/' + n;
const R = {};
const ramp = base => R[base] || (R[base] = ART.ramp(base).map(ART.toHex));
const rampN = base => R['n' + base] || (R['n' + base] = ART.ramp(base, true).map(ART.toHex));
const lv = (base, i) => ramp(base)[Math.max(0, Math.min(5, i))];
const lvN = (base, i) => rampN(base)[Math.max(0, Math.min(5, i))];
const wob = (x, y, s, p) => (rnd(x, y, s) < (p || 0.18) ? (rnd(x, y, s + 3) < 0.5 ? -1 : 1) : 0);

// ---------- generic tiling textures ----------
function bricks(w, h, colors, mortar, bw, bh, seed) {
	return T(w, h).fill((x, y) => {
		const row = Math.floor(y / bh), off = (row % 2) * (bw / 2);
		const bx = ((x + off) % bw + bw) % bw, by = y % bh;
		if (by === bh - 1 || bx === bw - 1) return lv(mortar, by === bh - 1 && bx !== bw - 1 ? 2 : 3 + wob(x, y, seed, 0.2));
		const id = row * 31 + Math.floor((x + off) / bw);
		const base = colors[Math.floor(rnd(id, 7, seed) * colors.length)];
		let l = 3;
		if (by === 0 || bx === 0) l = 4;
		else if (by === bh - 2 || bx === bw - 2) l = 2;
		l += wob(x, y, seed + id, 0.14);
		return lv(base, l);
	});
}
function planks(w, h, base, seed, boardH) {
	boardH = boardH || 4;
	return T(w, h).fill((x, y) => {
		const board = Math.floor(y / boardH), by = y % boardH;
		const cut = Math.floor(rnd(board, 3, seed) * w);
		if (by === boardH - 1) return lv(base, 1);
		if (x === cut) return lv(base, 2);
		let l = by === 0 ? 4 : 3;
		if (rnd(Math.floor(x / 3), y, seed) < 0.25) l -= 1;
		if (rnd(x, y, seed + 5) < 0.06) l -= 1;
		return lv(base, l);
	});
}
function noise(base, amt, seed, neutral) {
	const f = neutral ? lvN : lv;
	return T().fill((x, y) => f(base, 3 + wob(x, y, seed, amt)));
}
function tiles(name, a, b) {
	T().fill((x, y) => {
		const cx = Math.floor(x / 8), cy = Math.floor(y / 8), lx = x % 8, ly = y % 8;
		const base = (cx + cy) % 2 ? a : b;
		if (lx === 7 || ly === 7) return lv('#e9e1d8', 2);
		let l = 3;
		if (lx === 0 || ly === 0) l = 4;
		if (lx === 1 && ly === 1) l = 5;
		if (lx === 6 || ly === 6) l = 2;
		return lv(base, l);
	}).save(B(name));
}

// ================= Grand Brick Oven (64px tiles, 1 texel per model unit) =================
bricks(64, 64, ['#b9583f', '#a94e38', '#c4684b', '#b05a43', '#9f4a37'], '#e2cfb2', 8, 4, 1).save(B('oven/bricks'));
T(64, 64).fill((x, y) => {
	const bx = x % 16, by = y % 8, row = Math.floor(y / 8), off = (row % 2) * 8;
	const sx = (x + off) % 16;
	if (by === 7 || sx === 15) return lv('#cdbb9c', 2);
	let l = 3 + wob(x, y, 4, 0.12);
	if (by === 0 || sx === 0) l = 4;
	if (by === 6 || sx === 14) l = 2;
	return lv('#efe3cc', l);
}).save(B('oven/stone'));
T(64, 64).fill((x, y) => lv('#3a2a26', 3 + wob(x, y, 8, 0.3) - (rnd(Math.floor(x / 4), Math.floor(y / 4), 2) < 0.2 ? 1 : 0))).save(B('oven/soot'));
T(64, 64).fill((x, y) => {
	const ember = rnd(x, y, 11);
	if (y > 44) return ember < 0.35 ? lv('#ffb347', 4) : ember < 0.6 ? lv('#ff7a2e', 3) : lv('#5a2a1c', 3);
	const glow = Math.max(0, (y - 20) / 44);
	return lv(glow > 0.5 ? '#7a3a22' : '#3a2a26', 3 + wob(x, y, 8, 0.3));
}).save(B('oven/inside_lit'));
T(64, 64).fill((x, y) => lv('#3a2a26', 3 + wob(x, y, 8, 0.3))).save(B('oven/inside'));
A().rc(1, 1, 14, 14, '#f7efe2', { profile: 'flat' }).heart(8, 8.4, 5.2, '#f27fa6', { shine: 2 }).save(B('oven/heart_plaque'));
T().fill((x, y) => {
	const d = Math.hypot(x - 7.5, y - 7.5);
	if (d > 7.5) return lv('#6b4428', 3);
	return Math.floor(d) % 3 === 0 ? lv('#c99a63', 2) : lv('#e2b57d', 3 + wob(x, y, 3, 0.1));
}).save(B('oven/log_end'));
T().fill((x, y) => lv('#7a5234', 3 + (x % 4 === 0 ? -1 : 0) + wob(x, y, 5, 0.2))).save(B('oven/bark'));
T().fill((x, y) => lv('#d9b07a', 3 + (y % 5 === 0 ? -1 : 0) + wob(x, y, 6, 0.1))).save(B('oven/peel'));

// Oven Heart block (16px)
bricks(16, 16, ['#b9583f', '#a94e38', '#c4684b', '#b05a43'], '#e2cfb2', 8, 4, 2).save(B('oven_bricks'));
(() => {
	const t = bricks(16, 16, ['#b9583f', '#a94e38', '#c4684b', '#b05a43'], '#e2cfb2', 8, 4, 3);
	const arch = (x, y) => (y >= 9 && x >= 3 && x <= 12) || (((x + 0.5 - 8) / 5) ** 2 + ((y + 0.5 - 9.5) / 3.4) ** 2 <= 1);
	t.fill((x, y) => {
		if (arch(x, y)) return y >= 14 ? lv('#5a4a44', 3) : lv('#2c1d1a', 3 + wob(x, y, 4, 0.2));
		const rim = [[0, 1], [0, -1], [1, 0], [-1, 0]].some(([dx, dy]) => arch(x + dx, y + dy));
		if (rim) return lv('#efe3cc', y < 9 ? 4 : 3);
		return undefined;
	});
	const heart = [[7, 2], [8, 2], [6, 3], [7, 3], [8, 3], [9, 3], [7, 4], [8, 4]];
	heart.forEach(([x, y]) => t.set(x, y, lv('#f27fa6', y === 2 ? 4 : 3)));
	t.save(B('oven_heart_front'));
})();

// ================= Kitchen =================
T().fill((x, y) => {
	if (y === 9 || y === 10) return lv('#f49ac0', y === 9 ? 4 : 3);
	if (y >= 11 && y <= 12 && (x + (y === 12 ? 2 : 0)) % 4 === 1) return lv('#f49ac0', 3);
	return lv('#fbf5ee', 3 + (x === 0 ? 1 : 0) + wob(x, y, 2, 0.06));
}).save(B('mixing_bowl'));
noise('#ece2d6', 0.1, 3).save(B('mixing_bowl_inner'));
noise('#f3d9a8', 0.15, 4).fill((x, y) => (rnd(x, y, 7) < 0.05 ? lv('#f3d9a8', 5) : undefined)).save(B('batter'));
planks(16, 16, '#d9ab72', 2).fill((x, y) => (rnd(x, y, 9) < 0.04 ? '#f8f0e0' : undefined)).save(B('pastry_board'));
noise('#f1d7a6', 0.15, 5).fill((x, y) => (rnd(x, y, 8) < 0.05 ? '#fff6e4' : undefined)).save(B('dough'));

// ================= Food surfaces =================
T().fill((x, y) => lv('#c87a34', 3 + wob(x, y, 1, 0.3) + (rnd(Math.floor(x / 2), Math.floor(y / 2), 4) < 0.15 ? 1 : 0))).fill((x, y) => {
	const d = (x + y * 0.6) % 5.3;
	return d < 0.9 && y > 2 && y < 14 ? lv('#f2c27a', 3) : undefined;
}).save(B('bread_crust'));
noise('#a3622c', 0.25, 2).save(B('bread_bottom'));
noise('#f6e4c0', 0.15, 3).fill((x, y) => (rnd(x, y, 5) < 0.08 ? lv('#d9bf8f', 3) : undefined)).save(B('bread_crumb'));
T().fill((x, y) => lv('#d8964a', 3 + wob(x, y, 7, 0.3))).fill((x, y) => ((x + 16 - y * 0.5) % 6 < 1 && y > 3 && y < 13 ? lv('#f6c983', 3) : undefined)).save(B('baguette_crust'));
T().fill((x, y) => ((x + y) % 5 < 2 ? lv('#e9a24a', 3 + wob(x, y, 2, 0.15)) : lv('#cf8436', 3 + wob(x, y, 3, 0.15)))).save(B('croissant'));
T().fill((x, y) => lv('#f49ac0', x % 2 ? 2 : 4)).save(B('cupcake_wrapper'));
noise('#e8b070', 0.2, 6).save(B('sponge'));
T().fill((x, y) => lvN('#f2f2f2', 3 + (((x + y * 2) % 7) === 0 ? 1 : 0) + wob(x, y, 4, 0.08))).save(B('frosting'));
noise('#d81f3c', 0.1, 7).fill((x, y) => (x === 4 && y === 4 ? '#ffd0d8' : undefined)).save(B('cherry'));
noise('#d6964f', 0.2, 8).save(B('donut_dough'));
T().fill((x, y) => lvN('#f4f4f4', 3 + (x === 3 && y === 3 ? 2 : 0) + wob(x, y, 5, 0.06))).save(B('glaze'));
T().fill((x, y) => (rnd(x, y, 21) < 0.13 ? ['#ff6b8a', '#ffd84a', '#6bc6ff', '#8de07a', '#c69cff', '#ffffff'][Math.floor(rnd(x, y, 5) * 6)] : null)).save(B('sprinkle_overlay'));
noise('#d9a35c', 0.25, 9).fill((x, y) => (rnd(x, y, 3) < 0.06 ? lv('#d9a35c', 1) : undefined)).save(B('cookie'));
noise('#efefef', 0.06, 10, true).save(B('chip'));
T().fill((x, y) => {
	const d = Math.hypot(x - 7.5, y - 7.5);
	if (d > 5.5 && d <= 6.5) return lv('#a6c8ec', 3);
	return lv('#f6f8fb', d < 4 ? 3 : 4);
}).save(B('plate'));
T().fill((x, y) => (y >= 13 ? lvN('#e6e6e6', (x % 2) ? 2 : 3) : lvN('#f4f4f4', 3 + wob(x, y, 9, 0.06)))).save(B('macaron_shell'));
noise('#fff1dc', 0.08, 11).save(B('macaron_filling'));
T().fill((x, y) => lv('#d79d55', 3 + wob(x, y, 12, 0.25) + ((x + y) % 4 === 0 ? 1 : 0))).save(B('pie_crust'));
T().fill((x, y) => lvN('#f0f0f0', 3 + wob(x, y, 13, 0.15) - (rnd(x, y, 2) < 0.06 ? 2 : 0))).save(B('pie_filling'));
T().fill((x, y) => (y >= 13 ? lv('#c88a42', 3 + wob(x, y, 2, 0.2)) : lv('#f1dfc4', 3 + wob(x, y, 3, 0.1)))).save(B('pie_inside'));
noise('#c9cdd8', 0.1, 14).fill((x, y) => (y === 0 ? lv('#c9cdd8', 5) : undefined)).save(B('pie_tin'));
T().fill((x, y) => {
	if (y === 10 || y === 13) return lv('#fff6ea', 4);
	return lv('#f2cf94', 3 + wob(x, y, 15, 0.15));
}).save(B('cake_side'));
T().fill((x, y) => {
	if (y === 10 || y === 13) return lv('#fff6ea', 4);
	return lv('#efc887', 3 + wob(x, y, 16, 0.25) - (rnd(x, y, 4) < 0.05 ? 1 : 0));
}).save(B('cake_inside'));
T().fill((x, y) => lvN('#f4f4f4', 3 + wob(x, y, 17, 0.07) + ((x * 3 + y) % 11 === 0 ? 1 : 0))).save(B('cake_frosting'));
noise('#e9c98f', 0.15, 18).save(B('cake_bottom'));
T().fill((x, y) => {
	const d = Math.hypot(x - 7.5, y - 7.5);
	return d > 6.6 ? lv('#f7b6cf', 3) : lv('#fdf8f4', 3 + wob(x, y, 2, 0.05));
}).save(B('cake_board'));

// toppings
noise('#e8324e', 0.12, 20).fill((x, y) => ((x * 5 + y * 3) % 7 === 0 ? '#ffe7a0' : undefined)).save(B('topping_strawberry'));
noise('#5cbf4e', 0.15, 21).save(B('leaf'));
noise('#4d63c9', 0.12, 22).fill((x, y) => ((x + y * 4) % 9 === 0 ? lv('#4d63c9', 5) : undefined)).save(B('topping_blueberry'));
noise('#e0405e', 0.15, 23).fill((x, y) => ((x % 3 === 0 && y % 3 === 0) ? lv('#e0405e', 5) : undefined)).save(B('topping_raspberry'));
noise('#fffaf3', 0.06, 24).save(B('topping_cream'));
T().fill((x, y) => {
	for (let k = 0; k < 3; k++) {
		const yo = 3 + k * 5;
		if (x >= 1 && x <= 14 && Math.abs(y - (yo + Math.sin(x * 0.8 + k) * 1.6)) < 0.7) return lv('#5a3220', 3);
	}
	return null;
}).save(B('topping_chocolate'));
noise('#5a3220', 0.15, 25).save(B('chocolate'));
T().fill((x, y) => (x >= 1 && x <= 14 && y >= 1 && y <= 14 && rnd(Math.floor(x / 2), Math.floor(y / 2), 31) < 0.28 ? lv(rnd(x, y, 2) < 0.5 ? '#f27a9a' : '#f591ad', 3 + wob(x, y, 4, 0.2)) : null)).save(B('topping_rose'));
noise('#ec5a86', 0.15, 26).save(B('petal'));
noise('#9b7fd6', 0.2, 27).save(B('topping_lavender'));
noise('#ffd84a', 0.12, 28).fill((x, y) => ((x * 7 + y) % 13 === 0 ? '#ffffff' : undefined)).save(B('topping_star'));
T().fill((x, y) => (x >= 1 && x <= 14 && y >= 1 && y <= 14 && rnd(x, y, 41) < 0.3 ? lv('#fbf6ec', 3 + wob(x, y, 2, 0.3)) : null)).save(B('topping_coconut'));
noise('#62d090', 0.15, 29).save(B('topping_mint'));
T().fill((x, y) => {
	const d = Math.hypot(x - 7.5, y - 7.5);
	if (d > 7.6) return null;
	if (d > 6.6) return lv('#ffd93d', 3);
	if (d > 5.8) return lv('#fff6c8', 4);
	const ang = Math.atan2(y - 7.5, x - 7.5);
	return Math.abs(Math.sin(ang * 4)) < 0.15 ? lv('#fff6c8', 4) : lv('#fff07a', 3 + wob(x, y, 3, 0.1));
}).save(B('topping_lemon'));

// mug + drinks
T().fill((x, y) => lv('#f8f1ea', 3 + (x === 0 ? 1 : 0) + wob(x, y, 30, 0.05))).fill((x, y) => {
	const u = (x - 7.5) / 2.2, v = -(y - 11) / 2.2 + 0.25, q = u * u + v * v - 1;
	return q * q * q - u * u * v * v * v <= 0 ? lv('#f27fa6', 3) : undefined;
}).save(B('mug'));
noise('#d6ccc2', 0.08, 31).save(B('mug_inner'));
T().fill((x, y) => lvN('#ececec', 3 + wob(x, y, 32, 0.08) + (Math.abs(Math.hypot(x - 7.5, y - 7.5) - 3) < 0.6 ? 1 : 0))).save(B('liquid'));

// ================= Shop & decor =================
planks(16, 16, '#f2d9c0', 40).save(B('counter_wood'));
T().fill((x, y) => lv('#fbf4ee', 3 + wob(x, y, 41, 0.1) + ((Math.sin(x * 0.7 + y * 0.3) > 0.92) ? -1 : 0))).save(B('marble'));
T().fill((x, y) => {
	if (x === 0 || y === 0 || x === 15 || y === 15) return lv('#e88aae', 3);
	if (x === 1 || y === 1 || x === 14 || y === 14) return lv('#fbe1ea', 4);
	return lv('#f7c4d8', 3 + wob(x, y, 42, 0.06));
}).fill((x, y) => {
	const u = (x - 7.5) / 3, v = -(y - 8) / 3 + 0.25, q = u * u + v * v - 1;
	return q * q * q - u * u * v * v * v <= 0 ? lv('#ffffff', 3) : undefined;
}).save(B('counter_panel'));
T().fill((x, y) => lv('#f2b8cc', 3 + (y < 2 ? 1 : 0) + wob(x, y, 43, 0.06))).save(B('register'));
T().fill((x, y) => lv('#e8c46a', 3 + (y === 0 ? 2 : 0) + wob(x, y, 44, 0.1))).save(B('brass'));
T().fill((x, y) => lv('#93e0b4', 3 + ((x + y) % 4 === 0 ? 1 : 0))).save(B('sign_open'));
T().fill((x, y) => lv('#b8aeb2', 3 + ((x + y) % 4 === 0 ? 1 : 0))).save(B('sign_closed'));
T().fill((x, y) => {
	if (x === 0 || y === 0 || x === 15 || y === 15) return lv('#ffffff', 4);
	return (x + y) % 9 === 0 || (x + y) % 9 === 1 ? '#ffffff70' : '#e8f6fb2a';
}).save(B('case_glass'));
planks(16, 16, '#f7d9e3', 45).save(B('case_wood'));
T().fill((x, y) => {
	const a = Math.floor(x / 2) % 2, b = Math.floor(y / 2) % 2;
	return a && b ? lv('#f27fa6', 3) : a || b ? lv('#f9c3d6', 3) : lv('#fff7f2', 3);
}).save(B('tablecloth'));
planks(16, 16, '#c99263', 46).save(B('table_wood'));
noise('#f49ac0', 0.1, 47).fill((x, y) => ((x % 4 === 2 && y % 4 === 2) ? lv('#f49ac0', 5) : undefined)).save(B('cushion'));
T().fill((x, y) => lv('#eadcbf', 3 + ((x + y) % 2 ? 0 : -1 * (rnd(x, y, 3) < 0.3 ? 1 : 0)) + wob(x, y, 48, 0.1))).fill((x, y) => {
	if (y >= 6 && y <= 11 && x >= 5 && x <= 10) {
		if (x === 7 || x === 8) return lv('#d9a94a', 3);
		if ((y + x) % 3 === 0) return lv('#e8bf62', 3);
	}
	return undefined;
}).save(B('sack'));
T().fill((x, y) => ((x + (y % 4 < 2 ? 0 : 2)) % 4 < 2 ? lv('#c99a5c', 3 + wob(x, y, 49, 0.15)) : lv('#a8783f', 3 + wob(x, y, 50, 0.15)))).save(B('wicker'));
T().fill((x, y) => lv('#2f3d39', 3 + wob(x, y, 51, 0.15))).fill((x, y) => {
	if ((y === 3 && x > 2 && x < 13) || (y === 6 && x > 2 && x < 10) || (y === 8 && x > 2 && x < 12) || (y === 10 && x > 2 && x < 9)) return rnd(x, y, 4) < 0.85 ? '#e9efe9' : undefined;
	if ((y === 12 || y === 13) && (x === 11 || x === 12)) return '#f7b6cf';
	return undefined;
}).save(B('chalkboard'));
T().fill((x, y) => {
	if (y === 1) return lv('#c9a06b', 3);
	const flag = Math.floor(x / 4), fx = x % 4;
	const colors = ['#f7b6cf', '#a8e6cf', '#fff1c1', '#c6b0f0'];
	if (y >= 2 && fx <= 3 && (y - 2) <= (fx < 2 ? fx * 3 : (3 - fx) * 3) + 1 && y < 9) return lv(colors[flag % 4], y === 2 ? 4 : 3);
	return null;
}).save(B('bunting'));
noise('#cfe8f0', 0.06, 52).save(B('jar_glass'));
T().fill((x, y) => {
	if (x === 0 || x === 15 || y === 0 || y === 15) return lv('#a8783f', 2);
	if (y % 5 === 0) return lv('#a8783f', 2);
	return lv('#c99a5c', 3 + wob(x, y, 53, 0.15));
}).save(B('crate'));
function pile(name, base, extra) {
	const a = A();
	const spots = [[3, 3], [8, 2.5], [13, 3.5], [5.5, 7], [10.5, 7.5], [3, 11], [8, 11.5], [13, 11], [6, 14.5], [11, 14.5]];
	spots.forEach(([x, y]) => a.el(x, y, 2.6, 2.4, base, { shine: 1, outline: false }));
	if (extra) extra(a);
	a.save(B(name));
}
pile('pile_strawberry', '#e8324e', a => a.dots([[3, 2], [8, 1], [13, 2], [5, 6], [10, 6]], '#5cbf4e', { lvl: 3 }));
pile('pile_lemon', '#ffd93d');
pile('pile_coconut', '#7a4e2e');
tiles('pink_tiles', '#f7b6cf', '#fff3f6');
tiles('mint_tiles', '#a8e6cf', '#f3fff9');
tiles('lavender_tiles', '#cdb8f0', '#f8f4ff');
tiles('cream_tiles', '#f6e3bd', '#fffaf0');

// ================= Fruit tree wood =================
function woodSet(name, bark, plank, leafCol, leafCol2) {
	T().fill((x, y) => {
		const seg = (y + (x % 4 < 2 ? 0 : 2)) % 4;
		return lv(bark, (seg === 0 ? 2 : 3) + (x % 4 === 0 ? -1 : 0) + wob(x, y, 60, 0.15));
	}).save(B(name + '_log'));
	T().fill((x, y) => {
		const d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
		if (d > 6.5) return lv(bark, 3 + wob(x, y, 61, 0.2));
		return Math.round(d) % 2 === 0 ? lv(plank, 2) : lv(plank, 3 + wob(x, y, 62, 0.1));
	}).save(B(name + '_log_top'));
	T().fill((x, y) => lv(plank, 3 + (x % 5 === 0 ? -1 : 0) + wob(x, y, 63, 0.12))).save(B('stripped_' + name + '_log'));
	T().fill((x, y) => {
		const d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
		return Math.round(d) % 2 === 0 ? lv(plank, 2) : lv(plank, 3 + wob(x, y, 64, 0.1));
	}).save(B('stripped_' + name + '_log_top'));
	planks(16, 16, plank, 65).save(B(name + '_planks'));
	const door = top => T().fill((x, y) => {
		if (x === 0 || x === 15 || (top && y === 0) || (!top && y === 15)) return lv(plank, 1);
		if (top && x >= 3 && x <= 12 && y >= 3 && y <= 12) {
			if (x === 7 || x === 8 || y === 7 || y === 8) return lv(plank, 2);
			return '#d8eef6cc';
		}
		if (!top && x >= 3 && x <= 12 && y >= 3 && y <= 12) return lv(plank, (x === 3 || y === 3) ? 2 : (x === 12 || y === 12) ? 4 : 3);
		if (!top && x === 12 && y === 1) return lv('#e8c46a', 4);
		return lv(plank, 3 + (x % 5 === 0 ? -1 : 0) + wob(x, y, 66, 0.08));
	});
	door(true).save(B(name + '_door_top'));
	door(false).save(B(name + '_door_bottom'));
	T().fill((x, y) => {
		if (x === 0 || x === 15 || y === 0 || y === 15) return lv(plank, 1);
		if ((x >= 2 && x <= 6 || x >= 9 && x <= 13) && (y >= 2 && y <= 6 || y >= 9 && y <= 13)) return null;
		return lv(plank, 3 + wob(x, y, 67, 0.1));
	}).save(B(name + '_trapdoor'));
	T().fill((x, y) => {
		const r = rnd(x, y, 70);
		if (r < 0.12) return null;
		const l = 3 + (rnd(Math.floor(x / 2), Math.floor(y / 2), 71) < 0.3 ? 1 : 0) - (rnd(x, y, 72) < 0.25 ? 1 : 0);
		return lv(rnd(Math.floor(x / 3), Math.floor(y / 3), 73) < 0.5 ? leafCol : leafCol2, l);
	}).save(B(name + '_leaves'));
	const item = A().rc(3, 1, 12, 14, plank, { profile: 'flat' }).rc(5, 3, 10, 6, '#d8eef6', { profile: 'flat', outline: false }).dots([[10, 9]], '#e8c46a', { lvl: 4 });
	item.save('item/' + name + '_door');
}
woodSet('palm', '#8a6a4a', '#e8cfa6', '#4fae47', '#6cc35a');
woodSet('citrus', '#7a5a42', '#f3e2a2', '#3f9a42', '#58b04e');
(() => {
	const a = A();
	a.ln(8, 15.5, 8.6, 7, 1.4, '#8a6a4a');
	[[-60, 0], [-20, 1], [20, 2], [60, 3]].forEach(([ang, k]) => a.el(8.6 + Math.sin(ang * Math.PI / 180) * 3.6, 6.2 - Math.cos(ang * Math.PI / 180) * 1.4, 4, 1.2, k % 2 ? '#4fae47' : '#6cc35a', { rot: ang, outline: false }));
	a.el(8.4, 9.2, 1.4, 1.4, '#6b4a2e', { outline: false });
	a.save(B('palm_sapling'));
})();
(() => {
	const a = A();
	a.ln(8, 15.5, 8, 6, 1.2, '#7a5a42');
	[[5, 6], [11, 5], [8, 3], [6, 9.5], [10.5, 9]].forEach(([x, y], i) => a.el(x, y, 2.4, 1.6, i % 2 ? '#3f9a42' : '#58b04e', { rot: i * 40, outline: false }));
	a.el(10.4, 11.4, 1.3, 1.1, '#ffd93d', { outline: false, shine: 1 });
	a.save(B('citrus_sapling'));
})();
noise('#7a4e2e', 0.3, 80).save(B('coconut_shell'));
noise('#7fae4a', 0.2, 81).save(B('coconut_young'));
noise('#ffd93d', 0.12, 82).fill((x, y) => (rnd(x, y, 3) < 0.05 ? lv('#ffd93d', 5) : undefined)).save(B('lemon_peel'));
noise('#9ccc4a', 0.12, 83).save(B('lemon_young'));
noise('#6b5a3a', 0.2, 84).save(B('fruit_stem'));

// ================= Crops (plane textures + fruit cube textures) =================
const STEM = '#4f8f3a', LEAF = '#5cae46', LEAF2 = '#72c457';
const plant = (name, draw) => { const a = A(); draw(a); a.save(B(name)); };
const sprout = (a, c) => { a.ln(7, 16, 7, 12.5, 1, STEM, { outline: false }).ln(10, 16, 10, 13.5, 1, STEM, { outline: false }); a.el(6, 12, 1.6, 0.9, c || LEAF2, { rot: -30, outline: false }).el(8.4, 12, 1.6, 0.9, c || LEAF, { rot: 30, outline: false }).el(11, 13, 1.4, 0.8, c || LEAF2, { rot: 30, outline: false }); };
// Airy plants: a few stems with pairs of pointed leaves along them, so the shape reads as a plant, not a hedge.
function bush(a, h, cols, seed) {
	const stems = [[3, -0.8], [8, 0.2], [13, 0.8]];
	stems.forEach(([x, lean], i) => {
		const top = 16 - h + Math.floor(rnd(i, h, seed) * 3);
		const tx = x + lean * 2;
		a.ln(x, 16, tx, top, 1, STEM, { outline: false });
		for (let y = 14 - (i % 2) * 1.5; y > top + 1; y -= 3.4) {
			const t = (16 - y) / (16 - top), sx = x + (tx - x) * t;
			const col = cols[Math.floor(rnd(i, Math.round(y), seed) * cols.length)];
			a.el(sx - 1.5, y - 0.4, 1.6, 0.75, col, { rot: -30, contrast: 5, outline: false });
			a.el(sx + 1.5, y - 1.2, 1.6, 0.75, col, { rot: 30, contrast: 5, outline: false });
		}
		a.el(tx, top + 0.3, 1, 1.4, cols[i % cols.length], { contrast: 5, outline: false });
	});
}
function crop(name, stages) { stages.forEach((draw, i) => plant(name + '_stage' + i, draw)); }
crop('strawberry_crop', [a => sprout(a), a => bush(a, 6, [LEAF, LEAF2], 1),
	a => { bush(a, 8, [LEAF, LEAF2], 1); a.dots([[4, 10], [10, 9], [7, 12], [12, 11]], '#ffffff', { lvl: 4 }).dots([[4, 9], [10, 8]], '#ffe08a', { lvl: 4 }); },
	a => bush(a, 9, [LEAF, LEAF2], 1)]);
crop('blueberry_crop', [a => sprout(a), a => bush(a, 8, ['#4f9a3c', LEAF], 2),
	a => { bush(a, 12, ['#4f9a3c', LEAF], 2); a.dots([[4, 7], [9, 6], [12, 9], [6, 10], [10, 11]], '#a8c87a', { lvl: 3 }); },
	a => bush(a, 13, ['#4f9a3c', LEAF], 2)]);
crop('raspberry_crop', [a => sprout(a), a => bush(a, 9, [LEAF2, '#5aa046'], 3),
	a => { bush(a, 12, [LEAF2, '#5aa046'], 3); a.dots([[4, 6], [10, 5], [7, 9], [12, 8]], '#f5f0e0', { lvl: 4 }); },
	a => bush(a, 14, [LEAF2, '#5aa046'], 3)]);
crop('tea_crop', [a => sprout(a, '#4f9a3c'), a => bush(a, 6, ['#3f8a35', '#4f9a3c'], 4), a => bush(a, 10, ['#3f8a35', '#4f9a3c'], 4),
	a => { bush(a, 12, ['#3f8a35', '#4f9a3c'], 4); [[3, 4], [7, 3], [11, 4], [13, 6], [5, 6]].forEach(([x, y]) => a.el(x, y, 1.6, 1, '#a8e07a', { outline: false, rot: 30 })); }]);
crop('mint_crop', [a => sprout(a, '#62d090'), a => bush(a, 5, ['#4fc07e', '#62d090'], 5), a => bush(a, 7, ['#4fc07e', '#62d090'], 5),
	a => { bush(a, 8, ['#4fc07e', '#62d090'], 5); a.dots([[4, 8], [8, 7], [12, 8]], '#f5f0ff', { lvl: 4 }); }]);
crop('vanilla_crop', [a => sprout(a),
	a => { [[6, 13], [9, 11]].forEach(([x, y], i) => a.el(x, y, 2.2, 1.2, LEAF, { rot: i ? -30 : 30, outline: false })); a.ln(8, 16, 8, 9, 1, '#6a9a3a', { outline: false }); },
	a => { a.ln(8, 16, 8, 2, 1, '#6a9a3a', { outline: false }); [[6, 12], [10, 9], [6, 6], [10, 3]].forEach(([x, y], i) => a.el(x, y, 2.2, 1.2, LEAF, { rot: i % 2 ? -30 : 30, outline: false })); a.el(10, 6, 1.4, 1.2, '#fff4d0', { outline: false }).el(6, 9, 1.4, 1.2, '#fff4d0', { outline: false }); },
	a => { a.ln(8, 16, 8, 2, 1, '#6a9a3a', { outline: false }); [[6, 12], [10, 9], [6, 6], [10, 3]].forEach(([x, y], i) => a.el(x, y, 2.2, 1.2, LEAF, { rot: i % 2 ? -30 : 30, outline: false })); }]);
function lavenderStage(a, h, bloom) {
	[[3, 0.5], [6, -0.3], [9, 0.4], [12, -0.5]].forEach(([x, lean], i) => {
		const top = 16 - h + (i % 2);
		a.ln(x + 0.5, 16, x + 0.5 + lean, top + 1, 1, STEM, { outline: false });
		if (bloom) for (let k = 0; k < 4; k++) a.el(x + 0.5 + lean + (k % 2 ? 0.4 : -0.4), top + k * 1.3 + 0.5, 0.9, 0.8, k % 2 ? '#a383de' : bloom, { outline: false });
	});
	a.el(4, 14, 2, 1, LEAF, { rot: -20, outline: false }).el(11, 14, 2, 1, LEAF, { rot: 20, outline: false });
}
crop('lavender_crop', [a => sprout(a), a => lavenderStage(a, 7), a => lavenderStage(a, 11, '#d4c4f4'), a => lavenderStage(a, 14, '#8d6ccf')]);
crop('rose_crop', [a => sprout(a, '#3f8a3a'), a => bush(a, 7, ['#3f8a3a', '#4f9a3c'], 6),
	a => { bush(a, 10, ['#3f8a3a', '#4f9a3c'], 6); [[4, 7], [10, 6], [7, 9]].forEach(([x, y]) => a.el(x, y, 0.9, 1.2, '#f27a9a', { outline: false })); },
	a => bush(a, 11, ['#3f8a3a', '#4f9a3c'], 6)]);
function rosette(a, h) {
	for (let i = 0; i < 7; i++) {
		const ang = -70 + i * 23;
		const r = h * 0.8;
		a.ln(8, 15.5, 8 + Math.sin(ang * Math.PI / 180) * r * 0.9, 15.5 - Math.cos(ang * Math.PI / 180) * r, 1.3, i % 2 ? '#4f9a3c' : '#6aa84f', { outline: false });
	}
}
crop('pineapple_crop', [a => sprout(a, '#6aa84f'), a => rosette(a, 6), a => rosette(a, 8), a => rosette(a, 9)]);
crop('starbloom_crop', [a => { sprout(a, '#8fd07a'); a.px(7, 11, '#fff3b0'); },
	a => a.ln(8, 16, 8, 8, 1, '#7ab86a', { outline: false }).el(6.4, 11, 1.8, 0.9, '#8fd07a', { rot: 30, outline: false }).el(9.6, 12, 1.8, 0.9, '#8fd07a', { rot: -30, outline: false }),
	a => { a.ln(8, 16, 8, 5, 1, '#7ab86a', { outline: false }).el(6.4, 11, 1.8, 0.9, '#8fd07a', { rot: 30, outline: false }).el(9.6, 12, 1.8, 0.9, '#8fd07a', { rot: -30, outline: false }); a.el(8, 4.5, 1.6, 1.9, '#fff3b0', { outline: false }); },
	a => { a.ln(8, 16, 8, 6, 1, '#7ab86a', { outline: false }).el(6.4, 11, 1.8, 0.9, '#8fd07a', { rot: 30, outline: false }).el(9.6, 12, 1.8, 0.9, '#8fd07a', { rot: -30, outline: false }); a.pxs([[2, 3], [13, 2], [3, 8], [13, 9]], '#fff6c8'); }]);
noise('#e8324e', 0.12, 90).fill((x, y) => ((x + y * 3) % 5 === 0 ? '#ffe7a0' : undefined)).save(B('fruit_strawberry'));
noise('#4d63c9', 0.12, 91).fill((x, y) => (x === 4 && y === 4 ? '#9fb0f5' : undefined)).save(B('fruit_blueberry'));
noise('#e0405e', 0.2, 92).fill((x, y) => ((x % 2 === 0 && y % 2 === 0) ? lv('#e0405e', 5) : undefined)).save(B('fruit_raspberry'));
noise('#ec5a86', 0.15, 93).fill((x, y) => (Math.abs(Math.hypot(x - 7.5, y - 7.5) - 3.5) < 0.7 ? lv('#ec5a86', 1) : undefined)).save(B('fruit_rose'));
T().fill((x, y) => lv('#e8b33a', (Math.round(x + y) % 4 === 0 || Math.round(x - y + 32) % 4 === 0) ? 2 : 3 + wob(x, y, 94, 0.1))).save(B('fruit_pineapple'));
noise('#ffd84a', 0.1, 95).save(B('fruit_star'));
noise('#5a3826', 0.2, 96).save(B('fruit_vanilla'));

// ================= Guide book GUI =================
(() => {
	const a = A(256, 256);
	a.rc(0, 2, 255, 167, '#e8789f', { profile: 'flat', radius: 2 });
	a.rc(6, 6, 125, 161, '#fff8f0', { profile: 'flat', flatLvl: 3, outline: false });
	a.rc(130, 6, 249, 161, '#fff8f0', { profile: 'flat', flatLvl: 3, outline: false });
	a.dec(M.rc(126, 4, 129, 163), '#c95680', { lvl: 1 });
	a.dec(M.or(M.rc(6, 6, 125, 7), M.rc(130, 6, 249, 7)), '#fff8f0', { lvl: 2 });
	for (let i = 0; i < 6; i++) a.heart(20 + i * 18, 157, 1.6, '#f7b6cf', { outline: false }).heart(144 + i * 18, 157, 1.6, '#f7b6cf', { outline: false });
	a.save('gui/guide');
})();

return window.ART.saved.length + ' textures total';
})()
