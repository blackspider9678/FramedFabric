package com.spider.framedfabric.screen;

import com.spider.framedfabric.net.payload.WoodWorkbenchRecipesPayload;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.registry.ModRecipeTypes;
import com.spider.framedfabric.registry.ModScreenHandlers;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import java.util.List;

public class WoodWorkbenchScreenHandler extends AbstractContainerMenu {
    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();

            if (WoodWorkbenchScreenHandler.this.world != null && !WoodWorkbenchScreenHandler.this.world.isClientSide()) {
                var server = WoodWorkbenchScreenHandler.this.world.getServer();
                if (server != null) {
                    server.execute(WoodWorkbenchScreenHandler.this::onInputChanged);
                }
            }
        }
    };

    private final Level world;
    private ItemStack lastRecipeInput = ItemStack.EMPTY;

    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess context;
    private final ContainerData properties = new SimpleContainerData(1);
    private List<RecipeHolder<WoodWorkbenchRecipe>> availableRecipes = List.of();

    private int displayRevision = 0;
    private int lastAppliedClientRevision = -1;
    private List<ItemStack> lastSentDisplayRecipes = List.of();

    private List<ItemStack> displayRecipes = List.of();
    private Runnable slotUpdateListener = () -> {};

    public List<ItemStack> getDisplayRecipes() {
        return this.displayRecipes;
    }

    public WoodWorkbenchScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    private static List<RecipeHolder<WoodWorkbenchRecipe>> findMatchingRecipes(Level world, ItemStack stack) {
        if (stack.isEmpty()) return List.of();

        return world.recipeAccess()
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

        this.slotUpdateListener.run();
    }

    public WoodWorkbenchScreenHandler(int syncId, Inventory playerInventory, ContainerLevelAccess context) {
        super(ModScreenHandlers.WOOD_WORKBENCH, syncId);
        this.context = context;
        this.world = playerInventory.player.level();

        // 0 = selected recipe index
        this.properties.set(0, -1);
        this.addDataSlots(this.properties);



        // Input slot
        this.addSlot(new Slot(this.input, 0, 20, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });

        // Result slot
        this.addSlot(new Slot(this.result, 0, 143, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player playerEntity) {
                return hasValidSelection();
            }

            @Override
            public void onTake(Player playerEntity, ItemStack stack) {
                craftSelected(playerEntity);
                super.onTake(playerEntity, stack);
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
        return this.getSlot(0).getItem();
    }

    public List<RecipeHolder<WoodWorkbenchRecipe>> getAvailableRecipes() {
        return this.availableRecipes;
    }

    public int getOptionCount() {
        return this.displayRecipes.size();
    }

    private void refreshAvailableRecipes() {
        ItemStack inputStack = this.input.getItem(0);

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
        if (this.world.isClientSide()) return;
        if (!(this.world instanceof net.minecraft.server.level.ServerLevel serverWorld)) return;

        List<ItemStack> snapshot = this.displayRecipes.stream()
                .map(ItemStack::copy)
                .toList();

        for (Player player : serverWorld.players()) {
            if (player.containerMenu == this && player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(
                        serverPlayer,
                        new WoodWorkbenchRecipesPayload(this.containerId, snapshot)
                );
            }
        }
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.isSameItemSameComponents(a.get(i), b.get(i))) return false;
            if (a.get(i).getCount() != b.get(i).getCount()) return false;
        }

        return true;
    }

    public void refreshRecipesIfNeeded() {
        if (!this.world.isClientSide()) {
            return;
        }

        ItemStack current = this.getSlot(0).getItem();

        if (!ItemStack.isSameItemSameComponents(current, this.lastRecipeInput)) {
            this.lastRecipeInput = current.copy();

            if (current.isEmpty()) {
                this.displayRecipes = List.of();
                this.properties.set(0, -1);
                this.result.setItem(0, ItemStack.EMPTY);
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
        broadcastChanges();
    }

    public boolean hasInputItem() {
        return !this.getSlot(0).getItem().isEmpty() && !this.displayRecipes.isEmpty();
    }

    public void registerUpdateListener(Runnable listener) {
        this.slotUpdateListener = listener;
    }

    private boolean hasValidSelection() {
        int selected = getSelectedIndex();
        return selected >= 0
                && selected < this.availableRecipes.size()
                && !this.input.getItem(0).isEmpty();
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
        broadcastChanges();
    }

    private void updateResult() {
        int selected = getSelectedIndex();

        if (selected < 0 || selected >= this.availableRecipes.size()) {
            this.result.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }

        WoodWorkbenchRecipe recipe = this.availableRecipes.get(selected).value();
        this.result.setItem(0, recipe.getResultStack());
        broadcastChanges();
    }

    private void craftSelected(Player player) {
        int selected = getSelectedIndex();

        if (selected < 0 || selected >= this.availableRecipes.size()) return;

        WoodWorkbenchRecipe recipe = this.availableRecipes.get(selected).value();
        ItemStack inputStack = this.input.getItem(0);

        if (inputStack.isEmpty() || inputStack.getCount() < recipe.getInputCount()) {
            this.result.setItem(0, ItemStack.EMPTY);
            return;
        }

        inputStack.shrink(recipe.getInputCount());

        if (inputStack.isEmpty()) {
            this.input.setItem(0, ItemStack.EMPTY);
        }

        onInputChanged();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < this.availableRecipes.size()) {
            setSelectedIndex(id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        newStack = original.copy();

        // Result slot
        if (slotIndex == 1) {
            original.getItem().onCraftedBy(original, player);
            if (!this.moveItemStackTo(original, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(original, newStack);
        }
        // Input slot
        else if (slotIndex == 0) {
            if (!this.moveItemStackTo(original, 2, 38, false)) {
                return ItemStack.EMPTY;
            }
        }
        // Player inventory
        else {
            if (this.slots.get(0).getItem().isEmpty()) {
                if (!this.moveItemStackTo(original, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= 2 && slotIndex < 29) {
                if (!this.moveItemStackTo(original, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= 29 && slotIndex < 38) {
                if (!this.moveItemStackTo(original, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (original.getCount() == newStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, original);
        return newStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.context.execute((world, pos) -> this.clearContainer(player, this.input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.context, player, ModBlocks.WOOD_WORKBENCH);
    }
}
