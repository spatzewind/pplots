package jplots.layer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jplots.JPlot;
import jplots.axes.JAxis;
import jplots.maths.AffineBuilder;
import jplots.maths.JDEdge;
import jplots.maths.JDLine;
import jplots.maths.JDPoint;
import jplots.maths.JDQuad;
import jplots.maths.JPlotMath;
import jplots.shapes.JEllipseShape;
import jplots.shapes.JGroupShape;
import jplots.shapes.JLineShape;
import jplots.shapes.JPlotShape;
import jplots.shapes.JPolygonShape;
import processing.core.PGraphics;

public class JHatchLayer2D extends JPlotsLayer {
	
	private boolean input2d;
	private double Xin, Xax, Yin, Yax, density;
	private double[] xarrayx, yarrayy;
	private double[][] xarrayx2, yarrayy2, zarrayz;
	private String pattern;
	private JDPoint[][] points;
	private JDQuad[] quads;
	private Comparator<JDEdge> comparator;
	
	public JHatchLayer2D(float[] x, float[] y, float[][] x2, float[][] y2, float[][] z, float stroke_weight, float lowerBound, float upperBound, String pattern) {
		input2d = true;
		if(x!=null || y!=null) input2d = false;
		else if(x2==null || y2==null)
			throw new IllegalArgumentException("Either 1D or 2D coordinate arrays x,y have to be given, but all are null");
		if (!input2d) {
			xarrayx = new double[x.length];
			for(int i=0; i<x.length; i++) xarrayx[i] = x[i];
			yarrayy = new double[y.length];
			for(int i=0; i<y.length; i++) yarrayy[i] = y[i];
			xarrayx2 = null;
			yarrayy2 = null;
			minX = JPlotMath.dmin(xarrayx);
			maxX = JPlotMath.dmax(xarrayx);
			minY = JPlotMath.dmin(yarrayy);
			maxY = JPlotMath.dmax(yarrayy);
		} else {
			xarrayx = null;
			yarrayy = null;
			xarrayx2 = new double[x2.length][x2[0].length];
			for(int j=0; j<x2.length; j++)
				for(int i=0; i<x2[j].length; i++)
					xarrayx2[j][i] = x2[j][i];
			yarrayy2 = new double[y2.length][y2[0].length];
			for(int j=0; j<y2.length; j++)
				for(int i=0; i<y2[j].length; i++)
					yarrayy2[j][i] = y2[j][i];
			minX = JPlotMath.dmin(xarrayx2);
			maxX = JPlotMath.dmax(xarrayx2);
			minY = JPlotMath.dmin(yarrayy2);
			maxY = JPlotMath.dmax(yarrayy2);
		}
		zarrayz = new double[z.length][z[0].length];
		for(int j=0; j<z.length; j++)
			for(int i=0; i<z[j].length; i++)
				zarrayz[j][i] = z[j][i];
		minZ = lowerBound;
		maxZ = upperBound;
		if(Double.isNaN(upperBound)) maxZ = 1d + JPlotMath.fmax(z);
		lw = stroke_weight;
		this.pattern = pattern;
		init();
	}
	
	public JHatchLayer2D(double[] x, double[] y, double[][] x2, double[][] y2, double[][] z, double stroke_weight, double lowerBound, double upperBound, String pattern) {
		input2d = true;
		if(x!=null || y!=null) input2d = false;
		else if(x2==null || y2==null)
			throw new IllegalArgumentException("Either 1D or 2D coordinate arrays x,y have to be given, but all are null");
		if (!input2d) {
			xarrayx = x;
			yarrayy = y;
			xarrayx2 = null;
			yarrayy2 = null;
			minX = JPlotMath.dmin(xarrayx);
			maxX = JPlotMath.dmax(xarrayx);
			minY = JPlotMath.dmin(yarrayy);
			maxY = JPlotMath.dmax(yarrayy);
		} else {
			xarrayx = null;
			yarrayy = null;
			xarrayx2 = x2;
			yarrayy2 = y2;
			minX = JPlotMath.dmin(xarrayx2);
			maxX = JPlotMath.dmax(xarrayx2);
			minY = JPlotMath.dmin(yarrayy2);
			maxY = JPlotMath.dmax(yarrayy2);
		}
		zarrayz = z;
		minZ = lowerBound;
		maxZ = upperBound;
		if(Double.isNaN(upperBound)) maxZ = 1d + JPlotMath.dmax(z);
		lw = (float) stroke_weight;
		this.pattern = pattern;
		init();
	}
	
