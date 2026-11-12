(function () {
const ART = window.ART, A = ART.A, M = ART.M;
const I = n => 'item/' + n;
const W = '#ffffff';
const NEUTRAL = { neutral: true };
const N = o => Object.assign({ neutral: true }, o || {});

// ================= Tools =================
A().ln(4.5, 11.5, 11.5, 4.5, 3.6, '#e2ae76', { grain: 0.05 })
	.ln(1.6, 14.4, 4.4, 11.6, 1.9, '#b5763f').ln(11.6, 4.4, 14.4, 1.6, 1.9, '#b5763f')
	.dec(M.or(M.ln(5.5, 11.5, 11.5, 5.5, 0.5)), '#e2ae76', { shift: -1 }).save(I('rolling_pin'));

(() => {
	const a = A().ln(2.2, 13.8, 6.8, 9.2, 2.3, '#f27fa6').el(2.5, 13.5, 1.4, 1.4, '#e05f8c');
	a.el(10.6, 5.4, 3.7, 5.6, '#d4dce6', { rot: 45, ring: 0.66, profile: 'soft' });
	a.el(10.6, 5.4, 1.9, 5.4, '#bfc9d6', { rot: 45, ring: 0.35, profile: 'soft' });
	a.ln(6.6, 9.4, 7.6, 8.4, 1.6, '#c0c8d2').save(I('whisk'));
})();

(() => {
	const a = A().ln(2, 14, 7.2, 8.8, 2.1, '#c98b55', { grain: 0.06 }).el(2.2, 13.8, 1.3, 1.3, '#a86a3a');
	a.ln(6.6, 9.4, 8.4, 7.6, 1.4, '#e8c37a');
	a.heart(10.8, 5.3, 4.3, '#f59ab8', { shine: 2 });
	a.dec(M.or(M.ln(9.2, 4.2, 10.2, 3.2, 0.7), M.ln(10.4, 5.6, 11.6, 4.4, 0.7), M.ln(11.6, 7, 12.8, 5.8, 0.7)), '#c9567e', { lvl: 1 });
	a.save(I('sweetheart_spatula'));
})();

(() => {
	const a = A().pg([[2.5, 3], [12.5, 2], [13.5, 13], [3.5, 14.5]], '#f7f1e6', { profile: 'flat', flatLvl: 3 });
	a.pg([[2, 2.5], [12, 1.5], [12.5, 12.5], [2.5, 13.8]], '#e8789f', { profile: 'flat' });
	a.heart(7.3, 6.8, 2.6, '#fff1f5', { shine: 1 });
	a.dec(M.ln(2.6, 3, 2.9, 13.4, 0.8), '#c95680', { lvl: 1 });
	a.dec(M.or(M.ln(4.6, 10.8, 10.2, 10.2, 0.6), M.ln(4.8, 12, 9.2, 11.6, 0.6)), '#fff1f5', { lvl: 4 });
	a.save(I('bakery_guide'));
})();

// ================= Seed packets =================
function packet(name, band, draw) {
	const a = A().rc(3, 2, 12, 14, '#f7efe0', { profile: 'flat' }).rc(3, 2, 12, 4, band, { profile: 'flat' });
	a.dec(M.or(M.pts([[3, 4], [5, 4], [7, 4], [9, 4], [11, 4]])), band, { lvl: 1 });
	a.dec(M.ln(3, 13.6, 13, 13.6, 0.5), '#d8c8ae', { lvl: 2 });
	draw(a);
	a.save(I(name));
}
packet('strawberry_seeds', '#f27a9a', a => a.add(M.or(M.el(7.8, 9, 2.7, 2.4), M.pg([[5.2, 9.3], [10.4, 9.3], [7.8, 12.6]])), '#e8324e', { outline: false, shine: 1 })
	.pg([[5.6, 7.2], [7.8, 6.2], [10, 7.2], [7.8, 7.8]], '#5cbf4e', { outline: false }).dots([[7, 9], [9, 10], [8, 11]], '#ffe7a0', { lvl: 4 }));
packet('blueberry_seeds', '#7f8fe6', a => { [[6.3, 10], [9.5, 10.2], [7.9, 7.6]].forEach(([x, y]) => a.el(x, y, 1.8, 1.8, '#4d63c9', { outline: false, shine: 1 })); });
packet('raspberry_seeds', '#ec4f78', a => { for (let i = 0; i < 6; i++) a.el(6.4 + (i % 3) * 1.6, 8 + Math.floor(i / 3) * 1.8 + (i % 2) * 0.4, 1.1, 1.1, '#e0405e', { outline: false, shine: 1 }); a.el(8, 12, 1.1, 1.1, '#e0405e', { outline: false }); });
packet('tea_seeds', '#7fbf6a', a => a.el(6.6, 9.5, 2.6, 1.4, '#4f9a3c', { rot: -35, outline: false }).el(9.6, 10, 2.6, 1.4, '#6fbf4a', { rot: 30, outline: false }));
packet('mint_seeds', '#7fdcb0', a => a.el(6.5, 9.4, 2, 2.3, '#4fc07e', { rot: -20, outline: false }).el(9.6, 9.6, 2, 2.3, '#62d090', { rot: 20, outline: false }).ln(8, 12.5, 8, 9, 0.7, '#3f8f3a', { outline: false }));
packet('vanilla_seeds', '#f3d98a', a => a.el(7.8, 9.2, 2.6, 2.2, '#fff4d0', { outline: false }).el(7.8, 9.2, 0.9, 0.9, '#f2c94c', { outline: false }).ln(5, 12.6, 10.5, 11.2, 0.9, '#5a3826', { outline: false }));
packet('lavender_seeds', '#b79ae6', a => { for (let k = 0; k < 4; k++) a.el(7.8 + (k % 2 ? 0.6 : -0.6), 7 + k * 1.3, 1, 0.8, k % 2 ? '#a383de' : '#8d6ccf', { outline: false }); a.ln(7.8, 12.6, 7.8, 10.5, 0.7, '#5d9a46', { outline: false }); });
packet('rose_seeds', '#f591ad', a => a.el(7.8, 9, 2.8, 2.6, '#e8487a', { outline: false }).dec(M.el(7.8, 9, 1.5, 1.2, 0, 0.4), '#e8487a', { shift: -2 }).ln(7.8, 12.8, 7.8, 11.3, 0.7, '#3f8a3a', { outline: false }));
packet('pineapple_seeds', '#ffd25a', a => a.el(7.8, 10.2, 2.3, 2.6, '#e8b33a', { outline: false }).pg([[6.4, 8], [7.2, 5.6], [7.8, 7.4], [8.6, 5.4], [9.3, 8]], '#4fae47', { outline: false }));
packet('starbloom_seeds', '#8a7fe0', a => { a.star(7.8, 9.2, 3.2, 1.4, '#ffd84a', { outline: false, shine: 1 }); a.pxs([[5, 6], [11, 7], [10, 12]], '#ffffff'); });

// ================= Harvest =================
A().add(M.or(M.el(8, 9.2, 5.3, 4.6), M.pg([[2.8, 9.6], [13.2, 9.6], [8, 15.4]])), '#e8324e', { shine: 2 })
	.dots([[6, 8], [10, 8], [8, 10], [5, 11], [11, 11], [7, 13], [9, 13], [8, 7], [4, 9], [12, 9]], '#ffe7a0', { lvl: 4 })
	.pg([[3.5, 5.6], [6, 3.8], [8, 5.2], [10, 3.8], [12.5, 5.6], [10.5, 6.9], [8, 6.1], [5.5, 6.9]], '#5cbf4e').ln(8, 0.8, 8.8, 4.6, 1.2, '#3f8f3a')
	.save(I('strawberry'));
(() => {
	const a = A();
	[[5.4, 10.6], [10.6, 10.4], [8, 6.2]].forEach(([x, y]) => a.el(x, y, 3.3, 3.2, '#4d63c9', { shine: 1 }));
	a.dots([[5, 13], [6, 13], [10, 13], [11, 13], [8, 9], [7, 9]], '#2c356e', { lvl: 0 });
	a.pg([[9.5, 3.2], [13.5, 1], [14.5, 3.4], [11.5, 5]], '#5cbf4e').save(I('blueberry'));
})();
(() => {
	const a = A().pg([[5, 4.5], [8, 3.5], [11, 4.5], [8, 5.6]], '#5cbf4e');
	const spots = [[6, 6.5], [8, 6.2], [10, 6.5], [5.2, 8.6], [7.1, 8.4], [9, 8.4], [10.9, 8.6], [6, 10.6], [8, 10.6], [10, 10.6], [6.9, 12.5], [9.1, 12.5], [8, 14.2]];
	spots.forEach(([x, y]) => a.el(x, y, 1.3, 1.25, '#e0405e', { shine: 1, contrast: 5 }));
	a.save(I('raspberry'));
})();
A().pg([[2, 13.5], [4.5, 6.5], [10, 2], [9.5, 8], [4.5, 13]], '#6fbf4a').pg([[6, 14.5], [9, 9], [14.5, 6], [13.5, 11.5], [8.5, 14.5]], '#86cf5c')
	.dec(M.or(M.ln(2.5, 13.6, 9.6, 2.8, 0.8), M.ln(6.4, 14.4, 14, 6.4, 0.8)), '#6fbf4a', { shift: -2 }).save(I('tea_leaves'));
(() => {
	const a = A().ln(8, 15, 8, 6, 1, '#3f8f3a');
	a.el(5.2, 9.5, 3, 2.4, '#4fc07e', { rot: -35 }).el(10.8, 9.2, 3, 2.4, '#58c888', { rot: 35 }).el(8, 4.6, 2.6, 3.2, '#62d090');
	a.dec(M.or(M.ln(3, 11, 7, 8.5, 0.6), M.ln(13, 10.8, 9, 8.3, 0.6), M.ln(8, 7.5, 8, 2, 0.6)), '#4fc07e', { shift: -2 });
	a.save(I('mint_leaves'));
})();
(() => {
	const a = A().ln(2.6, 14, 11.6, 4.6, 2.2, '#4a2e1f', { shine: 1 }).ln(4.6, 14.6, 13.6, 5.4, 2.2, '#5a3826', { shine: 1 });
	a.el(12.6, 3.4, 2.4, 2.2, '#fff4d6').el(12.4, 3.2, 1, 1, '#f2c94c', { outline: false });
	a.ln(2, 10.5, 6, 13.6, 1.1, '#f27fa6').save(I('vanilla_pod'));
})();
(() => {
	const a = A();
	[[5, 0.8], [8, 0], [11, -0.8]].forEach(([x, lean]) => a.ln(x, 15, x + lean * 2, 7, 1, '#5d9a46'));
	[[5, 0.8, 7], [8, 0, 4], [11, -0.8, 6.5]].forEach(([x, lean, top]) => {
		for (let k = 0; k < 5; k++) a.el(x + lean * 2 * (1 - k / 5) + (k % 2 ? 0.5 : -0.5), top + k * 1.25, 1.2, 0.9, k % 2 ? '#a383de' : '#8d6ccf');
	});
	a.rc(5, 11.5, 11, 12.5, '#f27fa6', { profile: 'flat' }).save(I('lavender'));
})();
A().heart(5.8, 6, 3.3, '#f27a9a', { shine: 1 }).heart(10.6, 8.6, 3.3, '#f591ad', { shine: 1 }).heart(6.4, 11.8, 3, '#ec5a86', { shine: 1 }).save(I('rose_petals'));
(() => {
	const a = A();
	a.pg([[5.5, 6], [4.5, 1.5], [7, 4.5], [8, 0.5], [9, 4.5], [11.5, 1.5], [10.5, 6]], '#4fae47');
	a.el(8, 10.8, 4.4, 4.8, '#e8b33a', { shine: 1 });
	const lattice = (x, y) => (Math.round(x + y) % 3 === 0) || (Math.round(x - y + 30) % 3 === 0);
	a.dec(M.and(M.el(8, 10.8, 4.1, 4.5), lattice), '#e8b33a', { shift: -2 });
	a.save(I('pineapple'));
})();
A().star(8, 8.6, 6.9, 3, '#ffd84a', { shine: 2 }).pxs([[2, 2], [14, 3], [13, 14], [2, 13]], '#fff6c8').pxs([[2, 1], [1, 2], [3, 2], [2, 3]], '#ffeaa0').save(I('star_petals'));
(() => {
	const a = A().el(7, 7.4, 5.4, 5.2, '#7a4e2e', { grain: 0.25 });
	a.dots([[5, 5], [7, 4], [6, 6]], '#3a2216', { lvl: 0 });
	a.el(10.5, 11.6, 4.4, 3.4, '#8a5a36', { grain: 0.2 }).el(10.5, 10.6, 3.4, 1.8, '#fbf6ec', { outline: false, profile: 'soft' });
	a.save(I('coconut'));
})();
A().add(M.or(M.el(8, 9, 5.2, 4.2, -25), M.el(3.5, 11.4, 1.2, 1, -25), M.el(12.5, 6.6, 1.2, 1, -25)), '#ffd93d', { shine: 2, grain: 0.05 })
	.el(11.6, 3.4, 2.4, 1.2, '#5cbf4e', { rot: -30 }).save(I('lemon'));

// ================= Pantry =================
(() => {
	const a = A().pg([[3, 7], [5, 5], [11, 5], [13, 7], [13.5, 14.5], [2.5, 14.5]], '#efe3cb', { grain: 0.05 });
	a.pg([[5, 5], [6, 1.5], [10, 1.5], [11, 5]], '#e6d7b8');
	a.rc(5, 4, 11, 4, '#c9a06b', { profile: 'flat' });
	a.el(8, 1.7, 2.6, 1, '#ffffff', { outline: false });
	a.dec(M.or(M.ln(8, 13, 8, 8.5, 0.6), M.el(7, 10, 0.8, 0.5), M.el(9, 9.5, 0.8, 0.5), M.el(7, 11.5, 0.8, 0.5), M.el(9, 11, 0.8, 0.5)), '#d9a94a', { lvl: 3 });
	a.save(I('flour'));
})();
(() => {
	const a = A().el(8, 11.5, 7, 3, '#bfe0f0');
	a.pg([[3, 8], [8, 5.5], [13, 7.5], [8, 10]], '#fff3a8', { profile: 'flat', flatLvl: 4 });
	a.pg([[3, 8], [8, 10], [8, 13], [3, 11]], '#f7dc6f', { profile: 'flat', flatLvl: 3 });
	a.pg([[8, 10], [13, 7.5], [13, 10.5], [8, 13]], '#f7dc6f', { profile: 'flat', flatLvl: 2 });
	a.dec(M.ln(4, 8, 7.5, 6.4, 0.6), '#ffffff', { lvl: 5 });
	a.save(I('butter'));
})();
(() => {
	const a = A().pg([[3, 11], [13, 11], [11.5, 15], [4.5, 15]], '#f49ac0', { profile: 'flat' });
	a.el(8, 10.4, 5.2, 2.2, '#fffaf3', N()).el(8, 8, 4.1, 2, '#fffaf3', N()).el(8, 5.8, 2.8, 1.7, '#fffaf3', N()).el(8.6, 3.9, 1.3, 1.3, '#fffaf3', N());
	a.save(I('whipped_cream'));
})();
(() => {
	const a = A().pg([[2, 6.5], [9, 2.5], [14, 9.5], [7, 13.5]], '#6b3a24', { profile: 'flat' });
	const grid = (x, y) => { const u = (x - y * 0.55), v = (x + y * 1.7); return Math.abs((u % 3.2 + 3.2) % 3.2) < 0.55 || Math.abs((v % 4.2 + 4.2) % 4.2) < 0.6; };
	a.dec(M.and(M.pg([[2, 6.5], [9, 2.5], [14, 9.5], [7, 13.5]]), grid), '#6b3a24', { shift: 1 });
	a.pg([[4.5, 11], [11, 7.2], [14, 9.5], [7, 13.5]], '#f27fa6', { profile: 'flat' });
	a.dec(M.ln(4.6, 10.8, 11, 7, 0.7), '#ffd84a', { lvl: 4 });
	a.save(I('chocolate_bar'));
})();
(() => {
	const a = A().el(8, 11, 6.2, 4, '#3f6f6a').cut(M.rc(0, 0, 15, 8));
	a.el(8, 9, 6.2, 1.8, '#2c4f4a');
	a.el(8, 8.6, 4.6, 2.4, '#8fbf4d', { grain: 0.1 });
	a.ln(11, 2.5, 14, 6.5, 1.1, '#d9b07a').el(10.8, 2.3, 1.2, 0.8, '#d9b07a');
	a.save(I('matcha_powder'));
})();
(() => {
	const a = A().el(8, 11.4, 6.2, 3.6, '#8a5a36', { grain: 0.15 }).cut(M.rc(0, 0, 15, 9));
	a.el(8, 9.6, 6, 1.8, '#5a3826');
	[[5, 8.6], [7, 8], [9, 8.4], [11, 8.8], [6, 9.6], [8.5, 9.4], [10.4, 9.8], [7.5, 7.2]].forEach(([x, y], i) => a.el(x, y, 1.4, 0.7, '#fbf6ec', { rot: i * 37, outline: false, profile: 'soft' }));
	a.save(I('coconut_flakes'));
})();
(() => {
	const a = A().rc(4, 5, 11, 14, '#dceef6cc', { profile: 'flat', flatLvl: 4 }).rc(4, 2, 11, 4, '#f27fa6', { profile: 'flat' });
	a.dots([[6, 3], [8, 3], [10, 3]], '#ffffff', { lvl: 5 });
	const cols = ['#ff6b8a', '#ffd84a', '#6bc6ff', '#8de07a', '#c69cff', '#ffffff'];
	let k = 0;
	for (let y = 7; y <= 13; y++) for (let x = 5; x <= 10; x++) if ((x * 7 + y * 3) % 4 === 0) a.px(x, y, cols[k++ % cols.length]);
	a.pxs([[5, 6], [5, 7], [5, 8]], '#ffffffcc');
	a.save(I('sprinkles'));
})();
A().heart(5.8, 6.4, 3.1, '#f27a9a', { shine: 1 }).heart(10.6, 8.8, 3.1, '#c69cff', { shine: 1 }).heart(6.4, 12, 2.8, '#f591ad', { shine: 1 })
	.pxs([[5, 5], [7, 6], [10, 8], [11, 10], [6, 12], [12, 4], [3, 3], [13, 13]], '#ffffff').save(I('candied_petals'));
function jar(name, fill, lid, extra) {
	const a = A().rc(4, 5, 11, 14, '#d6eef6aa', { profile: 'flat', flatLvl: 4 });
	if (fill) a.rc(5, 7, 10, 13, fill, { shine: 1, profile: 'soft' });
	a.rc(4, 2, 11, 4, lid, { profile: 'flat' });
	if (extra) extra(a);
	a.pxs([[5, 6], [5, 7]], '#ffffffcc');
	a.save(I(name));
}
jar('glass_jar', null, '#c9a06b', a => a.dec(M.ln(4.5, 3, 11.5, 3, 0.5), '#c9a06b', { shift: -1 }));
const gingham = a => a.dots([[4, 2], [6, 2], [8, 2], [10, 2], [5, 3], [7, 3], [9, 3], [11, 3], [4, 4], [6, 4], [8, 4], [10, 4]], '#ffffff', { lvl: 5 });
jar('strawberry_jam', '#c8243c', '#e8486a', a => { gingham(a); a.rc(6, 9, 9, 11, '#fff5e6', { profile: 'flat', outline: false }).heart(7.6, 10, 1.2, '#e8324e', { outline: false }); });
jar('blueberry_jam', '#39408f', '#7a8de0', a => { gingham(a); a.rc(6, 9, 9, 11, '#fff5e6', { profile: 'flat', outline: false }).heart(7.6, 10, 1.2, '#4d63c9', { outline: false }); });
jar('raspberry_jam', '#b8204a', '#ec4f78', a => { gingham(a); a.rc(6, 9, 9, 11, '#fff5e6', { profile: 'flat', outline: false }).heart(7.6, 10, 1.2, '#e0405e', { outline: false }); });
A().pg([[2, 5], [12, 3], [14, 11], [4, 13]], '#f6e7b2', { profile: 'flat' }).pg([[8, 4], [12, 3], [14, 11], [10, 12]], '#ecd89c', { profile: 'flat' })
	.dots([[5, 7], [6, 10], [11, 6], [12, 9]], '#e8b84a', { lvl: 3 }).save(I('wax_paper'));
A().pg([[2, 5], [12, 3], [14, 11], [4, 13]], '#c4e8f6', { profile: 'flat' }).pg([[8, 4], [12, 3], [14, 11], [10, 12]], '#afdcee', { profile: 'flat' })
	.pxs([[6, 6], [6, 8], [5, 7], [7, 7], [6, 7], [11, 8], [10, 8], [12, 8], [11, 7], [11, 9], [4, 10]], '#ffffff').save(I('frost_wrap'));

// ================= Drinks =================
function mug(name, liquid, extra) {
	const a = A().el(12.3, 9.8, 2.8, 3, '#f8f1ea', { ring: 0.42 });
	a.add(M.or(M.rc(2, 6, 11, 13), M.el(6.5, 13.4, 4.5, 1.4), M.el(6.5, 6, 4.5, 2)), '#f8f1ea', { profile: 'soft' });
	a.el(6.5, 6, 3.6, 1.4, liquid || '#b8aca2', { profile: 'flat', flatLvl: 3, outline: false });
	a.heart(6.6, 10.2, 1.7, '#f27fa6', { outline: false });
	if (extra) extra(a);
	a.save(I(name));
}
mug('mug', null);
const steam = a => a.pxs([[5, 2], [6, 1], [8, 2], [9, 0], [7, 3]], '#ffffff99');
mug('matcha_latte', '#8fc25a', a => { steam(a); a.heart(6.5, 6, 1, '#f7f2e0', { outline: false }); });
mug('hot_chocolate', '#6a3b22', a => { steam(a); a.el(5, 5.6, 1.2, 0.9, '#ffffff', { outline: false }).el(8, 5.8, 1.2, 0.9, '#fff1f5', { outline: false }); });
mug('strawberry_milk', '#f7afc6', a => a.el(10.6, 4.4, 1.8, 1.6, '#e8324e', { shine: 1 }).pg([[9.6, 3.2], [10.6, 2.2], [11.6, 3.2]], '#5cbf4e'));
function teacup(name, liquid, extra) {
	const a = A().el(8, 13.4, 6.5, 1.8, '#fbf3f6');
	a.el(13, 9.5, 2, 2, '#fbf3f6', { ring: 0.4 });
	a.add(M.and(M.el(7.6, 8.4, 5.4, 5), M.rc(0, 7, 15, 15)), '#fbf3f6', { profile: 'soft' });
	a.el(7.6, 7.6, 5.2, 1.6, liquid, { profile: 'flat', flatLvl: 3, outline: false });
	a.dec(M.ln(3, 10, 12.2, 10, 0.6), '#f27fa6', { lvl: 3 });
	if (extra) extra(a);
	a.save(I(name));
}
teacup('lavender_tea', '#b89bdd', a => { steam(a); a.ln(10.5, 7, 13, 2.5, 0.8, '#5d9a46').dots([[13, 2], [12, 3], [13, 4]], '#9b7fd6', { lvl: 3 }); });
teacup('rose_tea', '#ec8ea6', a => { steam(a); a.heart(6.6, 7.4, 1.2, '#f27a9a', { outline: false }); });
teacup('starlight_tea', '#ffe79a', a => { a.star(7.4, 3, 2.4, 1, '#ffd84a', { shine: 1 }); a.pxs([[12, 2], [2, 4]], '#ffffff'); });
teacup('mint_tea', '#9fddb5', a => { steam(a); a.el(10.5, 6.6, 1.6, 1, '#4fc07e', { rot: 30 }); });
function glass(name, liquid, extra) {
	const a = A().pg([[4, 3], [12, 3], [11, 15], [5, 15]], '#e6f4f8aa', { profile: 'flat', flatLvl: 4 });
	a.pg([[4.6, 6], [11.4, 6], [10.6, 14.4], [5.4, 14.4]], liquid, { profile: 'soft', outline: false });
	a.ln(9, 6.5, 12.5, 0.8, 0.9, '#f27fa6');
	if (extra) extra(a);
	a.pxs([[5, 7], [5, 8], [5, 9]], '#ffffffaa');
	a.save(I(name));
}
glass('lemonade', '#fff29a', a => a.el(4.4, 4.2, 2.4, 2.4, '#ffd93d').dec(M.el(4.4, 4.2, 1.5, 1.5, 0, 0.5), '#fff6c0', { lvl: 5 }));
glass('pineapple_smoothie', '#ffd66b', a => a.pg([[3, 5.5], [5, 2], [7, 5.5]], '#e8b33a').pg([[4, 2.6], [4.6, 0.6], [5.6, 2.4]], '#4fae47'));
(() => {
	const a = A().el(8, 10.6, 6.4, 4.6, '#7a4e2e', { grain: 0.25 }).cut(M.rc(0, 0, 15, 7));
	a.el(8, 8, 6.3, 1.9, '#fbf6ec', { profile: 'soft' }).el(8, 8.1, 4.8, 1.3, '#f8f4ec', { outline: false });
	a.ln(9.5, 8, 12.5, 1.5, 0.9, '#f27fa6');
	a.pg([[9, 3.5], [12.5, 1.5], [15, 4]], '#ff8fb5').ln(12.3, 2.2, 12.6, 5, 0.6, '#c98b55');
	a.save(I('coconut_milk'));
})();

// ================= Dough =================
function doughBall(name, col, extra) {
	const a = A().add(M.or(M.el(8, 10.6, 6.4, 4.2), M.el(7.6, 8.6, 4.6, 3.2)), col, { contrast: 6 });
	a.dots([[5, 8], [10, 7], [8, 12], [12, 11], [4, 11]], '#ffffff', { lvl: 5 });
	if (extra) extra(a);
	a.save(I(name));
}
doughBall('bread_dough', '#f0d39e');
doughBall('sweet_dough', '#f9e3c2', a => a.heart(10.4, 8, 1, '#f7b6cf', { outline: false }));
(() => {
	const a = A().rc(3, 9, 13, 13, '#f3dab0', { profile: 'flat' }).rc(4, 6, 12, 9, '#f7e3bf', { profile: 'flat' }).rc(5, 4, 11, 6, '#fbecd2', { profile: 'flat' });
	a.save(I('pastry_dough'));
})();
doughBall('cookie_dough', '#e2b47a', a => a.dots([[5, 9], [9, 8], [11, 11], [7, 12], [6, 11], [10, 10]], '#5a3220', { lvl: 1 }));
A().pxs([[6, 9], [9, 7], [10, 11], [5, 12], [8, 11], [12, 10]], '#ffffff').save(I('dough_flavor'));

// ================= Bakes =================
const BAKED = { crust: '#cd8238', top: '#d8913f', score: '#f4c47c', side: '#b8692b' };
const RAW = { crust: '#f1d6a6', top: '#f6e1bb', score: '#fff4dc', side: '#e8c78f' };
function loaf(name, p) {
	A().el(8, 10.4, 7.2, 4, p.side, { grain: 0.06 }).el(8, 8.6, 6.4, 4.4, p.top, { grain: 0.06 })
		.dec(M.or(M.ln(4.6, 9.8, 6.6, 6.2, 0.9), M.ln(7.6, 10.2, 9.6, 6.2, 0.9), M.ln(10.6, 9.8, 12.2, 6.8, 0.9)), p.score, { shift: 1 })
		.save(I(name));
}
loaf('bread_loaf', BAKED);
loaf('raw_loaf', RAW);
function baguette(name, p) {
	const a = A().ln(2.4, 13.6, 13.6, 2.4, 4.4, p.top, { grain: 0.05 });
	[[4.8, 10.6], [7.6, 7.8], [10.4, 5]].forEach(([x, y]) => a.dec(M.ln(x - 1.3, y - 0.2, x + 0.3, y - 1.8, 0.9), p.score, { shift: 1 }));
	a.save(I(name));
}
baguette('baguette', BAKED);
baguette('raw_baguette', RAW);
function croissant(name, p) {
	const a = A();
	a.el(2.8, 11.6, 2, 1.6, p.top, { rot: 30 }).el(13.2, 11.6, 2, 1.6, p.top, { rot: -30 });
	a.el(5, 9.6, 2.6, 2.4, p.crust).el(11, 9.6, 2.6, 2.4, p.crust);
	a.el(8, 8.4, 3.2, 3.4, p.top, { shine: 1 });
	a.save(I(name));
}
croissant('croissant', BAKED);
croissant('raw_croissant', RAW);

function wrapper(a) {
	a.pg([[3.5, 9], [12.5, 9], [11.3, 15], [4.7, 15]], '#f49ac0', { profile: 'flat' });
	a.dec(M.or(M.ln(5.8, 9.4, 6.2, 15, 0.6), M.ln(8, 9.4, 8, 15, 0.6), M.ln(10.2, 9.4, 9.8, 15, 0.6)), '#f49ac0', { shift: -2 });
}
(() => { const a = A(); wrapper(a); a.el(8, 9, 4.6, 1.6, '#e8b070'); a.el(8.4, 2.6, 1.3, 1.3, '#e8324e', { shine: 1 }); a.save(I('cupcake')); })();
A().el(8, 8.2, 5.4, 2.4, '#f4f4f4', N()).el(8, 6.3, 4, 2, '#f4f4f4', N()).el(8, 4.6, 2.5, 1.5, '#f4f4f4', N()).save(I('cupcake_frosting'));
(() => { const a = A(); wrapper(a); a.el(8, 8.2, 4.4, 2.4, '#f9e6c6'); a.save(I('raw_cupcake')); })();

function donut(name, col) { A().el(8, 8.8, 6.8, 5.6, col, { grain: 0.04 }).cut(M.el(8, 8.4, 2, 1.5)).save(I(name)); }
donut('donut', '#d6964f');
donut('raw_donut', '#f1d6a6');
A().add(M.or(M.el(8, 8.2, 5.8, 4.4), M.el(3.4, 10.4, 0.9, 1.6), M.el(12.4, 11, 0.9, 1.4), M.el(6.4, 12.4, 0.9, 1.2)), '#f4f4f4', N({ shine: 1 }))
	.cut(M.el(8, 8.2, 2.7, 2.1)).save(I('donut_glaze'));

function cookies(name, col) {
	const a = A().el(5.4, 10.2, 4.2, 3.6, col, { grain: 0.08 }).el(10.8, 10.8, 4.2, 3.6, col, { grain: 0.08 }).el(8, 6, 4.2, 3.4, col, { grain: 0.08 });
	a.dec(M.or(M.ln(4, 9.5, 6, 10.5, 0.5), M.ln(10, 11, 12, 10, 0.5), M.ln(7, 5.5, 9, 6.5, 0.5)), col, { shift: -2 });
	a.save(I(name));
}
cookies('cookies', '#d9a35c');
cookies('raw_cookies', '#ecd0a0');
A().pxs([[4, 9], [6, 11], [10, 10], [12, 12], [7, 5], [9, 6], [11, 9], [5, 8], [8, 4], [3, 11]], '#efefef').save(I('cookies_chips'));

(() => {
	const a = A().rc(4, 6, 12, 6, '#fff3e0', { profile: 'flat', outline: false }).rc(3, 12, 11, 12, '#fff3e0', { profile: 'flat', outline: false });
	a.save(I('macarons'));
})();
A().el(8, 5, 4.8, 1.9, '#f4f4f4', N({ shine: 1 })).el(8, 7.6, 4.8, 1.4, '#e8e8e8', N()).el(7, 11, 4.8, 1.9, '#f4f4f4', N({ shine: 1 })).el(7, 13.6, 4.8, 1.4, '#e8e8e8', N())
	.cut(M.rc(4, 6, 12, 6)).cut(M.rc(3, 12, 11, 12)).save(I('macarons_shells'));
A().el(8, 5, 4.8, 1.9, '#f6e1bb').el(8, 7.6, 4.8, 1.4, '#efd3a0').el(7, 11, 4.8, 1.9, '#f6e1bb').el(7, 13.6, 4.8, 1.4, '#efd3a0').save(I('raw_macarons'));

(() => {
	const a = A().el(8, 14, 7.4, 1.6, '#e6eef6');
	a.pg([[2, 8], [13.5, 8], [13.5, 13.4], [2, 13.4]], '#f3d39b', { profile: 'flat' });
	a.dec(M.or(M.rc(2, 10, 13, 10), M.rc(2, 12, 13, 12)), '#fff6ea', { lvl: 4 });
	a.el(8, 3.8, 1.4, 1.4, '#e8324e', { shine: 1 });
	a.save(I('cake'));
})();
A().add(M.or(M.el(7.8, 7.4, 6.2, 2.6), M.rc(2, 7, 13, 8), M.el(3.6, 9.4, 0.8, 1.4), M.el(7, 9.6, 0.8, 1.6), M.el(10.6, 9.2, 0.8, 1.2)), '#f4f4f4', N())
	.el(7.8, 5.2, 1.6, 0.9, '#ffffff', N({ outline: false })).save(I('cake_frosting'));
(() => {
	const a = A().el(8, 14, 7.4, 1.6, '#e6eef6');
	a.pg([[2, 8], [13.5, 8], [13.5, 13.4], [2, 13.4]], '#f6e1bb', { profile: 'flat' }).el(7.8, 7.8, 6, 2.4, '#f9e8c8');
	a.save(I('raw_cake'));
})();

(() => {
	const a = A().el(8, 10.4, 7.2, 4.2, '#c9c9d4');
	a.el(8, 9.4, 6.8, 3.6, '#d9a052', { grain: 0.06 });
	a.dec(M.or(M.ln(5, 6.5, 5, 12, 0.9), M.ln(8, 6, 8, 12.6, 0.9), M.ln(11, 6.5, 11, 12, 0.9), M.ln(2.5, 8.4, 13.5, 8.4, 0.9), M.ln(2.5, 10.6, 13.5, 10.6, 0.9)), '#efc27a', { shift: 1 });
	a.save(I('pie'));
})();
(() => {
	const lattice = M.or(M.ln(5, 6.5, 5, 12, 1.2), M.ln(8, 6, 8, 12.6, 1.2), M.ln(11, 6.5, 11, 12, 1.2), M.ln(2.5, 8.4, 13.5, 8.4, 1.2), M.ln(2.5, 10.6, 13.5, 10.6, 1.2));
	A().add(M.and(M.el(8, 9.4, 5.8, 2.8), M.not(lattice)), '#f2f2f2', N({ outline: false, profile: 'soft' })).save(I('pie_filling'));
})();
(() => {
	const a = A().el(8, 10.4, 7.2, 4.2, '#c9c9d4').el(8, 9.4, 6.8, 3.6, '#f1d6a6');
	a.dec(M.or(M.ln(4, 7.5, 12, 11.5, 1), M.ln(4, 11.5, 12, 7.5, 1)), '#fff4dc', { shift: 1 });
	a.save(I('raw_pie'));
})();
A().el(8, 10, 6, 4, '#2b2220', { grain: 0.2 }).el(6, 8, 3, 2.5, '#3a2e2a').el(10.5, 8.5, 3, 2.5, '#322724')
	.pxs([[6, 11], [10, 10], [8, 12]], '#ff7a2e').pxs([[7, 4], [8, 3], [9, 2], [6, 2]], '#9a9a9aaa').save(I('burnt_pastry'));

return window.ART.saved.length + ' item textures';
})()
