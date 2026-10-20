package net.lidl.lidlclient.plinko;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlinkoDropTest {
    @Test void startsCenteredAndTouchesEveryTriangularRow() {
        var drop=new PlinkoDrop(new Random(42));
        assertEquals(.5,drop.x(0)); assertEquals(.10,drop.y(0),1e-9);
        for (int row=0;row<12;row++) {
            double age=.32+row*.22;
            double column=(drop.x(age)-.5)/(.84/13)+row/2.0;
            assertEquals(Math.rint(column),column,1e-8);
            assertTrue(column>=-1e-8 && column<=row+1e-8);
            assertEquals(.178+row*.60/11,drop.y(age),1e-8);
        }
    }
    @Test void landsInBucketCenterAndFadesOut() {
        var drop=new PlinkoDrop(new Random(7));
        double slot=(drop.x(PlinkoDrop.LAND)-.08)/(.84/13)-.5;
        assertEquals(Math.rint(slot),slot,1e-8);
        assertEquals(.865,drop.y(PlinkoDrop.LAND),1e-9);
        assertEquals(0,drop.opacity(PlinkoDrop.DURATION),1e-9);
        assertEquals(drop.x(PlinkoDrop.LAND),drop.x(100),1e-9);
    }
    @Test void pathIsBoundedAndIndependentOfRenderFrequency() {
        var a=new PlinkoDrop(new Random(123)); var b=new PlinkoDrop(new Random(123));
        for (int i=0;i<1000;i++) {
            double t=i*.004;
            assertTrue(a.x(t)>=.08 && a.x(t)<=.92);
            assertTrue(a.y(t)>=.09 && a.y(t)<=.87);
        }
        assertEquals(a.x(1.73),b.x(1.73)); assertEquals(a.y(1.73),b.y(1.73));
    }
}
