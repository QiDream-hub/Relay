package qdream.relay.mc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import qdream.relay.operations.arithmetic.*;
import qdream.relay.operations.base.*;
import qdream.relay.operations.block.*;
import qdream.relay.operations.communication.*;
import qdream.relay.operations.container.*;
import qdream.relay.operations.control.*;
import qdream.relay.operations.disk.*;
import qdream.relay.operations.entity.*;
import qdream.relay.operations.type.*;
import qdream.relay.operations.vector.*;
import qdream.relay.operations.logic.*;
import qdream.relay.operations.list.*;
import qdream.relay.operations.stack.*;
import qdream.relay.operations.summon.display.*;
import qdream.relay.operations.summon.shell.*;
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
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new NumberData(0)));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new BooleanData(false)));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new StringData(Component.literal(""))));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new VectorData(new Vec3(0, 0, 0))));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new EntityData(null, null, null)));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> NullData.INSTANCE));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new ListData(new ArrayList<>())));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new BlockEntityData(null, null, null)));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new BlockData(null, null, null)));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new TypeData("")));
                OperationRegistry.register(
                                new OperationRegistry.DataEntry(() -> new SlotData(null, null, -1)));
        }

        private static void registerOperations() {
                // 基础操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new Pop()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Dup()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Swap()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new BatchDup()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new MoveToTop()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new CopyToTop()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new StackRearrange()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetDataStackLength()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetProgramStackLength()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new CheckWorldInteractor()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetSelf()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetPos()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetEntityEyePos()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetOwner()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new IsPlayer()));

                // 实体获取操作
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetBlockEntity()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetEntity()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetBlock()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new ScanEntities()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new PickupItem()));

                // 类型操作
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetType()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new ToString()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new ToBoolean()));

                // 算术操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new Add()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Sub()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Mul()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Div()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Mod()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Floor()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Ceil()));

                // 逻辑操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new And()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Or()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Not()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new IsPlayer()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new IsAnimal()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new IsItem()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new IsEntityShell()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new IsHostile()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new IsNeutral()));

                // 比较操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new Eq()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Lt()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Gt()));

                // 控制流
                OperationRegistry.register(new OperationRegistry.OpEntry(new Eval()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new If()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new For()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new While()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Stop()));

                // 通信操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new Send()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Recv()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new Peek()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new SendMessage()));

                // 列表操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new ListAppend()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new ListAddUnique()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new ListGet()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new ListRemove()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new ListSet()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new ListLength()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new ListCreate()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new ListUnpack()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new ListUniq()));

                // 向量操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new VectorAdd()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new VectorSub()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new VectorMul()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new VectorDot()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new VectorCross()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new VectorNormalize()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new VectorLength()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new VectorDistance()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new BuildVector()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new VectorSplit()));

                // 世界交互操作（需要世界交互器）
                OperationRegistry.register(new OperationRegistry.OpEntry(new DetectBlock()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new DetectEntity()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new PushVector()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetLookVector()));

                // 实体朝向操作
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SetEntityLook()));

                // 挖掘方块操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new BreakBlock()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new BreakBlockFortune()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new BreakBlockSilkTouch()));

                // 放置方块
                OperationRegistry.register(new OperationRegistry.OpEntry(new PlaceBlock()));

                // 右键交互操作
                OperationRegistry.register(new OperationRegistry.OpEntry(new RightClick()));

                // 视线追踪操作
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new EntityRaycast()));
                OperationRegistry.register(new OperationRegistry.OpEntry(new BlockRaycast()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new BlockFaceRaycast()));

                // 容器操作
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetContainerItems()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetSlotOp()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new MoveItems()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetItemCount()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new DropItem()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new ReadDisk()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new WriteDisk()));

                // Shell 控制操作
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SpawnShell()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new RemoveShell()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new AddShellEnergy()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new GetShellEnergy()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new AddShellEnergyFromSlot()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new EntityShellReset()));

                // StringDisplay 控制操作
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SpawnStringDisplay()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new RemoveStringDisplay()));

                // StringDisplay 配置操作
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SetStringDisplayText()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SetStringDisplayTextColor()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SetStringDisplayBackgroundColor()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SetStringDisplaySeeThrough()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SetStringDisplayTrackPlayer()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new SetStringDisplayRotation()));
                OperationRegistry.register(
                                new OperationRegistry.OpEntry(new AddStringDisplayEnergy()));
        }
}
