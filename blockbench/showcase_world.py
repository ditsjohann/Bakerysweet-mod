"""
Builds the downloadable "Sweetheart Bakery Showcase" flat world (for screenshots and videos on CurseForge).

Writes run/showcase_world.txt, a script for the dev Showcase client (see the "showcaseWorld" run config): it builds
everything with commands in the superflat world run/saves/sweetheart_showcase, takes a few preview shots and quits.

Layout (ground level y = -60, the spawn plaza at 0, 0):
    south   the Sweetheart Bakery shop (furnished, cats, coffee corner, garden front, terrace)
    east    the kitchen hall: every machine in action (/bakery demo), a coffee bar, wallpaper and floor samples,
            all the decorations in rows, and a little café with customers
    west    the farm: every crop fully grown, and all seven fruit trees
    north   a wild meadow with a pond, then a street with a little house front for each of the seven woods
Run: python3 blockbench/showcase_world.py
"""
import json
import os

G = -60  # the floor everyone stands on (the grass is at -61)
OUT = os.path.join(os.path.dirname(__file__), "..", "run", "showcase_world.txt")
WOODS = ["blossom", "citrus", "peachwood", "orangewood", "mangowood", "plumwood", "palm"]

L = []


def cmd(s):
    L.append("cmd " + s)


def sb(x, y, z, block):
    cmd(f"setblock {x} {y} {z} {block}")


def fill(x1, y1, z1, x2, y2, z2, block):
    cmd(f"fill {x1} {y1} {z1} {x2} {y2} {z2} {block}")


def sign(x, z, rotation, lines, wood="cherry", y=G):
    """A standing sign; lines are (text, colour) pairs."""
    msgs = [json.dumps(json.dumps({"text": t, "color": c, "bold": i == 0})) for i, (t, c) in enumerate(lines)]
    while len(msgs) < 4:
        msgs.append(json.dumps('""'))
    text = f"{{has_glowing_text:0b,messages:[{','.join(msgs)}]}}"
    sb(x, y, z, f"minecraft:{wood}_sign[rotation={rotation}]{{front_text:{text},back_text:{text}}}")


PINK, MINT, BROWN = "#d0567e", "#3e9a76", "#8a5a3a"


def path(x1, z1, x2, z2):
    fill(x1, G - 1, z1, x2, G - 1, z2, "bakery:cream_tiles")


# ------------------------------------------------------------------ setup

def setup():
    L.append("note === building the Sweetheart Bakery showcase world ===")
    for rule, v in (("sendCommandFeedback", "false"), ("doDaylightCycle", "false"), ("doWeatherCycle", "false"), ("doMobSpawning", "false"),
                    ("doFireTick", "false"), ("randomTickSpeed", "0"), ("doInsomnia", "false"), ("doPatrolSpawning", "false"),
                    ("doTraderSpawning", "false"), ("keepInventory", "true"), ("announceAdvancements", "false")):
        cmd(f"gamerule {rule} {v}")
    cmd("time set 6000")
    cmd("weather clear")
    cmd("difficulty peaceful")
    cmd(f"forceload add -80 -70 80 60")
    L.append("wait 60")
    cmd("kill @e[type=!player]")


# ------------------------------------------------------------------ plaza

