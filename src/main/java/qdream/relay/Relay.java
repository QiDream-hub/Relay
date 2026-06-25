package qdream.relay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qdream.relay.Component.RelayDataComponents;
import qdream.relay.blocks.RelayBlocks;
import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.commands.RelayCommands;
import qdream.relay.entities.RelayEntityTypes;
import qdream.relay.items.RelayItems;
import qdream.relay.mc.RelayOperations;
import qdream.relay.networking.RelayServerNetworking;

public class Relay implements ModInitializer {
	public static final String MOD_ID = "relay";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// ✅ 全局 Server 引用
	private static MinecraftServer SERVER_INSTANCE = null;

	// ✅ 便捷方法：获取 Server
	public static MinecraftServer getServer() {
		if (SERVER_INSTANCE == null) {
			throw new IllegalStateException("Server is not initialized yet!");
		}
		return SERVER_INSTANCE;
	}

	// ✅ 便捷方法：根据 ID 获取世界
	public static ServerLevel getWorld(String worldId) {
		// 解析ID
		Identifier id = Identifier.tryParse(worldId);
		if (id == null) {
			// ID格式无效
			return null;
		}

		// 构建ResourceKey
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);

		// 获取世界
		ServerLevel level = SERVER_INSTANCE.getLevel(key);

		if (level == null) {
			// 该维度未加载或不存在
			return null;
		}
		return level;
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Relay Mod");

		// 注册操作和数据类型
		RelayOperations.register();

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

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			SERVER_INSTANCE = server;
			LOGGER.info("Server started! Server reference captured.");
		});

		LOGGER.info("Relay Mod initialized!");
	}
}