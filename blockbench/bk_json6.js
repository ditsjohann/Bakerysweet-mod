(function () {
// Sixth wave data, "Sweet Life": models, blockstates, loot, recipes, tags and English text.
// Runs anywhere bk_art-style fs is available (Blockbench or the bk_server page). Merges into existing tag/lang files.
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
const simpleState = name => state(name, { variants: { '': { model: b(name) } } });
const facingState = (name, modelName) => {
	const v = {};
	[['north', 0], ['east', 90], ['south', 180], ['west', 270]].forEach(([f, y]) => v['facing=' + f] = { model: b(modelName || name), y });
	state(name, { variants: v });
};
const loot = name => {
	put(D + 'bakery/loot_table/blocks/' + name + '.json', { type: 'minecraft:block', pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'bakery:' + name }],
		conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
	files++;
};
const recipe = (name, obj) => { put(D + 'bakery/recipe/' + name + '.json', obj); files++; };
const ing = id => id.startsWith('#') ? { tag: id.slice(1) } : { item: id.includes(':') ? id : 'minecraft:' + id };
const shaped = (name, pattern, key, count, result) => {
	const k = {};
	Object.entries(key).forEach(([c, id]) => k[c] = ing(id));
	recipe(name, { type: 'minecraft:crafting_shaped', category: 'misc', pattern, key: k, result: { id: result || 'bakery:' + name, count: count || 1 } });
};
const shapeless = (name, list, count, result) => recipe(name, { type: 'minecraft:crafting_shapeless', category: 'misc',
	ingredients: list.map(ing), result: { id: result || 'bakery:' + name, count: count || 1 } });
const mergeTag = (path, values) => {
	let tag = { replace: false, values: [] };
	if (fs.existsSync(path)) tag = JSON.parse(fs.readFileSync(path));
	values.forEach(v => { if (!tag.values.includes(v)) tag.values.push(v); });
	put(path, tag);
	files++;
};

// Element helper: faces get UVs from the element's size (like Blockbench auto-UV), clamped to the texture.
const cl = v => Math.max(0, Math.min(16, v));
function box(from, to, tex, opts) {
	opts = opts || {};
	const [x1, y1, z1] = from, [x2, y2, z2] = to;
	const w = x2 - x1, h = y2 - y1, d = z2 - z1;
	const uv = {
		north: [cl(x1), cl(16 - y2), cl(x2), cl(16 - y1)], south: [cl(x1), cl(16 - y2), cl(x2), cl(16 - y1)],
		east: [cl(z1), cl(16 - y2), cl(z2), cl(16 - y1)], west: [cl(z1), cl(16 - y2), cl(z2), cl(16 - y1)],
		up: [cl(x1), cl(z1), cl(x2), cl(z2)], down: [cl(x1), cl(z1), cl(x2), cl(z2)]
	};
	if (y2 > 16 || y1 < 0) ['north', 'south', 'east', 'west'].forEach(f => uv[f] = [0, 0, cl(f === 'east' || f === 'west' ? d : w), cl(h)]);
	const faces = {};
	['north', 'south', 'east', 'west', 'up', 'down'].forEach(f => {
		if (opts.skip && opts.skip.includes(f)) return;
		faces[f] = { uv: (opts.uv && opts.uv[f]) || uv[f], texture: '#' + ((opts.tex && opts.tex[f]) || tex) };
		if (opts.tint) faces[f].tintindex = 0;
	});
	const e = { from, to, faces };
	if (opts.rot) e.rotation = opts.rot;
	return e;
}
const R = (axis, angle, origin) => ({ axis, angle, origin });
const BLOCK_DISPLAY = { gui: { rotation: [30, 225, 0], scale: [0.625, 0.625, 0.625] }, ground: { translation: [0, 3, 0], scale: [0.25, 0.25, 0.25] },
	fixed: { scale: [0.5, 0.5, 0.5] }, thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
	firstperson_righthand: { rotation: [0, 45, 0], scale: [0.4, 0.4, 0.4] }, firstperson_lefthand: { rotation: [0, 225, 0], scale: [0.4, 0.4, 0.4] } };

// ---------------- Items ----------------
item('sweet_coin', { parent: 'minecraft:item/generated', textures: { layer0: it('sweet_coin') } });
['heart', 'star', 'bunny', 'flower'].forEach((s, i) => {
	item(s + '_cutter', { parent: 'minecraft:item/handheld', textures: { layer0: it(s + '_cutter') } });
	item('raw_sugar_cookie_' + s, { parent: 'minecraft:item/generated', textures: { layer0: it('raw_cookie_' + s) } });
	item('sugar_cookie_' + s, { parent: 'minecraft:item/generated', textures: { layer0: it('cookie_' + s) } });
	item('sugar_cookie_' + s + '_iced', { parent: 'minecraft:item/generated', textures: { layer0: it('cookie_' + s), layer1: it('cookie_' + s + '_icing') } });
});
// Overrides: later entries win, so shapes go in ascending order with the iced variant right after each.
const cookieOverrides = iced => {
	const list = [];
	['heart', 'star', 'bunny', 'flower'].forEach((s, i) => {
		if (i > 0) list.push({ predicate: { 'bakery:shape': i * 0.25 }, model: 'bakery:item/' + (iced ? 'sugar_cookie_' : 'raw_sugar_cookie_') + s });
		if (iced) list.push({ predicate: { 'bakery:shape': i * 0.25, 'bakery:iced': 1 }, model: 'bakery:item/sugar_cookie_' + s + '_iced' });
	});
	return list;
};
item('raw_sugar_cookie', { parent: 'minecraft:item/generated', textures: { layer0: it('raw_cookie_heart') }, overrides: cookieOverrides(false) });
item('sugar_cookie', { parent: 'minecraft:item/generated', textures: { layer0: it('cookie_heart') }, overrides: cookieOverrides(true) });
['pink', 'mint', 'lilac', 'lemon', 'sky', 'white'].forEach(c =>
	item(c + '_icing', { parent: 'minecraft:item/generated', textures: { layer0: it('icing_bag'), layer1: it('icing_bag_tint') } }));
item('gift_box', { parent: 'minecraft:item/generated', textures: { layer0: it('gift_box') } });
item('wrapped_gift', { parent: 'minecraft:item/generated', textures: { layer0: it('wrapped_gift'), layer1: it('wrapped_gift_ribbon') } });
item('frilly_apron', { parent: 'minecraft:item/generated', textures: { layer0: it('frilly_apron') } });

// Hats: little 3D models. The head spans about 1.6-14.4 in these coordinates, top at 14.4, front (face) at low z.
const HAT_DISPLAY = { head: { translation: [0, 0, 0] }, gui: { rotation: [25, 200, 0], translation: [0, -5, 0], scale: [0.55, 0.55, 0.55] },
	ground: { translation: [0, -2, 0], scale: [0.35, 0.35, 0.35] }, fixed: { translation: [0, -4, 0], scale: [0.6, 0.6, 0.6] },
	thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, -2, 2], scale: [0.35, 0.35, 0.35] },
	firstperson_righthand: { rotation: [0, 45, 0], translation: [0, -2, 0], scale: [0.35, 0.35, 0.35] },
	firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, -2, 0], scale: [0.35, 0.35, 0.35] } };
