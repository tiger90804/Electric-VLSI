/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: nMOS.java
 * nmos technology description
 * Generated automatically from a library
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.technology.technologies;

import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;

import java.awt.Point;
import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * This is the n-channel MOS (from Mead & Conway) Technology.
 */
public class nMOS extends Technology
{
	/** the n-channel MOS (from Mead & Conway) Technology object. */	public static final nMOS tech = new nMOS();

/** defines the 1st transparent layer. */			private static final int TRANSPARENT_1 = 1;
/** defines the 2nd transparent layer. */			private static final int TRANSPARENT_2 = 2;
/** defines the 3rd transparent layer. */			private static final int TRANSPARENT_3 = 3;
/** defines the 4th transparent layer. */			private static final int TRANSPARENT_4 = 4;
/** defines the 5th transparent layer. */			private static final int TRANSPARENT_5 = 5;

	// -------------------- private and protected methods ------------------------
	private nMOS()
	{
		setTechName("nmos");
		setTechDesc("n-channel MOS (from Mead & Conway)");
		setScale(2000);   // in nanometers: really 2 microns
		setNoNegatedArcs();
		setStaticTechnology();
		setColorMap(new Color []
		{
			new Color(200,200,200), //  0:      +           +         +       +              
			new Color(  0,  0,200), //  1: Metal+           +         +       +              
			new Color(220,  0,120), //  2:      +Polysilicon+         +       +              
			new Color( 80,  0,160), //  3: Metal+Polysilicon+         +       +              
			new Color( 70,250, 70), //  4:      +           +Diffusion+       +              
			new Color(  0,140,140), //  5: Metal+           +Diffusion+       +              
			new Color(180,130,  0), //  6:      +Polysilicon+Diffusion+       +              
			new Color( 55, 70,140), //  7: Metal+Polysilicon+Diffusion+       +              
			new Color(250,250,  0), //  8:      +           +         +Implant+              
			new Color( 85,105,160), //  9: Metal+           +         +Implant+              
			new Color(190, 80,100), // 10:      +Polysilicon+         +Implant+              
			new Color( 70, 50,150), // 11: Metal+Polysilicon+         +Implant+              
			new Color( 80,210,  0), // 12:      +           +Diffusion+Implant+              
			new Color( 50,105,130), // 13: Metal+           +Diffusion+Implant+              
			new Color(170,110,  0), // 14:      +Polysilicon+Diffusion+Implant+              
			new Color( 60, 60,130), // 15: Metal+Polysilicon+Diffusion+Implant+              
			new Color(180,180,180), // 16:      +           +         +       +Buried-Contact
			new Color(  0,  0,180), // 17: Metal+           +         +       +Buried-Contact
			new Color(200,  0,100), // 18:      +Polysilicon+         +       +Buried-Contact
			new Color( 60,  0,140), // 19: Metal+Polysilicon+         +       +Buried-Contact
			new Color( 50,230, 50), // 20:      +           +Diffusion+       +Buried-Contact
			new Color(  0,120,120), // 21: Metal+           +Diffusion+       +Buried-Contact
			new Color(160,110,  0), // 22:      +Polysilicon+Diffusion+       +Buried-Contact
			new Color( 35, 50,120), // 23: Metal+Polysilicon+Diffusion+       +Buried-Contact
			new Color(230,230,  0), // 24:      +           +         +Implant+Buried-Contact
			new Color( 65, 85,140), // 25: Metal+           +         +Implant+Buried-Contact
			new Color(170, 60, 80), // 26:      +Polysilicon+         +Implant+Buried-Contact
			new Color( 50, 30,130), // 27: Metal+Polysilicon+         +Implant+Buried-Contact
			new Color( 60,190,  0), // 28:      +           +Diffusion+Implant+Buried-Contact
			new Color( 30, 85,110), // 29: Metal+           +Diffusion+Implant+Buried-Contact
			new Color(150, 90,  0), // 30:      +Polysilicon+Diffusion+Implant+Buried-Contact
			new Color( 40, 40,110), // 31: Metal+Polysilicon+Diffusion+Implant+Buried-Contact
		});

		//**************************************** LAYERS ****************************************

		/** M layer */
		Layer M_lay = Layer.newInstance(this, "Metal",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_1, 70,250,70,0.8,1,
			new int[] { 0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888}));// X   X   X   X   

