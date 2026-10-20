package net.lidl.lidlclient.plinko;

import net.lidl.lidlclient.config.ClientConfig;
import net.lidl.lidlclient.gui.GuiColor;
import net.lidl.lidlclient.gui.GuiColorPickerScreen;
import net.lidl.lidlclient.hud.HudPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Static board foundation. Future game state can be added without changing the panel host. */
public final class PlinkoPanel implements HudPanel {
    public static final String ID = "plinko";
    private static boolean enabled;
    private static double scale=100, opacity=25, border=2, rows=12;
    private static int accent=loadColour("Accent colour",0xFF91BFFF);
    private static int pegs=loadColour("Peg colour",0xFFE8EDF7);
    private static int background=loadColour("Panel colour",0xFF101622);
    private static final Component TITLE=Component.literal("PLINKO");
    private static final Component PREVIEW=Component.literal("PLINKO · preview");
    private int cachedWidth=-1,cachedHeight=-1,cachedRows=-1;
    private int[] pegX=new int[0],pegY=new int[0],binEdges=new int[0];
    private int pegSize, boardBottom, binBottom;

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled=value; }
    public static double scale() { return scale; }
    public static void setScale(double value) { scale=clamp(value,50,175,100); }
    public static double opacity() { return opacity; }
    public static void setOpacity(double value) { opacity=clamp(value,0,80,25); }
    public static double border() { return border; }
    public static void setBorder(double value) { border=clamp(value,1,3,2); }
    public static double rows() { return rows; }
    public static void setRows(double value) { rows=Math.round(clamp(value,6,16,12)); }
    private static double clamp(double value,double min,double max,double fallback) {
        return Double.isFinite(value)?Math.clamp(value,min,max):fallback;
    }
    private static int loadColour(String key,int fallback) {
        String text=ClientConfig.string(ID,key);
        Integer value=text==null?null:GuiColor.parse(text);
        return value==null?fallback:0xFF000000|value;
    }
    public static void editAccent() { editColour("Accent colour",accent,0xFF91BFFF); }
    public static void editPegs() { editColour("Peg colour",pegs,0xFFE8EDF7); }
    public static void editBackground() { editColour("Panel colour",background,0xFF101622); }
    private static void editColour(String key,int current,int fallback) {
        Minecraft client=Minecraft.getInstance();
        client.setScreen(new GuiColorPickerScreen(client.screen,"Plinko "+key,current,fallback,value -> {
            int opaque=0xFF000000|value;
            switch (key) {
                case "Accent colour" -> accent=opaque;
                case "Peg colour" -> pegs=opaque;
                default -> background=opaque;
            }
            ClientConfig.set(ID,key,GuiColor.hex(opaque));
        }));
    }
    @Override public String id() { return ID; }
    @Override public boolean enabled() { return enabled; }
    @Override public int preferredWidth() { return (int)Math.round(260*scale/100); }
    @Override public int preferredHeight() { return (int)Math.round(280*scale/100); }

    private void layout(int w,int h) {
        int count=(int)rows;
        if (w==cachedWidth && h==cachedHeight && count==cachedRows) return;
        cachedWidth=w; cachedHeight=h; cachedRows=count;
        int total=count*(count+5)/2;
        pegX=new int[total]; pegY=new int[total]; binEdges=new int[count+4];
        double pitch=(w*.84)/(count+2), top=h*.19, bottom=h*.79;
        pegSize=Math.max(1,(int)Math.round(Math.min(w,h)/100.0));
        int index=0;
        for (int row=0;row<count;row++) {
            int columns=row+3;
            for (int col=0;col<columns;col++) {
                pegX[index]=(int)Math.round(w*.5+(col-(columns-1)*.5)*pitch)-pegSize/2;
                pegY[index]=(int)Math.round(top+row*(bottom-top)/(count-1))-pegSize/2;
                index++;
            }
        }
        for (int i=0;i<binEdges.length;i++)
            binEdges[i]=(int)Math.round(w*.08+i*(w*.84)/(binEdges.length-1));
        boardBottom=(int)Math.round(h*.85); binBottom=(int)Math.round(h*.94);
    }
    @Override public void render(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean editing) {
        layout(w,h);
        int alpha=(int)Math.round(opacity*255/100);
        if (alpha>0) g.fill(x,y,x+w,y+h,(alpha<<24)|(background&0xFFFFFF));
        int stroke=Math.min(Math.min(w,h),Math.max(1,(int)border));
        g.fill(x,y,x+w,y+stroke,accent);
        g.fill(x,y+h-stroke,x+w,y+h,accent);
        g.fill(x,y+stroke,x+stroke,y+h-stroke,accent);
        g.fill(x+w-stroke,y+stroke,x+w,y+h-stroke,accent);
        // Clip tiny-window previews as well as future panel contents to their bounds.
        g.enableScissor(x,y,x+w,y+h);
        var font=Minecraft.getInstance().font;
        Component title=editing && !enabled?PREVIEW:TITLE;
        if (w>=font.width(title)+12 && h>=60)
            g.text(font,title,x+(w-font.width(title))/2,y+Math.max(6,h/25),accent,false);
        for (int i=0;i<pegX.length;i++)
            g.fill(x+pegX[i],y+pegY[i],x+pegX[i]+pegSize,y+pegY[i]+pegSize,pegs);
        int binTint=0x38000000|(accent&0xFFFFFF);
        for (int i=0;i<binEdges.length-1;i++)
            g.fill(x+binEdges[i]+1,y+boardBottom,x+binEdges[i+1]-1,y+binBottom,binTint);
        g.fill(x+binEdges[0],y+binBottom,x+binEdges[binEdges.length-1],y+binBottom+1,accent);
        g.disableScissor();
    }
}
