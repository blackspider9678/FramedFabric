package com.spider.framedfabric.screen;

import com.spider.framedfabric.net.payload.WoodWorkbenchRecipesPayload;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.registry.ModRecipeTypes;
import com.spider.framedfabric.registry.ModScreenHandlers;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.List;

public class WoodWorkbenchScreenHandler extends ScreenHandler {
    private final Inventory input = new SimpleInventory(1) {
        @Override
        public void markDirty() {
            super.markDirty();

            if (WoodWorkbenchScreenHandler.this.world != null && !WoodWorkbenchScreenHandler.this.world.isClient()) {
                var server = WoodWorkbenchScreenHandler.this.world.getServer();
                if (server != null) {
                    server.execute(WoodWorkbenchScreenHandler.this::onInputChanged);
                }
            }
        }
    };

    private final World world;
    private ItemStack lastRecipeInput = ItemStack.EMPTY;

    private final CraftingResultInventory result = new CraftingResultInventory();
    private final ScreenHandlerContext context;
    private final PropertyDelegate properties = new ArrayPropertyDelegate(1);
    private List<RecipeEntry<WoodWorkbenchRecipe>> availableRecipes = List.of();

    private int displayRevision = 0;
    private int lastAppliedClientRevision = -1;
    private List<ItemStack> lastSentDisplayRecipes = List.of();

    private List<ItemStack> displayRecipes = List.of();

    public List<ItemStack> getDisplayRecipes() {
        return this.displayRecipes;
    }

    public WoodWorkbenchScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    private static List<RecipeEntry<WoodWorkbenchRecipe>> findMatchingRecipes(World world, ItemStack stack) {
        if (stack.isEmpty()) return List.of();

        return world.getRecipeManager()
                .getSynchronizedRecipes()
                .getAllOfType(ModRecipeTypes.WOOD_WORKBENCH)
                .stream()
                .filter(entry -> entry.value().matchesStack(stack))
                .toList();
    }

    public void setClientDisplayRecipes(List<ItemStack> recipes) {
        this.displayRecipes = recipes.stream()
                .map(ItemStack::copy)
                .toList();

        if (this.displayRecipes.isEmpty()) {
            this.properties.set(0, -1);
        } else if (this.getSelectedIndex() < 0 || this.getSelectedIndex() >= this.displayRecipes.size()) {
            this.properties.set(0, 0);
        }
    }

