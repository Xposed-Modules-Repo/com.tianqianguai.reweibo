package com.tianqianguai.reweibo;
import org.junit.Test;
import static org.junit.Assert.*;

public class GestureEvidenceTest {
    private GestureEvidence drag() {
        GestureEvidence e = new GestureEvidence(); e.reset(500); e.move(400); e.move(200); return e;
    }
    @Test public void distinguishDeliveryBoundaryAndUnresponsiveList() {
        GestureEvidence e = drag();
        assertEquals("gesture_not_delivered", e.classify(48, false, true, true));
        e.delivered = true;
        assertEquals("boundary", e.classify(48, true, true, true));
        assertEquals("gesture_no_scroll", e.classify(48, false, true, true));
        assertEquals("normal", e.classify(48, false, true, false));
    }
    @Test public void tapsCancellationAndNormalScrollingAreNotAnomalies() {
        GestureEvidence e = new GestureEvidence(); e.reset(100); e.move(102);
        assertEquals("normal", e.classify(48, false, true, true));
        e = drag(); e.delivered = true; e.scroll(300);
        assertEquals("normal", e.classify(48, false, false, true));
        e.cancelled = true;
        assertEquals("normal", e.classify(48, false, true, true));
    }
    @Test public void returningToStartRequiresModuleEvidenceToFlagPullback() {
        GestureEvidence e = drag(); e.delivered = true; e.scroll(300); e.scroll(-300);
        assertEquals("normal", e.classify(48, false, true, true));
        e.mutations++;
        assertEquals("possible_anchor_pullback", e.classify(48, false, true, true));
        assertEquals("module_during_gesture", e.classify(48, false, false, true));
        e.reset(0); assertEquals(0, e.travel); assertEquals(0, e.mutations);
    }
    @Test public void moveHotPathIsConstantSpaceAndReportsMeasuredCost() {
        GestureEvidence e = new GestureEvidence(); e.reset(0);
        long start = System.nanoTime();
        for (int i=0;i<1000000;i++) {e.move(i % 500); e.scroll(2);}
        long elapsed=System.nanoTime()-start;
        assertEquals(1000000,e.moves); assertEquals(2000000,e.travel);
        System.out.println("gesture_move_plus_scroll_ns="+elapsed/1000000);
    }
}
