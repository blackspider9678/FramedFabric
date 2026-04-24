package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FramedMiniCubeBlock extends AbstractFramedEntityBlock {

    public static final MapCodec<FramedMiniCubeBlock> CODEC = simpleCodec(FramedMiniCubeBlock::new);

    // head-like placement props (1.21.11)
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;          // FLOOR / WALL / CEILING
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING; // N/E/S/W
    public static final IntegerProperty ROTATION_16 = BlockStateProperties.ROTATION_16;                // 0..15

    // --- shapes (8x8x8 cube) ---
    private static final VoxelShape SHAPE_CENTER = Block.box(4, 0, 4, 12, 8, 12);

    // wall-mounted: stick out from the wall
    private static final VoxelShape SHAPE_NORTH = Block.box(4, 4, 0, 12, 12, 8);
    private static final VoxelShape SHAPE_SOUTH = Block.box(4, 4, 8, 12, 12, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(0, 4, 4, 8, 12, 12);
    private static final VoxelShape SHAPE_EAST  = Block.box(8, 4, 4, 16, 12, 12);

    public FramedMiniCubeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(ROT, 1)
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH)
                .setValue(ROTATION_16, 0)
        );
    }

    @Override
    protected MapCodec<? extends FramedMiniCubeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACE, FACING, ROTATION_16);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();

        AttachFace face = switch (side) {
            case DOWN -> AttachFace.CEILING;
            case UP   -> AttachFace.FLOOR;
            default   -> AttachFace.WALL;
        };

        BlockState s = this.defaultBlockState().setValue(FACE, face);

        if (face == AttachFace.WALL) {
            // Wall: face away from the wall
            Direction f = side.getOpposite();
            if (!f.getAxis().isHorizontal()) f = ctx.getHorizontalDirection().getOpposite();
            s = s.setValue(FACING, f).setValue(ROTATION_16, 0);
        } else {
            // Floor/Ceiling: 16-step yaw like skulls
            Player p = ctx.getPlayer();
            float yaw = (p != null) ? p.getYRot() : 0.0f;
            int rot = rotationFromYaw(yaw);

            s = s.setValue(ROTATION_16, rot)
                    .setValue(FACING, ctx.getHorizontalDirection().getOpposite()); // optional, but keeps state stable
        }

        return s;
    }

    private static int rotationFromYaw(float yaw) {
        return ((int) Math.floor((yaw * 16.0f / 360.0f) + 0.5f)) & 15;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(FACE) == AttachFace.WALL) {
            return switch (state.getValue(FACING)) {
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
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (state.getValue(FACE) == AttachFace.WALL) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        } else {
            int r = state.getValue(ROTATION_16);
            return state.setValue(ROTATION_16, (r + rotationToSteps(rotation)) & 15);
        }
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (state.getValue(FACE) == AttachFace.WALL) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        } else {
            int r = state.getValue(ROTATION_16);
            return state.setValue(ROTATION_16, mirrorRotation(r, mirror));
        }
    }

    private static int rotationToSteps(Rotation rot) {
        return switch (rot) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 4;
            case CLOCKWISE_180 -> 8;
            case COUNTERCLOCKWISE_90 -> 12;
        };
    }

    private static int mirrorRotation(int r, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> (16 - r) & 15;
            case FRONT_BACK -> (8 - r) & 15;
            default -> r;
        };
    }
}
