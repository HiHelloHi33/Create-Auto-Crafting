package net.hihellohi.createautocrafting.client.ponder;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.PonderHilo;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class CACScenes {
    /** Position along the chain that frogport tongues reach to (and where we park the package to be grabbed). */
    private static final float FROG_CHAIN_POS = 1.6f;

    public static void coilOne(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("coil_scene", "Logic Coils");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(2,1,2, 2,2,2), Direction.DOWN);
        scene.idle(20);
        scene.addKeyframe();
        scene.overlay().showText(20)
                .text("This is a logic coil")
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(30);

        scene.overlay().showText(40)
                .text("Logic coils are essential for an autocrafting system")
                .pointAt(util.vector().centerOf(util.grid().at(2, 3, 2)))
                .placeNearTarget();
        scene.addKeyframe();
        scene.idle(40);
        scene.world().hideSection(util.select().fromTo(2,1,2, 2,2,2), Direction.UP);
        scene.idle(20);
        scene.world().showSection(util.select().fromTo(2,1,2 , 3,3,3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(60)
            .text("Stack Logic Coils into a multiblock to power autocrafting, with more coils allowing more crafting steps")
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(util.grid().at(2, 3, 2)))
                .placeNearTarget();
        scene.idle(70);
        scene.addKeyframe();
        scene.world().showSection(util.select().position(1,1,2), Direction.DOWN);
        scene.overlay().showText(60)
                .text("To complete the setup, link the coil to a network with a stock link")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(util.grid().at(2, 0, 3)))
                .placeNearTarget();
        scene.idle(60);
        scene.markAsFinished();
    }

    /*public static void coilTwo(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("coil_2", "Logic Coils");
        scene.configureBasePlate(0, 0, 8);
        scene.showBasePlate();
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        // Real cardboard packages: an Oak Log in (addressed to the crafter) and the crafted Oak Planks back out.
        ItemStack log = new ItemStack(Items.OAK_LOG);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 4);
        ItemStack logBox = PackageItem.containing(List.of(log.copy()));
        PackageItem.addAddress(logBox, "Crafting");
        ItemStack planksBox = PackageItem.containing(List.of(planks.copy()));

        // Key positions in the 8x5x8 layout (matches the mechanical-crafter build in coil_2.nbt).
        BlockPos ticker = util.grid().at(5, 1, 2);          // stock ticker, facing west
        BlockPos coil = util.grid().at(5, 2, 5);            // centre of the coil multiblock
        BlockPos conveyorA = util.grid().at(6, 4, 2);       // chain conveyor serving the crafter side
        BlockPos conveyorB = util.grid().at(2, 4, 6);       // chain conveyor serving the vault + crafter-out
        BlockPos inFrog = util.grid().at(2, 2, 1);          // frogport feeding the crafter
        BlockPos inPackager = util.grid().at(2, 1, 1);      // packager that unpacks the log into the crafter
        BlockPos crafter = util.grid().at(2, 1, 2);         // mechanical crafter (faces west, cog-driven)
        BlockPos outPackager = util.grid().at(2, 2, 3);     // packager that bundles the finished planks
        BlockPos outFrog = util.grid().at(2, 3, 3);         // frogport sending the planks onward
        BlockPos vaultFrog = util.grid().at(2, 3, 5);       // vault's frogport
        BlockPos vaultPackager = util.grid().at(2, 2, 5);   // vault's packager (faces east)
        BlockPos itemVault = util.grid().at(1, 1, 5);       // the item vault itself

        // Chain-link offsets between the two connected conveyors (used to aim the frogport grab animation).
        BlockPos connA = new BlockPos(-4, 0, 4);   // conveyorA -> conveyorB
        BlockPos connB = new BlockPos(4, 0, -4);   // conveyorB -> conveyorA

        // Keep the chain conveyors turning so packages ride them. Hold the crafter's driveline at zero so it does
        // NOT run a live craft cycle (that's what spat out the null/missing-item particles) — we show the craft
        // with insertItem + setCraftingResult instead.
        scene.world().setKineticSpeed(util.select().position(conveyorA), 0f);
        scene.world().setKineticSpeed(util.select().position(conveyorB), 0f);
        scene.world().setKineticSpeed(util.select().fromTo(2, 0, 2, 2, 1, 2), 0f);

        // Start from a clean slate so leftover packages can't accumulate across scene replays.
        conveyorClear(scene, conveyorA);
        conveyorClear(scene, conveyorB);

        // text_1 — request Oak Planks at the ticker. Show the icon first, hold a beat, then the caption.
        scene.addKeyframe();
        scene.overlay().showControls(util.vector().blockSurface(ticker, Direction.WEST), Pointing.RIGHT, 80)
                .withItem(planks)
                .rightClick();
        scene.idle(25);
        scene.overlay().showText(60)
                .text("Right-click the Stock Ticker to request Oak Planks")
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(ticker))
                .placeNearTarget();
        scene.idle(75);

        // text_2 — the request reaches the coils, which schedule the steps and gather ingredients.
        scene.addKeyframe();
        scene.overlay().showLine(PonderPalette.RED, util.vector().centerOf(ticker), util.vector().centerOf(coil), 70);
        scene.effects().indicateRedstone(coil);
        scene.idle(15);
        scene.overlay().showText(60)
                .text("The Logic Coils schedule the crafting steps and gather the ingredients")
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(coil))
                .placeNearTarget();
        scene.idle(70);

        // text_3 — the Vault releases the raw Oak Log and packages it up.
        PonderHilo.packagerCreate(scene, vaultPackager, logBox);
        scene.idle(20);
        scene.overlay().showText(70)
                .text("The Vault packages up a raw Oak Log")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(vaultPackager))
                .placeNearTarget();
        scene.idle(80);

        // text_4 — the vault frogport grabs the package off the packager and throws it onto the chain conveyor;
        // the crafter's port then catches it off the conveyor and unpacks it. We clear both conveyors around the
        // frog grabs so the frogport's own export side-effect can't pile packages up — exactly one rides at a time.
        frogThrow(scene, vaultFrog, conveyorB, connB, logBox);
        PonderHilo.packagerClear(scene, vaultPackager);   // the package has left the packager
        conveyorClear(scene, conveyorA);                  // same tick: wipe the frog's export before it renders
        conveyorClear(scene, conveyorB);
        conveyorPlace(scene, conveyorA, connA, logBox);   // the single package, parked where the crafter frog grabs
        scene.idle(8);
        scene.overlay().showText(70)
                .text("A Frogport sends it along the Chain Conveyor to the Crafter")
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(conveyorA))
                .placeNearTarget();
        scene.idle(70);
        frogCatch(scene, inFrog, conveyorA, connA, logBox);
        conveyorClear(scene, conveyorA);
        conveyorClear(scene, conveyorB);
        PonderHilo.packagerUnpack(scene, inPackager, logBox);
        scene.idle(20);

        // text_5 — the Mechanical Crafter turns the Oak Log into Oak Planks.
        scene.world().modifyBlockEntity(crafter, MechanicalCrafterBlockEntity.class,
                be -> be.getInventory().insertItem(0, log.copy(), false));
        scene.idle(20);
        scene.world().setCraftingResult(crafter, planks.copy());
        scene.effects().indicateSuccess(crafter);
        scene.idle(15);
        scene.overlay().showText(70)
                .text("The Mechanical Crafter turns the Oak Log into Oak Planks")
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(crafter))
                .placeNearTarget();
        scene.idle(80);

        // text_6 — the planks are packaged, the frogport takes them off the packager and onto the chain conveyor,
        // and the vault's port catches and stores them.
        PonderHilo.packagerCreate(scene, outPackager, planksBox);
        scene.idle(15);
        frogThrow(scene, outFrog, conveyorB, connB, planksBox);
        PonderHilo.packagerClear(scene, outPackager);   // the package has left the packager
        conveyorClear(scene, conveyorA);                // same tick: wipe the frog's export before it renders
        conveyorClear(scene, conveyorB);
        conveyorPlace(scene, conveyorB, connB, planksBox); // the single package, parked where the vault frog grabs
        scene.idle(8);
        scene.overlay().showText(70)
                .text("The Oak Planks are packaged and stored back in the Vault")
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(conveyorB))
                .placeNearTarget();
        scene.idle(70);
        frogCatch(scene, vaultFrog, conveyorB, connB, planksBox);
        conveyorClear(scene, conveyorA);
        conveyorClear(scene, conveyorB);
        PonderHilo.packagerUnpack(scene, vaultPackager, planksBox);
        scene.effects().indicateSuccess(itemVault);
        scene.idle(40);
        scene.markAsFinished();
    }*/

    /** Frogport throw: it reaches down to its packager, grabs the package, and tosses it onto the chain conveyor. */
    private static void frogThrow(CreateSceneBuilder scene, BlockPos frog, BlockPos conveyor, BlockPos connection,
                                  ItemStack pkg) {
        frogAnimate(scene, frog, conveyor, connection, pkg, true);
    }

    /** Frogport catch: it reaches up to the chain conveyor, grabs the travelling package, and drops it in. */
    private static void frogCatch(CreateSceneBuilder scene, BlockPos frog, BlockPos conveyor, BlockPos connection,
                                  ItemStack pkg) {
        frogAnimate(scene, frog, conveyor, connection, pkg, false);
    }

    /**
     * Plays a frogport's grab animation. The build leaves most frogports unlinked (no {@code target}), which would
     * NPE inside {@code startAnimation}; so for those we synthesise a chain-conveyor target aimed at the connected
     * conveyor. The whole thing is guarded — a frogport that still can't resolve a target simply skips its animation
     * instead of crashing the scene.
     */
    private static void frogAnimate(CreateSceneBuilder scene, BlockPos frog, BlockPos conveyor, BlockPos connection,
                                    ItemStack pkg, boolean outbound) {
        scene.world().modifyBlockEntity(frog, FrogportBlockEntity.class, fp -> {
            try {
                if (fp.target == null) {
                    BlockPos relative = conveyor.subtract(fp.getBlockPos());
                    PackagePortTarget.ChainConveyorFrogportTarget target =
                            new PackagePortTarget.ChainConveyorFrogportTarget(relative, FROG_CHAIN_POS, connection);
                    target.setup(fp, fp.getLevel(), fp.getBlockPos());
                    fp.target = target;
                }
                fp.startAnimation(pkg.copy(), outbound);
            } catch (Exception ignored) {
                // A frogport that can't resolve its conveyor target just won't animate — never crash the scene.
            }
        });
    }

    /**
     * Places the package on the chain at exactly the spot the frogport's tongue reaches — same {@code chainPos} and
     * {@code connection} as {@link #frogAnimate}'s target — so the frog grabs it instead of snatching at empty chain
     * a few blocks away. The conveyors are held at zero speed so it stays put for the grab.
     */
    private static void conveyorPlace(CreateSceneBuilder scene, BlockPos conveyor, BlockPos connection, ItemStack pkg) {
        scene.world().modifyBlockEntity(conveyor, ChainConveyorBlockEntity.class,
                be -> be.addTravellingPackage(new ChainConveyorPackage(FROG_CHAIN_POS, pkg.copy()), connection));
    }

    /** Clears any packages off a conveyor once they've been caught, so nothing lingers. */
    private static void conveyorClear(CreateSceneBuilder scene, BlockPos conveyor) {
        scene.world().modifyBlockEntity(conveyor, ChainConveyorBlockEntity.class, be -> {
            be.getLoopingPackages().clear();
            be.getTravellingPackages().clear();
        });
    }
}