const hat = (name, textures, elements) => item(name, { parent: 'minecraft:block/block', textures, elements, display: HAT_DISPLAY });
hat('chef_hat', { cloth: b('hat_chef'), particle: b('hat_chef') }, [
	box([1.2, 13.4, 1.2], [14.8, 15.8, 14.8], 'cloth'),
	box([0.6, 15.8, 0.6], [15.4, 21.5, 15.4], 'cloth'),
	box([2, 21.5, 2], [14, 22.6, 14], 'cloth')]);
hat('strawberry_beret', { cap: b('hat_beret'), particle: b('hat_beret') }, [
	box([0.4, 14.2, 0.4], [15.6, 15.8, 15.6], 'cap', { rot: R('z', 22.5, [8, 15, 8]), uv: { up: [0, 0, 16, 12], down: [0, 0, 16, 12] } }),
	box([7.4, 15.4, 7.4], [8.6, 17.2, 8.6], 'cap', { rot: R('z', 22.5, [8, 15, 8]), uv: { north: [0, 12, 2, 16], south: [0, 12, 2, 16], east: [0, 12, 2, 16], west: [0, 12, 2, 16], up: [0, 12, 2, 14] } }),
	box([8.6, 15.6, 6.8], [11, 16.2, 9.2], 'cap', { rot: R('z', 22.5, [8, 15, 8]), uv: { up: [4, 12, 8, 16], north: [4, 12, 8, 13], south: [4, 12, 8, 13], east: [4, 12, 8, 13], west: [4, 12, 8, 13], down: [4, 12, 8, 16] } })]);
const earUv = { north: [0, 8, 3, 16], south: [0, 0, 3, 8], east: [0, 0, 1, 8], west: [0, 0, 1, 8], up: [0, 0, 3, 1], down: [0, 0, 3, 1] };
hat('bunny_ears', { fur: b('hat_bunny'), band: b('hat_band'), particle: b('hat_bunny') }, [
	box([1, 14.2, 6.8], [15, 15.2, 8.8], 'band'),
	box([3.6, 15, 7.2], [6.4, 24.5, 8.4], 'fur', { rot: R('z', 22.5, [5, 15, 7.8]), uv: earUv }),
	box([9.6, 15, 7.2], [12.4, 24.5, 8.4], 'fur', { rot: R('z', -22.5, [11, 15, 7.8]), uv: earUv })]);