	@Override
	public void createRasterImg(JPlot plot, PGraphics g) {
	}
	
	@Override
	public void createVectorImg(JAxis ax, int layernum, JGroupShape s) {
//		System.out.println("Do hatching!");
		int[] q = ax.getSize();
		Xin = ax.isXlogAxis() ? Math.log10(minX) : minX;
		Xax = ax.isXlogAxis() ? Math.log10(maxX) : maxX;
		Yin = ax.isYlogAxis() ? Math.log10(minY) : minY;
		Yax = ax.isYlogAxis() ? Math.log10(maxY) : maxY;
		double xs = q[2] / (Xax - Xin), ys = q[3] / (Yax - Yin);
		// double tol = Math.max(Math.abs(maxX-minX), Math.abs(maxY-minY)) * 1.0e-12d;

		// step 1: collect valid corners of grid and project them
		collectValidPoints(ax, ax.getPlot().isDebug());
		AffineBuilder affine = new AffineBuilder().scale(invertAxisX ? -1d : 1d, invertAxisY ? 1d : -1d)
				.translate(invertAxisX ? maxX : -minX, invertAxisY ? -minY : maxY).scale(xs, ys).translate(q[0], q[1]);
		for(JDPoint[] parr: points)
			for(JDPoint pt: parr)
				pt.affine(affine.getMatrix());
		
		points2quads();
		
		// step 4: create filling between contours if wished
		int[] codes = patternCodes(pattern);
		int lineCode = codes[0];
		int pointCode = codes[1];
		
		if(lineCode<1 && pointCode<1) {
			System.err.println("Cannot recognize patter '"+pattern+"'");
			return;
		}
		JPlot parent = ax.getPlot();
		int[] p = parent.getSize();
		double pdist = Math.min((double)p[0]/(double)parent.getNumColumns(), (double)p[1]/(double)parent.getNumRows()) / (10d*density);
		if(lineCode>0) drawLines(quads, lineCode, s, ax, pdist);
		if(pointCode>0) drawMarkers(quads, pointCode, s, ax, pdist);
	}
	
	//* **************************************** *
	//* ********** GETTER AND SETTER  ********** *
	//* **************************************** *
	
	
	public double getDensity() {
		return density;
	}
	public void setDensity(double d) {
		density = d;
	}
	
	public String getPattern() {
		return pattern;
	}
	
	public double[] getZRange() {
		return new double[] { minZ, maxZ };
	}
	
	
	
	
	//* **************************************** *
	//* ********** PUBLIC METHODS     ********** *
	//* **************************************** *
	
