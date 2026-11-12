(function () {
// JSON for the third wave: kitchen machines, cores and big machines, corn/peas/zucchini,
// new items, recipes, loot tables, tags and worldgen for the five new fruit trees.
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
const lt = (name, obj) => write(D + 'bakery/loot_table/blocks/' + name + '.json', Object.assign({ type: 'minecraft:block' }, obj));
const self = (name, item) => lt(name, { pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: item || 'bakery:' + name }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });

// ================= Kitchen machines (facing x level) =================
['juicer', 'ice_cream_churn', 'jam_pot', 'proofing_basket'].forEach(n => {
	const v = {};
	Object.entries(FACING).forEach(([f, y]) => { for (let l = 0; l < 4; l++) v['facing=' + f + ',level=' + l] = { model: m(n + '_' + l), y }; });
	bs(n, { variants: v });
	im(n, { parent: m(n + '_' + (n === 'proofing_basket' ? 3 : n === 'juicer' ? 2 : 1)) });
	self(n);
});

// ================= Cores and big machines =================
['millstone_core', 'fountain_core'].forEach(n => {
	const v = {};
	Object.entries(FACING).forEach(([f, y]) => v['facing=' + f] = { model: m(n), y });
	bs(n, { variants: v });
	im(n, { parent: m(n) });
	self(n);
});
[['grand_millstone', 'grand_millstone', 'grand_millstone', 'millstone_core'], ['chocolate_fountain', 'chocolate_fountain', 'chocolate_fountain_active', 'fountain_core']]
	.forEach(([n, idle, active, core]) => {
		const v = {};
		Object.entries(FACING).forEach(([f, y]) => {
			v['facing=' + f + ',active=false'] = { model: m(idle), y };
			v['facing=' + f + ',active=true'] = { model: m(active), y };
		});
		bs(n, { variants: v });
		self(n, 'bakery:' + core);
	});

// ================= Corn and peas (segment x age) =================
const corn = {}, peas = {};
for (let age = 0; age < 4; age++) for (let seg = 0; seg < 3; seg++) {
	let c = age === 0 ? 'corn_stage0' : age === 1 ? 'corn_stage1' : 'corn_bottom';
	if (seg === 1) c = age === 3 ? 'corn_middle_ripe' : 'corn_middle';
	if (seg === 2) c = 'corn_top';
	corn['age=' + age + ',segment=' + seg] = { model: m(c) };
	let p = age === 0 ? 'pea_stage0' : age === 1 ? 'pea_stage1' : age === 3 ? 'pea_bottom_ripe' : 'pea_bottom';
	if (seg >= 1) p = age === 3 ? 'pea_top_ripe' : 'pea_top';
	peas['age=' + age + ',segment=' + seg] = { model: m(p) };
}
bs('corn_crop', { variants: corn });
bs('pea_crop', { variants: peas });
const tallLoot = (name, produce, seeds) => lt(name, { pools: [
	{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:' + seeds }],
		conditions: [{ condition: 'minecraft:block_state_property', block: 'bakery:' + name, properties: { segment: '0' } }] },
	{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:' + produce, functions: [{ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: 1, max: 3 } }] }],
		conditions: [{ condition: 'minecraft:block_state_property', block: 'bakery:' + name, properties: { segment: '0', age: '3' } }] }] });
tallLoot('corn_crop', 'corn', 'corn_seeds');
tallLoot('pea_crop', 'pea_pod', 'pea_seeds');
gen('corn_seeds');
gen('pea_seeds');
gen('zucchini_seeds');

// ================= Zucchini (vanilla-style stem) =================
const sv = {};
for (let a = 0; a < 8; a++) {
	model('zucchini_stem_stage' + a, { parent: 'minecraft:block/stem_growth' + a, textures: { stem: 'bakery:block/zucchini_stem' } });
	sv['age=' + a] = { model: m('zucchini_stem_stage' + a) };
}
bs('zucchini_stem', { variants: sv });
model('attached_zucchini_stem', { parent: 'minecraft:block/stem_fruit', textures: { stem: 'bakery:block/zucchini_stem', upperstem: 'bakery:block/zucchini_stem_attached' } });
bs('attached_zucchini_stem', { variants: {
	'facing=west': { model: m('attached_zucchini_stem') }, 'facing=east': { model: m('attached_zucchini_stem'), y: 180 },
	'facing=north': { model: m('attached_zucchini_stem'), y: 90 }, 'facing=south': { model: m('attached_zucchini_stem'), y: 270 } } });
const zb = {};
Object.entries(FACING).forEach(([f, y]) => zb['facing=' + f] = { model: m('zucchini_block'), y });
bs('zucchini_block', { variants: zb });
lt('zucchini_block', { pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:zucchini', functions: [
	{ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: 2, max: 4 } },
	{ function: 'minecraft:apply_bonus', enchantment: 'minecraft:fortune', formula: 'minecraft:uniform_bonus_count', parameters: { bonusMultiplier: 1 } }] }] }],
	functions: [{ function: 'minecraft:explosion_decay' }] });