const catUv = { north: [0, 8, 4, 12], south: [0, 0, 4, 4], east: [0, 0, 2, 4], west: [0, 0, 2, 4], up: [0, 0, 4, 2], down: [0, 0, 4, 2] };
hat('cat_ears', { fur: b('hat_cat'), band: b('hat_band'), particle: b('hat_cat') }, [
	box([1, 14.2, 6.8], [15, 15.2, 8.8], 'band'),
	box([2.4, 14.6, 7], [6.4, 17, 8.6], 'fur', { uv: catUv }), box([3.4, 17, 7], [5.4, 18.8, 8.6], 'fur', { uv: catUv }),
	box([9.6, 14.6, 7], [13.6, 17, 8.6], 'fur', { uv: catUv }), box([10.6, 17, 7], [12.6, 18.8, 8.6], 'fur', { uv: catUv })]);
hat('ribbon_bow', { satin: b('hat_bow'), particle: b('hat_bow') }, [
	box([6.8, 14.2, 9.2], [9.2, 16.6, 11.2], 'satin'),
	box([2.6, 14.4, 9.6], [6.8, 18.4, 10.8], 'satin', { rot: R('z', -22.5, [6.8, 15.4, 10]) }),
	box([9.2, 14.4, 9.6], [13.4, 18.4, 10.8], 'satin', { rot: R('z', 22.5, [9.2, 15.4, 10]) }),
	box([5.8, 12.4, 11], [7.2, 14.4, 11.4], 'satin'), box([8.8, 12.4, 11], [10.2, 14.4, 11.4], 'satin')]);

// ---------------- Blocks ----------------
['pink_checker_tiles', 'mint_checker_tiles', 'lilac_checker_tiles', 'butter_checker_tiles',
	'strawberry_wallpaper', 'gingham_wallpaper', 'heart_wallpaper', 'polka_dot_wallpaper'].forEach(n => {
	model(n, { parent: 'minecraft:block/cube_all', textures: { all: b(n) } });
	simpleState(n);
	item(n, { parent: b(n) });
});
['heart_rug', 'cloud_rug'].forEach(n => {
	model(n, { parent: 'minecraft:block/carpet', textures: { wool: b(n) } });
	simpleState(n);
	item(n, { parent: b(n) });
});
['pink', 'lilac', 'mint'].forEach(c => {
	const n = c + '_heart_balloon';
	model(n, { parent: 'minecraft:block/cross', textures: { cross: b(n) } });
	facingState(n);
	item(n, { parent: 'minecraft:item/generated', textures: { layer0: b(n) } });
});
model('fairy_lights', { parent: 'minecraft:block/block', ambientocclusion: false, textures: { lights: b('fairy_lights'), particle: b('fairy_lights') },
	elements: [box([0, 0, 15.5], [16, 16, 15.5], 'lights', { skip: ['east', 'west', 'up', 'down'], uv: { north: [0, 0, 16, 16], south: [16, 0, 0, 16] } })] });
facingState('fairy_lights');
item('fairy_lights', { parent: 'minecraft:item/generated', textures: { layer0: b('fairy_lights') } });

['pink', 'mint'].forEach(c => {
	const n = c + '_awning';
	model(n, { parent: 'minecraft:block/block', textures: { cloth: b(n), particle: b(n) }, elements: [
		box([0, 12.5, 4], [16, 13.5, 16], 'cloth', { rot: R('x', -22.5, [8, 13, 16]), uv: { up: [0, 0, 16, 12], down: [0, 0, 16, 12] } }),
		box([0, 8.5, 3.8], [16, 12.5, 4], 'cloth', { uv: { north: [0, 12, 16, 16], south: [0, 12, 16, 16] }, skip: ['up', 'down'] })] });
	facingState(n);
	item(n, { parent: b(n), display: BLOCK_DISPLAY });
});

model('cupcake_lamp', { parent: 'minecraft:block/block', textures: { wrap: b('cupcake_lamp_wrapper'), frost: b('cupcake_lamp_frosting'),
	cherry: b('cupcake_lamp_cherry'), particle: b('cupcake_lamp_frosting') }, elements: [
	box([5, 0, 5], [11, 5, 11], 'wrap'), box([4, 5, 4], [12, 9, 12], 'frost'), box([5, 9, 5], [11, 11, 11], 'frost'),
	box([6.5, 11, 6.5], [9.5, 12, 9.5], 'frost'), box([7, 12, 7], [9, 14, 9], 'cherry')] });
facingState('cupcake_lamp');
item('cupcake_lamp', { parent: b('cupcake_lamp') });

model('cookie_jar', { parent: 'minecraft:block/block', textures: { glass: b('cookie_jar_glass'), lid: b('cookie_jar_lid'),
	cookies: b('cookie_jar_cookies'), particle: b('cookie_jar_lid') }, elements: [
	box([5, 0.2, 5], [11, 6, 11], 'cookies'), box([4, 0, 4], [12, 9, 12], 'glass', { skip: ['up'] }),
	box([3.5, 9, 3.5], [12.5, 10, 12.5], 'lid'), box([7, 10, 7], [9, 11.5, 9], 'lid')] });
