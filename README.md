# Sweetheart Bakery

A cozy bakery mod for Minecraft 1.21.1 (Fabric). Grow pretty ingredients, mix and roll dough, bake it to perfection, decorate it, and let the villagers review your bakes.

## How to bake

1. **Farm.** Break grass to find seeds: strawberry, blueberry, tea, vanilla, lavender, rose and the rare glowing **starbloom**. Plant them on farmland. Each crop has 4 growth stages.
2. **Stock the pantry.** Flour (3 wheat), butter (milk bucket), whipped cream, chocolate, matcha powder (smelt tea leaves), sprinkles and candied petals.
3. **Mix.** Place a **Mixing Bowl**, drop ingredients in one by one, then stir with a **Whisk** 8 times. The HUD shows what you are about to make.
   - Bread dough: 2 flour + water bucket
   - Sweet dough: flour + egg + sugar + butter
   - Pastry dough: 2 flour + 2 butter
   - Cookie dough: flour + 2 sugar + butter
   - Add **one** flavoring to flavor it: vanilla, chocolate, strawberry, blueberry, matcha, lavender, rose or star petals.
4. **Roll.** Put dough on a **Pastry Board** and use a **Rolling Pin**. Choose what to make, then press SPACE (or click) when the pin is over the green zone. Gold is perfect. Five rolls set the starting stars.
5. **Bake.** Everything happens by hand at the **Grand Brick Oven**:
   - Feed the fire coal or wood. Click the oven with an empty hand to swing the iron door down, click again to slide the wire rack out.
   - Set raw bakes on the 3x3 rack where you click. Small bakes take one square, a loaf two, a baguette three, and a pie or cake a 2x2 block.
   - Click the oven to slide the rack back in, sneak-click to shut the door. It only bakes with the rack inside, and an open door lets the heat out.
   - Cookies, sugar cookies and donuts sizzle halfway through: pull the rack out and click them to flip them, or one side scorches.
   - A bell rings when a bake is ready (golden sparkles). Pull the rack out and click it to lift it off. Wait too long and it burns.
6. **Decorate.** Hold a cupcake, donut, cookies, macarons or cake in your offhand and use a topping (strawberries, blueberries, cream, sprinkles, chocolate, petals, lavender, star petals). Placed cakes can be decorated directly. Each topping adds stars.
7. **Serve.** Every bake can be placed as decoration and eaten later. Right-click to eat a serving, or sneak + right-click to pick it back up.

## Freshness

Food has an expiry date: Fresh, then Still good, then Stale, then Spoiled (moldy). Stale food gives Hunger. Spoiled food poisons you.

To keep food longer, hold it in your offhand and use one of these:
- **Wax Paper**: 2x longer
- **Frost Wrap**: 3x longer
- **Glass Jar**: 5x longer (fruit, cream, petals, butter)

Jams and candied petals never go bad.

## Drinks

Matcha latte, hot chocolate, strawberry milk, lavender tea, rose tea and starlight tea. Each gives effects, and mugs can be placed on tables.

## Your café

Place a **Bakery Counter**, then lay the way in with a **Path Ribbon**: click the floor at your door (the entrance), then at each turn, then click the counter to tie the path to it. The counter won't open without one. Customers walk in along the path, line up on its last stretch in front of the counter, move up as you serve, and walk back out the same way. Hold the ribbon to see your paths.

Cakes are decorated on the **Cake Turntable**: smooth round, rounded-square or heart tiers, piped dots, rosettes and drips. Hold a topper to see a preview of where it lands; toppers go on top of a tier (facing you) or pressed onto its side, and the Sweetheart Spatula turns them.

The **Chocolate Fountain** is a single block you place anywhere. **Couches** placed side by side join into one long sofa.

## Villager reviews

Sneak + right-click a villager while holding a bake. They taste it and give it 1 to 5 stars, based on quality, freshness and whether it is their favorite flavor (they hint at it). Good reviews pay emeralds and improve prices. Check your rank with `/bakery stats`, from Apprentice Baker up to Star Patissier.

Villagers can also become **Bakers** by working at a Brick Oven.

## Development

- Build: `gradlew build` (the jar ends up in `build/libs`)
- Every texture and block model was painted and built in Blockbench through the Blockbench MCP. The generator scripts are in `blockbench/`.
- Round models (fountain, couch, jam pot, juicer, millstone, catalog stand, flower pots) come from `blockbench/round_model.py` and the `gen_*.py` scripts; 32x32 materials from `blockbench/gen_textures.py`; `blockbench/upscale_epx.py` doubles any texture still at 16x16.
- `./gradlew runGametest` plays through every feature on a headless server.
