package com.tianqianguai.reweibo;

/** Server cursor evidence is distinct from cache duration and from an uninitialised cursor. */
final class PagingEvidence {
    boolean requested;
    boolean responseSeen;
    long nextCursor = -1;
    int generation;

    int request(boolean refresh) {
        requested = true;
        if (refresh) { generation++; responseSeen = false; nextCursor = -1; }
        return generation;
    }
    boolean response(int token, long cursor) {
        if (token != generation) return false;
        responseSeen = true; nextCursor = cursor; return true;
    }
    boolean terminal() { return responseSeen && nextCursor == 0; }
    boolean terminalFor(int token) { return token == generation && terminal(); }
    boolean mayRestoreCursor() { return !requested && !responseSeen; }
    static boolean shouldWait(long hostCursor, boolean gapActive) { return hostCursor == 0 && !gapActive; }
}
