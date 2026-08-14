package qdream.relay.tools;

import net.minecraft.network.chat.Component;

/**
 * 错误消息生成工具类
 * <p>
 * 提供类型安全的错误消息生成方法，确保在非渲染上下文中也能正确格式化
 * </p>
 */
public final class ErrorMessageTools {

    private ErrorMessageTools() {
        // 防止实例化
    }

    /**
     * 错误类型枚举
     * <p>
     * 每个枚举常量对应语言文件中的一个错误消息翻译键
     * </p>
     */
    public enum ErrorType {
        // === 栈错误 ===
        /** 数据栈为空 */
        STACK_EMPTY("stack_empty"),
        /** 数据栈不足，需要 {0} 个参数 */
        STACK_INSUFFICIENT("stack_insufficient"),
        /** 索引无效：{0} */
        INVALID_INDEX("invalid_index"),
        /** 索引 {0} 超出范围 (大小：{1}) */
        INDEX_OUT_OF_BOUNDS("index_out_of_bounds"),
        /** 列表索引超出范围：{0} */
        LIST_INDEX_OUT_OF_BOUNDS("list_index_out_of_bounds"),

        // === 类型错误 ===
        /** 类型不匹配：期望 {0}, 实际为 {1} */
        TYPE_MISMATCH("type_mismatch"),

        // === 能量错误 ===
        /** 能量不足：需要 {0} */
        ENERGY_INSUFFICIENT("energy_insufficient"),

        // === 世界交互器错误 ===
        /** {0} 需要世界交互器 */
        WORLD_INTERACTOR_MISSING("world_interactor_missing"),
        /** {0} 超出世界交互器范围：{1} > {2} */
        WORLD_INTERACTOR_OUT_OF_RANGE("world_interactor_out_of_range"),

        // === 引用有效性错误 ===
        /** 无效的实体引用 */
        INVALID_ENTITY("invalid_entity"),
        /** 无效的方块实体引用 */
        INVALID_BLOCK_ENTITY("invalid_block_entity"),
        /** 无效的方块引用 */
        INVALID_BLOCK("invalid_block"),
        /** 无效的物品 */
        INVALID_ITEM("invalid_item"),
        /** 无效的容器 */
        INVALID_CONTAINER("invalid_container"),
        /** 错误的物品 */
        INVALID_ITEM_FOR_ACTION("invalid_item_for_action"),

        // === 容器/插槽错误 ===
        /** 目标不是容器 */
        NOT_A_CONTAINER("not_a_container"),
        /** 目标物品不是法术磁盘 */
        NOT_A_SPELL_DISK("not_a_spell_disk"),
        /** 容器不存在 */
        CONTAINER_NOT_FOUND("container_not_found"),
        /** 插槽不存在 */
        SLOT_NOT_FOUND("slot_not_found"),
        /** 物品不存在 */
        ITEM_NOT_FOUND("item_not_found"),
        /** 磁盘为空 */
        DISK_EMPTY("disk_empty"),

        // === 世界/位置错误 ===
        /** 世界不存在：{0} */
        WORLD_NOT_FOUND("world_not_found"),
        /** 无效的位置：{0} */
        POSITION_INVALID("position_invalid"),
        /** 无法获取世界 */
        WORLD_NOT_AVAILABLE("world_not_available"),

        // === 参数/执行错误 ===
        /** 无效的参数：{0} */
        PARAMETER_INVALID("parameter_invalid"),
        /** 执行失败：{0} */
        EXECUTION_FAILED("execution_failed"),
        /** 参数超出有效范围：{0} */
        PARAMETER_OUT_OF_RANGE("parameter_out_of_range"),
        /** 批量复制：参数必须是数字 */
        BATCH_COUNT_MUST_BE_NUMBER("batch_count_must_be_number"),
        /** 批量复制：计数必须是整数 */
        BATCH_COUNT_MUST_BE_INTEGER("batch_count_must_be_integer"),
        /** 批量复制：计数必须大于 0 */
        BATCH_COUNT_MUST_BE_POSITIVE("batch_count_must_be_positive"),
        /** 批量复制：计数超出栈大小 */
        BATCH_COUNT_EXCEEDS_STACK("batch_count_exceeds_stack"),
        /** 能量值必须大于 0 */
        ENERGY_MUST_BE_POSITIVE("energy_must_be_positive"),
        /** 核心数量超出有效范围 */
        CORE_COST_OUT_OF_RANGE("core_cost_out_of_range"),
        /** 执行间隔超出有效范围 */
        INTERVAL_OUT_OF_RANGE("interval_out_of_range"),
        /** 交互范围超出有效范围 */
        RANGE_OUT_OF_RANGE("range_out_of_range"),
        /** 预付能量不足 */
        PREPAID_ENERGY_INSUFFICIENT("prepaid_energy_insufficient"),

