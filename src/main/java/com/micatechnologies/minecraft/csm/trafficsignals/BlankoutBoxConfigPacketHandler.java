package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BlankoutBoxConfigPacketHandler
        implements IMessageHandler<BlankoutBoxConfigPacket, IMessage> {

    @Override
    public IMessage onMessage( BlankoutBoxConfigPacket message, MessageContext ctx ) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.server.addScheduledTask( () -> {
            if ( !CsmPacketUtils.canPlayerReach( player, message.getPos() ) ) {
                return;
            }
            World world = player.world;
            TileEntity te = world.getTileEntity( message.getPos() );
            if ( !( te instanceof TileEntityBlankoutBox ) ) {
                return;
            }

            int ordinal = message.getActionOrdinal();
            if ( ordinal < 0 || ordinal >= BlankoutBoxConfigAction.values().length ) {
                return;
            }

            TileEntityBlankoutBox boTe = (TileEntityBlankoutBox) te;
            switch ( BlankoutBoxConfigAction.values()[ ordinal ] ) {
                case CYCLE_BODY_COLOR:
                    boTe.getNextBodyPaintColor();
                    break;
                case CYCLE_VISOR_COLOR:
                    boTe.getNextVisorPaintColor();
                    break;
                case CYCLE_VISOR_TYPE:
                    boTe.getNextVisorType();
                    break;
                case CYCLE_MOUNT_TYPE:
                    boTe.getNextMountType();
                    break;
                case CYCLE_BODY_TILT:
                    boTe.getNextBodyTilt();
                    break;
                case CYCLE_SIGN_TYPE:
                    boTe.getNextBlankoutType();
                    break;
            }
        } );
        return null;
    }
}
