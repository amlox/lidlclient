package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

class DungeonScanProgressTest {
    @Test void onlyAResolvedWholeGridCompletesAndResetStartsANewScan() {
        var progress=new DungeonScanProgress();assertFalse(progress.complete());
        for(int i=0;i<GRID*GRID-1;i++) progress.finish(i,false);
        assertFalse(progress.complete()); // One unloaded/unidentified position keeps scanning alive.
        progress.finish(GRID*GRID-1,true);assertTrue(progress.complete());
        progress.finish(0,false);assertTrue(progress.complete());
        progress.reset();assertFalse(progress.complete());assertFalse(progress.finished(0));
    }
    @Test void newlyRevealedGeometryReopensEmptyProbesWithoutRestartingResolvedRooms() {
        var model=new DungeonMapModel();var progress=new DungeonScanProgress();
        for(int i=0;i<GRID*GRID;i++) progress.finish(i,true);
        assertTrue(progress.complete());progress.reconcile(model);assertTrue(progress.complete());
        model.at(0,0).kind=Kind.ROOM;progress.reconcile(model);
        assertFalse(progress.complete());assertFalse(progress.finished(0));assertTrue(progress.finished(1));
        progress.finish(0,false);progress.reconcile(model);assertTrue(progress.complete());
        model.at(0,0).state=State.CLEARED;model.at(0,0).discovered=true;
        progress.reconcile(model);assertTrue(progress.complete());
    }
}
