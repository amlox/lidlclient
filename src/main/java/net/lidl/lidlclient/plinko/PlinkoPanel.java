package net.lidl.lidlclient.plinko;

import net.lidl.lidlclient.config.ClientConfig;
import net.lidl.lidlclient.gui.GuiColor;
import net.lidl.lidlclient.gui.GuiColorPickerScreen;
import net.lidl.lidlclient.hud.HudPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Cached triangular board with bounded visual-only test drops. */
public final class PlinkoPanel implements HudPanel {
    public static final String ID = "plinko";
    private static boolean enabled;
    private static double opacity=25, border=2;
    private record Ball(PlinkoDrop path,long started) { }
    private static final java.util.List<Ball> BALLS=new java.util.ArrayList<>(8);
    private static final Component DROP=Component.literal("Drop test");
    private static final Component FULL=Component.literal("Drop test (8/8)");
    public static void dropTest() {
        net.lidl.lidlclient.hud.PanelHost.openEditor();
        spawn();
    }
    private static void spawn() {
        long now=System.nanoTime();
        expire(now);
        if (BALLS.size()<8) BALLS.add(new Ball(new PlinkoDrop(java.util.concurrent.ThreadLocalRandom.current()),now));
    }
    private static void expire(long now) {
        for (int i=BALLS.size()-1;i>=0;i--)
            if ((now-BALLS.get(i).started())/1e9>=PlinkoDrop.DURATION) BALLS.remove(i);
    }
    private static int accent=loadColour("Accent colour",0xFF91BFFF);
    private static int pegs=loadColour("Peg colour",0xFFE8EDF7);
    private static int background=loadColour("Panel colour",0xFF101622);
    private static final Component TITLE=Component.literal("PLINKO");
    private static final Component PREVIEW=Component.literal("PLINKO · preview");
    private int cachedWidth=-1,cachedHeight=-1,cachedRows=-1;
    private int[] pegX=new int[0],pegY=new int[0],binEdges=new int[0];
    private int pegSize, boardBottom, binBottom;

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled=value; if (!value) BALLS.clear(); }
    public static double opacity() { return opacity; }
    public static void setOpacity(double value) { opacity=clamp(value,0,80,25); }
    public static double border() { return border; }
    public static void setBorder(double value) { border=clamp(value,1,3,2); }
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
    @Override public int preferredWidth() { return 260; }
    @Override public int preferredHeight() { return 280; }

    @Override public int minimumWidth() { return 104; }
    @Override public int minimumHeight() { return 112; }
    @Override public boolean click(double x,double y,int w,int h) {
        x=x*260/w; y=y*280/h; w=260; h=280;
        if (x>=w/2-44 && x<w/2+44 && y>=h-23 && y<h-7) { spawn(); return true; }
        return false;
    }

    private void layout(int w,int h) {
        int count=PlinkoDrop.ROWS;
        if (w==cachedWidth && h==cachedHeight && count==cachedRows) return;
        cachedWidth=w; cachedHeight=h; cachedRows=count;
        int total=count*(2*PlinkoDrop.FIRST_ROW+count-1)/2;
        pegX=new int[total]; pegY=new int[total]; binEdges=new int[PlinkoDrop.BUCKETS+1];
        double pitch=(w*.84)/PlinkoDrop.BUCKETS, top=h*.19, bottom=h*.79;
        pegSize=Math.max(1,(int)Math.round(Math.min(w,h)/100.0));
        int index=0;
        for (int row=0;row<count;row++) {
            int columns=row+PlinkoDrop.FIRST_ROW;
            for (int col=0;col<columns;col++) {
                pegX[index]=(int)Math.round(w*.5+(col-(columns-1)*.5)*pitch)-pegSize/2;
                pegY[index]=(int)Math.round(top+row*(bottom-top)/(count-1))-pegSize/2;
                index++;
            }
        }
        for (int i=0;i<binEdges.length;i++)
            binEdges[i]=(int)Math.round(w*.08+i*(w*.84)/(binEdges.length-1));
        boardBottom=(int)Math.round(h*.83); binBottom=(int)Math.round(h*.89);
    }
    @Override public void render(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean editing) {
        // Render in a stable design space: text, buttons, balls and pegs scale together.
        g.pose().pushMatrix();
        g.pose().translate((float)x,(float)y);
        g.pose().scale(w/260f,h/280f);
        try { renderBoard(g,editing); } finally { g.pose().popMatrix(); }
    }
    private void renderBoard(GuiGraphicsExtractor g,boolean editing) {
        int x=0,y=0,w=260,h=280;
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
        long now=System.nanoTime();
        expire(now);
        int radius=Math.max(2,(int)Math.round(Math.min(w,h)*.009));
        for (int i=0;i<BALLS.size();i++) {
            Ball ball=BALLS.get(i);
            double age=(now-ball.started())/1e9;
            int bx=x+(int)Math.round(ball.path().x(age)*w),by=y+(int)Math.round(ball.path().y(age)*h);
            int tint=((int)Math.round(255*ball.path().opacity(age))<<24)|0xFFF0A6;
            // Two clipped quads form a small rounded ball without textures or particles.
            g.fill(bx-radius+1,by-radius,bx+radius-1,by+radius,tint);
            g.fill(bx-radius,by-radius+1,bx+radius,by+radius-1,tint);
        }
        if (editing && w>=100 && h>=100) {
            g.fill(x+w/2-44,y+h-23,x+w/2+44,y+h-7,0x553A506E);
            Component label=BALLS.size()<8?DROP:FULL;
            g.text(font,label,x+(w-font.width(label))/2,y+h-19,accent,false);
        }
        g.disableScissor();
    }
}