		/** P layer */
		Layer P_lay = Layer.newInstance(this, "Polysilicon",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_2, 250,250,0,0.8,1,
			new int[] { 0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222}));//   X   X   X   X 

		/** D layer */
		Layer D_lay = Layer.newInstance(this, "Diffusion",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_3, 180,180,180,0.8,1,
			new int[] { 0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111}));//    X   X   X   X

		/** I layer */
		Layer I_lay = Layer.newInstance(this, "Implant",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_4, 0,0,0,0.8,1,
			new int[] { 0x0000,   //                 
						0x0000,   //                 
						0x1111,   //    X   X   X   X
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1111,   //    X   X   X   X
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1111,   //    X   X   X   X
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1111,   //    X   X   X   X
						0x0000}));//                 

		/** CC layer */
		Layer CC_lay = Layer.newInstance(this, "Contact-Cut",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 180,130,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** BC layer */
		Layer BC_lay = Layer.newInstance(this, "Buried-Contact",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_5, 0,0,0,0.8,1,
			new int[] { 0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888}));// X   X   X   X   

		/** O layer */
		Layer O_lay = Layer.newInstance(this, "Overglass",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] { 0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x5555,   //  X X X X X X X X
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x5555,   //  X X X X X X X X
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x5555,   //  X X X X X X X X
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x5555,   //  X X X X X X X X
						0x2222}));//   X   X   X   X 

		/** LI layer */
		Layer LI_lay = Layer.newInstance(this, "Light-Implant",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 150,90,0,0.8,1,
			new int[] { 0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** OC layer */
		Layer OC_lay = Layer.newInstance(this, "Oversize-Contact",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** HE layer */
		Layer HE_lay = Layer.newInstance(this, "Hard-Enhancement",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] { 0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808,   //     X       X   
						0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808}));//     X       X   

		/** LE layer */
		Layer LE_lay = Layer.newInstance(this, "Light-Enhancement",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] { 0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020}));//   X       X     

		/** T layer */
		Layer T_lay = Layer.newInstance(this, "Transistor",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 200,200,200,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PM layer */
		Layer PM_lay = Layer.newInstance(this, "Pseudo-Metal",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_1, 70,250,70,0.8,1,
			new int[] { 0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888}));// X   X   X   X   

		/** PP layer */
		Layer PP_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_2, 250,250,0,0.8,1,
			new int[] { 0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222}));//   X   X   X   X 

		/** PD layer */
		Layer PD_lay = Layer.newInstance(this, "Pseudo-Diffusion",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_3, 180,180,180,0.8,1,
			new int[] { 0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111}));//    X   X   X   X

		// The layer functions
		M_lay.setFunction(Layer.Function.METAL1);		// Metal
		P_lay.setFunction(Layer.Function.POLY1);		// Polysilicon
		D_lay.setFunction(Layer.Function.DIFF);		// Diffusion
		I_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.DEPLETION|Layer.Function.HEAVY);		// Implant
		CC_lay.setFunction(Layer.Function.CONTACT1);		// Contact-Cut
		BC_lay.setFunction(Layer.Function.IMPLANT);		// Buried-Contact
		O_lay.setFunction(Layer.Function.OVERGLASS);		// Overglass
		LI_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.DEPLETION|Layer.Function.LIGHT);		// Light-Implant
		OC_lay.setFunction(Layer.Function.CONTACT3);		// Oversize-Contact
		HE_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.ENHANCEMENT|Layer.Function.HEAVY);		// Hard-Enhancement
		LE_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.ENHANCEMENT|Layer.Function.LIGHT);		// Light-Enhancement
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);		// Transistor
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal
		PP_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon
		PD_lay.setFunction(Layer.Function.DIFF, Layer.Function.PSEUDO);		// Pseudo-Diffusion

		// The CIF names
		M_lay.setCIFLayer("NM");		// Metal
		P_lay.setCIFLayer("NP");		// Polysilicon
		D_lay.setCIFLayer("ND");		// Diffusion
		I_lay.setCIFLayer("NI");		// Implant
		CC_lay.setCIFLayer("NC");		// Contact-Cut
		BC_lay.setCIFLayer("NB");		// Buried-Contact
		O_lay.setCIFLayer("NG");		// Overglass
		LI_lay.setCIFLayer("NJ");		// Light-Implant
		OC_lay.setCIFLayer("NO");		// Oversize-Contact
		HE_lay.setCIFLayer("NE");		// Hard-Enhancement
		LE_lay.setCIFLayer("NF");		// Light-Enhancement
		T_lay.setCIFLayer("");		// Transistor
		PM_lay.setCIFLayer("");		// Pseudo-Metal
		PP_lay.setCIFLayer("");		// Pseudo-Polysilicon
		PD_lay.setCIFLayer("");		// Pseudo-Diffusion

		// The DXF names
		M_lay.setDXFLayer("");		// Metal
		P_lay.setDXFLayer("");		// Polysilicon
		D_lay.setDXFLayer("");		// Diffusion
		I_lay.setDXFLayer("");		// Implant
		CC_lay.setDXFLayer("");		// Contact-Cut
		BC_lay.setDXFLayer("");		// Buried-Contact
		O_lay.setDXFLayer("");		// Overglass
		LI_lay.setDXFLayer("");		// Light-Implant
		OC_lay.setDXFLayer("");		// Oversize-Contact
		HE_lay.setDXFLayer("");		// Hard-Enhancement
		LE_lay.setDXFLayer("");		// Light-Enhancement
		T_lay.setDXFLayer("");		// Transistor
		PM_lay.setDXFLayer("");		// Pseudo-Metal
		PP_lay.setDXFLayer("");		// Pseudo-Polysilicon
		PD_lay.setDXFLayer("");		// Pseudo-Diffusion

		// The GDS names
		M_lay.setGDSLayer("");		// Metal
		P_lay.setGDSLayer("");		// Polysilicon
		D_lay.setGDSLayer("");		// Diffusion
		I_lay.setGDSLayer("");		// Implant
		CC_lay.setGDSLayer("");		// Contact-Cut
		BC_lay.setGDSLayer("");		// Buried-Contact
		O_lay.setGDSLayer("");		// Overglass
		LI_lay.setGDSLayer("");		// Light-Implant
		OC_lay.setGDSLayer("");		// Oversize-Contact
		HE_lay.setGDSLayer("");		// Hard-Enhancement
		LE_lay.setGDSLayer("");		// Light-Enhancement
		T_lay.setGDSLayer("");		// Transistor
		PM_lay.setGDSLayer("");		// Pseudo-Metal
		PP_lay.setGDSLayer("");		// Pseudo-Polysilicon
		PD_lay.setGDSLayer("");		// Pseudo-Diffusion

		// The SPICE information
		M_lay.setDefaultParasitics(0.03f, 0.03f, 0);		// Metal
		P_lay.setDefaultParasitics(50.0f, 0.04f, 0);		// Polysilicon
		D_lay.setDefaultParasitics(10.0f, 0.1f, 0);		// Diffusion
		I_lay.setDefaultParasitics(0, 0, 0);		// Implant
		CC_lay.setDefaultParasitics(0, 0, 0);		// Contact-Cut
		BC_lay.setDefaultParasitics(0, 0, 0);		// Buried-Contact
		O_lay.setDefaultParasitics(0, 0, 0);		// Overglass
		LI_lay.setDefaultParasitics(0, 0, 0);		// Light-Implant
		OC_lay.setDefaultParasitics(0, 0, 0);		// Oversize-Contact
		HE_lay.setDefaultParasitics(0, 0, 0);		// Hard-Enhancement
		LE_lay.setDefaultParasitics(0, 0, 0);		// Light-Enhancement
		T_lay.setDefaultParasitics(0, 0, 0);		// Transistor
		PM_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Metal
		PP_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Polysilicon
		PD_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Diffusion
		setDefaultParasitics(50, 50);

		//******************** ARCS ********************

		/** Metal arc */
		PrimitiveArc Metal_arc = PrimitiveArc.newInstance(this, "Metal", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M_lay, 0, Poly.Type.FILLED)
		});
		Metal_arc.setFunction(PrimitiveArc.Function.METAL1);
		Metal_arc.setFixedAngle();
		Metal_arc.setWipable();
		Metal_arc.setAngleIncrement(90);

		/** Polysilicon arc */
		PrimitiveArc Polysilicon_arc = PrimitiveArc.newInstance(this, "Polysilicon", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_arc.setFunction(PrimitiveArc.Function.POLY1);
		Polysilicon_arc.setFixedAngle();
		Polysilicon_arc.setWipable();
		Polysilicon_arc.setAngleIncrement(90);

		/** Diffusion arc */
		PrimitiveArc Diffusion_arc = PrimitiveArc.newInstance(this, "Diffusion", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(D_lay, 0, Poly.Type.FILLED)
		});
		Diffusion_arc.setFunction(PrimitiveArc.Function.DIFF);
		Diffusion_arc.setFixedAngle();
		Diffusion_arc.setWipable();
		Diffusion_arc.setAngleIncrement(90);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromTop(3)),
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromTop(3)),
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromBottom(1)),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromTop(3)),
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromTop(3)),
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(3)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(3)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromBottom(1)),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeTopEdge()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeTopEdge()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(1)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5)),
			new Technology.TechPoint(EdgeH.fromRight(1.5), EdgeV.fromTop(1.5)),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_13 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_14 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_15 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_16 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_17 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_18 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_19 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};
		Technology.TechPoint [] box_20 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_21 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5)),
			new Technology.TechPoint(EdgeH.fromRight(0.5), EdgeV.fromTop(0.5)),
		};
		Technology.TechPoint [] box_22 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};

		//******************** NODES ********************

		/** Metal-Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("Metal-Pin", this, 4, 4, new SizeOffset(0.5, 0.5, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_21)
			});
		mp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_arc}, "metal", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		mp_node.setFunction(NodeProto.Function.PIN);
		mp_node.setArcsWipe();
		mp_node.setArcsShrink();

		/** Polysilicon-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Polysilicon-Pin", this, 4, 4, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_20)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		pp_node.setFunction(NodeProto.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** Diffusion-Pin */
		PrimitiveNode dp_node = PrimitiveNode.newInstance("Diffusion-Pin", this, 4, 4, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_20)
			});
		dp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dp_node, new ArcProto [] {Diffusion_arc}, "diffusion", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		dp_node.setFunction(NodeProto.Function.PIN);
		dp_node.setArcsWipe();
		dp_node.setArcsShrink();

		/** Metal-Polysilicon-Con */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("Metal-Polysilicon-Con", this, 6, 6, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_19)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Polysilicon_arc, Metal_arc}, "metal-poly", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		mpc_node.setFunction(NodeProto.Function.CONTACT);
		mpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc_node.setSpecialValues(new double [] {2, 2, 1, 2});

		/** Metal-Diffusion-Con */
		PrimitiveNode mdc_node = PrimitiveNode.newInstance("Metal-Diffusion-Con", this, 6, 6, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_19)
			});
		mdc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdc_node, new ArcProto [] {Diffusion_arc, Metal_arc}, "metal-diff", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		mdc_node.setFunction(NodeProto.Function.CONTACT);
		mdc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mdc_node.setSpecialValues(new double [] {2, 2, 1, 2});

		/** Butting-Con */
		PrimitiveNode bc_node = PrimitiveNode.newInstance("Butting-Con", this, 8, 6, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_18),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_17),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_22)
			});
		bc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bc_node, new ArcProto [] {Diffusion_arc, Metal_arc}, "but-diff", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromLeft(4), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bc_node, new ArcProto [] {Polysilicon_arc, Metal_arc}, "but-poly", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(3), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		bc_node.setFunction(NodeProto.Function.CONNECT);

		/** Buried-Con-Cross */
		PrimitiveNode bcc_node = PrimitiveNode.newInstance("Buried-Con-Cross", this, 8, 6, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_16),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15)
			});
		bcc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcc_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-bottom", 270,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(4), EdgeV.fromBottom(2)),
				PrimitivePort.newInstance(this, bcc_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(3), EdgeH.fromLeft(2), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, bcc_node, new ArcProto [] {Diffusion_arc}, "bur-diff-right", 0,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(2), EdgeV.fromBottom(3), EdgeH.fromRight(2), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, bcc_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(4), EdgeV.fromTop(2))
			});
		bcc_node.setFunction(NodeProto.Function.CONNECT);

		/** Transistor */
		PrimitiveNode t_node = PrimitiveNode.newInstance("Transistor", this, 8, 8, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_16, 3, 3, 0, 0),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14, 1, 1, 2, 2)
			});
		t_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Polysilicon_arc}, "trans-poly-left", 180,85, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(4), EdgeH.fromLeft(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Diffusion_arc}, "trans-diff-top", 90,85, 3, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(4), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Polysilicon_arc}, "trans-poly-right", 0,85, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(2), EdgeV.fromBottom(4), EdgeH.fromRight(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Diffusion_arc}, "trans-diff-bottom", 270,85, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(4), EdgeV.fromBottom(2))
			});
		t_node.setFunction(NodeProto.Function.TRANMOS);
		t_node.setHoldsOutline();
		t_node.setCanShrink();
		t_node.setSpecialType(PrimitiveNode.SERPTRANS);
		t_node.setSpecialValues(new double [] {0.025, 1, 1, 2, 1, 1});

		/** Implant-Transistor */
		PrimitiveNode it_node = PrimitiveNode.newInstance("Implant-Transistor", this, 8, 8, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14, 1, 1, 2, 2),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_16, 3, 3, 0, 0),
				new Technology.NodeLayer(I_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_11, 2.5, 2.5, 1.5, 1.5)
			});
		it_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, it_node, new ArcProto [] {Polysilicon_arc}, "imp-trans-poly-left", 180,85, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(4), EdgeH.fromLeft(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, it_node, new ArcProto [] {Diffusion_arc}, "imp-trans-diff-top", 90,85, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(4), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, it_node, new ArcProto [] {Polysilicon_arc}, "imp-trans-poly-right", 0,85, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(2), EdgeV.fromBottom(4), EdgeH.fromRight(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, it_node, new ArcProto [] {Diffusion_arc}, "imp-trans-diff-bottom", 270,85, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(4), EdgeV.fromBottom(2))
			});
		it_node.setFunction(NodeProto.Function.TRADMOS);
		it_node.setHoldsOutline();
		it_node.setCanShrink();
		it_node.setSpecialType(PrimitiveNode.SERPTRANS);
		it_node.setSpecialValues(new double [] {0.0333333, 1, 1, 2, 1, 1});

		/** Buried-Con-Cross-S */
		PrimitiveNode bccs_node = PrimitiveNode.newInstance("Buried-Con-Cross-S", this, 6, 6, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		bccs_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bccs_node, new ArcProto [] {Diffusion_arc, Polysilicon_arc}, "bur-end-right", 0,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(3), EdgeH.fromRight(2), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, bccs_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(2), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bccs_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-bottom", 270,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromBottom(2)),
				PrimitivePort.newInstance(this, bccs_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(3), EdgeH.fromLeft(2), EdgeV.fromTop(3))
			});
		bccs_node.setFunction(NodeProto.Function.CONNECT);

		/** Buried-Con-Cross-T */
		PrimitiveNode bcct_node = PrimitiveNode.newInstance("Buried-Con-Cross-T", this, 6, 6, new SizeOffset(1, 1, 0, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_8),
				new Technology.NodeLayer(P_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10)
			});
		bcct_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcct_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(3), EdgeH.fromLeft(2), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, bcct_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(2), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcct_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-bottom", 270,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromBottom(2))
			});
		bcct_node.setFunction(NodeProto.Function.CONNECT);

		/** Buried-Con-Polysurr */
		PrimitiveNode bcp_node = PrimitiveNode.newInstance("Buried-Con-Polysurr", this, 7, 6, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		bcp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcp_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-1", 0,135, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcp_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-2", 0,135, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcp_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(3), EdgeH.fromLeft(2), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, bcp_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-3", 0,135, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		bcp_node.setFunction(NodeProto.Function.CONNECT);

		/** Buried-Con-Diffsurr-I */
		PrimitiveNode bcdi_node = PrimitiveNode.newInstance("Buried-Con-Diffsurr-I", this, 7, 4, new SizeOffset(1, 0, 1, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_6),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_5)
			});
		bcdi_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcdi_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-right", 0,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcdi_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromLeft(2), EdgeV.fromTop(2))
			});
		bcdi_node.setFunction(NodeProto.Function.CONNECT);

		/** Buried-Con-Diffsurr-T */
		PrimitiveNode bcdt_node = PrimitiveNode.newInstance("Buried-Con-Diffsurr-T", this, 8, 6, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_4),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_3)
			});
		bcdt_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcdt_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(4), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcdt_node, new ArcProto [] {Diffusion_arc}, "bur-diff-right", 0,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, bcdt_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromLeft(2), EdgeV.fromTop(4))
			});
		bcdt_node.setFunction(NodeProto.Function.CONNECT);

		/** Buried-Con-Diffsurr-L */
		PrimitiveNode bcdl_node = PrimitiveNode.newInstance("Buried-Con-Diffsurr-L", this, 6, 6, new SizeOffset(1, 1, 0, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_2),
				new Technology.NodeLayer(P_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_1),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_8)
			});
		bcdl_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcdl_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromLeft(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, bcdl_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		bcdl_node.setFunction(NodeProto.Function.CONNECT);

		/** Metal-Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal-Node", this, 4, 4, new SizeOffset(0.5, 0.5, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_21)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_arc}, "metal", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		mn_node.setFunction(NodeProto.Function.NODE);
		mn_node.setHoldsOutline();
		mn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Polysilicon-Node", this, 4, 4, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		pn_node.setFunction(NodeProto.Function.NODE);
		pn_node.setHoldsOutline();
		pn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Diffusion-Node */
		PrimitiveNode dn_node = PrimitiveNode.newInstance("Diffusion-Node", this, 4, 4, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		dn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dn_node, new ArcProto [] {Diffusion_arc}, "diffusion", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		dn_node.setFunction(NodeProto.Function.NODE);
		dn_node.setHoldsOutline();
		dn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Implant-Node */
		PrimitiveNode in_node = PrimitiveNode.newInstance("Implant-Node", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(I_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22)
			});
		in_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, in_node, new ArcProto [] {}, "implant", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		in_node.setFunction(NodeProto.Function.NODE);
		in_node.setHoldsOutline();
		in_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Cut-Node */
		PrimitiveNode cn_node = PrimitiveNode.newInstance("Cut-Node", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_22)
			});
		cn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cn_node, new ArcProto [] {}, "cut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		cn_node.setFunction(NodeProto.Function.NODE);
		cn_node.setHoldsOutline();
		cn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Buried-Node */
		PrimitiveNode bn_node = PrimitiveNode.newInstance("Buried-Node", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22)
			});
		bn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bn_node, new ArcProto [] {}, "buried", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		bn_node.setFunction(NodeProto.Function.NODE);
		bn_node.setHoldsOutline();
		bn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Overglass-Node */
		PrimitiveNode on_node = PrimitiveNode.newInstance("Overglass-Node", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(O_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22)
			});
		on_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, on_node, new ArcProto [] {}, "overglass", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		on_node.setFunction(NodeProto.Function.NODE);
		on_node.setHoldsOutline();
		on_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Light-Implant-Node */
		PrimitiveNode lin_node = PrimitiveNode.newInstance("Light-Implant-Node", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(LI_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22)
			});
		lin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, lin_node, new ArcProto [] {}, "light-implant", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		lin_node.setFunction(NodeProto.Function.NODE);
		lin_node.setHoldsOutline();
		lin_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Oversize-Cut-Node */
		PrimitiveNode ocn_node = PrimitiveNode.newInstance("Oversize-Cut-Node", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(OC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_22)
			});
		ocn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ocn_node, new ArcProto [] {}, "oversize-contact", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		ocn_node.setFunction(NodeProto.Function.NODE);
		ocn_node.setHoldsOutline();
		ocn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Hard-Enhancement-Node */
		PrimitiveNode hen_node = PrimitiveNode.newInstance("Hard-Enhancement-Node", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(HE_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22)
			});
		hen_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, hen_node, new ArcProto [] {}, "hard-enhancement", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		hen_node.setFunction(NodeProto.Function.NODE);
		hen_node.setHoldsOutline();
		hen_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Light-Enhancement-Node */
		PrimitiveNode len_node = PrimitiveNode.newInstance("Light-Enhancement-Node", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(LE_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22)
			});
		len_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, len_node, new ArcProto [] {}, "light-enhancement", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		len_node.setFunction(NodeProto.Function.NODE);
		len_node.setHoldsOutline();
		len_node.setSpecialType(PrimitiveNode.POLYGONAL);
	};
}
