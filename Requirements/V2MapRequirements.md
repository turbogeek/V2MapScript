# V2MapRequirements.md

## 1. Overview

These are requirements for a script to display a map diagram of a SysMLv2 model.

## 2. Requirements

### 2.1 General Requirements

- The script should be able to display a map diagram of a SysMLv2 model.
- The script shere should be two lists of elements that allow the user to select the elements to display in the map diagram.
- The script should use only diagram libraries already a part of MagicDraw, no external libraries should be required and a custom solution is not to be used as the script should be able to run in any MagicDraw environment with minimal size.
- The graphics library we are using shal be <https://jgrapht.org/> version 1.5.1 and compatible version of JGraphX which is currently installed in MagicDraw.
- The tool should use standard SysMLv2 relationships renderings which means the arrows and lines should conform to SysMLv2 standard renderings.
- The tool fpr remdering nodes can render as standard symbols or simple circles, rectangles or squares. As a minium pattern,all useages shall be circles and defimitions are boxes. The symbol for the package needs to look like a folder.
- We need to be able to expand the depth for an individual node interactively. In other words, when at a leaf node at depth of 3, the user may want to expand that leaf to see one level further from that node.
- User has the ability to display name of relationship, above or below the relationship line.
- Use E:\_Documents\git\SysMLv2ClientAPI\scripts\SysMLv2Logger.groovy as part of the logging and debugging of the script.
- We will be using Groovy to build the script
- The graphics library we are using shal be <https://jgrapht.org/> version 1.5.1
- There are obviously other options in the MagicDraw environment that we can use to build the script, so we need to create an additional panel to control the features and options.
- Some features will need to be enabled by default and others will need to be enabled by the user.
- Need to be able to search for a node or edge name, and center and (maybe) zoom in on the element in the map diagram.

### Oter Requirements

- Create, modify, or aquire,    any sub agents as needed to assist in the development of the script.
- At each step, we want to be able to run the script and see the results of our changes.
- Use unit tests to verify the script works as expected.
- Use integration tests to verify the script works as expected.
- Use system tests to verify the script works as expected.
- Use acceptance tests to verify the script works as expected.
- Use regression tests to verify the script works as expected.
- Maintain good security and coding practices
- Never commit code that is not tested and working.
- do not use any libraries that are not already installed in MagicDraw unless explicitly approved.
- Ensure best practice for exception handling and logging to ensure that the script cannot crash MagicDraw and that all exceptions are caught and logged with the user being notified of the error.
-Please ensure that you are following best practice for GUI that ensures readability, limit the number of colors, fonts, and other visual elements to the minimum necessary to convey the information effectively.
-For style of interface, support a dark mode and a light mode and hello kitty mode.
- Ensure you are using the correct AL LLM engine for the task (Gemini/Claude/DeepSeek/etc.). When you can use a lower cost engine, use it. Only use high cost engines when necessary for very complex tasks.
- Document results of LLM usage in the script's log file.
- Document research and build sub-agents when necessary, for example learning the API for jgrapht.

## 2.2 Use Cases

### 2.2.1 Use Case 1 - Launching of the map viewer

- The user launches the map viewer by selecting the "Map Viewer" option from the "Tools" menu.
- The map viewer opens in a new window.
- The map viewer displays the map diagram of the SysMLv2 model.

### 2.2.2 Use Case 2 - Changing the start Node On the Map Diagram

- The user can change the start node by selecting a different node in the map viewer.
- The map viewer updates the map diagram to display the new start node.

### 2.2.3 Use Case 3 - Changing the start Node by selecting one or elements in the Containment Tree

- The user can change the start node by selecting a different node in the Containment Tree.
- The map viewer updates the map diagram to display the new start node(s).

### 2.2.4 Use Case 4 - Editing Node Element Type List

- The user can select a checkbox next to the element type to enable or disable the display of nodes. Each node type has a checkbox for definitions and useages.
- As the items are enabled the nodes are added to the map diagram.
- As the items are disabled the nodes are removed from the map diagram.
- Includes 2.2.8

