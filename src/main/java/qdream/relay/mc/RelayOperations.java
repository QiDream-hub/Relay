package qdream.relay.mc;

import net.minecraft.world.phys.Vec3;
import qdream.relay.operations.arithmetic.AddOp;
import qdream.relay.operations.arithmetic.DivOp;
import qdream.relay.operations.arithmetic.MulOp;
import qdream.relay.operations.arithmetic.SubOp;
import qdream.relay.operations.base.DupOp;
import qdream.relay.operations.base.GetWorldInteractorOp;
import qdream.relay.operations.base.PopOp;
import qdream.relay.operations.base.SwapOp;
import qdream.relay.operations.communication.PeekOp;
import qdream.relay.operations.communication.RecvOp;
import qdream.relay.operations.communication.SendOp;
import qdream.relay.operations.communication.SendMessageOp;
import qdream.relay.operations.control.EvalOp;
import qdream.relay.operations.control.IfOp;
import qdream.relay.operations.control.StopOp;
import qdream.relay.operations.entity.GetOwnerOp;
import qdream.relay.operations.entity.GetSelfOp;
import qdream.relay.operations.entity.IsPlayerOp;
import qdream.relay.operations.logic.AndOp;
import qdream.relay.operations.logic.EqOp;
import qdream.relay.operations.logic.GtOp;
import qdream.relay.operations.logic.LtOp;
import qdream.relay.operations.logic.NotOp;
import qdream.relay.operations.logic.OrOp;
import qdream.relay.operations.list.ListAppendOp;
import qdream.relay.operations.list.ListCreatOp;
import qdream.relay.operations.list.ListGetOp;
import qdream.relay.operations.list.ListLengthOp;
import qdream.relay.operations.list.ListSetOp;
import qdream.relay.types.BooleanIota;
import qdream.relay.types.BlockEntityIota;
import qdream.relay.types.EntityIota;
import qdream.relay.types.ListIota;
import qdream.relay.types.NullIota;
import qdream.relay.types.NumberIota;
import qdream.relay.types.StringIota;
import qdream.relay.types.VectorIota;

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
                new OperationRegistry.DataEntry(() -> new NumberIota(0)));
        OperationRegistry.register("relay:boolean",
                new OperationRegistry.DataEntry(() -> new BooleanIota(false)));
        OperationRegistry.register("relay:string",
                new OperationRegistry.DataEntry(() -> new StringIota("")));
        OperationRegistry.register("relay:vector",
                new OperationRegistry.DataEntry(() -> new VectorIota(new Vec3(0, 0, 0))));
        OperationRegistry.register("relay:entity",
                new OperationRegistry.DataEntry(() -> new EntityIota(null, null, null)));
        OperationRegistry.register("relay:null",
                new OperationRegistry.DataEntry(() -> NullIota.INSTANCE));
        OperationRegistry.register("relay:list",
                new OperationRegistry.DataEntry(() -> new ListIota(new ArrayList<>())));
        OperationRegistry.register("relay:block_entity",
                new OperationRegistry.DataEntry(() -> new BlockEntityIota(null, null, null)));
    }

    private static void registerOperations() {
        // 基础操作
        OperationRegistry.register("relay:pop", new OperationRegistry.OpEntry(new PopOp()));
        OperationRegistry.register("relay:dup", new OperationRegistry.OpEntry(new DupOp()));
        OperationRegistry.register("relay:swap", new OperationRegistry.OpEntry(new SwapOp()));
        OperationRegistry.register("relay:get_world_interactor",
                new OperationRegistry.OpEntry(new GetWorldInteractorOp()));
        OperationRegistry.register("relay:get_self",
                new OperationRegistry.OpEntry(new GetSelfOp()));
        OperationRegistry.register("relay:get_owner",
                new OperationRegistry.OpEntry(new GetOwnerOp()));
        OperationRegistry.register("relay:is_player",
                new OperationRegistry.OpEntry(new IsPlayerOp()));

        // 算术操作
        OperationRegistry.register("relay:add", new OperationRegistry.OpEntry(new AddOp()));
        OperationRegistry.register("relay:sub", new OperationRegistry.OpEntry(new SubOp()));
        OperationRegistry.register("relay:mul", new OperationRegistry.OpEntry(new MulOp()));
        OperationRegistry.register("relay:div", new OperationRegistry.OpEntry(new DivOp()));

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
        OperationRegistry.register("relay:stop", new OperationRegistry.OpEntry(new StopOp()));

        // 通信操作
        OperationRegistry.register("relay:send", new OperationRegistry.OpEntry(new SendOp()));
        OperationRegistry.register("relay:recv", new OperationRegistry.OpEntry(new RecvOp()));
        OperationRegistry.register("relay:peek", new OperationRegistry.OpEntry(new PeekOp()));
        OperationRegistry.register("relay:send_message", new OperationRegistry.OpEntry(new SendMessageOp()));

        // 列表操作
        OperationRegistry.register("relay:list_append", new OperationRegistry.OpEntry(new ListAppendOp()));
        OperationRegistry.register("relay:list_get", new OperationRegistry.OpEntry(new ListGetOp()));
        OperationRegistry.register("relay:list_set", new OperationRegistry.OpEntry(new ListSetOp()));
        OperationRegistry.register("relay:list_length", new OperationRegistry.OpEntry(new ListLengthOp()));
        OperationRegistry.register("relay:list_creat", new OperationRegistry.OpEntry(new ListCreatOp()));
    }
}
