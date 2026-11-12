(function () {
const ART = window.ART, fs = ART.fs, path = ART.path;
const RES = 'C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/src/main/resources/';
const A = RES + 'assets/bakery/', D = RES + 'data/';
let count = 0;
const write = (file, obj) => { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, JSON.stringify(obj, null, '\t')); count++; };
const m = n => 'bakery:block/' + n;
const FACING = { north: 0, east: 90, south: 180, west: 270 };
const bs = (name, obj) => write(A + 'blockstates/' + name + '.json', obj);
const model = (name, obj) => write(A + 'models/block/' + name + '.json', obj);
const im = (name, obj) => write(A + 'models/item/' + name + '.json', obj);
const gen = (name, layers) => im(name, { parent: 'minecraft:item/generated', textures: Object.fromEntries((layers || ['item/' + name]).map((l, i) => ['layer' + i, 'bakery:' + l])) });
const facingVariants = (modelName, extraKey) => {
	const v = {};
	Object.entries(FACING).forEach(([f, y]) => v['facing=' + f + (extraKey || '')] = { model: m(modelName), y });
	return { variants: v };
};
const multipart = fn => { const parts = []; Object.entries(FACING).forEach(([f, y]) => fn(f, y, parts)); return { multipart: parts }; };

// ================= Oven =================
const oven = {};
Object.entries(FACING).forEach(([f, y]) => {
	oven['facing=' + f + ',lit=false'] = { model: m('grand_oven'), y };
	oven['facing=' + f + ',lit=true'] = { model: m('grand_oven_lit'), y };
});
bs('grand_oven', { variants: oven });
const PART_KINDS = { brick: 'minecraft:bricks', oven_heart: 'bakery:oven_heart', stone: 'minecraft:stone_bricks', wall: 'minecraft:cobblestone_wall',
	terracotta: 'minecraft:white_terracotta', gold: 'minecraft:gold_block' };
bs('multiblock_part', { variants: Object.fromEntries(Object.keys(PART_KINDS).map(k => ['kind=' + k, { model: m('multiblock_part') }])) });
model('multiblock_part', { textures: { particle: 'bakery:block/oven_bricks' } });
bs('oven_heart', facingVariants('oven_heart'));
im('oven_heart', { parent: m('oven_heart') });

// ================= Kitchen =================
bs('mixing_bowl', { variants: { 'filled=false': { model: m('mixing_bowl') }, 'filled=true': { model: m('mixing_bowl_filled') } } });
const board = {};
Object.entries(FACING).forEach(([f, y]) => {
	board['facing=' + f + ',has_dough=false'] = { model: m('pastry_board'), y };
	board['facing=' + f + ',has_dough=true'] = { model: m('pastry_board_dough'), y };
});
bs('pastry_board', { variants: board });
im('mixing_bowl', { parent: m('mixing_bowl_filled') });
im('pastry_board', { parent: m('pastry_board_dough') });

// ================= Shop & decor =================
const counter = {};
Object.entries(FACING).forEach(([f, y]) => {
	counter['facing=' + f + ',open=true'] = { model: m('bakery_counter_open'), y };
	counter['facing=' + f + ',open=false'] = { model: m('bakery_counter_closed'), y };
});
bs('bakery_counter', { variants: counter });
im('bakery_counter', { parent: m('bakery_counter_open') });
['display_case', 'service_bell', 'cafe_table', 'cafe_chair', 'flour_sack', 'bread_basket', 'cake_stand', 'menu_board', 'bunting', 'jar_shelf',
	'strawberry_crate', 'lemon_crate', 'coconut_crate'].forEach(n => { bs(n, facingVariants(n)); im(n, { parent: m(n) }); });
['pink_tiles', 'mint_tiles', 'lavender_tiles', 'cream_tiles'].forEach(n => {
	model(n, { parent: 'minecraft:block/cube_all', textures: { all: m(n) } });
	bs(n, { variants: { '': { model: m(n) } } });
	im(n, { parent: m(n) });
});

// ================= Placed bakes =================
['bread_loaf', 'baguette', 'croissant'].forEach(n => bs(n, facingVariants(n)));
bs('cupcake', multipart((f, y, p) => [1, 2, 3, 4].forEach(c => p.push({ when: { facing: f, count: '' + c }, apply: { model: m('cupcake_' + c), y } }))));
bs('donut', multipart((f, y, p) => [1, 2, 3, 4].forEach(c => p.push({ when: { facing: f, count: '' + c }, apply: { model: m('donut_' + Math.min(c, 3)), y } }))));
bs('cookies', multipart((f, y, p) => p.push({ when: { facing: f }, apply: { model: m('cookies'), y } })));
bs('macarons', multipart((f, y, p) => p.push({ when: { facing: f }, apply: { model: m('macarons'), y } })));
bs('pie', multipart((f, y, p) => [0, 1, 2, 3].forEach(b => p.push({ when: { facing: f, bites: '' + b }, apply: { model: m('pie_slice' + b), y } }))));
const TOPPINGS = ['strawberries', 'blueberries', 'cream', 'sprinkles', 'chocolate', 'rose_petals', 'lavender', 'star_petals', 'raspberries', 'coconut_flakes', 'mint', 'lemon_slices',
	'cherries', 'candyfloss'];
bs('cake', multipart((f, y, p) => [0, 1, 2, 3].forEach(b => {
	p.push({ when: { facing: f, bites: '' + b }, apply: { model: m('cake_slice' + b), y } });
	TOPPINGS.forEach(t => {
		if (fs.existsSync(A + 'models/block/cake_topping_' + t + '_slice' + b + '.json')) {
			p.push({ when: { facing: f, bites: '' + b, topping: t }, apply: { model: m('cake_topping_' + t + '_slice' + b), y } });
		}
	});
})));
const DRINKS = ['matcha_latte', 'hot_chocolate', 'strawberry_milk', 'lavender_tea', 'rose_tea', 'starlight_tea', 'lemonade', 'coconut_milk', 'mint_tea', 'pineapple_smoothie'];
const mug = multipart((f, y, p) => p.push({ when: { facing: f }, apply: { model: m('mug'), y } }));
mug.multipart.push({ when: { drink: DRINKS.join('|') }, apply: { model: m('mug_liquid') } });
bs('mug', mug);

// ================= Crops & fruit =================
const CROPS = ['strawberry', 'blueberry', 'raspberry', 'tea', 'mint', 'vanilla', 'lavender', 'rose', 'pineapple', 'starbloom',
	'sweet_potato', 'peanut', 'rhubarb', 'hibiscus', 'cranberry', 'candyfloss', 'sugar_beet'];
const ALL_WOODS = ['palm', 'citrus', 'blossom', 'orangewood', 'peachwood', 'mangowood', 'plumwood'];
const FRUITS = ['coconut', 'lemon', 'cherries', 'orange', 'peach', 'mango', 'plum'];
CROPS.forEach(c => {
	const v = {};
	for (let a = 0; a < 4; a++) v['age=' + a] = { model: m(c + '_crop_stage' + a) };
	bs(c + '_crop', { variants: v });
});
FRUITS.forEach(f => bs(f, { variants: { 'age=0': { model: m(f + '_stage0') }, 'age=1': { model: m(f + '_stage1') }, 'age=2': { model: m(f + '_stage2') } } }));

// ================= Items =================
['bakery_guide', 'sweetheart_spatula', 'rolling_pin', 'whisk'].forEach(n => im(n, { parent: 'minecraft:item/handheld', textures: { layer0: 'bakery:item/' + n } }));
im('bakery_guide', { parent: 'minecraft:item/generated', textures: { layer0: 'bakery:item/bakery_guide' } });
CROPS.forEach(c => gen(c + '_seeds'));
['strawberry', 'blueberry', 'raspberry', 'tea_leaves', 'mint_leaves', 'vanilla_pod', 'lavender', 'rose_petals', 'pineapple', 'star_petals', 'coconut', 'lemon',
	'flour', 'butter', 'whipped_cream', 'chocolate_bar', 'matcha_powder', 'coconut_flakes', 'sprinkles', 'candied_petals', 'glass_jar',
	'strawberry_jam', 'blueberry_jam', 'raspberry_jam', 'wax_paper', 'frost_wrap', 'mug',
	'raw_loaf', 'raw_baguette', 'raw_croissant', 'raw_cupcake', 'raw_donut', 'raw_cookies', 'raw_macarons', 'raw_cake', 'raw_pie',
	'bread_loaf', 'baguette', 'croissant', 'burnt_pastry'].concat(DRINKS).forEach(n => gen(n));
['bread_dough', 'sweet_dough', 'pastry_dough', 'cookie_dough'].forEach(n => gen(n, ['item/' + n, 'item/dough_flavor']));
[['cupcake', 'cupcake_frosting'], ['donut', 'donut_glaze'], ['cookies', 'cookies_chips'], ['macarons', 'macarons_shells'], ['cake', 'cake_frosting'], ['pie', 'pie_filling']]
	.forEach(([a, b]) => gen(a, ['item/' + a, 'item/' + b]));

// ================= Wood sets =================
function woodSet(w) {
	const tex = n => 'bakery:block/' + n;
	model(w + '_log', { parent: 'minecraft:block/cube_column', textures: { end: tex(w + '_log_top'), side: tex(w + '_log') } });
	model(w + '_log_horizontal', { parent: 'minecraft:block/cube_column_horizontal', textures: { end: tex(w + '_log_top'), side: tex(w + '_log') } });
	model(w + '_wood', { parent: 'minecraft:block/cube_column', textures: { end: tex(w + '_log'), side: tex(w + '_log') } });
	model('stripped_' + w + '_log', { parent: 'minecraft:block/cube_column', textures: { end: tex('stripped_' + w + '_log_top'), side: tex('stripped_' + w + '_log') } });
	model('stripped_' + w + '_log_horizontal', { parent: 'minecraft:block/cube_column_horizontal', textures: { end: tex('stripped_' + w + '_log_top'), side: tex('stripped_' + w + '_log') } });
	model('stripped_' + w + '_wood', { parent: 'minecraft:block/cube_column', textures: { end: tex('stripped_' + w + '_log'), side: tex('stripped_' + w + '_log') } });
	const axis = n => ({ variants: { 'axis=x': { model: m(n + (n.endsWith('wood') ? '' : '_horizontal')), x: 90, y: 90 }, 'axis=y': { model: m(n) }, 'axis=z': { model: m(n + (n.endsWith('wood') ? '' : '_horizontal')), x: 90 } } });
	[w + '_log', w + '_wood', 'stripped_' + w + '_log', 'stripped_' + w + '_wood'].forEach(n => { bs(n, axis(n)); im(n, { parent: m(n) }); });

	const planks = tex(w + '_planks');
	model(w + '_planks', { parent: 'minecraft:block/cube_all', textures: { all: planks } });
	bs(w + '_planks', { variants: { '': { model: m(w + '_planks') } } });
	im(w + '_planks', { parent: m(w + '_planks') });

	const sides = { bottom: planks, top: planks, side: planks };
	model(w + '_stairs', { parent: 'minecraft:block/stairs', textures: sides });
	model(w + '_stairs_inner', { parent: 'minecraft:block/inner_stairs', textures: sides });
	model(w + '_stairs_outer', { parent: 'minecraft:block/outer_stairs', textures: sides });
	const stairs = {};
	const base = { east: 0, south: 90, west: 180, north: 270 };
	Object.entries(base).forEach(([f, y]) => {
		const r = d => (y + d + 360) % 360;
		const put = (half, shape, mdl, rot, x) => { const v = { model: m(w + mdl), uvlock: true }; if (rot) v.y = rot; if (x) v.x = x; stairs['facing=' + f + ',half=' + half + ',shape=' + shape] = v; };
		put('bottom', 'straight', '_stairs', r(0));
		put('bottom', 'outer_right', '_stairs_outer', r(0));
		put('bottom', 'outer_left', '_stairs_outer', r(-90));
		put('bottom', 'inner_right', '_stairs_inner', r(0));
		put('bottom', 'inner_left', '_stairs_inner', r(-90));
		put('top', 'straight', '_stairs', r(0), 180);
		put('top', 'outer_right', '_stairs_outer', r(90), 180);
		put('top', 'outer_left', '_stairs_outer', r(0), 180);
		put('top', 'inner_right', '_stairs_inner', r(90), 180);
		put('top', 'inner_left', '_stairs_inner', r(0), 180);
	});
	bs(w + '_stairs', { variants: stairs });
	im(w + '_stairs', { parent: m(w + '_stairs') });

	model(w + '_slab', { parent: 'minecraft:block/slab', textures: sides });
	model(w + '_slab_top', { parent: 'minecraft:block/slab_top', textures: sides });
	bs(w + '_slab', { variants: { 'type=bottom': { model: m(w + '_slab') }, 'type=top': { model: m(w + '_slab_top') }, 'type=double': { model: m(w + '_planks') } } });
	im(w + '_slab', { parent: m(w + '_slab') });

	model(w + '_fence_post', { parent: 'minecraft:block/fence_post', textures: { texture: planks } });
	model(w + '_fence_side', { parent: 'minecraft:block/fence_side', textures: { texture: planks } });
	model(w + '_fence_inventory', { parent: 'minecraft:block/fence_inventory', textures: { texture: planks } });
	bs(w + '_fence', { multipart: [
		{ apply: { model: m(w + '_fence_post') } },
		{ when: { north: 'true' }, apply: { model: m(w + '_fence_side'), uvlock: true } },
		{ when: { east: 'true' }, apply: { model: m(w + '_fence_side'), y: 90, uvlock: true } },
		{ when: { south: 'true' }, apply: { model: m(w + '_fence_side'), y: 180, uvlock: true } },
		{ when: { west: 'true' }, apply: { model: m(w + '_fence_side'), y: 270, uvlock: true } }] });
	im(w + '_fence', { parent: m(w + '_fence_inventory') });

	['', '_open', '_wall', '_wall_open'].forEach(s => model(w + '_fence_gate' + s, { parent: 'minecraft:block/template_fence_gate' + s, textures: { texture: planks } }));
	const gate = {};
	const gy = { south: 0, west: 90, north: 180, east: 270 };
	Object.entries(gy).forEach(([f, y]) => [false, true].forEach(wall => [false, true].forEach(open => {
		gate['facing=' + f + ',in_wall=' + wall + ',open=' + open] = { model: m(w + '_fence_gate' + (wall ? '_wall' : '') + (open ? '_open' : '')), y, uvlock: true };
	})));
	bs(w + '_fence_gate', { variants: gate });
	im(w + '_fence_gate', { parent: m(w + '_fence_gate') });

	const doorTex = { bottom: tex(w + '_door_bottom'), top: tex(w + '_door_top') };
	['bottom_left', 'bottom_left_open', 'bottom_right', 'bottom_right_open', 'top_left', 'top_left_open', 'top_right', 'top_right_open']
		.forEach(s => model(w + '_door_' + s, { parent: 'minecraft:block/door_' + s, textures: doorTex }));
	const door = {};
	Object.entries(base).forEach(([f, y]) => ['lower', 'upper'].forEach(half => ['left', 'right'].forEach(hinge => [false, true].forEach(open => {
		const part = (half === 'lower' ? 'bottom_' : 'top_') + hinge + (open ? '_open' : '');
		let rot = y;
		if (open) rot = hinge === 'left' ? (y + 90) % 360 : (y + 270) % 360;
		const v = { model: m(w + '_door_' + part) };
		if (rot) v.y = rot;
		door['facing=' + f + ',half=' + half + ',hinge=' + hinge + ',open=' + open] = v;
	}))));
	bs(w + '_door', { variants: door });
	gen(w + '_door', ['item/' + w + '_door']);

	const tdTex = { texture: tex(w + '_trapdoor') };
	model(w + '_trapdoor_bottom', { parent: 'minecraft:block/template_orientable_trapdoor_bottom', textures: tdTex });
	model(w + '_trapdoor_top', { parent: 'minecraft:block/template_orientable_trapdoor_top', textures: tdTex });
	model(w + '_trapdoor_open', { parent: 'minecraft:block/template_orientable_trapdoor_open', textures: tdTex });
	const trap = {};
	const ty = { north: 0, east: 90, south: 180, west: 270 };
	Object.entries(ty).forEach(([f, y]) => {
		const yy = v => (v ? { y: v } : {});
		trap['facing=' + f + ',half=bottom,open=false'] = Object.assign({ model: m(w + '_trapdoor_bottom') }, yy(y));
		trap['facing=' + f + ',half=top,open=false'] = Object.assign({ model: m(w + '_trapdoor_top') }, yy(y));
		trap['facing=' + f + ',half=bottom,open=true'] = Object.assign({ model: m(w + '_trapdoor_open') }, yy(y));
		trap['facing=' + f + ',half=top,open=true'] = Object.assign({ model: m(w + '_trapdoor_open'), x: 180 }, yy((y + 180) % 360));
	});
	bs(w + '_trapdoor', { variants: trap });
	im(w + '_trapdoor', { parent: m(w + '_trapdoor_bottom') });

	model(w + '_leaves', { parent: 'minecraft:block/leaves', textures: { all: tex(w + '_leaves') } });
	bs(w + '_leaves', { variants: { '': { model: m(w + '_leaves') } } });
	im(w + '_leaves', { parent: m(w + '_leaves') });
	model(w + '_sapling', { parent: 'minecraft:block/cross', textures: { cross: tex(w + '_sapling') } });
	bs(w + '_sapling', { variants: { 'stage=0': { model: m(w + '_sapling') }, 'stage=1': { model: m(w + '_sapling') } } });
	im(w + '_sapling', { parent: 'minecraft:item/generated', textures: { layer0: tex(w + '_sapling') } });
}
ALL_WOODS.forEach(woodSet);

// ================= Loot tables =================
const lt = (name, obj) => write(D + 'bakery/loot_table/blocks/' + name + '.json', Object.assign({ type: 'minecraft:block' }, obj));
const self = name => lt(name, { pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:' + name }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
['oven_heart', 'mixing_bowl', 'pastry_board', 'bakery_counter', 'display_case', 'service_bell', 'cafe_table', 'cafe_chair', 'flour_sack', 'bread_basket',
	'cake_stand', 'menu_board', 'bunting', 'jar_shelf', 'strawberry_crate', 'lemon_crate', 'coconut_crate', 'pink_tiles', 'mint_tiles', 'lavender_tiles', 'cream_tiles'].forEach(self);
lt('multiblock_part', { pools: [{ rolls: 1, entries: [{ type: 'minecraft:alternatives', children: Object.entries(PART_KINDS).map(([k, item]) =>
	({ type: 'minecraft:item', name: item, conditions: [{ condition: 'minecraft:block_state_property', block: 'bakery:multiblock_part', properties: { kind: k } }] })) }] }] });
const PRODUCE = { strawberry: 'strawberry', blueberry: 'blueberry', raspberry: 'raspberry', tea: 'tea_leaves', mint: 'mint_leaves', vanilla: 'vanilla_pod',
	lavender: 'lavender', rose: 'rose_petals', pineapple: 'pineapple', starbloom: 'star_petals', sweet_potato: 'sweet_potato', peanut: 'peanut',
	rhubarb: 'rhubarb', hibiscus: 'hibiscus', cranberry: 'cranberry', candyfloss: 'candyfloss', sugar_beet: 'sugar_beet' };
CROPS.forEach(c => {
	const mature = { condition: 'minecraft:block_state_property', block: 'bakery:' + c + '_crop', properties: { age: '3' } };
	const few = c === 'starbloom' || c === 'pineapple';
	lt(c + '_crop', {
		pools: [
			{ rolls: 1, entries: [{ type: 'minecraft:alternatives', children: [
				{ type: 'minecraft:item', name: 'bakery:' + PRODUCE[c], conditions: [mature],
					functions: [{ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: few ? 1 : 2, max: few ? 2 : 4 } },
						{ function: 'minecraft:apply_bonus', enchantment: 'minecraft:fortune', formula: 'minecraft:binomial_with_bonus_count', parameters: { extra: 1, probability: 0.5 } }] },
				{ type: 'minecraft:item', name: 'bakery:' + c + '_seeds' }] }] },
			{ rolls: 1, conditions: [mature], entries: [{ type: 'minecraft:item', name: 'bakery:' + c + '_seeds',
				functions: [{ function: 'minecraft:apply_bonus', enchantment: 'minecraft:fortune', formula: 'minecraft:binomial_with_bonus_count', parameters: { extra: few ? 0 : 1, probability: 0.5714286 } }] }] }
		],
		functions: [{ function: 'minecraft:explosion_decay' }]
	});
});
FRUITS.forEach(f => lt(f, { pools: [{ rolls: 1, conditions: [{ condition: 'minecraft:block_state_property', block: 'bakery:' + f, properties: { age: '2' } }],
	entries: [{ type: 'minecraft:item', name: 'bakery:' + f, functions: [{ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: 1, max: 2 } }] }] }] }));
const silkOrShears = { condition: 'minecraft:any_of', terms: [
	{ condition: 'minecraft:match_tool', predicate: { items: 'minecraft:shears' } },
	{ condition: 'minecraft:match_tool', predicate: { predicates: { 'minecraft:enchantments': [{ enchantments: 'minecraft:silk_touch', levels: { min: 1 } }] } } }] };
ALL_WOODS.forEach(w => {
	[w + '_log', w + '_wood', 'stripped_' + w + '_log', 'stripped_' + w + '_wood', w + '_planks', w + '_stairs', w + '_fence', w + '_fence_gate', w + '_trapdoor', w + '_sapling'].forEach(self);
	lt(w + '_slab', { pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:' + w + '_slab', functions: [
		{ function: 'minecraft:set_count', count: 2, conditions: [{ condition: 'minecraft:block_state_property', block: 'bakery:' + w + '_slab', properties: { type: 'double' } }] },
		{ function: 'minecraft:explosion_decay' }] }] }] });
	lt(w + '_door', { pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:' + w + '_door',
		conditions: [{ condition: 'minecraft:block_state_property', block: 'bakery:' + w + '_door', properties: { half: 'lower' } }] }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
	lt(w + '_leaves', { pools: [
		{ rolls: 1, entries: [{ type: 'minecraft:alternatives', children: [
			{ type: 'minecraft:item', name: 'bakery:' + w + '_leaves', conditions: [silkOrShears] },
			{ type: 'minecraft:item', name: 'bakery:' + w + '_sapling', conditions: [{ condition: 'minecraft:survives_explosion' },
				{ condition: 'minecraft:table_bonus', enchantment: 'minecraft:fortune', chances: [0.05, 0.0625, 0.083333336, 0.1] }] }] }] },
		{ rolls: 1, conditions: [{ condition: 'minecraft:inverted', term: silkOrShears }], entries: [{ type: 'minecraft:item', name: 'minecraft:stick',
			conditions: [{ condition: 'minecraft:table_bonus', enchantment: 'minecraft:fortune', chances: [0.02, 0.022222223, 0.025, 0.033333335, 0.1] }],
			functions: [{ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: 1, max: 2 } }, { function: 'minecraft:explosion_decay' }] }] }] });
});

