package com.micatechnologies.minecraft.csm.tools.tool_framework;

import java.io.File;

@FunctionalInterface
public interface CsmToolRunnable
{
    void run( File devEnvironmentPath ) throws Exception;
}

