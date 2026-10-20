package net.lidl.lidlclient.hud;

/** Pure geometry for anchored edge/corner resizing in GUI coordinates. */
public final class PanelResize {
    public static final int LEFT=1, RIGHT=2, TOP=4, BOTTOM=8;
    public record Bounds(int x,int y,int width,int height) { }
    private PanelResize() { }
    public static int edges(double mx,double my,int x,int y,int w,int h) {
        int result=0;
        if (mx<x+6) result|=LEFT; else if (mx>=x+w-6) result|=RIGHT;
        if (my<y+6) result|=TOP; else if (my>=y+h-6) result|=BOTTOM;
        return result;
    }
    public static Bounds resize(Bounds start,int edges,double dx,double dy,int vw,int vh,int minW,int minH) {
        int l=start.x(),t=start.y(),r=l+start.width(),b=t+start.height();
        minW=Math.min(minW,vw); minH=Math.min(minH,vh);
        if ((edges&LEFT)!=0) l=(int)Math.clamp(Math.round(l+dx),0,Math.max(0,r-minW));
        if ((edges&RIGHT)!=0) r=(int)Math.clamp(Math.round(r+dx),Math.min(vw,l+minW),vw);
        if ((edges&TOP)!=0) t=(int)Math.clamp(Math.round(t+dy),0,Math.max(0,b-minH));
        if ((edges&BOTTOM)!=0) b=(int)Math.clamp(Math.round(b+dy),Math.min(vh,t+minH),vh);
        return new Bounds(l,t,r-l,b-t);
    }
}
