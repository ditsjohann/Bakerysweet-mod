(function () {
// Seventh wave data, "Cozy Café": machines split into still and moving parts, the cake plate, 26 decor blocks,
// the picket fence, toppers, molds, the bellows, loot, tags and English text. Merges into lang and tag files.
const fs = require('fs');
const MOD = 'C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/src/main/resources/';
const A = MOD + 'assets/bakery/';
const D = MOD + 'data/';
const put = (path, obj) => fs.writeFileSync(path, JSON.stringify(obj, null, '\t') + '\n');
const b = n => 'bakery:block/' + n;
const it = n => 'bakery:item/' + n;
let files = 0;
const model = (name, obj) => { put(A + 'models/block/' + name + '.json', obj); files++; };
const item = (name, obj) => { put(A + 'models/item/' + name + '.json', obj); files++; };
const state = (name, obj) => { put(A + 'blockstates/' + name + '.json', obj); files++; };
const load = name => JSON.parse(fs.readFileSync(A + 'models/block/' + name + '.json'));
const FACINGS = [['north', 0], ['east', 90], ['south', 180], ['west', 270]];
const facingState = (name, modelName) => {
	const v = {};
	FACINGS.forEach(([f, y]) => v['facing=' + f] = { model: b(modelName || name), y });
	state(name, { variants: v });
};
const loot = name => {
	put(D + 'bakery/loot_table/blocks/' + name + '.json', { type: 'minecraft:block', pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:' + name }],
		conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
	files++;
};
const mergeTag = (path, values) => {
	let tag = { replace: false, values: [] };
	if (fs.existsSync(path)) tag = JSON.parse(fs.readFileSync(path));
	values.forEach(v => { if (!tag.values.includes(v)) tag.values.push(v); });
	put(path, tag);
	files++;
};

const cl = v => Math.max(0, Math.min(16, v));
function box(from, to, tex, opts) {
	opts = opts || {};
	const [x1, y1, z1] = from, [x2, y2, z2] = to;
	const w = x2 - x1, h = y2 - y1, d = z2 - z1;
	const uv = {
		north: [cl(16 - x2), cl(16 - y2), cl(16 - x1), cl(16 - y1)], south: [cl(x1), cl(16 - y2), cl(x2), cl(16 - y1)],
		east: [cl(16 - z2), cl(16 - y2), cl(16 - z1), cl(16 - y1)], west: [cl(z1), cl(16 - y2), cl(z2), cl(16 - y1)],
		up: [cl(x1), cl(z1), cl(x2), cl(z2)], down: [cl(x1), cl(16 - z2), cl(x2), cl(16 - z1)]
	};
	if (y2 > 16 || y1 < 0) ['north', 'south', 'east', 'west'].forEach(f => uv[f] = [0, 0, cl(f === 'east' || f === 'west' ? d : w), cl(h)]);
	const faces = {};
	['north', 'south', 'east', 'west', 'up', 'down'].forEach(f => {
		if (opts.skip && opts.skip.includes(f)) return;
		faces[f] = { uv: (opts.uv && opts.uv[f]) || (opts.full && opts.full.includes(f) ? [0, 0, 16, 16] : uv[f]), texture: '#' + ((opts.tex && opts.tex[f]) || tex) };
	});
	const e = { from, to, faces };
	if (opts.rot) e.rotation = opts.rot;
	if (opts.shade === false) e.shade = false;
	return e;
}
const R = (axis, angle, origin) => ({ axis, angle, origin });
// A flat sprite plane facing north/south (for signs, curtains, mirrors): the whole texture on both sides.
const plane = (x1, y1, x2, y2, z, tex) => box([x1, y1, z], [x2, y2, z], tex, { skip: ['east', 'west', 'up', 'down'],
	uv: { north: [16, 0, 0, 16], south: [0, 0, 16, 16] } });
// Crossed sprites like a flower (rotated 45 degrees).
const cross = (x1, y1, x2, y2, tex) => [
	box([x1, y1, 8], [x2, y2, 8], tex, { skip: ['east', 'west', 'up', 'down'], uv: { north: [16, 0, 0, 16], south: [0, 0, 16, 16] }, rot: R('y', 45, [8, 8, 8]), shade: false }),
	box([8, y1, x1], [8, y2, x2], tex, { skip: ['north', 'south', 'up', 'down'], uv: { east: [16, 0, 0, 16], west: [0, 0, 16, 16] }, rot: R('y', 45, [8, 8, 8]), shade: false })];
const BLOCK_DISPLAY = { gui: { rotation: [30, 225, 0], scale: [0.625, 0.625, 0.625] }, ground: { translation: [0, 3, 0], scale: [0.25, 0.25, 0.25] },
	fixed: { scale: [0.5, 0.5, 0.5] }, thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
	firstperson_righthand: { rotation: [0, 45, 0], scale: [0.4, 0.4, 0.4] }, firstperson_lefthand: { rotation: [0, 225, 0], scale: [0.4, 0.4, 0.4] } };
const decor = (name, textures, elements, opts) => {
	opts = opts || {};
	textures.particle = textures.particle || Object.values(textures)[0];
	model(name, { parent: 'minecraft:block/block', ambientocclusion: opts.ao !== false, textures, elements });
	facingState(name);
	item(name, opts.flatItem ? { parent: 'minecraft:item/generated', textures: { layer0: opts.flatItem } } : { parent: b(name), display: opts.display || BLOCK_DISPLAY });
	loot(name);
};

// ============================================================ machines: still parts and moving parts
const named = (m, names) => m.elements.filter(e => names.includes(e.name));
const without = (m, names) => Object.assign({}, m, { elements: m.elements.filter(e => !names.includes(e.name)) });
const part = (name, src, names) => {
	const m = { parent: 'minecraft:block/block', textures: src.textures, elements: named(src, names) };
	put(A + 'models/block/part/' + name + '.json', m); files++;
};
const levelState = (name, modelName) => {
	const v = {};
	FACINGS.forEach(([f, y]) => { for (let l = 0; l <= 3; l++) v['facing=' + f + ',level=' + l] = { model: b(modelName), y }; });
	state(name, { variants: v });
};
{
	const j = load('juicer_1');
	part('juicer_lever', j, ['lever', 'lever_knob']);
	model('juicer', without(j, ['lever', 'lever_knob', 'juice']));
	levelState('juicer', 'juicer');
	item('juicer', { parent: b('juicer'), display: BLOCK_DISPLAY });

	const c = load('ice_cream_churn_1');
	part('churn_crank', c, ['axle', 'arm', 'knob']);
	model('ice_cream_churn', without(c, ['cream']));
	model('ice_cream_churn_static', without(c, ['cream', 'axle', 'arm', 'knob']));
	levelState('ice_cream_churn', 'ice_cream_churn_static');

	const p = load('jam_pot_2');
	part('jam_spoon', p, ['spoon', 'spoon_head']);
	model('jam_pot', without(p, ['jam']));
	model('jam_pot_static', without(p, ['jam', 'spoon', 'spoon_head']));
	levelState('jam_pot', 'jam_pot_static');

	const pb = load('proofing_basket_1');
	model('proofing_basket_static', without(pb, ['dough', 'dough_top']));
	levelState('proofing_basket', 'proofing_basket_static');

	state('mixing_bowl', { variants: { 'filled=false': { model: b('mixing_bowl') }, 'filled=true': { model: b('mixing_bowl') } } });

	const mill = load('grand_millstone');
	const runner = ['runner_a', 'runner_b', 'runner_c', 'axle', 'crossbar', 'handle_w', 'handle_e'];
	part('millstone_runner', mill, runner);
	model('grand_millstone_static', without(mill, runner));
	const mv = {};
	FACINGS.forEach(([f, y]) => [false, true].forEach(a => mv['facing=' + f + ',active=' + a] = { model: b('grand_millstone_static'), y }));
	state('grand_millstone', { variants: mv });

	['grand_oven', 'grand_oven_lit'].forEach(n => model(n, without(load(n), ['log1', 'log2'])));

	// Turntable: a low porcelain plate on a little pedestal (the plate's top is 3 pixels up, where the cake sits).
	model('cake_turntable', { parent: 'minecraft:block/block', textures: { base: b('turntable_base'), plate: b('turntable_plate'), particle: b('turntable_plate') },
		elements: [box([4.5, 0, 4.5], [11.5, 1, 11.5], 'base'), box([7, 1, 7], [9, 2, 9], 'base'),
			box([1.5, 2, 1.5], [14.5, 3, 14.5], 'base', { tex: { up: 'plate' }, uv: { up: [0, 0, 16, 16] } })] });
}

// ============================================================ the cake plate for hand-decorated cakes
model('cake_plate', { parent: 'minecraft:block/block', textures: { plate: b('turntable_plate'), base: b('turntable_base'), particle: b('turntable_plate') },
	elements: [box([1, 0, 1], [15, 1, 15], 'base', { tex: { up: 'plate' }, uv: { up: [0, 0, 16, 16] } })] });
{
	const cake = JSON.parse(fs.readFileSync(A + 'blockstates/cake.json'));
	const tag = w => {
		if (!w) return { custom: 'false' };
		if (w.OR) { w.OR.forEach(o => o.custom = 'false'); return w; }
		w.custom = 'false';
		return w;
	};
	cake.multipart = cake.multipart.filter(p => !(p.when && p.when.custom === 'true'));
	cake.multipart.forEach(p => p.when = tag(p.when));
	FACINGS.forEach(([f, y]) => cake.multipart.push({ when: { custom: 'true', facing: f }, apply: { model: b('cake_plate'), y } }));
	state('cake', cake);
}

// ============================================================ crates and cupcakes, prettier
const fruitBall = (x, z, s, y0) => box([x, y0, z], [x + s, y0 + s * 0.9, z + s], 'fruit');
const crate = (name, slats, pile, fruit, fruitSize) => {
	const els = [
		box([0, 0, 0], [16, 1, 16], 'slat'),
		box([0, 1, 0], [16, 11, 1.5], 'slat', { full: ['north', 'south'] }), box([0, 1, 14.5], [16, 11, 16], 'slat', { full: ['north', 'south'] }),
		box([0, 1, 1.5], [1.5, 11, 14.5], 'slat', { full: ['east', 'west'] }), box([14.5, 1, 1.5], [16, 11, 14.5], 'slat', { full: ['east', 'west'] }),
		box([1.5, 1, 1.5], [14.5, 10.5, 14.5], 'pile', { skip: ['north', 'south', 'east', 'west', 'down'] })];
	const s = fruitSize;
	[[2.5, 2.5], [8.5, 3], [5, 7.5], [10.5, 8.5], [3, 11], [9, 11.5]].forEach(([x, z], i) => els.push(fruitBall(x, z, s - (i % 2) * 0.4, 10.4 + (i % 3 === 1 ? 0.6 : 0))));
	decor(name, { slat: b(slats), pile: b(pile), fruit: b(fruit) }, els);
};
crate('strawberry_crate', 'crate_strawberry', 'pile_strawberry', 'topping_strawberry', 2.6);
crate('lemon_crate', 'crate_lemon', 'pile_lemon', 'lemon_skin', 3.4);
crate('coconut_crate', 'crate_coconut', 'pile_coconut', 'coconut_shell', 4);

// ============================================================ indoor decor
decor('pink_loveseat', { velvet: b('velvet_pink'), gold: b('metal_gold') }, [
	box([1, 1, 2], [15, 6, 15], 'velvet'), box([2, 6, 2.5], [14, 8, 12], 'velvet'),
	box([1, 6, 12], [15, 15, 15], 'velvet', { full: ['north', 'south'] }), box([2.5, 15, 12.5], [13.5, 16.5, 14.5], 'velvet'),
	box([0, 1, 2], [2, 10, 15], 'velvet'), box([14, 1, 2], [16, 10, 15], 'velvet'),
	box([-0.3, 9, 1.8], [2.3, 11, 15.2], 'velvet'), box([13.7, 9, 1.8], [16.3, 11, 15.2], 'velvet'),
	box([1.5, 0, 2.5], [3, 1, 4], 'gold'), box([13, 0, 2.5], [14.5, 1, 4], 'gold'), box([1.5, 0, 13], [3, 1, 14.5], 'gold'), box([13, 0, 13], [14.5, 1, 14.5], 'gold')]);
decor('macaron_pouf', { shell: b('macaron_shell'), cream: b('macaron_cream') }, [
	box([2.5, 0, 2.5], [13.5, 3.5, 13.5], 'shell'), box([2, 0.5, 3], [14, 3, 13], 'shell'), box([3, 0.5, 2], [13, 3, 14], 'shell'),
	box([3, 3.5, 3], [13, 5.5, 13], 'cream'),
	box([2.5, 5.5, 2.5], [13.5, 8.5, 13.5], 'shell'), box([2, 6, 3], [14, 8, 13], 'shell'), box([3, 6, 2], [13, 8, 14], 'shell'),
	box([4, 8.5, 4], [12, 9.2, 12], 'shell')]);
decor('heart_mirror', { mirror: b('heart_mirror') }, [plane(1, 1, 15, 15, 15.6, 'mirror'), plane(1, 1, 15, 15, 15.8, 'mirror')], { flatItem: b('heart_mirror') });
decor('neon_heart_sign', { neon: b('neon_heart'), back: b('neon_back') }, [
	box([1, 2, 15.4], [15, 14, 16], 'back', { full: ['north', 'south'] }),
	Object.assign(plane(0.5, 1.5, 15.5, 14.5, 15.2, 'neon'), { shade: false })], { flatItem: b('neon_heart') });
decor('bunny_plush', { fur: b('plush_white'), face: b('bunny_face'), pink: b('plush_pink') }, [
	box([5, 0, 6], [11, 6, 11], 'fur'), box([4.5, 0, 5], [6.5, 1.5, 7], 'fur'), box([9.5, 0, 5], [11.5, 1.5, 7], 'fur'),
	box([5.2, 6, 6.2], [10.8, 11, 10.8], 'fur', { tex: { north: 'face' }, uv: { north: [0, 3, 16, 14] } }),
	box([5.8, 11, 7.8], [7.3, 15.5, 9], 'fur', { tex: { north: 'pink' } }), box([8.7, 11, 7.8], [10.2, 15.5, 9], 'fur', { tex: { north: 'pink' } }),
	box([3.8, 2.5, 7.5], [5, 5.5, 9.5], 'fur'), box([11, 2.5, 7.5], [12.2, 5.5, 9.5], 'fur'), box([7, 1.5, 10.8], [9, 3.5, 12], 'fur')]);
decor('bear_plush', { fur: b('plush_brown'), face: b('bear_face'), bow: b('velvet_pink') }, [
	box([5, 0, 6], [11, 6, 11], 'fur'), box([4.5, 0, 5], [6.5, 1.5, 7], 'fur'), box([9.5, 0, 5], [11.5, 1.5, 7], 'fur'),
	box([5, 6, 6], [11, 11, 11], 'fur', { tex: { north: 'face' }, uv: { north: [0, 3, 16, 14] } }),
	box([4.5, 10, 7.5], [6.5, 12, 9], 'fur'), box([9.5, 10, 7.5], [11.5, 12, 9], 'fur'),
	box([3.8, 2.5, 7.5], [5, 5.5, 9.5], 'fur'), box([11, 2.5, 7.5], [12.2, 5.5, 9.5], 'fur'),
	box([6, 5.6, 5.6], [10, 6.6, 6.2], 'bow'), box([6.8, 5.2, 5.4], [9.2, 7, 5.8], 'bow')]);
decor('tulip_vase', { vase: b('ceramic_pink'), flowers: b('tulips') }, [
	box([6, 0, 6], [10, 5, 10], 'vase'), box([6.5, 5, 6.5], [9.5, 6, 9.5], 'vase'), box([5.8, 6, 5.8], [10.2, 6.8, 10.2], 'vase'),
	...cross(2, 5, 14, 16, 'flowers')]);
decor('tea_set', { china: b('porcelain_floral'), white: b('ceramic_white'), tea: b('tea') }, [
	box([1.5, 0, 2.5], [14.5, 0.6, 13.5], 'white'),
	box([3, 0.6, 5], [8, 5, 10], 'china'), box([3.5, 5, 5.5], [7.5, 5.8, 9.5], 'white'), box([5, 5.8, 7], [6, 6.6, 8], 'china'),
	box([8, 2, 7], [10, 3, 8], 'china', { rot: R('z', 22.5, [8, 2.5, 7.5]) }), box([1.8, 2, 7], [3, 4.5, 8], 'china'),
	box([10, 0.6, 3.5], [13, 3, 6.5], 'china', { tex: { up: 'tea' } }), box([10, 0.6, 9], [13, 3, 12], 'china', { tex: { up: 'tea' } })]);
decor('cake_shelf', { wood: b('wood_pink'), cakes: b('mini_cakes') }, [
	box([0, 2, 10], [16, 3, 16], 'wood'), box([0, 9, 10], [16, 10, 16], 'wood'), box([0.5, 2, 15], [1.5, 16, 16], 'wood'), box([14.5, 2, 15], [15.5, 16, 16], 'wood'),
	box([1, 3, 11], [15, 12, 15], 'cakes', { skip: ['up', 'down', 'east', 'west', 'south'], uv: { north: [0, 4, 16, 13] } }),
	box([1, 10, 11], [15, 19, 15], 'cakes', { skip: ['up', 'down', 'east', 'west', 'south'], uv: { north: [0, 4, 16, 13] } })]);
decor('donut_wall', { board: b('donut_wall'), wood: b('wood_mint') }, [
	box([1, 1, 15], [15, 15, 16], 'board', { full: ['north', 'south'] }), box([0.5, 0.5, 14.6], [15.5, 1.5, 16], 'wood'), box([0.5, 14.5, 14.6], [15.5, 15.5, 16], 'wood')]);
decor('lace_curtains', { lace: b('lace_curtains') }, [plane(0, 0, 16, 16, 15, 'lace'), plane(0, 0, 16, 16, 15.2, 'lace')], { flatItem: b('lace_curtains') });
decor('star_lights', { lights: b('star_lights') }, [box([0, 0, 15.5], [16, 16, 15.5], 'lights', { skip: ['east', 'west', 'up', 'down'], uv: { north: [0, 0, 16, 16], south: [16, 0, 0, 16] }, shade: false })],
	{ flatItem: b('star_lights'), ao: false });
decor('cash_register', { body: b('register_body'), keys: b('register_keys'), screen: b('register_display'), gold: b('metal_gold') }, [
	box([2, 0, 3], [14, 4, 14], 'body'), box([2.5, 4, 4], [13.5, 6, 14], 'body', { tex: { up: 'keys' }, uv: { up: [0, 0, 16, 16] } }),
	box([3.5, 6, 9], [12.5, 11, 13.5], 'body'), box([4.5, 11, 10], [11.5, 13, 12.5], 'screen', { full: ['north', 'south'] }),
	box([13, 4, 7], [15, 5, 8], 'gold'), box([14, 5, 7], [15, 8, 8], 'gold'), box([13.6, 8, 6.6], [15.4, 9.4, 8.4], 'gold')]);
decor('tip_jar', { glass: b('jar_glass'), coins: b('coin_pile'), label: b('tip_label'), lid: b('metal_gold') }, [
	box([5.5, 0.2, 5.5], [10.5, 4, 10.5], 'coins'), box([5, 0, 5], [11, 8, 11], 'glass', { skip: ['up'] }),
	box([6, 2, 4.9], [10, 6, 4.9], 'label', { skip: ['east', 'west', 'up', 'down', 'south'], uv: { north: [2, 4, 14, 12] } }),
	box([4.6, 8, 4.6], [11.4, 9, 11.4], 'lid')]);
decor('open_sign', { sign: b('open_sign'), wood: b('wood_cream') }, [
	box([2, 4, 15.3], [14, 11.5, 15.8], 'sign', { uv: { north: [16, 4, 0, 13], south: [0, 4, 16, 13] } }),
	box([4, 11.5, 15.5], [4.5, 15, 15.6], 'wood', { rot: R('z', -22.5, [4, 11.5, 15.5]) }), box([11.5, 11.5, 15.5], [12, 15, 15.6], 'wood', { rot: R('z', 22.5, [12, 11.5, 15.5]) })],
	{ flatItem: b('open_sign') });

// ============================================================ outdoor decor
decor('chalkboard_sign', { board: b('chalkboard'), wood: b('wood_pink') }, [
	box([2, 0, 6.5], [14, 15, 7.5], 'board', { rot: R('x', -22.5, [8, 0, 8]), uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } }),
	box([2, 0, 8.5], [14, 15, 9.5], 'board', { rot: R('x', 22.5, [8, 0, 8]), uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } }),
	box([7, 13.2, 6.8], [9, 14.6, 9.2], 'wood')]);
decor('pastel_bench', { wood: b('wood_mint'), metal: b('metal_white') }, [
	box([0, 6, 3], [16, 7.5, 6.5], 'wood'), box([0, 6, 7], [16, 7.5, 10.5], 'wood'),
	box([0, 9, 11.2], [16, 11.5, 12.4], 'wood'), box([0, 12.5, 11.2], [16, 15, 12.4], 'wood'),
	box([1, 0, 3.5], [2.2, 6, 4.7], 'metal'), box([13.8, 0, 3.5], [15, 6, 4.7], 'metal'), box([1, 0, 10], [2.2, 15.5, 11.2], 'metal'),
	box([13.8, 0, 10], [15, 15.5, 11.2], 'metal'), box([0.6, 9, 3], [2.6, 10, 11], 'metal'), box([13.4, 9, 3], [15.4, 10, 11], 'metal'),
	box([1.2, 7.5, 3.5], [2, 9, 4.3], 'metal'), box([14, 7.5, 3.5], [14.8, 9, 4.3], 'metal')]);
decor('flower_box', { wood: b('wood_pink'), soil: b('soil'), flowers: b('box_flowers') }, [
	box([0, 0, 10], [16, 5, 16], 'wood', { skip: ['up'] }), box([0.5, 4, 10.5], [15.5, 4.5, 15.5], 'soil'),
	box([0, 4, 13], [16, 14, 13], 'flowers', { skip: ['east', 'west', 'up', 'down'], uv: { north: [16, 4, 0, 14], south: [0, 4, 16, 14] }, shade: false }),
	box([0, 4, 11.5], [16, 13, 11.5], 'flowers', { skip: ['east', 'west', 'up', 'down'], uv: { north: [0, 4, 16, 13], south: [16, 4, 0, 13] }, shade: false })]);
decor('heart_lamp_post', { metal: b('metal_pink'), glow: b('heart_lantern'), gold: b('metal_gold') }, [
	box([5.5, 0, 5.5], [10.5, 1.5, 10.5], 'metal'), box([7.2, 1.5, 7.2], [8.8, 10, 8.8], 'metal'),
	box([5, 10, 7.5], [11, 16, 8.5], 'glow', { shade: false, full: ['north', 'south'] }), box([7.5, 10, 5], [8.5, 16, 11], 'glow', { shade: false, full: ['east', 'west'] }),
	box([6, 16, 6], [10, 16.8, 10], 'gold'), box([7.5, 16.8, 7.5], [8.5, 18, 8.5], 'gold')], { ao: false });
decor('parasol_table', { wood: b('wood_cream'), cloth: b('parasol'), metal: b('metal_white') }, [
	box([4, 0, 4], [12, 1, 12], 'metal'), box([7.3, 1, 7.3], [8.7, 13, 8.7], 'metal'), box([1, 13, 1], [15, 14.5, 15], 'wood'),
	box([7.6, 14.5, 7.6], [8.4, 27, 8.4], 'metal'),
	box([-2, 24, -2], [18, 25.5, 18], 'cloth', { uv: { up: [0, 0, 16, 16], down: [0, 0, 16, 16] } }),
	box([1, 25.5, 1], [15, 27, 15], 'cloth', { uv: { up: [0, 0, 16, 16] } }), box([4.5, 27, 4.5], [11.5, 28, 11.5], 'cloth'),
	box([7.3, 28, 7.3], [8.7, 29.2, 8.7], 'metal')]);
decor('tulip_pot', { pot: b('flower_pot_peach'), soil: b('soil'), flowers: b('tulips') }, [
	box([4, 0, 4], [12, 6, 12], 'pot', { full: ['north', 'south', 'east', 'west'] }), box([3.5, 6, 3.5], [12.5, 7.2, 12.5], 'pot'), box([4.5, 6, 4.5], [11.5, 6.6, 11.5], 'soil'),
	...cross(1.5, 6, 14.5, 16, 'flowers')]);
decor('hanging_flowers', { basket: b('wicker'), flowers: b('trailing_flowers'), chain: b('metal_gold') }, [
	box([4, 6, 4], [12, 10, 12], 'basket'), box([4.5, 9.5, 4.5], [11.5, 10.5, 11.5], 'basket'),
	box([7.6, 10, 4.5], [8.4, 16, 5.3], 'chain', { rot: R('x', 22.5, [8, 10, 4.9]) }), box([7.6, 10, 10.7], [8.4, 16, 11.5], 'chain', { rot: R('x', -22.5, [8, 10, 11.1]) }),
	...cross(1, 1, 15, 13, 'flowers')]);
decor('bunny_statue', { stone: b('stone_cream'), pink: b('ceramic_pink') }, [
	box([3, 0, 3.5], [13, 1.5, 12.5], 'stone'), box([4.5, 1.5, 5], [11.5, 8, 11.5], 'stone'), box([5.5, 8, 5.5], [10.5, 12, 10], 'stone'),
	box([6, 12, 7], [7.5, 16.5, 8.5], 'stone', { tex: { north: 'pink' } }), box([8.5, 12, 7], [10, 16.5, 8.5], 'stone', { tex: { north: 'pink' } }),
	box([7.2, 3, 11.5], [8.8, 4.6, 12.8], 'stone'), box([4, 1.5, 4.5], [6, 3, 7], 'stone'), box([10, 1.5, 4.5], [12, 3, 7], 'stone')]);
decor('pink_mailbox', { box: b('mailbox'), front: b('mailbox_front'), post: b('wood_cream'), flag: b('metal_gold') }, [
	box([7, 0, 7], [9, 9, 9], 'post'), box([5, 9, 3], [11, 14, 13], 'box', { tex: { north: 'front' }, uv: { north: [0, 2, 16, 14] } }),
	box([5.5, 14, 3], [10.5, 15, 13], 'box'), box([6.5, 15, 3], [9.5, 15.6, 13], 'box'),
	box([11, 12, 9], [11.6, 16, 9.8], 'flag'), box([11, 14.6, 9.8], [11.6, 16, 12], 'flag')]);
decor('pastry_cart', { wood: b('wood_lilac'), cloth: b('parasol'), glass: b('pastry_display'), metal: b('metal_white') }, [
	box([1, 4, 3], [15, 12, 13], 'wood', { tex: { north: 'glass' }, uv: { north: [0, 1, 16, 15] } }), box([0.5, 12, 2.5], [15.5, 13, 13.5], 'wood'),
	box([0, 1, 4], [1, 7, 9], 'metal'), box([15, 1, 4], [16, 7, 9], 'metal'), box([-0.4, 3, 6], [16.4, 4, 7], 'metal'),
	box([2, 0, 11], [3, 4, 12], 'metal'), box([13, 0, 11], [14, 4, 12], 'metal'),
	box([1.5, 13, 12], [2.5, 22, 13], 'metal'), box([13.5, 13, 12], [14.5, 22, 13], 'metal'),
	box([-1, 22, 1], [17, 23.5, 14], 'cloth', { rot: R('x', -22.5, [8, 22, 13]), uv: { up: [0, 0, 16, 16], down: [0, 0, 16, 16] } })]);
decor('welcome_mat', { mat: b('welcome_mat') }, [box([0, 0, 1], [16, 0.8, 15], 'mat', { uv: { up: [0, 0, 16, 16] } })]);

// The pink picket fence: vanilla fence shapes with pastel planks.
model('pink_picket_fence_post', { parent: 'minecraft:block/fence_post', textures: { texture: b('wood_pink') } });
model('pink_picket_fence_side', { parent: 'minecraft:block/fence_side', textures: { texture: b('wood_pink') } });
model('pink_picket_fence_inventory', { parent: 'minecraft:block/fence_inventory', textures: { texture: b('wood_pink') } });
state('pink_picket_fence', { multipart: [
	{ apply: { model: b('pink_picket_fence_post') } },
	{ when: { north: 'true' }, apply: { model: b('pink_picket_fence_side'), uvlock: true } },
	{ when: { east: 'true' }, apply: { model: b('pink_picket_fence_side'), y: 90, uvlock: true } },
	{ when: { south: 'true' }, apply: { model: b('pink_picket_fence_side'), y: 180, uvlock: true } },
	{ when: { west: 'true' }, apply: { model: b('pink_picket_fence_side'), y: 270, uvlock: true } }] });
item('pink_picket_fence', { parent: b('pink_picket_fence_inventory') });
loot('pink_picket_fence');

model('bow_wallpaper', { parent: 'minecraft:block/cube_all', textures: { all: b('bow_wallpaper') } });
state('bow_wallpaper', { variants: { '': { model: b('bow_wallpaper') } } });
item('bow_wallpaper', { parent: b('bow_wallpaper') });
loot('bow_wallpaper');

// ============================================================ items: bellows, molds, toppers (little 3D figurines)
['bellows', 'round_cake_mold', 'square_cake_mold', 'heart_cake_mold'].forEach(n => item(n, { parent: 'minecraft:item/handheld', textures: { layer0: it(n) } }));
const TOPPER_DISPLAY = { gui: { rotation: [20, 200, 0], translation: [0, 0, 0], scale: [0.9, 0.9, 0.9] }, ground: { scale: [0.5, 0.5, 0.5] },
	fixed: { scale: [1, 1, 1] }, thirdperson_righthand: { rotation: [0, 0, 0], translation: [0, 1, 1], scale: [0.5, 0.5, 0.5] },
	firstperson_righthand: { rotation: [0, -30, 0], translation: [0, 2, 0], scale: [0.6, 0.6, 0.6] },
	firstperson_lefthand: { rotation: [0, 150, 0], translation: [0, 2, 0], scale: [0.6, 0.6, 0.6] } };
const topper = (name, textures, elements) => {
	textures.particle = textures.particle || Object.values(textures)[0];
	item(name, { parent: 'minecraft:block/block', textures, elements, display: TOPPER_DISPLAY });
};
topper('fondant_heart', { p: b('ceramic_pink') }, [
	box([4, 4, 7], [8, 12, 9], 'p'), box([8, 4, 7], [12, 12, 9], 'p'), box([2.5, 8, 7], [7.5, 13, 9], 'p'), box([8.5, 8, 7], [13.5, 13, 9], 'p'),
	box([5.5, 2, 7], [10.5, 5, 9], 'p'), box([7, 0.5, 7.2], [9, 2.5, 8.8], 'p')]);
topper('fondant_star', { y: b('metal_gold') }, [
	box([6, 1, 7], [10, 13, 9], 'y'), box([2, 7, 7], [14, 10, 9], 'y'), box([4, 3, 7.2], [7, 7, 8.8], 'y', { rot: R('z', 22.5, [5.5, 5, 8]) }),
	box([9, 3, 7.2], [12, 7, 8.8], 'y', { rot: R('z', -22.5, [10.5, 5, 8]) })]);
topper('fondant_bunny', { fur: b('plush_white'), face: b('bunny_face'), pink: b('plush_pink') }, [
	box([4.5, 0, 5], [11.5, 6, 11], 'fur'), box([5, 6, 5.5], [11, 11, 10.5], 'fur', { tex: { north: 'face' }, uv: { north: [0, 3, 16, 14] } }),
	box([5.8, 11, 7.5], [7.4, 16, 9], 'fur', { tex: { north: 'pink' } }), box([8.6, 11, 7.5], [10.2, 16, 9], 'fur', { tex: { north: 'pink' } }),
	box([7, 1.5, 10.8], [9, 3.5, 12], 'fur')]);
topper('sugar_flower', { petal: b('plush_pink'), heart: b('metal_gold'), leaf: b('wood_mint') }, [
	box([6, 0, 6], [10, 3, 10], 'leaf'), box([2, 3, 6], [14, 6, 10], 'petal'), box([6, 3, 2], [10, 6, 14], 'petal'),
	box([3.5, 3, 3.5], [12.5, 6, 12.5], 'petal', { rot: R('y', 45, [8, 4.5, 8]) }), box([6, 6, 6], [10, 8, 10], 'heart')]);
topper('mini_macaron', { shell: b('wood_lilac'), cream: b('macaron_cream') }, [
	box([2.5, 0, 2.5], [13.5, 4, 13.5], 'shell'), box([3, 4, 3], [13, 6.5, 13], 'cream'), box([2.5, 6.5, 2.5], [13.5, 10.5, 13.5], 'shell'),
	box([4, 10.5, 4], [12, 11.2, 12], 'shell')]);
topper('sugar_pearls', { pearl: b('ceramic_white') }, [
	box([3, 0, 3], [7, 4, 7], 'pearl'), box([9, 0, 4], [13, 4, 8], 'pearl'), box([5, 0, 9], [9, 4, 13], 'pearl'), box([7, 3.5, 6], [11, 7.5, 10], 'pearl')]);
topper('chocolate_curl', { c: b('tea') }, [
	box([7, 0, 6], [9, 14, 10], 'c'), box([5, 4, 6.5], [7, 10, 9.5], 'c'), box([9, 6, 6.5], [11, 12, 9.5], 'c')]);
topper('sugar_butterfly', { wing: b('ceramic_white'), body: b('neon_back'), tint: b('velvet_mint') }, [
	box([7.4, 2, 7.4], [8.6, 12, 8.6], 'body'),
	box([1, 6, 7.6], [7.4, 14, 8.4], 'tint', { rot: R('y', 22.5, [7.4, 10, 8]) }), box([8.6, 6, 7.6], [15, 14, 8.4], 'tint', { rot: R('y', -22.5, [8.6, 10, 8]) }),
	box([2.5, 1, 7.7], [7.4, 6, 8.3], 'wing', { rot: R('y', 22.5, [7.4, 4, 8]) }), box([8.6, 1, 7.7], [13.5, 6, 8.3], 'wing', { rot: R('y', -22.5, [8.6, 4, 8]) })]);
topper('golden_crown', { gold: b('metal_gold'), gem: b('ceramic_pink') }, [
	box([2, 0, 2], [14, 5, 3], 'gold'), box([2, 0, 13], [14, 5, 14], 'gold'), box([2, 0, 3], [3, 5, 13], 'gold'), box([13, 0, 3], [14, 5, 13], 'gold'),
	box([2, 5, 2], [4, 9, 4], 'gold'), box([12, 5, 2], [14, 9, 4], 'gold'), box([2, 5, 12], [4, 9, 14], 'gold'), box([12, 5, 12], [14, 9, 14], 'gold'),
	box([7, 5, 1.6], [9, 10, 3.4], 'gold'), box([7, 2, 1.6], [9, 4, 1.6], 'gem', { skip: ['east', 'west', 'up', 'down', 'south'] }),
	box([7.2, 10, 2], [8.8, 11.6, 3], 'gem')]);
topper('cake_flag', { stick: b('wood_cream'), flag: b('velvet_pink') }, [
	box([7.5, 0, 7.5], [8.5, 15, 8.5], 'stick'), box([8.5, 9.5, 7.8], [14, 14, 8.2], 'flag'), box([8.5, 10.5, 7.7], [12.5, 13, 8.3], 'flag')]);

// ============================================================ tags
const COZY = ['pink_loveseat', 'macaron_pouf', 'heart_mirror', 'neon_heart_sign', 'bunny_plush', 'bear_plush', 'tulip_vase', 'tea_set', 'cake_shelf',
	'donut_wall', 'lace_curtains', 'star_lights', 'cash_register', 'tip_jar', 'open_sign', 'chalkboard_sign', 'pastel_bench', 'flower_box',
	'heart_lamp_post', 'parasol_table', 'tulip_pot', 'hanging_flowers', 'bunny_statue', 'pink_mailbox', 'pastry_cart', 'welcome_mat'];
mergeTag(D + 'bakery/tags/block/cozy.json', COZY.map(n => 'bakery:' + n));
mergeTag(D + 'minecraft/tags/block/mineable/axe.json', ['cake_shelf', 'donut_wall', 'open_sign', 'chalkboard_sign', 'pastel_bench', 'flower_box',
	'parasol_table', 'pastry_cart', 'pink_picket_fence'].map(n => 'bakery:' + n));
mergeTag(D + 'minecraft/tags/block/mineable/pickaxe.json', ['heart_mirror', 'neon_heart_sign', 'tulip_vase', 'tea_set', 'cash_register', 'tip_jar',
	'heart_lamp_post', 'tulip_pot', 'bunny_statue', 'pink_mailbox'].map(n => 'bakery:' + n));
mergeTag(D + 'minecraft/tags/block/wooden_fences.json', ['bakery:pink_picket_fence']);
mergeTag(D + 'minecraft/tags/item/wooden_fences.json', ['bakery:pink_picket_fence']);
mergeTag(D + 'minecraft/tags/block/fences.json', ['bakery:pink_picket_fence']);

// ============================================================ English
const lang = {
	'entity.bakery.customer': 'Customer',
	'block.bakery.pink_loveseat': 'Pink Loveseat', 'block.bakery.macaron_pouf': 'Macaron Pouf', 'block.bakery.heart_mirror': 'Heart Mirror',
	'block.bakery.neon_heart_sign': 'Neon Heart Sign', 'block.bakery.bunny_plush': 'Bunny Plushie', 'block.bakery.bear_plush': 'Teddy Bear',
	'block.bakery.tulip_vase': 'Tulip Vase', 'block.bakery.tea_set': 'Rose Tea Set', 'block.bakery.cake_shelf': 'Cake Shelf',
	'block.bakery.donut_wall': 'Donut Wall', 'block.bakery.lace_curtains': 'Lace Curtains', 'block.bakery.star_lights': 'Star String Lights',
	'block.bakery.bow_wallpaper': 'Bow Wallpaper', 'block.bakery.cash_register': 'Pastel Cash Register', 'block.bakery.tip_jar': 'Tip Jar',
	'block.bakery.open_sign': 'Open Sign', 'block.bakery.chalkboard_sign': 'Chalkboard Sign', 'block.bakery.pastel_bench': 'Pastel Bench',
	'block.bakery.flower_box': 'Window Flower Box', 'block.bakery.pink_picket_fence': 'Pink Picket Fence', 'block.bakery.heart_lamp_post': 'Heart Lamp Post',
	'block.bakery.parasol_table': 'Parasol Table', 'block.bakery.tulip_pot': 'Tulip Pot', 'block.bakery.hanging_flowers': 'Hanging Flowers',
	'block.bakery.bunny_statue': 'Bunny Statue', 'block.bakery.pink_mailbox': 'Pink Mailbox', 'block.bakery.pastry_cart': 'Pastry Cart',
	'block.bakery.welcome_mat': 'Welcome Mat',
	'item.bakery.bellows': 'Bellows', 'tooltip.bakery.bellows': 'Puff air into the Grand Oven to make the fire hotter',
	'item.bakery.round_cake_mold': 'Round Cake Mould', 'item.bakery.square_cake_mold': 'Square Cake Mould', 'item.bakery.heart_cake_mold': 'Heart Cake Mould',
	'tooltip.bakery.cake_mold': 'Use on a cake on the Cake Turntable to reshape it',
	'item.bakery.fondant_heart': 'Fondant Heart', 'item.bakery.fondant_star': 'Fondant Star', 'item.bakery.fondant_bunny': 'Fondant Bunny',
	'item.bakery.sugar_flower': 'Sugar Flower', 'item.bakery.mini_macaron': 'Mini Macaron', 'item.bakery.sugar_pearls': 'Sugar Pearls',
	'item.bakery.chocolate_curl': 'Chocolate Curl', 'item.bakery.sugar_butterfly': 'Sugar Butterfly', 'item.bakery.golden_crown': 'Golden Crown',
	'item.bakery.cake_flag': 'Cake Flag', 'tooltip.bakery.cake_topper': 'Click a cake on the Cake Turntable to stand it there · sneak to lift one off',
	'tooltip.bakery.icing_tip': 'Tip: %s', 'tooltip.bakery.icing_bag': 'Pipe onto a cake on the Cake Turntable · sneak-use to swap tips · ices Sugar Cookies held in your other hand',
	'tip.bakery.dots': 'Dots', 'tip.bakery.rosettes': 'Rosettes', 'tip.bakery.line': 'Line (click point to point)', 'tip.bakery.border': 'Border',
	'tip.bakery.drips': 'Drips', 'tip.bakery.frost': 'Frost a whole tier',
	'message.bakery.icing.tip': 'Piping tip: %s', 'message.bakery.icing.hint': 'Tip: %s · pipe onto a cake on the Cake Turntable (sneak-use to swap tips)',
	'message.bakery.customer.wants': '%s would love: %s', 'message.bakery.customer.hello': '%s waves hello! %s',
	'message.bakery.shop.wrong': '%s wanted %s, not that!', 'message.bakery.shop.gift': '%s left you a little present: %s!',
	'message.bakery.level.unlocked': 'Your bakery is now a %s! New on the menu:',
	'cafe_level.bakery.stall': 'Little Stall', 'cafe_level.bakery.corner': 'Cozy Corner', 'cafe_level.bakery.cafe': 'Sweet Café',
	'cafe_level.bakery.patisserie': 'Pâtisserie', 'cafe_level.bakery.grand': 'Grand Bakery', 'cafe_level.bakery.up': 'Your bakery grew!',
	'message.bakery.welcome': '♥ Welcome, baker! Place your Bakery Catalog to buy your first kitchen - you have 40 Sweet Coins. Its Read tab has the whole guide.',
	'message.bakery.catalog.locked': 'Unlocks when your bakery becomes a %s',
	'catalog.bakery.kitchen': 'Kitchen', 'catalog.bakery.pantry': 'Pantry', 'catalog.bakery.garden': 'Garden', 'catalog.bakery.decor': 'Decor',
	'catalog.bakery.outdoor': 'Outdoors', 'catalog.bakery.wardrobe': 'Outfits',
	'screen.bakery.catalog.level': 'Lv %s · %s', 'screen.bakery.catalog.read': 'Read the Guide', 'screen.bakery.catalog.lv': 'Lv %s',
	'screen.bakery.catalog.next_level': '%s more customers until %s', 'screen.bakery.catalog.max_level': 'Your bakery is as grand as it gets ♥',
	'screen.bakery.catalog.locked': 'Unlocks at Lv %s · %s', 'screen.bakery.catalog.special': '★ %s · %sc',
	'screen.bakery.menu': "Today's Menu", 'screen.bakery.menu.level': 'Lv %s · %s', 'screen.bakery.menu.locked': 'Lv %s',
	'screen.bakery.menu.hint': 'Tick what customers may order', 'message.bakery.menu.no_counter': 'Put the menu board near a Bakery Counter',
	'message.bakery.oven.vent': 'You open the damper - the fire cools a little',
	'message.bakery.oven.empty': 'Nothing in the oven yet - click it with a raw bake',
	'message.bakery.turntable.tall': 'Three tiers is as tall as it gets!', 'message.bakery.turntable.tier': 'Tier %s stacked on ♥',
	'message.bakery.turntable.shaped': 'Reshaped into a %s cake', 'message.bakery.turntable.miss': 'Click right on the cake',
	'message.bakery.turntable.full_icing': 'There is no room left for more icing', 'message.bakery.turntable.on_top': 'Toppers go on top of a tier',
	'message.bakery.turntable.full_toppers': 'That is plenty of toppers!',
	'message.bakery.turntable.placed': 'Pipe with an icing bag, add toppers, stack cakes · empty hand spins it · sneak to take it',
	'message.bakery.turntable.empty': 'Put a whole cake on the turntable first',
	'cake_shape.bakery.0': 'round', 'cake_shape.bakery.1': 'square', 'cake_shape.bakery.2': 'heart',
	'popup.bakery.coins': '+%s coins', 'popup.bakery.hint': 'loves %s', 'popup.bakery.loved': 'Their favourite! ♥',

	'guide.bakery.cozy_cafe.title': 'The Cozy Café',
	'guide.bakery.cozy_cafe.text': 'Everything happens in the world now: watch fruit squash in the juicer, the stone turn, the fire grow. Customers walk in, sit down to eat or take treats home, and your bakery grows a level at a time.',
	'guide.bakery.first_day.text': '1. Place your Bakery Catalog and buy a counter, bowl, board and oven heart.\n2. Mix dough, shape it, bake it.\n3. Open the counter: your first customer only wants bread or cookies!\n4. Every customer helps your bakery grow.',
	'guide.bakery.starter_shopping.title': 'First Shopping List',
	'guide.bakery.starter_shopping.text': 'With your 40 coins: a Bakery Counter, Mixing Bowl, Pastry Board, Rolling Pin, an Oven Heart and 16 Bricks, plus Flour, Butter, Sugar and a Water Bucket from the Pantry. Seeds are in the Garden tab.',
	'guide.bakery.tending.title': 'Tending the Fire',
	'guide.bakery.tending.text': 'Watch the gauge on the oven: puff the Bellows to heat it, sneak-click to open the damper and cool it. Keep the needle on the green notch. Golden sparkles mean perfect - click the bake to peel it out!',
	'guide.bakery.cake_studio.title': 'Cake Turntable',
	'guide.bakery.cake_studio.text': 'Put a cake on the turntable. Click with more cakes to stack up to three tiers, press a Mould to make it square or a heart. Click with an empty hand to spin it round.',
	'guide.bakery.piping.title': 'Piping',
	'guide.bakery.piping.text': 'Click the cake with an Icing Bag to pipe exactly there. Sneak-use the bag to change tips: dots, rosettes, lines (click point to point), a border, drips down the side, or frost a whole tier. Sneak-click to scrape it off.',
	'guide.bakery.toppers.title': 'Toppers',
	'guide.bakery.toppers.text': 'Fondant hearts and bunnies, sugar flowers, mini macarons, a golden crown, candles, fruit... click the top of a tier to stand one there. More tiers, shapes, colours and matching pairs = cuter = more stars!',
	'guide.bakery.customers.title': 'Customers',
	'guide.bakery.customers.text': 'Regulars walk in, queue at the counter and show what they want. Hand it over! Some take it home, others find a free café chair, eat or drink there and leave a tip on the table.',
	'guide.bakery.levels.title': 'Growing Your Bakery',
	'guide.bakery.levels.text': 'Little Stall → Cozy Corner (5 served) → Sweet Café (15) → Pâtisserie (35) → Grand Bakery (70). Each level brings new dishes, more customers and new things in the Catalog.',
	'guide.bakery.menu.title': 'The Menu Board',
	'guide.bakery.menu.text': 'Right-click a Menu Board near your counter to choose which dishes customers may order. Only ready for bread today? Untick the rest!',
	'guide.bakery.friends.text': 'Every regular has a name, a favourite flavour and one they dislike. 4-5 star service and gifts earn hearts: at 5 they are a regular, at 10 your best friend - with a present!',
	'guide.bakery.gifts.text': 'Hold anything in your other hand and use a Gift Box to wrap it. Give it to a customer once a day: +2 hearts, +3 if it is their favourite flavour.',
	'guide.bakery.charm.text': 'Cozy decorations within 6 blocks of a counter give it Charm hearts: plushies, lamps, rugs, furniture, flowers... Charm brings more customers who wait longer and tip more.',
	'guide.bakery.catalog.text': 'Right-click the Catalog to shop with Sweet Coins. Fancier goodies unlock as your bakery grows. One item each day is the Daily Special.',
	'guide.bakery.cozy_decor.title': 'Cozy Inside',
	'guide.bakery.cozy_decor.text': 'Loveseats and macaron poufs to sit on, plushies, a heart mirror, a neon heart, tea sets, cake shelves, lace curtains, a pastel register and a tip jar.',
	'guide.bakery.outdoor.title': 'Pretty Outside',
	'guide.bakery.outdoor.text': 'Parasol tables, pastel benches, a pink picket fence, heart lamp posts, flower boxes, a bunny statue, a mailbox, a pastry cart and a welcome mat.',
	'guide.bakery.juicer.text': 'Drop a fruit on the juicer and pull the lever five times - watch it squash! Each fruit makes two glasses; fill bottles from the jug.',
	'guide.bakery.millstone.text': 'Load the Grand Millstone, then push the crank round with an empty hand. Keep a steady rhythm and the stone turns twice as fast. Flour pours out of the spout.'
};
const langPath = A + 'lang/en_us.json';
const current = JSON.parse(fs.readFileSync(langPath));
Object.assign(current, lang);
put(langPath, current);
return files + ' files + ' + Object.keys(lang).length + ' lang keys';
})()