facingState('cookie_jar');
item('cookie_jar', { parent: b('cookie_jar') });

model('macaron_tower', { parent: 'minecraft:block/block', textures: { side: b('macaron_tower'), top: b('macaron_tower_top'),
	berry: b('cupcake_lamp_cherry'), particle: b('macaron_tower') }, elements: [
	box([3, 0, 3], [13, 4, 13], 'side', { tex: { up: 'top', down: 'top' } }), box([4, 4, 4], [12, 8, 12], 'side', { tex: { up: 'top', down: 'top' } }),
	box([5, 8, 5], [11, 12, 11], 'side', { tex: { up: 'top', down: 'top' } }), box([6, 12, 6], [10, 14, 10], 'side', { tex: { up: 'top', down: 'top' } }),
	box([7, 14, 7], [9, 15.5, 9], 'berry')] });
facingState('macaron_tower');
item('macaron_tower', { parent: b('macaron_tower') });

model('bakery_catalog', { parent: 'minecraft:block/block', textures: { wood: b('blossom_planks'), cover: b('catalog_cover'),
	pages: b('catalog_pages'), particle: b('catalog_cover') }, elements: [
	box([4, 0, 4], [12, 1.5, 12], 'wood'), box([6.5, 1.5, 6.5], [9.5, 9.5, 9.5], 'wood'),
	box([1, 9.5, 2], [15, 11.5, 14], 'pages', { rot: R('x', 22.5, [8, 10.5, 8]), tex: { up: 'cover', down: 'cover' } })] });
facingState('bakery_catalog');
item('bakery_catalog', { parent: b('bakery_catalog') });

model('cake_turntable', { parent: 'minecraft:block/block', textures: { base: b('turntable_base'), plate: b('turntable_plate'),
	particle: b('turntable_plate') }, elements: [
	box([4.5, 0, 4.5], [11.5, 1.5, 11.5], 'base'), box([7, 1.5, 7], [9, 3.5, 9], 'base'),
	box([2, 3.5, 2], [14, 5, 14], 'base', { tex: { up: 'plate' }, uv: { up: [0, 0, 16, 16] } })] });
simpleState('cake_turntable');
item('cake_turntable', { parent: b('cake_turntable') });

const NEW_BLOCKS = ['bakery_catalog', 'cake_turntable', 'pink_checker_tiles', 'mint_checker_tiles', 'lilac_checker_tiles', 'butter_checker_tiles',
	'strawberry_wallpaper', 'gingham_wallpaper', 'heart_wallpaper', 'polka_dot_wallpaper', 'heart_rug', 'cloud_rug', 'pink_heart_balloon',
	'lilac_heart_balloon', 'mint_heart_balloon', 'fairy_lights', 'cupcake_lamp', 'cookie_jar', 'macaron_tower', 'pink_awning', 'mint_awning'];
NEW_BLOCKS.forEach(loot);

// ---------------- Tags ----------------
const cozy = ['heart_rug', 'cloud_rug', 'pink_heart_balloon', 'lilac_heart_balloon', 'mint_heart_balloon', 'fairy_lights', 'cupcake_lamp', 'cookie_jar',
	'macaron_tower', 'pink_awning', 'mint_awning', 'bakery_catalog', 'cake_turntable', 'cafe_table', 'cafe_chair', 'flour_sack', 'bread_basket',
	'cake_stand', 'menu_board', 'bunting', 'jar_shelf', 'strawberry_crate', 'lemon_crate', 'coconut_crate', 'display_case', 'service_bell']
	.map(n => 'bakery:' + n).concat(['#minecraft:flower_pots', '#minecraft:candles', 'minecraft:lantern', 'minecraft:decorated_pot']);
mergeTag(D + 'bakery/tags/block/cozy.json', cozy);
mergeTag(D + 'minecraft/tags/block/mineable/pickaxe.json', ['bakery:pink_checker_tiles', 'bakery:mint_checker_tiles', 'bakery:lilac_checker_tiles',
	'bakery:butter_checker_tiles', 'bakery:cake_turntable', 'bakery:cupcake_lamp', 'bakery:macaron_tower']);
mergeTag(D + 'minecraft/tags/block/mineable/axe.json', ['bakery:bakery_catalog']);

// ---------------- Recipes ----------------
shapeless('bakery_catalog', ['book', 'pink_dye', '#minecraft:planks']);
shaped('cake_turntable', ['TTT', ' I '], { T: 'white_terracotta', I: 'iron_ingot' });
[['heart', 'pink_dye'], ['star', 'yellow_dye'], ['bunny', 'purple_dye'], ['flower', 'lime_dye']].forEach(([s, dye]) =>
	shapeless(s + '_cutter', ['iron_ingot', dye]));
