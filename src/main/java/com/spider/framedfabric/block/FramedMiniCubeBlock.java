package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class FramedMiniCubeBlock extends AbstractFramedEntityBlock {

    public static final MapCodec<FramedMiniCubeBlock> CODEC = createCodec(FramedMiniCubeBlock::new);

    // head-like placement props (1.21.11)
    public static final EnumProperty<BlockFace> FACE = Properties.BLOCK_FACE;          // FLOOR / WALL / CEILING
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING; // N/E/S/W
    public static final IntProperty ROTATION_16 = Properties.ROTATION;                // 0..15

    // --- shapes (8x8x8 cube) ---
    private static final VoxelShape SHAPE_CENTER = Block.createCuboidShape(4, 0, 4, 12, 8, 12);

    // wall-mounted: stick out from the wall
    private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(4, 4, 0, 12, 12, 8);
    private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(4, 4, 8, 12, 12, 16);
    private static final VoxelShape SHAPE_WEST  = Block.createCuboidShape(0, 4, 4, 8, 12, 12);
    private static final VoxelShape SHAPE_EAST  = Block.createCuboidShape(8, 4, 4, 16, 12, 12);

    public FramedMiniCubeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(ROT, 1)
                .with(FACE, BlockFace.FLOOR)
                .with(FACING, Direction.NORTH)
                .with(ROTATION_16, 0)
        );
    }

    @Override
    protected MapCodec<? extends FramedMiniCubeBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACE, FACING, ROTATION_16);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();

        BlockFace face = switch (side) {
            case DOWN -> BlockFace.CEILING;
            case UP   -> BlockFace.FLOOR;
            default   -> BlockFace.WALL;
        };

        BlockState s = this.getDefaultState().with(FACE, face);

        if (face == BlockFace.WALL) {
            // Wall: face away from the wall
            Direction f = side.getOpposite();
            if (!f.getAxis().isHorizontal()) f = ctx.getHorizontalPlayerFacing().getOpposite();
            s = s.with(FACING, f).with(ROTATION_16, 0);
        } else {
            // Floor/Ceiling: 16-step yaw like skulls
            PlayerEntity p = ctx.getPlayer();
            float yaw = (p != null) ? p.getYaw() : 0.0f;
            int rot = rotationFromYaw(yaw);

            s = s.with(ROTATION_16, rot)
                    .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()); // optional, but keeps state stable
        }

        return s;
    }

    private static int rotationFromYaw(float yaw) {
        return ((int) Math.floor((yaw * 16.0f / 360.0f) + 0.5f)) & 15;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(FACE) == BlockFace.WALL) {
            return switch (state.get(FACING)) {
                case NORTH -> SHAPE_NORTH;
                case SOUTH -> SHAPE_SOUTH;
                case WEST  -> SHAPE_WEST;
                case EAST  -> SHAPE_EAST;
                default    -> SHAPE_CENTER;
            };
        }
        return SHAPE_CENTER;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        if (state.get(FACE) == BlockFace.WALL) {
            return state.with(FACING, rotation.rotate(state.get(FACING)));
        } else {
            int r = state.get(ROTATION_16);
            return state.with(ROTATION_16, (r + rotationToSteps(rotation)) & 15);
        }
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        if (state.get(FACE) == BlockFace.WALL) {
            return state.rotate(mirror.getRotation(state.get(FACING)));
        } else {
            int r = state.get(ROTATION_16);
            return state.with(ROTATION_16, mirrorRotation(r, mirror));
        }
    }

    private static int rotationToSteps(BlockRotation rot) {
        return switch (rot) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 4;
            case CLOCKWISE_180 -> 8;
            case COUNTERCLOCKWISE_90 -> 12;
        };
    }

    private static int mirrorRotation(int r, BlockMirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> (16 - r) & 15;
            case FRONT_BACK -> (8 - r) & 15;
            default -> r;
        };
    }
}
