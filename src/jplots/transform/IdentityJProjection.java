package jplots.transform;

import java.util.ArrayList;
import java.util.List;

import jplots.JAxis;
import jplots.maths.JDPolygon;
import jplots.shapes.JGroupShape;
import jplots.shapes.JLineShape;
import jplots.shapes.JPlotShape;

public class IdentityJProjection implements JProjection {
	
	public IdentityJProjection() {
	}

	@Override
	public double[] fromPROJtoLATLON(double x, double y, boolean output_in_degree) {
		return new double[] {x,y};
	}

	@Override
	public double[] fromLATLONtoPROJ(double u, double v, boolean input_in_degree) {
		return new double[] {u,v};
	}
	
	@Override
	public double[] tissotFromLatLon(double u, double v, boolean input_in_degree) {
		return new double[] {1d,0d, 0d,1d};
	}
	
	@Override
	public double[] tissotFromProj(double x, double y) {
		return new double[] {1d,0d, 0d,1d};
	}
	
	@Override
	public List<JDPolygon> splitByMapBorder(JDPolygon poly) {
		List<JDPolygon> res = new ArrayList<>();
		res.add(poly);
		return res;
	}
	
	@Override
	public double[] defaultMapExtend() {
		return new double[] {-1d,1d,-1d,1d};
	}
	
	@Override
	public void drawBorder(JAxis ax, JGroupShape s) {
		JPlotShape.stroke(0xff000000); JPlotShape.strokeWeight(3f);
		int[] p = ax.getSize();
		s.addChild(new JLineShape(p[0],     p[1],     p[0]+p[2],p[1]));
		s.addChild(new JLineShape(p[0],     p[1]+p[3],p[0]+p[2],p[1]+p[3]));
		s.addChild(new JLineShape(p[0],     p[1],     p[0],     p[1]+p[3]));
		s.addChild(new JLineShape(p[0]+p[2],p[1],     p[0]+p[2],p[1]+p[3]));
	}
}
