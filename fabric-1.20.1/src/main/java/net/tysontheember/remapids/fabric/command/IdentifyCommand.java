package net.tysontheember.remapids.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.tysontheember.remapids.core.IdentifyHelper;
import net.tysontheember.remapids.core.RemapState;

public class IdentifyCommand {

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        dispatcher.register(
                Commands.literal("remapids")
                        .then(Commands.literal("id")
                                .then(Commands.literal("block")
                                        .executes(IdentifyCommand::identifyBlock))
                                .then(Commands.literal("hand")
                                        .executes(IdentifyCommand::identifyHand))
                        )
        );
    }

    private static int identifyBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HitResult hit = player.pick(5.0, 0.0f, false);

        if (hit.getType() != HitResult.Type.BLOCK) {
            ctx.getSource().sendFailure(Component.literal("No block in range"));
            return 0;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockState state = player.level().getBlockState(blockHit.getBlockPos());
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        String message = IdentifyHelper.formatBlockInfo(blockId, RemapState.get());
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int identifyHand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No item in hand"));
            return 0;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        String message = IdentifyHelper.formatItemInfo(itemId, RemapState.get());
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
