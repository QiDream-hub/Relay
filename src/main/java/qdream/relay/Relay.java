package qdream.relay;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qdream.relay.blocks.RelayBlocks;
import qdream.relay.blocks.RelayBlockEntities;
import qdream.relay.items.RelayItems;
import qdream.relay.operations.OperationsInit;
import qdream.relay.networking.RelayServerNetworking;

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
		RelayBlocks.init();
		RelayBlockEntities.init();

		// 注册物品
		RelayItems.init();

		// 注册操作
		OperationsInit.init();

		// 注册网络
		RelayServerNetworking.init();

		LOGGER.info("Relay Mod initialized!");
	}
}