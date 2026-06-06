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
                    json.addProperty("id", "relay:number");
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
                    json.addProperty("id", "relay:boolean");
                    json.addProperty("value", ((BooleanIota) exec).asBoolean());
                    return json;
                },
                json -> new BooleanIota(json.get("value").getAsBoolean())
        );

        // 字符串类型
        Executable.TypeRegistry.register("relay:string",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:string");
                    json.addProperty("value", ((StringIota) exec).asString());
                    return json;
                },
                json -> new StringIota(json.get("value").getAsString())
        );

        // 向量类型
        Executable.TypeRegistry.register("relay:vector",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:vector");
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
                    json.addProperty("id", "relay:entity");
                    json.addProperty("value", ((EntityIota) exec).asEntity().toString());
                    return json;
                },
                json -> new EntityIota(UUID.fromString(json.get("value").getAsString()))
        );

        // 列表类型
        Executable.TypeRegistry.register("relay:list",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:list");
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
                        list.add(Executable.TypeRegistry.fromJson(elem));
                    }
                    return new ProgramBlock(list);
                }
        );

        // 空值类型
        Executable.TypeRegistry.register("relay:null",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:null");
                    return json;
                },
                json -> NullIota.INSTANCE
        );

        // 基础操作
        Executable.TypeRegistry.register("relay:add",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:add");
                    return json;
                },
                json -> new Operation("relay:add")
        );

        Executable.TypeRegistry.register("relay:sub",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:sub");
                    return json;
                },
                json -> new Operation("relay:sub")
        );

        Executable.TypeRegistry.register("relay:mul",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:mul");
                    return json;
                },
                json -> new Operation("relay:mul")
        );

        Executable.TypeRegistry.register("relay:div",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:div");
                    return json;
                },
                json -> new Operation("relay:div")
        );

        Executable.TypeRegistry.register("relay:and",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:and");
                    return json;
                },
                json -> new Operation("relay:and")
        );

        Executable.TypeRegistry.register("relay:or",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:or");
                    return json;
                },
                json -> new Operation("relay:or")
        );

        Executable.TypeRegistry.register("relay:not",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:not");
                    return json;
                },
                json -> new Operation("relay:not")
        );

        Executable.TypeRegistry.register("relay:eq",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:eq");
                    return json;
                },
                json -> new Operation("relay:eq")
        );

        Executable.TypeRegistry.register("relay:lt",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:lt");
                    return json;
                },
                json -> new Operation("relay:lt")
        );

        Executable.TypeRegistry.register("relay:gt",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:gt");
                    return json;
                },
                json -> new Operation("relay:gt")
        );

        Executable.TypeRegistry.register("relay:eval",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:eval");
                    return json;
                },
                json -> new Operation("relay:eval")
        );

        Executable.TypeRegistry.register("relay:if",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:if");
                    return json;
                },
                json -> new Operation("relay:if")
        );

        Executable.TypeRegistry.register("relay:send",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:send");
                    return json;
                },
                json -> new Operation("relay:send")
        );

        Executable.TypeRegistry.register("relay:recv",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:recv");
                    return json;
                },
                json -> new Operation("relay:recv")
        );

        Executable.TypeRegistry.register("relay:peek",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:peek");
                    return json;
                },
                json -> new Operation("relay:peek")
        );

        // 列表操作
        Executable.TypeRegistry.register("relay:list",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:list");
                    return json;
                },
                json -> new Operation("relay:list")
        );

        Executable.TypeRegistry.register("relay:get",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:get");
                    return json;
                },
                json -> new Operation("relay:get")
        );

        Executable.TypeRegistry.register("relay:set",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:set");
                    return json;
                },
                json -> new Operation("relay:set")
        );

        Executable.TypeRegistry.register("relay:len",
                exec -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", "relay:len");
                    return json;
                },
                json -> new Operation("relay:len")
        );
    }
}