// ================= Tags =================
const tag = (kind, ns, name, values) => write(D + ns + '/tags/' + kind + '/' + name + '.json', { replace: false, values });
const b = n => 'bakery:' + n;
const woods = ALL_WOODS;
woods.forEach(w => {
	const logs = [w + '_log', w + '_wood', 'stripped_' + w + '_log', 'stripped_' + w + '_wood'].map(b);
	tag('block', 'bakery', w + '_logs', logs);
	tag('item', 'bakery', w + '_logs', logs);
});
const both = (name, values) => { tag('block', 'minecraft', name, values); tag('item', 'minecraft', name, values); };
both('logs_that_burn', woods.map(w => '#bakery:' + w + '_logs'));
both('planks', woods.map(w => b(w + '_planks')));
both('wooden_stairs', woods.map(w => b(w + '_stairs')));
both('wooden_slabs', woods.map(w => b(w + '_slab')));
both('wooden_fences', woods.map(w => b(w + '_fence')));
both('fence_gates', woods.map(w => b(w + '_fence_gate')));
both('wooden_doors', woods.map(w => b(w + '_door')));
both('wooden_trapdoors', woods.map(w => b(w + '_trapdoor')));
both('leaves', woods.map(w => b(w + '_leaves')));
both('saplings', woods.map(w => b(w + '_sapling')));
tag('block', 'bakery', 'oven_bricks', ['minecraft:bricks']);
tag('block', 'minecraft', 'mineable/pickaxe', ['oven_heart', 'multiblock_part', 'grand_oven', 'millstone_core', 'fountain_core', 'grand_millstone',
	'chocolate_fountain', 'juicer', 'jam_pot', 'mixing_bowl', 'pink_tiles', 'mint_tiles', 'lavender_tiles', 'cream_tiles', 'cake_stand', 'service_bell'].map(b));