	public void drawLines(JDQuad[] quads, int lineCode, JGroupShape s, JAxis ax, double pdist) {
		int[] p = ax.getSize();
		
		boolean dblDens = (lineCode & 0x10) != 0;
		float pd = (float) (dblDens ? 0.5d*pdist : pdist);
		JPlotShape.stroke(lc); JPlotShape.strokeWeight(Math.min(0.15f*pd, 2f*(float)lw));
		List<JDEdge> lines = new ArrayList<>();
		
		float lwf = Math.min(0.3f*pd, (float)lw);
		if((lineCode&0x01)==1) {
			lines.clear();
			int st = (int) (p[0]/pd), en = (int) ((p[0]+p[2])/pd+1);
			for(int i=st; i<=en; i++) {
				if(i*pd<p[0]) continue;
				if(i*pd>p[0]+p[2]) continue;
				JDPoint top = new JDPoint(i*pd, p[1]);
				JDPoint bottom = new JDPoint(i*pd, p[1]+p[3]);
				for(JDQuad qu: quads) {
					JDLine[] ls = qu.getLevelRangeLines(top, bottom, minZ, maxZ);
					if(ls==null) continue;
					lines.addAll(ls[0].toEdges());
					if(ls.length>1) lines.addAll(ls[1].toEdges());
				}
			}
			lineCombination(lines);
			lineCombination(lines);
			for(JDEdge l: lines) {
//				JPlotShape.stroke(l.b.y>l.a.y?0xff00ff00:0xffff0000);
				s.addChild(new JLineShape(lwf, lc, (float)l.a.x, (float)l.a.y, (float)l.b.x, (float)l.b.y));
			}
		}

		if((lineCode&0x02)==2) {
			lines.clear();
			int st = (int) (p[1]/pd), en = (int) ((p[1]+p[3])/pd+1);
			for(int i=st; i<=en; i++) {
				if(i*pd<p[1]) continue;
				if(i*pd>p[1]+p[3]) continue;
				JDPoint left  = new JDPoint(p[0], i*pd);
				JDPoint right = new JDPoint(p[0]+p[2], i*pd);
				for(JDQuad qu: quads) {
					JDLine[] ls = qu.getLevelRangeLines(left, right, minZ, maxZ);
					if(ls==null) continue;
					lines.addAll(ls[0].toEdges());
					if(ls.length>1) lines.addAll(ls[1].toEdges());
				}
			}
			lineCombination(lines);
			lineCombination(lines);
			for(JDEdge l: lines) {
//				JPlotShape.stroke(l.b.x>l.a.x?0xff00ff00:0xffff0000);
				s.addChild(new JLineShape(lwf, lc, (float)l.a.x, (float)l.a.y, (float)l.b.x, (float)l.b.y));
			}
		}
		
		//pd *= (float) Math.sqrt(2d);
		
		if((lineCode&0x04)==4) {
			lines.clear();
			double[][] revymat = { { 1d, 0d, 0d }, { 0d, -1d, 0d } };
			int o = p[0]+p[1];
			int st = (int) (o/pd), en = (int) ((o+p[2]+p[3])/pd+1);
			for(int i=st; i<=en; i++) {
				float m = i*pd - o;
				if(m<0) continue;
				if(m>p[2]+p[3]) continue;
				JDPoint topRight = new JDPoint(p[0]+Math.min(m, p[2]), p[1]+Math.max(m-p[2], 0));
				JDPoint leftBott = new JDPoint(p[0]+Math.max(m-p[3], 0), p[1]+Math.min(m, p[3]));
				for(JDQuad qu: quads) {
					JDLine[] ls = qu.getLevelRangeLines(topRight, leftBott, minZ, maxZ);
					if(ls==null) continue;
					lines.addAll(ls[0].affine(revymat).toEdges());
					if(ls.length>1) lines.addAll(ls[1].affine(revymat).toEdges());
				}
			}
			lineCombination(lines);
			lineCombination(lines);
			for(JDEdge l: lines) {
//				JPlotShape.stroke(l.b.x>l.a.x?0xff00ff00:0xffff0000);
				s.addChild(new JLineShape(lwf, lc, (float)l.a.x, -(float)l.a.y, (float)l.b.x, -(float)l.b.y));
			}
		}
		

		if((lineCode&0x08)==8) {
			lines.clear();
			int o = p[0]-p[1]-p[3];
			int st = (int) (o/pd)-(o<0?1:0), en = (int) ((o+p[2]+p[3])/pd)-(o+p[2]+p[3]<0?0:1);
			for(int i=st; i<=en; i++) {
				float fwd = i*pd - o;
				float bwd = p[2]+p[3]-fwd;
				if(fwd<0 || bwd<0) continue;
				JDPoint leftTop  = new JDPoint(p[0]+Math.max(fwd-p[3], 0), p[1]+Math.max(p[3]-fwd, 0));
				JDPoint botRight = new JDPoint(p[0]+Math.min(fwd, p[2]),   p[1]+Math.min(bwd, p[3]));
				for(JDQuad qu: quads) {
					JDLine[] ls = qu.getLevelRangeLines(leftTop, botRight, minZ, maxZ);
					if(ls==null) continue;
					lines.addAll(ls[0].toEdges());
					if(ls.length>1) lines.addAll(ls[1].toEdges());
				}
			}
			lineCombination(lines);
			lineCombination(lines);
			for(JDEdge l: lines) {
//				JPlotShape.stroke(l.b.x>l.a.x?0xff00ff00:0xffff0000);
				s.addChild(new JLineShape(lwf, lc, (float)l.a.x, (float)l.a.y, (float)l.b.x, (float)l.b.y));
			}
		}
	}
	
