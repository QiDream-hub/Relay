package qdream.relay.mc.signature;

/**
 * 参数来源
 * 描述操作从哪个栈获取参数或向哪个栈输出
 */
public enum ParameterSource {
    /** 从数据栈弹出/向数据栈压入 */
    DATA_STACK,
    /** 从程序栈弹出/向程序栈压入 */
    PROGRAM_STACK
}