def plaza():
    fill(-7, G - 1, -7, 7, G - 1, 7, "bakery:pink_checker_tiles")
    fill(-6, G - 1, -6, 6, G - 1, 6, "bakery:cream_tiles")
    fill(-2, G - 1, -2, 2, G - 1, 2, "bakery:pink_tiles")
    for x, z in ((-6, -6), (6, -6), (-6, 6), (6, 6)):
        sb(x, G, z, "bakery:heart_lamp_post[facing=north]")
    sb(0, G, 0, "bakery:bird_bath[facing=north]")
    for x, z, f in ((-3, -6, "south"), (3, -6, "south")):
        sb(x, G, z, f"bakery:pastel_bench[facing={f}]")
    for x, z in ((-6, -2), (-6, 2), (6, -2), (6, 2)):
        sb(x, G, z, "bakery:tulip_pot[facing=north]")
    for x, z in ((-2, -6), (2, -6)):
        sb(x, G, z, "bakery:potted_succulent[facing=north]")
    sign(-4, 7, 8, [("Sweetheart", PINK), ("Bakery", PINK), ("Showcase", BROWN), ("♥", PINK)])
    sign(-7, 3, 4, [("← The Farm", MINT), ("crops & trees", BROWN)])
    sign(7, 3, 12, [("Kitchen Hall →", PINK), ("machines, coffee,", BROWN), ("decor & café", BROWN)])
    sign(3, -7, 8, [("↑ Wild Meadow", MINT), ("& the Woods", BROWN)])
    sign(4, 7, 8, [("↓ The Bakery", PINK), ("cats & coffee", BROWN)])
    # paths to every area
    path(-1, 8, 1, 12)        # south to the shop
    path(8, -1, 27, 1)        # east to the kitchen hall
    path(-14, -1, -8, 1)      # west to the farm
    path(-1, -14, 1, -8)      # north to the meadow
    for x in (-11, 11, 17, 23):
        if x > 0:
            sb(x, G, 2, "bakery:daisies")
            sb(x + 2, G, -2, "bakery:buttercups")
        else:
            sb(x, G, 2, "bakery:pink_cosmos")


# ------------------------------------------------------------------ the bakery shop (south)

