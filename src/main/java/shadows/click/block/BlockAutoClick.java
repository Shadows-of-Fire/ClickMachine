package shadows.click.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import shadows.click.block.gui.AutoClickContainer;
import shadows.placebo.block_entity.TickingEntityBlock;
import shadows.placebo.container.ContainerUtil;

public class BlockAutoClick extends Block implements TickingEntityBlock {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public BlockAutoClick() {
		super(BlockBehaviour.Properties.of(Material.METAL).strength(5));
		this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, true));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
		return new TileAutoClick(pPos, pState);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}

	@Override
	public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation direction) {
		return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BlockEntity te = world.getBlockEntity(pos);
		if (!world.isClientSide && te instanceof TileAutoClick && placer instanceof Player) {
			((TileAutoClick) te).setPlayer((Player) placer);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Player placer = context.getPlayer();
		Direction face = placer.getDirection().getOpposite();
		if (placer.getXRot() > 50) face = Direction.UP;
		else if (placer.getXRot() < -50) face = Direction.DOWN;
		return this.defaultBlockState().setValue(FACING, face);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
		return ContainerUtil.openGui(player, pos, AutoClickContainer::new);
	}

	@Override
	@Deprecated
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() == this && newState.getBlock() == this) return;
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof TileAutoClick) popResource(world, pos, ((TileAutoClick) te).held.getStackInSlot(0));
		super.onRemove(state, world, pos, newState, isMoving);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState pState) {
		return true;
	}

	@Override
	@Deprecated
	public int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
		BlockEntity te = pLevel.getBlockEntity(pPos);
		if (te instanceof TileAutoClick) {
			ItemStack i = ((TileAutoClick) te).held.getStackInSlot(0);
			if (i.isEmpty()) return 0;
			if (i.getMaxStackSize() == 1 && i.isDamageableItem()) {
				return Mth.floor(15F * (i.getMaxDamage() - i.getDamageValue()) / i.getMaxDamage());
			} else return Mth.floor(15F * i.getCount() / i.getMaxStackSize());
		}
		return 0;
	}

}
