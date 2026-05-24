package com.zhlon.lrifix.mixin;

import me.xjqsh.lrtactical.client.input.AttackKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AttackKeys.class, remap = false)
public class AttackKeysMixin {

    @Inject(method = "onSpAttack", at = @At("HEAD"), remap = false, cancellable = true)
    private static void cancelSpAttackOnInteract(InputEvent.MouseButton.Post event, CallbackInfo ci) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.hitResult == null) return;

        if (mc.player.isShiftKeyDown()) {
            return;
        }

        boolean shouldCancelSpAttack = false;
        HitResult.Type hitType = mc.hitResult.getType();

        if (hitType == HitResult.Type.MISS) {
            shouldCancelSpAttack = false;
        }
        else if (hitType == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
            var state = mc.level.getBlockState(blockHit.getBlockPos());
            var block = state.getBlock();

            String blockClass = block.getClass().getSimpleName().toLowerCase();
            var blockKey = ForgeRegistries.BLOCKS.getKey(block);
            String blockId = blockKey != null ? blockKey.getPath().toLowerCase() : "";
            String modId = blockKey != null ? blockKey.getNamespace().toLowerCase() : "";
            String blockName = modId + ":" + blockId + "_" + blockClass;

            boolean hasGui = state.getMenuProvider(mc.level, blockHit.getBlockPos()) != null;

            boolean isIgnoredEntityBlock = blockName.contains("pipe") || blockName.contains("cable") ||
                    blockName.contains("wire") || blockName.contains("conduit") ||
                    blockName.contains("duct") || modId.contains("framedblocks");

            boolean isInteractiveEntityBlock = state.hasBlockEntity() && !isIgnoredEntityBlock;

            boolean isInteractiveBlock = hasGui
                    || isInteractiveEntityBlock
                    || state.is(BlockTags.DOORS)
                    || state.is(BlockTags.TRAPDOORS)
                    || state.is(BlockTags.BUTTONS)
                    || state.is(BlockTags.FENCE_GATES)
                    || state.is(BlockTags.BEDS)
                    || state.is(BlockTags.CAMPFIRES)
                    || block instanceof CraftingTableBlock
                    || block instanceof LeverBlock
                    || block instanceof AnvilBlock
                    || block instanceof EnchantmentTableBlock
                    || block instanceof GrindstoneBlock
                    || block instanceof JukeboxBlock
                    || block instanceof NoteBlock
                    || block instanceof CakeBlock
                    || block instanceof RepeaterBlock
                    || block instanceof ComparatorBlock
                    || block instanceof HopperBlock
                    || block instanceof DaylightDetectorBlock
                    || block instanceof DispenserBlock
                    || block instanceof DropperBlock
                    || block instanceof DoorBlock
                    || block instanceof TrapDoorBlock
                    || block instanceof ButtonBlock
                    || block instanceof FenceGateBlock
                    || blockName.contains("door") || blockName.contains("window")
                    || blockName.contains("drawer") || blockName.contains("cabinet")
                    || blockName.contains("cupboard") || blockName.contains("desk")
                    || blockName.contains("table") || blockName.contains("chair")
                    || blockName.contains("bridge") || blockName.contains("roof")
                    || blockName.contains("vehicle") || blockName.contains("seat")
                    || blockName.contains("engine") || blockName.contains("lever")
                    || blockName.contains("button") || blockName.contains("switch")
                    || blockName.contains("part") || blockName.contains("control")
                    || modId.contains("immersivevehicles") || modId.contains("mts")
                    || modId.contains("mcw");

            shouldCancelSpAttack = isInteractiveBlock;
        }
        else if (hitType == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) mc.hitResult;
            var entity = entityHit.getEntity();

            String className = entity.getClass().getSimpleName().toLowerCase();
            var entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            String registryName = entityType != null ? entityType.getPath().toLowerCase() : "";
            String modId = entityType != null ? entityType.getNamespace().toLowerCase() : "";
            String entityName = modId + ":" + registryName + "_" + className;

            if (entity instanceof Enemy || entity instanceof Projectile || entity instanceof ItemEntity) {
                shouldCancelSpAttack = false;
            }
            else if (entity instanceof Player) {
                shouldCancelSpAttack = false;
            }
            else if (entity instanceof Npc || entity instanceof Villager || entity instanceof WanderingTrader ||
                    entity instanceof AbstractMinecart || entity instanceof Boat ||
                    entity instanceof ArmorStand || entity instanceof HangingEntity) {
                shouldCancelSpAttack = true;
            }
            else if (entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AmbientCreature) {
                if (entity instanceof TamableAnimal tamable && tamable.isTame()) shouldCancelSpAttack = true;
                else if (entity instanceof AbstractHorse horse && horse.isTamed()) shouldCancelSpAttack = true;
                else if (entity instanceof Saddleable saddleable && saddleable.isSaddled()) shouldCancelSpAttack = true;
                else shouldCancelSpAttack = false;
            }
            else if (entity instanceof MenuProvider || entity instanceof Container) {
                shouldCancelSpAttack = true;
            }
            else if (entityName.contains("npc") || entityName.contains("trader") || entityName.contains("merchant") ||
                    entityName.contains("villager") || entityName.contains("citizen") || entityName.contains("maid") ||
                    entityName.contains("vehicle") || entityName.contains("mount") || entityName.contains("seat") ||
                    entityName.contains("chair") || entityName.contains("sofa") || entityName.contains("stool") ||
                    entityName.contains("cushion") || entityName.contains("plane") || entityName.contains("car") ||
                    entityName.contains("train") || entityName.contains("ship") || entityName.contains("cart") ||
                    entityName.contains("door") || entityName.contains("gate") || entityName.contains("contraption") ||
                    entityName.contains("dummy") || entityName.contains("statue") || entityName.contains("mannequin") ||
                    entityName.contains("part") || entityName.contains("control") || entityName.contains("interact") ||
                    modId.contains("immersivevehicles") || modId.contains("mts")) {
                shouldCancelSpAttack = true;
            }
            else {
                shouldCancelSpAttack = false;
            }
        }

        // ==========================================
        // 最终处决：仅在真正交互时显示原版挥手
        // ==========================================
        if (shouldCancelSpAttack) {
            while (AttackKeys.SPECIAL_ATTACK.consumeClick()) {}

            // 因为即将取消战术攻击去执行开门等操作，这里手动补充一个原版挥手，显得自然
            mc.player.swing(InteractionHand.MAIN_HAND);

            ci.cancel();
        }
    }

//    /**
//     * ✨ 核心修复：定向爆破模组里的冗余挥手代码！
//     * 拦截原模组在左键(onNormalAttack)、右键(onSpAttack)、长按(tick)时硬编码的 player.swing()
//     * 从此，当你触发真正的处决或挥砍时，原版手臂将彻底消失，只留下最纯净、最震撼的战术动画！
//     */
//    @Redirect(
//            method = {"onSpAttack", "onNormalAttack", "tick"},
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"),
//            remap = true
//    )
//    private static void preventVanillaSwingDuringTacticalAction(LocalPlayer instance, InteractionHand hand) {
//        // 里面留空，什么都不做。直接无视掉模组作者强加的原版挥手！
//    }
}