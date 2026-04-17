import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.browser.Node
import com.dassault_systemes.modeler.kerml.model.kerml.*
import com.dassault_systemes.modeler.sysml.model.sysml.*

import javax.swing.*
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import java.awt.*
import java.util.List
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import com.dassault_systemes.modeler.kerml.model.kerml.Package

import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.layout.mxCompactTreeLayout

// -----------------------------------------------------------------------------------
// V2Map Proof of Concept — Core Script
// Phase 1 implementation — Direct mxGraph rendering
// -----------------------------------------------------------------------------------
String scriptDir = "E:\\_Documents\\git\\SysMLv2ClientAPI\\scripts"
File loggerFile = new File(scriptDir, "SysMLv2Logger.groovy")
def SysMLv2Logger = new GroovyClassLoader(getClass().getClassLoader()).parseClass(loggerFile)
def logger = SysMLv2Logger.newInstance("V2Map_PoC")

// --- Debug log file (overwritten each run / rebuild) ---
File debugFile = new File(scriptDir, "V2Map/V2Map_Debug.log")

def logDebug = { String msg ->
    try {
        debugFile.append("[${new Date()}] ${msg}\n")
    } catch (Exception ignore) {}
    logger.info(msg)
}

def logCrash = { String contextMsg, Throwable t ->
    StringBuilder sb = new StringBuilder()
    sb.append("=== V2MAP EXCEPTION ===\n")
    sb.append("Time: ${new Date()}\n")
    sb.append("Context: ${contextMsg}\n")
    sb.append("Exception: ${t.getClass().getName()}\n")
    sb.append("Message: ${t.getMessage()}\n\n")
    Throwable cause = t.getCause()
    if (cause != null) {
        sb.append("Cause: ${cause.getClass().getName()}: ${cause.getMessage()}\n\n")
    }
    sb.append("Relevant Stack Trace:\n")
    t.getStackTrace().each { ste ->
        String cls = ste.getClassName()
        if (!cls.startsWith("org.codehaus.groovy") &&
            !cls.startsWith("java.lang.reflect") &&
            !cls.startsWith("sun.reflect") &&
            !cls.startsWith("groovy.lang.MetaClass")) {
            sb.append("\tat ${ste}\n")
        }
    }
    try {
        debugFile.append(sb.toString())
    } catch (Exception ignore) {}
    logger.error(contextMsg, t)
}

// Clear debug log for this run
try { debugFile.text = "=== V2Map PoC Debug Log â€” ${new Date()} ===\n\n" } catch (Exception ignore) {}