def shop():
    # Built from the old playtest shop: old (x, y, z) -> (x - 346, y - 160, z - 285)
    def T(x, y, z):
        return x - 346, y - 160, z - 285

    def s(x, y, z, b):
        sb(*T(x, y, z), b)

    def f(x1, y1, z1, x2, y2, z2, b):
        a, b1, c = T(x1, y1, z1)
        d, e, g = T(x2, y2, z2)
        fill(a, b1, c, d, e, g, b)

    f(342, 99, 301, 350, 99, 307, "bakery:pink_checker_tiles")
    for y in range(100, 104):
        blk = "bakery:blossom_planks" if y in (100, 103) else "bakery:strawberry_wallpaper"
        f(341, y, 308, 351, y, 308, blk)
        f(341, y, 300, 341, y, 308, blk)
        f(351, y, 300, 351, y, 308, blk)
    f(341, 100, 300, 351, 103, 300, "bakery:blossom_planks")
    # the outside of the side and back walls is planks, the strawberry wallpaper only shows inside
    for y in (101, 102):
        f(340, y, 300, 340, y, 309, "bakery:blossom_planks")
        f(352, y, 300, 352, y, 309, "bakery:blossom_planks")
        f(340, y, 309, 352, y, 309, "bakery:blossom_planks")
    for x, z in ((341, 300), (351, 300), (341, 308), (351, 308)):
        f(x, 100, z, x, 103, z, "bakery:stripped_blossom_log[axis=y]")
    for x in (343, 344, 348, 349):
        for y in (101, 102):
            s(x, y, 300, "minecraft:pink_stained_glass_pane[east=true,west=true]")
    for z in (303, 304):
        for y in (101, 102):
            for x in (340, 341, 351, 352):
                s(x, y, z, "minecraft:pink_stained_glass_pane[north=true,south=true]")
    s(346, 100, 300, "bakery:blossom_door[facing=south,half=lower,hinge=left,open=true]")
    s(346, 101, 300, "bakery:blossom_door[facing=south,half=upper,hinge=left,open=true]")
    for i, y in enumerate((104, 105, 106)):
        x1, x2, z1, z2 = 339 + i, 353 - i, 299 + i, 310 - i
        f(x1, y, z1, x2, y, z1, "bakery:blossom_stairs[facing=south]")
        f(x1, y, z2, x2, y, z2, "bakery:blossom_stairs[facing=north]")
        f(x1, y, z1 + 1, x1, y, z2 - 1, "bakery:blossom_stairs[facing=east]")
        f(x2, y, z1 + 1, x2, y, z2 - 1, "bakery:blossom_stairs[facing=west]")
        f(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, "bakery:blossom_planks")
    f(342, 107, 302, 350, 107, 307, "bakery:blossom_slab[type=bottom]")
    # front
    for x in (343, 344, 348, 349):
        s(x, 103, 299, "bakery:pink_awning[facing=north]")
        s(x, 100, 299, "bakery:flower_box[facing=north]")
    s(346, 102, 299, "bakery:mint_awning[facing=north]")
    s(345, 101, 299, "bakery:open_sign[facing=north]")
    s(347, 103, 299, "bakery:bunting[facing=north]")
    s(345, 103, 299, "bakery:bunting[facing=north]")
    s(346, 100, 299, "bakery:welcome_mat[facing=north]")
    for z in (296, 297, 298):
        s(346, 100, z, "bakery:stepping_stones")
    s(342, 100, 297, "bakery:heart_lamp_post[facing=north]")
    s(350, 100, 297, "bakery:heart_lamp_post[facing=north]")
    for x in list(range(340, 345)) + list(range(348, 353)):
        e = "true" if x not in (344, 352) else "false"
        w = "true" if x not in (340, 348) else "false"
        s(x, 100, 295, f"bakery:pink_picket_fence[east={e},west={w}]")
    s(347, 100, 297, "bakery:chalkboard_sign[facing=north]")
    s(343, 100, 297, "bakery:bunny_statue[facing=north]")
    s(339, 100, 296, "bakery:pink_mailbox[facing=north]")
    for x, b in ((341, "bakery:rose_bush"), (344, "bakery:tulip_pot[facing=north]"), (348, "bakery:hydrangea_bush"),
                 (349, "bakery:daisies"), (351, "bakery:rose_bush"), (345, "bakery:buttercups")):
        s(x, 100, 298, b)
    for x, b in ((341, "bakery:pink_cosmos"), (344, "bakery:forget_me_nots"), (345, "bakery:wildflower_patch"),
                 (349, "bakery:bluebells"), (350, "bakery:clover_patch"), (351, "bakery:foxglove")):
        s(x, 100, 297, b)
    s(339, 101, 303, "bakery:hanging_ivy[facing=west]")
    s(339, 101, 305, "bakery:hanging_ivy[facing=west]")
    # terrace (east)
    f(353, 99, 301, 357, 99, 307, "bakery:cream_tiles")
    s(355, 100, 303, "bakery:parasol_table[facing=north]")
    s(354, 100, 303, "bakery:cafe_chair[facing=east]")
    s(356, 100, 303, "bakery:cafe_chair[facing=west]")
    s(355, 100, 306, "bakery:pastel_bench[facing=north]")
    s(357, 100, 301, "bakery:potted_fern[facing=north]")
    s(353, 100, 307, "bakery:tulip_pot[facing=north]")
    s(357, 100, 307, "bakery:mushroom_lamp[facing=north]")
    s(357, 100, 304, "bakery:stump_stool[facing=west]")
    # inside: counter row
    s(343, 100, 305, "bakery:display_case[facing=north]")
    s(344, 100, 305, "bakery:display_case[facing=north]")
    s(346, 100, 305, "bakery:bakery_counter[facing=north,open=true]")
    s(347, 100, 305, "bakery:pastry_shelf[facing=north]")
    s(348, 100, 305, "bakery:tiered_stand[facing=north]")
    f(342, 100, 307, 350, 100, 307, "bakery:stripped_blossom_wood")
    s(342, 101, 307, "bakery:coffee_machine[facing=north,level=3]")
    s(343, 101, 307, "bakery:syrup_rack[facing=north,slot0=1,slot1=2,slot2=4,slot3=5]")
    s(344, 101, 307, "bakery:cash_register[facing=north]")
    s(345, 101, 307, "bakery:cookie_jar[facing=north]")
    s(346, 101, 307, "bakery:tip_jar[facing=north]")
    s(347, 101, 307, "bakery:bread_basket[facing=north]")
    s(348, 101, 307, "bakery:glass_cloche[facing=north]")
    s(349, 101, 307, "bakery:cake_stand[facing=north]")
    s(350, 101, 307, "bakery:cupcake_lamp[facing=north]")
    s(343, 102, 307, "bakery:cake_shelf[facing=north]")
    s(345, 102, 307, "bakery:menu_board[facing=north]")
    s(347, 102, 307, "bakery:neon_heart_sign[facing=north]")
    s(349, 102, 307, "bakery:donut_wall[facing=north]")
    for x in range(342, 351):
        s(x, 103, 307, "bakery:star_lights[facing=north]")
    # seating
    s(343, 100, 302, "bakery:cafe_table[facing=north]")
    s(342, 100, 302, "bakery:cafe_chair[facing=east]")
    s(344, 100, 302, "bakery:cafe_chair[facing=west]")
    s(349, 100, 302, "bakery:cafe_table[facing=north]")
    s(348, 100, 302, "bakery:cafe_chair[facing=east]")
    s(350, 100, 302, "bakery:cafe_chair[facing=west]")
    s(343, 101, 302, "bakery:tea_set[facing=north]")
    s(349, 101, 302, "bakery:tulip_vase[facing=north]")
    s(342, 100, 303, "bakery:pink_couch[facing=east,part=left]")
    s(342, 100, 304, "bakery:pink_couch[facing=east,part=right]")
    s(350, 100, 304, "bakery:cat_bed[facing=west]")
    s(348, 100, 304, "bakery:macaron_pouf[facing=north]")
    s(345, 100, 301, "bakery:pink_heart_balloon[facing=north]")
    s(347, 100, 301, "bakery:mint_heart_balloon[facing=north]")
    s(350, 100, 301, "bakery:potted_fern[facing=north]")
    s(342, 100, 301, "bakery:flour_sack[facing=north]")
    s(345, 100, 304, "bakery:bunny_plush[facing=north]")
    s(344, 100, 304, "bakery:bear_plush[facing=north]")
    for x in (343, 344, 348, 349):
        s(x, 101, 301, "bakery:lace_curtains[facing=south]")
    for x in (342, 345, 347, 350):
        s(x, 103, 301, "bakery:fairy_lights[facing=south]")
    s(342, 102, 306, "bakery:heart_mirror[facing=east]")
    s(346, 103, 303, "bakery:hanging_flowers[facing=north]")
    s(350, 100, 306, "bakery:cafe_jukebox[facing=west,playing=true]")
    for x, z, v, c in ((349.5, 304.5, "calico", 6), (343.5, 303.5, "ragdoll", 3), (355.5, 305.5, "red", 2)):
        a, b, d = T(x, 100, z)
        cmd(f'summon bakery:cafe_cat {a} {b} {d} {{variant:"minecraft:{v}",Owner:[I;1,2,3,4],CollarColor:{c}b,PersistenceRequired:1b}}')
    cmd(f"execute as @e[type=bakery:cafe_cat,x={T(340,100,295)[0]},y={G - 2},z={T(340,100,295)[2]},dx=20,dy=8,dz=16] run data merge entity @s {{CafeHome:[I;{T(350,100,304)[0]},{G},{T(350,100,304)[2]}],CafeRadius:8}}")


