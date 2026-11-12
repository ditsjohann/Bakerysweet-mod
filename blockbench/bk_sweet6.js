(function () {
// Sixth wave, "Sweet Life": coins, cookie cutters, icing bags, sugar cookies, gifts, outfits, the catalog,
// the cake turntable and a set of cozy pastel decor. Runs after bk_art.js (in Blockbench or the bk_server page).
const ART = window.ART, A = ART.A, T = ART.T, M = ART.M, rnd = ART.rnd;
const B = n => 'block/' + n;
const I = n => 'item/' + n;
const R = {};
const ramp = base => R[base] || (R[base] = ART.ramp(base).map(ART.toHex));
const lv = (base, i) => ramp(base)[Math.max(0, Math.min(5, Math.round(i)))];
const N = o => Object.assign({ neutral: true }, o || {});
const wob = (x, y, s, p) => (rnd(x, y, s) < (p || 0.18) ? (rnd(x, y, s + 3) < 0.5 ? -1 : 1) : 0);

// ---------------- Cookie shapes (shared by cutters, cookies and icing) ----------------
const shape = (name, s, cx, cy) => {
	cx = cx || 8; cy = cy || 8.5;
	switch (name) {
		case 'heart': return M.heart(cx, cy - 0.2, 6.4 * s);
		case 'star': return M.star(cx, cy + 0.3, 7.4 * s, 3.4 * s);
		case 'bunny': return M.or(M.el(cx, cy + 2, 4.8 * s, 4.2 * s), M.el(cx - 2.2 * s, cy - 3.6 * s, 1.5 * s, 3.6 * s, -12), M.el(cx + 2.2 * s, cy - 3.6 * s, 1.5 * s, 3.6 * s, 12));
		default: {
			const petals = [];
			for (let i = 0; i < 5; i++) {
				const a = -Math.PI / 2 + i * Math.PI * 2 / 5;
				petals.push(M.el(cx + Math.cos(a) * 3.6 * s, cy + Math.sin(a) * 3.6 * s, 2.9 * s, 2.9 * s));
			}
			return M.or(M.el(cx, cy, 3.2 * s, 3.2 * s), ...petals);
		}
	}
};
const SHAPES = ['heart', 'star', 'bunny', 'flower'];
const CUTTER = { heart: '#f49ab8', star: '#ffd36b', bunny: '#c5a3ef', flower: '#8fd9b6' };

SHAPES.forEach(name => {
	// Cutter: a coloured plastic outline of the shape with a little grip on top.
	A().add(M.and(shape(name, 1.02), M.not(shape(name, 0.74))), CUTTER[name], { shine: 2, profile: 'soft' })
		.save(I(name + '_cutter'));
	// Baked cookie: golden, a few darker freckles.
	A().add(shape(name, 0.98), '#e2ab6a', { grain: 0.12, profile: 'soft' })
		.dec(M.and(shape(name, 0.98), (x, y) => rnd(Math.floor(x), Math.floor(y), 31) < 0.08), '#b97a3c', { shift: -1 })
		.save(I('cookie_' + name));
	// Icing: a white (tinted in game) layer inset from the cookie's edge, glossy.
	A().add(shape(name, 0.74), '#f6f6f6', N({ shine: 2, profile: 'soft', outline: false }))
		.dec(M.and(shape(name, 0.74), (x, y) => rnd(Math.floor(x), Math.floor(y), 37) < 0.07), '#ffffff', N({ lvl: 5 }))
		.save(I('cookie_' + name + '_icing'));
	// Raw: pale dough on a little parchment square.
	A().rc(1, 2, 14, 14, '#f4eee2', N({ profile: 'flat', flatLvl: 4 }))
		.add(shape(name, 0.86), '#f7e7c8', N({ profile: 'soft', grain: 0.08 }))
		.save(I('raw_cookie_' + name));
});

// ---------------- Coin, icing bags, gifts ----------------
A().el(8, 8, 6.6, 6.6, '#ffc2d6', { shine: 2 })
	.dec(M.el(8, 8, 6.6, 6.6, 0, 0.82), '#e889ab', { lvl: 2 })
	.heart(8, 8.6, 3.0, '#ff6f91', { outline: false, shine: 1 })
	.save(I('sweet_coin'));

// Icing bag: a cone of cloth (layer 0) with the icing inside (layer 1, tinted) and a silver nozzle.
A().pg([[2.5, 2], [13.5, 2], [9.2, 12.5], [7.8, 12.5]], '#ececec', N({ profile: 'soft' }))
	.rc(2, 1, 13, 2, '#d8d8d8', N({ profile: 'flat' }))
	.pg([[7, 12], [10, 12], [8.6, 15.5]], '#cfd6de', N({ shine: 1 }))
	.save(I('icing_bag'));
A().pg([[4, 3.2], [12, 3.2], [8.9, 11.6], [8.1, 11.6]], '#f8f8f8', N({ profile: 'soft', outline: false, shine: 1 }))
	.save(I('icing_bag_tint'));

// Empty gift box: cream box, pink ribbon and bow.
A().rc(2, 7, 13, 14, '#fff3f6', { profile: 'soft' })
	.rc(1, 5, 14, 7, '#ffe3ec', { profile: 'flat' })
	.rc(7, 5, 8, 14, '#f49ab8', { profile: 'flat', outline: false })
	.el(5.2, 3.6, 2.6, 1.8, '#f49ab8', { rot: 20 }).el(10.8, 3.6, 2.6, 1.8, '#f49ab8', { rot: -20 })
	.el(8, 4.4, 1.3, 1.3, '#e0628c')
	.save(I('gift_box'));
// Wrapped gift: polka-dot paper (layer 0) + ribbon and bow in white (layer 1, tinted).
A().rc(2, 7, 13, 14, '#fbfbfb', N({ profile: 'soft' }))
	.rc(1, 5, 14, 7, '#f4f4f4', N({ profile: 'flat' }))
	.dots([[4, 9], [10, 9], [5, 12], [11, 12], [3, 6], [12, 6]], '#ffd6e5', N({ lvl: 3 }))
	.save(I('wrapped_gift'));
A().rc(7, 5, 8, 14, '#f6f6f6', N({ profile: 'flat', outline: false }))
	.rc(2, 9, 13, 10, '#f6f6f6', N({ profile: 'flat', outline: false }))
	.el(5, 3.4, 2.7, 1.9, '#f6f6f6', N({ rot: 20 })).el(11, 3.4, 2.7, 1.9, '#f6f6f6', N({ rot: -20 }))
	.el(8, 4.3, 1.4, 1.3, '#e6e6e6', N())
	.save(I('wrapped_gift_ribbon'));

// ---------------- Outfit ----------------
A().pg([[5, 1], [11, 1], [11, 6], [14, 7], [13, 15], [3, 15], [2, 7], [5, 6]], '#f7b6cf', { profile: 'soft' })
	.dec(M.rc(3, 14, 12, 14), '#ffffff', N({ lvl: 5 }))
	.dec(M.pts([[3, 15], [5, 15], [7, 15], [9, 15], [11, 15]]), '#ffffff', N({ lvl: 4 }))
	.heart(8, 10.5, 2.0, '#ff8fb0', { outline: false })
	.dec(M.ln(5, 1, 3, 0, 0.8), '#ffffff', N({ lvl: 5 }))
	.save(I('frilly_apron'));

// Armor layer (64x32): the apron drawn on the body's front, ties at the back.
{
	const t = T(64, 32);
	const pink = '#f7b6cf';
	for (let y = 20; y < 32; y++) for (let x = 20; x < 28; x++) {
		const bib = y < 26 && (x < 21 || x > 26);
		if (bib) continue;
		let c = lv(pink, 3 + wob(x, y, 51, 0.12));
		if (y === 20 || x === 21 && y < 26 || x === 26 && y < 26) c = '#ffffff';
		if (y === 31) c = x % 2 ? '#ffffff' : lv(pink, 4);
		if (y === 26) c = lv(pink, 2);
		if ((x === 23 || x === 24) && (y === 28 || y === 29) || (x === 22 || x === 25) && y === 28) c = '#ff7fa6';
		t.set(x, y, c);
	}
	for (let x = 16; x < 40; x++) t.set(x, 26, x === 35 || x === 36 ? '#ffffff' : lv(pink, 2));
	[[34, 25], [37, 25], [34, 27], [37, 27], [35, 28], [36, 28]].forEach(([x, y]) => t.set(x, y, lv(pink, 3)));
	t.save('models/armor/frilly_apron_layer_1');
}

// Hat fabrics (16x16 sheets used by the 3D hat models).
T().fill((x, y) => lv('#fbfbfb', 3 + (y % 4 === 0 ? -1 : 0) + wob(x, y, 52, 0.1))).save(B('hat_chef'));
T().fill((x, y) => (y >= 12 ? lv('#5fb84a', 3 + wob(x, y, 53, 0.2)) : (x * 3 + y * 5) % 7 === 0 ? lv('#ffe27a', 4) : lv('#e83e5c', 3 + wob(x, y, 54, 0.15)))).save(B('hat_beret'));
T().fill((x, y) => (y >= 8 ? lv('#ffb3c7', 3 + wob(x, y, 55, 0.1)) : lv('#fafafa', 3 + wob(x, y, 56, 0.2)))).save(B('hat_bunny'));
T().fill((x, y) => (y >= 8 ? lv('#ffb3c7', 3 + wob(x, y, 57, 0.1)) : lv('#cdb4f0', 3 + wob(x, y, 58, 0.2)))).save(B('hat_cat'));
T().fill((x, y) => ((x + y) % 5 === 0 && y % 2 === 0 ? '#ffffff' : lv('#f78fb3', 3 + (x % 8 === 0 ? -1 : 0) + wob(x, y, 59, 0.12)))).save(B('hat_bow'));
T().fill((x, y) => lv('#f6d4e0', 3 + wob(x, y, 60, 0.15))).save(B('hat_band'));

// ---------------- Decor tiles ----------------
const checker = (name, color) => T().fill((x, y) => {
	const on = (Math.floor(x / 4) + Math.floor(y / 4)) % 2 === 0;
	const base = on ? color : '#fff9f4';
	if (x % 4 === 3 || y % 4 === 3) return lv(base, 2);
	if (x % 4 === 0 && y % 4 === 0) return lv(base, 5);
	return lv(base, 3 + wob(x, y, 61, 0.06));
}).save(B(name));
checker('pink_checker_tiles', '#f7b6cf');
checker('mint_checker_tiles', '#a8e6cf');
checker('lilac_checker_tiles', '#cdb4f0');
checker('butter_checker_tiles', '#ffe9a3');

T().fill((x, y) => {
	const cx = (x + (Math.floor(y / 8) % 2) * 4) % 8, cy = y % 8;
	if (cx >= 2 && cx <= 4 && cy >= 3 && cy <= 5) return (cx + cy) % 3 === 0 ? '#ffe27a' : lv('#e8475f', cy === 3 ? 4 : 3);
	if (cy === 2 && cx >= 2 && cx <= 4) return cx === 3 ? lv('#5fb84a', 4) : lv('#5fb84a', 3);
	if (cy === 6 && cx === 3) return lv('#e8475f', 2);
	return lv('#fff6ee', 3 + (x % 8 === 7 ? -1 : 0) + wob(x, y, 62, 0.04));
}).save(B('strawberry_wallpaper'));
T().fill((x, y) => {
	const h = y % 4 < 2, v = x % 4 < 2;
	return h && v ? lv('#f49ab8', 3) : h || v ? lv('#fbd0de', 3) : lv('#ffffff', 3);
}).save(B('gingham_wallpaper'));
{
	// A hand-drawn 7x6 pixel heart repeated on an offset 8x8 grid.
	const HEART = ['.XX.XX.', 'XXXXXXX', 'XXXXXXX', '.XXXXX.', '..XXX..', '...X...'];
	T().fill((x, y) => {
		const ox = (Math.floor(y / 8) % 2) * 4;
		const hx = (x + ox) % 8, hy = y % 8 - 1;
		if (hy >= 0 && hy < 6 && hx < 7 && HEART[hy][hx] === 'X') return hy >= 3 ? lv('#ffffff', 2) : '#ffffff';
		return lv('#f9c6d8', 3 + wob(x, y, 63, 0.05));
	}).save(B('heart_wallpaper'));
}
T().fill((x, y) => {
	const ox = (Math.floor(y / 8) % 2) * 4;
	const dx = ((x + ox) % 8) - 3.5, dy = (y % 8) - 3.5;
	if (dx * dx + dy * dy < 3.2) return dy > 0.5 ? lv('#ffffff', 2) : '#ffffff';
	return lv('#b9def7', 3 + wob(x, y, 64, 0.05));
}).save(B('polka_dot_wallpaper'));

{
	const heart = M.heart(8, 8.6, 5.6);
	T().fill((x, y) => {
		const fluff = wob(x, y, 65, 0.3);
		if (heart(x + 0.5, y + 0.5)) return lv('#ff7fa6', 3 + fluff + (y < 7 ? 1 : 0));
		if (x === 0 || y === 0 || x === 15 || y === 15) return lv('#ffffff', 3);
		return lv('#fbc3d6', 3 + fluff);
	}).save(B('heart_rug'));
	T().fill((x, y) => {
		const fluff = wob(x, y, 66, 0.3);
		const scallop = Math.min(x, 15 - x, y, 15 - y);
		const bump = (Math.sin(x * 1.2) + Math.sin(y * 1.2)) * 0.6;
		if (scallop + bump < 1.2) return lv('#b9def7', 3 + fluff);
		return lv('#ffffff', 3 + fluff - (scallop < 2.5 ? 1 : 0));
	}).save(B('cloud_rug'));
}

// Heart balloons (cross sprites): a shiny heart on a curly string.
[['pink', '#ff8fb3'], ['lilac', '#c8a6f2'], ['mint', '#8fdcbb']].forEach(([name, color]) => {
	A().ln(8, 9, 7, 11, 0.8, '#d9d9d9', N({ outline: false })).ln(7, 11, 9, 13, 0.8, '#d9d9d9', N({ outline: false }))
		.ln(9, 13, 8, 15.5, 0.8, '#d9d9d9', N({ outline: false }))
		.heart(8, 4.8, 4.6, color, { shine: 2 })
		.pg([[7.2, 9.4], [8.8, 9.4], [8, 8.4]], color)
		.save(B(name + '_heart_balloon'));
});

// Fairy lights: a sagging wire with glowing bulbs (wall sprite).
{
	const a = A();
	a.add((x, y) => Math.abs(y - (3 + Math.sin((x / 16) * Math.PI) * 4)) < 0.55, '#7a8f6a', N({ outline: false, profile: 'flat' }));
	[[2, 4.6], [5.5, 6.2], [9, 7], [12.5, 6.1]].forEach(([x, y], i) => {
		const c = ['#ffe27a', '#ff9fc0', '#a8e6cf', '#ffe27a'][i];
		a.el(x, y + 1.6, 1.3, 1.7, c, { shine: 1, outline: false });
	});
	a.save(B('fairy_lights'));
}

// Awnings: candy stripes with a scalloped edge at the bottom.
[['pink', '#f7a8c6'], ['mint', '#9fdcc2']].forEach(([name, color]) => T().fill((x, y) => {
	if (y >= 13) {
		const r = Math.hypot((x % 4) - 1.5, y - 12.5);
		if (r > 2.4) return null;
	}
	const stripe = Math.floor(x / 4) % 2 === 0;
	return stripe ? lv(color, 3 + (y === 0 ? 1 : 0) + wob(x, y, 67, 0.05)) : lv('#ffffff', 3 + (y === 0 ? 1 : 0));
}).save(B(name + '_awning')));

// Cupcake lamp
T().fill((x, y) => lv(x % 3 === 0 ? '#e889ab' : '#f7b6cf', 3 + (y < 2 ? 1 : 0))).save(B('cupcake_lamp_wrapper'));
T().fill((x, y) => {
	if (rnd(x, y, 68) < 0.07) return ['#ff6f91', '#7ec8f2', '#ffe27a', '#a8e6cf'][Math.floor(rnd(x, y, 69) * 4)];
	return lv('#fff6e8', 3 + ((x + y) % 5 === 0 ? 1 : 0) + wob(x, y, 70, 0.08));
}).save(B('cupcake_lamp_frosting'));
T().fill((x, y) => lv('#e8304e', 3 + (x < 6 && y < 6 ? 1 : 0) + wob(x, y, 71, 0.1))).save(B('cupcake_lamp_cherry'));

// Cookie jar: clear glass with white glints, pink lid, cookies inside.
T().fill((x, y) => (x === 0 || x === 15 ? '#ffffffb0' : (x === 3 && y > 2 && y < 12) || (x === 4 && y > 4 && y < 9) ? '#ffffffd0' : null)).save(B('cookie_jar_glass'));
T().fill((x, y) => lv('#f7a8c6', 3 + (y < 3 ? 1 : 0) + wob(x, y, 72, 0.08))).save(B('cookie_jar_lid'));
T().fill((x, y) => {
	const cx = (x % 6) - 2.5, cy = (y % 6) - 2.5;
	if (cx * cx + cy * cy > 7) return lv('#a86c3c', 2);
	return rnd(x, y, 73) < 0.15 ? lv('#5a3220', 3) : lv('#d9a066', 3 + wob(x, y, 74, 0.15));
}).save(B('cookie_jar_cookies'));

// Macaron tower: stacked rows of pastel macarons.
{
	const colors = ['#f7a8c6', '#a8e6cf', '#cdb4f0', '#ffe9a3'];
	T().fill((x, y) => {
		const row = Math.floor(y / 4), r = y % 4;
		const c = colors[(row + Math.floor(x / 4)) % 4];
		if (r === 2) return lv('#fff6ea', 3);
		if (x % 4 === 3) return lv(c, 2);
		return lv(c, r === 0 ? 4 : 3);
	}).save(B('macaron_tower'));
	T().fill((x, y) => {
		const d = Math.hypot(x - 7.5, y - 7.5);
		if (d < 3) return lv('#e8304e', 3 + (x < 7 && y < 7 ? 1 : 0));
		return lv('#fff6ea', 3 + wob(x, y, 75, 0.1));
	}).save(B('macaron_tower_top'));
}

// Bakery Catalog: a pink cover with a golden heart, cream pages.
{
	const heart = M.heart(8, 8.4, 4.2);
	T().fill((x, y) => {
		if (x === 0 || y === 0 || x === 15 || y === 15) return lv('#d06a92', 3);
		if (heart(x + 0.5, y + 0.5)) return lv('#ffd36b', y < 7 ? 4 : 3);
		if (x === 1 || y === 1 || x === 14 || y === 14) return lv('#ffd36b', 2);
		return lv('#f7a8c6', 3 + wob(x, y, 76, 0.06));
	}).save(B('catalog_cover'));
	T().fill((x, y) => (y % 3 === 1 && x > 1 && x < 14 ? lv('#e9d9cf', 3) : lv('#fffaf2', 3 + wob(x, y, 77, 0.04)))).save(B('catalog_pages'));
}

// Cake turntable: white porcelain plate with a pink rim and a pastel stand.
T().fill((x, y) => {
	const d = Math.hypot(x - 7.5, y - 7.5);
	if (d > 6.8) return lv('#f7a8c6', 3);
	if (d > 5.9 && Math.floor(Math.atan2(y - 7.5, x - 7.5) * 4) % 2 === 0) return lv('#f7a8c6', 4);
	return lv('#ffffff', 3 + (d < 2 ? 1 : 0) - (x + y > 22 ? 1 : 0));
}).save(B('turntable_plate'));
T().fill((x, y) => lv('#fbd0de', 3 + (y % 4 === 0 ? 1 : 0) + wob(x, y, 78, 0.06))).save(B('turntable_base'));

return window.ART.saved.length + ' textures saved';
})()
