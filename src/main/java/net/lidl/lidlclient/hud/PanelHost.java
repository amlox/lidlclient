package net.lidl.lidlclient.hud;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.lidl.lidlclient.LidlClient;
import net.lidl.lidlclient.config.ClientConfig;
import net.lidl.lidlclient.plinko.PlinkoPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** One render hook for all movable panels. No polling, tick loop, or idle animation. */
public final class PanelHost {
    private static final List<Entry> PANELS = new ArrayList<>();
    private static boolean initialized;
    private PanelHost() { }
    public static final class Entry {
        public final HudPanel panel;
        public final PanelPlacement placement;
        private int savedWidth, savedHeight;
        private Entry(HudPanel panel) {
            this.panel = panel;
            Double x = ClientConfig.number("panel_layout", panel.id()+".x");
            Double y = ClientConfig.number("panel_layout", panel.id()+".y");
            placement = new PanelPlacement(x == null ? .8 : x, y == null ? .5 : y);
            Double w=ClientConfig.number("panel_layout",panel.id()+".width");
            Double h=ClientConfig.number("panel_layout",panel.id()+".height");
            savedWidth=w==null?panel.preferredWidth():(int)Math.clamp(w,panel.minimumWidth(),8192);
            savedHeight=h==null?panel.preferredHeight():(int)Math.clamp(h,panel.minimumHeight(),8192);
        }
        public int width(int vw,int vh) { return Math.max(1,Math.min(vw,savedWidth)); }
        public int height(int vw,int vh) { return Math.max(1,Math.min(vh,savedHeight)); }
        public void resize(PanelResize.Bounds bounds,int vw,int vh) {
            savedWidth=Math.max(panel.minimumWidth(),bounds.width());
            savedHeight=Math.max(panel.minimumHeight(),bounds.height());
            placement.move(bounds.x(),bounds.y(),vw,vh,width(vw,vh),height(vw,vh));
        }
        public void save() {
            ClientConfig.set("panel_layout",panel.id()+".width",savedWidth);
            ClientConfig.set("panel_layout",panel.id()+".height",savedHeight);
            ClientConfig.set("panel_layout", panel.id()+".x", placement.xFraction());
            ClientConfig.set("panel_layout", panel.id()+".y", placement.yFraction());
        }
    }
    public static void register(HudPanel panel) {
        if (PANELS.stream().anyMatch(entry -> entry.panel.id().equals(panel.id())))
            throw new IllegalArgumentException("Duplicate panel: "+panel.id());
        if (panel.preferredWidth() <= 0 || panel.preferredHeight() <= 0)
            throw new IllegalArgumentException("Panel dimensions must be positive");
        PANELS.add(new Entry(panel));
    }
    static List<Entry> entries() { return PANELS; }
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        register(new PlinkoPanel());
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, LidlClient.id("panels"), (g,delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.screen != null) return;
            render(g,false);
        });
    }
    static void render(GuiGraphicsExtractor g, boolean editing) {
        int vw = g.guiWidth(), vh = g.guiHeight();
        for (Entry entry : PANELS) {
            if (!editing && !entry.panel.enabled()) continue;
            int w = entry.width(vw,vh), h = entry.height(vw,vh);
            entry.panel.render(g,entry.placement.x(vw,w),entry.placement.y(vh,h),w,h,editing);
        }
    }
    public static boolean editing() { return Minecraft.getInstance().screen instanceof PanelEditorScreen; }
    public static void openEditor() {
        Minecraft client = Minecraft.getInstance();
        if (!editing()) client.setScreen(new PanelEditorScreen(client.screen));
    }
    public static void setEditing(boolean value) {
        // Scheduling also lets module-menu input finish before replacing its screen.
        if (value) Minecraft.getInstance().schedule(PanelHost::openEditor);
    }
}
