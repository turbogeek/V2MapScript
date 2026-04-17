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
import java.awt.*
import java.util.List
import java.util.Map
import java.util.Set
import java.awt.event.*

import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.layout.mxCompactTreeLayout


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
Map<Element, Integer> expandedNodes = [:]
def elementToCellMapGlobal = [null]

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
    String shape = isDef ? mxConstants.SHAPE_RECTANGLE : mxConstants.SHAPE_ELLIPSE
    if (config.name == "Package") shape = mxConstants.SHAPE_SWIMLANE
    int arcSize = isDef ? 12 : 0
    int fontStyle = isDef ? 1 : 0
    def theme = THEMES[currentThemeName[0]]
    String strokeColor = isRoot ? theme.rootBorder : fillColor
    float strokeWidth = isRoot ? 3.0f : 1.2f

    String style = "${mxConstants.STYLE_SHAPE}=${shape};" +
           (config.name == "Package" ? "${mxConstants.STYLE_STARTSIZE}=22;${mxConstants.STYLE_SWIMLANE_FILLCOLOR}=#FFF5D1;" : "") +
           "${mxConstants.STYLE_FILLCOLOR}=${fillColor};" +
           "${mxConstants.STYLE_FONTCOLOR}=#FFFFFF;" +
           "${mxConstants.STYLE_ROUNDED}=1;" +
           "${mxConstants.STYLE_ARCSIZE}=${arcSize};" +
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
    boolean dashed = edgeConfig?.dashed ?: false
    String startArrow = edgeConfig?.startArrow ?: "none"
    String endArrow = edgeConfig?.endArrow ?: mxConstants.ARROW_OPEN
    def theme = THEMES[currentThemeName[0]]
    return "${mxConstants.STYLE_STROKECOLOR}=${color};" +
           "${mxConstants.STYLE_FONTCOLOR}=${theme.edgeLabelFg};" +
           "${mxConstants.STYLE_FONTSIZE}=10;" +
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
    JComboBox<String> layoutCombo = new JComboBox<>(["Hierarchical", "Circle", "Compact Tree", "Organic"] as String[])
    layoutCombo.setPreferredSize(new Dimension(120, 26))

    // --- Theme ---
    JLabel themeLabel = new JLabel("Theme:")
    JComboBox<String> themeCombo = new JComboBox<>(["Dark", "Light", "Hello Kitty"] as String[])
    themeCombo.setPreferredSize(new Dimension(110, 26))

    // --- Edge Labels ---
    JLabel labelPosLabel = new JLabel("Labels:")
    JComboBox<String> labelPosCombo = new JComboBox<>(["Inside", "Above", "Below", "Hidden"] as String[])
    labelPosCombo.setPreferredSize(new Dimension(90, 26))

    // --- Display Mode ---
    JCheckBox denseModeCheck = new JCheckBox("Dense", false)
    denseModeCheck.setToolTipText("Prioritize space using small icons without labels")
    
    JCheckBox filterLibsCheck = new JCheckBox("Hide Std Libs", true)
    filterLibsCheck.setToolTipText("Hide elements stored in SysML or KerML library packages")

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
    toolbar.add(Box.createHorizontalStrut(8))
    toolbar.add(themeLabel)
    toolbar.add(themeCombo)
    toolbar.add(Box.createHorizontalStrut(8))
    toolbar.add(labelPosLabel)
    toolbar.add(labelPosCombo)
    toolbar.add(Box.createHorizontalStrut(8))
    toolbar.add(denseModeCheck)
    toolbar.add(Box.createHorizontalStrut(8))
    toolbar.add(filterLibsCheck)
    toolbar.add(Box.createHorizontalStrut(12))
    toolbar.add(saveButton)
    toolbar.add(loadButton)
    toolbar.add(exportButton)

    // --- Status bar ---
    JLabel statusLabel = new JLabel("  Select an element in the Containment Tree to begin.")
    statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12))
    statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8))

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

    // Store checkbox references for rebuild triggers
    java.util.List<JCheckBox> allNodeCheckboxes = []

    nodeTypeRegistry.each { config ->
        // Usage row
        JPanel usageRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1))
        usageRow.setMaximumSize(new Dimension(250, 26))
        usageRow.setAlignmentX(Component.LEFT_ALIGNMENT)
        JPanel usageSwatch = new JPanel()
        usageSwatch.setPreferredSize(new Dimension(14, 14))
        usageSwatch.setBackground(Color.decode(config.usageColor))
        usageSwatch.setBorder(BorderFactory.createLineBorder(Color.GRAY))
        usageSwatch.setToolTipText("Click to change color")
        JCheckBox usageCb = new JCheckBox("${config.name}", config.usageEnabled)
        usageCb.setFont(new Font("SansSerif", Font.PLAIN, 11))
        usageCb.addItemListener { e ->
            config.usageEnabled = (e.getStateChange() == ItemEvent.SELECTED)
            if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
        }
        // Color swatch click -> color chooser
        usageSwatch.addMouseListener(new MouseAdapter() {
            @Override
            void mouseClicked(MouseEvent e) {
                Color chosen = JColorChooser.showDialog(null, "Choose color for ${config.name} usage", Color.decode(config.usageColor))
                if (chosen != null) {
                    config.usageColor = "#${Integer.toHexString(chosen.getRGB() & 0xFFFFFF).padLeft(6, '0').toUpperCase()}"
                    usageSwatch.setBackground(chosen)
                    if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
                }
            }
        })
        usageRow.add(usageSwatch)
        usageRow.add(usageCb)
        allNodeCheckboxes.add(usageCb)
        nodeSection.add(usageRow)

        // Definition row
        JPanel defRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1))
        defRow.setMaximumSize(new Dimension(250, 26))
        defRow.setAlignmentX(Component.LEFT_ALIGNMENT)
        JPanel defSwatch = new JPanel()
        defSwatch.setPreferredSize(new Dimension(14, 14))
        defSwatch.setBackground(Color.decode(config.defColor))
        defSwatch.setBorder(BorderFactory.createLineBorder(Color.GRAY))
        defSwatch.setToolTipText("Click to change color")
        JCheckBox defCb = new JCheckBox("${config.name} Def", config.defEnabled)
        defCb.setFont(new Font("SansSerif", Font.PLAIN, 11))
        defCb.addItemListener { e ->
            config.defEnabled = (e.getStateChange() == ItemEvent.SELECTED)
            if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
        }
        defSwatch.addMouseListener(new MouseAdapter() {
            @Override
            void mouseClicked(MouseEvent e) {
                Color chosen = JColorChooser.showDialog(null, "Choose color for ${config.name} def", Color.decode(config.defColor))
                if (chosen != null) {
                    config.defColor = "#${Integer.toHexString(chosen.getRGB() & 0xFFFFFF).padLeft(6, '0').toUpperCase()}"
                    defSwatch.setBackground(chosen)
                    if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
                }
            }
        })
        defRow.add(defSwatch)
        defRow.add(defCb)
        allNodeCheckboxes.add(defCb)
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

    edgeTypeRegistry.each { config ->
        JPanel edgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1))
        edgeRow.setMaximumSize(new Dimension(250, 26))
        edgeRow.setAlignmentX(Component.LEFT_ALIGNMENT)
        JPanel edgeSwatch = new JPanel()
        edgeSwatch.setPreferredSize(new Dimension(14, 14))
        edgeSwatch.setBackground(Color.decode(config.color))
        edgeSwatch.setBorder(BorderFactory.createLineBorder(Color.GRAY))
        edgeSwatch.setToolTipText("Click to change color")
        JCheckBox edgeCb = new JCheckBox("${config.name}", config.enabled)
        edgeCb.setFont(new Font("SansSerif", Font.PLAIN, 11))
        edgeCb.addItemListener { e ->
            config.enabled = (e.getStateChange() == ItemEvent.SELECTED)
            if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
        }
        edgeSwatch.addMouseListener(new MouseAdapter() {
            @Override
            void mouseClicked(MouseEvent e) {
                Color chosen = JColorChooser.showDialog(null, "Choose color for ${config.name} edge", Color.decode(config.color))
                if (chosen != null) {
                    config.color = "#${Integer.toHexString(chosen.getRGB() & 0xFFFFFF).padLeft(6, '0').toUpperCase()}"
                    edgeSwatch.setBackground(chosen)
                    if (currentRoot[0] != null) rebuildGraph(currentRoot[0])
                }
            }
        })
        edgeRow.add(edgeSwatch)
        edgeRow.add(edgeCb)
        edgeSection.add(edgeRow)
    }

    controlPanel.add(edgeSection)
    controlPanel.add(Box.createVerticalGlue())

    // Wrap in scroll pane for overflow
    JScrollPane controlScroll = new JScrollPane(controlPanel,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
    controlScroll.getVerticalScrollBar().setUnitIncrement(16)
    controlScroll.setPreferredSize(new Dimension(230, 600))
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
        if (choice == "Circle") {
            layout = new mxCircleLayout(mxg)
        } else if (choice == "Compact Tree") {
            layout = new mxCompactTreeLayout(mxg, false)
        } else if (choice == "Organic") {
            // mxOrganicLayout may not be available in all MagicDraw mxGraph builds
            try {
                def organicClass = Class.forName("com.mxgraph.layout.mxOrganicLayout")
                layout = organicClass.getConstructor(mxGraph.class).newInstance(mxg)
            } catch (Exception ignore) {
                layout = new mxHierarchicalLayout(mxg)  // fallback
            }
        } else {
            layout = new mxHierarchicalLayout(mxg)
        }
        layout.execute(mxg.getDefaultParent())
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

                Object rootCell = mxg.insertVertex(gParent, null, currentRootLabel, 0, 0, nW, nH, rootStyle)
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
                        String edgeTypeName = "Satisfy"
                        if (edgeLbl.contains("dependency")) edgeTypeName = "Dependency"
                        else if (edgeLbl.contains("allocate")) edgeTypeName = "Allocation"

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
                            Object cell = mxg.insertVertex(gParent, null, label, 0, 0, nW, nH, style)
                            cellMap[other] = cell
                            vertexCount++
                            
                            int childRemaining = remaining - 1
                            int childExtra = expandedNodes.getOrDefault(other, 0)
                            queue.add([other, Math.max(childRemaining, childExtra)])
                        }

                        Object srcCell = cellMap[current]
                        Object tgtCell = cellMap[other]
                        if (srcCell != null && tgtCell != null) {
                            String displayLabel = edgeLbl
                            String eStyle = getEdgeStyle(edgeTypeName)
                            mxg.insertEdge(gParent, null, displayLabel, srcCell, tgtCell, eStyle)
                            edgeCount++
                        }
                    }
                }

                // --- Containment edges (owned children) ---
                if (isEdgeEnabled("Containment")) {
                    try {
                        current.getOwnedElement().each { Element child ->
                            if (isEnabledNode(child)) {
                                if (!visited.contains(child)) {
                                    if (remaining <= 0) return
                                    visited.add(child)
                                    String label = getLabel(child)
                                    String style = getNodeStyle(child, false, labelPos)
                                    Object cell = mxg.insertVertex(gParent, null, label, 0, 0, nW, nH, style)
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
                                    if (!edgeKeys.contains(edgeKey)) {
                                        edgeKeys.add(edgeKey)
                                        mxg.insertEdge(gParent, null, containLabel, srcCell, tgtCell, getEdgeStyle("Containment"))
                                        edgeCount++
                                    }
                                }
                            }
                        }
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
                                    displayLabel = "<<supplier>>"
                                }
                                
                                mxg.insertEdge(gParent, null, displayLabel, srcEdgeNode, tgtEdgeNode, getEdgeStyle("Dependency"))
                                edgeCount++
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
                                    String displayLabel = queries[2]
                                    mxg.insertEdge(gParent, null, displayLabel, srcCell, childCell, getEdgeStyle(edgeType))
                                    edgeCount++
                                }
                            }
                        }
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime
            scanTimeMs[0] = (int) elapsed
            String perfWarning = (elapsed > 2000) ? "  \u26A0 SLOW (${elapsed}ms)" : ""
            logDebug("Graph: ${vertexCount} vertices, ${edgeCount} edges, ${elapsed}ms")
            statusLabel.setText("  \u2713 ${currentRootLabel} \u2014 ${vertexCount} nodes, ${edgeCount} edges  (depth=${maxDepth}, ${elapsed}ms)${perfWarning}")

        } finally {
            mxg.getModel().endUpdate()
        }

        try { applyLayout() } catch (Exception ex) { logCrash("Layout error", ex) }
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
        depthWarning.setText(d > 5 ? " âš " : "")
        depthWarning.setToolTipText(d > 5 ? "Depth > 5 may be very slow on large models" : "")
    }
    updateDepthWarning()

    depthSpinner.addChangeListener { evt ->
        updateDepthWarning()
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }
    layoutCombo.addActionListener { e -> if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false) }
    labelPosCombo.addActionListener { e -> if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false) }

    themeCombo.addActionListener { e ->
        currentThemeName[0] = themeCombo.getSelectedItem()
        applyFullTheme()
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }

    denseModeCheck.addItemListener { e ->
        isDenseMode[0] = (e.getStateChange() == ItemEvent.SELECTED)
        if (currentRoot[0] != null) rebuildGraph(currentRoot[0], false)
    }

    exportButton.addActionListener { e ->
        try {
            JFileChooser fc = new JFileChooser()
            fc.setDialogTitle("Export Graph (PNG)")
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fc.getSelectedFile()
                if (!fileToSave.getAbsolutePath().toLowerCase().endsWith(".png")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".png")
                }
                def image = com.mxgraph.util.mxCellRenderer.createBufferedImage(mxg, null, 1.0, java.awt.Color.WHITE, true, null)
                if (image != null) {
                    javax.imageio.ImageIO.write(image, "PNG", fileToSave)
                    statusLabel.setText("  \u2713 Exported to ${fileToSave.getName()}")
                    logDebug("Exported PNG: ${fileToSave.getAbsolutePath()}")
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
            theme: currentThemeName[0],
            depth: (int) depthSpinner.getValue(),
            layout: layoutCombo.getSelectedItem(),
            labelPos: labelPosCombo.getSelectedItem(),
            nodeTypes: nodeTypeRegistry.collect { c ->
                [name: c.name, usageEnabled: c.usageEnabled, defEnabled: c.defEnabled,
                 usageColor: c.usageColor, defColor: c.defColor]
            },
            edgeTypes: edgeTypeRegistry.collect { c ->
                [name: c.name, enabled: c.enabled, color: c.color]
            }
        ]
    }

    def applyConfigMap = { Map cfg ->
        try {
            if (cfg.theme) { currentThemeName[0] = cfg.theme; themeCombo.setSelectedItem(cfg.theme) }
            if (cfg.depth) depthSpinner.setValue(cfg.depth)
            if (cfg.layout) layoutCombo.setSelectedItem(cfg.layout)
            if (cfg.labelPos) labelPosCombo.setSelectedItem(cfg.labelPos)
            if (cfg.nodeTypes != null) {
                cfg.nodeTypes.each { saved ->
                    def match = nodeTypeRegistry.find { it.name == saved.name }
                    if (match) {
                        match.usageEnabled = saved.usageEnabled
                        match.defEnabled = saved.defEnabled
                        if (saved.usageColor) match.usageColor = saved.usageColor
                        if (saved.defColor) match.defColor = saved.defColor
                    }
                }
            }
            if (cfg.edgeTypes != null) {
                cfg.edgeTypes.each { saved ->
                    def match = edgeTypeRegistry.find { it.name == saved.name }
                    if (match) {
                        match.enabled = saved.enabled
                        if (saved.color) match.color = saved.color
                    }
                }
            }
            applyFullTheme()
        } catch (Exception ex) {
            logCrash("Error applying config", ex)
        }
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
            statusLabel.setText("  âœ“ Configuration '${name}' saved.")
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

            String[] names = configs.collect { it.configName ?: "Unnamed" } as String[]
            String chosen = (String) JOptionPane.showInputDialog(null, "Select configuration:", "Load Configuration",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0])
            if (chosen == null) return

            Map cfg = configs.find { it.configName == chosen }
            if (cfg) {
                applyConfigMap(cfg)
                statusLabel.setText("  âœ“ Configuration '${chosen}' loaded.")
                logDebug("Config loaded: ${chosen}")
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
                // Load the most recently saved config
                Map lastCfg = configs.last()
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