# ------------------------------------------------------------------ kitchen hall (east)

def kitchen():
    o = (30, G, 0)
    cmd(f"bakery demo {o[0]} {o[1]} {o[2]}")
    L.append("wait 40")
    # coffee bar along the back (z = -14)
    x0, z = 30, -14
    fill(x0, G, z, x0 + 12, G, z, "bakery:stripped_blossom_wood")
    sb(x0, G + 1, z, "bakery:coffee_machine[facing=south,level=3]")
    sb(x0 + 1, G + 1, z, "bakery:syrup_rack[facing=south,slot0=1,slot1=2,slot2=3,slot3=4]")
    sb(x0 + 2, G + 1, z, "bakery:syrup_rack[facing=south,slot0=5,slot1=0,slot2=1,slot3=0]")
    for i, d in enumerate(("latte", "coffee", "matcha_latte", "hot_chocolate", "strawberry_milk", "lavender_tea", "rose_tea", "mint_tea",
                           "lemonade", "pineapple_smoothie")):
        sb(x0 + 3 + i, G + 1, z, f"bakery:mug[facing=south,drink={d}]")
    fill(x0 - 1, G, z + 1, x0 + 13, G, z + 1, "air")
    sign(x0 - 1, z + 1, 0, [("Coffee Bar", BROWN), ("syrups & drinks", BROWN)], wood="cherry")
    # floor and wall samples (z = 14..15)
    walls = ["pink_checker_tiles", "mint_checker_tiles", "lilac_checker_tiles", "butter_checker_tiles", "pink_tiles", "mint_tiles",
             "lavender_tiles", "cream_tiles", "gingham_wallpaper", "strawberry_wallpaper", "heart_wallpaper", "polka_dot_wallpaper", "bow_wallpaper"]
    for i, w in enumerate(walls):
        x = 30 + i * 2
        fill(x, G, 15, x + 1, G + 2, 15, f"bakery:{w}")
        fill(x, G - 1, 13, x + 1, G - 1, 14, f"bakery:{w}")
    sb(56, G, 14, "bakery:heart_rug")
    sb(57, G, 14, "bakery:cloud_rug")
    sign(29, 14, 4, [("Floors &", PINK), ("Wallpapers", PINK)])
    # the placed treats on a table (z = 12)
    foods = ["bread_loaf", "baguette", "croissant", "cupcake", "donut", "cookies", "macarons", "pie", "cake"]
    fill(30, G, 12, 30 + len(foods) * 2, G, 12, "bakery:stripped_peachwood_wood")
    for i, food in enumerate(foods):
        sb(31 + i * 2, G + 1, 12, f"bakery:{food}[facing=south]" + ("" if food not in ("cupcake", "cookies", "donut", "macarons") else ""))
    sign(30, 11, 0, [("Treats", PINK), ("(placed bakes)", BROWN)])


