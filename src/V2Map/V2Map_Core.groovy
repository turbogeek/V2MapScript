import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.browser.Node
import com.dassault_systemes.modeler.kerml.model.kerml.Dependency
import com.dassault_systemes.modeler.kerml.model.kerml.Element
import com.dassault_systemes.modeler.kerml.model.kerml.Namespace
import com.dassault_systemes.modeler.kerml.model.kerml.Package
import com.dassault_systemes.modeler.kerml.model.kerml.Relationship
import com.dassault_systemes.modeler.sysml.model.sysml.*

import javax.swing.*
import javax.swing.border.*
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.*
import java.util.List
import java.util.Map
import java.util.Set
import java.awt.event.*

import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxConstants
import com.mxgraph.util.mxCellRenderer
import com.mxgraph.util.mxXmlUtils
import com.mxgraph.view.mxGraph
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.layout.mxCompactTreeLayout

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult



import groovy.json.JsonSlurper
import groovy.json.JsonOutput

// =============================================================================
// V2Map â€” SysMLv2 Model Map Viewer (Production Version)
// =============================================================================
// Phases A+B+C: Full type support, control panel, toolbar, search, theming
// =============================================================================

String scriptDir = "E:\\_Documents\\git\\SysMLv2ClientAPI\\scripts"
File loggerFile = new File(scriptDir, "SysMLv2Logger.groovy")
def SysMLv2Logger = new GroovyClassLoader(getClass().getClassLoader()).parseClass(loggerFile)
def logger = SysMLv2Logger.newInstance("V2Map")

// --- Debug Log ---
File debugFile = new File(scriptDir, "V2Map/V2Map_Debug.log")
def logDebug = { String msg ->
    try { debugFile.append("[${new Date()}] ${msg}\n") } catch (Exception ignore) {}
    logger.info(msg)
}
def logCrash = { String ctx, Throwable t ->
    StringBuilder sb = new StringBuilder()
    sb.append("=== V2MAP EXCEPTION ===\n")
    sb.append("Time: ${new Date()}\nContext: ${ctx}\n")
    sb.append("Exception: ${t.getClass().getName()}\nMessage: ${t.getMessage()}\n\n")
    Throwable cause = t.getCause()
    if (cause) sb.append("Cause: ${cause.getClass().getName()}: ${cause.getMessage()}\n\n")
    sb.append("Relevant Stack Trace:\n")
    t.getStackTrace().each { ste ->
        String cls = ste.getClassName()
        if (!cls.startsWith("org.codehaus.groovy") && !cls.startsWith("java.lang.reflect") &&
            !cls.startsWith("sun.reflect") && !cls.startsWith("groovy.lang.MetaClass")) {
            sb.append("\tat ${ste}\n")
        }
    }
    try { debugFile.append(sb.toString()) } catch (Exception ignore) {}
    logger.error(ctx, t)
}
try { debugFile.text = "=== V2Map Debug Log â€” ${new Date()} ===\n\n" } catch (Exception ignore) {}

// =============================================================================
// SECTION 1: Theme Definitions
// =============================================================================
def THEMES = [
    "Dark": [
        canvasBg:    new Color(42, 42, 54),
        panelBg:     new Color(50, 50, 65),
        panelBg2:    new Color(58, 58, 75),
        toolbarBg:   new Color(40, 40, 52),
        textColor:   Color.WHITE,
        textDim:     new Color(170, 170, 190),
        accent:      new Color(74, 144, 217),
        border:      new Color(70, 70, 90),
        statusBg:    new Color(45, 45, 58),
        edgeLabelFg: "#BBBBCC",
        rootBorder:  "#1A9B52",
        searchHighlight: new Color(255, 235, 59)
    ],
    "Light": [
        canvasBg:    new Color(245, 245, 248),
        panelBg:     new Color(235, 235, 240),
        panelBg2:    new Color(225, 225, 232),
        toolbarBg:   new Color(230, 230, 238),
        textColor:   new Color(40, 40, 50),
        textDim:     new Color(100, 100, 120),
        accent:      new Color(41, 98, 163),
        border:      new Color(200, 200, 210),
        statusBg:    new Color(228, 228, 236),
        edgeLabelFg: "#444455",
        rootBorder:  "#1A7A42",
        searchHighlight: new Color(255, 193, 7)
    ],
    "Hello Kitty": [
        canvasBg:    new Color(255, 228, 232),
        panelBg:     new Color(255, 200, 210),
        panelBg2:    new Color(255, 185, 200),
        toolbarBg:   new Color(255, 182, 193),
        textColor:   new Color(139, 69, 96),
        textDim:     new Color(180, 100, 130),
        accent:      new Color(255, 105, 140),
        border:      new Color(255, 160, 180),
        statusBg:    new Color(255, 192, 203),
        edgeLabelFg: "#8B4560",
        rootBorder:  "#FF69B4",
        searchHighlight: new Color(255, 215, 0)
    ]
]

// =============================================================================
// SECTION 2: Node Type Registry
// =============================================================================
// Each entry: [name, usageClass, defClass, usageColor, defColor, usageEnabled, defEnabled]
// Colors are hex strings. Shapes: definitions = rectangle, usages = ellipse
def nodeTypeRegistry = [
    [name: "Part",        usageClass: PartUsage.class,        defClass: PartDefinition.class,
     usageColor: "#5DADE2", defColor: "#2E86C1", usageEnabled: true, defEnabled: true],

    [name: "Satisfy",     usageClass: SatisfyRequirementUsage.class, defClass: null,
     usageColor: "#82E0AA", defColor: "#1E8449", usageEnabled: true, defEnabled: false],

    [name: "Requirement", usageClass: RequirementUsage.class, defClass: RequirementDefinition.class,
     usageColor: "#58D68D", defColor: "#28B463", usageEnabled: true, defEnabled: true],

    [name: "Action",      usageClass: ActionUsage.class,      defClass: ActionDefinition.class,
     usageColor: "#F5B041", defColor: "#E67E22", usageEnabled: true, defEnabled: true],

    [name: "State",       usageClass: StateUsage.class,       defClass: StateDefinition.class,
     usageColor: "#C39BD3", defColor: "#8E44AD", usageEnabled: true, defEnabled: true],

    [name: "UseCase",     usageClass: UseCaseUsage.class,     defClass: UseCaseDefinition.class,
     usageColor: "#48C9B0", defColor: "#17A589", usageEnabled: true, defEnabled: true],

    [name: "Constraint",  usageClass: ConstraintUsage.class,  defClass: ConstraintDefinition.class,
     usageColor: "#F1948A", defColor: "#E74C3C", usageEnabled: true, defEnabled: true],

    [name: "Concern",     usageClass: ConcernUsage.class,     defClass: ConcernDefinition.class,
     usageColor: "#F0B27A", defColor: "#CA6F1E", usageEnabled: false, defEnabled: false],

    [name: "View",        usageClass: ViewUsage.class,        defClass: ViewDefinition.class,
     usageColor: "#7FB3D8", defColor: "#2C3E50", usageEnabled: false, defEnabled: false],

    [name: "Viewpoint",   usageClass: ViewpointUsage.class,   defClass: ViewpointDefinition.class,
     usageColor: "#9FA8DA", defColor: "#3F51B5", usageEnabled: false, defEnabled: false],

    [name: "Connection",  usageClass: ConnectionUsage.class,  defClass: ConnectionDefinition.class,
     usageColor: "#FFCC80", defColor: "#FF8F00", usageEnabled: false, defEnabled: false],

    [name: "Interface",   usageClass: InterfaceUsage.class,   defClass: InterfaceDefinition.class,
     usageColor: "#80DEEA", defColor: "#00838F", usageEnabled: false, defEnabled: false],

    [name: "Port",        usageClass: PortUsage.class,        defClass: PortDefinition.class,
     usageColor: "#BCAAA4", defColor: "#6D4C41", usageEnabled: false, defEnabled: false],

    [name: "Item",        usageClass: ItemUsage.class,        defClass: ItemDefinition.class,
     usageColor: "#F48FB1", defColor: "#C62828", usageEnabled: false, defEnabled: false],

    [name: "Attribute",   usageClass: AttributeUsage.class,   defClass: AttributeDefinition.class,
     usageColor: "#B0BEC5", defColor: "#546E7A", usageEnabled: false, defEnabled: false],

    [name: "Occurrence",  usageClass: OccurrenceUsage.class,  defClass: OccurrenceDefinition.class,
     usageColor: "#C5E1A5", defColor: "#558B2F", usageEnabled: false, defEnabled: false],

    [name: "Allocation",  usageClass: AllocationUsage.class,  defClass: AllocationDefinition.class,
     usageColor: "#CE93D8", defColor: "#6A1B9A", usageEnabled: false, defEnabled: false],

    [name: "Package",     usageClass: Package.class,          defClass: null,
     usageColor: "#FFC300", defColor: "#FFC300", usageEnabled: true, defEnabled: false],
]

// =============================================================================
// SECTION 3: Edge Type Registry
// =============================================================================
def edgeTypeRegistry = [
    [name: "Satisfy",     enabled: true,  color: "#27AE60", dashed: true, startArrow: "none", endArrow: mxConstants.ARROW_BLOCK],
    [name: "Dependency",  enabled: true,  color: "#7F8C8D", dashed: true, startArrow: "none", endArrow: mxConstants.ARROW_OPEN],
    [name: "Allocation",  enabled: true,  color: "#8E44AD", dashed: true, startArrow: "none", endArrow: mxConstants.ARROW_BLOCK],
    [name: "Containment", enabled: true,  color: "#95A5A6", dashed: false, startArrow: "oval", endArrow: "none"],
    [name: "Owns",        enabled: true,  color: "#95A5A6", dashed: false, startArrow: "oval", endArrow: "none"],
    [name: "References",  enabled: true,  color: "#D35400", dashed: true, startArrow: "none", endArrow: mxConstants.ARROW_OPEN],
    [name: "Reference Subsetting", enabled: true, color: "#E67E22", dashed: false, startArrow: "none", endArrow: mxConstants.ARROW_BLOCK],
    [name: "Subsetting",  enabled: true,  color: "#E67E22", dashed: false, startArrow: "none", endArrow: mxConstants.ARROW_BLOCK],
    [name: "FeatureMembership", enabled: true, color: "#3498DB", dashed: false, startArrow: "oval", endArrow: "none"],
    [name: "Feature Typing", enabled: true, color: "#2E86C1", dashed: true, startArrow: "none", endArrow: mxConstants.ARROW_OPEN],
    [name: "Import",      enabled: true,  color: "#16A085", dashed: true, startArrow: "none", endArrow: mxConstants.ARROW_OPEN]
]

// =============================================================================
// SECTION 4: State Variables
// =============================================================================
String[] currentThemeName = ["Dark"]
Element[] currentRoot = [null]
int[] scanTimeMs = [0]
boolean[] isDenseMode = [false]
boolean[] filterStandardLibs = [true]
boolean[] traversePackageChildren = [false]
boolean[] filterLegend = [false]
Map<Element, Integer> expandedNodes = [:]
def elementToCellMapGlobal = [null]
def usedLegendItems = [node: new HashSet<String>(), edge: new HashSet<String>()] // For legend filtering
File[] lastExportDir = [null]
Map layoutConfigs = [
    "Hierarchical": [intra: 60, inter: 90],
    "Hierarchical (L-R)": [intra: 60, inter: 90],
    "Circle": [radius: 180],
    "Compact Tree": [nodeDist: 60, levelDist: 90, groupPadding: 14],
    "Tree (Horiz)": [nodeDist: 60, levelDist: 90, groupPadding: 14],
    "Organic": [force: 120, min: 90],
    "Organic (Fast)": [force: 120, min: 90],
    "Composite (Nested)": [nodeDist: 40, levelDist: 60, padding: 30]
]

// Shape catalog: maps shape ID strings to human-readable names
def SHAPE_CATALOG = [
    "rectangle":    "Rectangle",
    "roundedRect":  "Rounded Rectangle",
    "ellipse":      "Ellipse",
    "doubleEllipse":"Double Ellipse",
    "circle":       "Circle",
    "diamond":      "Diamond",
    "hexagon":      "Hexagon",
    "triangle":     "Triangle",
    "cloud":        "Cloud",
    "cylinder":     "Cylinder",
    "folder":       "Folder",
    "actor":        "Person/Actor"
]

// Arrow catalog: maps arrow ID strings to human-readable names
def ARROW_CATALOG = [
    "none":         "None",
    "open":         "Open Arrow",
    "block":        "Filled Arrow",
    "classic":      "Classic Arrow",
    "oval":         "Circle/Dot",
    "diamond":      "Diamond",
    "diamondThin":  "Thin Diamond"
]

// Font family catalog: curated list of fonts available in MagicDraw's JRE
def FONT_CATALOG = [
    "Roboto",
    "Arial",
    "Helvetica",
    "SansSerif",
    "Serif",
    "Monospaced",
    "Dialog",
    "Verdana",
    "Tahoma",
    "Segoe UI",
    "Calibri",
    "Consolas"
]

// Fill in missing default style parameters for nodeTypeRegistry and edgeTypeRegistry
nodeTypeRegistry.each { nt ->
    if (!nt.containsKey("defStroke")) nt.defStroke = "#333333"
    if (!nt.containsKey("defText")) nt.defText = "#FFFFFF"
    if (!nt.containsKey("defWidth")) nt.defWidth = 2
    if (!nt.containsKey("usageStroke")) nt.usageStroke = "#333333"
    if (!nt.containsKey("usageText")) nt.usageText = "#FFFFFF"
    if (!nt.containsKey("usageWidth")) nt.usageWidth = 2
    // Shape defaults: usages = rounded rect, definitions = sharp rectangle
    if (!nt.containsKey("usageShape")) nt.usageShape = "roundedRect"
    if (!nt.containsKey("defShape")) nt.defShape = "rectangle"
    if (!nt.containsKey("denseUsageShape")) nt.denseUsageShape = "circle"
    if (!nt.containsKey("denseDefShape")) nt.denseDefShape = "diamond"
    if (!nt.containsKey("usageFontStyle")) nt.usageFontStyle = "bold"
    if (!nt.containsKey("defFontStyle")) nt.defFontStyle = "bold"
    if (!nt.containsKey("usageFontFamily")) nt.usageFontFamily = "Roboto"
    if (!nt.containsKey("defFontFamily")) nt.defFontFamily = "Roboto"
}
// Special shape defaults by SysMLv2 convention
nodeTypeRegistry.find { it.name == "UseCase" }?.with { it.usageShape = it.usageShape ?: "ellipse"; it.defShape = it.defShape ?: "ellipse" }
nodeTypeRegistry.find { it.name == "Package" }?.with { it.usageShape = "folder"; it.defShape = "folder" }
nodeTypeRegistry.find { it.name == "Constraint" }?.with { it.denseUsageShape = it.denseUsageShape ?: "diamond"; it.denseDefShape = it.denseDefShape ?: "diamond" }

edgeTypeRegistry.each { et ->
    if (!et.containsKey("width")) et.width = 2
    if (!et.containsKey("textColor")) et.textColor = "#BBBBCC"
    if (!et.containsKey("fontStyle")) et.fontStyle = "plain"
    if (!et.containsKey("fontSize")) et.fontSize = 10
    if (!et.containsKey("fontFamily")) et.fontFamily = "SansSerif"
}