[['pink', 'pink_dye'], ['mint', 'lime_dye'], ['lilac', 'purple_dye'], ['lemon', 'yellow_dye'], ['sky', 'light_blue_dye'], ['white', 'white_dye']].forEach(([c, dye]) =>
	shapeless(c + '_icing', ['paper', 'sugar', 'sugar', dye]));
shapeless('gift_box', ['paper', 'paper', 'string', 'pink_dye'], 2);
shaped('chef_hat', ['WWW', 'W W'], { W: 'white_wool' });
shapeless('strawberry_beret', ['red_wool', 'green_dye', 'bakery:strawberry']);
shapeless('bunny_ears', ['white_wool', 'pink_dye', 'string', 'rabbit_hide']);
shapeless('cat_ears', ['purple_wool', 'pink_dye', 'string']);
shapeless('ribbon_bow', ['pink_wool', 'string']);
shaped('frilly_apron', ['S S', 'PPP', 'PWP'], { S: 'string', P: 'pink_wool', W: 'white_wool' });
[['pink', 'pink_terracotta'], ['mint', 'lime_terracotta'], ['lilac', 'purple_terracotta'], ['butter', 'yellow_terracotta']].forEach(([c, t]) =>
	shaped(c + '_checker_tiles', ['CW', 'WC'], { C: t, W: 'white_terracotta' }, 4));
shapeless('strawberry_wallpaper', ['paper', 'paper', 'white_wool', 'bakery:strawberry'], 4);
shapeless('gingham_wallpaper', ['paper', 'paper', 'white_wool', 'pink_wool'], 4);
shapeless('heart_wallpaper', ['paper', 'paper', 'pink_wool', 'red_dye'], 4);
shapeless('polka_dot_wallpaper', ['paper', 'paper', 'light_blue_wool', 'white_dye'], 4);
shapeless('heart_rug', ['pink_carpet', 'pink_carpet', 'red_dye'], 2);
shapeless('cloud_rug', ['white_carpet', 'white_carpet', 'light_blue_dye'], 2);
[['pink', 'pink_dye'], ['lilac', 'purple_dye'], ['mint', 'lime_dye']].forEach(([c, dye]) => shapeless(c + '_heart_balloon', ['string', 'paper', dye]));
shaped('fairy_lights', ['SGS'], { S: 'string', G: 'glowstone_dust' }, 3);
shapeless('cupcake_lamp', ['bakery:cupcake', 'torch', 'pink_dye']);
shapeless('cookie_jar', ['glass', 'bakery:cookies', 'pink_dye']);
shapeless('macaron_tower', ['bakery:macarons', 'bakery:macarons', 'bakery:macarons', 'bakery:cake_stand']);
shaped('pink_awning', ['PWP'], { P: 'pink_wool', W: 'white_wool' }, 3);
shaped('mint_awning', ['PWP'], { P: 'lime_wool', W: 'white_wool' }, 3);