# ------------------------------------------------------------------ farm (west)

CROPS = ["strawberry", "blueberry", "raspberry", "sugar_beet", "coffee", "cinnamon", "tea", "mint", "rose", "lavender", "vanilla",
         "pineapple", "starbloom", "sweet_potato", "peanut", "rhubarb", "hibiscus", "cranberry", "candyfloss"]


def farm():
    x0, z0 = -16, -9
    # rows run west; each crop gets a 1 x 3 strip, water between every two rows
    fill(x0 - 2 * len(CROPS) - 8, G - 1, z0 - 1, x0 + 1, G - 1, z0 + 5, "minecraft:grass_block")
    for i, crop in enumerate(CROPS):
        x = x0 - i * 2
        fill(x, G - 1, z0, x, G - 1, z0 + 2, "minecraft:farmland[moisture=7]")
        fill(x - 1, G - 1, z0, x - 1, G - 1, z0 + 2, "minecraft:water" if i % 2 == 0 else "minecraft:farmland[moisture=7]")
    for i, crop in enumerate(CROPS):
        x = x0 - i * 2
        for z in range(z0, z0 + 3):
            sb(x, G, z, f"bakery:{crop}_crop[age=3]")
        sb(x, G, z0 + 4, "minecraft:cherry_sign[rotation=8]{front_text:{messages:['\"\"','" + json.dumps({"text": crop.replace('_', ' ').title(), "color": MINT}) + "','\"\"','\"\"']}}")
    # tall crops and zucchini
    xt = x0 - len(CROPS) * 2 - 1
    for k, (crop, h) in enumerate((("corn", 3), ("pea", 2))):
        for dz in range(3):
            x, z = xt - k * 2, z0 + dz
            sb(x, G - 1, z, "minecraft:farmland[moisture=7]")
            for seg in range(h):
                sb(x, G + seg, z, f"bakery:{crop}_crop[age=3,segment={seg}]")
        sb(xt - k * 2, G, z0 + 4, "minecraft:cherry_sign[rotation=8]{front_text:{messages:['\"\"','" + json.dumps({"text": crop.title(), "color": MINT}) + "','\"\"','\"\"']}}")
    xz = xt - 5
    sb(xz, G - 1, z0 + 1, "minecraft:farmland[moisture=7]")
    sb(xz, G, z0 + 1, "bakery:attached_zucchini_stem[facing=south]")
    sb(xz, G, z0 + 2, "bakery:zucchini_block[facing=north]")
    sb(xz, G, z0 + 4, "minecraft:cherry_sign[rotation=8]{front_text:{messages:['\"\"','" + json.dumps({"text": "Zucchini", "color": MINT}) + "','\"\"','\"\"']}}")
    sign(-13, -3, 4, [("The Farm", MINT), ("every crop,", BROWN), ("fully grown", BROWN)])
    # fruit trees (z = 8), each with its name
    trees = ["peach_tree", "lemon_tree", "cherry_tree", "orange_tree", "mango_tree", "plum_tree", "palm_tree"]
    for i, t in enumerate(trees):
        x, z = -18 - i * 9, 9
        if t == "palm_tree":
            fill(x - 2, G - 1, z - 2, x + 2, G - 1, z + 2, "minecraft:sand")
        cmd(f"place feature bakery:{t} {x} {G} {z}")
        sb(x, G, z - 4, "minecraft:cherry_sign[rotation=8]{front_text:{messages:['\"\"','" + json.dumps({"text": t.replace('_', ' ').title(), "color": PINK}) + "','\"\"','\"\"']}}")
    path(-80, 3, -14, 4)


