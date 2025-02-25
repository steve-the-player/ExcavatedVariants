package dev.lukebemish.excavatedvariants.impl.neoforge;

import com.google.auto.service.AutoService;
import dev.lukebemish.excavatedvariants.impl.ExcavatedVariants;
import dev.lukebemish.excavatedvariants.impl.platform.services.CreativeTabLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

@SuppressWarnings("unused")
@AutoService(CreativeTabLoader.class)
public class CreativeTabLoaderImpl implements CreativeTabLoader {

    private static void setup(CreativeModeTab.Builder builder) {
        builder
                .title(Component.translatable("itemGroup."+CREATIVE_TAB_ID.getNamespace()+"."+CREATIVE_TAB_ID.getPath()))
                .icon(() -> new ItemStack(Items.DEEPSLATE_COPPER_ORE))
                .displayItems((displayParameters, output) -> {
                    for (var supplier : ExcavatedVariants.ITEMS) {
                        output.accept(supplier.get());
                    }
                });
    }

    @Override
    public void registerCreativeTab() {
        ExcavatedVariantsNeoForge.CREATIVE_TABS.register(CREATIVE_TAB_ID.getPath(), () -> {
            var builder = CreativeModeTab.builder();
            setup(builder);
            return builder.build();
        });
    }

    @Override
    public CreativeModeTab getCreativeTab() {
        return Objects.requireNonNull(BuiltInRegistries.CREATIVE_MODE_TAB.get(CREATIVE_TAB_ID));
    }
}
