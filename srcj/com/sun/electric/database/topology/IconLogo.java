package com.sun.electric.database.topology;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Jul 25, 2005
 * Time: 5:43:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class IconLogo extends NodeInst
{
    /**
	 * The private constructor of NodeInst. Use the factory "newInstance" instead.
	 * @param parent the Cell in which this NodeInst will reside.
	 * @param protoType the NodeProto of which this is an instance.
	 * @param name name of new NodeInst
	 * @param duplicate duplicate index of this NodeInst
     * @param nameDescriptor TextDescriptor of name of this NodeInst
	 * @param center the center location of this NodeInst.
	 * @param width the width of this NodeInst.
	 * If negative, flip the X coordinate (or flip ABOUT the Y axis).
	 * @param height the height of this NodeInst.
	 * If negative, flip the Y coordinate (or flip ABOUT the X axis).
	 * @param angle the angle of this NodeInst (in tenth-degrees).
	 * @param userBits flag bits of this NodeInst.
     * @param protoDescriptor TextDescriptor of prototype name of this NodeInst
	 */
    protected IconLogo(Cell parent, NodeProto protoType,
            Name name, int duplicate, ImmutableTextDescriptor nameDescriptor,
            Point2D center, double width, double height, int angle,
            int userBits, ImmutableTextDescriptor protoDescriptor)
	{
        super(parent, protoType, name, duplicate, nameDescriptor, center, width, height,
              angle, userBits, protoDescriptor);
    }

    public synchronized Iterator getVariables()
    {
        System.out.println("Overwrite getVariables");
        return (new ArrayList().iterator());
    }
    public synchronized int getNumVariables() {return 0;}

    public Variable getVar(Variable.Key key, Class type) { return null;}

    public Variable newVar(Variable.Key key, Object value, TextDescriptor td) { return null;}

    public void lowLevelUnlinkVar(Variable var)
    {
        System.out.println("Overwrite lowLevelUnlinkVar");
    }
    public void lowLevelLinkVar(Variable var)
    {
        System.out.println("Overwrite lowLevelLinkVar");
    }
    public void setVar(Variable.Key key, Object value, int index)
    {
        System.out.println("Overwrite setVar");
    }
}