# ------------------------------------------------------------------ meadow and pond (north), woods street

def meadow():
    sign(2, -13, 8, [("Wild Meadow", MINT)])
    # pond
    fill(-9, G - 2, -24, -3, G - 1, -19, "minecraft:water")
    fill(-8, G - 2, -25, -4, G - 1, -25, "minecraft:water")
    for x, z in ((-8, -22), (-5, -20), (-4, -23)):
        sb(x, G, z, "bakery:blossom_lily_pad")
    for x, z in ((-10, -21), (-10, -23), (-2, -20)):
        sb(x, G, z, "bakery:cattails")
    flowers = ["daisies", "pink_cosmos", "forget_me_nots", "lily_of_the_valley", "bluebells", "buttercups", "foxglove", "wild_fern"]
    k = 0
    for x in range(2, 14):
        for z in range(-26, -15):
            if (x * 7 + z * 3) % 4 == 0:
                sb(x, G, z, f"bakery:{flowers[k % len(flowers)]}")
                k += 1
    for x, z, b in ((4, -18, "bakery:rose_bush"), (11, -24, "bakery:hydrangea_bush"), (8, -21, "bakery:mossy_log[facing=east]"),
                    (12, -18, "bakery:mossy_boulder[facing=north]"), (5, -24, "bakery:wild_mushrooms"), (9, -17, "bakery:clover_patch"),
                    (13, -21, "bakery:mossy_pebbles"), (3, -22, "bakery:wildflower_patch"), (-12, -18, "bakery:stump_stool[facing=east]"),
                    (-12, -24, "bakery:mushroom_lamp[facing=north]"), (-1, -26, "bakery:bird_bath[facing=north]")):
        sb(x, G, z, b)
    for z in range(-26, -14, 2):
        sb(0, G, z, "bakery:stepping_stones")
        sb(-1, G, z + 1, "bakery:stepping_stones")


