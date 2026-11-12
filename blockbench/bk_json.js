(function () {
const BK = window.BK;
const fs = BK.fs, path = BK.path;
const RES = 'C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/src/main/resources/';
const A = RES + 'assets/bakery/', D = RES + 'data/';
let count = 0;
function write(file, obj) {
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, JSON.stringify(obj, null, '\t'));
	count++;
}
const FACING = { north: 0, east: 90, south: 180, west: 270 };
const m = n => 'bakery:block/' + n;

// ================= Blockstates =================
const bs = (name, obj) => write(A + 'blockstates/' + name + '.json', obj);
const variants = {};
Object.entries(FACING).forEach(([f, y]) => {
	variants['facing=' + f + ',lit=false'] = { model: m('oven'), y };
	variants['facing=' + f + ',lit=true'] = { model: m('oven_lit'), y };
});
bs('oven', { variants });
bs('mixing_bowl', { variants: { 'filled=false': { model: m('mixing_bowl') }, 'filled=true': { model: m('mixing_bowl_filled') } } });
const board = {};
Object.entries(FACING).forEach(([f, y]) => {
	board['facing=' + f + ',has_dough=false'] = { model: m('pastry_board'), y };
	board['facing=' + f + ',has_dough=true'] = { model: m('pastry_board_dough'), y };
});
bs('pastry_board', { variants: board });
['bread_loaf', 'baguette', 'croissant'].forEach(n => {
	const v = {};
	Object.entries(FACING).forEach(([f, y]) => v['facing=' + f] = { model: m(n), y });
	bs(n, { variants: v });
});
function multipart(fn) {
	const parts = [];
	Object.entries(FACING).forEach(([f, y]) => fn(f, y, parts));
	return { multipart: parts };
}
bs('cupcake', multipart((f, y, p) => [1, 2, 3, 4].forEach(c => p.push({ when: { facing: f, count: '' + c }, apply: { model: m('cupcake_' + c), y } }))));
bs('donut', multipart((f, y, p) => [1, 2, 3, 4].forEach(c => p.push({ when: { facing: f, count: '' + c }, apply: { model: m('donut_' + Math.min(c, 3)), y } }))));
bs('cookies', multipart((f, y, p) => p.push({ when: { facing: f }, apply: { model: m('cookies'), y } })));
bs('macarons', multipart((f, y, p) => p.push({ when: { facing: f }, apply: { model: m('macarons'), y } })));
bs('pie', multipart((f, y, p) => [0, 1, 2, 3].forEach(b => p.push({ when: { facing: f, bites: '' + b }, apply: { model: m('pie_slice' + b), y } }))));
const TOPPINGS = ['strawberries', 'blueberries', 'cream', 'sprinkles', 'chocolate', 'rose_petals', 'lavender', 'star_petals'];
bs('cake', multipart((f, y, p) => [0, 1, 2, 3].forEach(b => {
	p.push({ when: { facing: f, bites: '' + b }, apply: { model: m('cake_slice' + b), y } });
	TOPPINGS.forEach(t => {
		if (fs.existsSync(A + 'models/block/cake_topping_' + t + '_slice' + b + '.json')) {
			p.push({ when: { facing: f, bites: '' + b, topping: t }, apply: { model: m('cake_topping_' + t + '_slice' + b), y } });
		}
	});
})));
const DRINKS = ['matcha_latte', 'hot_chocolate', 'strawberry_milk', 'lavender_tea', 'rose_tea', 'starlight_tea'];
bs('mug', multipart((f, y, p) => {
	p.push({ when: { facing: f }, apply: { model: m('mug'), y } });
}));
JSON.parse(fs.readFileSync(A + 'blockstates/mug.json', 'utf8'));
const mug = JSON.parse(fs.readFileSync(A + 'blockstates/mug.json', 'utf8'));
mug.multipart.push({ when: { drink: DRINKS.join('|') }, apply: { model: m('mug_liquid') } });
bs('mug', mug);

const CROPS = ['strawberry', 'blueberry', 'tea', 'vanilla', 'lavender', 'rose', 'starbloom'];
CROPS.forEach(c => {
	const v = {};
	for (let a = 0; a < 4; a++) {
		v['age=' + a] = { model: m(c + '_crop_stage' + a) };
		write(A + 'models/block/' + c + '_crop_stage' + a + '.json', { parent: 'minecraft:block/crop', textures: { crop: 'bakery:block/' + c + '_crop_stage' + a } });
	}
	bs(c + '_crop', { variants: v });
});

// ================= Item models =================
const im = (name, obj) => write(A + 'models/item/' + name + '.json', obj);
const gen = (name, layers) => im(name, { parent: 'minecraft:item/generated', textures: Object.fromEntries((layers || [name]).map((l, i) => ['layer' + i, 'bakery:item/' + l])) });
['strawberry_seeds', 'blueberry_seeds', 'tea_seeds', 'vanilla_seeds', 'lavender_seeds', 'rose_seeds', 'starbloom_seeds',
	'strawberry', 'blueberry', 'tea_leaves', 'vanilla_pod', 'lavender', 'rose_petals', 'star_petals',
	'flour', 'butter', 'whipped_cream', 'chocolate_bar', 'matcha_powder', 'sprinkles', 'candied_petals', 'glass_jar',
	'strawberry_jam', 'blueberry_jam', 'wax_paper', 'frost_wrap', 'mug',
	'raw_loaf', 'raw_baguette', 'raw_croissant', 'raw_cupcake', 'raw_donut', 'raw_cookies', 'raw_macarons', 'raw_cake', 'raw_pie',
	'bread_loaf', 'baguette', 'croissant', 'burnt_pastry'].concat(DRINKS).forEach(n => gen(n));
['bread_dough', 'sweet_dough', 'pastry_dough', 'cookie_dough'].forEach(n => gen(n, [n, 'dough_flavor']));
gen('cupcake', ['cupcake', 'cupcake_frosting']);
gen('donut', ['donut', 'donut_glaze']);
gen('cookies', ['cookies', 'cookies_chips']);
gen('macarons', ['macarons', 'macarons_shells']);
gen('cake', ['cake', 'cake_frosting']);
gen('pie', ['pie', 'pie_filling']);
['rolling_pin', 'whisk'].forEach(n => im(n, { parent: 'minecraft:item/handheld', textures: { layer0: 'bakery:item/' + n } }));
im('oven', { parent: m('oven') });
im('mixing_bowl', { parent: m('mixing_bowl_filled') });
im('pastry_board', { parent: m('pastry_board_dough') });

// ================= Loot tables =================
const lt = (name, obj) => write(D + 'bakery/loot_table/blocks/' + name + '.json', Object.assign({ type: 'minecraft:block' }, obj));
['oven', 'mixing_bowl', 'pastry_board'].forEach(n => lt(n, { pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:' + n }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] }));
const PRODUCE = { strawberry: 'strawberry', blueberry: 'blueberry', tea: 'tea_leaves', vanilla: 'vanilla_pod', lavender: 'lavender', rose: 'rose_petals', starbloom: 'star_petals' };
CROPS.forEach(c => {
	const mature = { condition: 'minecraft:block_state_property', block: 'bakery:' + c + '_crop', properties: { age: '3' } };
	lt(c + '_crop', {
		pools: [
			{ rolls: 1, entries: [{ type: 'minecraft:alternatives', children: [
				{ type: 'minecraft:item', name: 'bakery:' + PRODUCE[c], conditions: [mature],
					functions: [{ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: c === 'starbloom' ? 1 : 2, max: c === 'starbloom' ? 2 : 4 } },
						{ function: 'minecraft:apply_bonus', enchantment: 'minecraft:fortune', formula: 'minecraft:binomial_with_bonus_count', parameters: { extra: 1, probability: 0.5 } }] },
				{ type: 'minecraft:item', name: 'bakery:' + c + '_seeds' }] }] },
			{ rolls: 1, conditions: [mature], entries: [{ type: 'minecraft:item', name: 'bakery:' + c + '_seeds',
				functions: [{ function: 'minecraft:apply_bonus', enchantment: 'minecraft:fortune', formula: 'minecraft:binomial_with_bonus_count', parameters: { extra: c === 'starbloom' ? 0 : 1, probability: 0.5714286 } }] }] }
		],
		functions: [{ function: 'minecraft:explosion_decay' }]
	});
});

// ================= Tags =================
write(D + 'minecraft/tags/block/mineable/pickaxe.json', { values: ['bakery:oven', 'bakery:mixing_bowl'] });
write(D + 'minecraft/tags/block/mineable/axe.json', { values: ['bakery:pastry_board'] });
write(D + 'minecraft/tags/block/crops.json', { values: CROPS.map(c => 'bakery:' + c + '_crop') });
write(D + 'minecraft/tags/block/bee_growables.json', { values: CROPS.map(c => 'bakery:' + c + '_crop') });
write(D + 'minecraft/tags/item/villager_plantable_seeds.json', { values: ['bakery:strawberry_seeds', 'bakery:blueberry_seeds'] });
write(D + 'minecraft/tags/point_of_interest_type/acquirable_job_site.json', { values: ['bakery:baker'] });

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
shapeless('flour', ['minecraft:wheat', 'minecraft:wheat', 'minecraft:wheat'], 'flour', 2);
shapeless('butter', ['minecraft:milk_bucket'], 'butter', 2);
shapeless('whipped_cream', ['minecraft:milk_bucket', 'minecraft:sugar'], 'whipped_cream', 3);
shapeless('chocolate_bar', ['minecraft:cocoa_beans', 'minecraft:cocoa_beans', 'minecraft:sugar', 'minecraft:milk_bucket'], 'chocolate_bar', 3);
cook('matcha_powder', 'tea_leaves', 'matcha_powder');
shapeless('sprinkles', ['minecraft:sugar', 'minecraft:pink_dye', 'minecraft:light_blue_dye', 'minecraft:yellow_dye'], 'sprinkles', 6);
shapeless('candied_petals', ['rose_petals', 'lavender', 'minecraft:sugar'], 'candied_petals', 2);
shaped('glass_jar', ['W', 'G', 'G'], { W: '#minecraft:wooden_slabs', G: 'minecraft:glass' }, 'glass_jar', 2);
shapeless('strawberry_jam', ['glass_jar', 'strawberry', 'strawberry', 'strawberry', 'minecraft:sugar'], 'strawberry_jam');
shapeless('blueberry_jam', ['glass_jar', 'blueberry', 'blueberry', 'blueberry', 'minecraft:sugar'], 'blueberry_jam');
shapeless('wax_paper', ['minecraft:paper', 'minecraft:paper', 'minecraft:paper', 'minecraft:honeycomb'], 'wax_paper', 3);
shapeless('frost_wrap', ['minecraft:paper', 'minecraft:snowball', 'minecraft:snowball'], 'frost_wrap', 2);
shaped('mug', ['T T', 'TTT'], { T: 'minecraft:terracotta' }, 'mug', 4);
shaped('rolling_pin', ['S  ', ' P ', '  S'], { S: 'minecraft:stick', P: '#minecraft:planks' }, 'rolling_pin');
shaped('whisk', ['N N', ' N ', ' S '], { N: 'minecraft:iron_nugget', S: 'minecraft:stick' }, 'whisk');
shaped('mixing_bowl', ['T T', ' T '], { T: 'minecraft:white_terracotta' }, 'mixing_bowl');
shaped('pastry_board', ['F', 'P'], { F: 'flour', P: '#minecraft:wooden_slabs' }, 'pastry_board');
shaped('oven', ['BBB', 'BFB', 'BBB'], { B: 'minecraft:bricks', F: 'minecraft:furnace' }, 'oven');
shapeless('strawberry_seeds', ['strawberry'], 'strawberry_seeds', 2);
shapeless('blueberry_seeds', ['blueberry'], 'blueberry_seeds', 2);
shapeless('tea_seeds', ['tea_leaves', 'tea_leaves'], 'tea_seeds');
shapeless('vanilla_seeds', ['vanilla_pod'], 'vanilla_seeds', 2);
shapeless('lavender_seeds', ['lavender'], 'lavender_seeds', 2);
shapeless('rose_seeds', ['rose_petals', 'rose_petals'], 'rose_seeds');
shapeless('rose_seeds_from_bush', ['minecraft:rose_bush'], 'rose_seeds', 3);
shapeless('starbloom_seeds', ['star_petals', 'star_petals'], 'starbloom_seeds');
shapeless('matcha_latte', ['mug', 'matcha_powder', 'minecraft:milk_bucket', 'minecraft:sugar'], 'matcha_latte');
shapeless('hot_chocolate', ['mug', 'chocolate_bar', 'minecraft:milk_bucket'], 'hot_chocolate');
shapeless('strawberry_milk', ['mug', 'strawberry', 'strawberry', 'minecraft:milk_bucket'], 'strawberry_milk');
shapeless('lavender_tea', ['mug', 'lavender', 'lavender', 'minecraft:water_bucket'], 'lavender_tea');
shapeless('rose_tea', ['mug', 'rose_petals', 'rose_petals', 'minecraft:water_bucket', 'minecraft:sugar'], 'rose_tea');
shapeless('starlight_tea', ['mug', 'star_petals', 'minecraft:water_bucket', 'minecraft:honey_bottle'], 'starlight_tea');

return count + ' json files';
})()
