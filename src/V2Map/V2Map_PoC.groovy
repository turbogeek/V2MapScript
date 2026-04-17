// =============================================================================
// V2Map PoC Launcher
// Loads mxGraph (jgraphx) JAR into classloader, then evaluates the core script.
// MagicDraw's JSR-223 ScriptRunner does not support @Grab, so we must load
// external JARs manually via GroovyClassLoader before the core script's
// import statements are resolved.
// =============================================================================

def jarFile = new File(System.getProperty('user.home'), '.groovy/grapes/com.github.vlsi.mxgraph/jgraphx/jars/jgraphx-4.2.2.jar')
if (!jarFile.exists()) {
    javax.swing.JOptionPane.showMessageDialog(null,
        "mxGraph JAR not found at:\n${jarFile.absolutePath}\n\nPlease ensure jgraphx-4.2.2.jar is in the Groovy Grape cache.",
        "V2Map: Missing Dependency", javax.swing.JOptionPane.ERROR_MESSAGE)
    return
}

def cl = new GroovyClassLoader(this.getClass().getClassLoader())
cl.addURL(jarFile.toURI().toURL())

def coreScript = new File("E:\\_Documents\\git\\SysMLv2ClientAPI\\scripts\\V2Map\\V2Map_PoC_Core.groovy")
def shell = new GroovyShell(cl)
shell.evaluate(coreScript)
