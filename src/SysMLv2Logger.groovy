import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.GUILog
import org.apache.log4j.Logger
import java.io.StringWriter
import java.io.PrintWriter

/**
 * Robust Logger for SysMLv2 Groovy Scripts.
 * Ensures that all messages and exceptions are output to both
 * the standard Log4j system AND the MagicDraw GUI Console.
 */
class SysMLv2Logger {
    private final Logger log
    private final GUILog gl

    /**
     * Initialize logger for a specific class.
     */
    public SysMLv2Logger(Class<?> clazz) {
        this.log = Logger.getLogger(clazz)
        this.gl = Application.getInstance().getGUILog()
    }

    /**
     * Initialize logger for a specific name.
     */
    public SysMLv2Logger(String name) {
        this.log = Logger.getLogger(name)
        this.gl = Application.getInstance().getGUILog()
    }

    /**
     * Log an info message.
     * Note: Does not echo to GUI log to reduce console clutter.
     * Only WARN and ERROR are shown in the user console.
     */
    public void info(String message) {
        log.info(message)
    }

    /**
     * Log a debug message. 
     * Note: Does not echo to GUI log by default to avoid clutter.
     */
    public void debug(String message) {
        log.debug(message)
    }

    /**
     * Log a warning message.
     */
    public void warn(String message) {
        log.warn(message)
        if (gl != null) {
            gl.log("[WARNING] " + message) // GUILog.log prints in black/blue depending on MD version.
        }
    }

    /**
     * Log a warning with an exception.
     * STRICT RULE: Never swallow exceptions.
     */
    public void warn(String message, Throwable t) {
        log.warn(message, t)
        if (gl != null) {
            gl.log("[WARNING] " + message + "\n" + getStackTraceAsString(t))
        } else {
            // Fallback to standard error if GUI log is unavailable
            System.err.println("[WARNING] " + message)
            t.printStackTrace(System.err)
        }
    }

    /**
     * Log an error message to GUI popup/console and log4j.
     */
    public void error(String message) {
        log.error(message)
        if (gl != null) {
            gl.log("[ERROR] " + message)
        } else {
            System.err.println("[ERROR] " + message)
        }
    }

    /**
     * Log a critical error with an exception.
     * STRICT RULE: Never swallow exceptions.
     */
    public void error(String message, Throwable t) {
        log.error(message, t)
        if (gl != null) {
            gl.log("[ERROR] " + message + "\n" + getStackTraceAsString(t))
        } else {
            System.err.println("[ERROR] " + message)
            t.printStackTrace(System.err)
        }
    }

    /**
     * Utility method to format exception stack traces into a string.
     */
    private String getStackTraceAsString(Throwable t) {
        if (t == null) {
            return ""
        }
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        t.printStackTrace(pw)
        return sw.toString()
    }
}
