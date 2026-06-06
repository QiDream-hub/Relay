package qdream.relay.mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.IData;
import qdream.relay.engine.IExecutable;
import qdream.relay.engine.IotaTypeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * McIota 类型注册
 * 注册所有内置类型的 JSON 序列化/反序列化器
 */
public class McIotaTypes {

    /**
     * 注册所有内置类型
     */
    public static void register() {
        // 数字类型
        IData.TypeRegistry.register("relay:number",
                // 序列化
                data -> {
                    McIota iota = (McIota) data;
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:number");
                    if (iota.getValue() instanceof Double) {
                        json.addProperty("value", iota.asDouble());
                    } else {
                        json.addProperty("value", iota.asInt());
                    }
                    return json;
                },
                // 反序列化
                json -> {
                    JsonElement valueElem = json.get("value");
                    String valueStr = valueElem.getAsString();
                    if (valueStr.contains(".")) {
                        return McIota.ofDouble(valueElem.getAsDouble());
                    } else {
                        return McIota.ofInt(valueElem.getAsInt());
                    }
                }
        );

        // 布尔类型
        IData.TypeRegistry.register("relay:boolean",
                data -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:boolean");
                    json.addProperty("value", ((McIota) data).asBoolean());
                    return json;
                },
                json -> McIota.ofBoolean(json.get("value").getAsBoolean())
        );

        // 字符串类型
        IData.TypeRegistry.register("relay:string",
                data -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:string");
                    json.addProperty("value", ((McIota) data).asString());
                    return json;
                },
                json -> McIota.ofString(json.get("value").getAsString())
        );

        // 向量类型
        IData.TypeRegistry.register("relay:vector",
                data -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:vector");
                    Object vec = ((McIota) data).asVector();
                    if (vec instanceof McVec3Adapter) {
                        net.minecraft.world.phys.Vec3 v = ((McVec3Adapter) vec).getVec3();
                        JsonObject posJson = new JsonObject();
                        posJson.addProperty("x", v.x);
                        posJson.addProperty("y", v.y);
                        posJson.addProperty("z", v.z);
                        json.add("value", posJson);
                    }
                    return json;
                },
                json -> {
                    JsonObject posJson = json.get("value").getAsJsonObject();
                    double x = posJson.get("x").getAsDouble();
                    double y = posJson.get("y").getAsDouble();
                    double z = posJson.get("z").getAsDouble();
                    return McIota.ofVector(new McVec3Adapter(new net.minecraft.world.phys.Vec3(x, y, z)));
                }
        );

        // 实体类型
        IData.TypeRegistry.register("relay:entity",
                data -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:entity");
                    json.addProperty("value", ((McIota) data).asEntity().toString());
                    return json;
                },
                json -> McIota.ofEntity(UUID.fromString(json.get("value").getAsString()))
        );

        // 列表类型
        IData.TypeRegistry.register("relay:list",
                data -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:list");
                    JsonArray array = new JsonArray();
                    for (IExecutable item : ((McIota) data).asList()) {
                        array.add(item.toJson());
                    }
                    json.add("value", array);
                    return json;
                },
                json -> {
                    JsonArray array = json.get("value").getAsJsonArray();
                    List<IExecutable> list = new ArrayList<>();
                    for (JsonElement elem : array) {
                        list.add((IExecutable) IData.TypeRegistry.fromJson(elem));
                    }
                    return McIota.ofList(list);
                }
        );

        // 空值类型
        IData.TypeRegistry.register("relay:null",
                data -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:null");
                    return json;
                },
                json -> McIota.ofNull()
        );

        // 操作类型（特殊的字符串，用于存储操作 ID）
        IData.TypeRegistry.register("relay:operation",
                data -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "relay:operation");
                    json.addProperty("op", ((McIota) data).asString());
                    return json;
                },
                json -> McIota.ofString(json.get("op").getAsString())
        );
    }

    private McIotaTypes() {}
}
