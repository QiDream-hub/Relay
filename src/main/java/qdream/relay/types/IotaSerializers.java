package qdream.relay.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.mc.McVec3Adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Iota 序列化器注册
 * 注册所有内置类型的 JSON 序列化/反序列化器
 */
public final class IotaSerializers {

    private IotaSerializers() {}

    /**
     * 注册所有内置类型
     */
    public static void register() {
        // 数字类型
        Executable.TypeRegistry.register("relay:number",
                // 序列化
                exec -> {
                    NumberIota iota = (NumberIota) exec;
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:number");
                    if (iota.isInteger()) {
                        json.addProperty("value", iota.asInt());
                    } else {
                        json.addProperty("value", iota.asDouble());
                    }
                    return json;
                },
                // 反序列化
                json -> {
                    JsonElement valueElem = json.get("value");
                    if (valueElem.isJsonPrimitive() && valueElem.getAsJsonPrimitive().isNumber()) {
                        try {
                            int intVal = valueElem.getAsInt();
                            if (valueElem.getAsString().contains(".")) {
                                return new NumberIota(valueElem.getAsDouble());
                            } else {
                                return new NumberIota(intVal);
                            }
                        } catch (Exception e) {
                            return new NumberIota(valueElem.getAsDouble());
                        }
                    }
                    String valueStr = valueElem.getAsString();
                    if (valueStr.contains(".")) {
                        return new NumberIota(Double.parseDouble(valueStr));
                    } else {
                        return new NumberIota(Integer.parseInt(valueStr));
                    }
                }
        );

        // 布尔类型
        Executable.TypeRegistry.register("relay:boolean",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:boolean");
                    json.addProperty("value", ((BooleanIota) exec).asBoolean());
                    return json;
                },
                json -> new BooleanIota(json.get("value").getAsBoolean())
        );

        // 字符串类型
        Executable.TypeRegistry.register("relay:string",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:string");
                    json.addProperty("value", ((StringIota) exec).asString());
                    return json;
                },
                json -> new StringIota(json.get("value").getAsString())
        );

        // 向量类型
        Executable.TypeRegistry.register("relay:vector",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:vector");
                    McVec3Adapter vec = ((VectorIota) exec).asVector();
                    JsonObject posJson = new JsonObject();
                    posJson.addProperty("x", vec.x());
                    posJson.addProperty("y", vec.y());
                    posJson.addProperty("z", vec.z());
                    json.add("value", posJson);
                    return json;
                },
                json -> {
                    JsonObject posJson = json.get("value").getAsJsonObject();
                    double x = posJson.get("x").getAsDouble();
                    double y = posJson.get("y").getAsDouble();
                    double z = posJson.get("z").getAsDouble();
                    return new VectorIota(new McVec3Adapter(new net.minecraft.world.phys.Vec3(x, y, z)));
                }
        );

        // 实体类型
        Executable.TypeRegistry.register("relay:entity",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:entity");
                    json.addProperty("value", ((EntityIota) exec).asEntity().toString());
                    return json;
                },
                json -> new EntityIota(UUID.fromString(json.get("value").getAsString()))
        );

        // 列表类型
        Executable.TypeRegistry.register("relay:list",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:list");
                    JsonArray array = new JsonArray();
                    for (Executable item : ((ProgramBlock) exec).getItems()) {
                        array.add(item.toJson());
                    }
                    json.add("value", array);
                    return json;
                },
                json -> {
                    JsonArray array = json.get("value").getAsJsonArray();
                    List<Executable> list = new ArrayList<>();
                    for (JsonElement elem : array) {
                        list.add((Executable) Executable.TypeRegistry.fromJson(elem));
                    }
                    return new ProgramBlock(list);
                }
        );

        // 空值类型
        Executable.TypeRegistry.register("relay:null",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:null");
                    return json;
                },
                json -> NullIota.INSTANCE
        );

        // 操作类型（特殊的可执行单元，用于存储操作 ID）
        Executable.TypeRegistry.register("relay:operation",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:operation");
                    json.addProperty("op", ((Operation) exec).getOpId());
                    return json;
                },
                json -> new Operation(json.get("op").getAsString())
        );
    }
}
