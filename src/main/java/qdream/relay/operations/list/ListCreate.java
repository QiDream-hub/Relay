package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.StackException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;

import java.util.ArrayList;
import java.util.List;

/**
 * List Create 操作 - 创建列表
 * 输入：size (数值), elements (任意数量的元素)
 * 输出：新列表
 */
public class ListCreate extends Instruction {
    public ListCreate() {
        super("relay:list_create", 1, 0.25, OperationSignature.builder()
                .consumesFromData("size", "relay:number")
                .consumesFromData("elements", "...any")
                .producesToData("result", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData sizeData = StackHelpers.popNumber(executor, id);

        int size = sizeData.asInt();
        if (executor.getDataStackSize() < size) {
            throw new StackException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorType.LIST_CREATE_SIZE_MISMATCH, size, executor.getDataStackSize()));
        }

        List<Executable> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(executor.popData());
        }
        executor.pushData(new ListData(list));
    }
}
