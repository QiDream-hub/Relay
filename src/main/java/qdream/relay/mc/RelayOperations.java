package qdream.relay.mc;

import net.minecraft.world.phys.Vec3;
import qdream.relay.operations.arithmetic.*;
import qdream.relay.operations.base.*;
import qdream.relay.operations.block.*;
import qdream.relay.operations.communication.*;
import qdream.relay.operations.container.*;
import qdream.relay.operations.control.*;
import qdream.relay.operations.entity.*;
import qdream.relay.operations.type.*;
import qdream.relay.operations.vector.*;
import qdream.relay.operations.logic.*;
import qdream.relay.operations.list.*;
import qdream.relay.operations.spawn.*;
import qdream.relay.operations.entity.*;
import qdream.relay.types.*;

import java.util.ArrayList;

/**
 * 注册所有操作和数据类型
 */
public class RelayOperations {

        private RelayOperations() {
        }

        public static void register() {
                // ========== 注册数据类型 ==========
                registerDataTypes();

                // ========== 注册操作 ==========
                registerOperations();
        }

        private static void registerDataTypes() {
                // 基础类型 - 工厂方法创建默认值实例
                OperationRegistry.register("relay:number",
                                new OperationRegistry.DataEntry(() -> new NumberData(0)));
                OperationRegistry.register("relay:boolean",
                                new OperationRegistry.DataEntry(() -> new BooleanData(false)));
                OperationRegistry.register("relay:string",
                                new OperationRegistry.DataEntry(() -> new StringData("")));
                OperationRegistry.register("relay:vector",
                                new OperationRegistry.DataEntry(() -> new VectorData(new Vec3(0, 0, 0))));
                OperationRegistry.register("relay:entity",
                                new OperationRegistry.DataEntry(() -> new EntityData(null, null, null)));
                OperationRegistry.register("relay:null",
                                new OperationRegistry.DataEntry(() -> NullData.INSTANCE));
                OperationRegistry.register("relay:list",
                                new OperationRegistry.DataEntry(() -> new ListData(new ArrayList<>())));
                OperationRegistry.register("relay:block_entity",
                                new OperationRegistry.DataEntry(() -> new BlockEntityData(null, null, null)));
                OperationRegistry.register("relay:block",
                                new OperationRegistry.DataEntry(() -> new BlockData(null, null, null)));
                OperationRegistry.register("relay:type",
                                new OperationRegistry.DataEntry(() -> new TypeData("")));
                OperationRegistry.register("relay:slot",
                                new OperationRegistry.DataEntry(() -> new SlotData(null, null, -1)));
        }