### 2.2.5 Use Case 5 - Editing Edges Type List

- The user can select a checkbox next to the element type to enable or disable the display of edges. Each edge type has a checkbox for definitions and useages.
- As the items are enabled the edges are added to the map diagram.
- As the items are disabled the edges are removed from the map diagram.
- Includes 2.2.8

### 2.2.6 Use Case 6 - Editing the Color scheme of Nodes

- The user can select a color from a color palette to change the color of the nodes.
- The color of the nodes is changed to the selected color and background color is changed to the selected color.
- On initial load the color scheme is set to the default color schemes for all node types to a json file that will psersist any changes made by the user. The initial color scheme should be of high contrast and visually appealing    .
- In the enable/disable control, the color of the node type is displayed in a small box next to the name of the element type.
- The when the user clicks on the color box next to the node type, the color palette is displayed and the user can select a new fill color, fill pattern, and outline color for the node type. After the node is edited, save the changes to the json file.
- Includes 2.2.8

### 2.2.7 Use Case 7 - Editing the Color scheme of Edges

- The user can select a color from a color palette to change the color of the edges.
- The color of the edges is changed to the selected color and background color is changed to the selected color, line type and icons (circle, square, diamond, triangle,etc.).
- On initial load the color scheme is set to the default color schemes for all edge types to a json file that will psersist any changes made by the user. The initial color scheme and patterns should be of high contrast and visually appealing    .
- In the enable/disable control, the color of the edge type is displayed in a small box next to the name of the edge type.
- The when the user clicks on the color box next to the edge type, the color palette is displayed and the user can select a new fill color, fill pattern, and outline color for the edge type.
- After the edge is edited, save the changes to the json file.
- Includes 2.2.8

### 2.2.8 Use Case 8 - Save and Load of Map Configurations

- The user can save the current map configuration to a json file.
- The user can load a map configuration from a json file.
- The json file has zero or more configurations. Each configuration has a name, description, and a list of selected nodeswith option to display the enabled nodes and edges and their color schemes.
- the interface has a load button to launch a dialog listing available configurations to select and or delete.
- The interface has a save button to save the current map configuration to a json file. The user is given an option to overwrite the curent configuration or create a new configuration.

### 2.2.9 Use Case 9 - Performance Warnings & Highlight

- The GUI must highlight or warn the user about node/edge configurations or features (like deep traversal or dense layouts) that are known to cause slow graph regeneration or UI lag.

### 2.2.10 Use Case 10 - Layout & Routing Selection

- The user can select the graph layout (e.g., Hierarchical, Circular, Organic) and edge routing algorithm from a dropdown or similar control to optimize the readability of the visualization based on the current model elements.

### 2.2.11 Use Case 11 - Model information Navigation from graph

- The user can select a node or edge in the graph and navigate to the corresponding element in the model browser.
- The user can select a node or edge in the graph and open the properties dialog for that element.
- The user can select a node or edge in the graph and open the definition or usage dialog for that element.
- Can open the text editor for the namepace of the element.
- Can find a diagram that includes the element and open the diagram.
- Can find a diagram that includes the element and open the diagram and center on the element.
- Can find a diagram that includes the element and open the diagram and center on the element and zoom in on the element.

### 2.2.12 Use Case 12 - when an element is selectected in a diagram,use as a starting node

- The user can select a node or edge in the graph and use it as a starting node for a new map diagram.
- The user can select a node or edge in the graph and use it as a starting node for a new map diagram and display the new map diagram in a new window.

### 2.2.13 Use Case 13 - when an edge is selectected in diagram or containment tree, use the two nodes as starting nodes

- The user can select an edge in the graph or containment tree and use the two nodes as starting nodes for a new map diagram.
- The user can select an edge in the graph or containment tree and use the two nodes as starting nodes for a new map diagram and display the new map diagram in a new window.

### 2.3 GUI Best Practices

