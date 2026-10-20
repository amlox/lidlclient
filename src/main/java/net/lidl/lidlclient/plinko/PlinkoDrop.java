package net.lidl.lidlclient.plinko;

import java.util.random.RandomGenerator;

/** Visual path only: no economy, payout selection, or frame-rate-dependent physics. */
public final class PlinkoDrop {
    public static final int ROWS=12, FIRST_ROW=5, BUCKETS=ROWS+FIRST_ROW;
    public static final double FALL=.32, HOP=.22, LAND=FALL+ROWS*HOP, DURATION=LAND+.45;
    private final double[] xs=new double[ROWS+2];
    public PlinkoDrop(RandomGenerator random) {
        xs[0]=xs[1]=.5;
        double step=.84/BUCKETS/2;
        for (int i=2;i<xs.length;i++) xs[i]=xs[i-1]+(random.nextBoolean()?step:-step);
    }
    public double x(double age) {
        if (age<=FALL) return .5;
        double progress=Math.clamp((age-FALL)/HOP,0,ROWS);
        int index=Math.min(ROWS-1,(int)progress);
        double t=progress-index;
        return xs[index+1]+(xs[index+2]-xs[index+1])*t;
    }
    public double y(double age) {
        if (age<=FALL) { double t=Math.clamp(age/FALL,0,1); return .10+(.178-.10)*t*t; }
        double progress=Math.clamp((age-FALL)/HOP,0,ROWS);
        int index=Math.min(ROWS-1,(int)progress);
        double t=progress-index;
        double from=.178+index*.60/(ROWS-1);
        double to=index==ROWS-1?.865:.178+(index+1)*.60/(ROWS-1);
        return from+(to-from)*t-Math.sin(Math.PI*t)*.025;
    }
    public double opacity(double age) { return Math.clamp((DURATION-age)/.45,0,1); }
}
