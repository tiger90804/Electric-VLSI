package com.sun.electric.database.hierarchy;

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.database.network.JNetwork;

import java.awt.geom.AffineTransform;

/**
 * An Export is a PortProto at the Cell level.  It points to the
 * PortProto that got exported, and the NodeInst for that port.
 * It also allows instant access to the PrimitivePort that grounds
 * out the PortProto chain.
 */
public class Export extends PortProto
{
	// -------------------------- private data ---------------------------
	/** the PortProto that the exported port belongs to */	private PortInst originalPort;
	/** the NodeInst that the exported port belongs to */	private NodeInst originalNode;

	// -------------------- protected and private methods --------------
	protected Export()
	{
		this.userBits = 0;
	}

	/**
	 * Low-level access routine to create a cell in library "lib".
	 */
	public static Export lowLevelAllocate()
	{
		Export pp = new Export();
		return pp;
	}

	/**
	 * Low-level access routine to fill-in the cell name.
	 * Returns true on error.
	 */
	public boolean lowLevelPopulate(Cell parent, NodeInst originalNode, PortInst originalPort, String protoName)
	{
		// initialize the parent object
		this.parent = parent;
		this.protoName = protoName;

		// initialize this object
		this.originalPort = originalPort;
		this.originalNode = originalNode;
		return false;
	}

	/**
	 * Low-level access routine to link a cell into its library.
	 * Returns true on error.
	 */
	public boolean lowLevelLink()
	{
		originalNode.addExport(this);
		setParent(parent);
		return false;
	}

	/** Initialize this Export with a parent (the Cell we're a port of),
	 * the original PortProto we're based on, the NodeInst that
	 * original port belongs to, and an appropriate Network. */
	public static Export newInstance(Cell parent, NodeInst originalNode, PortInst originalPort, String protoName)
	{
		Export pp = lowLevelAllocate();
		if (pp.lowLevelPopulate(parent, originalNode, originalPort, protoName)) return null;
		if (pp.lowLevelLink()) return null;
		return pp;
	}	

	public void remove()
	{
		originalNode.removeExport(this);
		super.remove();
	}

	/** Get the PortProto that was exported to create this Export. */
	public PortInst getOriginalPort() { return originalPort; }

	/** Get the NodeInst that the port returned by getOriginal belongs to */
	public NodeInst getOriginalNode() { return originalNode; }

	/** Get the outline of this Export, relative to some arbitrary
	 * instance of this Export's parent, passed in as a Geometric
	 * object.
	 * @param ni the instance of the port's parent
	 * @return the shape of the port, transformed into the coordinates
	 * of the instance's Cell. */
	public Poly getPoly(NodeInst ni)
	{
		// We just figure out where our basis thinks it is, and ask ni to
		// transform it for us.
		Poly poly = originalPort.getPortProto().getPoly(originalNode);
		AffineTransform af = ni.transformOut();
		poly.transform(af);
		return poly;
	}

	protected void getInfo()
	{
		System.out.println(" Original: " + originalPort);
		System.out.println(" Base: " + getBasePort());
		System.out.println(" Cell: " + parent.describe());
		System.out.println(" Instance: " + originalNode.describe());
		super.getInfo();
	}

	// ----------------------- public methods ----------------------------

	/** Get the base PrimitivePort that generated this Export */
	public PrimitivePort getBasePort()
	{
		PortProto pp = originalPort.getPortProto();
		return pp.getBasePort();
	}

	/** Get the PortInst exported by this Export */
	public PortInst getPortInst()
	{
		return originalPort;
	}

	public JNetwork getNetwork()
	{
		return getPortInst().getNetwork();
	}

	/** If this PortProto belongs to an Icon View Cell then return the
	 * PortProto with the same name on the corresponding Schematic View
	 * Cell.
	 *
	 * <p> If this PortProto doesn't belong to an Icon View Cell then
	 * return this PortProto.
	 *
	 * <p> If the Icon View Cell has no corresponding Schematic View
	 * Cell then return null. If the corresponding Schematic View Cell
	 * has no port with the same name then return null.
	 *
	 * <p> If there are multiple versions of the Schematic Cell return
	 * the latest. */
	public PortProto getEquivalent()
	{
		NodeProto equiv = parent.getEquivalent();
		if (equiv == parent)
			return this;
		if (equiv == null)
			return null;
		return equiv.findPortProto(protoName);
	}

	public String toString()
	{
		return "Export " + protoName;
	}

	// Return the first internal connection that exists on the port this
	// Export was exported from, or null if no such Connection exists.
//	Connection findFirstConnection() {
//	  Iterator i= owner.getConnections();
//	  while (i.hasNext()) {
//	    Connection c= (Connection)i.next();
//	    if (c.getPort()== originalPort)  return c;
//	  }
//	  return null;
//	}
}