	public void drawMarkers(JDQuad[] quads, int pointCode, JGroupShape s, JAxis ax, double pdist) {
		int[] p = ax.getSize();
		boolean dblDens = (pointCode & 0x10) != 0;
		float pdx = (float) (dblDens ? 0.5d*pdist : pdist);
		float pdy = pdx*(float)Math.sqrt(0.75d);
		JPlotShape.fill(lc); JPlotShape.stroke(lc); JPlotShape.strokeWeight(Math.min(0.15f*pdx, 2f*(float)lw));
		int si=(int)(p[0]/pdx), ei=1+(int)((p[0]+p[2])/pdx);
		int sj=(int)(p[1]/pdy), ej=1+(int)((p[1]+p[3])/pdy);
		for(int j=sj; j<=ej; j++) {
			float y = j*pdy;
			if(y<p[1]) continue;
			if(y>p[1]+p[3]) continue;
			for(int i=si; i<=ei; i++) {
				float x = (i + (j%2==1?0.5f:0f))*pdx;
				if(x<p[0]) continue;
				if(x>p[0]+p[2]) continue;
				JDPoint pt = new JDPoint(x,y);
				for(JDQuad qu: quads) {
					if(!qu.contains(pt, 0.05d))
						continue;
					double v = qu.valueAt(pt);
					if(Double.isNaN(v)) continue;
					if(v<minZ) continue;
					if(v>maxZ) continue;
					switch(pointCode&0x0f) {
						case 1: s.addChild(new JEllipseShape(x, y, 0.2f*pdx, 0.2f*pdx, false)); break;
						case 2: s.addChild(new JEllipseShape(x, y, 0.4f*pdx, 0.4f*pdx, false)); break;
						case 3: s.addChild(new JEllipseShape(x, y, 0.2f*pdx, 0.2f*pdx, false));
								s.addChild(new JEllipseShape(x, y, 0.4f*pdx, 0.4f*pdx, false)); break;
						case 4: s.addChild(new JEllipseShape(x, y, 0.2f*pdx, 0.2f*pdx, true)); break;
						case 5: s.addChild(new JPolygonShape(new float[][] {
							{x+0.000f*pdx,y-0.600f*pdx}, {x-0.134f*pdx,y-0.185f*pdx}, {x-0.571f*pdx,y-0.185f*pdx},
							{x-0.217f*pdx,y+0.071f*pdx}, {x-0.353f*pdx,y+0.485f*pdx}, {x+0.000f*pdx,y+0.228f*pdx},
							{x+0.353f*pdx,y+0.485f*pdx}, {x+0.217f*pdx,y+0.071f*pdx}, {x+0.571f*pdx,y-0.185f*pdx},
							{x+0.134f*pdx,y-0.185f*pdx}}, true, false)); break;
						default: break;
					}
				}
			}
		}
	}
	
	
	
	
	//* **************************************** *
	//* ********** PRIVATE METHODS    ********** *
	//* **************************************** *
	
	private void init() {
		density = 1d;
		comparator = new Comparator<JDEdge>() {
			@Override
			public int compare(JDEdge e1, JDEdge e2) {
				return e1.a.compareTo(e2.a);
			}
		};
	}

	private void collectValidPoints(JAxis ax, boolean debug) {
		boolean xLog = ax.isXlogAxis();
		boolean yLog = ax.isYlogAxis();
		int nx = 0, ny = 0;
		if (input2d) {
			ny = xarrayx2.length;
			if (yarrayy2.length != ny)
				throw new IllegalArgumentException("x, y and z are of different shapes!");
			nx = xarrayx2.length;
			if (yarrayy2.length != nx)
				throw new IllegalArgumentException("x, y and z are of different shapes!");
		} else {
			nx = xarrayx.length;
			ny = yarrayy.length;
		}
		points = new JDPoint[zarrayz.length][zarrayz[0].length];
		double[] xy;
		for (int j = 0; j < ny; j++)
			for (int i = 0; i < nx; i++) {
				if (input2d) {
					xy = inputProj.fromPROJtoLATLON(xLog ? Math.log10(xarrayx2[j][i]) : xarrayx2[j][i],
							yLog ? Math.log10(yarrayy2[j][i]) : yarrayy2[j][i], false, false);
				} else {
					xy = inputProj.fromPROJtoLATLON(xLog ? Math.log10(xarrayx[i]) : xarrayx[i],
							yLog ? Math.log10(yarrayy[j]) : yarrayy[j], false, false);
				}
				if(ax.isGeoAxis()) {
					xy = ax.getGeoProjection().fromLATLONtoPROJ(xy[0], xy[1], false, false);
				}
				points[j][i] = new JDPoint(xy[0], xy[1], zarrayz[j][i]);
			}
		double xyScale = Math.max(Math.max(-Xin, Xax), Math.max(-Yin, Yax));
		xyScale = Math.max(xyScale, Math.max(Xax - Xin, Yax - Yin));
	}
	