// ---------------- English ----------------
const lang = {
	'itemGroup.bakery.sweet_life': 'Sweet Life',
	'item.bakery.sweet_coin': 'Sweet Coin', 'tooltip.bakery.sweet_coin': 'Customers pay with these. Spend them at the Bakery Catalog!',
	'block.bakery.bakery_catalog': 'Bakery Catalog', 'block.bakery.cake_turntable': 'Cake Turntable',
	'item.bakery.heart_cutter': 'Heart Cookie Cutter', 'item.bakery.star_cutter': 'Star Cookie Cutter', 'item.bakery.bunny_cutter': 'Bunny Cookie Cutter',
	'item.bakery.flower_cutter': 'Flower Cookie Cutter', 'tooltip.bakery.cookie_cutter': 'Use on Cookie Dough sitting on a Pastry Board',
	'item.bakery.raw_sugar_cookie': 'Raw Sugar Cookies', 'item.bakery.raw_sugar_cookie.shaped': 'Raw %s Sugar Cookies',
	'item.bakery.sugar_cookie': 'Sugar Cookie', 'item.bakery.sugar_cookie.shaped': '%s Sugar Cookie',
	'cookie_shape.bakery.heart': 'Heart', 'cookie_shape.bakery.star': 'Star', 'cookie_shape.bakery.bunny': 'Bunny', 'cookie_shape.bakery.flower': 'Flower',
	'shape.bakery.sugar_cookie': 'Sugar Cookies',
	'item.bakery.pink_icing': 'Pink Icing Bag', 'item.bakery.mint_icing': 'Mint Icing Bag', 'item.bakery.lilac_icing': 'Lilac Icing Bag',
	'item.bakery.lemon_icing': 'Lemon Icing Bag', 'item.bakery.sky_icing': 'Sky Icing Bag', 'item.bakery.white_icing': 'Vanilla Icing Bag',
	'icing.bakery.pink': 'Pink', 'icing.bakery.mint': 'Mint', 'icing.bakery.lilac': 'Lilac', 'icing.bakery.lemon': 'Lemon', 'icing.bakery.sky': 'Sky', 'icing.bakery.white': 'Vanilla',
	'tooltip.bakery.icing_bag': 'Use with a Sugar Cookie in your other hand · frosting for the Cake Turntable',
	'tooltip.bakery.iced': '%s icing', 'tooltip.bakery.ice_me': 'Pipe some icing on me!',
	'item.bakery.gift_box': 'Gift Box', 'tooltip.bakery.gift_box': 'Use with something in your other hand to wrap it',
	'item.bakery.wrapped_gift': 'Wrapped Gift', 'tooltip.bakery.wrapped_gift': 'Use to unwrap · give it to a villager to make a friend',
	'tooltip.bakery.gift_from': 'From %s ♥',
	'item.bakery.chef_hat': 'Chef Hat', 'item.bakery.strawberry_beret': 'Strawberry Beret', 'item.bakery.bunny_ears': 'Bunny Ears',
	'item.bakery.cat_ears': 'Cat Ears', 'item.bakery.ribbon_bow': 'Ribbon Bow', 'item.bakery.frilly_apron': 'Frilly Apron',
	'tooltip.bakery.outfit': 'Hat + Frilly Apron = Sweet Baker outfit: customers tip extra',
	'block.bakery.pink_checker_tiles': 'Pink Checker Tiles', 'block.bakery.mint_checker_tiles': 'Mint Checker Tiles',
	'block.bakery.lilac_checker_tiles': 'Lilac Checker Tiles', 'block.bakery.butter_checker_tiles': 'Butter Checker Tiles',
	'block.bakery.strawberry_wallpaper': 'Strawberry Wallpaper', 'block.bakery.gingham_wallpaper': 'Gingham Wallpaper',
	'block.bakery.heart_wallpaper': 'Heart Wallpaper', 'block.bakery.polka_dot_wallpaper': 'Polka Dot Wallpaper',
	'block.bakery.heart_rug': 'Heart Rug', 'block.bakery.cloud_rug': 'Cloud Rug',
	'block.bakery.pink_heart_balloon': 'Pink Heart Balloon', 'block.bakery.lilac_heart_balloon': 'Lilac Heart Balloon', 'block.bakery.mint_heart_balloon': 'Mint Heart Balloon',
	'block.bakery.fairy_lights': 'Fairy Lights', 'block.bakery.cupcake_lamp': 'Cupcake Lamp', 'block.bakery.cookie_jar': 'Cookie Jar',
	'block.bakery.macaron_tower': 'Macaron Tower', 'block.bakery.pink_awning': 'Pink Awning', 'block.bakery.mint_awning': 'Mint Awning',

	'message.bakery.icing.hint': 'Hold a baked Sugar Cookie in your other hand', 'message.bakery.icing.already': 'That cookie is already iced',
	'message.bakery.icing.done': 'So pretty! %s', 'message.bakery.board.cutter_needs_cookie_dough': 'Cookie cutters need Cookie Dough on the board',
	'message.bakery.gift.hint': 'Hold the present in your other hand', 'message.bakery.gift.wrapped': 'Wrapped with a pretty ribbon ♥',
	'message.bakery.gift.opened': 'You got %s!',
	'message.bakery.turntable.full': 'There is already a cake on the turntable', 'message.bakery.turntable.placed': 'Right-click to decorate · sneak-click to take it back',
	'message.bakery.turntable.empty': 'Put a whole cake on the turntable first', 'message.bakery.turntable.missing': 'You need %s',
	'message.bakery.turntable.done': 'Cuteness %s',
	'message.bakery.catalog.poor': 'Not enough Sweet Coins (%s needed)', 'message.bakery.catalog.bought': 'Bought %s for %s coins ♥',
	'message.bakery.sticker': '✿ New sticker: %s (+%s Sweet Coins)',
	'message.bakery.friend.regular': '%s is now one of your regulars! ♥', 'message.bakery.friend.best': '%s is your best friend! They brought you a present ♥',
	'message.bakery.friend.gift_today': '%s already got a gift from you today', 'message.bakery.friend.gift': '%s loved your gift! ♥ %s/%s',
	'message.bakery.friend.gift_loved': '%s ADORED your gift - their favourite! ♥ %s/%s',
	'message.bakery.shop.till': 'Collected %s Sweet Coins from the till',

	'screen.bakery.cake_studio': 'Cake Studio', 'screen.bakery.cake_studio.cuteness': 'Cuteness',
	'screen.bakery.cake_studio.piping_on': 'Piping ✔', 'screen.bakery.cake_studio.piping_off': 'Piping',
	'screen.bakery.cake_studio.mirror_on': 'Mirror ✔', 'screen.bakery.cake_studio.mirror_off': 'Mirror',
	'screen.bakery.cake_studio.clear': 'Clear', 'screen.bakery.cake_studio.done': 'Done ♥',
	'screen.bakery.cake_studio.frosting': 'Frosting', 'screen.bakery.cake_studio.flavor_frosting': 'Flavour',
	'screen.bakery.cake_studio.toppers': 'Toppers', 'screen.bakery.cake_studio.no_toppers': 'Bring fruit, cream, petals or candles to decorate with!',
	'screen.bakery.catalog': 'Bakery Catalog', 'screen.bakery.catalog.special': '★ Daily Special: %s - only %s coins ★',
	'screen.bakery.catalog.price': '%s Sweet Coins', 'screen.bakery.catalog.hint': 'Click to buy · new special every day',
	'catalog.bakery.decor': 'Decor', 'catalog.bakery.wardrobe': 'Outfits', 'catalog.bakery.garden': 'Garden', 'catalog.bakery.kitchen': 'Kitchen',
	'toast.bakery.sticker': 'Sticker get! ✿',
	'hud.bakery.shop.charm': 'Charm', 'hud.bakery.shop.charm_next': ' (%s more decor)', 'hud.bakery.review.coins': '+%s Sweet Coins',

	'sticker.bakery.first_bake': 'Fresh From the Oven', 'sticker.bakery.first_bake.desc': 'Bake anything in the Grand Brick Oven.',
	'sticker.bakery.five_stars': 'Five Star Treat', 'sticker.bakery.five_stars.desc': 'Pull a perfect 5-star bake out of the oven.',
	'sticker.bakery.first_customer': 'Open for Business', 'sticker.bakery.first_customer.desc': 'Serve your first customer at the Bakery Counter.',
	'sticker.bakery.regular': 'A Regular', 'sticker.bakery.regular.desc': 'Reach 5 friendship hearts with a villager.',
	'sticker.bakery.best_friend': 'Best Friends', 'sticker.bakery.best_friend.desc': 'Reach 10 friendship hearts with a villager.',
	'sticker.bakery.cake_artist': 'Cake Artist', 'sticker.bakery.cake_artist.desc': 'Decorate a cake at the Cake Turntable.',
	'sticker.bakery.cookie_artist': 'Cookie Artist', 'sticker.bakery.cookie_artist.desc': 'Ice a Sugar Cookie with an Icing Bag.',
	'sticker.bakery.gift_giver': 'Sweet Surprise', 'sticker.bakery.gift_giver.desc': 'Give a Wrapped Gift to a villager.',
	'sticker.bakery.dress_up': 'Dress Up', 'sticker.bakery.dress_up.desc': 'Wear a bakery hat and the Frilly Apron together.',
	'sticker.bakery.cozy_cafe': 'Cozy Café', 'sticker.bakery.cozy_cafe.desc': 'Decorate around a counter until it has 5 hearts of Charm.',
	'sticker.bakery.shopper': 'Window Shopper', 'sticker.bakery.shopper.desc': 'Buy something from the Bakery Catalog.',
	'sticker.bakery.piggy_bank': 'Piggy Bank', 'sticker.bakery.piggy_bank.desc': 'Earn 100 Sweet Coins.',
	'sticker.bakery.collector': 'Recipe Collector', 'sticker.bakery.collector.desc': 'Discover 25 different recipes.',
	'sticker.bakery.orchard': 'Orchard Keeper', 'sticker.bakery.orchard.desc': 'Pick ripe fruit from a fruit tree.',
	'sticker.bakery.dreamy': 'Sweet Dreams', 'sticker.bakery.dreamy.desc': 'Harvest Candyfloss (it only grows at night or in the rain).',
	'sticker.bakery.star_patissier': 'Star Pâtissier', 'sticker.bakery.star_patissier.desc': 'Become a Star Pâtissier: 50 reviews averaging 4.5 stars.',

	'guide.bakery.chapter.start': 'Start Here', 'guide.bakery.chapter.garden': 'Garden & Orchard', 'guide.bakery.chapter.kitchen': 'Kitchen',
	'guide.bakery.chapter.oven': 'Oven & Machines', 'guide.bakery.chapter.sweets': 'Sweets & Decorating', 'guide.bakery.chapter.cafe': 'Your Café',
	'guide.bakery.chapter.style': 'Style & Decor', 'guide.bakery.chapter.stickers': 'Sticker Book',
	'guide.bakery.sweet_life.title': 'The Sweet Life',
	'guide.bakery.sweet_life.text': 'Customers now pay in Sweet Coins. Spend them at the Bakery Catalog on decor, outfits, seeds and kitchen goodies. Decorate cakes, ice cookies, wrap gifts, make villager friends and fill your sticker book!',
	'guide.bakery.progress.title': 'My Bakery', 'guide.bakery.progress.recipes': 'Recipes: %s / %s', 'guide.bakery.progress.coins': 'Coins earned: %s',
	'guide.bakery.progress.served': 'Customers served: %s', 'guide.bakery.progress.friends': 'Regulars: %s', 'guide.bakery.progress.stickers': 'Stickers: %s / %s',
	'guide.bakery.progress.next': 'Next goal ♥', 'guide.bakery.progress.all_done': 'Every sticker collected! You are a true Sweetheart Baker.',
	'guide.bakery.first_day.title': 'Your First Day',
	'guide.bakery.first_day.text': '1. Plant berries and collect eggs, sugar and wheat.\n2. Grind flour, mix dough in a Mixing Bowl.\n3. Shape it on a Pastry Board.\n4. Build the Grand Oven and bake!\n5. Open a Bakery Counter and sell.',
	'guide.bakery.stickers.title': 'Sticker Book',
	'guide.bakery.cookies.title': 'Sugar Cookies',
	'guide.bakery.cookies.text': 'Put Cookie Dough on a Pastry Board and use a Cookie Cutter on it: heart, star, bunny or flower. Bake the tray in the Grand Oven for four Sugar Cookies.',
	'guide.bakery.icing.title': 'Icing Cookies',
	'guide.bakery.icing.text': 'Hold a baked Sugar Cookie in one hand and an Icing Bag in the other, then use the bag. Six pastel colours! Iced cookies get an extra star.',
	'guide.bakery.cake_studio.title': 'Cake Studio',
	'guide.bakery.cake_studio.text': 'Set a whole cake on the Cake Turntable and right-click it. Pick a frosting (from your Icing Bags), pick toppers from your bag and click the cake to place them. Right-click removes one.',
	'guide.bakery.cake_tips.title': 'Cute Cake Tips',
	'guide.bakery.cake_tips.text': 'Cuteness hearts come from frosting, piping, lots of toppers, three or more kinds and symmetry (try Mirror!). Candles count too. Cute cakes get bonus stars.',
	'guide.bakery.gifts.title': 'Gifts',
	'guide.bakery.gifts.text': 'Hold anything in your other hand and use a Gift Box to wrap it. Give it to a friend, or to a villager once a day: +2 hearts, +3 if it is their favourite flavour.',
	'guide.bakery.coins.title': 'Sweet Coins',
	'guide.bakery.coins.text': 'Customers and food critics pay in Sweet Coins. Tips grow with stars, speed, the right flavour, your bakery\'s Charm and the Sweet Baker outfit.',
	'guide.bakery.catalog.title': 'Bakery Catalog',
	'guide.bakery.catalog.text': 'Right-click the Catalog to shop: decor, outfits, seeds, saplings and kitchen tools. One item each day is the Daily Special at a quarter off.',
	'guide.bakery.charm.title': 'Charm',
	'guide.bakery.charm.text': 'Cozy decorations within 6 blocks of a counter give it Charm hearts: balloons, lamps, rugs, furniture, candles, flower pots... Charm brings more customers who wait longer and tip more.',
	'guide.bakery.friends.title': 'Regulars',
	'guide.bakery.friends.text': 'Every customer gets a cute name. Great service (4+ stars) and gifts earn friendship hearts. At 5 hearts they become a regular, at 10 your best friend - with a present!',
	'guide.bakery.wardrobe.title': 'Wardrobe',
	'guide.bakery.wardrobe.text': 'Hats go on your head with a right-click: Chef Hat, Strawberry Beret, Bunny Ears, Cat Ears and a Ribbon Bow. The Frilly Apron is worn like a chestplate.',
	'guide.bakery.outfit.title': 'Sweet Baker',
	'guide.bakery.outfit.text': 'Wear any bakery hat together with the Frilly Apron and you are a Sweet Baker: every customer tips an extra Sweet Coin.',
	'guide.bakery.decor.title': 'Pastel Decor',
	'guide.bakery.decor.text': 'Checker tiles, strawberry, gingham, heart and polka dot wallpaper, heart and cloud rugs and floating heart balloons. Craft them or buy them in the Catalog.',
	'guide.bakery.decor_more.title': 'Little Touches',
	'guide.bakery.decor_more.text': 'Fairy Lights and Cupcake Lamps glow, Cookie Jars and Macaron Towers dress up a counter, and striped Awnings make a lovely shop front.'
};
const langPath = A + 'lang/en_us.json';
const current = JSON.parse(fs.readFileSync(langPath));
Object.assign(current, lang);
// Coins replaced emeralds in a few existing lines.
current['hud.bakery.shop.stats'] = '%s waiting · till %s coins · %s served';
current['guide.bakery.reviews.text'] = 'Better bakes earn bigger tips in Sweet Coins. Stock a Display Case and customers serve themselves - collect the till by sneak-clicking the counter. Sneak-click any villager with a bake for a review!';
delete current['hud.bakery.review.emeralds'];
put(langPath, current);
return files + ' files + ' + Object.keys(lang).length + ' lang keys';
})()