lt('zucchini_stem', { pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:zucchini_seeds' }] }] });
lt('attached_zucchini_stem', { pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:zucchini_seeds' }] }] });

// ================= Items =================
['cherries', 'orange', 'peach', 'mango', 'plum', 'corn', 'pea_pod', 'sweet_potato', 'baked_sweet_potato', 'zucchini', 'rhubarb', 'sugar_beet',
	'cranberry', 'peanut', 'hibiscus', 'candyfloss', 'cocoa_powder', 'cornmeal', 'peanut_butter'].forEach(n => gen(n));
gen('juice', ['item/juice', 'item/juice_liquid']);
gen('ice_cream', ['item/ice_cream', 'item/ice_cream_scoop']);
gen('fruit_jam', ['item/fruit_jam', 'item/fruit_jam_fill']);
gen('chocolate_treat', ['item/chocolate_treat', 'item/chocolate_treat_fruit']);

// ================= Tags =================
// Adds to a tag file other generators may already have written.
const tag = (kind, ns, name, values) => {
	const file = D + ns + '/tags/' + kind + '/' + name + '.json';
	const existing = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')).values : [];
	write(file, { replace: false, values: [...new Set(existing.concat(values))] });
};
const crops = ['corn_crop', 'pea_crop', 'zucchini_stem', 'attached_zucchini_stem'].map(n => 'bakery:' + n);
tag('block', 'bakery', 'tall_crops', crops);
tag('block', 'minecraft', 'maintains_farmland', crops);
tag('block', 'minecraft', 'mineable/axe', ['ice_cream_churn', 'proofing_basket', 'zucchini_block'].map(n => 'bakery:' + n));

// ================= Worldgen for the new trees =================
const placed = (name, chance) => {
	write(D + 'bakery/worldgen/configured_feature/' + name + '.json', { type: 'bakery:' + name, config: {} });
	write(D + 'bakery/worldgen/placed_feature/' + name + '.json', { feature: 'bakery:' + name, placement: [
		{ type: 'minecraft:rarity_filter', chance }, { type: 'minecraft:in_square' },
		{ type: 'minecraft:heightmap', heightmap: 'WORLD_SURFACE_WG' }, { type: 'minecraft:biome' }] });
};
placed('cherry_tree', 10);
placed('orange_tree', 14);
placed('peach_tree', 24);
placed('mango_tree', 8);
placed('plum_tree', 16);

// ================= Recipes =================
const R = D + 'bakery/recipe/';
const ing = s => s.startsWith('#') ? { tag: s.slice(1) } : { item: s.includes(':') ? s : 'bakery:' + s };
const res = (id, n) => ({ id: id.includes(':') ? id : 'bakery:' + id, count: n || 1 });
const shapeless = (name, list, out, n) => write(R + name + '.json', { type: 'minecraft:crafting_shapeless', category: 'misc', ingredients: list.map(ing), result: res(out, n) });
const shaped = (name, pattern, key, out, n) => write(R + name + '.json', { type: 'minecraft:crafting_shaped', category: 'misc', pattern, key: Object.fromEntries(Object.entries(key).map(([k, v]) => [k, ing(v)])), result: res(out, n) });
const cook = (name, input, out) => {
	write(R + name + '_smelting.json', { type: 'minecraft:smelting', category: 'food', ingredient: ing(input), result: res(out), experience: 0.3, cookingtime: 200 });
	write(R + name + '_smoking.json', { type: 'minecraft:smoking', category: 'food', ingredient: ing(input), result: res(out), experience: 0.3, cookingtime: 100 });
	write(R + name + '_campfire.json', { type: 'minecraft:campfire_cooking', category: 'food', ingredient: ing(input), result: res(out), experience: 0.3, cookingtime: 400 });
};
shaped('juicer', [' I ', 'GGG', 'TTT'], { I: 'minecraft:iron_ingot', G: 'minecraft:glass_pane', T: 'minecraft:white_terracotta' }, 'juicer');
shaped('ice_cream_churn', ['S S', 'PIP', 'PPP'], { S: 'minecraft:stick', P: '#minecraft:planks', I: 'minecraft:iron_ingot' }, 'ice_cream_churn');
shaped('jam_pot', ['C C', 'C C', 'CCC'], { C: 'minecraft:copper_ingot' }, 'jam_pot');
shaped('proofing_basket', ['W W', 'WCW'], { W: 'minecraft:wheat', C: 'minecraft:white_carpet' }, 'proofing_basket');
shaped('millstone_core', ['SSS', 'SGS', 'SSS'], { S: 'minecraft:smooth_stone', G: 'minecraft:grindstone' }, 'millstone_core');
shaped('fountain_core', ['TCT', 'TGT', 'TTT'], { T: 'minecraft:white_terracotta', C: 'chocolate_bar', G: 'minecraft:gold_ingot' }, 'fountain_core');
cook('baked_sweet_potato', 'sweet_potato', 'baked_sweet_potato');
cook('roasted_corn', 'corn', 'cornmeal');
shapeless('sugar_from_beet', ['sugar_beet'], 'minecraft:sugar', 2);
shapeless('cocoa_powder', ['minecraft:cocoa_beans', 'minecraft:cocoa_beans'], 'cocoa_powder');
shapeless('peanut_butter', ['minecraft:glass_bottle', 'peanut', 'peanut', 'peanut'], 'peanut_butter');
[['corn', 'corn_seeds', 2], ['pea_pod', 'pea_seeds', 2], ['sweet_potato', 'sweet_potato_seeds', 1], ['zucchini', 'zucchini_seeds', 2], ['rhubarb', 'rhubarb_seeds', 1],
	['sugar_beet', 'sugar_beet_seeds', 2], ['cranberry', 'cranberry_seeds', 2], ['peanut', 'peanut_seeds', 2], ['hibiscus', 'hibiscus_seeds', 1], ['candyfloss', 'candyfloss_seeds', 1]]
	.forEach(([from, to, n]) => shapeless(to, [from], to, n));
[['cherries', 'blossom'], ['orange', 'orangewood'], ['peach', 'peachwood'], ['mango', 'mangowood'], ['plum', 'plumwood']]
	.forEach(([fruit, wood]) => shapeless(wood + '_sapling', [fruit, fruit, 'minecraft:bone_meal'], wood + '_sapling'));

return count + ' json files';
})()
