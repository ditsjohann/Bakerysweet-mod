(function () {
const BK = window.BK;
const { build, exportModel, slice, offset } = BK;
const P = { parent: 'block/block' };
function make(name, textures, cubes, extra) { build(name, textures, cubes); exportModel(name, extra || P); }

// ---------- Oven ----------
const ovenCubes = [
	{ name: 'base', from: [0, 0, 0], to: [16, 9, 16], tex: { all: 'bricks', north: 'front' } },
	{ name: 'hearth', from: [2, 0, -1], to: [14, 1.5, 0], tex: { all: 'bricks', south: null } },
	{ name: 'dome1', from: [1, 9, 1], to: [15, 12, 15], tex: { all: 'bricks', down: null } },
	{ name: 'dome2', from: [3, 12, 3], to: [13, 14, 13], tex: { all: 'bricks', down: null } },
	{ name: 'dome3', from: [5, 14, 5], to: [11, 15, 11], tex: { all: 'bricks', down: null } },
	{ name: 'chimney', from: [10, 13, 10], to: [13, 19, 13], tex: { all: 'bricks', up: 'soot', down: null } }
];
make('oven', { bricks: 'block/oven_bricks', front: 'block/oven_front', soot: 'block/oven_soot' }, ovenCubes);
make('oven_lit', { bricks: 'block/oven_bricks', front: 'block/oven_front_lit', soot: 'block/oven_soot' }, ovenCubes);

// ---------- Mixing bowl ----------
const bowl = [
	{ name: 'base', from: [5, 0, 5], to: [11, 1, 11], tex: 'bowl' },
	{ name: 'floor', from: [4, 1, 4], to: [12, 1.5, 12], tex: { all: 'bowl', up: 'inner' } },
	{ name: 'low_n', from: [3, 1, 3], to: [13, 3, 4], tex: { all: 'bowl', south: 'inner' } },
	{ name: 'low_s', from: [3, 1, 12], to: [13, 3, 13], tex: { all: 'bowl', north: 'inner' } },
	{ name: 'low_w', from: [3, 1, 4], to: [4, 3, 12], tex: { all: 'bowl', east: 'inner' } },
	{ name: 'low_e', from: [12, 1, 4], to: [13, 3, 12], tex: { all: 'bowl', west: 'inner' } },
	{ name: 'rim_n', from: [2, 3, 2], to: [14, 6, 3], tex: { all: 'bowl', south: 'inner' } },
	{ name: 'rim_s', from: [2, 3, 13], to: [14, 6, 14], tex: { all: 'bowl', north: 'inner' } },
	{ name: 'rim_w', from: [2, 3, 3], to: [3, 6, 13], tex: { all: 'bowl', east: 'inner' } },
	{ name: 'rim_e', from: [13, 3, 3], to: [14, 6, 13], tex: { all: 'bowl', west: 'inner' } }
];
const bowlTex = { bowl: 'block/mixing_bowl', inner: 'block/mixing_bowl_inner', batter: 'block/batter', wood: 'block/pastry_board' };
make('mixing_bowl', bowlTex, bowl);
make('mixing_bowl_filled', bowlTex, bowl.concat([
	{ name: 'batter', from: [3, 4.5, 3], to: [13, 4.5, 13], tex: { up: 'batter' } },
	{ name: 'spoon', from: [9, 4, 7.5], to: [10, 12, 8.5], tex: 'wood', rot: ['z', 22.5, [9.5, 5, 8]] },
	{ name: 'spoon_head', from: [8.5, 3.5, 7], to: [10.5, 5, 9], tex: 'wood', rot: ['z', 22.5, [9.5, 5, 8]] }
]));

// ---------- Pastry board ----------
const board = [
	{ name: 'board', from: [1, 0, 2], to: [15, 1, 14], tex: 'wood' },
	{ name: 'handle', from: [15, 0, 6.5], to: [16, 1, 9.5], tex: 'wood' }
];
const boardTex = { wood: 'block/pastry_board', dough: 'block/dough' };
make('pastry_board', boardTex, board);
make('pastry_board_dough', boardTex, board.concat([
	{ name: 'dough', from: [4, 1, 5], to: [12, 2, 11], tex: { all: 'dough', down: null } },
	{ name: 'dough_top', from: [5, 2, 6], to: [11, 2.75, 10], tex: { all: 'dough', down: null } },
	{ name: 'pin', from: [2, 1, 12], to: [14, 2.5, 13.5], tex: 'wood', rot: ['y', 22.5, [8, 1, 12.5]] }
]));

// ---------- Breads ----------
make('bread_loaf', { crust: 'block/bread_crust', bottom: 'block/bread_bottom' }, [
	{ name: 'loaf', from: [3, 0, 5], to: [13, 4, 11], tex: { all: 'crust', down: 'bottom' } },
	{ name: 'dome', from: [3.5, 4, 5.5], to: [12.5, 5.5, 10.5], tex: { all: 'crust', down: null } },
	{ name: 'top', from: [4.5, 5.5, 6.5], to: [11.5, 6.25, 9.5], tex: { all: 'crust', down: null } }
]);
make('baguette', { crust: 'block/baguette_crust', bottom: 'block/bread_bottom' }, [
	{ name: 'body', from: [1, 0, 6.5], to: [15, 2.5, 9.5], tex: { all: 'crust', down: 'bottom' }, rot: ['y', 22.5, [8, 0, 8]] },
	{ name: 'top', from: [1.5, 2.5, 7], to: [14.5, 3.5, 9], tex: { all: 'crust', down: null }, rot: ['y', 22.5, [8, 0, 8]] }
]);
make('croissant', { c: 'block/croissant' }, [
	{ name: 'center', from: [6, 0, 6], to: [10, 4, 10], tex: 'c' },
	{ name: 'left', from: [3.5, 0, 6.5], to: [6, 3, 9.5], tex: 'c', rot: ['y', -22.5, [6, 0, 8]] },
	{ name: 'right', from: [10, 0, 6.5], to: [12.5, 3, 9.5], tex: 'c', rot: ['y', 22.5, [10, 0, 8]] },
	{ name: 'left_tip', from: [1.5, 0, 7.5], to: [4, 1.8, 9.5], tex: 'c', rot: ['y', -45, [4, 0, 8.5]] },
	{ name: 'right_tip', from: [12, 0, 7.5], to: [14.5, 1.8, 9.5], tex: 'c', rot: ['y', 45, [12, 0, 8.5]] }
]);

// ---------- Cupcakes (1-4 on one block) ----------
const cupcake = [
	{ name: 'wrapper_base', from: [6.5, 0, 6.5], to: [9.5, 1, 9.5], tex: { all: 'wrapper', up: null } },
	{ name: 'wrapper', from: [6, 1, 6], to: [10, 3, 10], tex: { all: 'wrapper', up: 'sponge', down: null } },
	{ name: 'frosting1', from: [5.5, 3, 5.5], to: [10.5, 4.5, 10.5], tex: 'frosting', tint: true },
	{ name: 'frosting2', from: [6.5, 4.5, 6.5], to: [9.5, 5.5, 9.5], tex: { all: 'frosting', down: null }, tint: true },
	{ name: 'frosting3', from: [7.25, 5.5, 7.25], to: [8.75, 6.25, 8.75], tex: { all: 'frosting', down: null }, tint: true },
	{ name: 'cherry', from: [7.5, 6.25, 7.5], to: [8.5, 7.25, 8.5], tex: { all: 'cherry', down: null } }
];
const cupTex = { wrapper: 'block/cupcake_wrapper', sponge: 'block/sponge', frosting: 'block/frosting', cherry: 'block/cherry' };
const cupSpots = [[[0, 0]], [[-2.5, -2], [2.5, 2]], [[-3, -3], [3, -2.5], [0, 3]], [[-3.5, -3.5], [3.5, -3.5], [-3.5, 3.5], [3.5, 3.5]]];
cupSpots.forEach((spots, i) => make('cupcake_' + (i + 1), cupTex, [].concat(...spots.map(([x, z]) => offset(cupcake, x, 0, z)))));

// ---------- Donuts (stacked 1-3) ----------
const donut = [
	{ name: 'n', from: [4, 0, 4], to: [12, 2.5, 7], tex: 'dough' },
	{ name: 's', from: [4, 0, 9], to: [12, 2.5, 12], tex: 'dough' },
	{ name: 'w', from: [4, 0, 7], to: [7, 2.5, 9], tex: 'dough' },
	{ name: 'e', from: [9, 0, 7], to: [12, 2.5, 9], tex: 'dough' },
	{ name: 'glaze_n', from: [4.5, 2.5, 4.5], to: [11.5, 3, 7], tex: { all: 'glaze', down: null }, tint: true },
	{ name: 'glaze_s', from: [4.5, 2.5, 9], to: [11.5, 3, 11.5], tex: { all: 'glaze', down: null }, tint: true },
	{ name: 'glaze_w', from: [4.5, 2.5, 7], to: [7, 3, 9], tex: { all: 'glaze', down: null }, tint: true },
	{ name: 'glaze_e', from: [9, 2.5, 7], to: [11.5, 3, 9], tex: { all: 'glaze', down: null }, tint: true }
];
const donutTex = { dough: 'block/donut_dough', glaze: 'block/glaze' };
[1, 2, 3].forEach(n => {
	let cubes = [];
	for (let k = 0; k < n; k++) cubes = cubes.concat(offset(donut, k % 2 ? 0.75 : 0, k * 3, k % 2 ? -0.5 : 0));
	make('donut_' + n, donutTex, cubes);
});

// ---------- Cookies on a plate ----------
const cookie = (x, y, z) => [
	{ name: 'cookie', from: [x, y, z], to: [x + 4.5, y + 0.75, z + 4.5], tex: 'cookie' },
	{ name: 'chip', from: [x + 1, y + 0.75, z + 1.25], to: [x + 1.75, y + 1, z + 2], tex: { all: 'chip', down: null }, tint: true },
	{ name: 'chip', from: [x + 2.75, y + 0.75, z + 2.5], to: [x + 3.5, y + 1, z + 3.25], tex: { all: 'chip', down: null }, tint: true },
	{ name: 'chip', from: [x + 1.5, y + 0.75, z + 3.25], to: [x + 2.25, y + 1, z + 4], tex: { all: 'chip', down: null }, tint: true }
];
make('cookies', { plate: 'block/plate', cookie: 'block/cookie', chip: 'block/chip' }, [
	{ name: 'plate', from: [2, 0, 2], to: [14, 0.5, 14], tex: 'plate' }
].concat(cookie(3.5, 0.5, 4), cookie(8, 0.5, 7.5), cookie(8.5, 0.5, 3), cookie(5, 1.25, 6.5)));

// ---------- Macaron tower ----------
let macs = [{ name: 'plate', from: [3, 0, 3], to: [13, 0.5, 13], tex: 'plate' }];
for (let i = 0; i < 3; i++) {
	const y = 0.5 + i * 3.25, s = i % 2 ? 0.5 : -0.25;
	macs = macs.concat([
		{ name: 'shell_bottom', from: [5.5 + s, y, 5.5], to: [10.5 + s, y + 1.25, 10.5], tex: 'shell', tint: true },
		{ name: 'filling', from: [6 + s, y + 1.25, 6], to: [10 + s, y + 2, 10], tex: { all: 'filling', up: null, down: null } },
		{ name: 'shell_top', from: [5.5 + s, y + 2, 5.5], to: [10.5 + s, y + 3.25, 10.5], tex: 'shell', tint: true }
	]);
}
make('macarons', { plate: 'block/plate', shell: 'block/macaron_shell', filling: 'block/macaron_filling' }, macs);

// ---------- Pie (4 slices) ----------
const pie = [
	{ name: 'dish', from: [1, 0, 1], to: [15, 3, 15], tex: { all: 'crust', up: 'filling' } },
	{ name: 'filling', from: [2.5, 3, 2.5], to: [13.5, 3.5, 13.5], tex: { all: 'inside', up: 'filling', down: null }, tint: ['up'] },
	{ name: 'rim_n', from: [1, 3, 1], to: [15, 4, 2.5], tex: { all: 'crust', down: null } },
	{ name: 'rim_s', from: [1, 3, 13.5], to: [15, 4, 15], tex: { all: 'crust', down: null } },
	{ name: 'rim_w', from: [1, 3, 2.5], to: [2.5, 4, 13.5], tex: { all: 'crust', down: null } },
	{ name: 'rim_e', from: [13.5, 3, 2.5], to: [15, 4, 13.5], tex: { all: 'crust', down: null } },
	{ name: 'lattice1', from: [4, 3.5, 2.5], to: [5, 4, 13.5], tex: { all: 'crust', down: null } },
	{ name: 'lattice2', from: [7.5, 3.5, 2.5], to: [8.5, 4, 13.5], tex: { all: 'crust', down: null } },
	{ name: 'lattice3', from: [11, 3.5, 2.5], to: [12, 4, 13.5], tex: { all: 'crust', down: null } },
	{ name: 'lattice4', from: [2.5, 3.6, 5], to: [13.5, 4.1, 6], tex: { all: 'crust', down: null } },
	{ name: 'lattice5', from: [2.5, 3.6, 10], to: [13.5, 4.1, 11], tex: { all: 'crust', down: null } }
];
// The tinted dish top only shows once sliced.
pie[0].tint = ['up'];
const pieTex = { crust: 'block/pie_crust', filling: 'block/pie_filling', inside: 'block/pie_inside' };
for (let b = 0; b < 4; b++) make('pie_slice' + b, pieTex, b === 0 ? pie : slice(pie, 1 + b * 3.5, 'inside'));

// ---------- Cake (4 slices) + toppings ----------
const drip = (x, y0, z0, z1, axis) => axis === 'x'
	? { name: 'drip', from: [x, y0, z0], to: [x + 1, 6.5, z1], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true }
	: { name: 'drip', from: [z0, y0, x], to: [z1, 6.5, x + 1], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true };
const cake = [
	{ name: 'sponge', from: [1, 0, 1], to: [15, 6.5, 15], tex: { all: 'side', down: 'bottom', up: null } },
	{ name: 'frosting', from: [1, 6.5, 1], to: [15, 8, 15], tex: { all: 'frosting', down: null }, tint: true },
	{ name: 'drip', from: [3, 5, 0.75], to: [4, 6.5, 1], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [7, 4.5, 0.75], to: [8, 6.5, 1], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [11, 5.5, 0.75], to: [12, 6.5, 1], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [4.5, 5.5, 15], to: [5.5, 6.5, 15.25], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [9, 4.5, 15], to: [10, 6.5, 15.25], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [12.5, 5, 15], to: [13.5, 6.5, 15.25], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [15, 5, 3.5], to: [15.25, 6.5, 4.5], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [15, 4.5, 8], to: [15.25, 6.5, 9], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [15, 5.5, 12], to: [15.25, 6.5, 13], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true },
	{ name: 'drip', from: [0.75, 5, 6], to: [1, 6.5, 7], tex: { all: 'frosting', down: null, up: null }, tint: true, noInside: true }
];
// The cut face of the frosting layer stays untinted cake inside.
const cakeTex = { side: 'block/cake_side', bottom: 'block/cake_bottom', frosting: 'block/cake_frosting', inside: 'block/cake_inside' };
for (let b = 0; b < 4; b++) make('cake_slice' + b, cakeTex, b === 0 ? cake : slice(cake, 1 + b * 3.5, 'inside'));

const T = 8;
const toppings = {
	strawberries: { tex: { berry: 'block/topping_strawberry', leaf: 'block/leaf' }, cubes: [[3.5, 3.5], [12.5, 3.5], [3.5, 12.5], [12.5, 12.5], [8, 8]].flatMap(([x, z]) => [
		{ name: 'berry', from: [x - 1, T, z - 1], to: [x + 1, T + 2, z + 1], tex: { all: 'berry', down: null } },
		{ name: 'leaf', from: [x - 0.75, T + 2, z - 0.75], to: [x + 0.75, T + 2.4, z + 0.75], tex: { all: 'leaf', down: null } }]) },
	blueberries: { tex: { berry: 'block/topping_blueberry' }, cubes: [[3, 3], [6, 2.5], [10, 3], [13, 3.5], [3, 7], [13, 7.5], [3, 11], [13, 11.5], [4, 13], [8, 13.5], [12, 13], [7.5, 7.5], [9, 9]].map(([x, z]) =>
		({ name: 'berry', from: [x - 0.6, T, z - 0.6], to: [x + 0.6, T + 1.2, z + 0.6], tex: { all: 'berry', down: null } })) },
	cream: { tex: { cream: 'block/topping_cream' }, cubes: [0, 1, 2, 3, 4, 5, 6, 7].flatMap(i => {
		const a = i * Math.PI / 4, x = 8 + Math.cos(a) * 5, z = 8 + Math.sin(a) * 5;
		return [{ name: 'dollop', from: [x - 1, T, z - 1], to: [x + 1, T + 1.5, z + 1], tex: { all: 'cream', down: null } },
			{ name: 'tip', from: [x - 0.5, T + 1.5, z - 0.5], to: [x + 0.5, T + 2.25, z + 0.5], tex: { all: 'cream', down: null } }];
	}) },
	sprinkles: { tex: { sprinkles: 'block/topping_sprinkles' }, cubes: [{ name: 'sprinkles', from: [1, T + 0.02, 1], to: [15, T + 0.02, 15], tex: { up: 'sprinkles' } }] },
	chocolate: { tex: { drizzle: 'block/topping_chocolate', choc: 'block/chocolate' }, cubes: [
		{ name: 'drizzle', from: [1, T + 0.02, 1], to: [15, T + 0.02, 15], tex: { up: 'drizzle' } },
		{ name: 'chunk', from: [6.5, T, 6.5], to: [9.5, T + 1, 8.5], tex: { all: 'choc', down: null } },
		{ name: 'chunk', from: [3, T, 11], to: [5, T + 0.75, 12.5], tex: { all: 'choc', down: null } },
		{ name: 'chunk', from: [11, T, 3.5], to: [13, T + 0.75, 5], tex: { all: 'choc', down: null } }] },
	rose_petals: { tex: { rose: 'block/topping_rose', petal: 'block/cherry' }, cubes: [
		{ name: 'petals', from: [1, T + 0.02, 1], to: [15, T + 0.02, 15], tex: { up: 'rose' } },
		{ name: 'bud', from: [7, T, 7], to: [9, T + 1.5, 9], tex: { all: 'petal', down: null } },
		{ name: 'bud_top', from: [7.5, T + 1.5, 7.5], to: [8.5, T + 2, 8.5], tex: { all: 'petal', down: null } }] },
	lavender: { tex: { bud: 'block/topping_lavender', stem: 'block/leaf' }, cubes: [[4, 4], [12, 4.5], [4.5, 12], [11.5, 11.5], [8, 8]].flatMap(([x, z]) => [
		{ name: 'stem', from: [x - 0.25, T, z - 0.25], to: [x + 0.25, T + 1.5, z + 0.25], tex: { all: 'stem', down: null } },
		{ name: 'bud', from: [x - 0.5, T + 1.5, z - 0.5], to: [x + 0.5, T + 3.5, z + 0.5], tex: { all: 'bud', down: null } }]) },
	star_petals: { tex: { star: 'block/topping_star' }, cubes: [
		{ name: 'star_a', from: [5.5, T, 7.25], to: [10.5, T + 0.75, 8.75], tex: { all: 'star', down: null } },
		{ name: 'star_b', from: [7.25, T, 5.5], to: [8.75, T + 0.75, 10.5], tex: { all: 'star', down: null } },
		{ name: 'star_c', from: [7, T + 0.75, 7], to: [9, T + 1.25, 9], tex: { all: 'star', down: null } },
		{ name: 'sparkle', from: [3, T, 3], to: [4, T + 0.5, 4], tex: { all: 'star', down: null } },
		{ name: 'sparkle', from: [12, T, 12], to: [13, T + 0.5, 13], tex: { all: 'star', down: null } },
		{ name: 'sparkle', from: [12, T, 3.5], to: [13, T + 0.5, 4.5], tex: { all: 'star', down: null } },
		{ name: 'sparkle', from: [3.5, T, 12], to: [4.5, T + 0.5, 13], tex: { all: 'star', down: null } }] }
};
Object.entries(toppings).forEach(([key, t]) => {
	for (let b = 0; b < 4; b++) {
		const cubes = b === 0 ? t.cubes : slice(t.cubes, 1 + b * 3.5, null, 'drop');
		if (cubes.length) make('cake_topping_' + key + '_slice' + b, t.tex, cubes, {});
	}
});

// ---------- Mug ----------
const mugTex = { mug: 'block/mug', inner: 'block/mug_inner', liquid: 'block/liquid' };
make('mug', mugTex, [
	{ name: 'bottom', from: [5, 0, 5], to: [11, 0.5, 11], tex: { all: 'mug', up: 'inner' } },
	{ name: 'wall_n', from: [5, 0, 5], to: [11, 6, 6], tex: { all: 'mug', south: 'inner' } },
	{ name: 'wall_s', from: [5, 0, 10], to: [11, 6, 11], tex: { all: 'mug', north: 'inner' } },
	{ name: 'wall_w', from: [5, 0, 6], to: [6, 6, 10], tex: { all: 'mug', east: 'inner' } },
	{ name: 'wall_e', from: [10, 0, 6], to: [11, 6, 10], tex: { all: 'mug', west: 'inner' } },
	{ name: 'handle_low', from: [11, 1.5, 7.5], to: [12.5, 2.5, 8.5], tex: 'mug' },
	{ name: 'handle', from: [12, 2.5, 7.5], to: [13, 4.5, 8.5], tex: 'mug' },
	{ name: 'handle_high', from: [11, 4.5, 7.5], to: [12.5, 5.5, 8.5], tex: 'mug' }
]);
make('mug_liquid', mugTex, [{ name: 'liquid', from: [6, 5, 6], to: [10, 5, 10], tex: { up: 'liquid' }, tint: true }], {});

return BK.models.length + ' models';
})()
