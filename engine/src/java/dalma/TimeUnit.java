package dalma;

/**
 * Unit of time.
 *
 * @author Kohsuke Kawaguchi
 */
public enum TimeUnit {
    NANOSECONDS (1),
    MICROSECONDS(1000L),
    MILLISECONDS(1000L*1000),
    SECONDS     (1000L*1000*1000),
    MINUTES     (1000L*1000*1000*60),
    HOURS       (1000L*1000*1000*60*60),
    DAYS        (1000L*1000*1000*60*60*24),
    WEEKS       (1000L*1000*1000*60*60*24*7),
    YEARS       (1000L*1000*1000*60*60*24*365);

    private final long unitTime;


    TimeUnit(long unitTime) {
        this.unitTime = unitTime;
    }

    /**
     * Converts to nanoseconds.
     */
    public long toNano( long time ) {
        if(time>Long.MAX_VALUE/unitTime)
            return Long.MAX_VALUE;
        return time*unitTime;
    }

    public long toMilli(long time) {
        return toNano(time)/MILLISECONDS.unitTime;
    }
}
