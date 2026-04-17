import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.view.mxGraph

def mxg = new mxGraph()
def graphComponent = new mxGraphComponent(mxg) {
    String getToolTipForCell(Object cell) {
        return "Test"
    }
}
Write-Host "Success"
