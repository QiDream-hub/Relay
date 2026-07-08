package qdream.relay.mc;

import net.minecraft.world.phys.Vec3;
import qdream.relay.operations.arithmetic.*;
import qdream.relay.operations.base.*;
import qdream.relay.operations.communication.*;
import qdream.relay.operations.container.*;
import qdream.relay.operations.control.*;
import qdream.relay.operations.entity.*;
import qdream.relay.operations.type.*;
import qdream.relay.operations.vector.*;
import qdream.relay.operations.logic.*;
import qdream.relay.operations.list.*;
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
                OperationRegistry.register("relay:pop", new OperationRegistry.OpEntry(new PopOp()));
                OperationRegistry.register("relay:dup", new OperationRegistry.OpEntry(new DupOp()));
                OperationRegistry.register("relay:swap", new OperationRegistry.OpEntry(new SwapOp()));
                OperationRegistry.register("relay:batch_dup", new OperationRegistry.OpEntry(new BatchDupOp()));
                OperationRegistry.register("relay:move_to_top", new OperationRegistry.OpEntry(new MoveToTopOp()));
                OperationRegistry.register("relay:copy_to_top", new OperationRegistry.OpEntry(new CopyToTopOp()));
                OperationRegistry.register("relay:stack_rearrange",
                                new OperationRegistry.OpEntry(new StackRearrangeOp()));
                OperationRegistry.register("relay:get_data_stack_length",
                                new OperationRegistry.OpEntry(new GetDataStackLengthOp()));
                OperationRegistry.register("relay:get_program_stack_length",
                                new OperationRegistry.OpEntry(new GetProgramStackLengthOp()));
                OperationRegistry.register("relay:get_world_interactor",
                                new OperationRegistry.OpEntry(new GetWorldInteractorOp()));
                OperationRegistry.register("relay:get_self",
                                new OperationRegistry.OpEntry(new GetSelfOp()));
                OperationRegistry.register("relay:get_entity_pos",
                                new OperationRegistry.OpEntry(new GetEntityPosOp()));
                OperationRegistry.register("relay:get_entity_eye_pos",
                                new OperationRegistry.OpEntry(new GetEntityEyePosOp()));
                OperationRegistry.register("relay:get_owner",
                                new OperationRegistry.OpEntry(new GetOwnerOp()));
                OperationRegistry.register("relay:is_player",
                                new OperationRegistry.OpEntry(new IsPlayerOp()));

                // 实体获取操作
                OperationRegistry.register("relay:get_block_entity",
                                new OperationRegistry.OpEntry(new GetBlockEntityOp()));
                OperationRegistry.register("relay:get_entity",
                                new OperationRegistry.OpEntry(new GetEntityOp()));
                OperationRegistry.register("relay:get_block",
                                new OperationRegistry.OpEntry(new GetBlockOp()));
                OperationRegistry.register("relay:scan_entities",
                                new OperationRegistry.OpEntry(new ScanEntitiesOp()));
                OperationRegistry.register("relay:pickup_item",
                                new OperationRegistry.OpEntry(new PickupItemOp()));

                // 类型操作
                OperationRegistry.register("relay:get_type",
                                new OperationRegistry.OpEntry(new GetTypeOp()));
                OperationRegistry.register("relay:to_string",
                                new OperationRegistry.OpEntry(new ToStringOp()));

                // 算术操作
                OperationRegistry.register("relay:add", new OperationRegistry.OpEntry(new AddOp()));
                OperationRegistry.register("relay:sub", new OperationRegistry.OpEntry(new SubOp()));
                OperationRegistry.register("relay:mul", new OperationRegistry.OpEntry(new MulOp()));
                OperationRegistry.register("relay:div", new OperationRegistry.OpEntry(new DivOp()));
                OperationRegistry.register("relay:mod", new OperationRegistry.OpEntry(new ModOp()));
                OperationRegistry.register("relay:floor", new OperationRegistry.OpEntry(new FloorOp()));
                OperationRegistry.register("relay:ceil", new OperationRegistry.OpEntry(new CeilOp()));

                // 逻辑操作
                OperationRegistry.register("relay:and", new OperationRegistry.OpEntry(new AndOp()));
                OperationRegistry.register("relay:or", new OperationRegistry.OpEntry(new OrOp()));
                OperationRegistry.register("relay:not", new OperationRegistry.OpEntry(new NotOp()));

                // 比较操作
                OperationRegistry.register("relay:eq", new OperationRegistry.OpEntry(new EqOp()));
                OperationRegistry.register("relay:lt", new OperationRegistry.OpEntry(new LtOp()));
                OperationRegistry.register("relay:gt", new OperationRegistry.OpEntry(new GtOp()));

                // 控制流
                OperationRegistry.register("relay:eval", new OperationRegistry.OpEntry(new EvalOp()));
                OperationRegistry.register("relay:if", new OperationRegistry.OpEntry(new IfOp()));
                OperationRegistry.register("relay:for", new OperationRegistry.OpEntry(new ForOp()));
                OperationRegistry.register("relay:while", new OperationRegistry.OpEntry(new WhileOp()));
                OperationRegistry.register("relay:stop", new OperationRegistry.OpEntry(new StopOp()));

                // 通信操作
                OperationRegistry.register("relay:send", new OperationRegistry.OpEntry(new SendOp()));
                OperationRegistry.register("relay:recv", new OperationRegistry.OpEntry(new RecvOp()));
                OperationRegistry.register("relay:peek", new OperationRegistry.OpEntry(new PeekOp()));
                OperationRegistry.register("relay:send_message", new OperationRegistry.OpEntry(new SendMessageOp()));

                // 列表操作
                OperationRegistry.register("relay:list_append", new OperationRegistry.OpEntry(new ListAppendOp()));
                OperationRegistry.register("relay:list_add_unique",
                                new OperationRegistry.OpEntry(new ListAddUniqueOp()));
                OperationRegistry.register("relay:list_get", new OperationRegistry.OpEntry(new ListGetOp()));
                OperationRegistry.register("relay:list_remove", new OperationRegistry.OpEntry(new ListRemoveOp()));
                OperationRegistry.register("relay:list_set", new OperationRegistry.OpEntry(new ListSetOp()));
                OperationRegistry.register("relay:list_length", new OperationRegistry.OpEntry(new ListLengthOp()));
                OperationRegistry.register("relay:list_creat", new OperationRegistry.OpEntry(new ListCreatOp()));
                OperationRegistry.register("relay:list_unpack", new OperationRegistry.OpEntry(new ListUnpackOp()));
                OperationRegistry.register("relay:list_uniq", new OperationRegistry.OpEntry(new ListUniqOp()));

                // 向量操作
                OperationRegistry.register("relay:vector_add", new OperationRegistry.OpEntry(new VectorAddOp()));
                OperationRegistry.register("relay:vector_sub", new OperationRegistry.OpEntry(new VectorSubOp()));
                OperationRegistry.register("relay:vector_mul", new OperationRegistry.OpEntry(new VectorMulOp()));
                OperationRegistry.register("relay:vector_dot", new OperationRegistry.OpEntry(new VectorDotOp()));
                OperationRegistry.register("relay:vector_cross", new OperationRegistry.OpEntry(new VectorCrossOp()));
                OperationRegistry.register("relay:vector_normalize",
                                new OperationRegistry.OpEntry(new VectorNormalizeOp()));
                OperationRegistry.register("relay:vector_length", new OperationRegistry.OpEntry(new VectorLengthOp()));
                OperationRegistry.register("relay:vector_distance",
                                new OperationRegistry.OpEntry(new VectorDistanceOp()));
                OperationRegistry.register("relay:build_vector", new OperationRegistry.OpEntry(new BuildVectorOp()));

                // 世界交互操作（需要世界交互器）
                OperationRegistry.register("relay:raycast", new OperationRegistry.OpEntry(new RaycastOp()));
                OperationRegistry.register("relay:detect_block", new OperationRegistry.OpEntry(new DetectBlockOp()));
                OperationRegistry.register("relay:detect_entity", new OperationRegistry.OpEntry(new DetectEntityOp()));
                OperationRegistry.register("relay:push_vector", new OperationRegistry.OpEntry(new PushVectorOp()));
                OperationRegistry.register("relay:get_look_vector",
                                new OperationRegistry.OpEntry(new GetLookVectorOp()));

                // 实体朝向操作
                OperationRegistry.register("relay:set_entity_look",
                                new OperationRegistry.OpEntry(new SetEntityLookOp()));

                // 挖掘方块操作
                OperationRegistry.register("relay:break_block", new OperationRegistry.OpEntry(new BreakBlockOp()));
                OperationRegistry.register("relay:break_block_fortune",
                                new OperationRegistry.OpEntry(new BreakBlockFortuneOp()));
                OperationRegistry.register("relay:break_block_silk_touch",
                                new OperationRegistry.OpEntry(new BreakBlockSilkTouchOp()));

                // 视线追踪操作
                OperationRegistry.register("relay:entity_raycast",
                                new OperationRegistry.OpEntry(new EntityRaycastOp()));
                OperationRegistry.register("relay:block_raycast", new OperationRegistry.OpEntry(new BlockRaycastOp()));

                // 容器操作
                OperationRegistry.register("relay:get_container_items",
                                new OperationRegistry.OpEntry(new GetContainerItemsOp()));
                OperationRegistry.register("relay:merge_items",
                                new OperationRegistry.OpEntry(new MergeItemsOp()));
                OperationRegistry.register("relay:get_item_count",
                                new OperationRegistry.OpEntry(new GetItemCountOp()));
                OperationRegistry.register("relay:drop_item",
                                new OperationRegistry.OpEntry(new DropItemOp()));
        }
}