// =============================================================================
// SECTION 5: Helper Closures
// =============================================================================
def getLabel = { el ->
    try {
        String n = el.getHumanName()
        if (n != null && !n.isEmpty()) return n
        n = el.getDeclaredName()
        if (n != null && !n.isEmpty()) return n
        return el.getClass().getSimpleName().replaceAll("Impl\$", "")
    } catch (Exception e) { return "?" }
}

// Find which node type config matches an element. Returns [config, isDef] or null.
def findNodeType = { el ->
    def result = null
    int ni = 0
    while (ni < nodeTypeRegistry.size() && result == null) {
        def config = nodeTypeRegistry[ni]
        // Check definition FIRST (more specific subclasses may match both)
        if (config.defClass != null && config.defClass.isInstance(el)) { result = [config, true] }
        else if (config.usageClass != null && config.usageClass.isInstance(el)) { result = [config, false] }
        ni++
    }
    return result
}

// Check if element is a valid (enabled) node type
def isEnabledNode = { el ->
    if (filterStandardLibs[0]) {
        try {
            if (!el.isEditable()) return false // Read-only standard profiles are blocked
            
            String qn = el.respondsTo("getQualifiedName") ? el.getQualifiedName() : null
            if (qn != null) {
                String lqn = qn.toLowerCase()
                if (lqn.startsWith("sysml::") || lqn.startsWith("kerml::") || lqn.contains("::sysml::") || lqn.contains("::kerml::") || lqn.startsWith("uml standard profile")) {
                    return false
                }
            }
            
            def walker = el
            while (walker != null) {
                String n = walker.getName()
                if (n == "SysML" || n == "KerML" || n == "UML Standard Profile" || n == "MD Customization for SysML" || n == "SysML 2.0 Library") {
                    return false
                }
                walker = walker.getOwner()
            }
            if (el.getHumanName() != null && (el.getHumanName().contains("from SysML") || el.getHumanName().contains("from KerML"))) {
                return false
            }
        } catch (Exception ignore) {}
    }

    def match = findNodeType(el)
    if (match == null) return false
    def config = match[0]
    boolean isDef = match[1]
    return isDef ? config.defEnabled : config.usageEnabled
}

// Get mxGraph style string for a node
def getNodeStyle = { el, boolean isRoot, String labelPos = "Inside" ->
    def match = findNodeType(el)
    if (match == null) return ""
    def config = match[0]
    boolean isDef = match[1]
    String fillColor = isDef ? config.defColor : config.usageColor
    String strokeColorCfg = isDef ? config.defStroke : config.usageStroke
    int strokeWidthCfg = isDef ? config.defWidth : config.usageWidth
    String textColor = isDef ? config.defText : config.usageText
    String fontStyleStr = isDef ? (config.defFontStyle ?: "bold") : (config.usageFontStyle ?: "bold")
    String fontFamily = isDef ? (config.defFontFamily ?: "Roboto") : (config.usageFontFamily ?: "Roboto")
    int fontStyle = 0
    if (fontStyleStr == "bold") fontStyle = 1
    else if (fontStyleStr == "italic") fontStyle = 2
    else if (fontStyleStr == "bolditalic") fontStyle = 3
    
    // Shape resolution
    String shapeId = isDef ? (config.defShape ?: "rectangle") : (config.usageShape ?: "roundedRect")
    if (isDenseMode[0]) shapeId = isDef ? (config.denseDefShape ?: "diamond") : (config.denseUsageShape ?: "circle")
    
    // Map shape ID to JGraphX style fragments
    String shapePart
    String extraParts = ""
    if (shapeId == "rectangle") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_RECTANGLE}"
        extraParts = "${mxConstants.STYLE_ROUNDED}=0"
    } else if (shapeId == "roundedRect") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_RECTANGLE}"
        extraParts = "${mxConstants.STYLE_ROUNDED}=1;${mxConstants.STYLE_ARCSIZE}=15"
    } else if (shapeId == "ellipse" || shapeId == "circle") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_ELLIPSE}"
    } else if (shapeId == "doubleEllipse") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_DOUBLE_ELLIPSE}"
    } else if (shapeId == "diamond") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_RHOMBUS}"
    } else if (shapeId == "hexagon") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_HEXAGON}"
    } else if (shapeId == "triangle") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_TRIANGLE}"
    } else if (shapeId == "cloud") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_CLOUD}"
    } else if (shapeId == "cylinder") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_CYLINDER}"
    } else if (shapeId == "folder") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_SWIMLANE}"
        extraParts = "${mxConstants.STYLE_STARTSIZE}=22;${mxConstants.STYLE_SWIMLANE_FILLCOLOR}=#FFF5D1"
    } else if (shapeId == "actor") {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_ACTOR}"
    } else {
        shapePart = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_RECTANGLE}"
        extraParts = "${mxConstants.STYLE_ROUNDED}=1;${mxConstants.STYLE_ARCSIZE}=15"
    }
    
    def theme = THEMES[currentThemeName[0]]
    String strokeColor = isRoot ? theme.rootBorder : strokeColorCfg
    float strokeWidth = isRoot ? 3.0f : (float) strokeWidthCfg

    String style = "${shapePart};" +
           (extraParts ? "${extraParts};" : "") +
           "${mxConstants.STYLE_FILLCOLOR}=${fillColor};" +
           "${mxConstants.STYLE_FONTCOLOR}=${textColor};" +
           "${mxConstants.STYLE_FONTFAMILY}=${fontFamily};" +
           "${mxConstants.STYLE_FONTSIZE}=12;" +
           "${mxConstants.STYLE_FONTSTYLE}=${fontStyle};" +
           "${mxConstants.STYLE_SHADOW}=1;" +
           "${mxConstants.STYLE_STROKECOLOR}=${strokeColor};" +
           "${mxConstants.STYLE_STROKEWIDTH}=${strokeWidth}"

    if (labelPos == "Above") {
        style += ";${mxConstants.STYLE_VERTICAL_LABEL_POSITION}=top;${mxConstants.STYLE_LABEL_POSITION}=center"
    } else if (labelPos == "Below") {
        style += ";${mxConstants.STYLE_VERTICAL_LABEL_POSITION}=bottom;${mxConstants.STYLE_LABEL_POSITION}=center"
    } else if (labelPos == "Hidden" || isDenseMode[0]) {
        style += ";${mxConstants.STYLE_NOLABEL}=1"
    }
    
    return style
}

def getEdgeStyle = { String edgeTypeName ->
    def edgeConfig = edgeTypeRegistry.find { it.name == edgeTypeName }
    String color = edgeConfig?.color ?: "#888888"
    int lineWidth = edgeConfig?.width ?: 2
    boolean dashed = edgeConfig?.dashed ?: false
    String startArrow = edgeConfig?.startArrow ?: "none"
    String endArrow = edgeConfig?.endArrow ?: mxConstants.ARROW_OPEN
    String textColor = edgeConfig?.textColor ?: "#BBBBCC"
    String fontStyle = edgeConfig?.fontStyle ?: "plain"
    int fontSize = edgeConfig?.fontSize ?: 10
    String fontFamily = edgeConfig?.fontFamily ?: "SansSerif"
    int jFontStyle = 0
    if (fontStyle == "bold") jFontStyle = 1
    else if (fontStyle == "italic") jFontStyle = 2
    else if (fontStyle == "bolditalic") jFontStyle = 3
    return "${mxConstants.STYLE_STROKECOLOR}=${color};" +
           "${mxConstants.STYLE_STROKEWIDTH}=${lineWidth};" +
           "${mxConstants.STYLE_FONTCOLOR}=${textColor};" +
           "${mxConstants.STYLE_FONTFAMILY}=${fontFamily};" +
           "${mxConstants.STYLE_FONTSIZE}=${fontSize};" +
           "${mxConstants.STYLE_FONTSTYLE}=${jFontStyle};" +
           "${mxConstants.STYLE_ROUNDED}=1;" +
           "${mxConstants.STYLE_STARTARROW}=${startArrow};" +
           "${mxConstants.STYLE_ENDARROW}=${endArrow};" +
           "${mxConstants.STYLE_DASHED}=${dashed ? 1 : 0}"
}

// Check if an edge type is enabled
def isEdgeEnabled = { String edgeTypeName ->
    def config = edgeTypeRegistry.find { it.name == edgeTypeName }
    return config?.enabled ?: false
}


// Forward-declare rebuildGraph so UI listeners can reference it before its full definition
def rebuildGraph = null

