(function () {
const MB = window.MB;
const { make, slice, offset } = MB;
const NOAO = { parent: 'block/block', ambientocclusion: false };
const C = (name, from, to, tex, extra) => Object.assign({ name, from, to, tex }, extra || {});

// ================= Grand Brick Oven (3x3x3, rendered from the chamber block) =================
function grandOven(lit) {
	const tex = {
		bricks: { file: 'block/oven/bricks', big: true },
		stone: { file: 'block/oven/stone', big: true },
		inside: { file: lit ? 'block/oven/inside_lit' : 'block/oven/inside', big: true },
		soot: { file: 'block/oven/soot', big: true },
		heart: 'block/oven/heart_plaque', log: 'block/oven/log_end', bark: 'block/oven/bark', peel: 'block/oven/peel'
	};
	const cubes = [
		C('plinth', [-16, -16, -16], [32, -2, 32], 'bricks'),
		C('plinth_band', [-16, -2, -16], [32, 0, 32], { all: 'stone', down: null }),
		C('wall_left', [-15, 0, -14], [1, 14, 31], { all: 'bricks', east: 'inside', down: null }),
		C('wall_right', [15, 0, -14], [31, 14, 31], { all: 'bricks', west: 'inside', down: null }),
		C('wall_back', [1, 0, 14], [15, 11, 31], { all: 'bricks', north: 'inside', down: null }),
		C('ceiling', [1, 11, -14], [15, 14, 14], { all: 'bricks', down: 'inside' }),
		C('floor', [1, 0.02, -14], [15, 0.02, 14], { up: 'inside' }),
		C('jamb_left', [-1, 0, -15], [1, 12, -14], { all: 'stone', down: null }),
		C('jamb_right', [15, 0, -15], [17, 12, -14], { all: 'stone', down: null }),
		C('lintel', [-1, 10, -15.5], [17, 12.5, -14], { all: 'stone' }),
		C('keystone', [5, 12, -15.6], [11, 15.5, -14], { all: 'stone' }),
		C('heart', [6, 12.3, -16], [10, 15.3, -15.6], { all: 'stone', north: 'heart' }, { uv: { north: [0, 0, 16, 16] } }),
		C('hearth', [-2, 0, -16], [18, 1.5, -14], { all: 'stone', down: null }),
		C('dome1', [-14, 14, -13], [30, 18, 30], { all: 'bricks', down: null }),
		C('dome2', [-10, 18, -9], [26, 23, 26], { all: 'bricks', down: null }),
		C('dome3', [-5, 23, -4], [21, 27, 21], { all: 'bricks', down: null }),
		C('dome4', [0, 27, 1], [16, 30, 16], { all: 'bricks', down: null }),
		C('chimney', [11, 24, 18], [18, 32, 25], { all: 'bricks', up: 'soot', down: null }),
		C('chimney_cap', [10, 31, 17], [19, 32, 26], { all: 'stone', up: 'soot', down: null }),
		C('log1', [-13, 0, -16], [-5, 2.6, -14.2], { all: 'bark', east: 'log', west: 'log' }),
		C('log2', [-12, 2.6, -16], [-6, 5, -14.2], { all: 'bark', east: 'log', west: 'log' }),
		C('peel_handle', [22, 0, -15.6], [23, 15, -14.8], 'peel', { rot: ['z', -22.5, [22.5, 0, -15]] }),
		C('peel_paddle', [19.5, 0, -15.8], [25.5, 6, -15.2], 'peel', { rot: ['z', -22.5, [22.5, 0, -15]] })
	];
	make(lit ? 'grand_oven_lit' : 'grand_oven', tex, cubes, NOAO);
}
grandOven(false);
grandOven(true);

make('oven_heart', { bricks: 'block/oven_bricks', front: 'block/oven_heart_front', heart: 'block/oven/heart_plaque' }, [
	C('block', [0, 0, 0], [16, 16, 16], { all: 'bricks', north: 'front' }),
	C('plaque', [6, 12, -0.5], [10, 15.5, 0], { all: 'bricks', north: 'heart' }, { uv: { north: [0, 0, 16, 16] } })
]);

// ================= Mixing bowl =================
const bowl = [
	C('foot', [5, 0, 5], [11, 1, 11], 'bowl'),
	C('floor', [4, 1, 4], [12, 1.5, 12], { all: 'bowl', up: 'inner' }),
	C('low_n', [3, 1, 3], [13, 3, 4], { all: 'bowl', south: 'inner' }), C('low_s', [3, 1, 12], [13, 3, 13], { all: 'bowl', north: 'inner' }),
	C('low_w', [3, 1, 4], [4, 3, 12], { all: 'bowl', east: 'inner' }), C('low_e', [12, 1, 4], [13, 3, 12], { all: 'bowl', west: 'inner' }),
	C('mid_n', [2, 3, 2], [14, 5, 3], { all: 'bowl', south: 'inner' }), C('mid_s', [2, 3, 13], [14, 5, 14], { all: 'bowl', north: 'inner' }),
	C('mid_w', [2, 3, 3], [3, 5, 13], { all: 'bowl', east: 'inner' }), C('mid_e', [13, 3, 3], [14, 5, 13], { all: 'bowl', west: 'inner' }),
	C('rim_n', [1.5, 5, 1.5], [14.5, 6.5, 2.5], { all: 'bowl', south: 'inner' }), C('rim_s', [1.5, 5, 13.5], [14.5, 6.5, 14.5], { all: 'bowl', north: 'inner' }),
	C('rim_w', [1.5, 5, 2.5], [2.5, 6.5, 13.5], { all: 'bowl', east: 'inner' }), C('rim_e', [13.5, 5, 2.5], [14.5, 6.5, 13.5], { all: 'bowl', west: 'inner' })
];
const bowlTex = { bowl: 'block/mixing_bowl', inner: 'block/mixing_bowl_inner', batter: 'block/batter', wood: 'block/pastry_board', metal: 'block/pie_tin' };
make('mixing_bowl', bowlTex, bowl);
make('mixing_bowl_filled', bowlTex, bowl.concat([
	C('batter', [2.5, 5, 2.5], [13.5, 5, 13.5], { up: 'batter' }),
	C('whisk_handle', [9.5, 6, 7.5], [10.5, 13, 8.5], 'wood', { rot: ['z', 22.5, [9, 5, 8]] }),
	C('whisk_wires', [8, 2.5, 7], [11, 6.5, 9], { all: 'metal', up: null }, { rot: ['z', 22.5, [9, 5, 8]] })
]));

// ================= Pastry board =================
const boardCubes = [
	C('board', [1, 0, 2], [14.5, 1, 14], 'wood'),
	C('handle', [14.5, 0.2, 6.5], [16, 0.8, 9.5], 'wood')
];
const boardTex = { wood: 'block/pastry_board', dough: 'block/dough' };
make('pastry_board', boardTex, boardCubes);
make('pastry_board_dough', boardTex, boardCubes.concat([
	C('dough', [3.5, 1, 5], [11.5, 2, 11], { all: 'dough', down: null }),
	C('dough_top', [4.5, 2, 6], [10.5, 2.8, 10], { all: 'dough', down: null }),
	C('pin', [2, 1, 12], [14, 2.6, 13.6], 'wood', { rot: ['y', 22.5, [8, 1, 12.8]] })
]));

// ================= Placed bakes =================
make('bread_loaf', { wood: 'block/pastry_board', crust: 'block/bread_crust', bottom: 'block/bread_bottom', crumb: 'block/bread_crumb' }, [
	C('board', [2, 0, 3], [14, 1, 13], 'wood'),
	C('loaf', [3.5, 1, 5], [12.5, 5, 11], { all: 'crust', down: 'bottom' }),
	C('belly', [3, 1.5, 5.5], [13, 4.5, 10.5], { all: 'crust', down: null }),
	C('dome', [4, 5, 5.5], [12, 6.2, 10.5], { all: 'crust', down: null }),
	C('crown', [5, 6.2, 6.5], [11, 6.8, 9.5], { all: 'crust', down: null }),
	C('slice', [10.5, 1, 3.5], [13.5, 4.5, 4.3], { all: 'crust', north: 'crumb', south: 'crumb' })
], NOAO);
make('baguette', { cloth: 'block/tablecloth', crust: 'block/baguette_crust', bottom: 'block/bread_bottom' }, [
	C('cloth', [1, 0, 3], [15, 0.3, 13], 'cloth'),
	C('b1', [1, 0.3, 5], [15, 2.8, 7.5], { all: 'crust', down: 'bottom' }, { rot: ['y', 22.5, [8, 0, 6.25]] }),
	C('b1_top', [1.5, 2.8, 5.5], [14.5, 3.6, 7], { all: 'crust', down: null }, { rot: ['y', 22.5, [8, 0, 6.25]] }),
	C('b2', [1, 0.3, 8.5], [15, 2.8, 11], { all: 'crust', down: 'bottom' }, { rot: ['y', -22.5, [8, 0, 9.75]] }),
	C('b2_top', [1.5, 2.8, 9], [14.5, 3.6, 10.5], { all: 'crust', down: null }, { rot: ['y', -22.5, [8, 0, 9.75]] })
], NOAO);
make('croissant', { plate: 'block/plate', c: 'block/croissant' }, [
	C('plate', [2.5, 0, 2.5], [13.5, 0.5, 13.5], 'plate'),
	C('center', [6, 0.5, 6], [10, 4, 10], 'c'),
	C('left', [3.6, 0.5, 6.5], [6, 3.2, 9.5], 'c', { rot: ['y', -22.5, [6, 0, 8]] }),
	C('right', [10, 0.5, 6.5], [12.4, 3.2, 9.5], 'c', { rot: ['y', 22.5, [10, 0, 8]] }),
	C('left_tip', [2, 0.5, 7.4], [4.2, 2.2, 9.4], 'c', { rot: ['y', -45, [4, 0, 8.5]] }),
	C('right_tip', [11.8, 0.5, 7.4], [14, 2.2, 9.4], 'c', { rot: ['y', 45, [12, 0, 8.5]] })
], NOAO);

const cupcake = [
	C('wrapper_base', [6.25, 0, 6.25], [9.75, 1, 9.75], { all: 'wrapper', up: null }),
	C('wrapper', [5.75, 1, 5.75], [10.25, 3, 10.25], { all: 'wrapper', up: 'sponge', down: null }),
	C('frost1', [5.25, 3, 5.25], [10.75, 4.6, 10.75], 'frosting', { tint: true }),
	C('sprinkles', [5.3, 4.62, 5.3], [10.7, 4.62, 10.7], { up: 'sprinkles' }),
	C('frost2', [6, 4.6, 6], [10, 5.7, 10], { all: 'frosting', down: null }, { tint: true, rot: ['y', 45, [8, 5, 8]] }),
	C('frost3', [6.75, 5.7, 6.75], [9.25, 6.5, 9.25], { all: 'frosting', down: null }, { tint: true }),
	C('cherry', [7.5, 6.5, 7.5], [8.5, 7.5, 8.5], { all: 'cherry', down: null }),
	C('stem', [7.9, 7.5, 7.9], [8.1, 8.6, 8.1], { all: 'leaf', down: null, up: null })
];
const cupTex = { wrapper: 'block/cupcake_wrapper', sponge: 'block/sponge', frosting: 'block/frosting', cherry: 'block/cherry', sprinkles: 'block/sprinkle_overlay', leaf: 'block/leaf' };
const cupSpots = [[[0, 0]], [[-2.5, -2], [2.5, 2]], [[-3, -3], [3, -2.5], [0, 3]], [[-3.5, -3.5], [3.5, -3.5], [-3.5, 3.5], [3.5, 3.5]]];
cupSpots.forEach((spots, i) => make('cupcake_' + (i + 1), cupTex, [].concat(...spots.map(([x, z], k) => offset(cupcake, x, k * 0.05, z))), NOAO));

const donut = [
	C('n', [4, 0, 4], [12, 2.5, 7], 'dough'), C('s', [4, 0, 9], [12, 2.5, 12], 'dough'),
	C('w', [4, 0, 7], [7, 2.5, 9], 'dough'), C('e', [9, 0, 7], [12, 2.5, 9], 'dough'),
	C('glaze_n', [4.4, 2.5, 4.4], [11.6, 3, 7], { all: 'glaze', down: null }, { tint: true }),
	C('glaze_s', [4.4, 2.5, 9], [11.6, 3, 11.6], { all: 'glaze', down: null }, { tint: true }),
	C('glaze_w', [4.4, 2.5, 7], [7, 3, 9], { all: 'glaze', down: null }, { tint: true }),
	C('glaze_e', [9, 2.5, 7], [11.6, 3, 9], { all: 'glaze', down: null }, { tint: true }),
	C('drip1', [5, 1.4, 3.9], [6, 2.5, 4], { north: 'glaze' }, { tint: true }),
	C('drip2', [9.5, 1, 3.9], [10.5, 2.5, 4], { north: 'glaze' }, { tint: true }),
	C('sprinkles_n', [4.4, 3.02, 4.4], [11.6, 3.02, 7], { up: 'sprinkles' }), C('sprinkles_s', [4.4, 3.02, 9], [11.6, 3.02, 11.6], { up: 'sprinkles' })
];
const donutTex = { dough: 'block/donut_dough', glaze: 'block/glaze', sprinkles: 'block/sprinkle_overlay', plate: 'block/plate' };
[1, 2, 3].forEach(n => {
	let cubes = [C('plate', [2, 0, 2], [14, 0.5, 14], 'plate')];
	for (let k = 0; k < n; k++) cubes = cubes.concat(offset(donut, k % 2 ? 0.75 : 0, 0.5 + k * 3, k % 2 ? -0.5 : 0));
	make('donut_' + n, donutTex, cubes, NOAO);
});

const cookie = (x, y, z) => [
	C('cookie', [x, y, z], [x + 4.6, y + 0.8, z + 4.6], 'cookie'),
	C('cookie_top', [x + 0.5, y + 0.8, z + 0.5], [x + 4.1, y + 1.1, z + 4.1], { all: 'cookie', down: null }),
	C('chip', [x + 1, y + 1.1, z + 1.2], [x + 1.8, y + 1.35, z + 2], { all: 'chip', down: null }, { tint: true }),
	C('chip', [x + 2.7, y + 1.1, z + 2.4], [x + 3.5, y + 1.35, z + 3.2], { all: 'chip', down: null }, { tint: true }),
	C('chip', [x + 1.5, y + 1.1, z + 3.1], [x + 2.3, y + 1.35, z + 3.9], { all: 'chip', down: null }, { tint: true })
];
make('cookies', { plate: 'block/plate', cookie: 'block/cookie', chip: 'block/chip' }, [
	C('plate', [1, 0, 1], [15, 0.5, 15], 'plate')
].concat(cookie(3, 0.5, 3.5), cookie(8, 0.5, 7.5), cookie(8.5, 0.5, 2.5), cookie(4.5, 1.6, 6.5), cookie(3, 0.5, 9.5)), NOAO);

let macs = [C('plate', [2.5, 0, 2.5], [13.5, 0.5, 13.5], 'plate')];
for (let i = 0; i < 3; i++) {
	const y = 0.5 + i * 3.4, s = i % 2 ? 0.5 : -0.25;
	macs = macs.concat([
		C('shell_bottom', [5.5 + s, y, 5.5], [10.5 + s, y + 1.3, 10.5], 'shell', { tint: true }),
		C('filling', [5.9 + s, y + 1.3, 5.9], [10.1 + s, y + 2.1, 10.1], { all: 'filling', up: null, down: null }),
		C('shell_top', [5.5 + s, y + 2.1, 5.5], [10.5 + s, y + 3.4, 10.5], 'shell', { tint: true })
	]);
}
macs.push(C('mac_side_bottom', [10.5, 0.5, 9.5], [13.5, 1.3, 12.5], 'shell', { tint: true }), C('mac_side_top', [10.5, 1.8, 9.5], [13.5, 2.6, 12.5], 'shell', { tint: true }),
	C('mac_side_fill', [10.8, 1.3, 9.8], [13.2, 1.8, 12.2], { all: 'filling', up: null, down: null }));
make('macarons', { plate: 'block/plate', shell: 'block/macaron_shell', filling: 'block/macaron_filling' }, macs, NOAO);

// ---------- Pie in a tin, eaten in 4 slices ----------
const pie = [
	C('tin', [1, 0, 1], [15, 2.5, 15], { all: 'tin', up: 'crust' }),
	C('filling', [2.5, 2.5, 2.5], [13.5, 3.3, 13.5], { all: 'inside', up: 'filling', down: null }, { tint: ['up'] }),
	C('rim_n', [1, 2.5, 1], [15, 3.8, 2.5], { all: 'crust', down: null }), C('rim_s', [1, 2.5, 13.5], [15, 3.8, 15], { all: 'crust', down: null }),
	C('rim_w', [1, 2.5, 2.5], [2.5, 3.8, 13.5], { all: 'crust', down: null }), C('rim_e', [13.5, 2.5, 2.5], [15, 3.8, 13.5], { all: 'crust', down: null }),
	C('lattice1', [4, 3.3, 2.5], [5, 3.8, 13.5], { all: 'crust', down: null }), C('lattice2', [7.5, 3.3, 2.5], [8.5, 3.8, 13.5], { all: 'crust', down: null }),
	C('lattice3', [11, 3.3, 2.5], [12, 3.8, 13.5], { all: 'crust', down: null }),
	C('lattice4', [2.5, 3.4, 5], [13.5, 3.9, 6], { all: 'crust', down: null }), C('lattice5', [2.5, 3.4, 10], [13.5, 3.9, 11], { all: 'crust', down: null })
];
for (let i = 0; i < 7; i++) {
	pie.push(C('crimp', [1.5 + i * 2, 3.8, 1.2], [2.5 + i * 2, 4.3, 2.2], { all: 'crust', down: null }));
	pie.push(C('crimp', [1.5 + i * 2, 3.8, 13.8], [2.5 + i * 2, 4.3, 14.8], { all: 'crust', down: null }));
}
const pieTex = { tin: 'block/pie_tin', crust: 'block/pie_crust', filling: 'block/pie_filling', inside: 'block/pie_inside' };
for (let b = 0; b < 4; b++) make('pie_slice' + b, pieTex, b === 0 ? pie : slice(pie, 1 + b * 3.5, 'inside'), NOAO);

// ---------- Cake on a cake board, 4 slices + toppings ----------
const cake = [
	C('board', [0.5, 0, 0.5], [15.5, 0.5, 15.5], 'board', { noInside: true }),
	C('sponge', [1.5, 0.5, 1.5], [14.5, 7, 14.5], { all: 'side', down: null, up: null }),
	C('frosting', [1.5, 7, 1.5], [14.5, 8.5, 14.5], { all: 'frosting', down: null }, { tint: true })
];
for (let i = 0; i < 6; i++) {
	const p = 2.5 + i * 2.2;
	cake.push(C('pipe', [p, 8.5, 1.8], [p + 1, 9.3, 2.8], { all: 'frosting', down: null }, { tint: true, noInside: true }));
	cake.push(C('pipe', [p, 8.5, 13.2], [p + 1, 9.3, 14.2], { all: 'frosting', down: null }, { tint: true, noInside: true }));
	cake.push(C('pipe', [13.2, 8.5, p], [14.2, 9.3, p + 1], { all: 'frosting', down: null }, { tint: true, noInside: true }));
}
[[3, 5], [7, 4.5], [11, 5.5]].forEach(([x, y]) => {
	cake.push(C('drip', [x, y, 1.25], [x + 1, 7, 1.5], { all: 'frosting', up: null, down: null }, { tint: true, noInside: true }));
	cake.push(C('drip', [x + 1.5, y, 14.5], [x + 2.5, 7, 14.75], { all: 'frosting', up: null, down: null }, { tint: true, noInside: true }));
	cake.push(C('drip', [14.5, y, x + 1], [14.75, 7, x + 2], { all: 'frosting', up: null, down: null }, { tint: true, noInside: true }));
});
const cakeTex = { board: 'block/cake_board', side: 'block/cake_side', frosting: 'block/cake_frosting', inside: 'block/cake_inside' };
const CUT = b => 1.5 + b * 3.25;
for (let b = 0; b < 4; b++) make('cake_slice' + b, cakeTex, b === 0 ? cake : slice(cake, CUT(b), 'inside'), NOAO);

const T = 8.5;
const berry = (x, z, s, key, leaf) => [C('berry', [x - s, T, z - s], [x + s, T + s * 2, z + s], { all: key, down: null })].concat(leaf ? [C('leaf', [x - s * 0.7, T + s * 2, z - s * 0.7], [x + s * 0.7, T + s * 2 + 0.4, z + s * 0.7], { all: 'leaf', down: null })] : []);
const ring = (n, r, fn) => { const out = []; for (let i = 0; i < n; i++) { const a = i * Math.PI * 2 / n; out.push(...fn(8 + Math.cos(a) * r, 8 + Math.sin(a) * r, i)); } return out; };
const plane = (key) => [C('overlay', [1.6, T + 0.02, 1.6], [14.4, T + 0.02, 14.4], { up: key })];
const toppings = {
	strawberries: { tex: { berry: 'block/topping_strawberry', leaf: 'block/leaf' }, cubes: ring(6, 4.4, (x, z) => berry(x, z, 1, 'berry', true)).concat(berry(8, 8, 1.3, 'berry', true)) },
	blueberries: { tex: { berry: 'block/topping_blueberry' }, cubes: ring(10, 4.6, (x, z) => berry(x, z, 0.6, 'berry')).concat(ring(4, 1.5, (x, z) => berry(x, z, 0.6, 'berry'))) },
	raspberries: { tex: { berry: 'block/topping_raspberry', leaf: 'block/leaf' }, cubes: ring(7, 4.5, (x, z) => berry(x, z, 0.8, 'berry')).concat(berry(8, 8, 1, 'berry', true)) },
	cream: { tex: { cream: 'block/topping_cream' }, cubes: ring(8, 4.8, (x, z) => [C('dollop', [x - 1, T, z - 1], [x + 1, T + 1.5, z + 1], { all: 'cream', down: null }), C('tip', [x - 0.5, T + 1.5, z - 0.5], [x + 0.5, T + 2.2, z + 0.5], { all: 'cream', down: null })]) },
	sprinkles: { tex: { sprinkles: 'block/sprinkle_overlay' }, cubes: plane('sprinkles') },
	chocolate: { tex: { drizzle: 'block/topping_chocolate', choc: 'block/chocolate' }, cubes: plane('drizzle').concat([
		C('chunk', [6.5, T, 6.5], [9.5, T + 1, 8.5], { all: 'choc', down: null }), C('chunk', [3.5, T, 10.5], [5.5, T + 0.8, 12], { all: 'choc', down: null }),
		C('chunk', [10.5, T, 3.5], [12.5, T + 0.8, 5], { all: 'choc', down: null })]) },
	rose_petals: { tex: { rose: 'block/topping_rose', petal: 'block/petal', leaf: 'block/leaf' }, cubes: plane('rose').concat([
		C('bud', [6.8, T, 6.8], [9.2, T + 1.6, 9.2], { all: 'petal', down: null }), C('bud_top', [7.4, T + 1.6, 7.4], [8.6, T + 2.2, 8.6], { all: 'petal', down: null }),
		C('leaf', [9.2, T, 7.5], [10.8, T + 0.3, 8.5], { all: 'leaf', down: null })]) },
	lavender: { tex: { bud: 'block/topping_lavender', stem: 'block/leaf' }, cubes: ring(5, 4.4, (x, z) => [C('stem', [x - 0.25, T, z - 0.25], [x + 0.25, T + 1.5, z + 0.25], { all: 'stem', down: null }), C('bud', [x - 0.5, T + 1.5, z - 0.5], [x + 0.5, T + 3.4, z + 0.5], { all: 'bud', down: null })]) },
	star_petals: { tex: { star: 'block/topping_star' }, cubes: [
		C('star_a', [5.5, T, 7.25], [10.5, T + 0.8, 8.75], { all: 'star', down: null }), C('star_b', [7.25, T, 5.5], [8.75, T + 0.8, 10.5], { all: 'star', down: null }),
		C('star_c', [7, T + 0.8, 7], [9, T + 1.3, 9], { all: 'star', down: null })].concat(ring(4, 5, (x, z) => [C('sparkle', [x - 0.5, T, z - 0.5], [x + 0.5, T + 0.5, z + 0.5], { all: 'star', down: null })])) },
	coconut_flakes: { tex: { flakes: 'block/topping_coconut' }, cubes: plane('flakes').concat(ring(5, 3, (x, z) => [C('flake', [x - 0.8, T, z - 0.4], [x + 0.8, T + 0.4, z + 0.4], { all: 'flakes', down: null })])) },
	mint: { tex: { mint: 'block/topping_mint' }, cubes: ring(6, 4.6, (x, z) => [C('leaf', [x - 0.9, T, z - 0.5], [x + 0.9, T + 0.4, z + 0.5], { all: 'mint', down: null })]).concat([C('sprig', [7, T, 7.2], [9, T + 0.6, 8.8], { all: 'mint', down: null })]) },
	cherries: { tex: { berry: 'block/fruit_cherry', leaf: 'block/fruit_stem' }, cubes: ring(6, 4.4, (x, z) => [
		C('cherry', [x - 0.8, T, z - 0.8], [x + 0.8, T + 1.6, z + 0.8], { all: 'berry', down: null }),
		C('stem', [x - 0.1, T + 1.6, z - 0.1], [x + 0.1, T + 2.8, z + 0.1], { all: 'leaf', down: null })]).concat(berry(8, 8, 1, 'berry')) },
	candyfloss: { tex: { fluff: 'block/fruit_fluff' }, cubes: [
		C('cloud', [4, T, 4], [12, T + 3, 12], { all: 'fluff', down: null }),
		C('cloud_top', [5.5, T + 3, 5.5], [10.5, T + 4.5, 10.5], { all: 'fluff', down: null }),
		C('puff', [10, T, 3], [13, T + 2, 6], { all: 'fluff', down: null }),
		C('puff', [3, T, 10], [6, T + 2, 13], { all: 'fluff', down: null })] },
	lemon_slices: { tex: { lemon: 'block/topping_lemon' }, cubes: ring(5, 4.4, (x, z) => [C('slice', [x - 1.2, T, z - 1.2], [x + 1.2, T + 0.5, z + 1.2], { all: 'lemon', down: null }, { uv: { up: [0, 0, 16, 16] } })]) }
};
Object.entries(toppings).forEach(([key, t]) => {
	for (let b = 0; b < 4; b++) {
		const cubes = b === 0 ? t.cubes : slice(t.cubes, CUT(b), null, 'drop');
		if (cubes.length) make('cake_topping_' + key + '_slice' + b, t.tex, cubes, {});
	}
});

// ---------- Mug on a saucer ----------
const mugTex = { mug: 'block/mug', inner: 'block/mug_inner', liquid: 'block/liquid', plate: 'block/plate' };
make('mug', mugTex, [
	C('saucer', [3.5, 0, 3.5], [12.5, 0.5, 12.5], 'plate'),
	C('bottom', [5, 0.5, 5], [11, 1, 11], { all: 'mug', up: 'inner' }),
	C('wall_n', [5, 0.5, 5], [11, 6.5, 6], { all: 'mug', south: 'inner' }), C('wall_s', [5, 0.5, 10], [11, 6.5, 11], { all: 'mug', north: 'inner' }),
	C('wall_w', [5, 0.5, 6], [6, 6.5, 10], { all: 'mug', east: 'inner' }), C('wall_e', [10, 0.5, 6], [11, 6.5, 10], { all: 'mug', west: 'inner' }),
	C('handle_low', [11, 2, 7.5], [12.5, 3, 8.5], 'mug'), C('handle', [12, 3, 7.5], [13, 5, 8.5], 'mug'), C('handle_high', [11, 5, 7.5], [12.5, 6, 8.5], 'mug')
], NOAO);
make('mug_liquid', mugTex, [C('liquid', [6, 5.6, 6], [10, 5.6, 10], { up: 'liquid' }, { tint: true })], {});

// ================= Shop =================
function counter(open) {
	make('bakery_counter_' + (open ? 'open' : 'closed'), { wood: 'block/counter_wood', marble: 'block/marble', panel: 'block/counter_panel', register: 'block/register', brass: 'block/brass', sign: open ? 'block/sign_open' : 'block/sign_closed' }, [
		C('cabinet', [0.5, 0, 1], [15.5, 13, 16], 'wood'),
		C('panel', [1, 1, 0.5], [15, 12, 1], { all: 'wood', north: 'panel' }, { uv: { north: [0, 0, 16, 16] } }),
		C('kick', [0, 0, 0.5], [16, 1, 16], 'wood'),
		C('top', [0, 13, 0], [16, 15, 16], 'marble'),
		C('register', [9, 15, 7], [15, 18.5, 13], { all: 'register', up: 'brass' }),
		C('register_screen', [10, 18.5, 7.5], [14, 20.5, 9], 'brass'),
		C('drawer', [9.5, 15.5, 6.5], [14.5, 17, 7], 'brass'),
		C('sign_post', [3.75, 15, 2.25], [4.25, 16.5, 2.75], 'brass'),
		C('sign', [1.5, 16.5, 2.2], [6.5, 20, 2.8], { all: 'brass', north: 'sign', south: 'sign' })
	], NOAO);
}
counter(true);
counter(false);
make('display_case', { wood: 'block/case_wood', glass: 'block/case_glass' }, [
	C('base', [0, 0, 0], [16, 4, 16], 'wood'),
	C('glass_n', [0.5, 4, 0.5], [15.5, 15.5, 1], 'glass'), C('glass_s', [0.5, 4, 15], [15.5, 15.5, 15.5], 'glass'),
	C('glass_w', [0.5, 4, 1], [1, 15.5, 15], 'glass'), C('glass_e', [15, 4, 1], [15.5, 15.5, 15], 'glass'),
	C('shelf', [1, 8.5, 1], [15, 9, 15], 'glass'),
	C('top', [0, 15.5, 0], [16, 16, 16], 'wood')
], NOAO);
make('service_bell', { wood: 'block/table_wood', brass: 'block/brass' }, [
	C('base', [5, 0, 5], [11, 1, 11], 'wood'),
	C('dome', [5.5, 1, 5.5], [10.5, 3.2, 10.5], 'brass'),
	C('dome_top', [6.5, 3.2, 6.5], [9.5, 4.2, 9.5], 'brass'),
	C('button', [7.5, 4.2, 7.5], [8.5, 5.2, 8.5], 'brass')
], NOAO);

// ================= Decor =================
make('cafe_table', { cloth: 'block/tablecloth', wood: 'block/table_wood', mug: 'block/mug', petal: 'block/petal', leaf: 'block/leaf' }, [
	C('base', [4, 0, 4], [12, 1.5, 12], 'wood'),
	C('leg', [7, 1.5, 7], [9, 13, 9], 'wood'),
	C('top', [0, 13, 0], [16, 15, 16], 'cloth'),
	C('drape_n', [0, 10.5, -0.25], [16, 13, 0], 'cloth'), C('drape_s', [0, 10.5, 16], [16, 13, 16.25], 'cloth'),
	C('drape_w', [-0.25, 10.5, 0], [0, 13, 16], 'cloth'), C('drape_e', [16, 10.5, 0], [16.25, 13, 16], 'cloth'),
	C('vase', [7, 15, 7], [9, 17.5, 9], 'mug'),
	C('stem', [7.8, 17.5, 7.8], [8.2, 19, 8.2], 'leaf'),
	C('flower', [7.25, 19, 7.25], [8.75, 20.2, 8.75], 'petal')
], NOAO);
make('cafe_chair', { wood: 'block/table_wood', cushion: 'block/cushion' }, [
	C('leg1', [3, 0, 3], [4.5, 8, 4.5], 'wood'), C('leg2', [11.5, 0, 3], [13, 8, 4.5], 'wood'),
	C('leg3', [3, 0, 11.5], [4.5, 8, 13], 'wood'), C('leg4', [11.5, 0, 11.5], [13, 8, 13], 'wood'),
	C('seat', [2.5, 8, 2.5], [13.5, 9.5, 13.5], 'wood'),
	C('cushion', [3, 9.5, 3], [13, 10.5, 12], 'cushion'),
	C('post_l', [3, 9.5, 12], [4.5, 19, 13.5], 'wood'), C('post_r', [11.5, 9.5, 12], [13, 19, 13.5], 'wood'),
	C('back', [4.5, 13.5, 12.25], [11.5, 18.5, 13.25], 'cushion')
], NOAO);
make('flour_sack', { sack: 'block/sack', rope: 'block/pastry_board', flour: 'block/cake_frosting' }, [
	C('body', [3, 0, 4], [13, 9.5, 12], 'sack'),
	C('shoulder', [3.5, 9.5, 4.5], [12.5, 11, 11.5], { all: 'sack', down: null }),
	C('neck', [5.5, 11, 6.5], [10.5, 12, 9.5], { all: 'sack', down: null }),
	C('tie', [5.2, 11.6, 6.2], [10.8, 12.4, 9.8], 'rope'),
	C('flap', [6, 12.4, 7], [10, 13.4, 9], { all: 'sack', down: null }),
	C('spill', [11, 0, 10.5], [14.5, 0.4, 13.5], { all: 'flour', down: null })
], NOAO);
make('bread_basket', { wicker: 'block/wicker', cloth: 'block/tablecloth', crust: 'block/bread_crust', baguette: 'block/baguette_crust' }, [
	C('basket', [2, 0, 3], [14, 6, 13], 'wicker'),
	C('cloth', [2.5, 6, 3.5], [13.5, 6.4, 12.5], 'cloth'),
	C('drape', [13.5, 3, 5], [14.2, 6.4, 11], 'cloth'),
	C('loaf1', [3.5, 6.4, 4.5], [8.5, 9.4, 8.5], 'crust'),
	C('loaf2', [8, 6.4, 7.5], [13, 9.4, 11.5], 'crust'),
	C('baguette', [4.5, 5, 9], [6.5, 17, 11], 'baguette', { rot: ['z', -22.5, [5.5, 6, 10]] })
], NOAO);
make('cake_stand', { stand: 'block/mug', board: 'block/cake_board', side: 'block/cake_side', top: 'block/cake_frosting', cherry: 'block/cherry' }, [
	C('foot', [5, 0, 5], [11, 1, 11], 'stand'),
	C('stem', [7, 1, 7], [9, 6, 9], 'stand'),
	C('plate', [1, 6, 1], [15, 7, 15], { all: 'stand', up: 'board' }),
	C('cake', [3, 7, 3], [13, 12, 13], { all: 'side', up: 'top' }),
	C('icing', [3, 12, 3], [13, 13, 13], 'top'),
	C('cherry', [7.5, 13, 7.5], [8.5, 14, 8.5], 'cherry')
], NOAO);
make('menu_board', { wood: 'block/table_wood', chalk: 'block/chalkboard' }, [
	C('back', [1, 0, 15], [15, 16, 16], 'wood'),
	C('board', [2, 1, 14], [14, 15, 15], { all: 'wood', north: 'chalk' }, { uv: { north: [0, 0, 16, 16] } }),
	C('frame_top', [1, 15, 13.5], [15, 16, 16], 'wood'), C('frame_bottom', [1, 0, 13.5], [15, 1, 16], 'wood'),
	C('frame_left', [1, 1, 13.5], [2, 15, 16], 'wood'), C('frame_right', [14, 1, 13.5], [15, 15, 16], 'wood'),
	C('chalk', [3, 1, 13.2], [5, 1.4, 13.5], 'wood')
], NOAO);
make('bunting', { flags: 'block/bunting' }, [
	C('flags', [0, 6, 15.6], [16, 16, 15.6], { north: 'flags', south: 'flags' }, { uvAll: [0, 0, 16, 10] })
], NOAO);
const jar = (x, key) => [
	C('jar', [x, 10, 11], [x + 3, 13.5, 14], 'glass'),
	C('jam', [x + 0.4, 10.2, 11.4], [x + 2.6, 12.6, 13.6], key),
	C('lid', [x - 0.2, 13.5, 10.8], [x + 3.2, 14.4, 14.2], 'cloth')
];
make('jar_shelf', { wood: 'block/table_wood', glass: 'block/jar_glass', cloth: 'block/tablecloth', red: 'block/cherry', blue: 'block/topping_blueberry', yellow: 'block/lemon_peel' }, [
	C('plank', [0, 9, 10], [16, 10, 16], 'wood'),
	C('bracket_l', [2, 6, 15], [3, 9, 16], 'wood'), C('bracket_r', [13, 6, 15], [14, 9, 16], 'wood')
].concat(jar(1.5, 'red'), jar(6.5, 'blue'), jar(11.5, 'yellow')), NOAO);
function crate(name, pileKey, fruitKey, size) {
	const fruits = [[4, 4], [11, 5], [7.5, 9], [4.5, 12], [11.5, 11.5]].map(([x, z], i) => C('fruit', [x - size, 12.5, z - size], [x + size, 12.5 + size * 1.8, z + size], { all: 'fruit', down: null }, i % 2 ? { rot: ['y', 22.5, [x, 12, z]] } : {}));
	make(name, { crate: 'block/crate', pile: 'block/' + pileKey, fruit: 'block/' + fruitKey }, [
		C('crate', [0, 0, 0], [16, 12, 16], { all: 'crate', up: 'pile' })
	].concat(fruits), NOAO);
}
crate('strawberry_crate', 'pile_strawberry', 'topping_strawberry', 1.1);
crate('lemon_crate', 'pile_lemon', 'lemon_peel', 1.4);
crate('coconut_crate', 'pile_coconut', 'coconut_shell', 1.8);

// ================= Hanging fruit =================
function fruitModel(name, key, stem, w, h, y0, extra) {
	make(name, { fruit: 'block/' + key, stem: 'block/fruit_stem' }, [
		C('stem', [7.6, 15, 7.6], [8.4, 16, 8.4], 'stem'),
		C('fruit', [8 - w / 2, y0, 8 - w / 2], [8 + w / 2, y0 + h, 8 + w / 2], 'fruit')
	].concat(extra || []), NOAO);
}
fruitModel('coconut_stage0', 'coconut_young', 'fruit_stem', 3, 3, 12);
fruitModel('coconut_stage1', 'coconut_young', 'fruit_stem', 5, 5, 10);
fruitModel('coconut_stage2', 'coconut_shell', 'fruit_stem', 6, 6.5, 8.5, [C('fruit2', [3.5, 10, 9], [7.5, 14.5, 13], 'fruit')]);
fruitModel('lemon_stage0', 'lemon_young', 'fruit_stem', 2, 3, 12);
fruitModel('lemon_stage1', 'lemon_young', 'fruit_stem', 3, 4.5, 10.5);
fruitModel('lemon_stage2', 'lemon_peel', 'fruit_stem', 4, 5, 9.5, [C('tip', [7.5, 8.8, 7.5], [8.5, 9.5, 8.5], 'fruit'), C('leaf', [8.5, 14.5, 7.5], [11, 15, 9], 'stem')]);

// ================= Crops: four crossed planes plus little 3D fruit =================
const cropPlanes = key => [
	C('p1', [4, -1, 0], [4, 15, 16], { east: key, west: key }, { uvAll: [0, 0, 16, 16], shade: false }),
	C('p2', [12, -1, 0], [12, 15, 16], { east: key, west: key }, { uvAll: [0, 0, 16, 16], shade: false }),
	C('p3', [0, -1, 4], [16, 15, 4], { north: key, south: key }, { uvAll: [0, 0, 16, 16], shade: false }),
	C('p4', [0, -1, 12], [16, 15, 12], { north: key, south: key }, { uvAll: [0, 0, 16, 16], shade: false })
];
const fruitCubes = {
	strawberry_crop: { key: 'fruit_strawberry', list: [[3, 2.5, 6, 1], [10, 3, 11, 1], [7, 2, 13, 1], [12, 2.5, 3.5, 1]] },
	blueberry_crop: { key: 'fruit_blueberry', list: [[3, 9, 5, 0.6], [5, 7, 3, 0.6], [11, 9, 6, 0.6], [12, 6, 11, 0.6], [6, 10, 12, 0.6], [9, 8, 10, 0.6], [8, 11, 3.5, 0.6]] },
	raspberry_crop: { key: 'fruit_raspberry', list: [[3, 9, 6, 0.8], [11, 10, 4.5, 0.8], [12, 7, 11, 0.8], [6, 11, 12, 0.8], [8, 6, 8, 0.8]] },
	rose_crop: { key: 'fruit_rose', list: [[3.5, 8, 5, 1.1], [11.5, 9, 6, 1.1], [7, 10, 12, 1.1]] },
	vanilla_crop: { key: 'fruit_vanilla', pods: true, list: [[6, 7, 4, 0.4], [10, 9, 12, 0.4], [5, 11, 11, 0.4]] }
};
const CROPS = ['strawberry', 'blueberry', 'raspberry', 'tea', 'mint', 'vanilla', 'lavender', 'rose', 'pineapple', 'starbloom'];
CROPS.forEach(c => {
	const name = c + '_crop';
	for (let s = 0; s < 4; s++) {
		const tex = { plant: 'block/' + name + '_stage' + s };
		let cubes = cropPlanes('plant');
		const f = fruitCubes[name];
		if (s === 3 && f) {
			tex.fruit = 'block/' + f.key;
			f.list.forEach(([x, y, z, r]) => cubes.push(f.pods
				? C('pod', [x - r, y - 3, z - r], [x + r, y, z + r], 'fruit')
				: C('fruit', [x - r, y - r, z - r], [x + r, y + r * 1.4, z + r], 'fruit')));
		}
		if (name === 'vanilla_crop' && s > 0) {
			tex.stick = 'block/oven/peel';
			cubes.push(C('stick', [7.5, 0, 7.5], [8.5, 15, 8.5], 'stick'));
		}
		if (name === 'pineapple_crop' && s >= 2) {
			tex.fruit = 'block/fruit_pineapple';
			const h = s === 3 ? 5 : 3, w = s === 3 ? 4 : 2.5;
			cubes.push(C('pineapple', [8 - w / 2, 1, 8 - w / 2], [8 + w / 2, 1 + h, 8 + w / 2], 'fruit'));
		}
		if (name === 'starbloom_crop' && s === 3) {
			tex.star = 'block/fruit_star';
			cubes.push(C('star_a', [5.5, 10, 7.6], [10.5, 13, 8.4], 'star'), C('star_b', [7.6, 10, 5.5], [8.4, 13, 10.5], 'star'), C('core', [7, 10.8, 7], [9, 12.2, 9], 'star'));
		}
		make(name + '_stage' + s, tex, cubes, NOAO);
	}
});

return MB.models.length + ' models';
})()
