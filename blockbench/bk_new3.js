(function () {
// Textures for the third wave: new palm bark, five fruit-tree wood sets, new fruits, ten new crops,
// the kitchen machines and the buildable Millstone / Chocolate Fountain.
const ART = window.ART, A = ART.A, T = ART.T, M = ART.M, rnd = ART.rnd;
const B = n => 'block/' + n;
const I = n => 'item/' + n;
const R = {};
const ramp = base => R[base] || (R[base] = ART.ramp(base).map(ART.toHex));
const rampN = base => R['n' + base] || (R['n' + base] = ART.ramp(base, true).map(ART.toHex));
const lv = (base, i) => ramp(base)[Math.max(0, Math.min(5, i))];
const lvN = (base, i) => rampN(base)[Math.max(0, Math.min(5, i))];
const wob = (x, y, s, p) => (rnd(x, y, s) < (p || 0.18) ? (rnd(x, y, s + 3) < 0.5 ? -1 : 1) : 0);
const N = o => Object.assign({ neutral: true }, o || {});

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
const rings = (bark, plank, seed) => T().fill((x, y) => {
	const d = Math.hypot(x - 7.5, y - 7.5);
	if (d > 6.8) return lv(bark, 3 + wob(x, y, seed, 0.25));
	return Math.round(d + rnd(x, y, seed) * 0.6) % 2 === 0 ? lv(plank, 2) : lv(plank, 3 + wob(x, y, seed + 1, 0.12));
});

// ================= Wood sets =================
const BARKS = {
	// Palm: ringed trunk with overlapping diamond scales.
	palm: (x, y) => {
		const row = Math.floor(y / 4), cy = y % 4, cx = (x + (row % 2) * 2) % 4;
		const d = Math.abs(cx - 1.5) + Math.abs(cy - 1.2);
		if (d > 2.1) return lv('#5e4630', 2);
		if (cy === 3) return lv('#7a5c3c', 2);
		return lv(d < 0.9 ? '#c9a77a' : '#9a7854', (cy === 0 ? 4 : 3) + wob(x, y, 61, 0.1));
	},
	// Cherry: dark bark with horizontal lenticel stripes.
	blossom: (x, y) => (y % 5 === 2 && rnd(Math.floor(x / 3), y, 7) < 0.7 ? lv('#8a5a52', 4) : lv('#4f3330', 3 + wob(x, y, 62, 0.2))),
	// Orange: smooth grey-brown with thin vertical cracks.
	orangewood: (x, y) => ((x + Math.floor(y / 6)) % 5 === 0 ? lv('#6a5e52', 1) : lv('#8c8072', 3 + wob(x, y, 63, 0.12))),
	// Peach: rough reddish bark with deep fissures.
	peachwood: (x, y) => (Math.abs(((x * 2 + Math.floor(y / 3)) % 7) - 3) < 0.6 ? lv('#5a3426', 1) : lv('#7e4c3a', 3 + wob(x, y, 64, 0.25))),
	// Mango: dark patchy bark with pale lichen spots.
	mangowood: (x, y) => (rnd(Math.floor(x / 2), Math.floor(y / 2), 65) < 0.12 ? lv('#9aa88a', 3) : lv('#5a5042', 3 + wob(x, y, 66, 0.3))),
	// Plum: purple-grey bark with soft vertical streaks.
	plumwood: (x, y) => (x % 4 === 1 ? lv('#4a3a4e', 2) : lv('#625266', 3 + wob(x, y, 67, 0.15)))
};
function woodSet(name, plank, leafA, leafB, leafExtra, sapling) {
	const bark = BARKS[name];
	if (bark) T().fill(bark).save(B(name + '_log'));
	const barkBase = { palm: '#957252', blossom: '#4f3330', orangewood: '#8c8072', peachwood: '#7e4c3a', mangowood: '#5a5042', plumwood: '#625266' }[name];
	if (bark) rings(barkBase, plank, 70).save(B(name + '_log_top'));
	if (name === 'palm') return;
	T().fill((x, y) => lv(plank, 3 + (x % 5 === 0 ? -1 : 0) + wob(x, y, 71, 0.12))).save(B('stripped_' + name + '_log'));
	rings(plank, plank, 72).save(B('stripped_' + name + '_log_top'));
	planks(16, 16, plank, 73).save(B(name + '_planks'));
	const door = top => T().fill((x, y) => {
		if (x === 0 || x === 15 || (top && y === 0) || (!top && y === 15)) return lv(plank, 1);
		if (top && x >= 3 && x <= 12 && y >= 3 && y <= 12) {
			const u = (x - 7.5) / 3, v = -(y - 7.5) / 3 + 0.25, q = u * u + v * v - 1;
			if (q * q * q - u * u * v * v * v <= 0) return '#d8eef6cc';
			return lv(plank, 2);
		}
		if (!top && x >= 3 && x <= 12 && y >= 3 && y <= 12) return lv(plank, (x === 3 || y === 3) ? 2 : (x === 12 || y === 12) ? 4 : 3);
		if (!top && x === 12 && y === 1) return lv('#e8c46a', 4);
		return lv(plank, 3 + (x % 5 === 0 ? -1 : 0) + wob(x, y, 74, 0.08));
	});
	door(true).save(B(name + '_door_top'));
	door(false).save(B(name + '_door_bottom'));
	T().fill((x, y) => {
		if (x === 0 || x === 15 || y === 0 || y === 15) return lv(plank, 1);
		const u = (x - 7.5) / 4, v = -(y - 7.5) / 4 + 0.25, q = u * u + v * v - 1;
		if (q * q * q - u * u * v * v * v <= 0) return null;
		return lv(plank, 3 + wob(x, y, 75, 0.1));
	}).save(B(name + '_trapdoor'));
	T().fill((x, y) => {
		if (rnd(x, y, 76) < 0.12) return null;
		if (leafExtra && rnd(x, y, 77) < leafExtra[1]) return lv(leafExtra[0], 4);
		const l = 3 + (rnd(Math.floor(x / 2), Math.floor(y / 2), 78) < 0.3 ? 1 : 0) - (rnd(x, y, 79) < 0.25 ? 1 : 0);
		return lv(rnd(Math.floor(x / 3), Math.floor(y / 3), 80) < 0.5 ? leafA : leafB, l);
	}).save(B(name + '_leaves'));
	A().rc(3, 1, 12, 14, plank, { profile: 'flat' }).heart(7.5, 5, 2.2, '#d8eef6', { outline: false }).dots([[10, 9]], '#e8c46a', { lvl: 4 }).save(I(name + '_door'));
	const s = A();
	sapling(s);
	s.save(B(name + '_sapling'));
}
woodSet('palm', '#e8cfa6');
woodSet('blossom', '#f2b6c6', '#f7a8c8', '#ffcfe0', ['#6fae4a', 0.08], s => {
	s.ln(8, 15.5, 8, 7, 1.2, '#4f3330');
	[[5, 5], [11, 5], [8, 3], [6, 8], [10.5, 8]].forEach(([x, y], i) => s.el(x, y, 2, 1.7, i % 2 ? '#f7a8c8' : '#ffcfe0', { outline: false }));
});
woodSet('orangewood', '#eaa868', '#2f7a3a', '#3f8f42', ['#ffffff', 0.05], s => {
	s.ln(8, 15.5, 8, 6, 1.2, '#8c8072');
	s.el(8, 5, 4, 3.6, '#2f7a3a', { outline: false }).el(9.5, 6.5, 1.2, 1.2, '#ff9f2e', { outline: false, shine: 1 });
});
woodSet('peachwood', '#f2c3a6', '#78b84a', '#8fca5a', ['#ffb0c0', 0.05], s => {
	s.ln(8, 15.5, 8, 9, 1.2, '#7e4c3a').ln(8, 9, 5, 5, 1, '#7e4c3a').ln(8, 9, 11, 5.5, 1, '#7e4c3a');
	s.el(5, 4.5, 2.4, 1.6, '#78b84a', { outline: false }).el(11, 5, 2.4, 1.6, '#8fca5a', { outline: false });
});
woodSet('mangowood', '#e4bb62', '#2a6a32', '#3f8a3a', ['#b85a3a', 0.07], s => {
	s.ln(8, 15.5, 8, 5, 1.3, '#5a5042');
	s.el(8, 4.5, 5, 2, '#2a6a32', { outline: false }).el(8, 8, 3.6, 1.4, '#3f8a3a', { outline: false });
});
woodSet('plumwood', '#b896c2', '#7a3a6a', '#9a4a7a', ['#e8b0d8', 0.05], s => {
	s.ln(8, 15.5, 8, 6, 1.2, '#625266');
	s.el(8, 5, 4, 2.6, '#7a3a6a', { outline: false });
	[[4.5, 8], [11.5, 8], [6, 9]].forEach(([x, y]) => s.ln(x, 6, x, y + 2, 1, '#9a4a7a', { outline: false }));
});

// ================= Fruit (hanging blocks + items) =================
noise('#c8243c', 0.1, 81).fill((x, y) => ((x * 3 + y) % 9 === 0 ? lv('#c8243c', 5) : undefined)).save(B('fruit_cherry'));
noise('#ff9a2e', 0.12, 82).fill((x, y) => ((x + y * 2) % 4 === 0 ? lv('#ff9a2e', 2) : undefined)).save(B('fruit_orange'));
T().fill((x, y) => lv(y < 8 ? '#ff9a8a' : '#ffc8a0', 3 + wob(x, y, 83, 0.1))).save(B('fruit_peach'));
T().fill((x, y) => lv(x + y < 12 ? '#e8503a' : x + y < 20 ? '#f6b23a' : '#8cc04a', 3 + wob(x, y, 84, 0.1))).save(B('fruit_mango'));
noise('#6a2e7a', 0.12, 85).fill((x, y) => (rnd(x, y, 3) < 0.12 ? lv('#b8a0d0', 3) : undefined)).save(B('fruit_plum'));
noise('#8cbf4a', 0.15, 86).save(B('fruit_young'));

A().ln(8, 1, 5, 9, 0.9, '#3f8f3a').ln(8, 1, 11, 9, 0.9, '#3f8f3a').el(8.6, 1.6, 2, 1, '#5cbf4e', { rot: -20 })
	.el(5, 11, 3, 3, '#c8243c', { shine: 2 }).el(11, 11.4, 3, 3, '#d8304a', { shine: 2 }).save(I('cherries'));
A().el(8, 9, 6, 5.8, '#ff9a2e', { shine: 1, grain: 0.06 }).dots([[8, 4]], '#6a7a3a', { lvl: 2 }).el(10, 3, 2.4, 1.2, '#4f9a3c', { rot: -25 }).save(I('orange'));
A().add(M.or(M.el(7.4, 9.2, 5.6, 5.4), M.el(9.6, 9.6, 5, 5)), '#ffb894', { contrast: 5 }).dec(M.ln(8.6, 4.5, 8.2, 13, 0.6), '#e88a7a', { shift: -1 })
	.dec(M.el(6, 8, 3, 3), '#ff7a7a', { shift: 0 }).el(10.5, 3.4, 2.6, 1.2, '#6aaa44', { rot: -30 }).save(I('peach'));
A().el(8, 9, 5, 6.4, '#f6b23a', { rot: 30, shine: 1 }).dec(M.el(6, 7, 3, 3.6, 30), '#e8503a', { shift: 0 }).dec(M.el(11, 12, 2.6, 3, 30), '#8cc04a', { shift: 0 })
	.ln(9.5, 2.4, 10.5, 1, 0.8, '#5a3826').save(I('mango'));
A().el(8, 9, 5.4, 5.8, '#6a2e7a', { shine: 1 }).dec(M.ln(8, 4, 7.4, 14, 0.6), '#6a2e7a', { shift: -2 }).dots([[6, 7], [10, 9], [7, 12]], '#c8b0e0', { lvl: 3 })
	.ln(8.4, 3.6, 9, 1.4, 0.8, '#5a3826').save(I('plum'));

// ================= New crops: produce items =================
(() => {
	const a = A().el(7, 8.5, 3.2, 6.6, '#f6d45a', { rot: 30 });
	const kernels = (x, y) => (Math.round(x * 1.3 + y * 0.7) % 2 === 0);
	a.dec(M.and(M.el(7, 8.5, 3, 6.4, 30), kernels), '#f6d45a', { shift: 1 });
	a.pg([[9, 14.5], [13.5, 12], [12, 6], [10.5, 9.5]], '#7cb84a').pg([[4, 13], [7, 15], [5.5, 9]], '#8fca5a');
	a.save(I('corn'));
})();
A().el(8, 8, 6.4, 2.6, '#6fbf4a', { rot: -35 }).dots([[5, 11], [7, 9], [9, 7], [11, 5]], '#a8e07a', { lvl: 4 }).ln(13, 3, 14.5, 1.5, 0.8, '#4f8a3a').save(I('pea_pod'));
A().add(M.or(M.el(8, 8.6, 6.2, 4.4, -25), M.el(13.4, 5.4, 1.6, 1, -25)), '#c8603a', { grain: 0.12 }).dots([[6, 8], [9, 10], [11, 7]], '#9a3a2a', { lvl: 1 }).save(I('sweet_potato'));
A().add(M.or(M.el(8, 8.6, 6.2, 4.4, -25), M.el(13.4, 5.4, 1.6, 1, -25)), '#a8502e', { grain: 0.1 }).el(7.6, 7.8, 3.4, 1.8, '#ffa85a', { rot: -25, outline: false })
	.dots([[7, 7]], '#fff1a8', { lvl: 5 }).save(I('baked_sweet_potato'));
A().ln(3, 13, 12.5, 3.5, 4.4, '#4f8a3a').dec(M.or(M.ln(4, 11, 11, 4, 0.6), M.ln(5, 13, 13, 5, 0.6)), '#8fca5a', { lvl: 4 }).el(13, 3, 1.8, 1.8, '#ffd84a').save(I('zucchini'));
A().ln(4, 15, 9, 6, 1.6, '#d83a5a').ln(7, 15, 11, 6.5, 1.6, '#e85a78').el(10, 4.6, 4, 2.8, '#5aa846', { rot: -15 }).save(I('rhubarb'));
A().el(8, 10.4, 4.6, 4.4, '#9a2a4a', { shine: 1 }).dec(M.el(8, 9.4, 2.4, 2.4, 0, 0.5), '#e8a0b0', { lvl: 4 })
	.ln(8, 6, 6, 1.6, 0.9, '#c8304a').el(5.4, 2.2, 2.2, 1, '#5aa846', { rot: -30 }).el(10.6, 2.8, 2.2, 1, '#6fbf4a', { rot: 30 }).save(I('sugar_beet'));
A().el(5.6, 10, 2.8, 2.8, '#c21e46', { shine: 1 }).el(10.2, 10.6, 2.8, 2.8, '#d8304a', { shine: 1 }).el(7.8, 6, 2.8, 2.8, '#b81840', { shine: 1 })
	.pg([[9, 3], [13, 1.5], [12.6, 4]], '#5aa846').save(I('cranberry'));
A().add(M.or(M.el(5.8, 8.8, 3.4, 3), M.el(10.4, 7.6, 3.4, 3), M.ln(6, 8.6, 10, 7.8, 3.6)), '#d9a066', { grain: 0.15 })
	.dec(M.or(M.ln(5, 7, 6, 10, 0.5), M.ln(10, 6, 11, 9, 0.5)), '#d9a066', { shift: -1 }).save(I('peanut'));
(() => {
	const a = A();
	for (let i = 0; i < 5; i++) {
		const ang = i * Math.PI * 2 / 5 - Math.PI / 2;
		a.el(8 + Math.cos(ang) * 3.4, 8 + Math.sin(ang) * 3.4, 3, 2.4, '#e8457a', { rot: ang * 180 / Math.PI });
	}
	a.el(8, 8, 1.6, 1.6, '#a81a4a').ln(8, 8, 11, 4, 0.7, '#ffe27a', { outline: false }).px(11, 4, '#ffffff');
	a.save(I('hibiscus'));
})();
A().ln(8, 15, 8, 9, 1, '#f7efe0').el(8, 6.4, 5.6, 4.6, '#ffb8e0', { contrast: 4 }).el(5.6, 5, 2.4, 2, '#b8e0ff', { contrast: 4 }).el(10.6, 5.2, 2.2, 1.8, '#fff1a8', { contrast: 4 })
	.save(I('candyfloss'));

// ================= Seed packets (same style as wave two) =================
function packet(name, band, draw) {
	const a = A().rc(3, 2, 12, 14, '#f7efe0', { profile: 'flat' }).rc(3, 2, 12, 4, band, { profile: 'flat' });
	a.dec(M.pts([[3, 4], [5, 4], [7, 4], [9, 4], [11, 4]]), band, { lvl: 1 });
	a.dec(M.ln(3, 13.6, 13, 13.6, 0.5), '#d8c8ae', { lvl: 2 });
	draw(a);
	a.save(I(name));
}
packet('corn_seeds', '#f6d45a', a => a.el(7.8, 9.2, 1.8, 3.4, '#f6d45a', { outline: false, rot: 20 }).pg([[8.6, 12.4], [10.4, 10.6], [9.6, 7.4]], '#7cb84a', { outline: false }));
packet('pea_seeds', '#8fca5a', a => { [[6, 9], [8, 9.2], [10, 9]].forEach(([x, y]) => a.el(x, y, 1.1, 1.1, '#8fca5a', { outline: false, shine: 1 })); a.el(8, 9.2, 3.8, 1.8, '#5aa846', { outline: false, ring: 0.65 }); });
packet('sweet_potato_seeds', '#e88a4a', a => a.el(7.8, 9.4, 3, 1.8, '#c8603a', { outline: false, rot: -20 }).el(8, 6.6, 1.6, 1.2, '#7a5aa0', { outline: false }));
packet('zucchini_seeds', '#8cbf5a', a => a.ln(5.4, 11.6, 10.4, 7, 2.2, '#4f8a3a', { outline: false }).el(10.8, 6.6, 1.1, 1.1, '#ffd84a', { outline: false }));
packet('rhubarb_seeds', '#e85a78', a => a.ln(7, 12.4, 8, 7.6, 1.2, '#d83a5a', { outline: false }).ln(9, 12.4, 9.6, 8, 1.2, '#e85a78', { outline: false }).el(8.6, 7, 2.6, 1.4, '#5aa846', { outline: false }));
packet('sugar_beet_seeds', '#c8304a', a => a.el(7.8, 10, 2.4, 2.2, '#9a2a4a', { outline: false }).el(6.8, 7, 1.4, 0.8, '#5aa846', { outline: false, rot: -30 }).el(8.8, 7, 1.4, 0.8, '#6fbf4a', { outline: false, rot: 30 }));
packet('cranberry_seeds', '#c21e46', a => { [[6.6, 9.6], [9, 10], [7.8, 8]].forEach(([x, y]) => a.el(x, y, 1.3, 1.3, '#c21e46', { outline: false, shine: 1 })); });
packet('peanut_seeds', '#d9a066', a => a.add(M.or(M.el(6.6, 9.4, 1.6, 1.4), M.el(9, 9, 1.6, 1.4), M.ln(6.6, 9.4, 9, 9, 1.6)), '#d9a066', { outline: false }));
packet('hibiscus_seeds', '#e8457a', a => { for (let i = 0; i < 5; i++) { const g = i * Math.PI * 2 / 5; a.el(7.8 + Math.cos(g) * 1.6, 9.2 + Math.sin(g) * 1.6, 1.3, 1, '#e8457a', { outline: false }); } a.px(7, 9, '#ffe27a'); });
packet('candyfloss_seeds', '#ffb8e0', a => a.el(7.8, 8.4, 2.8, 2.2, '#ffb8e0', { outline: false }).el(6.6, 7.6, 1.2, 1, '#b8e0ff', { outline: false }).ln(7.8, 12.6, 7.8, 10.4, 0.7, '#f7efe0', { outline: false }));

// ================= Processed goods =================
A().el(8, 11, 6, 3.6, '#7a4e2e').cut(M.rc(0, 0, 15, 9)).el(8, 9.4, 5.8, 1.6, '#4a2e1f').el(8, 8.6, 4.4, 2.4, '#6a3a24', { grain: 0.1 }).save(I('cocoa_powder'));
A().el(8, 11, 6, 3.6, '#d9c09a').cut(M.rc(0, 0, 15, 9)).el(8, 9.4, 5.8, 1.6, '#a88a5a').el(8, 8.6, 4.4, 2.4, '#f6d45a', { grain: 0.15 }).save(I('cornmeal'));
(() => {
	const a = A().rc(4, 5, 11, 14, '#d6eef6aa', { profile: 'flat', flatLvl: 4 }).rc(5, 7, 10, 13, '#c88a4a', { profile: 'soft' }).rc(4, 2, 11, 4, '#c21e46', { profile: 'flat' });
	a.rc(6, 9, 9, 11, '#fff5e6', { profile: 'flat', outline: false }).add(M.or(M.el(7, 10, 0.8, 0.7), M.el(8.2, 10, 0.8, 0.7)), '#d9a066', { outline: false });
	a.save(I('peanut_butter'));
})();
// Juice: glass (layer0) + liquid (layer1, tinted)
(() => {
	const a = A().pg([[4, 3], [12, 3], [11, 15], [5, 15]], '#e6f4f8aa', { profile: 'flat', flatLvl: 4 });
	a.ln(9, 6.5, 12.5, 0.8, 0.9, '#f27fa6').pxs([[5, 7], [5, 8], [5, 9]], '#ffffffaa');
	a.save(I('juice'));
	A().pg([[4.6, 6], [11.4, 6], [10.6, 14.4], [5.4, 14.4]], '#f0f0f0', N({ profile: 'soft', outline: false })).dots([[7, 8], [9, 11], [8, 13]], '#ffffff', { lvl: 5, neutral: true })
		.save(I('juice_liquid'));
})();
// Ice cream: cone (layer0) + scoop (layer1, tinted)
(() => {
	const a = A().pg([[4.5, 8.5], [11.5, 8.5], [8, 15.5]], '#e2a65a');
	a.dec(M.and(M.pg([[4.5, 8.5], [11.5, 8.5], [8, 15.5]]), (x, y) => Math.round(x + y) % 3 === 0 || Math.round(x - y + 30) % 3 === 0), '#e2a65a', { shift: -2 });
	a.el(8.6, 2.4, 1.3, 1.3, '#e8324e', { shine: 1 });
	a.save(I('ice_cream'));
	A().add(M.or(M.el(8, 6.4, 4.6, 3.6), M.el(5, 8.6, 1.6, 1.2), M.el(11, 8.8, 1.6, 1.2), M.el(8, 9.2, 4.4, 1)), '#f6f6f6', N({ shine: 1 })).save(I('ice_cream_scoop'));
})();
// Fruit jam: jar (layer0) + jam (layer1)
(() => {
	const a = A().rc(4, 5, 11, 14, '#d6eef6aa', { profile: 'flat', flatLvl: 4 }).rc(4, 2, 11, 4, '#f27fa6', { profile: 'flat' });
	a.dots([[4, 2], [6, 2], [8, 2], [10, 2], [5, 3], [7, 3], [9, 3], [11, 3]], '#ffffff', { lvl: 5 }).pxs([[5, 6], [5, 7]], '#ffffffcc');
	a.save(I('fruit_jam'));
	A().rc(5, 7, 10, 13, '#e8e8e8', N({ shine: 1, profile: 'soft', outline: false })).cut(M.rc(6, 9, 9, 11)).save(I('fruit_jam_fill'));
})();
// Chocolate treat: chocolate coat (layer0) + fruit tip (layer1)
(() => {
	A().ln(13, 2, 9, 6, 0.9, '#c9a06b').add(M.or(M.el(7, 9.6, 4.6, 4.4), M.el(9, 7.6, 3, 2.6)), '#5a3220', { shine: 1 })
		.dec(M.or(M.ln(4, 9, 8, 13, 0.6), M.ln(5, 7, 9, 11, 0.6)), '#5a3220', { lvl: 4 }).save(I('chocolate_treat'));
	A().add(M.and(M.or(M.el(9, 7.6, 3.2, 2.8), M.el(10.4, 6, 2.6, 2.4)), (x, y) => x - y > 2.2), '#eeeeee', N({ shine: 1 })).save(I('chocolate_treat_fruit'));
})();

// ================= Crop plane textures =================
const STEM = '#4f8f3a', LEAF = '#5cae46', LEAF2 = '#72c457';
const plant = (name, draw) => { const a = A(); draw(a); a.save(B(name)); };
const sprout = (a, c) => { a.ln(7, 16, 7, 12.5, 1, STEM, { outline: false }).ln(10, 16, 10, 13.5, 1, STEM, { outline: false }); a.el(6, 12, 1.6, 0.9, c || LEAF2, { rot: -30, outline: false }).el(8.4, 12, 1.6, 0.9, c || LEAF, { rot: 30, outline: false }).el(11, 13, 1.4, 0.8, c || LEAF2, { rot: 30, outline: false }); };
function stalk(a, from, to, leaves, col, seed) {
	a.ln(8, from, 8, to, 1.6, col || '#6aa84f', { outline: false });
	for (let i = 0; i < leaves; i++) {
		const y = from - 2 - i * ((from - to) / Math.max(1, leaves)), left = i % 2 === 0;
		a.ln(8, y, left ? 2 + rnd(i, 1, seed) * 2 : 12 + rnd(i, 2, seed) * 2, y - 3.5, 1.2, i % 2 ? LEAF : LEAF2, { outline: false });
	}
}
// Corn: 3 blocks tall
plant('corn_stage0', a => sprout(a, '#7cb84a'));
plant('corn_stage1', a => stalk(a, 16, 7, 3, null, 1));
plant('corn_bottom', a => stalk(a, 16, 0, 5, null, 2));
plant('corn_middle', a => stalk(a, 16, 0, 4, null, 3));
plant('corn_middle_ripe', a => { stalk(a, 16, 0, 3, null, 4); a.el(10, 8, 1.8, 4.2, '#8fca5a', { rot: 15 }).el(10.2, 6.6, 1.1, 2.4, '#f6d45a', { rot: 15, outline: false }).ln(10.6, 3, 11.6, 1, 0.7, '#c9a06b', { outline: false }); });
plant('corn_top', a => { stalk(a, 16, 5, 2, null, 5); [[6, 1], [8, 0], [10, 1.5], [7, 3], [9.5, 3.4]].forEach(([x, y]) => a.ln(8, 6, x, y, 0.8, '#d9b45a', { outline: false })); });
// Peas: climbing vine, 2 blocks tall
function vine(a, flowers, pods, seed) {
	for (let i = 0; i < 3; i++) {
		const x = 4 + i * 4;
		a.ln(x, 16, x + (i - 1) * 0.6, 0, 1, '#5aa846', { outline: false });
		for (let y = 14; y > 1; y -= 3) {
			a.el(x + (y % 2 ? 1.6 : -1.6), y, 1.5, 1, y % 2 ? LEAF : LEAF2, { rot: y % 2 ? 30 : -30, outline: false });
			if (rnd(x, y, seed) < 0.3) a.ln(x, y - 1, x + 2, y - 2.5, 0.5, '#8fca5a', { outline: false });
		}
	}
	if (flowers) [[5, 6], [9, 4], [13, 8], [6, 11]].forEach(([x, y]) => a.el(x, y, 1.2, 1, '#fff6ff', { outline: false }));
	if (pods) [[5, 8], [9, 6], [13, 10], [8, 12]].forEach(([x, y]) => a.el(x, y, 0.9, 2.2, '#7cc84a', { rot: 10 }));
}
plant('pea_stage0', a => sprout(a));
plant('pea_stage1', a => { a.ln(8, 16, 8, 6, 1, '#5aa846', { outline: false }); [[6.5, 13], [9.5, 11], [6.5, 9]].forEach(([x, y], i) => a.el(x, y, 1.6, 1, LEAF, { rot: i % 2 ? 30 : -30, outline: false })); });
plant('pea_bottom', a => vine(a, false, false, 1));
plant('pea_top', a => vine(a, true, false, 2));
plant('pea_bottom_ripe', a => vine(a, false, true, 3));
plant('pea_top_ripe', a => vine(a, true, true, 4));
// Root crops & friends
function rosette(a, h, cols, stemCol) {
	for (let i = 0; i < 6; i++) {
		const ang = -75 + i * 30, r = h;
		const ex = 8 + Math.sin(ang * Math.PI / 180) * r * 0.85, ey = 16 - Math.cos(ang * Math.PI / 180) * r;
		a.ln(8, 16, ex, ey + 1.5, 1, stemCol || STEM, { outline: false });
		a.el(ex, ey, 1.8, 1.2, cols[i % cols.length], { rot: ang, outline: false });
	}
}
const stages = (name, fns) => fns.forEach((f, i) => plant(name + '_stage' + i, f));
stages('sweet_potato_crop', [a => sprout(a, '#7a8a5a'), a => rosette(a, 5, ['#6a8a4a', '#7a5aa0']), a => rosette(a, 8, ['#6a8a4a', '#7a5aa0', '#5a9a46']), a => rosette(a, 10, ['#6a8a4a', '#7a5aa0', '#5a9a46'])]);
stages('peanut_crop', [a => sprout(a), a => rosette(a, 5, [LEAF, LEAF2]), a => { rosette(a, 7, [LEAF, LEAF2]); a.dots([[4, 10], [11, 9], [8, 8]], '#ffd84a', { lvl: 4 }); }, a => rosette(a, 8, [LEAF, LEAF2])]);
stages('sugar_beet_crop', [a => sprout(a), a => rosette(a, 6, [LEAF, LEAF2], '#c8304a'), a => rosette(a, 9, [LEAF, LEAF2], '#c8304a'), a => rosette(a, 11, [LEAF, '#4f9a3c'], '#c8304a')]);
stages('rhubarb_crop', [a => sprout(a), a => rosette(a, 6, ['#5aa846'], '#d83a5a'), a => rosette(a, 10, ['#5aa846', '#4f9a3c'], '#d83a5a'), a => rosette(a, 12, ['#5aa846', '#4f9a3c'], '#e8506e')]);
function shrub(a, h, cols, seed) {
	[[4, -0.6], [8, 0], [12, 0.6]].forEach(([x, lean], i) => {
		const top = 16 - h + i % 2, tx = x + lean * 2;
		a.ln(x, 16, tx, top, 1, STEM, { outline: false });
		for (let y = 14 - (i % 2); y > top + 1; y -= 2.8) {
			a.el(tx - 1.4, y, 1.5, 0.8, cols[0], { rot: -25, outline: false }).el(tx + 1.4, y - 1, 1.5, 0.8, cols[1 % cols.length], { rot: 25, outline: false });
		}
	});
}
stages('hibiscus_crop', [a => sprout(a, '#3f8a3a'), a => shrub(a, 7, ['#3f8a3a', '#4f9a3c'], 1), a => { shrub(a, 11, ['#3f8a3a', '#4f9a3c'], 2); [[5, 6], [11, 5]].forEach(([x, y]) => a.el(x, y, 0.9, 1.4, '#e8457a', { outline: false })); }, a => shrub(a, 12, ['#3f8a3a', '#4f9a3c'], 3)]);
stages('cranberry_crop', [a => sprout(a, '#5a8a3a'), a => shrub(a, 4, ['#5a8a3a', '#4a7a3a'], 4), a => { shrub(a, 6, ['#5a8a3a', '#4a7a3a'], 5); a.dots([[4, 12], [9, 11], [12, 13]], '#ffc0d0', { lvl: 4 }); }, a => shrub(a, 7, ['#5a8a3a', '#7a3a3a'], 6)]);
stages('candyfloss_crop', [a => { sprout(a, '#c8e8ff'); }, a => a.ln(8, 16, 8, 8, 1, '#f7efe0', { outline: false }).el(6.6, 11, 1.6, 0.9, '#b8e0ff', { rot: 30, outline: false }).el(9.4, 12, 1.6, 0.9, '#ffd0ec', { rot: -30, outline: false }),
	a => { [[5, 1], [8, 0], [11, -1]].forEach(([x, l]) => a.ln(x, 16, x + l, 6, 1, '#f7efe0', { outline: false })); a.el(8, 5, 2.4, 2, '#ffd0ec', { outline: false }); },
	a => { [[5, 1], [8, 0], [11, -1]].forEach(([x, l]) => a.ln(x, 16, x + l, 7, 1, '#f7efe0', { outline: false })); a.pxs([[3, 4], [13, 3], [2, 9]], '#ffffff'); }]);
noise('#c8603a', 0.2, 90).save(B('fruit_sweet_potato'));
noise('#9a2a4a', 0.12, 91).fill((x, y) => (y < 4 ? lv('#f0d8d0', 3) : undefined)).save(B('fruit_sugar_beet'));
noise('#d83a5a', 0.1, 92).fill((x, y) => (x % 4 === 0 ? lv('#e85a78', 4) : undefined)).save(B('fruit_rhubarb'));
noise('#e8457a', 0.12, 93).fill((x, y) => (Math.hypot(x - 7.5, y - 7.5) < 2 ? lv('#a81a4a', 3) : undefined)).save(B('fruit_hibiscus'));
noise('#c21e46', 0.1, 94).save(B('fruit_cranberry'));
T().fill((x, y) => lvN('#f4f4f4', 3 + wob(x, y, 95, 0.3))).save(B('fruit_fluff'));
noise('#d9a066', 0.2, 96).save(B('fruit_peanut'));
noise('#8a6a48', 0.15, 97).save(B('trellis'));
// Zucchini: vanilla-style stem textures (greyscale, tinted in game) + the lying zucchini
T().fill((x, y) => ((x === 7 || x === 8) && y >= 16 - 16 ? lvN('#bfbfbf', 3) : (y > 4 && rnd(x, y, 9) < 0.08 && Math.abs(x - 7.5) < 3 ? lvN('#bfbfbf', 2) : null))).save(B('zucchini_stem'));
T().fill((x, y) => (y >= 7 && y <= 8 ? lvN('#bfbfbf', 3) : (x >= 0 && x <= 1 && y >= 5 && y <= 10 ? lvN('#bfbfbf', 2) : null))).save(B('zucchini_stem_attached'));
T().fill((x, y) => lv(y % 3 === 0 ? '#8cbf5a' : '#3f7a3a', 3 + wob(x, y, 98, 0.15))).save(B('zucchini_skin'));
T().fill((x, y) => (Math.hypot(x - 7.5, y - 7.5) < 6.5 ? lv('#e8f0c8', 3 + wob(x, y, 99, 0.1)) : lv('#3f7a3a', 3))).save(B('zucchini_end'));

// ================= Kitchen machines =================
noise('#fbf6f0', 0.05, 100).fill((x, y) => (y === 12 || y === 13 ? lv('#f49ac0', y === 12 ? 4 : 3) : undefined)).save(B('juicer_body'));
T().fill((x, y) => (x === 0 || x === 15 ? '#ffffffa0' : (x + y) % 11 === 0 ? '#ffffff70' : '#e8f6fb40')).save(B('juicer_glass'));
noise('#cfd6de', 0.08, 101).fill((x, y) => (y === 0 ? lv('#cfd6de', 5) : undefined)).save(B('metal'));
T().fill((x, y) => lvN('#ededed', 3 + wob(x, y, 102, 0.06) + (Math.abs(Math.hypot(x - 7.5, y - 7.5) - 3.5) < 0.6 ? 1 : 0))).save(B('juice_liquid'));
T().fill((x, y) => (y % 5 === 0 ? lv('#9aa4ae', 3) : lv(x % 4 === 0 ? '#b8875a' : '#c99a63', 3 + wob(x, y, 103, 0.12)))).save(B('churn_wood'));
T().fill((x, y) => lvN('#f4f4f4', 3 + wob(x, y, 104, 0.08))).save(B('ice_cream_block'));
noise('#c8743a', 0.12, 105).fill((x, y) => (y === 1 || y === 14 ? lv('#e8945a', 4) : undefined)).save(B('copper_pot'));
T().fill((x, y) => lvN('#e6e6e6', 3 + wob(x, y, 106, 0.12) + (rnd(x, y, 7) < 0.07 ? 2 : 0))).save(B('jam_surface'));
T().fill((x, y) => ((x + (y % 4 < 2 ? 0 : 2)) % 4 < 2 ? lv('#d8b07a', 3 + wob(x, y, 107, 0.15)) : lv('#b88a52', 3 + wob(x, y, 108, 0.15)))).save(B('banneton'));
T().fill((x, y) => (Math.floor(x / 2) % 2 && Math.floor(y / 2) % 2 ? lv('#f7b6cf', 3) : lv('#fff6f0', 3))).save(B('linen'));
noise('#f1d7a6', 0.12, 109).fill((x, y) => (rnd(x, y, 3) < 0.05 ? '#fff8e8' : undefined)).save(B('proof_dough'));
// Cores
T().fill((x, y) => {
	const d = Math.hypot(x - 7.5, y - 7.5);
	if (d < 6.5) return lv('#b8b4ae', (Math.abs(Math.sin(Math.atan2(y - 7.5, x - 7.5) * 4)) < 0.2 ? 2 : 3) + (d < 1.5 ? -1 : 0) + wob(x, y, 110, 0.15));
	return lv('#8a8680', 3 + ((x % 8 === 7 || y % 4 === 3) ? -1 : 0));
}).save(B('millstone_core'));
T().fill((x, y) => {
	if (y >= 12) return lv('#e8c46a', y === 12 ? 5 : 3);
	if (y < 6 && Math.abs(x - 7.5) < 1.5 + y * 0.4) return lv('#6a3a24', 3 + (x === 7 ? 1 : 0));
	return lv('#fbf5ee', 3 + wob(x, y, 111, 0.05));
}).save(B('fountain_core'));

// ================= Big machines (64px, one texel per unit) =================
T(64, 64).fill((x, y) => {
	const row = Math.floor(y / 8), off = (row % 2) * 8, bx = (x + off) % 16, by = y % 8;
	if (by === 7 || bx === 15) return lv('#7a766e', 2);
	return lv('#a8a49c', (by === 0 || bx === 0 ? 4 : by === 6 || bx === 14 ? 2 : 3) + wob(x, y, 112, 0.12));
}).save(B('mill/stone_bricks'));
T(64, 64).fill((x, y) => {
	const d = Math.hypot(x - 31.5, y - 31.5), a = Math.atan2(y - 31.5, x - 31.5);
	const groove = Math.abs(Math.sin(a * 8 + d * 0.12)) < 0.12;
	return lv('#c4c0b8', 3 + (groove ? -1 : 0) + (Math.round(d) % 9 === 0 ? -1 : 0) + wob(x, y, 113, 0.15));
}).save(B('mill/millstone'));
T(64, 64).fill((x, y) => lv('#a07a4e', 3 + (y % 6 === 0 ? -1 : 0) + wob(x, y, 114, 0.15))).save(B('mill/wood'));
T(64, 64).fill((x, y) => {
	const band = y % 16;
	if (band === 0 || band === 1) return lv('#f7b6cf', band === 0 ? 4 : 3);
	return lv('#fbf5ee', 3 + wob(x, y, 115, 0.05) + ((x + y) % 23 === 0 ? -1 : 0));
}).save(B('fountain/ceramic'));
T(64, 64).fill((x, y) => lv('#6a3a24', 3 + ((Math.sin(x * 0.4 + y * 0.15) > 0.94) ? 1 : 0) + wob(x, y, 116, 0.06))).save(B('fountain/chocolate'));
T(64, 64).fill((x, y) => lv('#e8c46a', 3 + (y % 8 === 0 ? 1 : 0) + wob(x, y, 117, 0.12))).save(B('fountain/gold'));

// ================= Baker villager outfit (chef hat + pink apron), 64x64 villager layout =================
(() => {
	const t = T(64, 64);
	const put = (x1, y1, x2, y2, fn) => { for (let y = y1; y <= y2; y++) for (let x = x1; x <= x2; x++) t.set(x, y, fn(x, y)); };
	put(40, 0, 47, 7, (x, y) => lv('#ffffff', 3 + (y === 0 ? 1 : 0)));
	put(48, 0, 55, 7, (x, y) => lv('#f4f4f4', 3));
	put(32, 8, 63, 10, (x, y) => lv('#ffffff', y === 8 ? 4 : 3));
	put(32, 11, 63, 11, () => lv('#f49ac0', 3));
	put(6, 45, 13, 45, () => lv('#f49ac0', 3));
	put(7, 46, 12, 63, (x, y) => lv('#fbfbfb', 3 + (x === 7 ? 1 : x === 12 ? -1 : 0)));
	put(8, 52, 11, 54, () => lv('#f49ac0', 3));
	put(23, 26, 23, 37, () => lv('#fbfbfb', 3));
	put(28, 26, 28, 37, () => lv('#fbfbfb', 3));
	t.save('entity/villager/profession/baker');
	t.save('entity/zombie_villager/profession/baker');
})();

return window.ART.saved.length + ' textures';
})()
