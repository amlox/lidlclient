package net.lidl.lidlclient.mixin;

import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(MapItemSavedData.class)
public interface DungeonMapDataAccessor {
    @Accessor("decorations") Map<String,MapDecoration> lidlclient$decorations();
}
