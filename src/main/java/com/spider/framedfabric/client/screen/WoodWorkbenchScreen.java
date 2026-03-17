package com.spider.framedfabric.client.screen;

import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import net.minecraft.recipe.RecipeEntry;

import java.util.List;

public class WoodWorkbenchScreen extends HandledScreen<WoodWorkbenchScreenHandler> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/stonecutter.png");

    private static final int SCROLLER_HEIGHT = 15;
    private static final int RECIPES_COLUMNS = 4;
    private static final int RECIPES_ROWS = 3;
    private static final int RECIPES_VISIBLE = RECIPES_COLUMNS * RECIPES_ROWS;

    private float scrollAmount;
    private boolean mouseClicked;
    private int scrollOffset;



    public WoodWorkbenchScreen(WoodWorkbenchScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = 8;
        this.titleY = 6;
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();

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
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x, y,
                0, 0,
                this.backgroundWidth, this.backgroundHeight,
                256, 256
        );

        int scrollbarY = y + 15 + (int) (41.0f * this.scrollAmount);
        int v = shouldShowScrollbar() ? 0 : 12;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x + 119, scrollbarY,
                176, v,
                12, SCROLLER_HEIGHT,
                256, 256
        );

        renderRecipeButtons(context, mouseX, mouseY, x, y);
    }

    private void renderRecipeButtons(DrawContext context, int mouseX, int mouseY, int x, int y) {
        List<ItemStack> recipes = this.handler.getDisplayRecipes();
        int start = this.scrollOffset;
        int end = Math.min(start + RECIPES_VISIBLE, recipes.size());

        for (int visibleIndex = 0; visibleIndex < end - start; visibleIndex++) {
            int recipeIndex = start + visibleIndex;
            int col = visibleIndex % RECIPES_COLUMNS;
            int row = visibleIndex / RECIPES_COLUMNS;

            int buttonX = x + 52 + col * 16;
            int buttonY = y + 14 + row * 18;

            boolean hovered = mouseX >= buttonX && mouseY >= buttonY && mouseX < buttonX + 16 && mouseY < buttonY + 18;
            boolean selected = recipeIndex == this.handler.getSelectedIndex();

            drawRecipeButton(context, buttonX, buttonY, selected, hovered);

            ItemStack stack = recipes.get(recipeIndex);
            context.drawItem(stack, buttonX, buttonY + 1);
        }
    }

    private void drawRecipeButton(DrawContext context, int x, int y, boolean selected, boolean hovered) {
        int fill;
        int borderLight;
        int borderDark;

        if (selected) {
            fill = 0xFFD6CAA3;
            borderLight = 0xFFF3E7BE;
            borderDark = 0xFF8C7A4F;
        } else if (hovered) {
            fill = 0xFFC8BC96;
            borderLight = 0xFFE8DCB2;
            borderDark = 0xFF7D6D46;
        } else {
            fill = 0x00000000;
            borderLight = 0x00000000;
            borderDark = 0x00000000;
        }

        if (fill != 0) {
            context.fill(x, y, x + 16, y + 18, fill);

            // top
            context.fill(x, y, x + 16, y + 1, borderLight);
            // left
            context.fill(x, y, x + 1, y + 18, borderLight);
            // bottom
            context.fill(x, y + 17, x + 16, y + 18, borderDark);
            // right
            context.fill(x + 15, y, x + 16, y + 18, borderDark);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        this.mouseClicked = false;

        double mouseX = click.x();
        double mouseY = click.y();

        if (hasInputAndOptions()) {
            int x = this.x + 52;
            int y = this.y + 14;

            for (int visibleIndex = 0; visibleIndex < RECIPES_VISIBLE; visibleIndex++) {
                int recipeIndex = this.scrollOffset + visibleIndex;
                if (recipeIndex >= this.handler.getOptionCount()) break;

                int col = visibleIndex % RECIPES_COLUMNS;
                int row = visibleIndex / RECIPES_COLUMNS;

                double dx = mouseX - (x + col * 16);
                double dy = mouseY - (y + row * 18);

                if (dx >= 0.0 && dy >= 0.0 && dx < 16.0 && dy < 18.0) {
                    this.client.interactionManager.clickButton(this.handler.syncId, recipeIndex);
                    return true;
                }
            }

            int scrollX1 = this.x + 119;
            int scrollY1 = this.y + 9;
            int scrollX2 = scrollX1 + 12;
            int scrollY2 = scrollY1 + 54;

            if (mouseX >= scrollX1 && mouseX < scrollX2 && mouseY >= scrollY1 && mouseY < scrollY2) {
                this.mouseClicked = true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.mouseClicked && shouldShowScrollbar()) {
            double mouseY = click.y();

            int top = this.y + 14;
            int bottom = top + 54;
            this.scrollAmount = ((float) mouseY - (float) top - 7.5f) / ((float) (bottom - top) - 15.0f);
            this.scrollAmount = Math.max(0.0f, Math.min(1.0f, this.scrollAmount));
            this.scrollOffset = Math.round(this.scrollAmount * getMaxScroll());
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (shouldShowScrollbar()) {
            int maxScroll = getMaxScroll();
            float step = (float) verticalAmount / (float) maxScroll;
            this.scrollAmount = Math.max(0.0f, Math.min(1.0f, this.scrollAmount - step));
            this.scrollOffset = Math.round(this.scrollAmount * maxScroll);
        }

        return true;
    }

    private boolean hasInputAndOptions() {
        return this.handler.hasInputItem() && this.handler.getOptionCount() > 0;
    }

    private boolean shouldShowScrollbar() {
        return this.handler.getOptionCount() > RECIPES_VISIBLE;
    }

    private int getMaxScroll() {
        return Math.max(0, (this.handler.getOptionCount() + RECIPES_COLUMNS - 1) / RECIPES_COLUMNS - RECIPES_ROWS);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        List<ItemStack> recipes = this.handler.getDisplayRecipes();
        int start = this.scrollOffset;
        int end = Math.min(start + RECIPES_VISIBLE, recipes.size());

        for (int visibleIndex = 0; visibleIndex < end - start; visibleIndex++) {
            int recipeIndex = start + visibleIndex;
            int col = visibleIndex % RECIPES_COLUMNS;
            int row = visibleIndex / RECIPES_COLUMNS;

            int buttonX = this.x + 52 + col * 16;
            int buttonY = this.y + 14 + row * 18;

            if (mouseX >= buttonX && mouseX < buttonX + 16 && mouseY >= buttonY && mouseY < buttonY + 18) {
                context.drawItemTooltip(
                        this.textRenderer,
                        recipes.get(recipeIndex),
                        mouseX,
                        mouseY
                );
                break;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}