try {
    Project project = Application.getInstance().getProject()
    if (project == null) {
        logger.error("No active MagicDraw project!")
        return
    }
    logDebug("Initializing V2Map Production Viewer...")

    // =========================================================================
    // SECTION 6: mxGraph Canvas
    // =========================================================================
    def mxg = new mxGraph() {
        String getToolTipForCell(Object cell) {
            if (this.getModel().isVertex(cell)) {
                return this.getModel().getValue(cell)?.toString()
            }
            return super.getToolTipForCell(cell)
        }
    }
    mxg.setAllowDanglingEdges(false)
    mxg.setCellsEditable(false)
    mxg.setConnectableEdges(false)

    def graphComponent = new mxGraphComponent(mxg)
    graphComponent.getGraphControl().setToolTipText(" ") // Enable tooltips
    graphComponent.setConnectable(false)
    graphComponent.setDragEnabled(false)

    def applyCanvasTheme = {
        def theme = THEMES[currentThemeName[0]]
        graphComponent.getViewport().setOpaque(true)
        graphComponent.getViewport().setBackground(theme.canvasBg)
        graphComponent.setBackground(theme.canvasBg)
    }
    applyCanvasTheme()

    // =========================================================================
    // SECTION 7: Toolbar
    // =========================================================================
    // --- Search ---
    JTextField searchField = new JTextField(15)
    searchField.setToolTipText("Search for a node by name (Enter to find)")
    JButton searchButton = new JButton("Find")

    // --- Depth ---
    JLabel depthLabel = new JLabel("Depth:")
    SpinnerNumberModel depthModel = new SpinnerNumberModel(3, 1, 10, 1)
    JSpinner depthSpinner = new JSpinner(depthModel)
    depthSpinner.setPreferredSize(new Dimension(50, 26))
    JLabel depthWarning = new JLabel("")

    // --- Layout ---
    JLabel layoutLabel = new JLabel("Layout:")
    JComboBox<String> layoutCombo = new JComboBox<>(["Hierarchical", "Hierarchical (L-R)", "Circle", "Compact Tree", "Tree (Horiz)", "Organic", "Organic (Fast)", "Composite (Nested)"] as String[])
    layoutCombo.setPreferredSize(new Dimension(140, 26))

    // --- Theme ---
    JLabel themeLabel = new JLabel("Theme:")
    JComboBox<String> themeCombo = new JComboBox<>(["Dark", "Light", "Hello Kitty"] as String[])
    themeCombo.setPreferredSize(new Dimension(110, 26))

    // --- Node Labels ---
    JLabel labelPosLabel = new JLabel("Node Labels:")
    JComboBox<String> labelPosCombo = new JComboBox<>(["Inside", "Above", "Below", "Hidden"] as String[])
    labelPosCombo.setPreferredSize(new Dimension(90, 26))

    // --- Edge Labels ---
    JComboBox<String> edgeLabelCombo = new JComboBox<>(["Show", "Hidden"] as String[])
    edgeLabelCombo.setPreferredSize(new Dimension(90, 26))

    // --- Display Mode ---
    JCheckBox denseModeCheck = new JCheckBox("Dense", false)
    denseModeCheck.setToolTipText("Prioritize space using small icons without labels")
    
    JCheckBox filterLibsCheck = new JCheckBox("Hide Std Libs", true)
    filterLibsCheck.setToolTipText("Hide elements stored in SysML or KerML library packages")
    
    JCheckBox traversePkgCheck = new JCheckBox("Traverse Pkgs", false)
    traversePkgCheck.setToolTipText("If off, stops children of packages from being explored")
    
    JCheckBox filterLegendCheck = new JCheckBox("Auto-Hide Legend", false)
    filterLegendCheck.setToolTipText("Hide legend checks for items that are not actively displayed")

    // --- Save / Load / Export ---
    JButton saveButton = new JButton("Save")
    JButton loadButton = new JButton("Load")
    JButton exportButton = new JButton("Export IMG")

    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3))
    toolbar.add(new JLabel(" "))
    toolbar.add(searchField)
    toolbar.add(searchButton)
    toolbar.add(Box.createHorizontalStrut(12))
    toolbar.add(depthLabel)
    toolbar.add(depthSpinner)
    toolbar.add(depthWarning)
    toolbar.add(Box.createHorizontalStrut(8))
    toolbar.add(layoutLabel)
    toolbar.add(layoutCombo)
    
    JButton layoutSettingsButton = new JButton("\u2699")
    layoutSettingsButton.setToolTipText("Layout Parameters")
    layoutSettingsButton.setMargin(new Insets(2, 4, 2, 4))
    toolbar.add(layoutSettingsButton)
    
    // --- Advanced Dialog ---
    JButton advancedConfigButton = new JButton("Advanced \u2699")
    toolbar.add(Box.createHorizontalStrut(8))
    toolbar.add(advancedConfigButton)
    toolbar.add(Box.createHorizontalStrut(8))
    toolbar.add(exportButton)

    JDialog advDialog = new JDialog((Frame)null, "Advanced Configuration", false)
    advDialog.setSize(500, 350)
    advDialog.setLocationRelativeTo(null)
    JPanel advPanel = new JPanel(new GridLayout(0, 2, 8, 8))
    advPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))

    advPanel.add(new JLabel("Map Theme:"))
    advPanel.add(themeCombo)
    advPanel.add(new JLabel("Node Labels:"))
    advPanel.add(labelPosCombo)
    advPanel.add(new JLabel("Edge Labels:"))
    advPanel.add(edgeLabelCombo)
    advPanel.add(new JLabel("Display Modes:"))
    advPanel.add(denseModeCheck)
    advPanel.add(new JLabel("Library Filters:"))
    advPanel.add(filterLibsCheck)
    advPanel.add(new JLabel("Traversal Constraint:"))
    advPanel.add(traversePkgCheck)
    advPanel.add(new JLabel("Legend Behavior:"))
    advPanel.add(filterLegendCheck)

    advPanel.add(new JLabel("Load / Save Config:"))
    JPanel confPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0))
    confPanel.add(saveButton)
    confPanel.add(loadButton)
    advPanel.add(confPanel)

    advDialog.add(advPanel)
    advancedConfigButton.addActionListener { e -> advDialog.setVisible(true) }

    // --- Layout Settings Dialog ---
    JDialog layoutDialog = new JDialog((Frame)null, "Layout Settings", false)
    layoutDialog.setSize(280, 200)
    layoutDialog.setLocationRelativeTo(null)
    JPanel layoutPanel = new JPanel(new GridLayout(0, 2, 8, 8))
    layoutPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    layoutDialog.add(layoutPanel)

    layoutSettingsButton.addActionListener { e ->
        layoutPanel.removeAll()
        String l = layoutCombo.getSelectedItem()
        Map cfg = layoutConfigs[l]
        if (cfg != null) {
            cfg.keySet().each { String k ->
                layoutPanel.add(new JLabel(k + ":"))
                JSpinner spinner = new JSpinner(new SpinnerNumberModel((int)cfg[k], 5, 1000, 5))
                spinner.addChangeListener { evt ->
                    cfg[k] = (int)spinner.getValue()
                    if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
                }
                layoutPanel.add(spinner)
            }
        }
        layoutDialog.setTitle(l + " Settings")
        layoutPanel.revalidate()
        layoutPanel.repaint()
        layoutDialog.setVisible(true)
    }


    // --- Status bar ---
    JLabel statusLabel = new JLabel("  Select an element in the Containment Tree to begin.")
    statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12))
    statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8))

    // =========================================================================
    // SECTION 7.1: UI Icon Renderer logic
    // =========================================================================
    def createColorIcon = { String fillC, String strokeC, int widthV, String shapeId ->
        return new Icon() {
            @Override void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create()
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setColor(Color.decode(fillC ?: "#FFFFFF"))
                String sid = shapeId ?: "roundedRect"
                if (sid == "ellipse" || sid == "circle" || sid == "doubleEllipse") {
                    g2.fillOval(x, y, 14, 14)
                } else if (sid == "diamond") {
                    int[] xp = [x+7, x+14, x+7, x]; int[] yp = [y, y+7, y+14, y+7]
                    g2.fillPolygon(xp, yp, 4)
                } else if (sid == "hexagon") {
                    int[] hx = [x+3, x+11, x+14, x+11, x+3, x]; int[] hy = [y, y, y+7, y+14, y+14, y+7]
                    g2.fillPolygon(hx, hy, 6)
                } else if (sid == "triangle") {
                    int[] tx = [x+7, x+14, x]; int[] ty = [y, y+14, y+14]
                    g2.fillPolygon(tx, ty, 3)
                } else if (sid == "cylinder") {
                    g2.fillRect(x, y+3, 14, 8); g2.fillOval(x, y, 14, 6); g2.fillOval(x, y+5, 14, 6)
                } else if (sid == "rectangle") {
                    g2.fillRect(x, y, 14, 14)
                } else if (sid == "folder") {
                    g2.fillRect(x, y+3, 14, 11); g2.fillRect(x, y, 7, 4)
                } else if (sid == "actor") {
                    g2.fillOval(x+4, y, 6, 6); g2.fillRect(x+5, y+6, 4, 5); g2.fillRect(x+2, y+7, 10, 2)
                } else {
                    g2.fillRoundRect(x, y, 14, 14, 6, 6)
                }
                g2.setColor(Color.decode(strokeC ?: "#333333"))
                g2.setStroke(new BasicStroke(Math.max(widthV, 1)))
                if (sid == "ellipse" || sid == "circle" || sid == "doubleEllipse") {
                    g2.drawOval(x, y, 14, 14)
                    if (sid == "doubleEllipse") { g2.drawOval(x+2, y+2, 10, 10) }
                } else if (sid == "diamond") {
                    int[] xp2 = [x+7, x+14, x+7, x]; int[] yp2 = [y, y+7, y+14, y+7]
                    g2.drawPolygon(xp2, yp2, 4)
                } else if (sid == "hexagon") {
                    int[] hx2 = [x+3, x+11, x+14, x+11, x+3, x]; int[] hy2 = [y, y, y+7, y+14, y+14, y+7]
                    g2.drawPolygon(hx2, hy2, 6)
                } else if (sid == "triangle") {
                    int[] tx2 = [x+7, x+14, x]; int[] ty2 = [y, y+14, y+14]
                    g2.drawPolygon(tx2, ty2, 3)
                } else if (sid == "cylinder") {
                    g2.drawRect(x, y+3, 14, 8); g2.drawOval(x, y, 14, 6)
                } else if (sid == "rectangle") {
                    g2.drawRect(x, y, 14, 14)
                } else if (sid == "folder") {
                    g2.drawRect(x, y+3, 14, 11); g2.drawRect(x, y, 7, 4)
                } else if (sid == "actor") {
                    g2.drawOval(x+4, y, 6, 6)
                } else {
                    g2.drawRoundRect(x, y, 14, 14, 6, 6)
                }
                g2.dispose()
            }
            @Override int getIconWidth() { return 16 }
            @Override int getIconHeight() { return 16 }
        }
    }
    def createLineIcon = { String strokeC, int widthV ->
        return new Icon() {
            @Override void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create()
                g2.setColor(Color.decode(strokeC ?: "#333333"))
                g2.setStroke(new BasicStroke(widthV))
                g2.drawLine(x, y + 8, x + 16, y + 8)
                g2.dispose()
            }
            @Override int getIconWidth() { return 16 }
            @Override int getIconHeight() { return 16 }
        }
    }

    // =========================================================================
    // SECTION 7.2: Node Style Dialog
    // =========================================================================
    def showNodeStyleDialog = { config, boolean isDef, JLabel swatchLabel ->
        String prefix = isDef ? "def" : "usage"
        String title = isDef ? "${config.name} Definition Style" : "${config.name} Usage Style"
        
        JPanel panel = new JPanel()
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS))
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12))
        
        // --- Live Preview ---
        String[] previewFill = [isDef ? config.defColor : config.usageColor]
        String[] previewStroke = [isDef ? config.defStroke : config.usageStroke]
        int[] previewWidth = [isDef ? config.defWidth : config.usageWidth]
        String[] previewText = [isDef ? config.defText : config.usageText]
        String[] previewShape = [isDef ? (config.defShape ?: "rectangle") : (config.usageShape ?: "roundedRect")]
        String[] previewDenseShape = [isDef ? (config.denseDefShape ?: "diamond") : (config.denseUsageShape ?: "circle")]
        String[] previewFontStyle = [isDef ? (config.defFontStyle ?: "bold") : (config.usageFontStyle ?: "bold")]
        String[] previewFontFamily = [isDef ? (config.defFontFamily ?: "Roboto") : (config.usageFontFamily ?: "Roboto")]
        
        JPanel previewPanel = new JPanel() {
            @Override void paintComponent(Graphics g) {
                super.paintComponent(g)
                Graphics2D g2 = (Graphics2D) g
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                int w = getWidth() - 20, h = getHeight() - 20
                g2.setColor(Color.decode(previewFill[0]))
                String sh = previewShape[0]
                if (sh == "ellipse" || sh == "circle" || sh == "doubleEllipse") { g2.fillOval(10, 10, w, h) }
                else if (sh == "diamond") {
                    int[] xp = [10+w.intdiv(2), 10+w, 10+w.intdiv(2), 10]; int[] yp = [10, 10+h.intdiv(2), 10+h, 10+h.intdiv(2)]
                    g2.fillPolygon(xp, yp, 4)
                } else if (sh == "hexagon") {
                    int[] hx = [10+w.intdiv(4), 10+3*w.intdiv(4), 10+w, 10+3*w.intdiv(4), 10+w.intdiv(4), 10]; int[] hy = [10, 10, 10+h.intdiv(2), 10+h, 10+h, 10+h.intdiv(2)]
                    g2.fillPolygon(hx, hy, 6)
                } else if (sh == "triangle") {
                    int[] tx = [10+w.intdiv(2), 10+w, 10]; int[] ty = [10, 10+h, 10+h]
                    g2.fillPolygon(tx, ty, 3)
                } else if (sh == "rectangle") { g2.fillRect(10, 10, w, h) }
                else if (sh == "cylinder") { g2.fillRect(10, 18, w, h-8); g2.fillOval(10, 10, w, 16) }
                else if (sh == "folder") { g2.fillRect(10, 18, w, h-8); g2.fillRect(10, 10, (int)(w*0.4), 10) }
                else { g2.fillRoundRect(10, 10, w, h, 16, 16) }
                g2.setColor(Color.decode(previewStroke[0]))
                g2.setStroke(new BasicStroke(previewWidth[0]))
                if (sh == "ellipse" || sh == "circle" || sh == "doubleEllipse") {
                    g2.drawOval(10, 10, w, h)
                    if (sh == "doubleEllipse") { g2.drawOval(14, 14, w-8, h-8) }
                }
                else if (sh == "diamond") {
                    int[] xp2 = [10+w.intdiv(2), 10+w, 10+w.intdiv(2), 10]; int[] yp2 = [10, 10+h.intdiv(2), 10+h, 10+h.intdiv(2)]
                    g2.drawPolygon(xp2, yp2, 4)
                } else if (sh == "rectangle") { g2.drawRect(10, 10, w, h) }
                else { g2.drawRoundRect(10, 10, w, h, 16, 16) }
                // Draw sample text
                int fs = 0
                if (previewFontStyle[0] == "bold") fs = Font.BOLD
                else if (previewFontStyle[0] == "italic") fs = Font.ITALIC
                else if (previewFontStyle[0] == "bolditalic") fs = Font.BOLD | Font.ITALIC
                g2.setFont(new Font(previewFontFamily[0], fs, 12))
                g2.setColor(Color.decode(previewText[0]))
                def fm = g2.getFontMetrics()
                String sampleTxt = config.name
                int tx = 10 + (int)((w - fm.stringWidth(sampleTxt)).intdiv(2))
                int ty = 10 + (int)((h + fm.getAscent()).intdiv(2)) - 2
                g2.drawString(sampleTxt, tx, ty)
            }
        }
        previewPanel.setPreferredSize(new Dimension(200, 70))
        previewPanel.setMaximumSize(new Dimension(9999, 70))
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Preview"),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ))
        previewPanel.setBackground(Color.WHITE)
        previewPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        panel.add(previewPanel)
        panel.add(Box.createVerticalStrut(8))
        
        // --- Shape ---
        JPanel shapePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        shapePanel.setBorder(BorderFactory.createTitledBorder("Shape"))
        shapePanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        String[] shapeKeys = SHAPE_CATALOG.keySet().toArray(new String[0])
        String[] shapeNames = shapeKeys.collect { SHAPE_CATALOG[it] } as String[]
        JComboBox shapeCombo = new JComboBox(shapeNames)
        int shapeIdx = shapeKeys.toList().indexOf(previewShape[0])
        if (shapeIdx >= 0) shapeCombo.setSelectedIndex(shapeIdx)
        shapeCombo.addActionListener { ev -> previewShape[0] = shapeKeys[shapeCombo.getSelectedIndex()]; previewPanel.repaint() }
        shapePanel.add(new JLabel("Normal:"))
        shapePanel.add(shapeCombo)
        
        JComboBox denseCombo = new JComboBox(shapeNames)
        int denseIdx = shapeKeys.toList().indexOf(previewDenseShape[0])
        if (denseIdx >= 0) denseCombo.setSelectedIndex(denseIdx)
        denseCombo.addActionListener { ev -> previewDenseShape[0] = shapeKeys[denseCombo.getSelectedIndex()] }
        shapePanel.add(new JLabel("  Dense:"))
        shapePanel.add(denseCombo)
        panel.add(shapePanel)
        panel.add(Box.createVerticalStrut(4))
        
        // --- Fill ---
        JPanel fillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        fillPanel.setBorder(BorderFactory.createTitledBorder("Fill"))
        fillPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        JLabel fillSwatch = new JLabel("  ")
        fillSwatch.setOpaque(true); fillSwatch.setBackground(Color.decode(previewFill[0]))
        fillSwatch.setPreferredSize(new Dimension(24, 18))
        JButton fillBtn = new JButton("Pick...")
        fillBtn.addActionListener { ev ->
            Color c = JColorChooser.showDialog(null, "Fill Color", Color.decode(previewFill[0]))
            if (c != null) {
                previewFill[0] = "#${Integer.toHexString(c.getRGB() & 0xFFFFFF).padLeft(6, '0').toUpperCase()}"
                fillSwatch.setBackground(c); previewPanel.repaint()
            }
        }
        fillPanel.add(new JLabel("Color:")); fillPanel.add(fillSwatch); fillPanel.add(fillBtn)
        panel.add(fillPanel)
        panel.add(Box.createVerticalStrut(4))
        
        // --- Outline ---
        JPanel outlinePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        outlinePanel.setBorder(BorderFactory.createTitledBorder("Outline"))
        outlinePanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        JLabel strokeSwatch = new JLabel("  ")
        strokeSwatch.setOpaque(true); strokeSwatch.setBackground(Color.decode(previewStroke[0]))
        strokeSwatch.setPreferredSize(new Dimension(24, 18))
        JButton strokeBtn = new JButton("Pick...")
        strokeBtn.addActionListener { ev ->
            Color c = JColorChooser.showDialog(null, "Outline Color", Color.decode(previewStroke[0]))
            if (c != null) {
                previewStroke[0] = "#${Integer.toHexString(c.getRGB() & 0xFFFFFF).padLeft(6, '0').toUpperCase()}"
                strokeSwatch.setBackground(c); previewPanel.repaint()
            }
        }
        SpinnerNumberModel widthModel = new SpinnerNumberModel(previewWidth[0], 1, 8, 1)
        JSpinner widthSpinner = new JSpinner(widthModel)
        widthSpinner.setPreferredSize(new Dimension(50, 24))
        widthSpinner.addChangeListener { ev -> previewWidth[0] = (int)widthSpinner.getValue(); previewPanel.repaint() }
        outlinePanel.add(new JLabel("Color:")); outlinePanel.add(strokeSwatch); outlinePanel.add(strokeBtn)
        outlinePanel.add(new JLabel("  Width:")); outlinePanel.add(widthSpinner)
        panel.add(outlinePanel)
        panel.add(Box.createVerticalStrut(4))
        
        // --- Text ---
        JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        textPanel.setBorder(BorderFactory.createTitledBorder("Text"))
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        JLabel textSwatch = new JLabel("  ")
        textSwatch.setOpaque(true); textSwatch.setBackground(Color.decode(previewText[0]))
        textSwatch.setPreferredSize(new Dimension(24, 18))
        JButton textBtn = new JButton("Pick...")
        textBtn.addActionListener { ev ->
            Color c = JColorChooser.showDialog(null, "Text Color", Color.decode(previewText[0]))
            if (c != null) {
                previewText[0] = "#${Integer.toHexString(c.getRGB() & 0xFFFFFF).padLeft(6, '0').toUpperCase()}"
                textSwatch.setBackground(c); previewPanel.repaint()
            }
        }
        JComboBox fontStyleCombo = new JComboBox(["plain", "bold", "italic", "bolditalic"] as String[])
        fontStyleCombo.setSelectedItem(previewFontStyle[0])
        fontStyleCombo.addActionListener { ev -> previewFontStyle[0] = (String)fontStyleCombo.getSelectedItem(); previewPanel.repaint() }
        textPanel.add(new JLabel("Color:")); textPanel.add(textSwatch); textPanel.add(textBtn)
        textPanel.add(new JLabel("  Style:")); textPanel.add(fontStyleCombo)
        panel.add(textPanel)
        
        // --- Font ---
        JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font"))
        fontPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        JComboBox fontFamilyCombo = new JComboBox(FONT_CATALOG as String[])
        fontFamilyCombo.setSelectedItem(previewFontFamily[0])
        fontFamilyCombo.setEditable(true)
        fontFamilyCombo.addActionListener { ev -> previewFontFamily[0] = (String)fontFamilyCombo.getSelectedItem(); previewPanel.repaint() }
        fontPanel.add(new JLabel("Family:")); fontPanel.add(fontFamilyCombo)
        panel.add(fontPanel)
        
        // --- Show dialog ---
        int result = JOptionPane.showConfirmDialog(null, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
        if (result == JOptionPane.OK_OPTION) {
            // Apply changes
            if (isDef) {
                config.defColor = previewFill[0]; config.defStroke = previewStroke[0]
                config.defWidth = previewWidth[0]; config.defText = previewText[0]
                config.defShape = previewShape[0]; config.denseDefShape = previewDenseShape[0]
                config.defFontStyle = previewFontStyle[0]
                config.defFontFamily = previewFontFamily[0]
            } else {
                config.usageColor = previewFill[0]; config.usageStroke = previewStroke[0]
                config.usageWidth = previewWidth[0]; config.usageText = previewText[0]
                config.usageShape = previewShape[0]; config.denseUsageShape = previewDenseShape[0]
                config.usageFontStyle = previewFontStyle[0]
                config.usageFontFamily = previewFontFamily[0]
            }
            String sh = isDef ? config.defShape : config.usageShape
            swatchLabel.setIcon(createColorIcon(
                isDef ? config.defColor : config.usageColor,
                isDef ? config.defStroke : config.usageStroke,
                isDef ? config.defWidth : config.usageWidth,
                sh
            ))
            if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
        }
    }

    // =========================================================================
    // SECTION 7.3: Edge Style Dialog
    // =========================================================================
    def showEdgeStyleDialog = { config, JLabel swatchLabel ->
        JPanel panel = new JPanel()
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS))
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12))
        
        String[] previewColor = [config.color ?: "#888888"]
        int[] previewWidth = [config.width ?: 2]
        boolean[] previewDashed = [config.dashed ?: false]
        String[] previewStartArrow = [config.startArrow ?: "none"]
        String[] previewEndArrow = [config.endArrow ?: mxConstants.ARROW_OPEN]
        String[] previewTextColor = [config.textColor ?: "#BBBBCC"]
        String[] previewFontStyle = [config.fontStyle ?: "plain"]
        int[] previewFontSize = [config.fontSize ?: 10]
        String[] previewFontFamily = [config.fontFamily ?: "SansSerif"]
        
        // --- Preview ---
        JPanel previewPanel = new JPanel() {
            @Override void paintComponent(Graphics g) {
                super.paintComponent(g)
                Graphics2D g2 = (Graphics2D) g
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                int w = getWidth(), h = getHeight()
                g2.setColor(Color.decode(previewColor[0]))
                float pw = (float) previewWidth[0]
                if (previewDashed[0]) {
                    g2.setStroke(new BasicStroke(pw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, [6f, 4f] as float[], 0f))
                } else {
                    g2.setStroke(new BasicStroke(pw))
                }
                int hh = h.intdiv(2)
                g2.drawLine(30, hh, w-30, hh)
                // Start arrow indicator
                if (previewStartArrow[0] == "oval") {
                    g2.fillOval(24, hh-4, 8, 8)
                } else if (previewStartArrow[0] != "none") {
                    int[] ax = [30, 20, 30]; int[] ay = [hh-5, hh, hh+5]
                    g2.fillPolygon(ax, ay, 3)
                }
                // End arrow indicator
                String ea = previewEndArrow[0]
                if (ea == "oval" || ea == mxConstants.ARROW_OVAL) {
                    g2.fillOval(w-32, hh-4, 8, 8)
                } else if (ea != "none") {
                    int[] ax = [w-30, w-20, w-30]; int[] ay = [hh-5, hh, hh+5]
                    if (ea == mxConstants.ARROW_OPEN || ea == "open") {
                        g2.setStroke(new BasicStroke(pw)); g2.drawPolyline(ax, ay, 3)
                    } else { g2.fillPolygon(ax, ay, 3) }
                }
                // Label preview
                int efs = 0
                if (previewFontStyle[0] == "bold") efs = Font.BOLD
                else if (previewFontStyle[0] == "italic") efs = Font.ITALIC
                else if (previewFontStyle[0] == "bolditalic") efs = Font.BOLD | Font.ITALIC
                g2.setFont(new Font(previewFontFamily[0], efs, previewFontSize[0]))
                g2.setColor(Color.decode(previewTextColor[0]))
                String lbl = "<<${config.name}>>"
                def fm = g2.getFontMetrics()
                g2.drawString(lbl, (int)((w - fm.stringWidth(lbl)).intdiv(2)), hh - 6)
            }
        }
        previewPanel.setPreferredSize(new Dimension(240, 50))
        previewPanel.setMaximumSize(new Dimension(9999, 50))
        previewPanel.setBackground(Color.WHITE)
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Preview"),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ))
        previewPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        panel.add(previewPanel)
        panel.add(Box.createVerticalStrut(6))
        
        // --- Line ---
        JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        linePanel.setBorder(BorderFactory.createTitledBorder("Line"))
        linePanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        JLabel colorSwatch = new JLabel("  ")
        colorSwatch.setOpaque(true); colorSwatch.setBackground(Color.decode(previewColor[0]))
        colorSwatch.setPreferredSize(new Dimension(24, 18))
        JButton colorBtn = new JButton("Pick...")
        colorBtn.addActionListener { ev ->
            Color c = JColorChooser.showDialog(null, "Edge Color", Color.decode(previewColor[0]))
            if (c != null) {
                previewColor[0] = "#${Integer.toHexString(c.getRGB() & 0xFFFFFF).padLeft(6, '0').toUpperCase()}"
                colorSwatch.setBackground(c); previewPanel.repaint()
            }
        }
        SpinnerNumberModel ewm = new SpinnerNumberModel(previewWidth[0], 1, 8, 1)
        JSpinner ewSpinner = new JSpinner(ewm)
        ewSpinner.setPreferredSize(new Dimension(50, 24))
        ewSpinner.addChangeListener { ev -> previewWidth[0] = (int)ewSpinner.getValue(); previewPanel.repaint() }
        JCheckBox dashCb = new JCheckBox("Dashed", previewDashed[0])
        dashCb.addActionListener { ev -> previewDashed[0] = dashCb.isSelected(); previewPanel.repaint() }
        linePanel.add(new JLabel("Color:")); linePanel.add(colorSwatch); linePanel.add(colorBtn)
        linePanel.add(new JLabel("  W:")); linePanel.add(ewSpinner)
        linePanel.add(dashCb)
        panel.add(linePanel)
        panel.add(Box.createVerticalStrut(4))
        
        // --- Arrows ---
        JPanel arrowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        arrowPanel.setBorder(BorderFactory.createTitledBorder("Arrows"))
        arrowPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        String[] arrowKeys = ARROW_CATALOG.keySet().toArray(new String[0])
        String[] arrowNames = arrowKeys.collect { ARROW_CATALOG[it] } as String[]
        // Map current values to arrow keys
        def findArrowIdx = { String val ->
            if (val == null || val == "none") return 0
            int idx = arrowKeys.toList().indexOf(val)
            if (idx >= 0) return idx
            // Check mxConstants values
            if (val == mxConstants.ARROW_OPEN) return arrowKeys.toList().indexOf("open")
            if (val == mxConstants.ARROW_BLOCK) return arrowKeys.toList().indexOf("block")
            if (val == mxConstants.ARROW_CLASSIC) return arrowKeys.toList().indexOf("classic")
            if (val == mxConstants.ARROW_OVAL) return arrowKeys.toList().indexOf("oval")
            if (val == mxConstants.ARROW_DIAMOND) return arrowKeys.toList().indexOf("diamond")
            if (val == mxConstants.ARROW_DIAMOND_THIN) return arrowKeys.toList().indexOf("diamondThin")
            return 0
        }
        def arrowKeyToConst = { String key ->
            if (key == "none") return "none"
            else if (key == "open") return mxConstants.ARROW_OPEN
            else if (key == "block") return mxConstants.ARROW_BLOCK
            else if (key == "classic") return mxConstants.ARROW_CLASSIC
            else if (key == "oval") return "oval"
            else if (key == "diamond") return mxConstants.ARROW_DIAMOND
            else if (key == "diamondThin") return mxConstants.ARROW_DIAMOND_THIN
            else return mxConstants.ARROW_OPEN
        }
        JComboBox startCombo = new JComboBox(arrowNames)
        startCombo.setSelectedIndex(findArrowIdx(previewStartArrow[0]))
        startCombo.addActionListener { ev -> previewStartArrow[0] = arrowKeys[startCombo.getSelectedIndex()]; previewPanel.repaint() }
        JComboBox endCombo = new JComboBox(arrowNames)
        endCombo.setSelectedIndex(findArrowIdx(previewEndArrow[0]))
        endCombo.addActionListener { ev -> previewEndArrow[0] = arrowKeys[endCombo.getSelectedIndex()]; previewPanel.repaint() }
        arrowPanel.add(new JLabel("Start:")); arrowPanel.add(startCombo)
        arrowPanel.add(new JLabel("  End:")); arrowPanel.add(endCombo)
        panel.add(arrowPanel)
        panel.add(Box.createVerticalStrut(4))
        
        // --- Text ---
        JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        textPanel.setBorder(BorderFactory.createTitledBorder("Label Color"))
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        JLabel textSwatch = new JLabel("  ")
        textSwatch.setOpaque(true); textSwatch.setBackground(Color.decode(previewTextColor[0]))
        textSwatch.setPreferredSize(new Dimension(24, 18))
        JButton textBtn = new JButton("Pick...")
        textBtn.addActionListener { ev ->
            Color c = JColorChooser.showDialog(null, "Edge Text Color", Color.decode(previewTextColor[0]))
            if (c != null) {
                previewTextColor[0] = "#${Integer.toHexString(c.getRGB() & 0xFFFFFF).padLeft(6, '0').toUpperCase()}"
                textSwatch.setBackground(c); previewPanel.repaint()
            }
        }
        textPanel.add(new JLabel("Color:")); textPanel.add(textSwatch); textPanel.add(textBtn)
        panel.add(textPanel)
        panel.add(Box.createVerticalStrut(4))
        
        // --- Font ---
        JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2))
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font"))
        fontPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
        JComboBox edgeFontFamilyCombo = new JComboBox(FONT_CATALOG as String[])
        edgeFontFamilyCombo.setSelectedItem(previewFontFamily[0])
        edgeFontFamilyCombo.setEditable(true)
        edgeFontFamilyCombo.addActionListener { ev -> previewFontFamily[0] = (String)edgeFontFamilyCombo.getSelectedItem(); previewPanel.repaint() }
        fontPanel.add(new JLabel("Family:")); fontPanel.add(edgeFontFamilyCombo)
        JComboBox edgeFontStyleCombo = new JComboBox(["plain", "bold", "italic", "bolditalic"] as String[])
        edgeFontStyleCombo.setSelectedItem(previewFontStyle[0])
        edgeFontStyleCombo.addActionListener { ev -> previewFontStyle[0] = (String)edgeFontStyleCombo.getSelectedItem(); previewPanel.repaint() }
        fontPanel.add(new JLabel("  Style:")); fontPanel.add(edgeFontStyleCombo)
        SpinnerNumberModel fszModel = new SpinnerNumberModel(previewFontSize[0], 6, 24, 1)
        JSpinner fszSpinner = new JSpinner(fszModel)
        fszSpinner.setPreferredSize(new Dimension(50, 24))
        fszSpinner.addChangeListener { ev -> previewFontSize[0] = (int)fszSpinner.getValue(); previewPanel.repaint() }
        fontPanel.add(new JLabel("  Size:")); fontPanel.add(fszSpinner)
        panel.add(fontPanel)
        
        // --- Show dialog ---
        int result = JOptionPane.showConfirmDialog(null, panel, "Style: ${config.name}", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
        if (result == JOptionPane.OK_OPTION) {
            config.color = previewColor[0]
            config.width = previewWidth[0]
            config.dashed = previewDashed[0]
            config.startArrow = arrowKeyToConst(previewStartArrow[0])
            config.endArrow = arrowKeyToConst(previewEndArrow[0])
            config.textColor = previewTextColor[0]
            config.fontStyle = previewFontStyle[0]
            config.fontSize = previewFontSize[0]
            config.fontFamily = previewFontFamily[0]
            swatchLabel.setIcon(createLineIcon(config.color, config.width))
            if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
        }
    }

    // =========================================================================
    // SECTION 8: Control Panel (Left Sidebar)
    // =========================================================================
    JPanel controlPanel = new JPanel()
    controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS))
    controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6))

    // --- Node Types Section ---
    JPanel nodeSection = new JPanel()
    nodeSection.setLayout(new BoxLayout(nodeSection, BoxLayout.Y_AXIS))
    nodeSection.setAlignmentX(Component.LEFT_ALIGNMENT)
    JLabel nodeSectionTitle = new JLabel(" \u25BC Node Types")
    nodeSectionTitle.setFont(new Font("SansSerif", Font.BOLD, 13))
    nodeSectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT)
    nodeSection.add(nodeSectionTitle)
    nodeSection.add(Box.createVerticalStrut(4))

    // Store references for dynamic legend filtering
    java.util.List<JPanel> allNodeRows = []
    java.util.List<String> allNodeRowNames = []
    java.util.List<JPanel> allEdgeRows = []
    java.util.List<String> allEdgeRowNames = []

    def sortedNodes = new ArrayList(nodeTypeRegistry)
    sortedNodes.sort { a, b -> a.name.compareToIgnoreCase(b.name) }

    sortedNodes.each { config ->
        // Usage row
        JPanel usageRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1))
        usageRow.setMaximumSize(new Dimension(9999, 26))
        usageRow.setAlignmentX(Component.LEFT_ALIGNMENT)
        JLabel usageSwatch = new JLabel(createColorIcon(config.usageColor, config.usageStroke, config.usageWidth, config.usageShape ?: "roundedRect"))
        usageSwatch.setToolTipText("Click to edit style")
        JCheckBox usageCb = new JCheckBox("${config.name}", config.usageEnabled)
        usageCb.setFont(new Font("SansSerif", Font.PLAIN, 11))
        usageCb.addItemListener { e ->
            config.usageEnabled = (e.getStateChange() == ItemEvent.SELECTED)
            if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
        }
        usageSwatch.addMouseListener(new MouseAdapter() {
            @Override void mouseClicked(MouseEvent e) {
                showNodeStyleDialog(config, false, usageSwatch)
            }
        })
        usageRow.add(usageSwatch)
        usageRow.add(usageCb)
        allNodeRows.add(usageRow)
        allNodeRowNames.add(config.name + "-Usage")
        nodeSection.add(usageRow)

        // Definition row
        JPanel defRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1))
        defRow.setMaximumSize(new Dimension(9999, 26))
        defRow.setAlignmentX(Component.LEFT_ALIGNMENT)
        JLabel defSwatch = new JLabel(createColorIcon(config.defColor, config.defStroke, config.defWidth, config.defShape ?: "rectangle"))
        defSwatch.setToolTipText("Click to edit style")
        JCheckBox defCb = new JCheckBox("${config.name} Def", config.defEnabled)
        defCb.setFont(new Font("SansSerif", Font.PLAIN, 11))
        defCb.addItemListener { e ->
            config.defEnabled = (e.getStateChange() == ItemEvent.SELECTED)
            if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
        }
        defSwatch.addMouseListener(new MouseAdapter() {
            @Override void mouseClicked(MouseEvent e) {
                showNodeStyleDialog(config, true, defSwatch)
            }
        })
        defRow.add(defSwatch)
        defRow.add(defCb)
        allNodeRows.add(defRow)
        allNodeRowNames.add(config.name + "-Def")
        nodeSection.add(defRow)
    }

    controlPanel.add(nodeSection)
    controlPanel.add(Box.createVerticalStrut(12))

    // --- Edge Types Section ---
    JPanel edgeSection = new JPanel()
    edgeSection.setLayout(new BoxLayout(edgeSection, BoxLayout.Y_AXIS))
    edgeSection.setAlignmentX(Component.LEFT_ALIGNMENT)
    JLabel edgeSectionTitle = new JLabel(" \u25BC Edge Types")
    edgeSectionTitle.setFont(new Font("SansSerif", Font.BOLD, 13))
    edgeSectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT)
    edgeSection.add(edgeSectionTitle)
    edgeSection.add(Box.createVerticalStrut(4))

    def sortedEdges = new ArrayList(edgeTypeRegistry)
    sortedEdges.sort { a, b -> a.name.compareToIgnoreCase(b.name) }

    sortedEdges.each { config ->
        JPanel edgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1))
        edgeRow.setMaximumSize(new Dimension(9999, 26))
        edgeRow.setAlignmentX(Component.LEFT_ALIGNMENT)
        JLabel edgeSwatch = new JLabel(createLineIcon(config.color, config.width))
        edgeSwatch.setToolTipText("Click to edit edge style")
        JCheckBox edgeCb = new JCheckBox("${config.name}", config.enabled)
        edgeCb.setFont(new Font("SansSerif", Font.PLAIN, 11))
        edgeCb.addItemListener { e ->
            config.enabled = (e.getStateChange() == ItemEvent.SELECTED)
            if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
        }
        edgeSwatch.addMouseListener(new MouseAdapter() {
            @Override void mouseClicked(MouseEvent e) {
                showEdgeStyleDialog(config, edgeSwatch)
            }
        })
        edgeRow.add(edgeSwatch)
        edgeRow.add(edgeCb)
        allEdgeRows.add(edgeRow)
        allEdgeRowNames.add(config.name)
        edgeSection.add(edgeRow)
    }

    controlPanel.add(edgeSection)
    controlPanel.add(Box.createVerticalGlue())

    // Wrap in scroll pane for overflow
    JScrollPane controlScroll = new JScrollPane(controlPanel,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
    controlScroll.getVerticalScrollBar().setUnitIncrement(16)
    controlScroll.setPreferredSize(new Dimension(270, 600))
    controlScroll.setBorder(BorderFactory.createEmptyBorder())

    // =========================================================================
    // SECTION 9: Apply Theme to all UI components
    // =========================================================================
    def applyFullTheme = {
        def theme = THEMES[currentThemeName[0]]
        // Canvas
        applyCanvasTheme()
        // Toolbar
        toolbar.setBackground(theme.toolbarBg)
        def tbComps = toolbar.getComponents()
        int ti = 0
        while (ti < tbComps.length) {
            def c = tbComps[ti]
            if (c instanceof JLabel) { c.setForeground(theme.textColor); c.setFont(new Font("SansSerif", Font.PLAIN, 12)) }
            ti++
        }
        depthWarning.setForeground(new Color(255, 193, 7))
        // Status bar
        statusLabel.setOpaque(true)
        statusLabel.setBackground(theme.statusBg)
        statusLabel.setForeground(theme.textColor)
        // Control panel
        controlPanel.setBackground(theme.panelBg)
        controlScroll.setBackground(theme.panelBg)
        controlScroll.getViewport().setBackground(theme.panelBg)
        // Recursively style control panel children
        def styleChildren
        styleChildren = { Container parent ->
            def comps = parent.getComponents()
            int ci = 0
            while (ci < comps.length) {
                def c = comps[ci]
                if (c instanceof JPanel) {
                    c.setBackground(theme.panelBg)
                    styleChildren(c)
                }
                if (c instanceof JCheckBox) {
                    c.setBackground(theme.panelBg)
                    c.setForeground(theme.textColor)
                }
                if (c instanceof JLabel) {
                    c.setForeground(theme.textColor)
                }
                ci++
            }
        }
        styleChildren(controlPanel)
        nodeSectionTitle.setForeground(theme.accent)
        edgeSectionTitle.setForeground(theme.accent)
    }

    // =========================================================================
    // SECTION 10: Layout Application
    // =========================================================================
    def applyLayout = {
        String choice = layoutCombo.getSelectedItem()
        def layout
        def cfg = layoutConfigs[choice]
        if (choice == "Circle") {
            layout = new com.mxgraph.layout.mxCircleLayout(mxg)
            if (cfg != null) layout.setRadius((double)(cfg.radius))
        } else if (choice == "Compact Tree") {
            layout = new com.mxgraph.layout.mxCompactTreeLayout(mxg, false)
            if (cfg != null) {
                layout.setNodeDistance((int)cfg.nodeDist)
                layout.setLevelDistance((int)cfg.levelDist)
                layout.setGroupPadding((int)cfg.groupPadding)
            }
        } else if (choice == "Tree (Horiz)") {
            layout = new com.mxgraph.layout.mxCompactTreeLayout(mxg, true)
            if (cfg != null) {
                layout.setNodeDistance((int)cfg.nodeDist)
                layout.setLevelDistance((int)cfg.levelDist)
                layout.setGroupPadding((int)cfg.groupPadding)
            }
        } else if (choice == "Organic" || choice == "Organic (Fast)") {
            try {
                layout = new com.mxgraph.layout.mxFastOrganicLayout(mxg)
                if (cfg != null) {
                    layout.setForceConstant((double)(cfg.force))
                    layout.setMinDistanceLimit((double)(cfg.min))
                }
            } catch (Exception err) {
                try {
                    def organicClass = Class.forName("com.mxgraph.layout.mxOrganicLayout")
                    layout = organicClass.getConstructor(mxGraph.class).newInstance(mxg)
                } catch (Exception fallback) {
                    layout = new com.mxgraph.layout.hierarchical.mxHierarchicalLayout(mxg)
                }
            }
        } else if (choice == "Composite (Nested)") {
            def applyCompositeLayout
            applyCompositeLayout = { Object parentCell ->
                Object[] children = mxg.getChildVertices(parentCell)
                if (children != null && children.length > 0) {
                    children.each { child -> applyCompositeLayout(child) }
                    def innerLayout = new com.mxgraph.layout.mxCompactTreeLayout(mxg, false)
                    if (cfg != null) {
                        innerLayout.setNodeDistance((int)cfg.nodeDist)
                        innerLayout.setLevelDistance((int)cfg.levelDist)
                    }
                    innerLayout.execute(parentCell)
                    mxg.updateGroupBounds([parentCell] as Object[], (cfg != null ? (int)cfg.padding : 30), true)
                }
            }
            try { applyCompositeLayout(mxg.getDefaultParent()) } catch(Exception err) { logCrash("Composite layout failure", err) }
            
            layout = new com.mxgraph.layout.hierarchical.mxHierarchicalLayout(mxg, SwingConstants.WEST)
            if (cfg != null) {
                layout.setIntraCellSpacing((double)cfg.nodeDist)
                layout.setInterRankCellSpacing((double)cfg.levelDist)
            }
        } else if (choice == "Hierarchical (L-R)") {
            layout = new com.mxgraph.layout.hierarchical.mxHierarchicalLayout(mxg, SwingConstants.WEST)
            if (cfg != null) {
                layout.setIntraCellSpacing((double)cfg.intra)
                layout.setInterRankCellSpacing((double)cfg.inter)
            }
        } else {
            layout = new com.mxgraph.layout.hierarchical.mxHierarchicalLayout(mxg, SwingConstants.NORTH)
            if (cfg != null) {
                layout.setIntraCellSpacing((double)cfg.intra)
                layout.setInterRankCellSpacing((double)cfg.inter)
            }
        }
        try { layout.execute(mxg.getDefaultParent()) } catch(Exception err) { logCrash("Layout failure", err) }
    }

    // =========================================================================
    // SECTION 11: Relationship Scanner (proven PoC pattern)
    // =========================================================================
    def scanNamespaceForRelationships = { Element scanRoot ->
        Map<Element, java.util.List<java.util.List>> relationIndex = new HashMap<>()

        def addRelation = { Element a, Element b, String label ->
            if (!relationIndex.containsKey(a)) relationIndex.put(a, new ArrayList<>())
            relationIndex.get(a).add([b, label])
            if (!relationIndex.containsKey(b)) relationIndex.put(b, new ArrayList<>())
            relationIndex.get(b).add([a, label])
        }

        def scanForRelationships
        scanForRelationships = { Element el ->
            if (filterStandardLibs[0]) {
                try {
                    if (!el.isEditable()) return
                    String n = el.getName()
                    if (n == "SysML" || n == "KerML" || n == "UML Standard Profile" || n == "MD Customization for SysML" || n == "SysML 2.0 Library") {
                        return
                    }
                    String qn = el.respondsTo("getQualifiedName") ? el.getQualifiedName() : null
                    if (qn != null) {
                        String lqn = qn.toLowerCase()
                        if (lqn.startsWith("sysml::") || lqn.startsWith("kerml::") || lqn.contains("::sysml::") || lqn.contains("::kerml::")) {
                            return
                        }
                    }
                } catch (Exception ignore) {}
            }
            try {
                if (el instanceof SatisfyRequirementUsage && isEdgeEnabled("Satisfy")) {
                    SatisfyRequirementUsage satisfy = (SatisfyRequirementUsage) el
                    def satisfiedReq = satisfy.getSatisfiedRequirement()
                    Element satisfyingFeature = satisfy.getSatisfyingFeature()
                    if (satisfyingFeature == null) satisfyingFeature = satisfy.getOwner()
                    if (satisfiedReq != null && satisfyingFeature != null) {
                        addRelation(satisfyingFeature, satisfiedReq, "<<satisfy>>")
                    }
                }
                if (el instanceof Dependency && isEdgeEnabled("Dependency")) {
                    Dependency dep = (Dependency) el
                    def clients = dep.getClient()
                    def suppliers = dep.getSupplier()
                    if (clients != null && suppliers != null) {
                        clients.each { c ->
                            suppliers.each { s ->
                                addRelation(c, s, "<<dependency>>")
                            }
                        }
                    }
                }
                if (el instanceof AllocationUsage && isEdgeEnabled("Allocation")) {
                    AllocationUsage alloc = (AllocationUsage) el
                    def related = alloc.getRelatedElement()
                    if (related != null && related.size() >= 2) {
                        addRelation(related[0], related[1], "<<allocate>>")
                    }
                }
            } catch (Exception ex) {
                logCrash("Error scanning element ${getLabel(el)}", ex)
            }
            try {
                el.getOwnedElement().each { child ->
                    scanForRelationships(child)
                }
            } catch (Exception ignore) {}
        }

        scanForRelationships(scanRoot)
        return relationIndex
    }

    // =========================================================================
    // SECTION 12: Core Graph Builder
    // =========================================================================
    rebuildGraph = { Element rootElement, boolean clearExpanded = true ->
        long startTime = System.currentTimeMillis()
        int maxDepth = (int) depthSpinner.getValue()
        logDebug("--- rebuildGraph ---")
        logDebug("Root: ${rootElement != null ? getLabel(rootElement) : 'null'} (${rootElement?.getClass()?.getSimpleName()})")
        logDebug("Depth: ${maxDepth}, Theme: ${currentThemeName[0]}")

        if (clearExpanded && !(currentRoot[0]?.is(rootElement))) {
            expandedNodes.clear()
        }
        currentRoot[0] = rootElement
        usedLegendItems.node.clear()
        usedLegendItems.edge.clear()

        // Clear graph
        mxg.getModel().beginUpdate()
        try { mxg.removeCells(mxg.getChildCells(mxg.getDefaultParent(), true, true)) }
        finally { mxg.getModel().endUpdate() }

        if (rootElement == null) {
            statusLabel.setText("  No element selected.")
            graphComponent.refresh()
            return
        }

        // Accept any element type as root â€” even if it's not an "enabled" type,
        // it acts as the starting point for traversal
        def rootMatch = findNodeType(rootElement)

        // Find containing namespace for relationship scan
        Element scanRoot = rootElement
        Element walker = rootElement.getOwner()
        while (walker != null) {
            if (walker instanceof Namespace) { scanRoot = walker; break }
            walker = walker.getOwner()
        }
        logDebug("Scan root: ${getLabel(scanRoot)}")

        // Scan for relationships
        Map<Element, java.util.List<java.util.List>> relationIndex = scanNamespaceForRelationships(scanRoot)
        logDebug("Relation index: ${relationIndex.size()} elements")

        // BFS from root
        Map<Element, Object> cellMap = [:]
        elementToCellMapGlobal[0] = cellMap
        Set<Element> visited = new HashSet<>()
        Set<String> edgeKeys = new HashSet<>()  // Deduplication
        java.util.List<java.util.List> queue = [] // [ [element, remainingDepth] ]

        String labelPos = labelPosCombo.getSelectedItem()
        String currentRootLabel = getLabel(rootElement)

        mxg.getModel().beginUpdate()
        try {
            Object gParent = mxg.getDefaultParent()

            // Node Dimensions
            int nW = isDenseMode[0] ? 30 : 160
            int nH = isDenseMode[0] ? 30 : 50

            boolean skipRootVertex = false
            def initialQueue = []

            if (rootElement instanceof Dependency) {
                Dependency depRoot = (Dependency) rootElement
                def rClients = depRoot.getClient() ?: []
                def rSuppliers = depRoot.getSupplier() ?: []
                if (rClients.size() == 1 && rSuppliers.size() == 1) {
                    skipRootVertex = true
                    Element rc = rClients.iterator().next()
                    Element rs = rSuppliers.iterator().next()
                    visited.add(rootElement)
                    initialQueue.addAll([rc, rs])
                    
                    visited.add(rc)
                    Object rcCell = mxg.insertVertex(gParent, null, getLabel(rc), 0, 0, nW, nH, getNodeStyle(rc, true))
                    cellMap[rc] = rcCell
                    
                    visited.add(rs)
                    Object rsCell = mxg.insertVertex(gParent, null, getLabel(rs), 0, 0, nW, nH, getNodeStyle(rs, true))
                    cellMap[rs] = rsCell
                    
                    String eLbl = (labelPos == "Hidden") ? "" : "<<dependency>>"
                    mxg.insertEdge(gParent, null, eLbl, rcCell, rsCell, getEdgeStyle("Dependency"))
                }
            }

            if (!skipRootVertex) {
                // Insert root vertex (always shown even if its type is disabled)
                String rootStyle = (rootMatch != null) ? getNodeStyle(rootElement, true, labelPos) :
                    "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_HEXAGON};${mxConstants.STYLE_FILLCOLOR}=#566573;" +
                    "${mxConstants.STYLE_FONTCOLOR}=#FFFFFF;${mxConstants.STYLE_ROUNDED}=1;${mxConstants.STYLE_FONTSIZE}=12;" +
                    "${mxConstants.STYLE_SHADOW}=1;${mxConstants.STYLE_STROKECOLOR}=${THEMES[currentThemeName[0]].rootBorder};${mxConstants.STYLE_STROKEWIDTH}=3"

                if (labelPos == "Hidden" || isDenseMode[0]) rootStyle += ";${mxConstants.STYLE_NOLABEL}=1"
                else if (labelPos == "Above") rootStyle += ";${mxConstants.STYLE_VERTICAL_LABEL_POSITION}=top;${mxConstants.STYLE_LABEL_POSITION}=center"
                else if (labelPos == "Below") rootStyle += ";${mxConstants.STYLE_VERTICAL_LABEL_POSITION}=bottom;${mxConstants.STYLE_LABEL_POSITION}=center"

                if (rootElement instanceof Dependency) {
                    // Dep nodes drawn as round circles
                    rootStyle = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_ELLIPSE};${mxConstants.STYLE_FILLCOLOR}=#80DEEA;" +
                        "${mxConstants.STYLE_FONTCOLOR}=#000000;${mxConstants.STYLE_ROUNDED}=1;${mxConstants.STYLE_FONTSIZE}=12;" +
                        "${mxConstants.STYLE_SHADOW}=1;${mxConstants.STYLE_STROKECOLOR}=${THEMES[currentThemeName[0]].rootBorder};${mxConstants.STYLE_STROKEWIDTH}=3"
                }

                Object rootCell = mxg.insertVertex(gParent, rootElement.getID(), currentRootLabel, 0, 0, nW, nH, rootStyle)
                cellMap[rootElement] = rootCell
                visited.add(rootElement)
                initialQueue.add(rootElement)
            }

            int rootRemaining = maxDepth + expandedNodes.getOrDefault(rootElement, 0)
            initialQueue.each { el -> queue.add([el, rootRemaining]) }

            int vertexCount = cellMap.size()
            int edgeCount = skipRootVertex ? 1 : 0

            while (!queue.isEmpty()) {
                def entry = queue.remove(0)
                Element current = entry[0]
                int remaining = entry[1]

                // --- Relationship edges ---
                java.util.List<java.util.List> relations = relationIndex.get(current)
                if (relations != null) {
                    relations.each { tuple ->
                        Element other = tuple[0]
                        String edgeLbl = tuple[1]
                        if (other == null) return

                        // Determine edge type name for filtering
                        String edgeTypeName = "Dependency"
                        String lowerLbl = edgeLbl.toLowerCase()
                        if (lowerLbl.contains("satisfy")) edgeTypeName = "Satisfy"
                        else if (lowerLbl.contains("allocate")) edgeTypeName = "Allocation"
                        else if (lowerLbl.contains("featuremembership")) edgeTypeName = "FeatureMembership"
                        else if (lowerLbl.contains("feature typing")) edgeTypeName = "Feature Typing"
                        else if (lowerLbl.contains("owns")) edgeTypeName = "Owns"
                        else if (lowerLbl.contains("subsetting") && lowerLbl.contains("reference")) edgeTypeName = "Reference Subsetting"
                        else if (lowerLbl.contains("subsetting")) edgeTypeName = "Subsetting"
                        else if (lowerLbl.contains("references")) edgeTypeName = "References"
                        else if (lowerLbl.contains("import")) edgeTypeName = "Import"

                        if (!isEdgeEnabled(edgeTypeName)) return
                        if (!isEnabledNode(other) && findNodeType(other) != null) return

                        // Deduplicate edges
                        int id1 = System.identityHashCode(current)
                        int id2 = System.identityHashCode(other)
                        String edgeKey = (id1 < id2) ? "${id1}-${id2}-${edgeLbl}" : "${id2}-${id1}-${edgeLbl}"
                        if (edgeKeys.contains(edgeKey)) return
                        edgeKeys.add(edgeKey)

                        if (!visited.contains(other)) {
                            if (remaining <= 0) return
                            visited.add(other)
                            String label = getLabel(other)
                            String style = getNodeStyle(other, false, labelPos)
                            Object cell = mxg.insertVertex(gParent, other.getID(), label, 0, 0, nW, nH, style)
                            cellMap[other] = cell
                            vertexCount++
                            
                            int childRemaining = remaining - 1
                            int childExtra = expandedNodes.getOrDefault(other, 0)
                            queue.add([other, Math.max(childRemaining, childExtra)])
                        }

                        Object srcCell = cellMap[current]
                        Object tgtCell = cellMap[other]
                        if (srcCell != null && tgtCell != null) {
                            String displayLabel = (edgeLabelCombo.getSelectedItem() == "Hidden") ? "" : edgeLbl
                            String eStyle = getEdgeStyle(edgeTypeName)
                            mxg.insertEdge(gParent, "${current.getID()}-${other.getID()}", displayLabel, srcCell, tgtCell, eStyle)
                            edgeCount++
                            usedLegendItems.edge.add(edgeTypeName)
                        }
                    }
                }

                // --- Containment edges (owned children) ---
                if (isEdgeEnabled("Containment")) {
                    try {
                        if (!traversePackageChildren[0] && current instanceof Package && !current.is(currentRoot[0])) {
                            // Skip exploring the children of packages if traversal is disabled, unless it's the root we started from
                        } else {
                            current.getOwnedElement().each { Element child ->
                                if (isEnabledNode(child)) {
                                    if (!visited.contains(child)) {
                                    if (remaining <= 0) return
                                    visited.add(child)
                                    String label = getLabel(child)
                                    String style = getNodeStyle(child, false, labelPos)
                                    Object targetParent = (layoutCombo.getSelectedItem() == "Composite (Nested)") ? cellMap[current] : gParent
                                    Object cell = mxg.insertVertex(targetParent, child.getID(), label, 0, 0, nW, nH, style)
                                    cellMap[child] = cell
                                    vertexCount++
    
                                    int childRemaining = remaining - 1
                                    int childExtra = expandedNodes.getOrDefault(child, 0)
                                    queue.add([child, Math.max(childRemaining, childExtra)])
                                }
                                
                                Object srcCell = cellMap[current]
                                Object tgtCell = cellMap[child]
                                if (srcCell != null && tgtCell != null) {
                                    String containLabel = "owns"
                                    int id1 = System.identityHashCode(current)
                                    int id2 = System.identityHashCode(child)
                                    String edgeKey = "${id1}-${id2}-owns"
                                    if (layoutCombo.getSelectedItem() != "Composite (Nested)" && !edgeKeys.contains(edgeKey)) {
                                        edgeKeys.add(edgeKey)
                                        mxg.insertEdge(gParent, "contain-${current.getID()}-${child.getID()}", containLabel, srcCell, tgtCell, getEdgeStyle("Containment"))
                                        edgeCount++
                                        usedLegendItems.edge.add("Containment")
                                    }
                                }
                            }
                        }
                        } // end if (!traversePackage... else
                    } catch (Exception ex) {
                        logCrash("Error traversing owned elements", ex)
                    }
                }

                // --- Implied relationship traversals (owners, sources, suppliers) ---
                // --- Implied relationship traversals (owners, sources, suppliers) ---
                try {
                    def mappedEndpoints = [] // [element, isSource/Client (true), isTarget/Supplier (false), isOwner (null)]
                    
                    if (current instanceof Dependency) {
                        Dependency dep = (Dependency) current
                        def clients = dep.getClient()
                        def suppliers = dep.getSupplier()
                        if (clients != null) clients.each { mappedEndpoints.add([it, true]) }
                        if (suppliers != null) suppliers.each { mappedEndpoints.add([it, false]) }
                    } else if (current instanceof Relationship) {
                        Relationship rel = (Relationship) current
                        try { if (rel.hasProperty("source")) rel.getProperty("source").each { mappedEndpoints.add([it, true]) } } catch (Exception ignore) {}
                        try { if (rel.hasProperty("target")) rel.getProperty("target").each { mappedEndpoints.add([it, false]) } } catch (Exception ignore) {}
                        try { if (rel.getRelatedElement() != null) rel.getRelatedElement().each { mappedEndpoints.add([it, null]) } } catch (Exception ignore) {}
                    }
                    
                    // Always show owner if we specifically asked to map this node and it doesn't have an owner drawn yet
                    if (current.getOwner() != null && current.is(currentRoot[0])) {
                        mappedEndpoints.add([current.getOwner(), null])
                    }

                    mappedEndpoints.each { mappedEntry ->
                        Element child = mappedEntry[0]
                        Boolean isClient = mappedEntry[1]
                        
                        if (child != null && isEnabledNode(child)) {
                            Object childCell
                            if (!visited.contains(child)) {
                                if (remaining <= 0) return
                                visited.add(child)
                                String label = getLabel(child)
                                String style = getNodeStyle(child, false, labelPos)
                                childCell = mxg.insertVertex(gParent, null, label, 0, 0, nW, nH, style)
                                cellMap[child] = childCell
                                vertexCount++
                                
                                int childRemaining = remaining - 1
                                int childExtra = expandedNodes.getOrDefault(child, 0)
                                queue.add([child, Math.max(childRemaining, childExtra)])
                            } else {
                                childCell = cellMap[child]
                            }

                            Object depCell = cellMap[current]
                            if (childCell != null && depCell != null) {
                                String displayLabel = "<<dependency>>"
                                try {
                                    if (current.getClass().getSimpleName().contains("Import")) {
                                        def vis = current.hasProperty("visibility") ? current.getProperty("visibility")?.toString() : ""
                                        displayLabel = "<<import>>\n" + vis
                                    }
                                } catch (Exception ignore) {}
                                
                                Object srcEdgeNode = depCell
                                Object tgtEdgeNode = childCell
                                
                                if (isClient == true) {
                                    srcEdgeNode = childCell
                                    tgtEdgeNode = depCell
                                    displayLabel = ""
                                } else if (isClient == false) {
                                    srcEdgeNode = depCell
                                    tgtEdgeNode = childCell
                                    displayLabel = (edgeLabelCombo.getSelectedItem() == "Hidden") ? "" : "<<supplier>>"
                                }
                                
                                mxg.insertEdge(gParent, null, displayLabel, srcEdgeNode, tgtEdgeNode, getEdgeStyle("Dependency"))
                                edgeCount++
                                usedLegendItems.edge.add("Dependency")
                            }
                        }
                    }
                } catch (Exception ex) {
                    logCrash("Error traversing implied edges", ex)
                }

                // --- Feature-Level Implied Properties (Subsetting, Typing, etc) ---
                def propertyImpliedEdges = [
                    "Feature Typing": ["type", "getType", "<<Feature Typing>>"],
                    "Subsetting": ["subsettedFeature", "getSubsettedFeature", "<<Subsetting>>"],
                    "Reference Subsetting": ["referencedFeature", "getReference", "<<Reference Subsetting>>"],
                    "FeatureMembership": ["feature", "getFeature", "<<FeatureMembership>>"]
                ]
                
                propertyImpliedEdges.each { edgeType, queries ->
                    if (isEdgeEnabled(edgeType)) {
                        def targets = []
                        try { if (current.hasProperty(queries[0])) { def prop = current.getProperty(queries[0]); if (prop != null) { if (prop instanceof Collection) targets.addAll(prop) else targets.add(prop) } } } catch (Exception ignore) {}
                        try { if (current.respondsTo(queries[1])) { def prop = current.invokeMethod(queries[1], null); if (prop != null) { if (prop instanceof Collection) targets.addAll(prop) else targets.add(prop) } } } catch (Exception ignore) {}
                        
                        targets.each { child ->
                            if (child != null && child instanceof Element && isEnabledNode(child)) {
                                Object childCell
                                if (!visited.contains(child)) {
                                    if (remaining <= 0) return
                                    visited.add(child)
                                    String label = getLabel(child)
                                    String style = getNodeStyle(child, false, labelPos)
                                    childCell = mxg.insertVertex(gParent, null, label, 0, 0, nW, nH, style)
                                    cellMap[child] = childCell
                                    vertexCount++
                                    
                                    int childRemaining = remaining - 1
                                    int childExtra = expandedNodes.getOrDefault(child, 0)
                                    queue.add([child, Math.max(childRemaining, childExtra)])
                                } else {
                                    childCell = cellMap[child]
                                }

                                Object srcCell = cellMap[current]
                                if (childCell != null && srcCell != null) {
                                    String displayLabel = (edgeLabelCombo.getSelectedItem() == "Hidden") ? "" : queries[2]
                                    mxg.insertEdge(gParent, null, displayLabel, srcCell, childCell, getEdgeStyle(edgeType))
                                    edgeCount++
                                    usedLegendItems.edge.add(edgeType)
                                }
                            }
                        }
                    }
                }
            }

            // --- Robust Final Legend Scan ---
            // Re-scan nodes from cellMap (authoritative source of what's on screen)
            usedLegendItems.node.clear()
            cellMap.keySet().each { el ->
                def m = findNodeType(el)
                if (m != null) usedLegendItems.node.add(m[0].name + (m[1] ? "-Def" : "-Usage"))
            }
            // Edge tracking is NOT cleared here — it was populated correctly
            // during the traversal loops above (relationship, containment, implied, feature-level).
            
            // Sync Legend Checkboxes based on used config
            boolean filterL = filterLegendCheck.isSelected()
            for (int i = 0; i < allNodeRows.size(); i++) {
                allNodeRows[i].setVisible(!filterL || usedLegendItems.node.contains(allNodeRowNames[i]))
            }
            for (int i = 0; i < allEdgeRows.size(); i++) {
                allEdgeRows[i].setVisible(!filterL || usedLegendItems.edge.contains(allEdgeRowNames[i]))
            }
            nodeSection.revalidate()
            edgeSection.revalidate()

            long elapsed = System.currentTimeMillis() - startTime
            scanTimeMs[0] = (int) elapsed
            String perfWarning = (elapsed > 2000) ? "  \u26A0 SLOW (${elapsed}ms)" : ""
            logDebug("Graph: ${vertexCount} vertices, ${edgeCount} edges, ${elapsed}ms")
            statusLabel.setText("  \u2713 ${currentRootLabel} \u2014 ${vertexCount} nodes, ${edgeCount} edges  (depth=${maxDepth}, ${elapsed}ms)${perfWarning}")

        } finally {
            mxg.getModel().endUpdate()
        }

        try { applyLayout() } catch (Exception ex) { logCrash("Layout error", ex) }
        
        try {
            def rootCell = cellMap[currentRoot[0]]
            if (rootCell != null) {
                graphComponent.scrollCellToVisible(rootCell, true)
            }
        } catch (Exception ignore) {}
        
        graphComponent.refresh()
    }

    // =========================================================================
    // SECTION 13: Search Functionality
    // =========================================================================
    def doSearch = {
        String query = searchField.getText()
        if (query != null) { query = query.trim().toLowerCase() }
        if (query != null && !query.isEmpty()) {
            def cells = mxg.getChildCells(mxg.getDefaultParent(), true, false)
            int numCells = (cells != null) ? cells.length : 0
            boolean found = false
            int idx = 0
            while (idx < numCells && !found) {
                String val = mxg.getModel().getValue(cells[idx])
                if (val != null) { val = val.toString().toLowerCase() }
                if (val != null && val.contains(query)) {
                    found = true
                    mxg.setSelectionCell(cells[idx])
                    graphComponent.scrollCellToVisible(cells[idx], true)
                    def theme = THEMES[currentThemeName[0]]
                    mxg.getModel().beginUpdate()
                    try {
                        String hlColor = "#" + Integer.toHexString(theme.searchHighlight.getRGB() & 0xFFFFFF).padLeft(6, '0')
                        mxg.setCellStyles(mxConstants.STYLE_STROKECOLOR, hlColor, [cells[idx]] as Object[])
                        mxg.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "4", [cells[idx]] as Object[])
                    } finally { mxg.getModel().endUpdate() }
                    statusLabel.setText("  Found: " + val)
                }
                idx++
            }
            if (!found) { statusLabel.setText("  No match for '" + query + "'") }
        }
    }

    searchField.addActionListener { e -> doSearch() }
    searchButton.addActionListener { e -> doSearch() }

    // =========================================================================
    // SECTION 14: Control Listeners
    // =========================================================================
    // Depth warning
    def updateDepthWarning = {
        int d = (int) depthSpinner.getValue()
        depthWarning.setText(d > 5 ? " [!]" : "")
        depthWarning.setToolTipText(d > 5 ? "Depth > 5 may be very slow on large models" : "")
    }
    updateDepthWarning()

    depthSpinner.addChangeListener { evt ->
        updateDepthWarning()
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }
    layoutCombo.addActionListener { e -> if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false) }
    labelPosCombo.addActionListener { e -> if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false) }
    edgeLabelCombo.addActionListener { e -> if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false) }

    themeCombo.addActionListener { e ->
        currentThemeName[0] = themeCombo.getSelectedItem()
        applyFullTheme()
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }

    denseModeCheck.addItemListener { e ->
        isDenseMode[0] = (e.getStateChange() == ItemEvent.SELECTED)
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }

    filterLibsCheck.addItemListener { e ->
        filterStandardLibs[0] = (e.getStateChange() == ItemEvent.SELECTED)
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }

    traversePkgCheck.addItemListener { e ->
        traversePackageChildren[0] = (e.getStateChange() == ItemEvent.SELECTED)
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }

    filterLegendCheck.addItemListener { e ->
        filterLegend[0] = (e.getStateChange() == ItemEvent.SELECTED)
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }

    exportButton.addActionListener { e ->
        try {
            JFileChooser fc = new JFileChooser()
            if (lastExportDir[0] != null) fc.setCurrentDirectory(lastExportDir[0])
            fc.setDialogTitle("Export Graph (PNG / SVG)")
            fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG Image (*.png)", "png"))
            fc.addChoosableFileFilter(new FileNameExtensionFilter("SVG Vector Graphics (*.svg)", "svg"))
            fc.setAcceptAllFileFilterUsed(false)
            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fc.getSelectedFile()
                lastExportDir[0] = fc.getCurrentDirectory()
                String fp = fileToSave.getAbsolutePath().toLowerCase()
                
                // Read selected filter to force extension if missing
                if (fc.getFileFilter() != null) {
                    if (fc.getFileFilter().getDescription().contains("SVG") && !fp.endsWith(".svg")) {
                        fp += ".svg"
                        fileToSave = new File(fileToSave.getAbsolutePath() + ".svg")
                    } else if (fc.getFileFilter().getDescription().contains("PNG") && !fp.endsWith(".png")) {
                        fp += ".png"
                        fileToSave = new File(fileToSave.getAbsolutePath() + ".png")
                    }
                }

                // Check dimensions for legend scaling
                controlPanel.doLayout()
                Dimension prefSize = controlPanel.getPreferredSize()
                int legendW = prefSize.width
                int legendH = prefSize.height

                if (fp.endsWith(".svg")) {
                    def svgDoc = mxCellRenderer.createSvgDocument(mxg, null, 1.0, java.awt.Color.WHITE, null)
                    String xml = mxXmlUtils.getXml(svgDoc.getDocumentElement())
                    
                    // Fix JGraphX "Namespace Pollution"
                    xml = xml.replace('xmlns=""', ' ')
                    
                    // Extract original width/height/viewbox
                    def wMatch = (xml =~ /width="(\d+)"/)
                    def hMatch = (xml =~ /height="(\d+)"/)
                    int graphW = wMatch.find() ? wMatch.group(1).toInteger() : 800
                    int graphH = hMatch.find() ? hMatch.group(1).toInteger() : 600
                    
                    // Calculate export dimensions using fixed SVG legend width
                    int svgLegW = 220  // 180 content + 40 margin
                    int totalW = graphW + svgLegW
                    int totalH = graphH
                    
                    xml = xml.replaceFirst(/width="\d+"/, "width=\"${totalW}\"")
                    xml = xml.replaceFirst(/height="\d+"/, "height=\"${totalH}\"")
                    xml = xml.replaceFirst(/viewBox="0 0 \d+ \d+"/, "viewBox=\"0 0 ${totalW} ${totalH}\"")
                    
                    // Manually build SVG legend group — content first, then wrap with correctly-sized background
                    StringBuilder legContent = new StringBuilder()
                    int yOff = 30
                    
                    // Section: Node Types
                    boolean hasNodes = nodeTypeRegistry.any { nt -> usedLegendItems.node.contains(nt.name + "-Usage") || usedLegendItems.node.contains(nt.name + "-Def") }
                    if (hasNodes) {
                        legContent.append("<text fill=\"#2C3E50\" font-family=\"Arial,Helvetica\" font-size=\"12\" font-weight=\"bold\" x=\"10\" y=\"${yOff}\">\u25BC Node Types</text>")
                        yOff += 18
                        // SVG shape icon helper for legend
                        def svgShapeIcon = { String shapeId, String fill, String stroke, int cx, int cy ->
                            String sid = shapeId ?: "roundedRect"
                            if (sid == "ellipse" || sid == "circle") {
                                return "<ellipse fill=\"${fill}\" stroke=\"${stroke}\" cx=\"${cx}\" cy=\"${cy}\" rx=\"7\" ry=\"7\" />"
                            } else if (sid == "doubleEllipse") {
                                return "<ellipse fill=\"${fill}\" stroke=\"${stroke}\" cx=\"${cx}\" cy=\"${cy}\" rx=\"7\" ry=\"7\" /><ellipse fill=\"none\" stroke=\"${stroke}\" cx=\"${cx}\" cy=\"${cy}\" rx=\"5\" ry=\"5\" />"
                            } else if (sid == "diamond") {
                                return "<polygon fill=\"${fill}\" stroke=\"${stroke}\" points=\"${cx},${cy-7} ${cx+7},${cy} ${cx},${cy+7} ${cx-7},${cy}\" />"
                            } else if (sid == "hexagon") {
                                return "<polygon fill=\"${fill}\" stroke=\"${stroke}\" points=\"${cx-4},${cy-7} ${cx+4},${cy-7} ${cx+7},${cy} ${cx+4},${cy+7} ${cx-4},${cy+7} ${cx-7},${cy}\" />"
                            } else if (sid == "triangle") {
                                return "<polygon fill=\"${fill}\" stroke=\"${stroke}\" points=\"${cx},${cy-7} ${cx+7},${cy+7} ${cx-7},${cy+7}\" />"
                            } else if (sid == "cylinder") {
                                return "<rect fill=\"${fill}\" stroke=\"${stroke}\" x=\"${cx-7}\" y=\"${cy-4}\" width=\"14\" height=\"10\" /><ellipse fill=\"${fill}\" stroke=\"${stroke}\" cx=\"${cx}\" cy=\"${cy-4}\" rx=\"7\" ry=\"3\" />"
                            } else if (sid == "rectangle") {
                                return "<rect fill=\"${fill}\" stroke=\"${stroke}\" x=\"${cx-7}\" y=\"${cy-7}\" width=\"14\" height=\"14\" />"
                            } else if (sid == "folder") {
                                return "<rect fill=\"${fill}\" stroke=\"${stroke}\" x=\"${cx-7}\" y=\"${cy-4}\" width=\"14\" height=\"11\" /><rect fill=\"${fill}\" stroke=\"${stroke}\" x=\"${cx-7}\" y=\"${cy-7}\" width=\"7\" height=\"4\" />"
                            } else if (sid == "actor") {
                                return "<circle fill=\"${fill}\" stroke=\"${stroke}\" cx=\"${cx}\" cy=\"${cy-4}\" r=\"3\" /><line x1=\"${cx}\" y1=\"${cy-1}\" x2=\"${cx}\" y2=\"${cy+4}\" stroke=\"${stroke}\" /><line x1=\"${cx-5}\" y1=\"${cy}\" x2=\"${cx+5}\" y2=\"${cy}\" stroke=\"${stroke}\" />"
                            } else {
                                return "<rect fill=\"${fill}\" stroke=\"${stroke}\" x=\"${cx-7}\" y=\"${cy-7}\" width=\"14\" height=\"14\" rx=\"4\" ry=\"4\" />"
                            }
                        }
                        nodeTypeRegistry.each { nt ->
                            if (usedLegendItems.node.contains(nt.name + "-Usage")) {
                                legContent.append(svgShapeIcon(nt.usageShape ?: "roundedRect", nt.usageColor, nt.usageStroke, 17, yOff + 7))
                                legContent.append("<text fill=\"#34495E\" font-family=\"Arial,Helvetica\" font-size=\"11\" x=\"30\" y=\"${yOff + 11}\">${nt.name}</text>")
                                yOff += 20
                            }
                            if (usedLegendItems.node.contains(nt.name + "-Def")) {
                                legContent.append(svgShapeIcon(nt.defShape ?: "rectangle", nt.defColor, nt.defStroke, 17, yOff + 7))
                                legContent.append("<text fill=\"#34495E\" font-family=\"Arial,Helvetica\" font-size=\"11\" x=\"30\" y=\"${yOff + 11}\">${nt.name} Def</text>")
                                yOff += 20
                            }
                        }
                    }
                    
                    // Section: Edge Types
                    boolean hasEdges = edgeTypeRegistry.any { et -> usedLegendItems.edge.contains(et.name) }
                    if (hasEdges) {
                        yOff += 6
                        legContent.append("<text fill=\"#2C3E50\" font-family=\"Arial,Helvetica\" font-size=\"12\" font-weight=\"bold\" x=\"10\" y=\"${yOff}\">▼ Edge Types</text>")
                        yOff += 18
                        edgeTypeRegistry.each { et ->
                            if (usedLegendItems.edge.contains(et.name)) {
                                String dash = (et.dashed) ? "stroke-dasharray=\"3,3\"" : ""
                                int ew = et.width ?: 2
                                legContent.append("<line x1=\"10\" y1=\"${yOff + 7}\" x2=\"24\" y2=\"${yOff + 7}\" stroke=\"${et.color}\" stroke-width=\"${ew}\" ${dash} />")
                                legContent.append("<text fill=\"#34495E\" font-family=\"Arial,Helvetica\" font-size=\"11\" x=\"30\" y=\"${yOff + 11}\">${et.name}</text>")
                                yOff += 20
                            }
                        }
                    }

                    int actualLegendH = yOff + 10
                    int svgLegendW = 180
                    
                    StringBuilder leg = new StringBuilder()
                    leg.append("<g id=\"legend_root\" transform=\"translate(${graphW + 20}, 20)\">")
                    leg.append("<rect fill=\"#F8F9F9\" stroke=\"#D5D8DC\" x=\"0\" y=\"0\" width=\"${svgLegendW}\" height=\"${actualLegendH}\" rx=\"5\" ry=\"5\" />")
                    leg.append("<text fill=\"#2C3E50\" font-family=\"Arial,Helvetica\" font-size=\"14\" font-weight=\"bold\" x=\"10\" y=\"18\">Legend</text>")
                    leg.append(legContent.toString())
                    leg.append("</g>")
                    
                    // Insert legend before closing tag
                    xml = xml.replace("</svg>", leg.toString() + "</svg>")
                    
                    // Strip partial headers and inject final valid ones
                    xml = xml.replaceFirst("(?s)<\\?xml.*?\\?>", "").trim()
                    xml = xml.replaceFirst("(?s)<!DOCTYPE.*?>", "").trim()
                    if (!xml.contains("xmlns=\"http://www.w3.org/2000/svg\"")) {
                        xml = xml.replaceFirst("<svg", "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.1\"")
                    } else if (!xml.contains("xmlns:xlink")) {
                        xml = xml.replaceFirst("<svg", "<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.1\"")
                    }
                    
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n" + xml
                    fileToSave.text = xml
                    statusLabel.setText("  \u2713 Exported vector to ${fileToSave.getName()}")
                    logDebug("Exported SVG: ${fileToSave.getAbsolutePath()}")
                } else {
                    if (!fp.endsWith(".png")) fileToSave = new File(fileToSave.getAbsolutePath() + ".png")
                    def image = mxCellRenderer.createBufferedImage(mxg, null, 1.0, java.awt.Color.WHITE, true, null)
                    
                    if (image != null) {
                         try {
                            String savedTheme = currentThemeName[0]
                            // Switch to Light for legend rendering to ensure export quality
                            currentThemeName[0] = "Light"
                            applyFullTheme()
                            
                            // Automatically filter legend
                            def oldVisNodes = allNodeRows.collect { it.isVisible() }
                            def oldVisEdges = allEdgeRows.collect { it.isVisible() }
                            
                            for (int i=0; i<allNodeRows.size(); i++) allNodeRows[i].setVisible(usedLegendItems['node'].contains(allNodeRowNames[i]))
                            for (int i=0; i<allEdgeRows.size(); i++) allEdgeRows[i].setVisible(usedLegendItems['edge'].contains(allEdgeRowNames[i]))
                            
                            // Force full recursive layout validation — critical for off-screen painting
                            controlPanel.invalidate()
                            controlPanel.validate()
                            controlPanel.doLayout()
                            prefSize = controlPanel.getPreferredSize()
                            controlPanel.setSize(prefSize)
                            controlPanel.invalidate()
                            controlPanel.validate()
                            controlPanel.doLayout()
                            
                            // Recursively size and validate every child for off-screen rendering
                            java.util.List<Component> stack = [controlPanel]
                            while (!stack.isEmpty()) {
                                Component comp = stack.remove(0)
                                if (comp instanceof Container) {
                                    Container cont = (Container) comp
                                    cont.doLayout()
                                    for (Component ch : cont.getComponents()) stack.add(ch)
                                }
                            }
                            
                            legendW = prefSize.width
                            legendH = prefSize.height
                            
                            // Create off-screen buffer for the legend panel
                            def legendImage = new java.awt.image.BufferedImage(
                                (int)Math.max(legendW, 1),
                                (int)Math.max(legendH, 1),
                                java.awt.image.BufferedImage.TYPE_INT_ARGB
                            )
                            def lg = legendImage.createGraphics()
                            lg.setColor(java.awt.Color.WHITE)
                            lg.fillRect(0, 0, legendW, legendH)
                            controlPanel.paint(lg)
                            lg.dispose()
                            
                            // Composite diagram + legend
                            def combined = new java.awt.image.BufferedImage(
                                (int)(image.getWidth() + legendW + 20), 
                                (int)Math.max(image.getHeight(), legendH), 
                                java.awt.image.BufferedImage.TYPE_INT_ARGB
                            )
                            def g2 = combined.createGraphics()
                            g2.setColor(java.awt.Color.WHITE)
                            g2.fillRect(0, 0, combined.getWidth(), combined.getHeight())
                            g2.drawImage(image, 0, 0, null)
                            g2.drawImage(legendImage, image.getWidth() + 10, 0, null)
                            g2.dispose()
                            
                            image = combined
                            
                            // Restore original UI theme
                            currentThemeName[0] = savedTheme
                            applyFullTheme()
                            
                            // Restore UI
                            for (int i=0; i<allNodeRows.size(); i++) allNodeRows[i].setVisible(oldVisNodes[i])
                            for (int i=0; i<allEdgeRows.size(); i++) allEdgeRows[i].setVisible(oldVisEdges[i])
                            controlPanel.setSize(controlPanel.getParent().getSize())
                            controlPanel.doLayout()
                        } catch (Exception ignore) {}
                        
                        javax.imageio.ImageIO.write(image, "PNG", fileToSave)
                        statusLabel.setText("  \u2713 Exported image + legend to ${fileToSave.getName()}")
                        logDebug("Exported PNG: ${fileToSave.getAbsolutePath()}")
                    }
                }
            }
        } catch (Exception ex) {
            logCrash("Export graph error", ex)
        }
    }

    // =========================================================================
    // SECTION 15: JSON Save / Load
    // =========================================================================
    File configFile = new File(scriptDir, "V2Map/V2Map_Config.json")

    def buildConfigMap = {
        return [
            rootId: currentRoot[0] != null ? currentRoot[0].getID() : null,
            theme: currentThemeName[0],
            depth: (int) depthSpinner.getValue(),
            layout: layoutCombo.getSelectedItem(),
            labelPos: labelPosCombo.getSelectedItem(),
            edgeLabel: edgeLabelCombo.getSelectedItem(),
            lastExportDir: lastExportDir[0] != null ? lastExportDir[0].getAbsolutePath() : null,
            layoutConfigs: layoutConfigs,
            nodeTypes: nodeTypeRegistry.collect { c ->
                [name: c.name, usageEnabled: c.usageEnabled, defEnabled: c.defEnabled,
                 usageColor: c.usageColor, defColor: c.defColor,
                 usageStroke: c.usageStroke, defStroke: c.defStroke,
                 usageWidth: c.usageWidth, defWidth: c.defWidth,
                 usageText: c.usageText, defText: c.defText,
                 usageShape: c.usageShape, defShape: c.defShape,
                 denseUsageShape: c.denseUsageShape, denseDefShape: c.denseDefShape,
                 usageFontStyle: c.usageFontStyle, defFontStyle: c.defFontStyle,
                 usageFontFamily: c.usageFontFamily, defFontFamily: c.defFontFamily]
            },
            edgeTypes: edgeTypeRegistry.collect { c ->
                [name: c.name, enabled: c.enabled, color: c.color,
                 width: c.width, dashed: c.dashed, textColor: c.textColor,
                 startArrow: c.startArrow, endArrow: c.endArrow,
                 fontStyle: c.fontStyle, fontSize: c.fontSize,
                 fontFamily: c.fontFamily]
            }
        ]
    }

    def applyConfigMap = { Map cfg ->
        try {
            if (cfg.theme) { currentThemeName[0] = cfg.theme; themeCombo.setSelectedItem(cfg.theme) }
            if (cfg.depth) depthSpinner.setValue(cfg.depth)
            if (cfg.layout) layoutCombo.setSelectedItem(cfg.layout)
            if (cfg.labelPos) labelPosCombo.setSelectedItem(cfg.labelPos)
            if (cfg.edgeLabel) edgeLabelCombo.setSelectedItem(cfg.edgeLabel)
            if (cfg.lastExportDir) {
                File dir = new File(cfg.lastExportDir)
                if (dir.isDirectory()) lastExportDir[0] = dir
            }
            if (cfg.layoutConfigs != null) {
                cfg.layoutConfigs.each { k, v ->
                    if (layoutConfigs.containsKey(k)) {
                        layoutConfigs[k].putAll(v)
                    }
                }
            }
            if (cfg.nodeTypes != null) {
                cfg.nodeTypes.each { saved ->
                    def match = nodeTypeRegistry.find { it.name == saved.name }
                    if (match) {
                        match.usageEnabled = saved.usageEnabled
                        match.defEnabled = saved.defEnabled
                        if (saved.usageColor) match.usageColor = saved.usageColor
                        if (saved.defColor) match.defColor = saved.defColor
                        if (saved.usageStroke) match.usageStroke = saved.usageStroke
                        if (saved.defStroke) match.defStroke = saved.defStroke
                        if (saved.usageWidth != null) match.usageWidth = saved.usageWidth
                        if (saved.defWidth != null) match.defWidth = saved.defWidth
                        if (saved.usageText) match.usageText = saved.usageText
                        if (saved.defText) match.defText = saved.defText
                        if (saved.usageShape) match.usageShape = saved.usageShape
                        if (saved.defShape) match.defShape = saved.defShape
                        if (saved.denseUsageShape) match.denseUsageShape = saved.denseUsageShape
                        if (saved.denseDefShape) match.denseDefShape = saved.denseDefShape
                        if (saved.usageFontStyle) match.usageFontStyle = saved.usageFontStyle
                        if (saved.defFontStyle) match.defFontStyle = saved.defFontStyle
                        if (saved.usageFontFamily) match.usageFontFamily = saved.usageFontFamily
                        if (saved.defFontFamily) match.defFontFamily = saved.defFontFamily
                    }
                }
            }
            if (cfg.edgeTypes != null) {
                cfg.edgeTypes.each { saved ->
                    def match = edgeTypeRegistry.find { it.name == saved.name }
                    if (match) {
                        match.enabled = saved.enabled
                        if (saved.color) match.color = saved.color
                        if (saved.width != null) match.width = saved.width
                        if (saved.dashed != null) match.dashed = saved.dashed
                        if (saved.textColor) match.textColor = saved.textColor
                        if (saved.startArrow) match.startArrow = saved.startArrow
                        if (saved.endArrow) match.endArrow = saved.endArrow
                        if (saved.fontStyle) match.fontStyle = saved.fontStyle
                        if (saved.fontSize != null) match.fontSize = saved.fontSize
                        if (saved.fontFamily) match.fontFamily = saved.fontFamily
                    }
                }
            }
            applyFullTheme()
        } catch (Exception ex) {
            logCrash("Error applying config", ex)
        }
    }

    def mapToJson; mapToJson = { obj ->
        if (obj == null) return "null"
        if (obj instanceof String) return '"' + obj.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n') + '"'
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString()
        if (obj instanceof java.util.List || obj instanceof java.util.Set || obj.getClass().isArray()) {
            return '[' + obj.collect{ mapToJson(it) }.join(',') + ']'
        }
        if (obj instanceof java.util.Map) {
            return '{' + obj.collect{ '"'+it.key+'":' + mapToJson(it.value) }.join(',') + '}'
        }
        return '"' + obj.toString().replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n') + '"'
    }

    saveButton.addActionListener { e ->
        try {
            // Load existing configs or start fresh
            java.util.List<Map> configs = []
            if (configFile.exists()) {
                try { configs = new JsonSlurper().parse(configFile) as java.util.List<Map> } catch (Exception ignore) {}
            }
            String name = JOptionPane.showInputDialog(null, "Configuration name:", "Save Configuration", JOptionPane.PLAIN_MESSAGE)
            if (name == null || name.trim().isEmpty()) return

            Map newCfg = buildConfigMap()
            newCfg.configName = name.trim()
            newCfg.savedAt = new Date().toString()

            // Check for overwrite
            int idx = configs.findIndexOf { it.configName == name.trim() }
            if (idx >= 0) {
                int ow = JOptionPane.showConfirmDialog(null, "Overwrite '${name}'?", "Confirm", JOptionPane.YES_NO_OPTION)
                if (ow == JOptionPane.YES_OPTION) configs[idx] = newCfg
                else return
            } else {
                configs.add(newCfg)
            }

            configFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(configs))
            statusLabel.setText("  \u2713 Configuration '${name}' saved.")
            logDebug("Config saved: ${name}")
        } catch (Exception ex) {
            logCrash("Error saving config", ex)
            JOptionPane.showMessageDialog(null, "Error saving: ${ex.getMessage()}", "Save Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    loadButton.addActionListener { e ->
        try {
            if (!configFile.exists()) {
                JOptionPane.showMessageDialog(null, "No saved configurations found.", "Load", JOptionPane.INFORMATION_MESSAGE)
                return
            }
            java.util.List<Map> configs = new JsonSlurper().parse(configFile) as java.util.List<Map>
            if (configs.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No saved configurations.", "Load", JOptionPane.INFORMATION_MESSAGE)
                return
            }

            // Filter out internal autosave from user-visible list
            java.util.List<Map> userConfigs = configs.findAll { it.configName != "__autosave__" }
            if (userConfigs.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No saved configurations.", "Load", JOptionPane.INFORMATION_MESSAGE)
                return
            }

            String[] names = userConfigs.collect { it.configName ?: "Unnamed" } as String[]
            String chosen = (String) JOptionPane.showInputDialog(null, "Select configuration:", "Load Configuration",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0])
            if (chosen == null) return

            Map cfg = userConfigs.find { it.configName == chosen }
            if (cfg) {
                applyConfigMap(cfg)
                statusLabel.setText("  \u2713 Configuration '${chosen}' loaded.")
                logDebug("Config loaded: ${chosen}")
                if (cfg.rootId != null) {
                    try {
                        def rootEl = com.nomagic.magicdraw.core.Application.getInstance().getProject().getElementByID((String)cfg.rootId)
                        if (rootEl != null) {
                            def tree = com.nomagic.magicdraw.core.Application.getInstance().getMainFrame().getBrowser().getContainmentTree()
                            tree.clearSelection()
                            def browserNode = tree.findNode(rootEl)
                            if (browserNode != null) tree.setSelectedNode(browserNode)
                            currentRoot[0] = rootEl
                        }
                    } catch (Exception ignore) {}
                }
                if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
            }
        } catch (Exception ex) {
            logCrash("Error loading config", ex)
            JOptionPane.showMessageDialog(null, "Error loading: ${ex.getMessage()}", "Load Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    // =========================================================================
    // SECTION 16: Auto-load last config on startup
    // =========================================================================
    try {
        if (configFile.exists()) {
            java.util.List<Map> configs = new JsonSlurper().parse(configFile) as java.util.List<Map>
            if (!configs.isEmpty()) {
                // Prefer __autosave__ entry, fall back to last
                Map lastCfg = configs.find { it.configName == "__autosave__" }
                if (lastCfg == null) lastCfg = configs.last()
                applyConfigMap(lastCfg)
                logDebug("Auto-loaded config: ${lastCfg.configName}")
            }
        }
    } catch (Exception ex) {
        logDebug("No config to auto-load: ${ex.getMessage()}")
    }

    // =========================================================================
    // SECTION 17: Frame Assembly
    // =========================================================================
    JFrame frame = new JFrame("V2Map — SysMLv2 Model Map Viewer")
    frame.setLayout(new BorderLayout())
    frame.add(toolbar, BorderLayout.NORTH)
    frame.add(controlScroll, BorderLayout.WEST)
    frame.add(graphComponent, BorderLayout.CENTER)
    frame.add(statusLabel, BorderLayout.SOUTH)
    frame.setSize(1400, 900)
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)

    // Apply initial theme
    applyFullTheme()

    // Interactive Expansion (Double Click)
    graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
        @Override
        void mouseReleased(MouseEvent e) {
            if (e.getClickCount() == 2 && currentRoot[0] != null) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY())
                if (cell != null) {
                    Map<Element, Object> cmap = elementToCellMapGlobal[0]
                    if (cmap != null) {
                        Element clickedEl = null
                        cmap.each { k, v -> if (v.equals(cell)) clickedEl = k }
                        if (clickedEl != null) {
                            expandedNodes.put(clickedEl, expandedNodes.getOrDefault(clickedEl, 0) + 1)
                            logDebug("Expanded node ${getLabel(clickedEl)}")
                            rebuildGraph(currentRoot[0], false)
                        }
                    }
                }
            }
        }
    })

    // =========================================================================
    // SECTION 18: Containment Tree Listener
    // =========================================================================
    def containmentTree = Application.getInstance().getMainFrame().getBrowser().getContainmentTree()
    def treeComponent = containmentTree.getTree()

    TreeSelectionListener selectionListener = new TreeSelectionListener() {
        @Override
        void valueChanged(TreeSelectionEvent e) {
            try {
                def path = e.getNewLeadSelectionPath()
                if (path != null) {
                    def lastComp = path.getLastPathComponent()
                    if (lastComp instanceof Node) {
                        def userObj = ((Node) lastComp).getUserObject()
                        if (userObj instanceof Element) {
                            rebuildGraph((Element) userObj)
                        }
                    }
                }
            } catch (Exception ex) {
                logCrash("Tree selection error", ex)
            }
        }
    }
    treeComponent.addTreeSelectionListener(selectionListener)

    frame.addWindowListener(new WindowAdapter() {
        @Override
        void windowClosed(WindowEvent e) {
            logDebug("Window closed \u2014 removing listener, auto-saving config.")
            treeComponent.removeTreeSelectionListener(selectionListener)
            // Auto-save current state
            try {
                java.util.List<Map> configs = []
                if (configFile.exists()) {
                    try { configs = new JsonSlurper().parse(configFile) as java.util.List<Map> } catch (Exception ignore) {}
                }
                Map autoCfg = buildConfigMap()
                autoCfg.configName = "__autosave__"
                autoCfg.savedAt = new Date().toString()
                int idx = configs.findIndexOf { it.configName == "__autosave__" }
                if (idx >= 0) configs[idx] = autoCfg
                else configs.add(autoCfg)
                configFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(configs))
            } catch (Exception ignore) {}
        }
    })

    // =========================================================================
    // SECTION 19: Seed Initial Selection
    // =========================================================================
    boolean seeded = false
    def selectedNodes = containmentTree.getSelectedNodes()
    if (selectedNodes != null && selectedNodes.length > 0) {
        def userObj = selectedNodes[0].getUserObject()
        if (userObj instanceof Element) {
            rebuildGraph((Element) userObj)
            seeded = true
        }
    }
    if (!seeded) {
        statusLabel.setText("  âš  Select an element in the Containment Tree to begin.")
        logDebug("No element selected at startup.")
    }

    frame.setLocationRelativeTo(Application.getInstance().getMainFrame())
    frame.setVisible(true)
    logDebug("V2Map window opened.")

} catch (Throwable t) {
    logCrash("Critical failure launching V2Map", t)
}

