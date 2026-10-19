package net.lidl.lidlclient.dungeons;

import java.util.BitSet;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

/** Completed loaded-world probes; unknown/unloaded columns never count as finished. */
public final class DungeonScanProgress {
    private final BitSet finished=new BitSet(GRID*GRID),empty=new BitSet(GRID*GRID);
    public boolean complete() {return finished.cardinality()==GRID*GRID;}
    public boolean finished(int index) {return finished.get(index);}
    public void finish(int index,boolean wasEmpty) {finished.set(index);empty.set(index,wasEmpty);}
    public void reset() {finished.clear();empty.clear();}
    /** Newly revealed geometry means an earlier empty probe wasn't the final layout. */
    public void reconcile(DungeonMapModel model) {
        for(int id=empty.nextSetBit(0);id>=0;id=empty.nextSetBit(id+1)) {
            if(model.at(id%GRID,id/GRID).kind!=Kind.EMPTY) {finished.clear(id);empty.clear(id);}
        }
    }
}
