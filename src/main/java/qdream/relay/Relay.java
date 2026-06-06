package qdream.relay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qdream.relay.blocks.RelayBlocks;
import qdream.relay.blocks.RelayBlockEntities;
import qdream.relay.items.RelayItems;
import qdream.relay.items.RelayDataComponents;
import qdream.relay.networking.RelayServerNetworking;
import qdream.relay.entities.RelayEntityTypes;
import qdream.relay.commands.RelayCommands;

public class Relay implements ModInitializer {
	public static final String MOD_ID = "relay";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Relay Mod");

		// 注册方块和方块实体
		RelayBlocks.register();
		RelayBlockEntities.register();

		// 注册物品
		RelayItems.register();

		// 注册自定义 DataComponent
		RelayDataComponents.register();

		// 注册实体
		RelayEntityTypes.register();

		// 注册网络
		RelayServerNetworking.register();

		// 注册命令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			RelayCommands.register(dispatcher);
		});

		LOGGER.info("Relay Mod initialized!");
	}
}