	private void points2quads() {
		int jlen = points.length-1;
		int ilen = points[0].length-1;
		quads = new JDQuad[jlen*ilen];
		for(int j=0; j<jlen; j++) {
			for(int i=0; i<ilen; i++) {
				int idx = j*ilen+i;
				quads[idx] = new JDQuad(points[j][i],points[j][i+1],points[j+1][i+1],points[j+1][i]);
//				if(quads[idx].area()<0d) quads[idx].reverse_orientation();
			}
		}
	}
	
	private void lineCombination(List<JDEdge> lines) {
		lines.sort(comparator);
		for(int j=lines.size()-1; j>0; j--) {
			JDEdge ej = lines.get(j);
			double tj = Math.abs(ej.a.x-ej.b.x) + Math.abs(ej.a.y-ej.b.y);
			boolean[] rem = new boolean[j];
			for(int i=0; i<j; i++) rem[i] = false;
			for(int i=j-1; i>=0; i--) {
				JDEdge ei = lines.get(i);
				double ti = Math.abs(ei.a.x-ei.b.x) + Math.abs(ei.a.y-ei.b.y); 
				double tol = 0.000001d*Math.min(tj, ti);
//				if(ej.a.equals(ei.a, tol)) {
//					lines.set(j,new JDEdge(ei.b, ej.b));
//					ej = lines.get(j); rem[i] = true; continue;
//				}
				if(ej.a.equals(ei.b, tol)) {
					lines.set(j,new JDEdge(ei.a, ej.b));
					ej = lines.get(j); rem[i] = true; continue;
				}
				if(ej.b.equals(ei.a, tol)) {
					lines.add(new JDEdge(ei.b, ej.a));
					ej = lines.get(j); rem[i] = true; continue;
				}
//				if(ej.b.equals(ei.b, tol)) {
//					lines.add(new JDEdge(ei.a, ej.a));
//					ej = lines.get(j); rem[i] = true; continue;
//				}
			}
			for(int i=j-1; i>=0; i--) if(rem[i]) {
				lines.remove(i);
				j--;
			}
		}
	}
	
	//* **************************************** *
	//* ********** STATIC METHODS     ********** *
	//* **************************************** *
	
	public static int[] patternCodes(String pattern) {
		int lineCode = 0, pointCode = 0;
		if(pattern.contains("|"))  lineCode |= 0x01; if(pattern.contains("||"))   lineCode |= 0x11;
		if(pattern.contains("-"))  lineCode |= 0x02; if(pattern.contains("--"))   lineCode |= 0x12;
		if(pattern.contains("+"))  lineCode |= 0x03; if(pattern.contains("++"))   lineCode |= 0x13;
		if(pattern.contains("/"))  lineCode |= 0x04; if(pattern.contains("//"))   lineCode |= 0x14;
		if(pattern.contains("\\")) lineCode |= 0x08; if(pattern.contains("\\\\")) lineCode |= 0x18;
		if(pattern.contains("x"))  lineCode |= 0x0c; if(pattern.contains("xx"))   lineCode |= 0x1c;
		if(pattern.contains("X"))  lineCode |= 0x0c; if(pattern.contains("XX"))   lineCode |= 0x1c;
		
		if(pattern.equals("o"))    pointCode = 0x01;
		if(pattern.equals("O"))    pointCode = 0x02;
		if(pattern.equals("Oo"))   pointCode = 0x03;
		if(pattern.equals("oO"))   pointCode = 0x03;
		if(pattern.equals("."))    pointCode = 0x04;
		if(pattern.equals(".."))   pointCode = 0x14;
		if(pattern.equals("*"))    pointCode = 0x05;
		if(pattern.equals("**"))   pointCode = 0x15;
		return new int[] {lineCode,pointCode};
	}
}
