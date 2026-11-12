(function () {
const BK = window.BK;
const { S } = BK;
const I = n => 'item/' + n;

// ---------- Tools ----------
S().ln(4.5, 11.5, 11.5, 4.5, 3.4, '#e0ad74').ln(1.8, 14.2, 4.2, 11.8, 1.8, '#a8713f').ln(11.8, 4.2, 14.2, 1.8, 1.8, '#a8713f')
	.pxs([[6, 9], [9, 6]], '#f3cf9e!').save(I('rolling_pin'));
(() => {
	const s = S().ln(2, 14, 7, 9, 2.2, '#c27a8f').ln(2.2, 13.8, 3.5, 12.5, 2.4, '#a5566d');
	s.el(10.8, 5.2, 3.6, 5.4, '#d9dee6', { rot: 45, ring: 0.62 }).el(10.8, 5.2, 1.6, 5.2, '#c3cad4', { rot: 45, ring: 0.3 });
	s.pxs([[7, 9], [8, 8]], '#9aa3ae!').save(I('whisk'));
})();

// ---------- Seeds ----------
function seeds(name, a, b, extra) {
	const s = S().el(5, 10, 2, 1.5, a, { rot: -30 }).el(10, 11, 2, 1.5, b, { rot: 20 }).el(8, 6, 2, 1.5, a, { rot: 60 });
	if (extra) extra(s);
	s.save(I(name));
}
seeds('strawberry_seeds', '#f0d27a', '#e8b860');
seeds('blueberry_seeds', '#8c7a5c', '#a08a68');
seeds('tea_seeds', '#7a5a3c', '#8f6c48');
seeds('vanilla_seeds', '#3d2a20', '#4e3628');
seeds('lavender_seeds', '#6f5a7c', '#836c92');
seeds('rose_seeds', '#a05a48', '#b8705a');
seeds('starbloom_seeds', '#ffd84a', '#ffe680', s => s.pxs([[12, 4], [3, 4], [13, 13]], '#ffffff!').pxs([[12, 3], [12, 5], [11, 4], [13, 4]], '#fff3b0!'));

// ---------- Harvest ----------
(() => {
	const s = S().el(8, 8.5, 5, 4.2, '#e8344e').pg([[3.3, 9], [12.7, 9], [8, 15]], '#e8344e');
	s.pxs([[6, 8], [10, 8], [8, 10], [5, 11], [11, 11], [7, 12], [9, 13], [8, 7]], '#ffe08a!');
	s.pg([[3.5, 5], [6, 3.5], [8, 5], [10, 3.5], [12.5, 5], [10, 6.5], [8, 5.8], [6, 6.5]], '#58b84c').rc(8, 1, 8, 4, '#3d8a35');
	s.save(I('strawberry'));
})();
(() => {
	const s = S();
	[[5.5, 10.5], [10.5, 10.5], [8, 6.5]].forEach(([x, y]) => s.el(x, y, 3.2, 3.2, '#4d63c9'));
	s.pxs([[5, 9], [10, 9], [7, 5]], '#8aa0f0!').pxs([[5, 12], [6, 12], [10, 12], [11, 12], [8, 8]], '#2c356e!');
	s.pg([[9, 3], [13, 1], [14, 3], [11, 5]], '#58b84c').save(I('blueberry'));
})();
(() => {
	const s = S().pg([[2, 13], [5, 6], [10, 2], [9, 8], [4, 13]], '#6fbf4a').pg([[6, 14], [9, 9], [14, 6], [13, 11], [8, 14]], '#86cf5c');
	s.ln(2.5, 13.5, 9, 3, 0.9, '#4e8a33!').ln(6.5, 14, 13.5, 6.5, 0.9, '#5a9a3c!').save(I('tea_leaves'));
})();
(() => {
	const s = S().ln(3, 14, 12, 4, 2.2, '#4a2e1f').ln(5, 14.5, 14, 5.5, 2.2, '#5a3826');
	s.el(12.5, 3.5, 2.3, 2.3, '#fff5d6').px(12, 3, '#ffd84a!').ln(2, 10, 6, 13, 1, '#e8a0b8!');
	s.save(I('vanilla_pod'));
})();
(() => {
	const s = S().ln(5, 15, 6, 6, 1, '#5d9a46').ln(8, 15, 9, 4, 1, '#5d9a46').ln(11, 15, 12, 7, 1, '#5d9a46');
	[[6, 6], [9, 4], [12, 7]].forEach(([x, y]) => { for (let i = 0; i < 4; i++) s.el(x + 0.2, y - i * 1.4 + 1.5, 1.3, 1, i % 2 ? '#a383de' : '#8d6ccf'); });
	s.rc(5, 12, 12, 12, '#f2a7c3').save(I('lavender'));
})();
(() => {
	const s = S().el(6, 6, 3.8, 2.8, '#f27a9a', { rot: -30 }).el(10.5, 8, 3.8, 2.6, '#f591ad', { rot: 25 }).el(6.5, 11.5, 3.6, 2.6, '#ec6a8c', { rot: 10 });
	s.pxs([[5, 5], [10, 7], [6, 11]], '#ffd1de!').save(I('rose_petals'));
})();
(() => {
	const s = S().star(8, 8.5, 6.8, 2.9, '#ffd84a').el(8, 8.5, 1.8, 1.8, '#fff3b0!');
	s.pxs([[2, 2], [14, 3], [13, 14]], '#ffffff!').pxs([[2, 1], [1, 2], [3, 2], [2, 3]], '#fff6c8!').save(I('star_petals'));
})();

// ---------- Pantry ----------
(() => {
	const s = S().el(8, 10.5, 5.5, 4.8, '#efe6d2').rc(3, 9, 13, 14, '#efe6d2').pg([[5, 6], [8, 3], [11, 6]], '#e6dbc2');
	s.rc(6, 5, 10, 6, '#c9a86b').rc(5, 10, 11, 13, '#a9c4e8!').pxs([[8, 11], [7, 12], [9, 12], [8, 12]], '#f0d27a!');
	s.pxs([[7, 2], [9, 2], [8, 1]], '#ffffff!').save(I('flour'));
})();
(() => {
	const s = S().pg([[2, 8], [7, 5], [14, 7], [9, 10]], '#fff0a0').pg([[2, 8], [9, 10], [9, 14], [2, 12]], '#f2d25c').pg([[9, 10], [14, 7], [14, 11], [9, 14]], '#e3bf45');
	s.pg([[2, 10], [9, 12], [9, 14], [2, 12]], '#f7f4ec').pg([[9, 12], [14, 9], [14, 11], [9, 14]], '#e9e4da').ln(2, 11, 9, 13, 0.8, '#7aa0d8!');
	s.save(I('butter'));
})();
(() => {
	const s = S().pg([[3, 11], [13, 11], [11, 15], [5, 15]], '#f2a7c3');
	s.el(8, 10.5, 5, 2.4, '#fffaf3').el(8, 8, 4, 2.2, '#fff6ec').el(8, 5.8, 2.6, 1.8, '#fffaf3').el(8.5, 4, 1.2, 1.2, '#fff6ec');
	s.pxs([[5, 13], [8, 13], [11, 13]], '#ffd1e0!').save(I('whipped_cream'));
})();
(() => {
	const s = S().pg([[2, 6], [9, 2], [14, 10], [7, 14]], '#6b3a24');
	[[4, 7], [7, 5], [10, 4], [6, 10], [9, 8], [11, 9]].forEach(([x, y]) => s.px(x, y, '#8a5034!'));
	s.pg([[5, 12], [11, 8], [14, 10], [7, 14]], '#e04f6a').ln(5.5, 11.5, 11.5, 7.5, 0.8, '#ffd84a!');
	s.save(I('chocolate_bar'));
})();
(() => {
	const s = S().el(8, 11, 6, 3.5, '#d8d2c8').rc(2, 8, 14, 10, null).el(8, 9, 6, 1.6, '#8a7f72');
	s.el(8, 8.5, 4.5, 2.4, '#8fbf4d').pxs([[7, 7], [9, 8]], '#b8e07a!').ln(11, 3, 14, 7, 1, '#c9a06b');
	s.save(I('matcha_powder'));
})();
(() => {
	const s = S().rc(5, 5, 11, 14, '#dceef5').rc(5, 2, 11, 4, '#f2a7c3').pxs([[6, 3], [8, 3], [10, 3]], '#ffd1e0!');
	const cols = ['#ff6b8a!', '#ffd84a!', '#6bc6ff!', '#8de07a!', '#c69cff!', '#ffffff!'];
	let i = 0; for (let y = 7; y <= 13; y++) for (let x = 6; x <= 10; x++) if ((x + y * 2) % 3 === 0) s.px(x, y, cols[i++ % cols.length]);
	s.save(I('sprinkles'));
})();
(() => {
	const s = S().el(6, 7, 3.6, 2.6, '#f27a9a', { rot: -30 }).el(10.5, 9.5, 3.6, 2.4, '#c69cff', { rot: 25 }).el(6.5, 12, 3.4, 2.4, '#f591ad');
	s.pxs([[5, 6], [7, 7], [10, 9], [11, 10], [6, 12], [7, 11], [12, 4], [3, 3]], '#ffffff!').save(I('candied_petals'));
})();
function jar(name, fill, lid, extra) {
	const s = S().rc(4, 5, 11, 14, '#cfe8f0cc').rc(5, 13, 10, 13, '#b5d6e2cc');
	if (fill) s.rc(5, 7, 10, 13, fill);
	s.rc(4, 2, 11, 4, lid);
	if (extra) extra(s);
	s.pxs([[5, 6], [5, 7], [5, 8]], '#ffffffcc!').save(I(name));
}
jar('glass_jar', null, '#c9a06b');
jar('strawberry_jam', '#c8243c', '#e04f6a', s => { s.pxs([[5, 2], [7, 2], [9, 2], [11, 2], [6, 3], [8, 3], [10, 3], [4, 3]], '#ffffff!').rc(6, 9, 9, 11, '#fff5e6!').px(7, 10, '#e8344e!').px(8, 10, '#58b84c!'); });
jar('blueberry_jam', '#3b3f8f', '#7a8de0', s => { s.pxs([[5, 2], [7, 2], [9, 2], [11, 2], [6, 3], [8, 3], [10, 3], [4, 3]], '#ffffff!').rc(6, 9, 9, 11, '#fff5e6!').px(7, 10, '#4d63c9!').px(8, 10, '#4d63c9!'); });
(() => {
	const s = S().pg([[2, 5], [12, 3], [14, 11], [4, 13]], '#f3e3a8').pg([[8, 4], [12, 3], [14, 11], [10, 12]], '#ead79a');
	s.pxs([[5, 7], [6, 10], [11, 6], [12, 9]], '#e8b84a!').save(I('wax_paper'));
})();
(() => {
	const s = S().pg([[2, 5], [12, 3], [14, 11], [4, 13]], '#bfe6f5').pg([[8, 4], [12, 3], [14, 11], [10, 12]], '#a8daee');
	s.pxs([[7, 6], [7, 8], [6, 7], [8, 7], [7, 7], [11, 9], [10, 9], [12, 9], [11, 8], [11, 10], [4, 10]], '#ffffff!').save(I('frost_wrap'));
})();

function mug(name, liquid, extra) {
	const s = S().rc(3, 4, 11, 14, '#f6efe8').el(12, 9.5, 2.6, 2.8, '#f6efe8', { ring: 0.45 });
	s.rc(4, 4, 10, 6, liquid || '#b3a79c!');
	s.pxs([[6, 9], [8, 9], [5, 10], [6, 10], [7, 10], [8, 10], [9, 10], [6, 11], [7, 11], [8, 11], [7, 12]], '#f27a9a!');
	if (extra) extra(s);
	s.save(I(name));
}
mug('mug', null);
const steam = s => s.pxs([[5, 3], [6, 2], [8, 3], [9, 1], [7, 0]], '#ffffffaa!');
mug('matcha_latte', '#8fc25a!', s => { steam(s); s.pxs([[6, 5], [8, 5], [7, 6]], '#f7f2e0!'); });
mug('hot_chocolate', '#6a3b22!', s => { steam(s); s.pxs([[5, 5], [6, 5], [9, 5]], '#ffffff!'); });
mug('strawberry_milk', '#f7afc6!', s => s.el(10.5, 4, 1.8, 1.6, '#e8344e').px(10, 2, '#58b84c!'));
mug('lavender_tea', '#b89bdd!', s => { steam(s); s.ln(10, 6, 12, 1, 0.9, '#5d9a46!').pxs([[12, 1], [11, 2], [12, 3]], '#9b7fd6!'); });
mug('rose_tea', '#ec8ea6!', s => { steam(s); s.el(6, 5.5, 1.6, 0.9, '#f27a9a'); });
mug('starlight_tea', '#ffe79a!', s => { s.star(7, 2.5, 2.4, 1, '#ffd84a').pxs([[11, 1], [2, 3]], '#ffffff!'); });

// ---------- Dough ----------
function dough(name, col, extra) {
	const s = S().el(8, 10.5, 6.2, 4.3, col).el(7.5, 8.5, 4.5, 3, col);
	if (extra) extra(s);
	s.save(I(name));
}
dough('bread_dough', '#f1d7a6', s => s.pxs([[5, 9], [10, 8], [8, 12], [11, 11]], '#fff8e6!'));
dough('sweet_dough', '#f9e5c4', s => s.pxs([[6, 8], [10, 10]], '#ffffff!'));
(() => {
	const s = S().rc(3, 6, 13, 13, '#f6e0b5').pxs([[3, 8], [4, 8], [5, 8], [6, 8], [7, 8], [8, 8], [9, 8], [10, 8], [11, 8], [12, 8], [13, 8]], '#e8cc98!').pxs([[3, 11], [4, 11], [5, 11], [6, 11], [7, 11], [8, 11], [9, 11], [10, 11], [11, 11], [12, 11], [13, 11]], '#e8cc98!');
	s.rc(10, 4, 13, 5, '#f8e8c8').save(I('pastry_dough'));
})();
dough('cookie_dough', '#e2b77c', s => s.pxs([[5, 9], [9, 8], [11, 11], [7, 12], [6, 11]], '#5a3220!'));
// Flavor speckles on top of dough (tinted)
S().pxs([[6, 9], [9, 7], [10, 11], [5, 12], [8, 11], [12, 10]], '#ffffff!').done({ outline: false }).save(I('dough_flavor'));

// ---------- Bakes (baked + raw palettes) ----------
const BAKED = { crust: '#c8843a', light: '#e8b468', side: '#b06e2c', base: '#d9933e' };
const RAW = { crust: '#f1d9ab', light: '#fbefd6', side: '#e6c98f', base: '#f4dfb6' };

function loaf(name, p) {
	const s = S().el(8, 10.5, 7, 4, p.side).el(8, 8.5, 6.5, 4.5, p.crust).rc(2, 10, 14, 12, p.crust);
	s.ln(4.5, 9, 6.5, 6, 0.9, p.light + '!').ln(7.5, 9.5, 9.5, 6, 0.9, p.light + '!').ln(10.5, 9.5, 12, 7, 0.9, p.light + '!');
	s.save(I(name));
}
loaf('bread_loaf', BAKED); loaf('raw_loaf', RAW);
function baguette(name, p) {
	const s = S().ln(2.5, 13.5, 13.5, 2.5, 4.2, p.crust);
	[[5, 10], [8, 7], [11, 4]].forEach(([x, y]) => s.ln(x - 1, y - 1, x + 1, y + 1, 0.8, p.light + '!'));
	s.save(I(name));
}
baguette('baguette', BAKED); baguette('raw_baguette', RAW);
function croissant(name, p) {
	const s = S().el(3.5, 11, 2.2, 1.8, p.base).el(5.8, 9, 2.6, 2.6, p.crust).el(8.5, 8, 3, 3.2, p.base).el(11.2, 9, 2.6, 2.6, p.crust).el(13, 11, 2.2, 1.8, p.base);
	s.save(I(name));
}
croissant('croissant', BAKED); croissant('raw_croissant', RAW);

function wrapper(s, cake) {
	s.pg([[3.5, 9], [12.5, 9], [11.3, 15], [4.7, 15]], '#f2a7c3');
	[5, 7, 9, 11].forEach(x => s.ln(x + 0.5, 9.5, x + 0.5 - (x - 8) * 0.12, 14.5, 0.8, '#e58fb0!'));
	if (cake) s.el(8, 9, 4.5, 1.6, cake);
}
(() => { const s = S(); wrapper(s, '#e8b878'); s.save(I('cupcake')); })();
S().el(8, 8.2, 5.2, 2.4, '#f4f4f4').el(8, 6.2, 3.8, 2, '#f8f8f8').el(8, 4.4, 2.4, 1.6, '#f2f2f2').el(8.6, 2.9, 1.1, 1.1, '#fafafa')
	.pxs([[6, 7], [10, 8], [7, 5]], '#ffffff!').save(I('cupcake_frosting'));
(() => { const s = S(); wrapper(s, '#f6e3bd'); s.el(8, 7.5, 4.2, 2.6, '#f9e8c8'); s.save(I('raw_cupcake')); })();

function donutBase(name, col) {
	const s = S().el(8, 8.5, 6.6, 5.6, col).el(8, 8, 2, 1.5, null);
	s.save(I(name));
}
donutBase('donut', '#d79a55'); donutBase('raw_donut', '#f3dcae');
S().el(8, 8, 5.6, 4.3, '#f4f4f4').el(8, 8, 2.6, 2, null).pxs([[3, 10], [13, 10], [6, 12], [11, 12]], '#f0f0f0').pxs([[6, 6], [11, 9]], '#ffffff!')
	.save(I('donut_glaze'));

function cookies(name, col) {
	const s = S().el(5.5, 9.5, 4, 3.6, col).el(10.5, 10.5, 4, 3.6, col).el(8, 6, 4, 3.4, col);
	s.save(I(name));
}
cookies('cookies', '#d9a35c'); cookies('raw_cookies', '#ecd0a0');
S().pxs([[4, 9], [6, 11], [10, 10], [12, 12], [7, 5], [9, 6], [11, 9], [5, 8]], '#f4f4f4!').done({ outline: false }).save(I('cookies_chips'));

(() => {
	const s = S().rc(4, 6, 12, 6, '#fff6e8!').rc(3, 12, 11, 12, '#fff6e8!');
	s.pxs([[4, 8], [6, 8], [8, 8], [10, 8], [12, 8], [3, 14], [5, 14], [7, 14], [9, 14], [11, 14]], '#e8e0d8!');
	s.save(I('macarons'));
})();
S().el(8, 5, 4.6, 1.8, '#f4f4f4').el(8, 7.6, 4.6, 1.4, '#ececec').el(7, 11, 4.6, 1.8, '#f4f4f4').el(7, 13.6, 4.6, 1.4, '#ececec').rc(4, 6, 12, 6, null).rc(3, 12, 11, 12, null)
	.pxs([[6, 4], [5, 10]], '#ffffff!').save(I('macarons_shells'));
(() => {
	const s = S().el(8, 5, 4.6, 1.8, '#f6e3bd').el(8, 7.6, 4.6, 1.4, '#f1d9ab').el(7, 11, 4.6, 1.8, '#f6e3bd').el(7, 13.6, 4.6, 1.4, '#f1d9ab');
	s.save(I('raw_macarons'));
})();

(() => {
	const s = S().rc(1, 14, 14, 14, '#dfe6ee').pg([[2, 8], [13, 8], [13, 13], [2, 13]], '#f3d39b');
	s.rc(2, 10, 13, 10, '#fff8ee!').rc(2, 12, 13, 12, '#f8e6c4!');
	s.save(I('cake'));
})();
S().el(7.5, 7.5, 6, 2.5, '#f4f4f4').rc(2, 7, 13, 8, '#f4f4f4').pxs([[3, 9], [3, 10], [6, 9], [9, 9], [9, 10], [9, 11], [12, 9]], '#f0f0f0')
	.pxs([[7, 3], [8, 3]], '#ffffff!').save(I('cake_frosting'));
(() => {
	const s = S().rc(1, 14, 14, 14, '#dfe6ee').pg([[2, 8], [13, 8], [13, 13], [2, 13]], '#f6e3bd').el(7.5, 8, 6, 2.5, '#f9e8c8');
	s.save(I('raw_cake'));
})();

(() => {
	const s = S().el(8, 10, 7, 4.2, '#d49a52').el(8, 9, 6.6, 3.4, '#e0ac62');
	[[2, 9], [4, 7], [7, 6], [10, 6], [12, 7], [14, 9]].forEach(([x, y]) => s.px(x, y, '#f0c27a!'));
	s.save(I('pie'));
})();
S().el(8, 9, 5.2, 2.5, '#f2f2f2').pxs([[6, 8], [10, 9], [8, 10]], '#ffffff!').ln(6, 8, 10, 10, 0.7, '#d0d0d0!').save(I('pie_filling'));
(() => {
	const s = S().el(8, 10, 7, 4.2, '#efd3a0').el(8, 9, 6.6, 3.4, '#f6e3bd').ln(4, 8, 12, 10, 1, '#efd3a0!').ln(4, 10, 12, 8, 1, '#efd3a0!');
	s.save(I('raw_pie'));
})();

(() => {
	const s = S().el(8, 10, 6, 4, '#2b2220').el(6, 8, 3, 2.5, '#3a2e2a').el(10.5, 8.5, 3, 2.5, '#322724');
	s.pxs([[6, 11], [10, 10]], '#ff7a2e!').pxs([[7, 4], [8, 3], [9, 2], [6, 2]], '#9a9a9aaa!').save(I('burnt_pastry'));
})();

return BK.saved.length + ' saved';
})()
