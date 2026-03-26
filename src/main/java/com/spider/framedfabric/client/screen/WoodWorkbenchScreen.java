package com.spider.framedfabric.client.screen;

import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class WoodWorkbenchScreen extends AbstractContainerScreen<WoodWorkbenchScreenHandler> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final Identifier RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final Identifier RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final int SCROLLER_HEIGHT = 15;
    private static final int RECIPES_COLUMNS = 4;
    private static final int RECIPES_ROWS = 3;
    private static final int RECIPES_VISIBLE = RECIPES_COLUMNS * RECIPES_ROWS;

    private float scrollAmount;
    private boolean scrolling;
    private int scrollOffset;

    public WoodWorkbenchScreen(WoodWorkbenchScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.menu.refreshRecipesIfNeeded();

        int maxScroll = getMaxScroll();
        if (this.scrollOffset > maxScroll) {
            this.scrollOffset = maxScroll;
        }

        if (maxScroll <= 0) {
            this.scrollOffset = 0;
            this.scrollAmount = 0.0f;
        } else {
            this.scrollAmount = (float) this.scrollOffset / (float) maxScroll;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x, y,
                0.0F, 0.0F,
                this.imageWidth, this.imageHeight,
                256, 256
        );

        int scrollbarY = y + 15 + (int) (41.0f * this.scrollAmount);
        Identifier scrollerSprite = shouldShowScrollbar() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, scrollerSprite, x + 119, scrollbarY, 12, SCROLLER_HEIGHT);

        extractRecipeButtons(graphics, mouseX, mouseY, x, y);
        extractRecipes(graphics, x, y);
    }

    private void extractRecipeButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y) {
        List<ItemStack> recipes = this.menu.getDisplayRecipes();
        int startIndex = this.scrollOffset * RECIPES_COLUMNS;
        int endIndex = Math.min(startIndex + RECIPES_VISIBLE, recipes.size());

        for (int visibleIndex = 0; visibleIndex < endIndex - startIndex; visibleIndex++) {
            int recipeIndex = startIndex + visibleIndex;
            int col = visibleIndex % RECIPES_COLUMNS;
            int row = visibleIndex / RECIPES_COLUMNS;
            int buttonX = x + 52 + col * 16;
            int buttonY = y + 15 + row * 18;

            boolean hovered = mouseX >= buttonX && mouseY >= buttonY - 1 && mouseX < buttonX + 16 && mouseY < buttonY + 17;
            boolean selected = recipeIndex == this.menu.getSelectedIndex();

            drawRecipeButton(graphics, buttonX, buttonY, selected, hovered);
        }
    }

    private void extractRecipes(GuiGraphicsExtractor graphics, int x, int y) {
        List<ItemStack> recipes = this.menu.getDisplayRecipes();
        int startIndex = this.scrollOffset * RECIPES_COLUMNS;
        int endIndex = Math.min(startIndex + RECIPES_VISIBLE, recipes.size());

        for (int visibleIndex = 0; visibleIndex < endIndex - startIndex; visibleIndex++) {
            int recipeIndex = startIndex + visibleIndex;
            int col = visibleIndex % RECIPES_COLUMNS;
            int row = visibleIndex / RECIPES_COLUMNS;
            int buttonX = x + 52 + col * 16;
            int buttonY = y + 16 + row * 18;
            graphics.item(recipes.get(recipeIndex), buttonX, buttonY);
        }
    }

    private void drawRecipeButton(GuiGraphicsExtractor graphics, int x, int y, boolean selected, boolean hovered) {
        Identifier sprite = selected
                ? RECIPE_SELECTED_SPRITE
                : hovered ? RECIPE_HIGHLIGHTED_SPRITE : RECIPE_SPRITE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y - 1, 16, 18);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        this.scrolling = false;

        double mouseX = click.x();
        double mouseY = click.y();

        if (hasInputAndOptions()) {
            int x = this.leftPos + 52;
            int y = this.topPos + 14;
            int startIndex = this.scrollOffset * RECIPES_COLUMNS;

            for (int visibleIndex = 0; visibleIndex < RECIPES_VISIBLE; visibleIndex++) {
                int recipeIndex = startIndex + visibleIndex;
                if (recipeIndex >= this.menu.getOptionCount()) {
                    break;
                }

                int col = visibleIndex % RECIPES_COLUMNS;
                int row = visibleIndex / RECIPES_COLUMNS;
                double dx = mouseX - (x + col * 16);
                double dy = mouseY - (y + row * 18);

                if (dx >= 0.0 && dy >= 0.0 && dx < 16.0 && dy < 18.0) {
                    if (this.menu.clickMenuButton(this.minecraft.player, recipeIndex)) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, recipeIndex);
                        return true;
                    }
                }
            }

            int scrollX1 = this.leftPos + 119;
            int scrollY1 = this.topPos + 9;
            int scrollX2 = scrollX1 + 12;
            int scrollY2 = scrollY1 + 54;

            if (mouseX >= scrollX1 && mouseX < scrollX2 && mouseY >= scrollY1 && mouseY < scrollY2) {
                this.scrolling = true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (this.scrolling && shouldShowScrollbar()) {
            float top = this.topPos + 14;
            float bottom = top + 54;
            this.scrollAmount = ((float) click.y() - top - 7.5f) / (bottom - top - 15.0f);
            this.scrollAmount = Mth.clamp(this.scrollAmount, 0.0f, 1.0f);
            this.scrollOffset = Math.round(this.scrollAmount * getMaxScroll());
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        if (shouldShowScrollbar()) {
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                float step = (float) verticalAmount / (float) maxScroll;
                this.scrollAmount = Mth.clamp(this.scrollAmount - step, 0.0f, 1.0f);
                this.scrollOffset = Math.round(this.scrollAmount * maxScroll);
            }
        }

        return true;
    }

    private boolean hasInputAndOptions() {
        return this.menu.hasInputItem() && this.menu.getOptionCount() > 0;
    }

    private boolean shouldShowScrollbar() {
        return this.menu.getOptionCount() > RECIPES_VISIBLE;
    }

    private int getMaxScroll() {
        return Math.max(0, (this.menu.getOptionCount() + RECIPES_COLUMNS - 1) / RECIPES_COLUMNS - RECIPES_ROWS);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        List<ItemStack> recipes = this.menu.getDisplayRecipes();
        int startIndex = this.scrollOffset * RECIPES_COLUMNS;
        int endIndex = Math.min(startIndex + RECIPES_VISIBLE, recipes.size());

        for (int visibleIndex = 0; visibleIndex < endIndex - startIndex; visibleIndex++) {
            int recipeIndex = startIndex + visibleIndex;
            int col = visibleIndex % RECIPES_COLUMNS;
            int row = visibleIndex / RECIPES_COLUMNS;
            int buttonX = this.leftPos + 52 + col * 16;
            int buttonY = this.topPos + 16 + row * 18;

            if (mouseX >= buttonX && mouseX < buttonX + 16 && mouseY >= buttonY && mouseY < buttonY + 18) {
                graphics.setTooltipForNextFrame(this.font, recipes.get(recipeIndex), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.scrolling = false;
        return super.mouseReleased(click);
    }
}