tag('block', 'minecraft', 'mineable/axe', ['pastry_board', 'bakery_counter', 'display_case', 'cafe_table', 'cafe_chair', 'bread_basket', 'menu_board', 'jar_shelf',
	'strawberry_crate', 'lemon_crate', 'coconut_crate'].map(b).concat(woods.flatMap(w => ['#bakery:' + w + '_logs', b(w + '_planks'), b(w + '_stairs'), b(w + '_slab'),
	b(w + '_fence'), b(w + '_fence_gate'), b(w + '_door'), b(w + '_trapdoor')])));
tag('block', 'minecraft', 'mineable/hoe', woods.map(w => b(w + '_leaves')));
tag('block', 'minecraft', 'crops', CROPS.map(c => b(c + '_crop')));
tag('block', 'minecraft', 'bee_growables', CROPS.map(c => b(c + '_crop')));
tag('item', 'minecraft', 'villager_plantable_seeds', ['strawberry_seeds', 'blueberry_seeds', 'raspberry_seeds'].map(b));
tag('point_of_interest_type', 'minecraft', 'acquirable_job_site', ['bakery:baker']);

// ================= Worldgen =================
write(D + 'bakery/worldgen/configured_feature/palm_tree.json', { type: 'bakery:palm_tree', config: {} });
write(D + 'bakery/worldgen/configured_feature/lemon_tree.json', { type: 'bakery:lemon_tree', config: {} });
const placed = (name, chance) => write(D + 'bakery/worldgen/placed_feature/' + name + '.json', { feature: 'bakery:' + name, placement: [
	{ type: 'minecraft:rarity_filter', chance }, { type: 'minecraft:in_square' },
	{ type: 'minecraft:heightmap', heightmap: 'WORLD_SURFACE_WG' }, { type: 'minecraft:biome' }] });