try {
    Project project = Application.getInstance().getProject()
    if (project == null) {
        logger.error("No active MagicDraw project!")
        return
    }

    logDebug("Initializing V2Map PoC Map Viewer...")

    // -----------------------------------------------------------------
    // Utility closures
    // -----------------------------------------------------------------
    def isValidNode = { el ->
        return (el instanceof RequirementUsage || el instanceof RequirementDefinition ||
                el instanceof PartUsage || el instanceof PartDefinition ||
                el instanceof ActionUsage || el instanceof ActionDefinition ||
                el instanceof StateUsage || el instanceof StateDefinition ||
                el instanceof UseCaseUsage || el instanceof UseCaseDefinition ||
                el instanceof ConstraintUsage || el instanceof ConstraintDefinition ||
                el instanceof ConcernUsage || el instanceof ConcernDefinition ||
                el instanceof ViewUsage || el instanceof ViewDefinition ||
                el instanceof ConnectionUsage || el instanceof ConnectionDefinition ||
                el instanceof InterfaceUsage || el instanceof InterfaceDefinition ||
                el instanceof PortUsage || el instanceof PortDefinition ||
                el instanceof ItemUsage || el instanceof ItemDefinition ||
                el instanceof AttributeUsage || el instanceof AttributeDefinition ||
                el instanceof OccurrenceUsage || el instanceof OccurrenceDefinition ||
                el instanceof Package ||
                el instanceof AllocationUsage || el instanceof AllocationDefinition)
    }

    def isDefinition = { el ->
        return (el instanceof PartDefinition || el instanceof RequirementDefinition ||
                el instanceof ActionDefinition || el instanceof StateDefinition ||
                el instanceof UseCaseDefinition || el instanceof ConstraintDefinition ||
                el instanceof ConcernDefinition || el instanceof ViewDefinition ||
                el instanceof ConnectionDefinition || el instanceof InterfaceDefinition ||
                el instanceof PortDefinition || el instanceof ItemDefinition ||
                el instanceof AttributeDefinition || el instanceof OccurrenceDefinition ||
                el instanceof AllocationDefinition)
    }

    def getLabel = { el ->
        try {
            String n = el.getHumanName()
            if (n != null && !n.isEmpty()) return n
            n = el.getDeclaredName()
            if (n != null && !n.isEmpty()) return n
            return el.getClass().getSimpleName()
        } catch (Exception e) {
            return "?"
        }
    }

    def getEdgeLabel = { Relationship rel ->
        try {
            if (rel instanceof SatisfyRequirementUsage) return "<<satisfy>>"
            if (rel instanceof AllocationUsage)         return "<<allocate>>"
            if (rel instanceof Dependency)              return "<<dependency>>"
            if (rel instanceof Specialization)          return "<<specializes>>"
            if (rel instanceof Subsetting)              return "<<subsets>>"
            if (rel instanceof ReferenceSubsetting)     return "<<references>>"
            if (rel instanceof FeatureTyping)           return "<<typed by>>"
            if (rel instanceof Redefinition)            return "<<redefines>>"
            String n = rel.getDeclaredName()
            if (n != null && !n.isEmpty()) return n
            return rel.getClass().getSimpleName()
        } catch (Exception e) {
            return "rel"
        }
    }

    // Style constants
    String STYLE_DEFINITION = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_RECTANGLE};" +
            "${mxConstants.STYLE_FILLCOLOR}=#4A90D9;${mxConstants.STYLE_FONTCOLOR}=#FFFFFF;" +
            "${mxConstants.STYLE_ROUNDED}=1;${mxConstants.STYLE_ARCSIZE}=12;" +
            "${mxConstants.STYLE_FONTSIZE}=12;${mxConstants.STYLE_FONTSTYLE}=1;" +
            "${mxConstants.STYLE_SHADOW}=1;${mxConstants.STYLE_STROKECOLOR}=#2C5F8A"

    String STYLE_USAGE = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_RECTANGLE};" +
            "${mxConstants.STYLE_FILLCOLOR}=#F5A623;${mxConstants.STYLE_FONTCOLOR}=#FFFFFF;" +
            "${mxConstants.STYLE_ROUNDED}=1;${mxConstants.STYLE_ARCSIZE}=20;" +
            "${mxConstants.STYLE_FONTSIZE}=12;${mxConstants.STYLE_FONTSTYLE}=0;" +
            "${mxConstants.STYLE_SHADOW}=1;${mxConstants.STYLE_STROKECOLOR}=#C07D15"

    String STYLE_ROOT = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_RECTANGLE};" +
            "${mxConstants.STYLE_FILLCOLOR}=#2ECC71;${mxConstants.STYLE_FONTCOLOR}=#FFFFFF;" +
            "${mxConstants.STYLE_ROUNDED}=1;${mxConstants.STYLE_ARCSIZE}=12;" +
            "${mxConstants.STYLE_FONTSIZE}=13;${mxConstants.STYLE_FONTSTYLE}=1;" +
            "${mxConstants.STYLE_SHADOW}=1;${mxConstants.STYLE_STROKECOLOR}=#1A9B52;" +
            "${mxConstants.STYLE_STROKEWIDTH}=2.5"
            
    String STYLE_PACKAGE = "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_SWIMLANE};" +
            "${mxConstants.STYLE_STARTSIZE}=22;${mxConstants.STYLE_FILLCOLOR}=#FFC300;" +
            "${mxConstants.STYLE_FONTCOLOR}=#333333;${mxConstants.STYLE_SWIMLANE_FILLCOLOR}=#FFF5D1;" +
            "${mxConstants.STYLE_FONTSIZE}=12;${mxConstants.STYLE_FONTSTYLE}=1;" +
            "${mxConstants.STYLE_SHADOW}=1;${mxConstants.STYLE_STROKECOLOR}=#D68910"

    String STYLE_EDGE = "${mxConstants.STYLE_STROKECOLOR}=#555555;" +
            "${mxConstants.STYLE_FONTCOLOR}=#333333;${mxConstants.STYLE_FONTSIZE}=10;" +
            "${mxConstants.STYLE_ROUNDED}=1"

    // -----------------------------------------------------------------
    // Build the mxGraph directly (not via JGraphXAdapter)
    // -----------------------------------------------------------------
    def mxg = new mxGraph()
    mxg.setAllowDanglingEdges(false)
    mxg.setCellsEditable(false)
    mxg.setConnectableEdges(false)

    def graphComponent = new mxGraphComponent(mxg)
    graphComponent.setConnectable(false)
    graphComponent.setDragEnabled(false)
    graphComponent.getViewport().setOpaque(true)
    graphComponent.getViewport().setBackground(new Color(42, 42, 54))
    graphComponent.setBackground(new Color(42, 42, 54))

    // -----------------------------------------------------------------
    // Status label & toolbar
    // -----------------------------------------------------------------
    JLabel statusLabel = new JLabel("  Select an element in the Containment Tree to begin.")
    statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13))
    statusLabel.setForeground(Color.WHITE)
    statusLabel.setOpaque(true)
    statusLabel.setBackground(new Color(50, 50, 65))
    statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10))

    JLabel depthLabel = new JLabel("Max Depth:")
    depthLabel.setForeground(Color.WHITE)
    depthLabel.setFont(new Font("SansSerif", Font.PLAIN, 12))

    SpinnerNumberModel depthModel = new SpinnerNumberModel(3, 1, 5, 1)
    JSpinner depthSpinner = new JSpinner(depthModel)
    depthSpinner.setPreferredSize(new Dimension(50, 26))
    depthSpinner.setToolTipText("Max traversal depth (1-5). Higher values may be slow on large models.")

    JLabel layoutLabel = new JLabel("  Layout:")
    layoutLabel.setForeground(Color.WHITE)
    layoutLabel.setFont(new Font("SansSerif", Font.PLAIN, 12))

    JComboBox<String> layoutCombo = new JComboBox<>(["Hierarchical", "Circle", "Compact Tree"] as String[])
    layoutCombo.setPreferredSize(new Dimension(130, 26))

    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4))
    toolbar.setBackground(new Color(50, 50, 65))
    toolbar.add(depthLabel)
    toolbar.add(depthSpinner)
    toolbar.add(layoutLabel)
    toolbar.add(layoutCombo)

    // -----------------------------------------------------------------
    // JFrame assembly
    // -----------------------------------------------------------------
    JFrame frame = new JFrame("V2Map Viewer (PoC)")
    frame.setLayout(new BorderLayout())
    frame.add(toolbar, BorderLayout.NORTH)
    frame.add(graphComponent, BorderLayout.CENTER)
    frame.add(statusLabel, BorderLayout.SOUTH)
    frame.setSize(1100, 800)
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)

    // -----------------------------------------------------------------
    // Track current root for rebuild on control changes
    // -----------------------------------------------------------------
    Element[] currentRoot = [null]
    Map<Element, Integer> expandedNodes = [:]
    def elementToCellMapGlobal = [null]

    // -----------------------------------------------------------------
    // Apply layout to the mxGraph
    // -----------------------------------------------------------------
    def applyLayout = {
        String choice = layoutCombo.getSelectedItem()
        def layout
        switch (choice) {
            case "Circle":
                layout = new mxCircleLayout(mxg)
                break
            case "Compact Tree":
                layout = new mxCompactTreeLayout(mxg, false)
                break
            default:
                layout = new mxHierarchicalLayout(mxg)
                break
        }
        layout.execute(mxg.getDefaultParent())
    }

    // -----------------------------------------------------------------
    // Core: rebuild the graph for a selected element
    // -----------------------------------------------------------------
    def rebuildGraph
    rebuildGraph = { Element rootElement, boolean clearExpanded = true ->
        int maxDepth = (int) depthSpinner.getValue()
        logDebug("--- rebuildGraph called ---")
        logDebug("Root: ${rootElement != null ? getLabel(rootElement) : 'null'}")
        logDebug("Root class: ${rootElement?.getClass()?.getName()}")
        logDebug("Max depth: ${maxDepth}")

        if (clearExpanded && currentRoot[0] != rootElement) {
            expandedNodes.clear()
        }
        currentRoot[0] = rootElement

        // Clear entire graph
        mxg.getModel().beginUpdate()
        try {
            mxg.removeCells(mxg.getChildCells(mxg.getDefaultParent(), true, true))
        } finally {
            mxg.getModel().endUpdate()
        }

        if (rootElement == null) {
            statusLabel.setText("  No element selected.")
            graphComponent.refresh()
            return
        }

        if (!isValidNode(rootElement)) {
            String typeName = rootElement.getClass().getSimpleName()
            statusLabel.setText("  âš  Selected element is '${getLabel(rootElement)}' (${typeName}) â€” not a supported PoC type. Try a Part, Requirement, Action, or State.")
            logDebug("Skipping: not a valid node type (${typeName})")
            graphComponent.refresh()
            return
        }

        // -----------------------------------------------------------------
        // Step 1: Find the containing namespace to scan for relationships
        // -----------------------------------------------------------------
        Element scanRoot = rootElement
        // Walk up to find a containing Namespace (package-level) so we can discover
        // Dependency and SatisfyRequirementUsage elements that reference our node
        Element parent2 = rootElement.getOwner()
        while (parent2 != null) {
            if (parent2 instanceof Namespace) {
                scanRoot = parent2
                break
            }
            parent2 = parent2.getOwner()
        }
        logDebug("Scan root for relationship index: ${getLabel(scanRoot)} (${scanRoot.getClass().getSimpleName()})")

        // -----------------------------------------------------------------
        // Step 2: Recursively scan the namespace to build relationship indices
        //   Finds TWO kinds of relationships:
        //   A) Child elements: SatisfyRequirementUsage, Dependency, AllocationUsage
        //      (these live as children in the containment tree)
        //   B) Owned relationships: Specialization, Subsetting, ReferenceSubsetting,
        //      FeatureTyping, Redefinition (these live as ownedRelationship on elements)
        // -----------------------------------------------------------------
        Map<Element, java.util.List<java.util.List>> relationIndex = new HashMap<>()
        Set<String> dedupKeys = new HashSet<>()  // prevent duplicates

        def addRelation = { Element a, Element b, String label ->
            if (a == null || b == null || a.is(b)) return
            // Deduplicate
            int h1 = System.identityHashCode(a)
            int h2 = System.identityHashCode(b)
            String key = (h1 < h2) ? "${h1}-${h2}-${label}" : "${h2}-${h1}-${label}"
            if (dedupKeys.contains(key)) return
            dedupKeys.add(key)

            if (!relationIndex.containsKey(a)) relationIndex.put(a, new ArrayList<>())
            relationIndex.get(a).add([b, label])
            if (!relationIndex.containsKey(b)) relationIndex.put(b, new ArrayList<>())
            relationIndex.get(b).add([a, label])
        }

        def scanForRelationships
        scanForRelationships = { Element el ->
            try {
                // --- (A) Child element relationships ---
                if (el instanceof SatisfyRequirementUsage) {
                    SatisfyRequirementUsage satisfy = (SatisfyRequirementUsage) el
                    def satisfiedReq = satisfy.getSatisfiedRequirement()
                    Element satisfyingFeature = satisfy.getSatisfyingFeature()
                    if (satisfyingFeature == null) satisfyingFeature = satisfy.getOwner()
                    if (satisfiedReq != null && satisfyingFeature != null) {
                        addRelation(satisfyingFeature, satisfiedReq, "<<satisfy>>")
                        logDebug("  satisfy: ${getLabel(satisfyingFeature)} -> ${getLabel(satisfiedReq)}")
                    }
                }

                if (el instanceof Dependency) {
                    Dependency dep = (Dependency) el
                    def clients = dep.getClient()
                    def suppliers = dep.getSupplier()
                    if (clients != null && suppliers != null) {
                        for (Element c : clients) {
                            for (Element s : suppliers) {
                                addRelation(c, s, "<<dependency>>")
                                logDebug("  dependency: ${getLabel(c)} -> ${getLabel(s)}")
                            }
                        }
                    }
                }

                if (el instanceof AllocationUsage) {
                    AllocationUsage alloc = (AllocationUsage) el
                    def related = alloc.getRelatedElement()
                    if (related != null && related.size() >= 2) {
                        addRelation(related[0], related[1], "<<allocate>>")
                        logDebug("  allocate: ${getLabel(related[0])} -> ${getLabel(related[1])}")
                    }
                }

                // --- (B) Owned relationship structural links ---
                // These are KerML relationships owned by the element itself
                try {
                    el.getOwnedRelationship().each { Relationship rel ->
                        def related = rel.getRelatedElement()
                        if (related == null || related.size() < 2) return
                        Element source = related[0]  // owning element
                        Element target = related[1]  // related element
                        if (source == null || target == null || source.is(target)) return
                        // Skip relationships to library/standard-library elements
                        // (they create noise; only track project-local elements)
                        String targetClass = target.getClass().getName()
                        if (targetClass.contains(".library.")) return

                        if (rel instanceof Specialization && !(rel instanceof Subsetting) && !(rel instanceof Redefinition) && !(rel instanceof FeatureTyping)) {
                            addRelation(source, target, "<<specializes>>")
                            logDebug("  specializes: ${getLabel(source)} -> ${getLabel(target)}")
                        } else if (rel instanceof ReferenceSubsetting) {
                            addRelation(source, target, "<<references>>")
                            logDebug("  references: ${getLabel(source)} -> ${getLabel(target)}")
                        } else if (rel instanceof Subsetting && !(rel instanceof ReferenceSubsetting) && !(rel instanceof Redefinition)) {
                            addRelation(source, target, "<<subsets>>")
                            logDebug("  subsets: ${getLabel(source)} -> ${getLabel(target)}")
                        } else if (rel instanceof FeatureTyping) {
                            addRelation(source, target, "<<typed by>>")
                            logDebug("  typed by: ${getLabel(source)} -> ${getLabel(target)}")
                        } else if (rel instanceof Redefinition) {
                            addRelation(source, target, "<<redefines>>")
                            logDebug("  redefines: ${getLabel(source)} -> ${getLabel(target)}")
                        }
                    }
                } catch (Exception relEx) {
                    // Some elements may not support getOwnedRelationship
                }

            } catch (Exception ex) {
                logCrash("Error scanning element ${getLabel(el)}", ex)
            }

            // Recurse into children
            try {
                for (Element child : el.getOwnedElement()) {
                    scanForRelationships(child)
                }
            } catch (Exception ignore) {}
        }

        logDebug("Scanning namespace for relationships...")
        scanForRelationships(scanRoot)
        logDebug("Relationship index built: ${relationIndex.size()} elements have relationships, ${dedupKeys.size()} unique edges")

        // -----------------------------------------------------------------
        // Step 3: BFS from the root element using the relationship index
        // -----------------------------------------------------------------
        Map<Element, Object> elementToCellMap = [:]  // Element -> mxCell
        elementToCellMapGlobal[0] = elementToCellMap
        Set<Element> visited = new HashSet<>()
        java.util.List<java.util.List> queue = []  // [ [element, remainingDepth] ]

        mxg.getModel().beginUpdate()
        try {
            Object parent = mxg.getDefaultParent()

            // Insert root vertex
            String rootLabel = getLabel(rootElement)
            Object rootCell = mxg.insertVertex(parent, null, rootLabel, 0, 0, 160, 50, STYLE_ROOT)
            elementToCellMap[rootElement] = rootCell
            visited.add(rootElement)
            
            int rootRemaining = maxDepth + expandedNodes.getOrDefault(rootElement, 0)
            queue.add([rootElement, rootRemaining])

            int edgeCount = 0
            int vertexCount = 1

            while (!queue.isEmpty()) {
                def entry = queue.remove(0)
                Element current = entry[0]
                int remaining = entry[1]

                if (remaining <= 0) continue

                // Use the relationship index to find connected elements
                java.util.List<java.util.List> relations = relationIndex.get(current)
                if (relations != null) {
                    relations.each { tuple ->
                        Element other = tuple[0]
                        String edgeLbl = tuple[1]

                        if (other != null) {
                            if (!visited.contains(other)) {
                                visited.add(other)
                                String label = getLabel(other)
                                String style = (other instanceof Package) ? STYLE_PACKAGE : (isDefinition(other) ? STYLE_DEFINITION : STYLE_USAGE)
                                Object cell = mxg.insertVertex(parent, null, label, 0, 0, 160, 50, style)
                                elementToCellMap[other] = cell
                                vertexCount++
                                
                                int childRemaining = remaining - 1
                                int childExtra = expandedNodes.getOrDefault(other, 0)
                                queue.add([other, Math.max(childRemaining, childExtra)])
                            }
                            // Add edge (avoid duplicates by checking both directions)
                            Object srcCell = elementToCellMap[current]
                            Object tgtCell = elementToCellMap[other]
                            if (srcCell != null && tgtCell != null) {
                                mxg.insertEdge(parent, null, edgeLbl, srcCell, tgtCell, STYLE_EDGE)
                                edgeCount++
                            }
                        }
                    }
                }

                // Also traverse direct owned children that are valid node types (containment)
                try {
                    current.getOwnedElement().each { Element child ->
                        if (isValidNode(child) && !visited.contains(child)) {
                            visited.add(child)
                            String label = getLabel(child)
                            String style = (child instanceof Package) ? STYLE_PACKAGE : (isDefinition(child) ? STYLE_DEFINITION : STYLE_USAGE)
                            Object cell = mxg.insertVertex(parent, null, label, 0, 0, 160, 50, style)
                            elementToCellMap[child] = cell
                            vertexCount++

                            // Containment edge (dashed)
                            Object srcCell = elementToCellMap[current]
                            String containStyle = STYLE_EDGE + ";${mxConstants.STYLE_DASHED}=1"
                            mxg.insertEdge(parent, null, "owns", srcCell, cell, containStyle)
                            edgeCount++

                            int childRemaining = remaining - 1
                            int childExtra = expandedNodes.getOrDefault(child, 0)
                            queue.add([child, Math.max(childRemaining, childExtra)])
                        }
                    }
                } catch (Exception ex) {
                    logCrash("Error traversing owned elements for ${getLabel(current)}", ex)
                }
            }

            logDebug("Graph built: ${vertexCount} vertices, ${edgeCount} edges")
            statusLabel.setText("  âœ“ ${getLabel(rootElement)} â€” ${vertexCount} nodes, ${edgeCount} edges  (depth=${maxDepth})")

        } finally {
            mxg.getModel().endUpdate()
        }

        // Apply layout
        try {
            applyLayout()
        } catch (Exception ex) {
            logCrash("Error applying layout", ex)
        }

        graphComponent.refresh()
    }

    // -----------------------------------------------------------------
    // Control listeners: rebuild on depth/layout change
    // -----------------------------------------------------------------
    depthSpinner.addChangeListener { evt ->
        if (currentRoot[0] != null) {
            rebuildGraph(currentRoot[0], false)
        }
    }

    layoutCombo.addActionListener(new ActionListener() {
        @Override
        void actionPerformed(ActionEvent ae) {
            if (currentRoot[0] != null) {
                rebuildGraph(currentRoot[0], false)
            }
        }
    })

    // -----------------------------------------------------------------
    // Interactive Expansion (Double Click)
    // -----------------------------------------------------------------
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

    // -----------------------------------------------------------------
    // Containment Tree listener
    // -----------------------------------------------------------------
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
                logCrash("Exception in Containment Tree Selection Event", ex)
            }
        }
    }

    treeComponent.addTreeSelectionListener(selectionListener)

    frame.addWindowListener(new WindowAdapter() {
        @Override
        void windowClosed(WindowEvent e) {
            logDebug("Window closed â€” removing listener.")
            treeComponent.removeTreeSelectionListener(selectionListener)
        }
    })

    // -----------------------------------------------------------------
    // Seed initial selection
    // -----------------------------------------------------------------
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
        statusLabel.setText("  âš  No element selected. Please select a Part, Requirement, Action, or State in the Containment Tree.")
        logDebug("No valid element selected at startup.")
    }

    frame.setLocationRelativeTo(Application.getInstance().getMainFrame())
    frame.setVisible(true)

    logDebug("V2Map PoC window opened successfully.")

} catch (Throwable t) {
    logCrash("Critical failure launching V2Map PoC", t)
}

