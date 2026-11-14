package com.bakery;

import com.bakery.block.PlacedFoodBlock;
import com.bakery.block.PlacedFoodBlockEntity;
import com.bakery.cafe.CustomerEntity;
import com.bakery.deco.DecoBlock;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import com.bakery.shop.CounterBlock;
import com.bakery.shop.CounterBlockEntity;
import com.bakery.sweet.CakeDesign;
import com.bakery.sweet.CakeGeometry;
import com.bakery.sweet.Icing;
import com.bakery.sweet.TurntableBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a showroom of everything in the mod at a spot (operators only, /bakery demo): every kitchen machine
 * mid-use, a hand-decorated cake, all the decor and a little café with customers. Handy for screenshots.
 */
public final class BakeryDemo {
	private BakeryDemo() {
	}

	private static void set(ServerWorld world, BlockPos pos, BlockState state) {
		world.setBlockState(pos, state, Block.NOTIFY_ALL);
	}

	private static void nbt(ServerWorld world, BlockPos pos, NbtCompound nbt) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be == null) return;
		NbtCompound full = be.createNbt(world.getRegistryManager());
		full.copyFrom(nbt);
		be.read(full, world.getRegistryManager());
		be.markDirty();
		world.updateListeners(pos, be.getCachedState(), be.getCachedState(), Block.NOTIFY_ALL);
	}

	private static ItemStack flavored(net.minecraft.item.Item item, String flavor) {
		ItemStack stack = new ItemStack(item);
		stack.set(BakeryComponents.FLAVOR, flavor);
		return stack;
	}

	private static NbtCompound stack(ServerWorld world, ItemStack stack) {
		return (NbtCompound) stack.encode(world.getRegistryManager());
	}

	public static CakeDesign showCake() {
		int pink = Icing.PINK.color(), white = Icing.WHITE.color(), mint = Icing.MINT.color(), lilac = Icing.LILAC.color();
		// Every tier its own shape: a flower at the bottom, a star in the middle, a heart on top.
		int[] shapes = {CakeGeometry.FLOWER, CakeGeometry.STAR, CakeGeometry.HEART};
		List<CakeDesign.Dab> dabs = new ArrayList<>();
		int tiers = 3;
		for (int t = 0; t < tiers; t++) {
			float r = CakeGeometry.radius(t, tiers), top = CakeGeometry.top(t, tiers);
			for (float[] e : CakeGeometry.perimeter(shapes[t], r - 0.6f, 1.6f)) {
				dabs.add(new CakeDesign.Dab(CakeDesign.Dab.ROSETTE, t == 0 ? white : t == 1 ? pink : mint, Math.round(e[0] * 2), Math.round((top + 0.3f) * 2), Math.round(e[1] * 2)));
			}
			int i = 0;
			for (float[] e : CakeGeometry.perimeter(shapes[t], r + 0.25f, 1.3f)) {
				if (t == 1) dabs.add(new CakeDesign.Dab(CakeDesign.Dab.DRIP + (i * 7 % 4), lilac, Math.round(e[0] * 2), Math.round(top * 2), Math.round(e[1] * 2)));
				i++;
			}
			if (t == 0) {
				// A dotted belt round the side of the bottom tier.
				for (float[] e : CakeGeometry.perimeter(shapes[t], r, 1.8f)) {
					float out = CakeGeometry.edge(shapes[t], e[2], e[3], r) + 0.35f;
					dabs.add(new CakeDesign.Dab(CakeDesign.Dab.DOT, pink, Math.round(e[2] * out * 2), Math.round(1.2f * 2), Math.round(e[3] * out * 2)));
				}
			}
		}
		List<CakeDesign.Topper> toppers = new ArrayList<>();
		float top = CakeGeometry.top(2, tiers) * 2;
		toppers.add(new CakeDesign.Topper(BakeryItems.FONDANT_BUNNY, 0, Math.round(top), 0, 0));
		float mid = CakeGeometry.top(1, tiers) * 2, low = CakeGeometry.top(0, tiers) * 2;
		toppers.add(new CakeDesign.Topper(BakeryItems.SUGAR_DAISY, -6, Math.round(mid), 3, 0));
		toppers.add(new CakeDesign.Topper(BakeryItems.SUGAR_DAISY, 6, Math.round(mid), 3, 0));
		toppers.add(new CakeDesign.Topper(BakeryItems.SUGAR_STRAWBERRY, -9, Math.round(low), 6, 0));
		toppers.add(new CakeDesign.Topper(BakeryItems.SUGAR_STRAWBERRY, 9, Math.round(low), 6, 0));
		toppers.add(new CakeDesign.Topper(BakeryItems.FONDANT_HEART, 0, Math.round(low), 10, 0));
		toppers.add(new CakeDesign.Topper(BakeryItems.SUGAR_BOW, -6, Math.round(low), -8, 0));
		toppers.add(new CakeDesign.Topper(BakeryItems.SUGAR_BOW, 6, Math.round(low), -8, 0));
		// Decals pressed on the side of the bottom tier, facing out.
		float side = CakeGeometry.bottom(0, tiers) + 2.6f;
		float front = CakeGeometry.edge(shapes[0], 0, 1, CakeGeometry.radius(0, tiers)) + 0.35f;
		toppers.add(new CakeDesign.Topper(BakeryItems.SUGAR_RAINBOW, 0, Math.round(side * 2), Math.round(front * 2), 0));
		return new CakeDesign(shapes[0], List.of(pink, white, pink), dabs, toppers, List.of(shapes[0], shapes[1], shapes[2]));
	}

	public static void build(ServerWorld world, BlockPos o) {
		for (BlockPos p : BlockPos.iterate(o.add(-2, -1, -16), o.add(30, 8, 16))) {
			set(world, p, p.getY() == o.getY() - 1 ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.AIR.getDefaultState());
		}
		Direction s = Direction.SOUTH;
		// ---- row 1: kitchen machines (z = 0), facing south toward the camera
		BlockPos juicer = o;
		set(world, juicer, BakeryBlocks.JUICER.getDefaultState().with(DecoBlock.FACING, s));
		NbtCompound j = new NbtCompound();
		j.putString("flavor", "strawberry");
		j.putBoolean("loaded", true);
		j.putInt("presses", 2);
		j.putInt("juice", 2);
		j.put("fruit", stack(world, new ItemStack(BakeryItems.STRAWBERRY)));
		nbt(world, juicer, j);

		BlockPos churn = o.east(2);
		set(world, churn, BakeryBlocks.ICE_CREAM_CHURN.getDefaultState().with(DecoBlock.FACING, s));
		NbtCompound c = new NbtCompound();
		c.putBoolean("cream", true);
		c.putBoolean("sugar", true);
		c.putBoolean("ice", true);
		c.putString("flavor", "strawberry");
		c.putInt("churns", 7);
		nbt(world, churn, c);

		BlockPos pot = o.east(4);
		set(world, pot.down(), Blocks.CAMPFIRE.getDefaultState());
		set(world, pot, BakeryBlocks.JAM_POT.getDefaultState().with(DecoBlock.FACING, s));
		NbtCompound jam = new NbtCompound();
		jam.putString("flavor", "blueberry");
		jam.putInt("fruit", 3);
		jam.putBoolean("sugar", true);
		jam.putInt("state", 2);
		jam.putInt("cooked", 250);
		jam.put("fruitType", stack(world, new ItemStack(BakeryItems.BLUEBERRY)));
		nbt(world, pot, jam);

		BlockPos basket = o.east(6);
		set(world, basket, BakeryBlocks.PROOFING_BASKET.getDefaultState().with(DecoBlock.FACING, s));
		NbtCompound pb = new NbtCompound();
		pb.put("dough", stack(world, new ItemStack(BakeryItems.BREAD_DOUGH)));
		pb.putLong("placedAt", world.getTime() - 400);
		nbt(world, basket, pb);

		BlockPos bowl = o.east(8);
		set(world, bowl, BakeryBlocks.MIXING_BOWL.getDefaultState());
		NbtCompound bw = new NbtCompound();
		NbtList contents = new NbtList();
		for (ItemStack in : new ItemStack[]{new ItemStack(BakeryItems.FLOUR), new ItemStack(Items.EGG), new ItemStack(Items.SUGAR),
				new ItemStack(BakeryItems.BUTTER), new ItemStack(BakeryItems.STRAWBERRY)}) contents.add(stack(world, in));
		bw.put("contents", contents);
		bw.putInt("stirs", 3);
		nbt(world, bowl, bw);

		BlockPos board = o.east(10);
		set(world, board, BakeryBlocks.PASTRY_BOARD.getDefaultState());
		NbtCompound bd = new NbtCompound();
		ItemStack dough = new ItemStack(BakeryItems.SWEET_DOUGH);
		dough.set(BakeryComponents.FLAVOR, "matcha");
		bd.put("dough", stack(world, dough));
		nbt(world, board, bd);

		BlockPos table = o.east(12);
		set(world, table, BakeryBlocks.CAKE_TURNTABLE.getDefaultState());
		if (world.getBlockEntity(table) instanceof TurntableBlockEntity turntable) {
			ItemStack cake = new ItemStack(BakeryItems.CAKE);
			cake.set(BakeryComponents.FLAVOR, "strawberry");
			cake.set(BakeryComponents.CAKE_DESIGN, showCake());
			turntable.setCake(cake);
		}
		BlockPos plated = o.east(14);
		set(world, plated.down(), Blocks.SMOOTH_QUARTZ.getDefaultState());
		ItemStack simple = new ItemStack(BakeryItems.CAKE);
		simple.set(BakeryComponents.FLAVOR, "chocolate");
		List<CakeDesign.Dab> dabs = new ArrayList<>();
		for (float[] e : CakeGeometry.perimeter(CakeGeometry.SQUARE, 4.5f - 0.6f, 1.6f)) {
			dabs.add(new CakeDesign.Dab(CakeDesign.Dab.ROSETTE, Icing.WHITE.color(), Math.round(e[0] * 2), Math.round((CakeGeometry.top(1, 2) + 0.3f) * 2), Math.round(e[1] * 2)));
		}
		simple.set(BakeryComponents.CAKE_DESIGN, new CakeDesign(CakeGeometry.SQUARE, List.of(Icing.MINT.color(), -1), dabs,
				List.of(new CakeDesign.Topper(BakeryItems.GOLDEN_CROWN, 0, Math.round(CakeGeometry.top(1, 2) * 2), 0, 0))));
		set(world, plated, PlacedFoodBlock.withStackProperties(BakeryBlocks.CAKE.getDefaultState().with(PlacedFoodBlock.FACING, s), simple));
		if (world.getBlockEntity(plated) instanceof PlacedFoodBlockEntity food) food.setStack(simple);

		// ---- row 2: big machines (z = -6), controllers only
		BlockPos oven = o.add(2, 0, -6);
		set(world, oven, BakeryBlocks.GRAND_OVEN.getDefaultState().with(DecoBlock.FACING, s).with(com.bakery.oven.GrandOvenBlock.LIT, true));
		NbtCompound ov = new NbtCompound();
		ov.putInt("fuel", 20000);
		ov.putFloat("heat", 62);
		// Door down and the rack pulled out: a pie (2x2), a loaf (1x2) and a tray of cookies waiting to be flipped.
		net.minecraft.nbt.NbtList bakes = new net.minecraft.nbt.NbtList();
		Object[][] onRack = {{BakeryItems.RAW_PIE, 0, 0, 2, 2, 60000}, {BakeryItems.RAW_LOAF, 2, 0, 1, 2, 54000},
				{BakeryItems.RAW_COOKIES, 2, 2, 1, 1, 15000}, {BakeryItems.RAW_CUPCAKE, 0, 2, 1, 1, 38000}, {BakeryItems.RAW_SUGAR_COOKIE, 1, 2, 1, 1, 5000}};
		for (Object[] b : onRack) {
			NbtCompound bake = new NbtCompound();
			bake.put("item", stack(world, new ItemStack((net.minecraft.item.Item) b[0])));
			bake.putIntArray("cell", new int[]{(int) b[1], (int) b[2], (int) b[3], (int) b[4]});
			bake.putInt("progress", (int) b[5]);
			bakes.add(bake);
		}
		ov.put("bakes", bakes);
		ov.putBoolean("doorOpen", true);
		ov.putBoolean("rackOut", true);
		nbt(world, oven, ov);
		set(world, oven.offset(s, 2), BakeryBlocks.OVEN_RACK.getDefaultState().with(com.bakery.oven.OvenRackBlock.FACING, s));
		BlockPos mill = o.add(8, 0, -6);
		set(world, mill, BakeryBlocks.GRAND_MILLSTONE.getDefaultState().with(DecoBlock.FACING, s));
		NbtCompound ml = new NbtCompound();
		ml.put("input", stack(world, new ItemStack(Items.WHEAT, 16)));
		ml.putFloat("angle", 30);
		ml.putFloat("prevAngle", 30);
		nbt(world, mill, ml);
		BlockPos fountain = o.add(14, 0, -6);
		set(world, fountain, BakeryBlocks.CHOCOLATE_FOUNTAIN.getDefaultState().with(DecoBlock.FACING, s));
		NbtCompound fn = new NbtCompound();
		fn.putInt("chocolate", 12);
		nbt(world, fountain, fn);

		// ---- row 3: decor (z = 4 and 6)
		Block[] decor = BakeryBlocks.COZY_DECOR;
		for (int i = 0; i < decor.length; i++) {
			BlockPos p = o.add((i % 13) * 2, 0, 4 + (i / 13) * 3);
			BlockState st = decor[i].getDefaultState();
			if (st.contains(DecoBlock.FACING)) st = st.with(DecoBlock.FACING, s);
			if (decor[i] == BakeryBlocks.HEART_MIRROR || decor[i] == BakeryBlocks.NEON_HEART_SIGN || decor[i] == BakeryBlocks.LACE_CURTAINS
					|| decor[i] == BakeryBlocks.STAR_LIGHTS || decor[i] == BakeryBlocks.CAKE_SHELF || decor[i] == BakeryBlocks.DONUT_WALL
					|| decor[i] == BakeryBlocks.OPEN_SIGN || decor[i] == BakeryBlocks.FLOWER_BOX) {
				set(world, p.north(), BakeryBlocks.BOW_WALLPAPER.getDefaultState());
			}
			set(world, p, st);
		}
		for (int i = 0; i < 6; i++) {
			set(world, o.add(i, 0, 11), BakeryBlocks.PINK_PICKET_FENCE.getDefaultState()
					.with(net.minecraft.block.HorizontalConnectingBlock.EAST, i < 5).with(net.minecraft.block.HorizontalConnectingBlock.WEST, i > 0));
		}

		// ---- a little café (x = 20..28, z = -6..0) with customers
		BlockPos counter = o.add(24, 0, -10);
		set(world, counter, BakeryBlocks.BAKERY_COUNTER.getDefaultState().with(CounterBlock.FACING, s).with(CounterBlock.OPEN, true));
		set(world, o.add(21, 0, -6), BakeryBlocks.CAFE_TABLE.getDefaultState());
		set(world, o.add(21, 0, -5), BakeryBlocks.CAFE_CHAIR.getDefaultState().with(DecoBlock.FACING, Direction.NORTH));
		set(world, o.add(27, 0, -6), BakeryBlocks.PARASOL_TABLE.getDefaultState());
		for (int i = 0; i < 3; i++) {
			set(world, o.add(26 + i, 0, -3), BakeryBlocks.PINK_COUCH.getDefaultState().with(DecoBlock.FACING, Direction.NORTH));
		}
		for (int i = 0; i < 3; i++) {
			BlockPos p = o.add(26 + i, 0, -3);
			set(world, p, Block.postProcessState(world.getBlockState(p), world, p));
		}
		if (world.getBlockEntity(counter) instanceof CounterBlockEntity shop) {
			// The walking path: in at the front (z = 2), up to the counter.
			shop.setPath(List.of(o.add(22, 0, 2), o.add(24, 0, 0), o.add(24, 0, -9)));
			for (int i = 0; i < 2; i++) {
				if (shop.callCustomer()) {
					CustomerEntity customer = world.getEntitiesByClass(CustomerEntity.class, new net.minecraft.util.math.Box(counter).expand(40),
							e -> e.spot() != null && counter.equals(e.counter()) && e.state() == CustomerEntity.ARRIVING).stream().findFirst().orElse(null);
					if (customer != null) customer.refreshPositionAndAngles(customer.spot().getX() + 0.5, customer.spot().getY(), customer.spot().getZ() + 0.5, 0, 0);
				}
			}
		}
		// ---- crates and placed treats (z = 9)
		Block[] crates = {BakeryBlocks.STRAWBERRY_CRATE, BakeryBlocks.LEMON_CRATE, BakeryBlocks.COCONUT_CRATE};
		for (int i = 0; i < crates.length; i++) set(world, o.add(14 + i * 2, 0, 9), crates[i].getDefaultState().with(DecoBlock.FACING, s));
		BlockState cupcakes = PlacedFoodBlock.withStackProperties(BakeryBlocks.CUPCAKE.getDefaultState().with(PlacedFoodBlock.FACING, s),
				flavored(BakeryItems.CUPCAKE, "strawberry"));
		if (cupcakes.contains(PlacedFoodBlock.COUNT)) cupcakes = cupcakes.with(PlacedFoodBlock.COUNT, 4);
		set(world, o.add(20, 0, 9), cupcakes);
		set(world, o.add(22, 0, 9), PlacedFoodBlock.withStackProperties(BakeryBlocks.DONUT.getDefaultState().with(PlacedFoodBlock.FACING, s), flavored(BakeryItems.DONUT, "chocolate")));
		set(world, o.add(24, 0, 9), PlacedFoodBlock.withStackProperties(BakeryBlocks.MACARONS.getDefaultState().with(PlacedFoodBlock.FACING, s), flavored(BakeryItems.MACARONS, "lavender")));

		// Anything that spilled while the area was cleared goes away.
		world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, new net.minecraft.util.math.Box(o).expand(40), e -> true).forEach(net.minecraft.entity.Entity::discard);
		CustomerEntity diner = CustomerEntity.create(world, counter, o.add(21, 0, -4), com.bakery.cafe.Regular.get(5));
		diner.refreshPositionAndAngles(o.getX() + 21.5, o.getY(), o.getZ() - 3.5, 0, 0);
		world.spawnEntity(diner);
		diner.demoSit(world, o.add(21, 0, -5), new ItemStack(BakeryItems.CUPCAKE));
		CustomerEntity diner2 = CustomerEntity.create(world, counter, o.add(28, 0, -5), com.bakery.cafe.Regular.get(2));
		diner2.refreshPositionAndAngles(o.getX() + 28.5, o.getY(), o.getZ() - 4.5, 0, 0);
		world.spawnEntity(diner2);
		diner2.demoSit(world, o.add(28, 0, -6), new ItemStack(BakeryItems.HOT_CHOCOLATE));
	}
}
