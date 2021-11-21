package shadows.click.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class BlockAutoClick extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public BlockAutoClick() {
		super(AbstractBlock.Properties.of(Material.METAL).strength(5));
		this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, true));
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new TileAutoClick();
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}

	@Override
	public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
		return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}

	@Override
	public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		TileEntity te = world.getBlockEntity(pos);
		if (!world.isClientSide && te instanceof TileAutoClick && placer instanceof PlayerEntity) {
			((TileAutoClick) te).setPlayer((PlayerEntity) placer);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		PlayerEntity placer = context.getPlayer();
		Direction face = placer.getDirection().getOpposite();
		if (placer.xRot > 50) face = Direction.UP;
		else if (placer.xRot < -50) face = Direction.DOWN;
		return defaultBlockState().setValue(FACING, face);
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		TileEntity te = world.getBlockEntity(pos);
		if (te instanceof INamedContainerProvider && !world.isClientSide) NetworkHooks.openGui((ServerPlayerEntity) player, (INamedContainerProvider) te, buf -> buf.writeBlockPos(pos));
		return ActionResultType.SUCCESS;
	}

	@Override
	@Deprecated
	public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() == this && newState.getBlock() == this) return;
		TileEntity te = world.getBlockEntity(pos);
		if (te instanceof TileAutoClick) popResource(world, pos, ((TileAutoClick) te).held.getStackInSlot(0));
		super.onRemove(state, world, pos, newState, isMoving);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState pState) {
		return true;
	}

	@Override
	@Deprecated
	public int getAnalogOutputSignal(BlockState pBlockState, World pLevel, BlockPos pPos) {
		TileEntity te = pLevel.getBlockEntity(pPos);
		if (te instanceof TileAutoClick) {
			ItemStack i = ((TileAutoClick) te).held.getStackInSlot(0);
			if (i.isEmpty()) return 0;
			if (i.getMaxStackSize() == 1 && i.isDamageableItem()) {
				return MathHelper.floor(15F * (i.getMaxDamage() - i.getDamageValue()) / i.getMaxDamage());
			} else return MathHelper.floor(15F * i.getCount() / i.getMaxStackSize());
		}
		return 0;
	}

}
