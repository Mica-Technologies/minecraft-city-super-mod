package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Interface for classes that have models.
 * <p>
 * See Source URL: <a
 * href="https://github.com/sa-shiro/Minecraft-Forge-1.12.2-BaseMod/blob/master/src/main/java/com/basemod/base/util/IHasModel.java">https://github.com/sa-shiro/Minecraft-Forge-1.12.2-BaseMod/blob/master/src/main/java/com/basemod/base/util/IHasModel.java</a>
 *
 * @author sa-shiro
 * @version c3af894
 * @since 2.0
 */
public interface IHasModel
{
    /**
     * Register the model for the object. (little utility to help us registering all models)
     *
     * @since 96cefd4
     */
    void registerModels();
}