        private static void registerOperations() {
                // 基础操作
                OperationRegistry.register("relay:pop", new OperationRegistry.OpEntry(new Pop()));
                OperationRegistry.register("relay:dup", new OperationRegistry.OpEntry(new Dup()));
                OperationRegistry.register("relay:swap", new OperationRegistry.OpEntry(new Swap()));
                OperationRegistry.register("relay:batch_dup", new OperationRegistry.OpEntry(new BatchDup()));
                OperationRegistry.register("relay:move_to_top", new OperationRegistry.OpEntry(new MoveToTop()));
                OperationRegistry.register("relay:copy_to_top", new OperationRegistry.OpEntry(new CopyToTop()));
                OperationRegistry.register("relay:stack_rearrange",
                                new OperationRegistry.OpEntry(new StackRearrange()));
                OperationRegistry.register("relay:get_data_stack_length",
                                new OperationRegistry.OpEntry(new GetDataStackLength()));
                OperationRegistry.register("relay:get_program_stack_length",
                                new OperationRegistry.OpEntry(new GetProgramStackLength()));
                OperationRegistry.register("relay:get_world_interactor",
                                new OperationRegistry.OpEntry(new CheckWorldInteractor()));
                OperationRegistry.register("relay:get_self",
                                new OperationRegistry.OpEntry(new GetSelf()));
                OperationRegistry.register("relay:get_entity_pos",
                                new OperationRegistry.OpEntry(new GetEntityPos()));
                OperationRegistry.register("relay:get_entity_eye_pos",
                                new OperationRegistry.OpEntry(new GetEntityEyePos()));
                OperationRegistry.register("relay:get_owner",
                                new OperationRegistry.OpEntry(new GetOwner()));
                OperationRegistry.register("relay:is_player",
                                new OperationRegistry.OpEntry(new IsPlayer()));

                // 实体获取操作
                OperationRegistry.register("relay:get_block_entity",
                                new OperationRegistry.OpEntry(new GetBlockEntity()));
                OperationRegistry.register("relay:get_entity",
                                new OperationRegistry.OpEntry(new GetEntity()));
                OperationRegistry.register("relay:get_block",
                                new OperationRegistry.OpEntry(new GetBlock()));
                OperationRegistry.register("relay:scan_entities",
                                new OperationRegistry.OpEntry(new ScanEntities()));
                OperationRegistry.register("relay:pickup_item",
                                new OperationRegistry.OpEntry(new PickupItem()));

                // 类型操作
                OperationRegistry.register("relay:get_type",
                                new OperationRegistry.OpEntry(new GetType()));
                OperationRegistry.register("relay:to_string",
                                new OperationRegistry.OpEntry(new ToString()));
                OperationRegistry.register("relay:to_bool",
                                new OperationRegistry.OpEntry(new ToBoolean()));

                // 算术操作
                OperationRegistry.register("relay:add", new OperationRegistry.OpEntry(new Add()));
                OperationRegistry.register("relay:sub", new OperationRegistry.OpEntry(new Sub()));
                OperationRegistry.register("relay:mul", new OperationRegistry.OpEntry(new Mul()));
                OperationRegistry.register("relay:div", new OperationRegistry.OpEntry(new Div()));
                OperationRegistry.register("relay:mod", new OperationRegistry.OpEntry(new Mod()));
                OperationRegistry.register("relay:floor", new OperationRegistry.OpEntry(new Floor()));
                OperationRegistry.register("relay:ceil", new OperationRegistry.OpEntry(new Ceil()));

                // 逻辑操作
                OperationRegistry.register("relay:and", new OperationRegistry.OpEntry(new And()));
                OperationRegistry.register("relay:or", new OperationRegistry.OpEntry(new Or()));
                OperationRegistry.register("relay:not", new OperationRegistry.OpEntry(new Not()));

                // 比较操作
                OperationRegistry.register("relay:eq", new OperationRegistry.OpEntry(new Eq()));
                OperationRegistry.register("relay:lt", new OperationRegistry.OpEntry(new Lt()));
                OperationRegistry.register("relay:gt", new OperationRegistry.OpEntry(new Gt()));

                // 控制流
                OperationRegistry.register("relay:eval", new OperationRegistry.OpEntry(new Eval()));
                OperationRegistry.register("relay:if", new OperationRegistry.OpEntry(new If()));
                OperationRegistry.register("relay:for", new OperationRegistry.OpEntry(new For()));
                OperationRegistry.register("relay:while", new OperationRegistry.OpEntry(new While()));
                OperationRegistry.register("relay:stop", new OperationRegistry.OpEntry(new Stop()));

                // 通信操作
                OperationRegistry.register("relay:send", new OperationRegistry.OpEntry(new Send()));
                OperationRegistry.register("relay:recv", new OperationRegistry.OpEntry(new Recv()));
                OperationRegistry.register("relay:peek", new OperationRegistry.OpEntry(new Peek()));
                OperationRegistry.register("relay:send_message", new OperationRegistry.OpEntry(new SendMessage()));

                // 列表操作
                OperationRegistry.register("relay:list_append", new OperationRegistry.OpEntry(new ListAppend()));
                OperationRegistry.register("relay:list_add_unique",
                                new OperationRegistry.OpEntry(new ListAddUnique()));
                OperationRegistry.register("relay:list_get", new OperationRegistry.OpEntry(new ListGet()));
                OperationRegistry.register("relay:list_remove", new OperationRegistry.OpEntry(new ListRemove()));
                OperationRegistry.register("relay:list_set", new OperationRegistry.OpEntry(new ListSet()));
                OperationRegistry.register("relay:list_length", new OperationRegistry.OpEntry(new ListLength()));
                OperationRegistry.register("relay:list_creat", new OperationRegistry.OpEntry(new ListCreate()));
                OperationRegistry.register("relay:list_unpack", new OperationRegistry.OpEntry(new ListUnpack()));
                OperationRegistry.register("relay:list_uniq", new OperationRegistry.OpEntry(new ListUniq()));

                // 向量操作
                OperationRegistry.register("relay:vector_add", new OperationRegistry.OpEntry(new VectorAdd()));
                OperationRegistry.register("relay:vector_sub", new OperationRegistry.OpEntry(new VectorSub()));
                OperationRegistry.register("relay:vector_mul", new OperationRegistry.OpEntry(new VectorMul()));
                OperationRegistry.register("relay:vector_dot", new OperationRegistry.OpEntry(new VectorDot()));
                OperationRegistry.register("relay:vector_cross", new OperationRegistry.OpEntry(new VectorCross()));
                OperationRegistry.register("relay:vector_normalize",
                                new OperationRegistry.OpEntry(new VectorNormalize()));
                OperationRegistry.register("relay:vector_length", new OperationRegistry.OpEntry(new VectorLength()));
                OperationRegistry.register("relay:vector_distance",
                                new OperationRegistry.OpEntry(new VectorDistance()));
                OperationRegistry.register("relay:build_vector", new OperationRegistry.OpEntry(new BuildVector()));

                // 世界交互操作（需要世界交互器）
                OperationRegistry.register("relay:raycast", new OperationRegistry.OpEntry(new Raycast()));
                OperationRegistry.register("relay:detect_block", new OperationRegistry.OpEntry(new DetectBlock()));
                OperationRegistry.register("relay:detect_entity", new OperationRegistry.OpEntry(new DetectEntity()));
                OperationRegistry.register("relay:push_vector", new OperationRegistry.OpEntry(new PushVector()));
                OperationRegistry.register("relay:get_look_vector",
                                new OperationRegistry.OpEntry(new GetLookVector()));

                // 实体朝向操作
                OperationRegistry.register("relay:set_entity_look",
                                new OperationRegistry.OpEntry(new SetEntityLook()));

                // 挖掘方块操作
                OperationRegistry.register("relay:break_block", new OperationRegistry.OpEntry(new BreakBlock()));
                OperationRegistry.register("relay:break_block_fortune",
                                new OperationRegistry.OpEntry(new BreakBlockFortune()));
                OperationRegistry.register("relay:break_block_silk_touch",
                                new OperationRegistry.OpEntry(new BreakBlockSilkTouch()));

                // 放置方块
                OperationRegistry.register("relay:place_block", new OperationRegistry.OpEntry(new PlaceBlock()));

                // 视线追踪操作
                OperationRegistry.register("relay:entity_raycast",
                                new OperationRegistry.OpEntry(new EntityRaycast()));
                OperationRegistry.register("relay:block_raycast", new OperationRegistry.OpEntry(new BlockRaycast()));
                OperationRegistry.register("relay:block_face_raycast", new OperationRegistry.OpEntry(new BlockFaceRaycast()));

                // 容器操作
                OperationRegistry.register("relay:get_container_items",
                                new OperationRegistry.OpEntry(new GetContainerItems()));
                OperationRegistry.register("relay:move_items",
                                new OperationRegistry.OpEntry(new MoveItems()));
                OperationRegistry.register("relay:get_item_count",
                                new OperationRegistry.OpEntry(new GetItemCount()));
                OperationRegistry.register("relay:get_item_type",
                                new OperationRegistry.OpEntry(new GetItemType()));
                OperationRegistry.register("relay:drop_item",
                                new OperationRegistry.OpEntry(new DropItem()));

                // 生成实体操作
                OperationRegistry.register("relay:spawn_shell",
                                new OperationRegistry.OpEntry(new SpawnShell()));
                OperationRegistry.register("relay:remove_shell",
                                new OperationRegistry.OpEntry(new RemoveShell()));

                // EntityShell 控制操作
                OperationRegistry.register("relay:entity_shell_add_energy",
                                new OperationRegistry.OpEntry(new EntityShellAddEnergy()));
                OperationRegistry.register("relay:entity_shell_get_energy",
                                new OperationRegistry.OpEntry(new EntityShellGetEnergy()));
                OperationRegistry.register("relay:entity_shell_reset",
                                new OperationRegistry.OpEntry(new EntityShellReset()));
        }
}
