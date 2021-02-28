package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TrafficSignalCircuit
{
    private final List< BlockPos > leftSignals  = new ArrayList<>();
    private final List< BlockPos > aheadSignals = new ArrayList<>();
    private final List< BlockPos > rightSignals      = new ArrayList<>();
    private final List< BlockPos > pedestrianSignals = new ArrayList<>();
    private final List< BlockPos > protectedSignals  = new ArrayList<>();
}
