package com.zhlon.lrifix.mixin;

import me.xjqsh.lrtactical.api.item.ICustomItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(
        value = me.xjqsh.lrtactical.client.ClientEventsHandler.class,
        remap = false
)
public class ItemMixin {

    /**
     * @author zhlon
     * @reason
     * 修复：
     * 手持近战武器无法交互
     *
     * 保留：
     * - 空气特殊攻击
     * - 非交互实体特殊攻击
     */
    @Overwrite(remap = false)
    public static void onClickInput(
            InputEvent.InteractionKeyMappingTriggered event
    ) {

        Minecraft mc = Minecraft.getInstance();

        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        ItemStack itemInHand =
                player.getItemInHand(InteractionHand.MAIN_HAND);

        /*
         * 主手不是战术物品
         */
        if (!(itemInHand.getItem() instanceof ICustomItem customItem)) {
            return;
        }

        /*
         * =====================================
         * 方块：
         * 优先交互
         * =====================================
         */
        if (mc.hitResult instanceof BlockHitResult) {

            return;
        }

        /*
         * =====================================
         * 实体
         * =====================================
         */
        if (mc.hitResult instanceof EntityHitResult entityHitResult) {

            var entity = entityHitResult.getEntity();

            boolean interactiveEntity =

                    entity instanceof AbstractVillager

                            || entity instanceof MenuProvider

                            || entity.isVehicle()

                            || entity instanceof OwnableEntity

                            || entity.canRiderInteract();

            /*
             * 可交互实体：
             * 放行 Minecraft 交互
             */
            if (interactiveEntity) {

                return;
            }

            /*
             * 非交互实体：
             * 保留 LesRaisins 原版特殊攻击
             */
        }

        /*
         * =====================================
         * 空气 / 非交互实体
         * 保留 LesRaisins 原逻辑
         * =====================================
         */
        boolean flag =
                (event.isAttack() && customItem.shouldBlockAttack())
                        || (event.isUseItem() && customItem.shouldBlockUse())
                        || (event.isPickBlock() && customItem.shouldBlockPickBlock());

        if (flag) {

            // 阻止 Minecraft 原版右键
            // 允许 LesRaisins AttackKeys 继续执行
            event.setCanceled(true);

            // 不关闭 swing
            // 否则动画有些武器不播放
        }
    }
}