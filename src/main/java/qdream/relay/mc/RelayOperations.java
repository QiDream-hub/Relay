package qdream.relay.mc;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.world.phys.Vec3;
import qdream.relay.operations.arithmetic.AddOp;
import qdream.relay.operations.arithmetic.DivOp;
import qdream.relay.operations.arithmetic.MulOp;
import qdream.relay.operations.arithmetic.SubOp;
import qdream.relay.operations.base.DupOp;
import qdream.relay.operations.base.PopOp;
import qdream.relay.operations.base.SwapOp;
import qdream.relay.operations.communication.PeekOp;
import qdream.relay.operations.communication.RecvOp;
import qdream.relay.operations.communication.SendOp;
import qdream.relay.operations.control.EvalOp;
import qdream.relay.operations.control.IfOp;
import qdream.relay.operations.control.StopOp;
import qdream.relay.operations.logic.AndOp;
import qdream.relay.operations.logic.EqOp;
import qdream.relay.operations.logic.GtOp;
import qdream.relay.operations.logic.LtOp;
import qdream.relay.operations.logic.NotOp;
import qdream.relay.operations.logic.OrOp;
import qdream.relay.operations.list.ListAppendOp;
import qdream.relay.operations.list.ListGetOp;
import qdream.relay.operations.list.ListLengthOp;
import qdream.relay.operations.list.ListSetOp;
import qdream.relay.types.BooleanIota;
import qdream.relay.types.EntityIota;
import qdream.relay.types.ListIota;
import qdream.relay.types.NullIota;
import qdream.relay.types.NumberIota;
import qdream.relay.types.StringIota;
import qdream.relay.types.VectorIota;

/**
 * 注册所有操作和数据类型
 */
public class RelayOperations {

    private RelayOperations() {}

    public static void register() {
        // ========== 注册数据类型 ==========
        registerDataTypes();

        // ========== 注册操作 ==========
        registerOperations();
    }

    private static void registerDataTypes() {
        // 基础类型 - 工厂方法创建默认值实例
        OperationRegistry.registerData("relay:number", () -> new NumberIota(0));
        OperationRegistry.registerData("relay:boolean", () -> new BooleanIota(false));
        OperationRegistry.registerData("relay:string", () -> new StringIota(""));
        OperationRegistry.registerData("relay:vector", () -> new VectorIota(new Vec3(0, 0, 0)));
        OperationRegistry.registerData("relay:entity", () -> new EntityIota(new UUID(0, 0)));
        OperationRegistry.registerData("relay:null", () -> NullIota.INSTANCE);
        OperationRegistry.registerData("relay:list", () -> new ListIota(new ArrayList<>()));
    }

    private static void registerOperations() {
        // 基础操作
        OperationRegistry.register("relay:pop", new PopOp());
        OperationRegistry.register("relay:dup", new DupOp());
        OperationRegistry.register("relay:swap", new SwapOp());

        // 算术操作
        OperationRegistry.register("relay:add", new AddOp());
        OperationRegistry.register("relay:sub", new SubOp());
        OperationRegistry.register("relay:mul", new MulOp());
        OperationRegistry.register("relay:div", new DivOp());

        // 逻辑操作
        OperationRegistry.register("relay:and", new AndOp());
        OperationRegistry.register("relay:or", new OrOp());
        OperationRegistry.register("relay:not", new NotOp());

        // 比较操作
        OperationRegistry.register("relay:eq", new EqOp());
        OperationRegistry.register("relay:lt", new LtOp());
        OperationRegistry.register("relay:gt", new GtOp());

        // 控制流
        OperationRegistry.register("relay:eval", new EvalOp());
        OperationRegistry.register("relay:if", new IfOp());
        OperationRegistry.register("relay:stop", new StopOp());

        // 通信操作
        OperationRegistry.register("relay:send", new SendOp());
        OperationRegistry.register("relay:recv", new RecvOp());
        OperationRegistry.register("relay:peek", new PeekOp());

        // 列表操作
        OperationRegistry.register("relay:list.append", new ListAppendOp());
        OperationRegistry.register("relay:list.get", new ListGetOp());
        OperationRegistry.register("relay:list.set", new ListSetOp());
        OperationRegistry.register("relay:list.length", new ListLengthOp());
    }
}