    public WoodWorkbenchScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.WOOD_WORKBENCH, syncId);
        this.context = context;
        this.world = playerInventory.player.getEntityWorld();

        // 0 = selected recipe index
        this.properties.set(0, -1);
        this.addProperties(this.properties);



        // Input slot
        this.addSlot(new Slot(this.input, 0, 20, 33) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return true;
            }
        });

        // Result slot
        this.addSlot(new Slot(this.result, 0, 143, 33) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerEntity) {
                return hasValidSelection();
            }

            @Override
            public void onTakeItem(PlayerEntity playerEntity, ItemStack stack) {
                craftSelected(playerEntity);
                super.onTakeItem(playerEntity, stack);
            }
        });

        // Player inventory
        int startX = 8;
        int startY = 84;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        startX + col * 18, startY + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }

        onInputChanged();
    }

    public ItemStack getInputStack() {
        return this.getSlot(0).getStack();
    }

    public List<RecipeEntry<WoodWorkbenchRecipe>> getAvailableRecipes() {
        return this.availableRecipes;
    }

    public int getOptionCount() {
        return this.displayRecipes.size();
    }

    private void refreshAvailableRecipes() {
        ItemStack inputStack = this.input.getStack(0);

        if (inputStack.isEmpty()) {
            this.availableRecipes = List.of();
            this.displayRecipes = List.of();
            syncDisplayRecipes();
            return;
        }

        this.availableRecipes = findMatchingRecipes(this.world, inputStack);
        this.displayRecipes = this.availableRecipes.stream()
                .map(entry -> entry.value().getResultStack())
                .toList();

        syncDisplayRecipes();
    }

    private void syncDisplayRecipes() {
        if (this.world.isClient()) return;
        if (!(this.world instanceof net.minecraft.server.world.ServerWorld serverWorld)) return;

        List<ItemStack> snapshot = this.displayRecipes.stream()
                .map(ItemStack::copy)
                .toList();

        for (PlayerEntity player : serverWorld.getPlayers()) {
            if (player.currentScreenHandler == this && player instanceof ServerPlayerEntity serverPlayer) {
                ServerPlayNetworking.send(
                        serverPlayer,
                        new WoodWorkbenchRecipesPayload(this.syncId, snapshot)
                );
            }
        }
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.areItemsAndComponentsEqual(a.get(i), b.get(i))) return false;
            if (a.get(i).getCount() != b.get(i).getCount()) return false;
        }

        return true;
    }

    public void refreshRecipesIfNeeded() {
        if (!this.world.isClient()) {
            return;
        }

        ItemStack current = this.getSlot(0).getStack();

        if (!ItemStack.areItemsAndComponentsEqual(current, this.lastRecipeInput)) {
            this.lastRecipeInput = current.copy();

            if (current.isEmpty()) {
                this.displayRecipes = List.of();
                this.properties.set(0, -1);
                this.result.setStack(0, ItemStack.EMPTY);
            }
        }
    }

    public int getSelectedIndex() {
        return this.properties.get(0);
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < this.availableRecipes.size()) {
            this.properties.set(0, index);
        } else {
            this.properties.set(0, -1);
        }
        updateResult();
        sendContentUpdates();
    }

    public boolean hasInputItem() {
        return !this.getSlot(0).getStack().isEmpty();
    }

    private boolean hasValidSelection() {
        int selected = getSelectedIndex();
        return selected >= 0
                && selected < this.availableRecipes.size()
                && !this.input.getStack(0).isEmpty();
    }

    private void onInputChanged() {
        refreshAvailableRecipes();

        int selected = getSelectedIndex();

        if (this.availableRecipes.isEmpty()) {
            this.properties.set(0, -1);
        } else if (selected < 0 || selected >= this.availableRecipes.size()) {
            this.properties.set(0, 0);
        }

        updateResult();
        sendContentUpdates();
    }

    private void updateResult() {
        int selected = getSelectedIndex();

        if (selected < 0 || selected >= this.availableRecipes.size()) {
            this.result.setStack(0, ItemStack.EMPTY);
            sendContentUpdates();
            return;
        }

        WoodWorkbenchRecipe recipe = this.availableRecipes.get(selected).value();
        this.result.setStack(0, recipe.getResultStack());
        sendContentUpdates();
    }

    private void craftSelected(PlayerEntity player) {
        int selected = getSelectedIndex();

        if (selected < 0 || selected >= this.availableRecipes.size()) return;

        WoodWorkbenchRecipe recipe = this.availableRecipes.get(selected).value();
        ItemStack inputStack = this.input.getStack(0);

        if (inputStack.isEmpty() || inputStack.getCount() < recipe.getInputCount()) {
            this.result.setStack(0, ItemStack.EMPTY);
            return;
        }

        inputStack.decrement(recipe.getInputCount());

        if (inputStack.isEmpty()) {
            this.input.setStack(0, ItemStack.EMPTY);
        }

        onInputChanged();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= 0 && id < this.availableRecipes.size()) {
            setSelectedIndex(id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot == null || !slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getStack();
        newStack = original.copy();

        // Result slot
        if (slotIndex == 1) {
            original.getItem().onCraftByPlayer(original, player);
            if (!this.insertItem(original, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickTransfer(original, newStack);
        }
        // Input slot
        else if (slotIndex == 0) {
            if (!this.insertItem(original, 2, 38, false)) {
                return ItemStack.EMPTY;
            }
        }
        // Player inventory
        else {
            if (this.slots.get(0).getStack().isEmpty()) {
                if (!this.insertItem(original, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= 2 && slotIndex < 29) {
                if (!this.insertItem(original, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= 29 && slotIndex < 38) {
                if (!this.insertItem(original, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        if (original.getCount() == newStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTakeItem(player, original);
        return newStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.context.run((world, pos) -> this.dropInventory(player, this.input));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(this.context, player, ModBlocks.WOOD_WORKBENCH);
    }
}