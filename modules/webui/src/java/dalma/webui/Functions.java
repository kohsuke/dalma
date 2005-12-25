package dalma.webui;

import java.util.Date;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi
 */
public class Functions {
    public static Date createDate(long lt) {
        return new Date(lt);
    }

    public static String getExceptionDetail(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Returns a human readable string that represents the duration.
     *
     * @param duration
     *      time in milliseconds.
     */
    public static String getTimeSpanString(long duration) {
        duration /= 1000;
        if(duration<60)
            return duration+" seconds";
        duration /= 60;
        if(duration<60)
            return duration+" minutes";
        duration /= 60;
        if(duration<24)
            return duration+" hours";
        duration /= 24;
        if(duration<30)
            return duration+" days";
        duration /= 30;
        if(duration<12)
            return duration+" months";
        duration /= 12;
        return duration+" years";
    }
}
