package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Set;

@ElementsCitySuperMod.ModElement.Tag
public class ItemApplePencil extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:applepencil" )
    public static final Item block = null;

    public ItemApplePencil( ElementsCitySuperMod instance ) {
        super( instance, 77 );
    }

    @Override
    public void initElements() {
        elements.items.add( () -> new ItemSpade( EnumHelper.addToolMaterial( "APPLEPENCIL", 1, 100, 4f, 1f, 2 ) )
        {
            {
                this.attackSpeed = -1.2f;
            }

            public Set< String > getToolClasses( ItemStack stack ) {
                HashMap< String, Integer > ret = new HashMap< String, Integer >();
                ret.put( "spade", 1 );
                return ret.keySet();
            }
        }.setUnlocalizedName( "applepencil" ).setRegistryName( "applepencil" ).setCreativeTab( TabMCLATechTab.tab ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( block, 0,
                                                    new ModelResourceLocation( "csm:applepencil", "inventory" ) );
    }
}
