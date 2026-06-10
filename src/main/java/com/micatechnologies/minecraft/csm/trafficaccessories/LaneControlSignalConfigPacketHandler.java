package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class LaneControlSignalConfigPacketHandler
        implements IMessageHandler<LaneControlSignalConfigPacket, IMessage> {

    @Override
    public IMessage onMessage(LaneControlSignalConfigPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.server.addScheduledTask(() -> {
            if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
                return;
            }
            World world = player.world;
            TileEntity te = world.getTileEntity(message.getPos());
            if (!(te instanceof TileEntityLaneControlSignal)) {
                return;
            }

            int ordinal = message.getActionOrdinal();
            if (ordinal < 0 || ordinal >= LaneControlSignalConfigAction.values().length) {
                return;
            }

            TileEntityLaneControlSignal lcsTe = (TileEntityLaneControlSignal) te;
            switch (LaneControlSignalConfigAction.values()[ordinal]) {
                case CYCLE_BODY_COLOR:
                    lcsTe.getNextBodyPaintColor();
                    break;
                case CYCLE_VISOR_COLOR:
                    lcsTe.getNextVisorPaintColor();
                    break;
                case CYCLE_VISOR_TYPE:
                    lcsTe.getNextVisorType();
                    break;
                case CYCLE_MOUNT_TYPE:
                    lcsTe.getNextMountType();
                    break;
                case CYCLE_BODY_TILT:
                    lcsTe.getNextBodyTilt();
                    break;
                case CYCLE_SIGNAL_TYPE:
                    lcsTe.getNextSignalType();
                    break;
            }
        });
        return null;
    }
}