def woods_street():
    z = -34
    path(-37, z + 3, 37, z + 4)
    sign(-38, z + 3, 4, [("The Woods", BROWN), ("7 fruit woods", BROWN)])
    for i, w in enumerate(WOODS):
        x = -35 + i * 10
        # a little house front: log corners, planks wall, door, trapdoor windows, stairs roof, fence and gate
        fill(x, G, z, x + 6, G + 3, z, f"bakery:{w}_planks")
        for cx in (x, x + 6):
            fill(cx, G, z, cx, G + 3, z, f"bakery:{w}_log[axis=y]")
        sb(x + 3, G, z, f"bakery:{w}_door[facing=south,half=lower]")
        sb(x + 3, G + 1, z, f"bakery:{w}_door[facing=south,half=upper]")
        for wx in (x + 1, x + 5):
            sb(wx, G + 1, z, "minecraft:glass_pane[east=true,west=true]")
            sb(wx, G + 1, z + 1, f"bakery:{w}_trapdoor[facing=south,half=top,open=true]")
        fill(x - 1, G + 4, z, x + 7, G + 4, z, f"bakery:{w}_stairs[facing=south]")
        fill(x, G + 5, z, x + 6, G + 5, z, f"bakery:{w}_slab[type=bottom]")
        fill(x + 1, G + 3, z - 1, x + 5, G + 3, z - 1, f"bakery:{w}_leaves")
        sb(x, G, z + 2, f"bakery:{w}_fence[east=true]")
        sb(x + 1, G, z + 2, f"bakery:{w}_fence_gate[facing=south]")
        sb(x + 2, G, z + 2, f"bakery:{w}_fence[west=true]")
        sb(x + 5, G, z + 2, f"bakery:stripped_{w}_log[axis=y]")
        sb(x + 6, G, z + 2, f"bakery:{w}_wood[axis=y]")
        sb(x + 5, G + 1, z + 2, f"bakery:{w}_slab[type=bottom]")
        sb(x + 4, G, z + 2, f"bakery:{w}_sapling")
        sb(x + 3, G - 1, z + 2, f"bakery:{w}_planks")
        sign(x + 6, z + 3, 0, [(w.replace("wood", " wood").title().strip(), PINK)])


def finish():
    cmd("kill @e[type=item]")
    cmd("setworldspawn 0 -60 0 0")
    cmd("defaultgamemode creative")
    cmd("gamemode creative @a")
    cmd("clear @a")
    for i, item in enumerate(("bakery:bakery_catalog", "bakery:bakery_guide", "bakery:path_ribbon", "bakery:kitten_basket", "bakery:latte",
                              "bakery:cupcake", "bakery:pink_icing", "bakery:cafe_jukebox", "bakery:sweet_coin 64")):
        cmd(f"item replace entity @a hotbar.{i} with {item}")
    cmd("forceload remove all")
    cmd("gamemode spectator @a")  # spectators float, so the high camera spots stay put
    L.append("cam 0.5 -60 -2.5 0 8")
    L.append("wait 60")
    L.append("hud off")
    for name, cam in (("sw_spawn", "0.5 -58.4 -3.5 0 12"), ("sw_shop", "0.5 -58.5 2.0 0 10"), ("sw_kitchen", "27.5 -56 -2.5 -90 20"),
                      ("sw_farm", "-14.5 -55 -4.5 90 25"), ("sw_meadow", "0.5 -56 -12.5 180 20"), ("sw_woods", "-0.5 -56 -25.5 180 12"),
                      ("sw_overview", "42.5 -30 46.5 145 26"), ("sw_overview2", "-46.5 -36 -50.5 -40 22")):
        L.append(f"cam {cam}")
        L.append("wait 40")
        L.append(f"shot {name}")
    cmd("gamemode creative @a")
    L.append("cam 0.5 -60 -2.5 0 8")
    L.append("wait 20")
    L.append("quit")


def build():
    setup()
    plaza()
    shop()
    kitchen()
    farm()
    meadow()
    woods_street()
    finish()
    with open(OUT, "w") as f:
        f.write("\n".join(L) + "\n")
    return len(L)


if __name__ == "__main__":
    print(build(), "steps written to", OUT)
