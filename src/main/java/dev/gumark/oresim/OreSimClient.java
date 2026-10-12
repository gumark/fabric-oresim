package dev.gumark.oresim;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.blaze3d.platform.InputConstants;
import dev.gumark.oresim.compat.Compat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * OreSim by gumark - predicts and renders ore locations from the world seed.
 *
 * <p>Toggle with the X key, set the seed with {@code /setoresimseed <seed>}.
 */
public class OreSimClient implements ClientModInitializer {
    public static final String MOD_ID = "oresim";

    private final OreSim oreSim = new OreSim();
    private KeyMapping toggleKey;
    private boolean openGuiQueued;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.oresim.toggle", Compat.keyboardType(), InputConstants.KEY_X, category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> oreSim.onChunkLoad(chunk));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> oreSim.onChunkUnload(chunk));
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> oreSim.render());

        // Seeds set with /setoresimseed are remembered per server address, so
        // rejoining the same server restores them without running the command
        // again (SeedStore resolves the seed lazily from ServerSeedMemory).
        // The seed is persisted here on disconnect and dropped, so it can
        // never carry over to a different server. Singleplayer reads the real
        // seed from the integrated server and is unaffected.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SeedStore.rememberCurrentServerSeed());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Long remembered = SeedStore.restoredSeedOrNull();
            if (remembered != null) {
                client.player.sendSystemMessage(Component.literal(
                    "OreSim: using remembered seed " + remembered + " for this server (change with /setoresimseed)"));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("oresim").executes(context -> {
                // Opening the screen from inside the chat submit path would be
                // undone by ChatScreen's own close sequence (it calls
                // gui.setScreen(null) right after the command returns), so
                // queue the screen for the next client tick instead.
                openGuiQueued = true;
                return 1;
            }));

            dispatcher.register(ClientCommands.literal("setoresimseed")
                .then(ClientCommands.argument("seed", StringArgumentType.greedyString())
                    .executes(context -> {
                        String input = StringArgumentType.getString(context, "seed");
                        long seed = SeedStore.parseSeed(input);
                        SeedStore.setSeed(seed);
                        String address = SeedStore.currentServerAddress();
                        if (address != null) {
                            context.getSource().sendFeedback(Component.literal(
                                "OreSim seed set to: " + seed + " (will be remembered for " + address + ")"));
                        } else {
                            context.getSource().sendFeedback(Component.literal(
                                "OreSim seed set to: " + seed + " (read from singleplayer world)"));
                        }
                        return 1;
                    }))
            );
        });
    }

    private void onTick() {
        if (openGuiQueued) {
            openGuiQueued = false;
            Minecraft.getInstance().setScreenAndShow(new OreSimScreen());
        }
        while (toggleKey.consumeClick()) {
            oreSim.toggle();
        }
        oreSim.tick();
    }
}
