package com.spider.framedfabric.screen;

import com.spider.framedfabric.registry.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class WoodWorkbenchRecipes {
    /*private static final List<WorkbenchOption> OPTIONS = new ArrayList<>();

    private WoodWorkbenchRecipes() {}

    public static void init() {
        if (!OPTIONS.isEmpty()) return;

        // =========================
        // Framed block conversions
        // =========================
        register(new WorkbenchOption(
                "framed_slab",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_SLAB, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_vertical_slab",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_VERTICAL_SLAB, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_half_slab",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_HALF_SLAB, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_stairs",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_STAIRS, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_vertical_stairs",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_VERTICAL_STAIRS, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_fence",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_FENCE, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_fence_gate",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_FENCE_GATE, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_wall",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_WALL, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_pane",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_PANE, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_trapdoor",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_TRAPDOOR, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_door",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_DOOR, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_button",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_BUTTON, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_pressure_plate",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_PRESSURE_PLATE, 1),
                1
        ));

        register(new WorkbenchOption(
                "framed_ladder",
                stack -> stack.isOf(ModBlocks.FRAMED_BLOCK.asItem()),
                () -> new ItemStack(ModBlocks.FRAMED_LADDER, 1),
                1
        ));

        // =========================
        // Vanilla oak examples
        // =========================
        register(new WorkbenchOption(
                "oak_slab",
                stack -> stack.isOf(Items.OAK_PLANKS),
                () -> new ItemStack(Items.OAK_SLAB, 2),
                1
        ));

        register(new WorkbenchOption(
                "oak_stairs",
                stack -> stack.isOf(Items.OAK_PLANKS),
                () -> new ItemStack(Items.OAK_STAIRS, 1),
                1
        ));

        register(new WorkbenchOption(
                "oak_fence",
                stack -> stack.isOf(Items.OAK_PLANKS),
                () -> new ItemStack(Items.OAK_FENCE, 1),
                1
        ));

        register(new WorkbenchOption(
                "oak_fence_gate",
                stack -> stack.isOf(Items.OAK_PLANKS),
                () -> new ItemStack(Items.OAK_FENCE_GATE, 1),
                1
        ));

        register(new WorkbenchOption(
                "oak_door",
                stack -> stack.isOf(Items.OAK_PLANKS),
                () -> new ItemStack(Items.OAK_DOOR, 1),
                1
        ));

        register(new WorkbenchOption(
                "oak_trapdoor",
                stack -> stack.isOf(Items.OAK_PLANKS),
                () -> new ItemStack(Items.OAK_TRAPDOOR, 1),
                1
        ));

        register(new WorkbenchOption(
                "oak_button",
                stack -> stack.isOf(Items.OAK_PLANKS),
                () -> new ItemStack(Items.OAK_BUTTON, 1),
                1
        ));

        register(new WorkbenchOption(
                "oak_pressure_plate",
                stack -> stack.isOf(Items.OAK_PLANKS),
                () -> new ItemStack(Items.OAK_PRESSURE_PLATE, 1),
                1
        ));
    }

    public static void register(WorkbenchOption option) {
        OPTIONS.add(option);
    }

    public static List<WorkbenchOption> getOptions(ItemStack input) {
        if (input.isEmpty()) return List.of();

        List<WorkbenchOption> result = new ArrayList<>();
        for (WorkbenchOption option : OPTIONS) {
            if (option.matches(input) && input.getCount() >= option.inputCount()) {
                result.add(option);
            }
        }
        return result;
    }

    public static boolean hasAnyMatch(ItemStack input) {
        if (input.isEmpty()) return false;

        for (WorkbenchOption option : OPTIONS) {
            if (option.matches(input) && input.getCount() >= option.inputCount()) {
                return true;
            }
        }
        return false;
    }*/
}