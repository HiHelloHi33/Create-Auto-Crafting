package net.hihellohi.createautocrafting.client.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;

/**
 * Ponder storyboards for this mod's items/blocks. Each method is a {@code PonderStoryBoard}
 * {@code (SceneBuilder, SceneBuildingUtil) -> void}, wrapped in Create's {@link CreateSceneBuilder}
 * for the richer instruction set. Each scene plays over a structure shipped at
 * {@code assets/createautocrafting/ponder/<path>.nbt} — those layouts are authored in-game (build
 * the contraption, then export it with Create's Ponder/structure tooling). Until an {@code .nbt}
 * exists, opening the scene shows an empty base plate; the code below still compiles and registers.
 *
 * <p>Positions/text here are sensible defaults to tune against the real structures.
 */
public final class CACScenes {

    private CACScenes() {}

    /** Crafting Blueprint: configure a 3x3 recipe + result, lock/unlock amounts. */
    public static void blueprint(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("blueprint", "Configuring a Crafting Blueprint");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Shift-right-click a Crafting Blueprint to open its recipe editor")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Fill the 3x3 grid and a result, then unlock to set ingredient amounts")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(70)
                .text("Insert the finished Blueprint into a Crafting Distributor to make its result craftable")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    /** Crafting Distributor: a Packager converted by the Crafter Kit; emits ingredients on request. */
    public static void distributor(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("distributor", "The Crafting Distributor");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("A Crafting Distributor holds Blueprints and links to the logistics network")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("When a craft is requested, it pulls the ingredients from the network and ships them here")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);
        scene.markAsFinished();
    }

    /** Logic Coil: the multiblock that provides crafting steps to a nearby network. */
    public static void logicCoil(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("logic_coil", "Logic Coils");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Stack Logic Coils into a prism to provide crafting steps")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Each coil adds steps; more coils let bigger crafting trees run at once")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(70)
                .text("Keep the prism within 32 blocks of a provider on the network")
                .colored(PonderPalette.MEDIUM)
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    /** Packager Crafter Kit: right-click a Packager to convert it into a Crafting Distributor. */
    public static void crafterKit(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("crafter_kit", "The Packager Crafter Kit");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Right-click a Packager with the Crafter Kit")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("This converts it into a Crafting Distributor, consuming the kit")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);
        scene.markAsFinished();
    }
}
