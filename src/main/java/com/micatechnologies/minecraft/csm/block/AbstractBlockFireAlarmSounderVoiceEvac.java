package com.micatechnologies.minecraft.csm.block;

public abstract class AbstractBlockFireAlarmSounderVoiceEvac extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getSoundResourceName() {
        return null;
    }

    @Override
    public int getSoundTickLen() {
        return 0;
    }
}