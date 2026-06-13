package net.hihellohi.createautocrafting.client.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;

/**
 * Ponder scenes for this mod. Only the Logic Coil ships a scene ({@code coil_1}); the other items
 * use a Create-style "Press [Shift]" tooltip instead.
 *
 * <p>The second, frogport-driven scene ({@code coil_2}) from the 1.20.1 build was dropped during the
 * 1.21.1 port: it drove deep Create-internal frogport/chain-conveyor animation that is not part of
 * Create's public API and would need re-validation against each Create build. Re-add it later if the
 * full logistics walkthrough is wanted.
 */
public final class CACScenes {

    private CACScenes() {}

    public static void coilOne(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("coil_scene", "Logic Coils");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 2, 2, 2), Direction.DOWN);
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
        scene.world().hideSection(util.select().fromTo(2, 1, 2, 2, 2, 2), Direction.UP);
        scene.idle(20);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 3, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(60)
                .text("Stack Logic Coils into a multiblock to power autocrafting, with more coils allowing more crafting steps")
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(util.grid().at(2, 3, 2)))
                .placeNearTarget();
        scene.idle(70);
        scene.addKeyframe();
        scene.world().showSection(util.select().position(1, 1, 2), Direction.DOWN);
        scene.overlay().showText(60)
                .text("To complete the setup, link the coil to a network with a stock link")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(util.grid().at(2, 0, 3)))
                .placeNearTarget();
        scene.idle(60);
        scene.markAsFinished();
    }
}