- Lists must be alphabetized by default.
- Lists must be contained within scrollable panels.
- For very large lists, categorize the contents to aid in location and findability.
- Findability is very important. Users must be able to locate items easily.
- Legends or node/edge configurators must show the color of text, outline, and fill color/pattern visible in a square next to the kind of node (including highlight colors if applicable).
- Similarly, line/edge controls must visually indicate the end symbol, pattern, color, line thickness, text color, and highlight color.
- All panels must be resizable, collapsible, and expandable to maximize drawing space.
- The layout interface must include a button to reset the layout/panels to their default configuration.
- The primary graph area must be scrollable to navigate large visualizations smoothly.
- **Selection Handling:** The map node generator must reliably hook into global selection states so that users interacting with items in external views or diagrams do not trigger synchronization bugs or crashes.

### 2.4 Symbology and Visual Modes

- **SysMLv2 Mode:** The visualization must strictly follow rules for SysMLv2 symbology to look similar to the CATIA Magic SysMLv2 implementation. This includes using the same symbology for edge arrows as outlined in the spec (specifically modeling strict edges like "owns", "references", "reference subsetting", "subsetting", "FeatureMembership", etc.).
- **Implied Relationships:** The implementation must map relationships that are implied by properties rather than just raw edges. For example, "Feature Typing" must visually map the type of a usage inherited by a definition, treating "Reference Subsetting" and "Subsetting" with similar implied handling.
- **Dense Mode:** The tool must feature alternative visual modes (e.g., a spatial footprint prioritizing "dense mode"). In this mode, the graph prioritizes space by utilizing smaller icons and actively hiding text labels.
- **Label Interactions:** In dense modes without regular labels, the user must be able to view node/edge information by rolling over (hovering over) the element. Alternatively, provide a hotkey or toggle button to turn labels on/off across the graph.
- **Image Export:** The graph must support exporting and saving the visualization as an image, specifically supporting **SVG, PNG, and JPG** formats. The exported image must auto-embed or provide an accompanying graphical key (legend) representing the currently enabled node and edge types.

### Node Style Dialog

For editing the style of the nodes in the graph.Where reasonable, the on the same line as the lable (like thickness) or a preview box, symbol, or line and a button or drop down to select the variation or color.

- Contains the following sections:
  - Node fill color
  - node fill pattern (horzontal stripes, vertical stripes, diagonal stripes, checkerboard, dots, solid, dots, circles, squares, crosses, etc.)
  - Node outline color
  - Node outline pattern
  - Node outline thickness
  - Node text color
  - Node text font
  - Node text style
  - Node text pattern
  - Node text thickness
  - Node highlight color
  - Node highlight pattern
  - Node highlight thickness
  - Node highlight text color
  - Node highlight text thickness
  - Node Shape (rectangle, rounded corner rectangle,oval,circle, diamond,Hexagon,pentagon, triangle,inverted triangle,folder, parallelagram,document, flopy disk, tank, airplane, person (stick figure), dog astronaught,view types (hierarchy icon, flow icon, IBD/interconnection, states,etc.), others as would be found in a vector drawing tool)
  - Dense Node Shape (and when text is not inside shape) (rectangle, rounded corner rectangle,oval,circle, diamond,Hexagon,pentagon, triangle,inverted triangle,folder, parallelagram,document, flopy disk, tank, airplane, person (stick figure), dog astronaught,view types (hierarchy icon, flow icon, IBD/interconnection, states,etc.), others as would be found in a vector drawing tool)
  - Node text style
  - Node text pattern
  - Node text thickness

### Edge Style Dialog

For editing the style of the edges in the graph.Where reasonable, the on the same line as the lable (like thickness) or a preview box, symbol, or line and a button or drop down to select the variation or color.

- Contains the following sections:
  - Edge pattern
  - Edge thickness
  - Edge text color
  - Edge highlight color
  - Edge highlight pattern
  - Edge highlight thickness
  - Edge highlight text color
  - Edge start symbol(different symbols for different start edge types (owns, references, reference subsetting, subsetting, FeatureMembership, etc.))
  - Edge end symbol (different symbols for different end edge types (owns, references, reference subsetting, subsetting, FeatureMembership, etc.))
