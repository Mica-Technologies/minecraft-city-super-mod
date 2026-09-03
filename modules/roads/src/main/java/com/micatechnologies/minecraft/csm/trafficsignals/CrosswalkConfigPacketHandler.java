package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for crosswalk signal config GUI packets. Applies the requested
 * action to the tile entity.
 */
public class CrosswalkConfigPacketHandler
        implements IMessageHandler<CrosswalkConfigPacket, IMessage> {

    @Override
    public IMessage onMessage( CrosswalkConfigPacket message, MessageContext ctx ) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.server.addScheduledTask( () -> {
            if ( !CsmPacketUtils.canPlayerReach( player, message.getPos() ) ) {
                return;
            }
            World world = player.world;
            TileEntity te = world.getTileEntity( message.getPos() );
            if ( !( te instanceof TileEntityCrosswalkSignalNew ) ) {
                return;
            }

            int ordinal = message.getActionOrdinal();
            if ( ordinal < 0 || ordinal >= CrosswalkConfigAction.values().length ) {
                return;
            }

            TileEntityCrosswalkSignalNew cwTe = (TileEntityCrosswalkSignalNew) te;
            switch ( CrosswalkConfigAction.values()[ ordinal ] ) {
                case CYCLE_BODY_COLOR:
                    cwTe.getNextBodyPaintColor();
                    break;
                case CYCLE_VISOR_COLOR:
                    cwTe.getNextVisorPaintColor();
                    break;
                case CYCLE_VISOR_TYPE:
                    cwTe.getNextVisorType();
                    break;
                case CYCLE_MOUNT_TYPE:
                    cwTe.getNextMountType();
                    break;
                case CYCLE_BODY_TILT:
                    cwTe.getNextBodyTilt();
                    break;
                case CYCLE_BULB_TYPE:
                    cwTe.getNextBulbType();
                    break;
            }
        } );
        return null;
    }
}
