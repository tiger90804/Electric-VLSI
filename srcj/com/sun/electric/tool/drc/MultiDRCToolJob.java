package com.sun.electric.tool.drc;

import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.Job;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.*;

import java.util.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Nov 15, 2007
 * Time: 2:18:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiDRCToolJob extends Job {
    private static long globalStartTime = 0;
    private Layer theLayer;
    private Cell topCell;
    private DRCTemplate minAreaRule, enclosedAreaRule, spacingRule;
    private ErrorLogger errorLogger;
    private CellLayersContainer cellLayersCon;

    // Must be static so it could be called in constructor
    private static String getJobName(Cell c, Layer l) { return "Design-Rule Check " + c + ", layer " + l.getName(); }

    MultiDRCToolJob(Cell c, Layer l, DRCTemplate minAreaR, DRCTemplate enclosedAreaR, DRCTemplate spacingR,
                    CellLayersContainer cellLayersC)
    {
        super(getJobName(c, l), DRC.getDRCTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
        this.topCell = c;
        this.theLayer = l;
        this.minAreaRule = minAreaR;
        this.enclosedAreaRule = enclosedAreaR;
        this.spacingRule = spacingR;
        this.cellLayersCon = cellLayersC;
        startJob();
    }

    public boolean doIt()
    {
        long startTime = System.currentTimeMillis();
        errorLogger = DRC.getDRCErrorLogger(true, false, theLayer);
        String msg = "Cell " + topCell.getName() + " , layer " + theLayer.getName();
        System.out.println("DRC for " + msg);
        HierarchyEnumerator.Visitor quickArea = new LayerAreaEnumerator(GeometryHandler.GHMode.ALGO_SWEEP,
                cellLayersCon);
        HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, quickArea);

        long endTime = System.currentTimeMillis();
        int errorCount = errorLogger.getNumErrors();
        int warnCount = errorLogger.getNumWarnings();
        System.out.println(errorCount + " errors and " + warnCount + " warnings found in layer " + msg
                + " (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
        long accuEndTime = System.currentTimeMillis() - globalStartTime;
        System.out.println("Accumulative time " + TextUtils.getElapsedTime(accuEndTime));
        return true;
    }

    public static void checkDesignRules(Cell cell)
    {
        // Check if there are DRC rules for particular tech
        Technology tech = cell.getTechnology();
		DRCRules rules = DRC.getRules(tech);
        // Nothing to check for this particular technology
		if (rules == null || rules.getNumberOfRules() == 0)
                return;

        globalStartTime = System.currentTimeMillis();
        CellLayersContainer cellLayersCon = new CellLayersContainer();
        CheckLayerEnumerator layerCheck = new CheckLayerEnumerator(cell.getTechnology().getNumLayers(),
                cellLayersCon);
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, layerCheck);

        // Multi thread the DRC algorithms per layer
        for (Iterator<Layer> it = cell.getTechnology().getLayers(); it.hasNext();)
        {
            Layer layer = it.next();
            if (layerCheck.layersMap.get(layer) == null)
            {
                System.out.println("Skipping Layer " + layer.getName());
                continue;
            }
            if (layer.getFunction().isDiff() && layer.getName().toLowerCase().equals("p-active-well"))
                continue; // dirty way to skip the MoCMOS p-active well

            DRCTemplate minAreaRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINAREA);
            DRCTemplate enclosedAreaRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINENCLOSEDAREA);
            DRCTemplate spaceRule = DRC.getSpacingRule(layer, null, layer, null, true, -1, -1.0, -1.0); // UCONSPA, CONSPA or SPACING
            if (minAreaRule == null && enclosedAreaRule == null && spaceRule == null)
                continue; // nothing to check
            new MultiDRCToolJob(cell, layer, minAreaRule, enclosedAreaRule, spaceRule, cellLayersCon);
        }
    }

    /**************************************************************************************************************
	 *  CheckLayerEnumerator class
	 **************************************************************************************************************/
    /** Class to collect which layers are available in the design
     *
     */
    private static class CellLayersContainer
    {
        private Map<Cell,Set<Layer>> cellLayersMap;

        CellLayersContainer()
        {
            cellLayersMap = new HashMap<Cell,Set<Layer>>();
        }
        
        boolean addCellLayers(Cell cell, Layer layer)
        {
            Set<Layer> set = cellLayersMap.get(cell);

            // first time the cell is accessed
            if (set == null)
            {
                set = new HashSet<Layer>(1);
                cellLayersMap.put(cell, set);
            }
            return set.add(layer);
        }
    }

    private static class CheckLayerEnumerator extends HierarchyEnumerator.Visitor
    {
        private int numLayersDone = 0, totalNumLayers;
        private Map<Layer,Layer> layersMap;
        private Map<Cell,Cell> cellsMap;
        private Map<PrimitiveNode,PrimitiveNode> primitivesMap;
        private CellLayersContainer cellLayersCon;

        CheckLayerEnumerator(int totalNumL, CellLayersContainer cellLayersC)
        {
            this.totalNumLayers = totalNumL;
            layersMap = new HashMap<Layer,Layer>(totalNumLayers);
            cellsMap = new HashMap<Cell,Cell>();
            primitivesMap = new HashMap<PrimitiveNode,PrimitiveNode>();
            cellLayersCon = cellLayersC;
        }

        /**
         * When the cell should be visited. Either it is the first time or the number of layers hasn't reached
         * the maximum
         * @param cell
         * @return
         */
        private boolean skipCell(Cell cell) {return cellsMap.get(cell) != null || doneWithLayers();}

        private boolean doneWithLayers() {return numLayersDone == totalNumLayers;}

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();
            if (skipCell(cell)) return false; // skip
            cellsMap.put(cell, cell);
            return true;
        }

        private Set<Layer> getLayersInCell(Cell cell)
        {
            Map<NodeProto,NodeProto> tempNodeMap = new HashMap<NodeProto,NodeProto>();
            Map<ArcProto,ArcProto> tempArcMap = new HashMap<ArcProto,ArcProto>();
            Set<Layer> set = new HashSet<Layer>();

            // Nodes
            for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
            {
                NodeInst ni = it.next();
                NodeProto np = ni.getProto();
                if (ni.isCellInstance())
                {
                    Set<Layer> s = cellLayersCon.cellLayersMap.get(np);
                    set.addAll(s);
                    assert(s != null); // it must have layers? unless is empty
                }
                else
                {
                    if (tempNodeMap.get(np) != null)
                        continue; // done with this PrimitiveNode
                    tempNodeMap.put(np, np);

                    if (NodeInst.isSpecialNode(ni)) // like pins
                        continue;

                    PrimitiveNode pNp = (PrimitiveNode)np;
                    for (Technology.NodeLayer nLayer : pNp.getLayers())
                    {
                        Layer layer = nLayer.getLayer();
                        set.add(layer);
                    }
                }
            }

            // Arcs
            for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                ArcProto ap = ai.getProto();
                if (tempArcMap.get(ap) != null)
                    continue; // done with this arc primitive
                tempArcMap.put(ap, ap);
                for (int i = 0; i < ap.getNumArcLayers(); i++)
                {
                    Layer layer = ap.getLayer(i);
                    set.add(layer);
                }
            }
            return set;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();
            Set<Layer> set = getLayersInCell(cell);
            cellLayersCon.cellLayersMap.put(cell, set);
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
            if (doneWithLayers()) return false; // done;
            NodeInst ni = no.getNodeInst();
            if (NodeInst.isSpecialNode(ni)) return (false);
            NodeProto np = ni.getProto();

            // Cells
            if (ni.isCellInstance()) return (true);

            PrimitiveNode pNp = (PrimitiveNode)np;
            if (primitivesMap.get(pNp) != null) return false; // done node.
            primitivesMap.put(pNp, pNp);

            for (Technology.NodeLayer nLayer : pNp.getLayers())
            {
                Layer layer = nLayer.getLayer();
                if (layersMap.get(layer) == null) // not in yet
                {
                    layersMap.put(layer, layer);
                    numLayersDone++;
                }
            }
            return false;
        }
    }

    /**************************************************************************************************************
	 *  LayerAreaEnumerator class
	 **************************************************************************************************************/
    /**
     * Class that uses local GeometryHandler to calculate the area per cell
     */
    private class LayerAreaEnumerator extends HierarchyEnumerator.Visitor
    {
        private Map<Cell,GeometryHandlerLayerBucket> cellsMap;
        private Layer.Function.Set thisLayerFunction;
        private GeometryHandler.GHMode mode;
        private Collection<PrimitiveNode> nodesList; // NodeProto that contains this layer
        private Collection<ArcProto> arcsList; // ArcProto that contains this layer
        private CellLayersContainer cellLayersCon;

        LayerAreaEnumerator(GeometryHandler.GHMode m, CellLayersContainer cellLayersC)
        {
            // This is required so the poly arcs will be properly merged with the transistor polys
            // even though non-electrical layers are retrieved
            this.thisLayerFunction = (theLayer.getFunction().isPoly()) ?
                    new Layer.Function.Set(theLayer.getFunction(), Layer.Function.GATE) :
                    new Layer.Function.Set(theLayer.getFunction());
            this.mode = m;
            cellsMap = new HashMap<Cell,GeometryHandlerLayerBucket>();
            nodesList = new ArrayList<PrimitiveNode>();
            this.cellLayersCon = cellLayersC;
            for (PrimitiveNode node : topCell.getTechnology().getNodesCollection())
            {
                for (Technology.NodeLayer nLayer : node.getLayers())
                {
                    if (thisLayerFunction.contains(nLayer.getLayer().getFunction()))
//                    if (nLayer.getLayer() == theLayer)
                    {
                        nodesList.add(node);
                        break; // found
                    }
                }
            }
            arcsList = new ArrayList<ArcProto>();
            for (ArcProto ap : topCell.getTechnology().getArcsCollection())
            {
                for (int i = 0; i < ap.getNumArcLayers(); i++)
                {
                    if (ap.getLayer(i) == theLayer)
                    {
                        arcsList.add(ap);
                        break; // found
                    }
                }
            }
        }

        private class GeometryHandlerLayerBucket {
            GeometryHandler local;
            boolean merged = false;
            GeometryHandlerLayerBucket()
            {
                local = GeometryHandler.createGeometryHandler(mode, 1);
            }
            void mergeGeometry(Cell cell)
            {
                if (!merged)
                {
                    merged = true;
                    for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
                    {
                        NodeInst ni = it.next();
                        if (!ni.isCellInstance()) continue; // only cell instances
                        AffineTransform trans = ni.transformOut();
                        Cell protoCell = (Cell)ni.getProto();
                        GeometryHandlerLayerBucket bucket = cellsMap.get(protoCell);
                        local.addAll(bucket.local, trans);
//                        if (protoCell.getId().numUsagesOf() <= 1) // used only here
//                        {
//                            System.out.println("Here");
//                            cellsMap.put(protoCell, null);
//                        }
                    }
                    local.postProcess(true);
                }
                else
                {
                    assert(false); // It should not happen
                }
            }
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
//            if (job != null && job.checkAbort()) return false;
            Cell cell = info.getCell();
            Set<Layer> set = cellLayersCon.cellLayersMap.get(cell);
            assert(set != null);

            if (!set.contains(theLayer))
                System.out.println("Cell " + cell.getName() + " doesn't have layer " + theLayer.getName());

            GeometryHandlerLayerBucket bucket = cellsMap.get(cell);
            if (bucket == null)
            {
                bucket = new GeometryHandlerLayerBucket();
                cellsMap.put(cell, bucket);
            }
            else
            {
                assert(bucket.merged);
                return false; // done with this cell
            }

            for(Iterator<ArcInst> it = info.getCell().getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                Network aNet = info.getNetlist().getNetwork(ai, 0);

                // aNet is null if ArcProto is Artwork
                if (aNet == null)
                    continue;
                ArcProto ap = ai.getProto();
                boolean notFound = !arcsList.contains(ap);

                if (notFound)
                    continue; // primitive doesn't contain this layer

                Technology tech = ap.getTechnology();
                Poly[] arcInstPolyList = tech.getShapeOfArc(ai, thisLayerFunction);
                int tot = arcInstPolyList.length;

                for(int i=0; i<tot; i++)
                {
                    Poly poly = arcInstPolyList[i];
                    addElementLocal(poly, theLayer, bucket);
                }
            }

            return true;
        }

        private void addElementLocal(Poly poly, Layer layer, GeometryHandlerLayerBucket bucket)
        {
            bucket.local.add(layer, poly);
        }

        public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();
            boolean isTopCell = cell == topCell;
            GeometryHandlerLayerBucket bucket = cellsMap.get(cell);

            bucket.mergeGeometry(cell);

            if (isTopCell)
            {
                for (Layer layer : bucket.local.getKeySet())
                {
                    checkMinAreaLayerWithTree(bucket.local, topCell, layer);
                }
            }
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {

//            if (job != null && job.checkAbort()) return false;
            // Facet or special elements
			NodeInst ni = no.getNodeInst();
            if (NodeInst.isSpecialNode(ni)) return (false);
			NodeProto np = ni.getProto();
            GeometryHandlerLayerBucket bucket = cellsMap.get(info.getCell());

            // Cells
            if (ni.isCellInstance()) return (true);

			AffineTransform trans = ni.rotateOut();
//            AffineTransform root = info.getTransformToRoot();
//			if (root.getType() != AffineTransform.TYPE_IDENTITY)
//				trans.preConcatenate(root);

            PrimitiveNode pNp = (PrimitiveNode)np;
            boolean notFound = !nodesList.contains(pNp);
            if (notFound) return false; // pNp doesn't have the layer

            Technology tech = pNp.getTechnology();

            // Don't get electric layers in case of transistors otherwise it is hard to detect ports
            Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, false, true, thisLayerFunction);
			int tot = nodeInstPolyList.length;
            
            for(int i=0; i<tot; i++)
			{
				Poly poly = nodeInstPolyList[i];

                poly.roundPoints(); // Trying to avoid mismatches while joining areas.
                poly.transform(trans);
                addElementLocal(poly, theLayer, bucket);
            }

            return true;
        }

        private int checkMinAreaLayerWithTree(GeometryHandler merge, Cell cell, Layer layer)
        {
            // Layer doesn't have min areae
            if (minAreaRule == null && enclosedAreaRule == null && spacingRule == null) return 0;

            Collection<PolyBase.PolyBaseTree> trees = merge.getTreeObjects(layer);
            GenMath.MutableInteger errorFound = new GenMath.MutableInteger(0);

            if (trees.isEmpty())
                System.out.println("Nothing for layer " + layer.getName() + " found.");

            for (PolyBase.PolyBaseTree obj : trees)
            {
                traversePolyTree(layer, obj, 0, minAreaRule, enclosedAreaRule, spacingRule, cell, errorFound);
            }
            return errorFound.intValue();
        }

        private void traversePolyTree(Layer layer, PolyBase.PolyBaseTree obj, int level,
                                      DRCTemplate minAreaRule, DRCTemplate encloseAreaRule,
                                      DRCTemplate spacingRule, Cell cell, GenMath.MutableInteger count)
        {
            List<PolyBase.PolyBaseTree> sons = obj.getSons();
            for (PolyBase.PolyBaseTree son : sons)
            {
                traversePolyTree(layer, son, level+1, minAreaRule, encloseAreaRule, spacingRule, cell, count);
            }
            boolean minAreaCheck = level%2 == 0;
            boolean checkMin = false, checkNotch = false;
            DRC.DRCErrorType errorType = DRC.DRCErrorType.MINAREAERROR;
            double minVal = 0;
            String ruleName = "";

            if (minAreaCheck)
            {
                if (minAreaRule == null) return; // no rule
                minVal = minAreaRule.getValue(0);
                ruleName = minAreaRule.ruleName;
                checkMin = true;
            }
            else
            {
                // odd level checks enclose area and holes (spacing rule)
                errorType = DRC.DRCErrorType.ENCLOSEDAREAERROR;
                if (encloseAreaRule != null)
                {
                    minVal = encloseAreaRule.getValue(0);
                    ruleName = encloseAreaRule.ruleName;
                    checkMin = true;
                }
                checkNotch = (spacingRule != null);
            }
            PolyBase poly = obj.getPoly();

            if (checkMin)
            {
                double area = poly.getArea();
                // isGreaterThan doesn't consider equals condition therefore negate condition is used
                if (!DBMath.isGreaterThan(minVal, area)) return; // larger than the min value
                count.increment();
                DRC.createDRCErrorLogger(errorLogger, null, DRC.DRCCheckMode.ERROR_CHECK_DEFAULT, true,
                        errorType, null, cell, minVal, area, ruleName,
                        poly, null, layer, null, null, null);
            }
            if (checkNotch)
            {
                // Notches are calculated using the bounding box of the polygon -> this is an approximation
                Rectangle2D bnd = poly.getBounds2D();
                if (bnd.getWidth() < spacingRule.getValue(0))
                {
                    count.increment();
                    DRC.createDRCErrorLogger(errorLogger, null, DRC.DRCCheckMode.ERROR_CHECK_DEFAULT, true,
                            DRC.DRCErrorType.NOTCHERROR, "(X axis)", cell, spacingRule.getValue(0), bnd.getWidth(),
                            spacingRule.ruleName, poly, null, layer, null, null, layer);
                }
                if (bnd.getHeight() < spacingRule.getValue(1))
                {
                    count.increment();
                    DRC.createDRCErrorLogger(errorLogger, null, DRC.DRCCheckMode.ERROR_CHECK_DEFAULT, true,
                            DRC.DRCErrorType.NOTCHERROR, "(Y axis)", cell, spacingRule.getValue(1), bnd.getHeight(),
                            spacingRule.ruleName, poly, null, layer, null, null, layer);
                }
            }
        }
    }
}
