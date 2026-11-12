(function () {
// Third wave of models: kitchen machines, cores, the Grand Millstone, the Chocolate Fountain,
// new hanging fruit, corn and peas, the special crops and the zucchini.
const MB = window.MB;
const { make } = MB;
const NOAO = { parent: 'block/block', ambientocclusion: false };
const C = (name, from, to, tex, extra) => Object.assign({ name, from, to, tex }, extra || {});
const hollow = (x1, y1, z1, x2, y2, z2, t, outer, inner) => [
	C('wall_n', [x1, y1, z1], [x2, y2, z1 + t], { all: outer, south: inner }),
	C('wall_s', [x1, y1, z2 - t], [x2, y2, z2], { all: outer, north: inner }),
	C('wall_w', [x1, y1, z1 + t], [x1 + t, y2, z2 - t], { all: outer, east: inner }),
	C('wall_e', [x2 - t, y1, z1 + t], [x2, y2, z2 - t], { all: outer, west: inner })
];

// ================= Juicer (translucent glass jug) =================
const juicerTex = { body: 'block/juicer_body', glass: 'block/juicer_glass', metal: 'block/metal', juice: 'block/juice_liquid', brass: 'block/brass' };
for (let level = 0; level < 4; level++) {
	const cubes = [
		C('base', [3, 0, 3], [13, 3, 13], 'body'),
		C('jug', [4, 3, 4], [12, 10, 12], { all: 'glass', down: null }),
		C('reamer_plate', [4.5, 10, 4.5], [11.5, 11, 11.5], 'metal'),
		C('reamer', [6.5, 11, 6.5], [9.5, 13, 9.5], 'metal'),
		C('reamer_tip', [7.5, 13, 7.5], [8.5, 14, 8.5], 'metal'),
		C('lever', [11.5, 10, 7.5], [15, 11, 8.5], 'brass'),
		C('lever_knob', [14, 9.5, 7], [15.5, 11.5, 9], 'brass'),
		C('spout', [7.5, 5, 3], [8.5, 6, 4], 'metal')
	];
	if (level > 0) cubes.push(C('juice', [4.5, 3.05, 4.5], [11.5, 3 + level * 2.2, 11.5], 'juice', { tint: true }));
	make('juicer_' + level, juicerTex, cubes, NOAO);
}

// ================= Ice Cream Churn =================
const churnTex = { wood: 'block/churn_wood', metal: 'block/metal', cream: 'block/ice_cream_block' };
for (let level = 0; level < 4; level++) {
	const cubes = [C('bottom', [3.5, 0, 3.5], [12.5, 1, 12.5], 'wood')].concat(hollow(3, 0, 3, 13, 11, 13, 1, 'wood', 'wood'), [
		C('band_low', [2.8, 2, 2.8], [13.2, 3, 13.2], { all: 'metal', up: null, down: null }),
		C('band_high', [2.8, 8, 2.8], [13.2, 9, 13.2], { all: 'metal', up: null, down: null }),
		C('axle', [13, 8, 7.5], [15, 9, 8.5], 'metal'),
		C('arm', [14.5, 8, 7.5], [15.5, 13, 8.5], 'wood'),
		C('knob', [13.5, 12, 7], [15.5, 13.5, 9], 'wood')
	]);
	if (level > 0) cubes.push(C('cream', [4, 1, 4], [12, 1 + level * 3, 12], { all: 'cream', down: null }, { tint: true }));
	make('ice_cream_churn_' + level, churnTex, cubes, NOAO);
}

// ================= Jam Pot =================
const potTex = { pot: 'block/copper_pot', jam: 'block/jam_surface', wood: 'block/churn_wood' };
for (let level = 0; level < 4; level++) {
	const cubes = [C('bottom', [3.5, 0, 3.5], [12.5, 1, 12.5], 'pot')].concat(hollow(3, 0, 3, 13, 8, 13, 1, 'pot', 'pot'), [
		C('handle_w', [1.5, 6, 7], [3, 7, 9], 'pot'),
		C('handle_e', [13, 6, 7], [14.5, 7, 9], 'pot'),
		C('rim', [2.7, 7.5, 2.7], [13.3, 8.2, 13.3], { all: 'pot', up: null, down: null })
	]);
	if (level > 0) {
		cubes.push(C('jam', [4, 1, 4], [12, [0, 3, 5, 6.5][level], 12], { all: 'jam', down: null }, { tint: true }));
		cubes.push(C('spoon', [9, 3, 7.5], [10, 13, 8.5], 'wood', { rot: ['z', -22.5, [9.5, 5, 8]] }));
		cubes.push(C('spoon_head', [8.5, 2, 7], [10.5, 3.5, 9], 'wood', { rot: ['z', -22.5, [9.5, 5, 8]] }));
	}
	make('jam_pot_' + level, potTex, cubes, NOAO);
}

// ================= Proofing Basket =================
const proofTex = { basket: 'block/banneton', linen: 'block/linen', dough: 'block/proof_dough' };
for (let level = 0; level < 4; level++) {
	const cubes = [C('bottom', [2.5, 0, 2.5], [13.5, 1, 13.5], { all: 'basket', up: 'linen' })].concat(hollow(2, 0, 2, 14, 5, 14, 1, 'basket', 'linen'));
	if (level === 0) {
		cubes.push(C('cloth', [1.5, 4.5, 1.5], [14.5, 5.2, 7], 'linen'));
	} else {
		const h = [0, 2.5, 4.5, 6][level], r = [0, 4, 3.3, 2.8][level];
		cubes.push(C('dough', [r, 1, r], [16 - r, 1 + h, 16 - r], { all: 'dough', down: null }));
		cubes.push(C('dough_top', [r + 1.5, 1 + h, r + 1.5], [16 - r - 1.5, 1.8 + h, 16 - r - 1.5], { all: 'dough', down: null }));
	}
	make('proofing_basket_' + level, proofTex, cubes, NOAO);
}

// ================= Cores =================
make('millstone_core', { stone: 'block/mill/stone_bricks', front: 'block/millstone_core' }, [
	C('block', [0, 0, 0], [16, 16, 16], { all: 'front' })
]);
make('fountain_core', { core: 'block/fountain_core' }, [C('block', [0, 0, 0], [16, 16, 16], 'core')]);

// ================= Grand Millstone (3x3, rendered from the core) =================
const millTex = { stone: { file: 'block/mill/stone_bricks', big: true }, mill: { file: 'block/mill/millstone', big: true }, wood: { file: 'block/mill/wood', big: true } };
make('grand_millstone', millTex, [
	C('plinth', [-16, -16, -16], [32, 0, 32], 'stone'),
	C('bed_a', [-11, 0, -6], [27, 5, 22], { all: 'mill', down: null }),
	C('bed_b', [-6, 0, -11], [22, 5, 27], { all: 'mill', down: null }),
	C('bed_c', [-9, 0, -9], [25, 5, 25], { all: 'mill', down: null }),
	C('runner_a', [-5, 5, -5], [21, 10, 21], { all: 'mill', down: null }),
	C('runner_b', [-3, 5, -7], [19, 10, 23], { all: 'mill', down: null }),
	C('runner_c', [-7, 5, -3], [23, 10, 19], { all: 'mill', down: null }),
	C('axle', [6, 10, 6], [10, 22, 10], 'wood'),
	C('crossbar', [-12, 19, 6], [28, 21, 10], 'wood'),
	C('handle_w', [-12, 12, 6.5], [-10, 19, 9.5], 'wood'),
	C('handle_e', [26, 12, 6.5], [28, 19, 9.5], 'wood'),
	C('spout', [5, 2, -16], [11, 4, -10], 'wood'),
	C('post_w', [-14, 0, 5], [-10, 13, 11], 'stone'),
	C('post_e', [26, 0, 5], [30, 13, 11], 'stone'),
	C('post_s', [5, 0, 26], [11, 13, 30], 'stone')
], NOAO);

// ================= Chocolate Fountain (3x3x3, rendered from the core) =================
function fountain(active) {
	const tex = { ceramic: { file: 'block/fountain/ceramic', big: true }, gold: { file: 'block/fountain/gold', big: true }, choc: { file: 'block/fountain/chocolate', big: true } };
	const cubes = [
		C('floor', [-12, 0, -12], [28, 1, 28], 'ceramic'),
		C('rim_n', [-14, 0, -14], [30, 4, -12], 'ceramic'), C('rim_s', [-14, 0, 28], [30, 4, 30], 'ceramic'),
		C('rim_w', [-14, 0, -12], [-12, 4, 28], 'ceramic'), C('rim_e', [28, 0, -12], [30, 4, 28], 'ceramic'),
		C('pedestal', [3, 1, 3], [13, 6, 13], 'ceramic'),
		C('column', [6, 6, 6], [10, 26, 10], 'gold'),
		C('tier1', [-2, 10, -2], [18, 12, 18], 'ceramic'),
		C('tier2', [2, 18, 2], [14, 20, 14], 'ceramic'),
		C('crown', [5, 26, 5], [11, 30, 11], 'gold'),
		C('crown_tip', [7, 30, 7], [9, 32, 9], 'gold')
	];
	if (active) {
		cubes.push(
			C('pool', [-12, 3, -12], [28, 3, 28], { up: 'choc' }),
			C('coat1', [-2, 12, -2], [18, 12.4, 18], { all: 'choc', down: null }),
			C('coat2', [2, 20, 2], [14, 20.4, 14], { all: 'choc', down: null }),
			C('sheet1_n', [-2, 3, -2.3], [18, 10, -2.1], { north: 'choc', south: 'choc' }),
			C('sheet1_s', [-2, 3, 18.1], [18, 10, 18.3], { north: 'choc', south: 'choc' }),
			C('sheet1_w', [-2.3, 3, -2], [-2.1, 10, 18], { west: 'choc', east: 'choc' }),
			C('sheet1_e', [18.1, 3, -2], [18.3, 10, 18], { west: 'choc', east: 'choc' }),
			C('sheet2_n', [2, 12.4, 1.7], [14, 18, 1.9], { north: 'choc', south: 'choc' }),
			C('sheet2_s', [2, 12.4, 14.1], [14, 18, 14.3], { north: 'choc', south: 'choc' }),
			C('sheet2_w', [1.7, 12.4, 2], [1.9, 18, 14], { west: 'choc', east: 'choc' }),
			C('sheet2_e', [14.1, 12.4, 2], [14.3, 18, 14], { west: 'choc', east: 'choc' })
		);
	}
	make('chocolate_fountain' + (active ? '_active' : ''), tex, cubes, NOAO);
}
fountain(false);
fountain(true);

// ================= Hanging fruit =================
function fruitModel(name, key, w, h, y0, extra) {
	make(name, { fruit: 'block/' + key, stem: 'block/fruit_stem' }, [
		C('stem', [7.6, Math.min(15, y0 + h), 7.6], [8.4, 16, 8.4], 'stem'),
		C('fruit', [8 - w / 2, y0, 8 - w / 2], [8 + w / 2, y0 + h, 8 + w / 2], 'fruit')
	].concat(extra || []), NOAO);
}
const pair = (key, s, y) => [
	C('stem_a', [5.6, y + s, 7.6], [6.4, 16, 8.4], 'stem'), C('stem_b', [9.6, y + s, 7.6], [10.4, 16, 8.4], 'stem'),
	C('fruit_a', [6 - s / 2, y, 8 - s / 2], [6 + s / 2, y + s, 8 + s / 2], 'fruit'),
	C('fruit_b', [10 - s / 2, y - 0.5, 8 - s / 2], [10 + s / 2, y + s - 0.5, 8 + s / 2], 'fruit')
];
make('cherries_stage0', { fruit: 'block/fruit_young', stem: 'block/fruit_stem' }, pair('fruit', 1.2, 12.5), NOAO);
make('cherries_stage1', { fruit: 'block/fruit_young', stem: 'block/fruit_stem' }, pair('fruit', 1.8, 11), NOAO);
make('cherries_stage2', { fruit: 'block/fruit_cherry', stem: 'block/fruit_stem' }, pair('fruit', 2.4, 10), NOAO);
[['orange', 'fruit_orange', [3, 3, 12], [4.5, 4.5, 10.5], [5.5, 5.5, 9.5]],
	['peach', 'fruit_peach', [3, 3, 12], [4, 4, 11], [5, 5, 10]],
	['mango', 'fruit_mango', [2.5, 3.5, 11.5], [3.5, 5, 10], [4.5, 6.5, 8.5]],
	['plum', 'fruit_plum', [2.5, 2.5, 12.5], [3.5, 3.5, 11.5], [4.5, 4.8, 10.5]]].forEach(([name, key, a, b, c]) => {
	fruitModel(name + '_stage0', 'fruit_young', a[0], a[1], a[2]);
	fruitModel(name + '_stage1', name === 'orange' || name === 'mango' ? 'fruit_young' : key, b[0], b[1], b[2]);
	fruitModel(name + '_stage2', key, c[0], c[1], c[2]);
});

// ================= Crops =================
const planes = key => [
	C('p1', [4, -1, 0], [4, 15, 16], { east: key, west: key }, { uvAll: [0, 0, 16, 16], shade: false }),
	C('p2', [12, -1, 0], [12, 15, 16], { east: key, west: key }, { uvAll: [0, 0, 16, 16], shade: false }),
	C('p3', [0, -1, 4], [16, 15, 4], { north: key, south: key }, { uvAll: [0, 0, 16, 16], shade: false }),
	C('p4', [0, -1, 12], [16, 15, 12], { north: key, south: key }, { uvAll: [0, 0, 16, 16], shade: false })
];
// Full-height cross planes for the tall stalks (no -1 offset so segments stack seamlessly).
const tallPlanes = key => [
	C('d1', [0.8, 0, 8], [15.2, 16, 8], { north: key, south: key }, { uvAll: [0, 0, 16, 16], shade: false, rot: ['y', 45, [8, 8, 8]], rescale: true }),
	C('d2', [8, 0, 0.8], [8, 16, 15.2], { east: key, west: key }, { uvAll: [0, 0, 16, 16], shade: false, rot: ['y', 45, [8, 8, 8]], rescale: true })
];
const cropModel = (name, texKey, extraTex, extra, tall) => make(name, Object.assign({ plant: 'block/' + texKey }, extraTex || {}),
	(tall ? tallPlanes('plant') : planes('plant')).concat(extra || []), NOAO);
['corn_stage0', 'corn_stage1'].forEach(n => cropModel(n, n));
['corn_bottom', 'corn_middle', 'corn_middle_ripe', 'corn_top'].forEach(n => cropModel(n, n, null, null, true));
const trellis = [C('pole_a', [3.5, 0, 3.5], [4.5, 16, 4.5], 'trellis'), C('pole_b', [11.5, 0, 11.5], [12.5, 16, 12.5], 'trellis')];
cropModel('pea_stage0', 'pea_stage0');
cropModel('pea_stage1', 'pea_stage1', { trellis: 'block/trellis' }, trellis);
['pea_bottom', 'pea_top', 'pea_bottom_ripe', 'pea_top_ripe'].forEach(n => cropModel(n, n, { trellis: 'block/trellis' }, trellis));
const bits = (key, list, h) => list.map(([x, y, z, s]) => C('bit', [x - s, y, z - s], [x + s, y + (h || 1.6) * s, z + s], key));
const special = {
	sweet_potato_crop: { key: 'fruit_sweet_potato', list: [[5, 0, 6, 1.4], [10, 0, 10, 1.2], [9, 0, 4.5, 1]], h: 0.9 },
	sugar_beet_crop: { key: 'fruit_sugar_beet', list: [[8, 0, 8, 2]], h: 1.1 },
	rhubarb_crop: { key: 'fruit_rhubarb', list: [[5, 0, 6, 0.6], [10, 0, 7, 0.6], [7.5, 0, 11, 0.6]], h: 10 },
	hibiscus_crop: { key: 'fruit_hibiscus', list: [[4, 9, 6, 1.3], [11.5, 10, 5, 1.3], [8, 12, 11, 1.4]], h: 0.6 },
	cranberry_crop: { key: 'fruit_cranberry', list: [[4, 3, 5, 0.6], [6, 4.5, 11, 0.6], [11, 3.5, 9, 0.6], [12, 5, 4, 0.6], [8, 2, 8, 0.6]], h: 1.6 },
	candyfloss_crop: { key: 'fruit_fluff', list: [[5, 8, 5, 2], [11, 10, 6, 2.2], [8, 12, 11, 2.4]], h: 1.4, tintless: true },
	peanut_crop: null
};
Object.entries(special).forEach(([name, f]) => {
	for (let s = 0; s < 4; s++) {
		const extra = [];
		const tex = {};
		if (f && (s === 3 || (s === 2 && (name === 'rhubarb_crop' || name === 'candyfloss_crop')))) {
			tex.fruit = 'block/' + f.key;
			extra.push(...bits('fruit', s === 2 ? f.list.slice(0, 2) : f.list, s === 2 && name === 'rhubarb_crop' ? 6 : f.h));
		}
		cropModel(name + '_stage' + s, name + '_stage' + s, tex, extra);
	}
});
make('zucchini_block', { skin: 'block/zucchini_skin', end: 'block/zucchini_end', flower: 'block/fruit_star' }, [
	C('body', [2, 0, 5], [14, 4.5, 11], { all: 'skin', east: 'end', west: 'end' }, { uv: { east: [0, 0, 16, 16], west: [0, 0, 16, 16] } }),
	C('top', [3, 4.5, 6], [13, 5.5, 10], { all: 'skin', down: null }),
	C('flower', [13.5, 1, 6.5], [15.5, 4, 9.5], 'flower')
], NOAO);

return MB.models.length + ' models';
})()
