package com.spider.framedfabric.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class WoodWorkbenchScreen extends AbstractContainerScreen<WoodWorkbenchScreenHandler> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");

    private static final int SCROLLER_HEIGHT = 15;
    private static final int RECIPES_COLUMNS = 4;
    private static final int RECIPES_ROWS = 3;
    private static final int RECIPES_VISIBLE = RECIPES_COLUMNS * RECIPES_ROWS;

    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private boolean displayRecipes;

    public WoodWorkbenchScreen(WoodWorkbenchScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166);
        handler.registerUpdateListener(this::containerChanged);
        this.inventoryLabelY = this.imageHeight - 94;
        this.containerChanged();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);

        int x = this.leftPos;
        int y = this.topPos;

        context.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x, y,
                0, 0,
                this.imageWidth, this.imageHeight,
                256, 256
        );

        int scrollBarOffset = (int) (41.0f * this.scrollOffs);
        Identifier scrollBarSprite = isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        int scrollBarX = x + 119;
        int scrollBarY = y + 15;

        context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                scrollBarSprite,
                scrollBarX,
                scrollBarY + scrollBarOffset,
                12,
                SCROLLER_HEIGHT
        );

        if (mouseX >= scrollBarX && mouseY >= scrollBarY && mouseX < scrollBarX + 12 && mouseY < scrollBarY + 54) {
            if (isScrollBarActive()) {
                context.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            } else {
                context.requestCursor(CursorTypes.NOT_ALLOWED);
            }
        }

        renderRecipeButtons(context, mouseX, mouseY, x, y);
    }

    private void renderRecipeButtons(GuiGraphicsExtractor context, int mouseX, int mouseY, int x, int y) {
        List<ItemStack> recipes = this.menu.getDisplayRecipes();
        int endIndex = Math.min(this.startIndex + RECIPES_VISIBLE, recipes.size());

        for (int visibleIndex = 0; visibleIndex < endIndex - this.startIndex; visibleIndex++) {
            int recipeIndex = this.startIndex + visibleIndex;
            int col = visibleIndex % RECIPES_COLUMNS;
            int row = visibleIndex / RECIPES_COLUMNS;

            int buttonX = x + 52 + col * 16;
            int buttonY = y + 14 + row * 18;

            boolean hovered = mouseX >= buttonX && mouseY >= buttonY && mouseX < buttonX + 16 && mouseY < buttonY + 18;
            boolean selected = recipeIndex == this.menu.getSelectedIndex();

            drawRecipeButton(context, buttonX, buttonY, selected, hovered);
            context.item(recipes.get(recipeIndex), buttonX, buttonY + 1);
        }
    }

    private void drawRecipeButton(GuiGraphicsExtractor context, int x, int y, boolean selected, boolean hovered) {
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
            context.fill(x, y, x + 16, y + 1, borderLight);
            context.fill(x, y, x + 1, y + 18, borderLight);
            context.fill(x, y + 17, x + 16, y + 18, borderDark);
            context.fill(x + 15, y, x + 16, y + 18, borderDark);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        this.scrolling = false;

        double mouseX = click.x();
        double mouseY = click.y();

        if (this.displayRecipes) {
            int x = this.leftPos + 52;
            int y = this.topPos + 14;

            for (int visibleIndex = 0; visibleIndex < RECIPES_VISIBLE; visibleIndex++) {
                int recipeIndex = this.startIndex + visibleIndex;
                if (recipeIndex >= this.menu.getOptionCount()) break;

                int col = visibleIndex % RECIPES_COLUMNS;
                int row = visibleIndex / RECIPES_COLUMNS;

                double dx = mouseX - (x + col * 16);
                double dy = mouseY - (y + row * 18);

                if (dx >= 0.0 && dy >= 0.0 && dx < 16.0 && dy < 18.0) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, recipeIndex);
                    return true;
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
        if (this.scrolling && isScrollBarActive()) {
            double mouseY = click.y();

            int top = this.topPos + 14;
            int bottom = top + 54;
            this.scrollOffs = ((float) mouseY - (float) top - 7.5f) / ((float) (bottom - top) - 15.0f);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0f, 1.0f);
            this.startIndex = (int) (this.scrollOffs * (float) this.getOffscreenRows() + 0.5f) * RECIPES_COLUMNS;
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        if (isScrollBarActive()) {
            int offscreenRows = this.getOffscreenRows();
            float step = (float) verticalAmount / (float) offscreenRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - step, 0.0f, 1.0f);
            this.startIndex = (int) (this.scrollOffs * (float) offscreenRows + 0.5f) * RECIPES_COLUMNS;
        }

        return true;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        List<ItemStack> recipes = this.menu.getDisplayRecipes();
        int endIndex = Math.min(this.startIndex + RECIPES_VISIBLE, recipes.size());

        for (int visibleIndex = 0; visibleIndex < endIndex - this.startIndex; visibleIndex++) {
            int recipeIndex = this.startIndex + visibleIndex;
            int col = visibleIndex % RECIPES_COLUMNS;
            int row = visibleIndex / RECIPES_COLUMNS;

            int buttonX = this.leftPos + 52 + col * 16;
            int buttonY = this.topPos + 14 + row * 18;

            if (mouseX >= buttonX && mouseX < buttonX + 16 && mouseY >= buttonY && mouseY < buttonY + 18) {
                context.setTooltipForNextFrame(this.font, recipes.get(recipeIndex), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.scrolling = false;
        return super.mouseReleased(click);
    }

    private boolean isScrollBarActive() {
        return this.displayRecipes && this.menu.getOptionCount() > RECIPES_VISIBLE;
    }

    private int getOffscreenRows() {
        return (this.menu.getOptionCount() + RECIPES_COLUMNS - 1) / RECIPES_COLUMNS - RECIPES_ROWS;
    }

    private void containerChanged() {
        this.displayRecipes = this.menu.hasInputItem();
        this.scrollOffs = 0.0f;
        this.startIndex = 0;
    }
}
