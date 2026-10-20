package net.lidl.lidlclient.gui;

/** Small RGB/HSV conversion shared by the mouse picker and its tests. */
public final class GuiColor {
    private GuiColor() { }
    public static String hex(int rgb) { return String.format("#%06X", rgb & 0xFFFFFF); }
    public static Integer parse(String text) {
        String value = text.strip().replaceFirst("^#", "");
        return value.matches("[0-9a-fA-F]{6}") ? 0xFF000000 | Integer.parseInt(value, 16) : null;
    }

    public static int rgb(double hue,double saturation,double value) {
        double h=(hue-Math.floor(hue))*6,s=Math.clamp(saturation,0,1),v=Math.clamp(value,0,1);
        double c=v*s,x=c*(1-Math.abs(h%2-1)),m=v-c;
        double r=0,g=0,b=0;
        switch((int)h) {case 0->{r=c;g=x;}case 1->{r=x;g=c;}case 2->{g=c;b=x;}case 3->{g=x;b=c;}case 4->{r=x;b=c;}default->{r=c;b=x;}}
        return 0xFF000000|((int)Math.round((r+m)*255)<<16)|((int)Math.round((g+m)*255)<<8)|(int)Math.round((b+m)*255);
    }
    public static double[] hsv(int rgb) {
        double r=((rgb>>16)&255)/255.0,g=((rgb>>8)&255)/255.0,b=(rgb&255)/255.0;
        double max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),d=max-min,h=0;
        if(d>0) {if(max==r) h=((g-b)/d)%6;else if(max==g) h=(b-r)/d+2;else h=(r-g)/d+4;h/=6;if(h<0) h++;}
        return new double[]{h,max==0?0:d/max,max};
    }
}
