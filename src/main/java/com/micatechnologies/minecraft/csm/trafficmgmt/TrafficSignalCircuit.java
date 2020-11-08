package com.micatechnologies.minecraft.csm.trafficmgmt;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public class TrafficSignalCircuit
{
    public final ArrayList< BlockPos > pairedLeftSignals;
    public final ArrayList< BlockPos > pairedAheadSignals;
    public final ArrayList< BlockPos > pairedRightSignals;
    public final ArrayList< BlockPos > pairedProtectedAheadSignals;
    public final ArrayList< BlockPos > pairedCrosswalkDevices;

    public TrafficSignalCircuit() {
        // Initialize arrays
        pairedLeftSignals = new ArrayList<>();
        pairedAheadSignals = new ArrayList<>();
        pairedRightSignals = new ArrayList<>();
        pairedProtectedAheadSignals = new ArrayList<>();
        pairedCrosswalkDevices = new ArrayList<>();
    }

    public TrafficSignalCircuit( String dataToLoad ) {
        // Initialize arrays
        pairedLeftSignals = new ArrayList<>();
        pairedAheadSignals = new ArrayList<>();
        pairedRightSignals = new ArrayList<>();
        pairedProtectedAheadSignals = new ArrayList<>();
        pairedCrosswalkDevices = new ArrayList<>();

        // Read data from string to arrays
        // TODO::
    }

    @Override
    public String toString() {
        // Build string from arrays

    }
}
