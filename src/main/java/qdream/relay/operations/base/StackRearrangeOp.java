package qdream.relay.operations.base;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 栈重排操作 - 从数据栈弹出元素后按索引列表重新排列
 * 
 * 签名:
 * indices,relay:list - 索引列表（从 1 开始计数）
 * amount,relay:number - 弹出数量
 * many,...any - 可变输入（从数据栈弹出的元素）
 * result,...any - 可变输出（重排后的结果）
 * 
 * 执行流程:
 * 1. 弹出 indices 列表
 * 2. 弹出 amount 数量
 * 3. 弹出 amount 个元素到临时数组（按弹出顺序）
 * 4. 根据 indices 从临时数组取元素（索引从 1 开始）
 * 5. 将结果压回数据栈
 */
public class StackRearrangeOp extends Spell {

    public StackRearrangeOp() {
        super("relay:stack_rearrange", 1, 1, OperationSignature.builder()
                .consumesFromData("indices", "relay:list")
                .consumesFromData("amount", "relay:number")
                .consumesFromData("many", "...any")
                .producesToData("result", "...any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 1. 弹出索引列表
        ListData indicesList = OperationHelpers.popList(executor, id);
        if (indicesList == null)
            return;

        // 2. 弹出数量
        NumberData amountData = OperationHelpers.popNumber(executor, id);
        if (amountData == null)
            return;

        int amount = amountData.asInt();
        if (amount <= 0) {
            executor.triggerMishap(id + ": amount 必须大于 0");
            return;
        }

        // 3. 弹出 amount 个元素到临时数组（按弹出顺序）
        List<Executable> tempArray = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            Executable element = executor.popData();
            if (element == null) {
                executor.triggerMishap(id + ": 数据栈不足，需要 " + amount + " 个元素");
                return;
            }
            tempArray.add(element);
        }
        // 此时 tempArray[0] 是最后弹出的元素（原栈顶），tempArray[amount-1] 是最先弹出的元素

        // 4. 根据 indices 从临时数组取元素（索引从 1 开始）
        List<Executable> indices = indicesList.getValue();
        List<Executable> result = new ArrayList<>();

        for (Executable indexExe : indices) {
            if (!(indexExe instanceof NumberData indexNum)) {
                executor.triggerMishap(id + ": 索引必须是数字");
                return;
            }

            int index = indexNum.asInt();
            if (index < 1 || index > amount) {
                executor.triggerMishap(id + ": 索引 " + index +
                        " 超出范围 [1, " + amount + "]");
                return;
            }

            // 索引从 1 开始，tempArray 索引从 0 开始，且 tempArray[0] 是最后弹出的元素
            // 所以 index=1 对应 tempArray[0], index=2 对应 tempArray[1], ...
            Executable element = tempArray.get(index - 1);
            result.add(element);
        }

        // 5. 将结果压回数据栈
        Collections.reverse(result);
        for (Executable executable : result) {
            executor.pushData(executable);
        }
    }
}
