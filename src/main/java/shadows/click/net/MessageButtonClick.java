package shadows.click.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import shadows.click.block.gui.ContainerAutoClick;

public class MessageButtonClick implements IMessage {

	int button;

	public MessageButtonClick(int button) {
		this.button = button;
	}

	public MessageButtonClick() {

	}

	@Override
	public void fromBytes(ByteBuf buf) {
		button = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(button);
	}

	public int getButton() {
		return button;
	}

	public static class ButtonClickHandler implements IMessageHandler<MessageButtonClick, IMessage> {

		@Override
		public MessageButtonClick onMessage(MessageButtonClick message, MessageContext ctx) {
			FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
				Container c = ctx.getServerHandler().player.openContainer;
				if (c instanceof ContainerAutoClick) {
					((ContainerAutoClick) c).handleButtonClick(message.getButton());
				}
			});
			return null;
		}

	}

}
