(function () {
const BK = window.BK;
const { S, J, rnd } = BK;
const B = n => 'block/' + n;
const flat = { shade: false, outline: false };

// Noise texture of one base color
function noise(name, col, amt, extra) {
	const s = S().fill((x, y) => J(col, x, y, amt, name.length));
	if (extra) extra(s);
	s.done(flat).save(B(name));
	return s;
}

// ---------- Oven ----------
function bricks(s, seed) {
	return s.fill((x, y) => {
		const band = Math.floor(y / 4), off = (band % 2) * 4;
		if (y % 4 === 3 || (x + off) % 8 === 7) return J('#d9c3a5', x, y, 0.06, seed);
		const id = band * 3 + Math.floor((x + off) / 8);
		const base = ['#b5563f', '#a44c37', '#c0644a', '#ae5440'][Math.floor(rnd(id, 7, seed) * 4)];
		return J(base, x, y, 0.07, seed);
	});
}
bricks(S(), 1).done(flat).save(B('oven_bricks'));
function ovenFront(name, lit) {
	const s = bricks(S(), 2);
	const inArch = (x, y) => (y >= 10 && x >= 3 && x <= 12 && y <= 15) || (((x + 0.5 - 8) / 5.2) ** 2 + ((y + 0.5 - 10.5) / 3.2) ** 2 <= 1 && y < 11);
	s.fill((x, y) => {
		if (inArch(x, y)) {
			if (!lit) return y >= 14 ? '#4a3c38' : J('#24140f', x, y, 0.05, 3);
			const heat = (y - 7) / 8;
			if (y >= 14) return '#5a4038';
			const flame = rnd(x, y, 9) < heat * 0.9;
			return flame ? (rnd(x, y, 4) < 0.5 ? '#ffe27a' : '#ffb03a') : (heat > 0.5 ? '#ff7a2e' : '#8a2a14');
		}
		const nearArch = [[0, 1], [0, -1], [1, 0], [-1, 0], [1, 1], [-1, 1]].some(([dx, dy]) => inArch(x + dx, y + dy));
		if (nearArch && y >= 6) return J('#e3d2b8', x, y, 0.05, 5);
		return undefined;
	});
	if (lit) s.pxs([[5, 14], [8, 14], [10, 14]], '#ffe27a');
	s.done(flat).save(B(name));
}
ovenFront('oven_front', false);
ovenFront('oven_front_lit', true);
noise('oven_soot', '#2e2422', 0.12);

// ---------- Mixing bowl ----------
S().fill((x, y) => {
	if (y === 10) return '#f2a7c3';
	if (y === 11) return x % 3 === 1 ? '#f27a9a' : '#fbf6f0';
	return J('#f7f1ea', x, y, 0.025, 11);
}).done(flat).save(B('mixing_bowl'));
noise('mixing_bowl_inner', '#e9e0d6', 0.03);
noise('batter', '#f3dcae', 0.04, s => s.pxs([[4, 5], [10, 3], [7, 11], [12, 9], [3, 12]], '#fbeccc'));

// ---------- Pastry board ----------
S().fill((x, y) => {
	const plank = Math.floor(y / 4);
	const base = plank % 2 ? '#c99a63' : '#d4a66e';
	if (y % 4 === 0) return '#a87a48';
	return rnd(x, y, plank) < 0.15 ? '#b88a55' : J(base, x, y, 0.04, 2);
}).pxs([[3, 6], [4, 7], [11, 5], [12, 10], [6, 13], [9, 2]], '#f5ecd8').done(flat).save(B('pastry_board'));
noise('dough', '#f1d7a6', 0.04, s => s.pxs([[3, 4], [9, 7], [12, 12], [5, 11]], '#fbeccc'));

// ---------- Breads ----------
noise('bread_crust', '#c8843a', 0.09, s => {
	for (let i = 0; i < 3; i++) s.ln(3 + i * 4, 12, 6 + i * 4, 4, 0.9, '#e8b468');
});
noise('bread_bottom', '#9e5f28', 0.08);
noise('bread_crumb', '#f4e2bd', 0.05, s => s.pxs([[3, 4], [8, 6], [11, 10], [5, 12], [12, 3]], '#d9bf8f'));
noise('baguette_crust', '#d39446', 0.09, s => {
	for (let x = 2; x < 16; x += 4) s.ln(x, 11, x + 2, 5, 0.9, '#efc37a');
});
S().fill((x, y) => ((x + y) % 4 < 2 ? J('#d9933e', x, y, 0.05, 1) : J('#e8b060', x, y, 0.05, 2))).done(flat).save(B('croissant'));

// ---------- Cupcakes ----------
S().fill((x, y) => (x % 2 ? '#e58fb0' : '#f2a7c3')).done(flat).save(B('cupcake_wrapper'));
noise('sponge', '#e8b878', 0.06);
S().fill((x, y) => J('#f2f2f2', x, y, 0.03, 4)).pxs([[3, 3], [4, 3], [10, 6], [11, 6], [6, 11], [7, 11], [12, 12]], '#ffffff').done(flat).save(B('frosting'));
noise('cherry', '#d81f3c', 0.06, s => s.pxs([[5, 5], [6, 5]], '#ff8a9a'));

// ---------- Donut ----------
noise('donut_dough', '#d79a55', 0.06);
S().fill((x, y) => J('#f4f4f4', x, y, 0.02, 6)).pxs([[4, 4], [5, 4], [11, 9], [12, 9]], '#ffffff').done(flat).save(B('glaze'));

// ---------- Cookies / macarons / plate ----------
noise('cookie', '#d9a35c', 0.08, s => s.pxs([[4, 6], [5, 6], [10, 11], [11, 11]], '#b87f3c'));
noise('chip', '#f0f0f0', 0.03);
S().fill((x, y) => {
	const d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
	if (d > 5 && d <= 6) return '#9cc3e6';
	return J('#f3f6f9', x, y, 0.02, 8);
}).done(flat).save(B('plate'));
S().fill((x, y) => J('#f4f4f4', x, y, 0.025, 9)).fill((x, y) => (y >= 14 && (x % 2 === 0) ? '#dcdcdc' : undefined)).done(flat).save(B('macaron_shell'));
noise('macaron_filling', '#fff3e0', 0.03);

// ---------- Pie ----------
noise('pie_crust', '#d49a52', 0.08, s => s.fill((x, y) => ((x + y) % 3 === 0 && rnd(x, y, 3) < 0.4 ? '#e8b468' : undefined)));
S().fill((x, y) => J('#f0f0f0', x, y, 0.04, 10)).pxs([[3, 5], [9, 3], [12, 10], [6, 12], [10, 13]], '#c8c8c8').done(flat).save(B('pie_filling'));
S().fill((x, y) => (y >= 14 ? J('#c88a42', x, y, 0.06, 2) : y >= 12 ? J('#f2e2c8', x, y, 0.04, 3) : J('#d49a52', x, y, 0.06, 4))).done(flat).save(B('pie_inside'));

// ---------- Cake ----------
const cakeRows = (sponge) => S().fill((x, y) => {
	if (y === 11 || y === 14) return J('#fff8ee', x, y, 0.02, 1);
	return J(sponge, x, y, 0.05, 2);
});
cakeRows('#f3d39b').done(flat).save(B('cake_side'));
cakeRows('#f0cc90').pxs([[3, 10], [9, 12], [12, 9], [6, 13]], '#d9b070').done(flat).save(B('cake_inside'));
S().fill((x, y) => J('#f4f4f4', x, y, 0.02, 12)).pxs([[2, 2], [3, 2], [12, 4], [13, 4], [7, 12], [8, 12]], '#ffffff').done(flat).save(B('cake_frosting'));
noise('cake_bottom', '#e9c98f', 0.05);

// ---------- Toppings ----------
noise('topping_strawberry', '#e8344e', 0.06, s => s.pxs([[1, 1], [4, 2], [2, 4], [6, 5], [9, 1], [12, 3], [10, 6], [14, 6], [3, 8], [7, 9], [12, 10], [5, 13], [10, 14], [14, 12]], '#ffe08a'));
noise('leaf', '#58b84c', 0.07);
noise('topping_blueberry', '#4d63c9', 0.06, s => s.pxs([[2, 2], [7, 4], [11, 9], [4, 12]], '#8aa0f0'));
noise('topping_cream', '#fffaf3', 0.025);
S().fill((x, y) => {
	if (x < 1 || x > 14 || y < 1 || y > 14) return undefined;
	const r = rnd(x, y, 21);
	if (r < 0.16) return ['#ff6b8a', '#ffd84a', '#6bc6ff', '#8de07a', '#c69cff', '#ffffff'][Math.floor(rnd(x, y, 5) * 6)];
	return undefined;
}).done(flat).save(B('topping_sprinkles'));
S().fill((x, y) => {
	const wave = Math.round(8 + Math.sin(x * 0.9) * 4);
	if (x >= 1 && x <= 14 && (y === wave || y === (wave + 5) % 16 || y === wave - 5)) return '#5a3220';
	return undefined;
}).done(flat).save(B('topping_chocolate'));
noise('chocolate', '#5a3220', 0.06);
S().fill((x, y) => (x >= 1 && x <= 14 && y >= 1 && y <= 14 && rnd(Math.floor(x / 2), Math.floor(y / 2), 31) < 0.28 ? (rnd(x, y, 2) < 0.5 ? '#f27a9a' : '#f591ad') : undefined))
	.done(flat).save(B('topping_rose'));
noise('topping_lavender', '#9b7fd6', 0.08, s => s.pxs([[2, 2], [6, 5], [10, 9], [13, 13]], '#c6b0f0'));
noise('topping_star', '#ffd84a', 0.05, s => s.pxs([[3, 3], [11, 7], [6, 12]], '#ffffff'));

// ---------- Mug ----------
S().fill((x, y) => J('#f6efe8', x, y, 0.02, 14))
	.pxs([[6, 11], [8, 11], [5, 12], [6, 12], [7, 12], [8, 12], [9, 12], [6, 13], [7, 13], [8, 13], [7, 14]], '#f27a9a').done(flat).save(B('mug'));
noise('mug_inner', '#cfc5bb', 0.03);
S().fill((x, y) => J('#efefef', x, y, 0.03, 15)).pxs([[6, 6], [7, 6], [8, 7], [9, 8], [8, 9]], '#ffffff').done(flat).save(B('liquid'));

// ---------- Crops ----------
const LEAF = '#5aa846', LEAF2 = '#6fbf55', STEM = '#4a8a38';
function crop(name, stages) {
	stages.forEach((draw, i) => { const s = S(); draw(s); s.done({ outline: false, hi: 0.18, lo: 0.15 }).save(B(name + '_stage' + i)); });
}
const sprout = s => s.ln(7, 15, 7, 13, 1, STEM).ln(10, 15, 10, 14, 1, STEM).pxs([[6, 12], [8, 12], [9, 13], [11, 13]], LEAF2);
function bush(s, h, col) {
	for (let x = 2; x <= 13; x += 3) s.ln(x + 0.5, 16, x + 1.5, 16 - h, 1, STEM);
	for (let i = 0; i < 9; i++) s.el(2 + (i * 5) % 12 + 1, 16 - h + 1 + (i * 3) % Math.max(2, h - 2), 1.8, 1.3, i % 2 ? LEAF : (col || LEAF2), { rot: i * 40 });
}
crop('strawberry_crop', [sprout, s => bush(s, 6), s => { bush(s, 8); s.pxs([[4, 10], [10, 9], [7, 12]], '#ffffff').pxs([[4, 9], [10, 8]], '#ffe08a'); },
	s => { bush(s, 9); [[3, 11], [9, 10], [12, 13], [6, 13]].forEach(([x, y]) => s.el(x + 0.5, y + 0.5, 1.4, 1.7, '#e8344e').px(x, y - 1, '#58b84c')); }]);
crop('blueberry_crop', [sprout, s => bush(s, 7), s => { bush(s, 11); s.pxs([[4, 8], [9, 7], [12, 10], [6, 11]], '#a8c87a'); },
	s => { bush(s, 12); [[3, 7], [5, 8], [9, 6], [10, 7], [12, 10], [6, 11], [7, 12]].forEach(([x, y]) => s.el(x + 0.5, y + 0.5, 1, 1, '#4d63c9')); }]);
crop('tea_crop', [sprout, s => bush(s, 6, '#4f9a3c'), s => bush(s, 10, '#4f9a3c'),
	s => { bush(s, 12, '#4f9a3c'); s.pxs([[3, 4], [6, 5], [9, 4], [12, 6], [7, 3]], '#a8e07a'); }]);
crop('vanilla_crop', [sprout,
	s => s.ln(8, 16, 8, 6, 1, '#8a6a48').ln(8, 14, 6, 11, 1, STEM).el(5.5, 10.5, 1.5, 1, LEAF),
	s => { s.ln(8, 16, 8, 1, 1, '#8a6a48'); [[6, 12], [10, 9], [6, 6], [10, 3]].forEach(([x, y]) => s.el(x, y, 2, 1.2, LEAF, { rot: x < 8 ? 30 : -30 })); s.pxs([[9, 6], [7, 9]], '#fff5b0'); },
	s => { s.ln(8, 16, 8, 1, 1, '#8a6a48'); [[6, 12], [10, 9], [6, 6], [10, 3]].forEach(([x, y]) => s.el(x, y, 2, 1.2, LEAF, { rot: x < 8 ? 30 : -30 })); s.ln(10, 10, 11, 15, 1, '#6a8a3a').ln(5, 7, 4, 12, 1, '#6a8a3a').ln(11, 4, 12, 8, 1, '#4a2e1f'); }]);
function lavenderStage(s, h, bloom) {
	[[3, 0], [6, 1], [9, 0], [12, 1]].forEach(([x, o]) => {
		s.ln(x + 0.5, 16, x + 0.5 + o * 0.5, 16 - h, 1, STEM);
		if (bloom) for (let k = 0; k < 4; k++) s.el(x + 0.5 + o * 0.5, 16 - h + k * 1.3 + 0.5, 1, 0.8, k % 2 ? '#a383de' : bloom);
	});
	s.pxs([[2, 13], [5, 12], [10, 13], [13, 12]], LEAF);
}
crop('lavender_crop', [sprout, s => lavenderStage(s, 6), s => lavenderStage(s, 10, '#c6b0f0'), s => lavenderStage(s, 13, '#8d6ccf')]);
crop('rose_crop', [sprout, s => bush(s, 7, '#3f8a3a'), s => { bush(s, 10, '#3f8a3a'); s.pxs([[4, 7], [10, 6], [7, 9]], '#f27a9a'); },
	s => { bush(s, 12, '#3f8a3a'); [[4, 5], [10, 4], [7, 8], [12, 9]].forEach(([x, y]) => s.el(x + 0.5, y + 0.5, 1.8, 1.6, '#ec5a82').px(x, y, '#ffb0c4')); }]);
crop('starbloom_crop', [s => { sprout(s); s.px(7, 12, '#fff3b0'); },
	s => s.ln(8, 16, 8, 8, 1, '#7ab86a').el(6.5, 11, 1.6, 0.9, '#8fd07a', { rot: 30 }).el(9.5, 12, 1.6, 0.9, '#8fd07a', { rot: -30 }),
	s => { s.ln(8, 16, 8, 5, 1, '#7ab86a').el(6.5, 11, 1.6, 0.9, '#8fd07a', { rot: 30 }).el(9.5, 12, 1.6, 0.9, '#8fd07a', { rot: -30 }); s.el(8, 4.5, 1.6, 1.8, '#fff3b0'); },
	s => { s.ln(8, 16, 8, 7, 1, '#7ab86a').el(6.5, 11, 1.6, 0.9, '#8fd07a', { rot: 30 }).el(9.5, 12, 1.6, 0.9, '#8fd07a', { rot: -30 }); s.star(8, 5, 4.6, 2, '#ffd84a').px(7, 4, '#ffffff').pxs([[2, 2], [13, 3], [3, 9], [13, 10]], '#fff6c8'); }]);

// ---------- Baker villager overlay ----------
(() => {
	const s = S(64, 64);
	s.rc(40, 0, 47, 7, '#ffffff').rc(48, 0, 55, 7, '#f4f4f4');
	s.rc(32, 8, 63, 10, '#ffffff').rc(32, 11, 63, 11, '#f2a7c3');
	s.rc(7, 46, 12, 63, '#fbfbfb').rc(8, 52, 11, 54, '#f2a7c3').rc(6, 45, 13, 45, '#f2a7c3');
	s.rc(23, 26, 23, 37, '#fbfbfb').rc(28, 26, 28, 37, '#fbfbfb');
	s.done(flat).save('entity/villager/profession/baker').save('entity/zombie_villager/profession/baker');
})();

return window.BK.saved.length + ' saved total';
})()
