/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.devices;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraftforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.ICalendar;
import org.jetbrains.annotations.Nullable;

/**
 * todo: improve the voxel shape to actually contact the mud bricks
 * todo: if we do the above placement becomes a bit more difficult - so, make sure that mud brick placement still works as smoothly as possible, without requiring the player to target awkward shapes
 */
public class DryingBricksBlock extends DeviceBlock
{
    public static final IntegerProperty COUNT = TFCBlockStateProperties.COUNT_1_4;
    public static final BooleanProperty DRIED = TFCBlockStateProperties.DRIED;

    public static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    private final Supplier<? extends Item> dryItem;

    public DryingBricksBlock(ExtendedProperties properties, Supplier<? extends Item> dryItem)
    {
        super(properties, InventoryRemoveBehavior.NOOP);
        this.dryItem = dryItem;
        registerDefaultState(getStateDefinition().any().setValue(COUNT, 1).setValue(DRIED, false));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return level.getBlockState(pos).getBlock() == this || !canSurvive(level, pos) ? null : super.getStateForPlacement(context);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        final ItemStack held = player.getItemInHand(hand);
        if (Helpers.isItem(held, asItem()) && !player.isShiftKeyDown() && !state.getValue(DRIED))
        {
            final int count = state.getValue(COUNT);
            if (count < 4)
            {
                level.setBlockAndUpdate(pos, state.setValue(COUNT, count + 1));
                final SoundType soundType = getSoundType(state, level, pos, player);
                level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1f) / 2f, soundType.getPitch() * 0.8f);
                TickCounterBlockEntity.reset(level, pos);
                if (!player.isCreative())
                {
                    held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        else if (held.isEmpty() && player.isShiftKeyDown())
        {
            int count = state.getValue(COUNT);
            ItemStack drop = new ItemStack(state.getValue(DRIED) ? dryItem.get() : asItem());
            ItemHandlerHelper.giveItemToPlayer(player, drop);
            if (count > 1)
            {
                level.setBlockAndUpdate(pos, state.setValue(COUNT, count - 1));
            }
            else
            {
                level.destroyBlock(pos, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        TickCounterBlockEntity.reset(level, pos);
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos)
    {
        return facing == Direction.DOWN && !facingState.isFaceSturdy(level, facingPos, Direction.UP) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state)
    {
        return !state.getValue(DRIED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random rand)
    {
        level.getBlockEntity(pos, TFCBlockEntities.TICK_COUNTER.get()).ifPresent(counter -> {
            if (level.isRainingAt(pos.above()))
            {
                counter.resetCounter();
            }
            else if (counter.getTicksSinceUpdate() > ICalendar.TICKS_IN_DAY)
            {
                level.setBlockAndUpdate(pos, state.setValue(DRIED, true));
            }
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(COUNT, DRIED));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    private boolean canSurvive(Level level, BlockPos pos)
    {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