placed('palm_tree', 3);
placed('lemon_tree', 18);

// ================= Recipes =================
const R = D + 'bakery/recipe/';
const ing = s => s.startsWith('#') ? { tag: s.slice(1) } : { item: s.includes(':') ? s : 'bakery:' + s };
const res = (id, n) => ({ id: id.includes(':') ? id : 'bakery:' + id, count: n || 1 });
const shapeless = (name, list, out, n) => write(R + name + '.json', { type: 'minecraft:crafting_shapeless', category: 'misc', ingredients: list.map(ing), result: res(out, n) });
const shaped = (name, pattern, key, out, n) => write(R + name + '.json', { type: 'minecraft:crafting_shaped', category: 'misc', pattern, key: Object.fromEntries(Object.entries(key).map(([k, v]) => [k, ing(v)])), result: res(out, n) });
const cook = (name, input, out) => {
	write(R + name + '_smelting.json', { type: 'minecraft:smelting', category: 'food', ingredient: ing(input), result: res(out), experience: 0.2, cookingtime: 200 });
	write(R + name + '_smoking.json', { type: 'minecraft:smoking', category: 'food', ingredient: ing(input), result: res(out), experience: 0.2, cookingtime: 100 });
	write(R + name + '_campfire.json', { type: 'minecraft:campfire_cooking', category: 'food', ingredient: ing(input), result: res(out), experience: 0.2, cookingtime: 400 });
};
// tools & stations
shapeless('bakery_guide', ['minecraft:book', 'minecraft:wheat'], 'bakery_guide');
shaped('sweetheart_spatula', ['G', 'P', 'S'], { G: 'minecraft:gold_ingot', P: 'minecraft:pink_dye', S: 'minecraft:stick' }, 'sweetheart_spatula');
shaped('oven_heart', ['BPB', 'BFB', 'BBB'], { B: 'minecraft:brick', P: 'minecraft:pink_dye', F: 'minecraft:furnace' }, 'oven_heart');
shaped('rolling_pin', ['S  ', ' P ', '  S'], { S: 'minecraft:stick', P: '#minecraft:planks' }, 'rolling_pin');
shaped('whisk', ['N N', ' N ', ' S '], { N: 'minecraft:iron_nugget', S: 'minecraft:stick' }, 'whisk');
shaped('mixing_bowl', ['T T', ' T '], { T: 'minecraft:white_terracotta' }, 'mixing_bowl');
shaped('pastry_board', ['F', 'P'], { F: 'flour', P: '#minecraft:wooden_slabs' }, 'pastry_board');
// pantry
shapeless('flour', ['minecraft:wheat', 'minecraft:wheat', 'minecraft:wheat'], 'flour', 2);
shapeless('butter', ['minecraft:milk_bucket'], 'butter', 2);
shapeless('whipped_cream', ['minecraft:milk_bucket', 'minecraft:sugar'], 'whipped_cream', 3);
shapeless('chocolate_bar', ['minecraft:cocoa_beans', 'minecraft:cocoa_beans', 'minecraft:sugar', 'minecraft:milk_bucket'], 'chocolate_bar', 3);
cook('matcha_powder', 'tea_leaves', 'matcha_powder');
shapeless('coconut_flakes', ['coconut'], 'coconut_flakes', 3);
shapeless('sprinkles', ['minecraft:sugar', 'minecraft:pink_dye', 'minecraft:light_blue_dye', 'minecraft:yellow_dye'], 'sprinkles', 6);
shapeless('candied_petals', ['rose_petals', 'lavender', 'minecraft:sugar'], 'candied_petals', 2);
shaped('glass_jar', ['W', 'G', 'G'], { W: '#minecraft:wooden_slabs', G: 'minecraft:glass' }, 'glass_jar', 2);
['strawberry', 'blueberry', 'raspberry'].forEach(f => shapeless(f + '_jam', ['glass_jar', f, f, f, 'minecraft:sugar'], f + '_jam'));
shapeless('wax_paper', ['minecraft:paper', 'minecraft:paper', 'minecraft:paper', 'minecraft:honeycomb'], 'wax_paper', 3);
shapeless('frost_wrap', ['minecraft:paper', 'minecraft:snowball', 'minecraft:snowball'], 'frost_wrap', 2);
shaped('mug', ['T T', 'TTT'], { T: 'minecraft:terracotta' }, 'mug', 4);
// seeds & saplings
shapeless('strawberry_seeds', ['strawberry'], 'strawberry_seeds', 2);
shapeless('blueberry_seeds', ['blueberry'], 'blueberry_seeds', 2);
shapeless('raspberry_seeds', ['raspberry'], 'raspberry_seeds', 2);
shapeless('tea_seeds', ['tea_leaves', 'tea_leaves'], 'tea_seeds');
shapeless('mint_seeds', ['mint_leaves', 'mint_leaves'], 'mint_seeds');
shapeless('vanilla_seeds', ['vanilla_pod'], 'vanilla_seeds', 2);
shapeless('lavender_seeds', ['lavender'], 'lavender_seeds', 2);
shapeless('rose_seeds', ['rose_petals', 'rose_petals'], 'rose_seeds');
shapeless('rose_seeds_from_bush', ['minecraft:rose_bush'], 'rose_seeds', 3);
shapeless('pineapple_seeds', ['pineapple'], 'pineapple_seeds', 2);
shapeless('starbloom_seeds', ['star_petals', 'star_petals'], 'starbloom_seeds');
shapeless('palm_sapling', ['coconut', 'minecraft:sand'], 'palm_sapling');
shapeless('citrus_sapling', ['lemon', 'lemon', 'minecraft:bone_meal'], 'citrus_sapling');
// drinks
shapeless('matcha_latte', ['mug', 'matcha_powder', 'minecraft:milk_bucket', 'minecraft:sugar'], 'matcha_latte');
shapeless('hot_chocolate', ['mug', 'chocolate_bar', 'minecraft:milk_bucket'], 'hot_chocolate');
shapeless('strawberry_milk', ['mug', 'strawberry', 'strawberry', 'minecraft:milk_bucket'], 'strawberry_milk');
shapeless('lavender_tea', ['mug', 'lavender', 'lavender', 'minecraft:water_bucket'], 'lavender_tea');
shapeless('rose_tea', ['mug', 'rose_petals', 'rose_petals', 'minecraft:water_bucket', 'minecraft:sugar'], 'rose_tea');
shapeless('starlight_tea', ['mug', 'star_petals', 'minecraft:water_bucket', 'minecraft:honey_bottle'], 'starlight_tea');
shapeless('lemonade', ['mug', 'lemon', 'lemon', 'minecraft:sugar', 'minecraft:water_bucket'], 'lemonade');
shapeless('coconut_milk', ['mug', 'coconut'], 'coconut_milk');
shapeless('mint_tea', ['mug', 'mint_leaves', 'mint_leaves', 'minecraft:water_bucket'], 'mint_tea');
shapeless('pineapple_smoothie', ['mug', 'pineapple', 'coconut_flakes', 'minecraft:milk_bucket'], 'pineapple_smoothie');
// shop & decor
shaped('bakery_counter', ['QQQ', 'WPW', 'WWW'], { Q: 'minecraft:quartz_slab', W: '#minecraft:planks', P: 'minecraft:pink_dye' }, 'bakery_counter');
shaped('display_case', ['GGG', 'G G', 'WWW'], { G: 'minecraft:glass_pane', W: '#minecraft:planks' }, 'display_case');
shaped('service_bell', [' N ', 'GGG'], { N: 'minecraft:iron_nugget', G: 'minecraft:gold_nugget' }, 'service_bell');
shaped('cafe_table', ['CCC', ' S ', ' S '], { C: 'minecraft:pink_carpet', S: 'minecraft:stick' }, 'cafe_table');
shaped('cafe_chair', ['W  ', 'WCW', 'S S'], { W: '#minecraft:planks', C: 'minecraft:pink_wool', S: 'minecraft:stick' }, 'cafe_chair', 2);
shapeless('flour_sack', ['flour', 'flour', 'flour', 'flour', 'minecraft:string'], 'flour_sack');
shaped('bread_basket', ['W W', 'WWW'], { W: 'minecraft:wheat' }, 'bread_basket');
shaped('cake_stand', ['TTT', ' T ', ' T '], { T: 'minecraft:white_terracotta' }, 'cake_stand');
shaped('menu_board', ['SSS', 'SCS', 'SSS'], { S: 'minecraft:stick', C: 'minecraft:coal' }, 'menu_board');
shapeless('bunting', ['minecraft:string', 'minecraft:pink_wool', 'minecraft:white_wool', 'minecraft:lime_wool'], 'bunting', 4);
shaped('jar_shelf', ['JJJ', 'SSS'], { J: 'glass_jar', S: '#minecraft:wooden_slabs' }, 'jar_shelf');
[['strawberry', 'strawberry'], ['lemon', 'lemon'], ['coconut', 'coconut']].forEach(([n, f]) => {
	shaped(n + '_crate', ['FFF', 'FFF', 'FFF'], { F: f }, n + '_crate');
	shapeless(f + '_from_crate', [n + '_crate'], f, 9);
});
[['pink', 'pink_dye'], ['mint', 'lime_dye'], ['lavender', 'purple_dye'], ['cream', 'yellow_dye']].forEach(([n, dye]) =>
	shaped(n + '_tiles', ['WD', 'DW'], { W: 'minecraft:white_terracotta', D: 'minecraft:' + dye }, n + '_tiles', 4));
woods.forEach(w => {
	shapeless(w + '_planks', ['#bakery:' + w + '_logs'], w + '_planks', 4);
	shaped(w + '_wood', ['LL', 'LL'], { L: w + '_log' }, w + '_wood', 3);
	shaped('stripped_' + w + '_wood', ['LL', 'LL'], { L: 'stripped_' + w + '_log' }, 'stripped_' + w + '_wood', 3);
	shaped(w + '_stairs', ['P  ', 'PP ', 'PPP'], { P: w + '_planks' }, w + '_stairs', 4);
	shaped(w + '_slab', ['PPP'], { P: w + '_planks' }, w + '_slab', 6);
	shaped(w + '_fence', ['PSP', 'PSP'], { P: w + '_planks', S: 'minecraft:stick' }, w + '_fence', 3);
	shaped(w + '_fence_gate', ['SPS', 'SPS'], { P: w + '_planks', S: 'minecraft:stick' }, w + '_fence_gate');
	shaped(w + '_door', ['PP', 'PP', 'PP'], { P: w + '_planks' }, w + '_door', 3);
	shaped(w + '_trapdoor', ['PPP', 'PPP'], { P: w + '_planks' }, w + '_trapdoor', 2);
});

return count + ' json files';
})()