        // === 运算错误 ===
        /** 除零错误 */
        DIVISION_BY_ZERO("division_by_zero"),
        /** 操作无法比较 */
        OPERATION_NOT_COMPARABLE("operation_not_comparable"),

        // === 向量相关错误 ===
        /** 无效的向量（NaN 或 Infinity） */
        INVALID_VECTOR("invalid_vector"),
        /** 向量不能为零向量 */
        ZERO_VECTOR("zero_vector"),

        // === 实体相关错误 ===
        /** 实体生成失败 */
        ENTITY_SPAWN_FAILED("entity_spawn_failed"),
        /** 无法获取所属者 */
        OWNER_NOT_FOUND("owner_not_found"),
        /** 无权执行此操作 */
        PERMISSION_DENIED("permission_denied"),
        /** 目标实体不是 可运行实体 */
        NOT_ENTITY_SHELL("not_entity_shell"),
        /** 实体引用无效 */
        ENTITY_REFERENCE_INVALID("entity_reference_invalid"),
        /** 方块实体引用无效 */
        BLOCK_ENTITY_REFERENCE_INVALID("block_entity_reference_invalid"),
        /** 方块引用无效 */
        BLOCK_REFERENCE_INVALID("block_reference_invalid"),
        /** 错误的实体 */
        INVALID_ENTITY_REFERENCE("invalid_entity_reference"),

        // === 程序相关错误 ===
        /** 程序解析失败：{0} */
        COMPILATION_FAILED("compilation_failed"),
        /** 程序序列化失败：{0} */
        SERIALIZATION_FAILED("serialization_failed"),
        /** 通信频道队列已满 */
        COMMUNICATION_QUEUE_FULL("communication_queue_full"),

        // === 文本错误 ===
        /** 文本不能为空 */
        TEXT_EMPTY("text_empty"),

        // === 容器/上下文错误 ===
        /** 无法获取容器上下文 */
        CONTAINER_CONTEXT_MISSING("container_context_missing"),

        // === 栈元素操作错误 ===
        /** 无法获取目标元素 */
        ELEMENT_NOT_GETTABLE("element_not_gettable"),
        /** 无法移除目标元素 */
        ELEMENT_NOT_REMOVABLE("element_not_removable"),
        /** 无法设置目标元素 */
        ELEMENT_NOT_SETTABLE("element_not_settable"),

        // === 栈重排操作错误 ===
        /** 栈重排：索引必须是数字 */
        STACK_REARRANGE_INDEX_MUST_BE_NUMBER("stack_rearrange_index_must_be_number"),
        /** 栈重排：索引超出范围 */
        STACK_REARRANGE_INDEX_OUT_OF_RANGE("stack_rearrange_index_out_of_range"),

        // === 列表创建操作错误 ===
        /** 创建列表：数据栈大小不足 */
        STACK_SIZE_INSUFFICIENT_FOR_LIST_CREATE("stack_size_insufficient_for_list_create"),
        /** 创建列表：大小不匹配 */
        LIST_CREATE_SIZE_MISMATCH("list_create_size_mismatch"),

        // === 方块射线追踪错误 ===
        /** 无法获取方块面 */
        BLOCK_FACE_NOT_FOUND("block_face_not_found");

        private final String code;

        ErrorType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static Component buildErrorMessage(ErrorType e, Object... args) {
        return TextTools.getComponent("error." + e.getCode(), args);
    }

}
