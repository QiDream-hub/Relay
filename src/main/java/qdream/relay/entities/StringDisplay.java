package qdream.relay.entities;

import com.mojang.math.Transformation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 字符串显示实体 - 基于 Display.TextDisplay，添加能量系统和快捷设置方法
 */
public class StringDisplay extends Display.TextDisplay {

    /**
     * 能量值
     */
    private double energy = 0;

    /**
     * 每 tick 减少的能量值
     */
    private static final float ENERGY_DRAIN_PER_TICK = 0.1f;

    public StringDisplay(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * 每 tick 调用，减少能量
     */
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            this.energy = Math.max(0.0f, this.energy - ENERGY_DRAIN_PER_TICK
                    - (super.getBillboardConstraints() == Display.BillboardConstraints.CENTER ? 0.5 : 0));

            // 能量耗尽时移除实体
            if (this.energy <= 0.0f) {
                this.discard();
            }
        }
    }

    /**
     * 获取当前能量值
     */
    public double getEnergy() {
        return this.energy;
    }

    /**
     * 设置当前能量值
     */
    public void setEnergy(double energy) {
        this.energy = Math.max(0.0f, energy);
    }

    /**
     * 为实体添加能量
     */
    public void addEnergy(double amount) {
        this.setEnergy(this.energy + amount);
    }

    /**
     * 检查是否有足够能量
     */
    public boolean hasEnoughEnergy(double required) {
        return getEnergy() >= required;
    }

    /**
     * 快捷方法：设置显示的文本（String 版本）
     */
    public void setTextString(String text) {
        super.setText(Component.literal(text));
    }

    /**
     * 快捷方法：设置文本颜色（RGB）
     */
    public void setTextColor(int rgb) {
        Component coloredText = Component.literal(this.getText().getString())
                .withColor(rgb);
        super.setText(coloredText);
    }

    /**
     * 快捷方法：设置文本颜色（带透明度）
     */
    public void setTextColor(int rgb, int alpha) {
        int colorWithAlpha = (alpha << 24) | (rgb & 0xFFFFFF);
        this.setTextColor(colorWithAlpha);
    }

    /**
     * 快捷方法：设置背景颜色（RGB，透明度自动设置）
     */
    public void setBackgroundColorRgb(int rgb) {
        // Minecraft 背景颜色格式：AARRGGBB
        int colorWithAlpha = (0x40 << 24) | (rgb & 0xFFFFFF); // 默认 25% 透明度
        super.setBackgroundColor(colorWithAlpha);
    }

    /**
     * 快捷方法：设置背景颜色（带自定义透明度）
     */
    public void setBackgroundColor(int rgb, int alpha) {
        int colorWithAlpha = (alpha << 24) | (rgb & 0xFFFFFF);
        super.setBackgroundColor(colorWithAlpha);
    }

    /**
     * 快捷方法：设置背景透明度（0-255）
     */
    public void setBackgroundAlpha(int alpha) {
        int currentColor = super.getBackgroundColor();
        int rgb = currentColor & 0xFFFFFF;
        int colorWithNewAlpha = (alpha << 24) | rgb;
        super.setBackgroundColor(colorWithNewAlpha);
    }

    /**
     * 快捷方法：启用/禁用阴影
     */
    public void setShadow(boolean enabled) {
        byte flags = super.getFlags();
        if (enabled) {
            super.setFlags((byte) (flags | FLAG_SHADOW));
        } else {
            super.setFlags((byte) (flags & ~FLAG_SHADOW));
        }
    }

    /**
     * 快捷方法：启用/禁用透明背景（see-through）
     */
    public void setSeeThrough(boolean enabled) {
        byte flags = super.getFlags();
        if (enabled) {
            super.setFlags((byte) (flags | FLAG_SEE_THROUGH));
        } else {
            super.setFlags((byte) (flags & ~FLAG_SEE_THROUGH));
        }
    }

    /**
     * 快捷方法：启用/禁用默认背景
     */
    public void setDefaultBackground(boolean enabled) {
        byte flags = super.getFlags();
        if (enabled) {
            super.setFlags((byte) (flags | FLAG_USE_DEFAULT_BACKGROUND));
        } else {
            super.setFlags((byte) (flags & ~FLAG_USE_DEFAULT_BACKGROUND));
        }
    }

    /**
     * 快捷方法：设置文本对齐方式
     */
    public void setAlignment(Alignment alignment) {
        byte flags = super.getFlags();
        // 清除现有的对齐标志
        flags = (byte) (flags & ~FLAG_ALIGN_LEFT & ~FLAG_ALIGN_RIGHT);
        // 设置新的对齐标志
        switch (alignment) {
            case LEFT -> flags |= FLAG_ALIGN_LEFT;
            case RIGHT -> flags |= FLAG_ALIGN_RIGHT;
            case CENTER -> {
            } // 中心对齐不需要标志
        }
        super.setFlags(flags);
    }

    /**
     * 快捷方法：设置文本宽度（换行宽度）
     */
    public void setLineWidthCustom(int width) {
        super.setLineWidth(width);
    }

    /**
     * 使用方向向量设置旋转（类似 Entity.lookAt 的方式）
     * <p>
     * 计算从实体位置指向目标方向的旋转角度
     * </p>
     * <p>
     * 此方法会覆盖 Billboard 约束，设置为 FIXED 模式
     * </p>
     *
     * @param direction 目标方向向量（会被归一化）
     */
    public void setRotation(Vec3 direction) {
        this.setRotation(direction, true);
    }

    /**
     * 使用方向向量设置旋转（类似 Entity.lookAt 的方式）
     * <p>
     * 计算从实体位置指向目标方向的旋转角度
     * </p>
     *
     * @param direction         目标方向向量（会被归一化）
     * @param overrideBillboard 是否覆盖 Billboard 约束为 FIXED 模式
     */
    public void setRotation(Vec3 direction, boolean overrideBillboard) {
        Vec3 normalizedDir = direction.normalize();
        float yaw = (float) Math.atan2(normalizedDir.x, normalizedDir.z);
        float pitch = (float) Math.asin(-normalizedDir.y);

        Transformation transformation = vec3ToRotationTransformation(pitch, yaw);
        super.setTransformation(transformation);

        if (overrideBillboard) {
            this.setBillboardConstraints(BillboardType.FIXED);
        }
    }

    /**
     * 使用目标点设置旋转（类似 Entity.lookAt 的方式）
     * <p>
     * 计算从实体位置指向目标点的旋转角度
     * </p>
     * <p>
     * 此方法会覆盖 Billboard 约束，设置为 FIXED 模式
     * </p>
     *
     * @param target 目标点坐标
     */
    public void lookAt(Vec3 target) {
        this.lookAt(target, true);
    }

    /**
     * 使用目标点设置旋转（类似 Entity.lookAt 的方式）
     * <p>
     * 计算从实体位置指向目标点的旋转角度
     * </p>
     *
     * @param target            目标点坐标
     * @param overrideBillboard 是否覆盖 Billboard 约束为 FIXED 模式
     */
    public void lookAt(Vec3 target, boolean overrideBillboard) {
        Vec3 direction = target.subtract(this.position());
        this.setRotation(direction, overrideBillboard);
    }

    /**
     * 使用 pitch 和 yaw 创建旋转变换
     *
     * @param pitch 俯仰角（弧度，绕 X 轴旋转）
     * @param yaw   偏航角（弧度，绕 Y 轴旋转）
     * @return Transformation 对象
     */
    public static Transformation vec3ToRotationTransformation(float pitch, float yaw) {
        Quaternionf quaternion = new Quaternionf().rotationXYZ(pitch, yaw, 0);
        return new Transformation(
                new Vector3f(0, 0, 0),
                quaternion,
                new Vector3f(1, 1, 1),
                new Quaternionf());
    }

    /**
     * 将旋转向量转换为 Transformation
     * <p>
     * 仅包含旋转，平移为 (0,0,0)，缩放为 (1,1,1)
     * </p>
     *
     * @param rotation 旋转向量（弧度制）
     * @return Transformation 对象
     */
    public static Transformation vec3ToRotationTransformation(Vec3 rotation) {
        return vec3ToTransformation(rotation, new Vec3(0, 0, 0), new Vec3(1, 1, 1));
    }

    /**
     * 将 Vec3 参数转换为完整的 Transformation
     * <p>
     * 支持自定义旋转、平移、缩放
     * </p>
     *
     * @param rotation    旋转向量（弧度制，XYZ 分别对应绕 XYZ 轴旋转）
     * @param translation 平移向量
     * @param scale       缩放向量
     * @return Transformation 对象
     */
    public static Transformation vec3ToTransformation(
            Vec3 rotation,
            Vec3 translation,
            Vec3 scale) {
        Quaternionf quaternion = new Quaternionf().rotationXYZ(
                (float) rotation.x,
                (float) rotation.y,
                (float) rotation.z);

        return new Transformation(
                new Vector3f((float) translation.x, (float) translation.y, (float) translation.z),
                quaternion,
                new Vector3f((float) scale.x, (float) scale.y, (float) scale.z),
                new Quaternionf());
    }

    /**
     * 快捷方法：设置文本透明度（0-255）
     */
    public void setTextOpacityCustom(int alpha) {
        super.setTextOpacity((byte) alpha);
    }

    /**
     * 快捷方法：设置广告牌约束（Billboard 渲染方向）
     */
    public void setBillboardConstraints(BillboardType type) {
        Display.BillboardConstraints constraint = switch (type) {
            case FIXED -> Display.BillboardConstraints.FIXED;
            case VERTICAL -> Display.BillboardConstraints.VERTICAL;
            case HORIZONTAL -> Display.BillboardConstraints.HORIZONTAL;
            case CENTER -> Display.BillboardConstraints.CENTER;
        };
        super.setBillboardConstraints(constraint);
    }

    /**
     * 广告牌渲染方向类型
     */
    public enum BillboardType {
        /** 固定方向，不随视角旋转 */
        FIXED,
        /** 垂直广告牌，始终面向玩家但保持垂直 */
        VERTICAL,
        /** 水平广告牌，始终面向玩家但保持水平 */
        HORIZONTAL,
        /** 中心广告牌，完全面向玩家 */
        CENTER
    }

    /**
     * 文本对齐方式枚举
     */
    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    /**
     * 读取 NBT 数据
     */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.energy = input.getDoubleOr("energy", 0);
    }

    /**
     * 写入 NBT 数据
     */
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("energy", this.energy);
    }
}
