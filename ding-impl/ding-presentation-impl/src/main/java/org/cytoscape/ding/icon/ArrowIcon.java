package org.cytoscape.ding.icon;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


/**
 * Icon for arrow shape.
 */
public class ArrowIcon extends VisualPropertyIcon<Shape> {
	private final static long serialVersionUID = 1202339877462891L;
	
	private static final Stroke EDGE_STROKE = new BasicStroke(6.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
	private static final Stroke EDGE_STROKE_SMALL = new BasicStroke(4.0f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER);
	protected Graphics2D g2d;
	private static final int DEF_L_PAD = 15;


	public ArrowIcon(final Shape shape, int width, int height, String name) {
		super(shape, width, height, name);
	}

	
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g2d = (Graphics2D) g;

		// Turn AA on
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(color);

		g2d.translate(leftPad, bottomPad);

		/*
		 * If shape is not defined, treat as no-head.
		 */
		if (value == null) {
			if ((width < 20) || (height < 20)) {
				
				g2d.setStroke(EDGE_STROKE_SMALL);
				g2d.drawLine(3, c.getHeight() / 2, width / 2 + 10, c.getHeight() / 2);
				
			} else {
				g2d.setStroke(EDGE_STROKE);
				g2d.drawLine(DEF_L_PAD, (height + 20) / 2,
			             (int) (c.getWidth()*0.3), (height + 20) / 2);
			}
			g2d.translate(-leftPad, -bottomPad);
			return;
		}

		final AffineTransform af = new AffineTransform();
		g2d.setStroke(new BasicStroke(2.0f));

		final Rectangle2D bound = value.getBounds2D();
		final double minx = bound.getMinX();
		final double miny = bound.getMinY();

		Shape newShape = value;

		/*
		 * Adjust position if it is NOT in first quadrant.
		 */
		if (minx < 0) {
			af.setToTranslation(Math.abs(minx), 0);
			newShape = af.createTransformedShape(newShape);
		}

		if (miny < 0) {
			af.setToTranslation(0, Math.abs(miny));
			newShape = af.createTransformedShape(newShape);
		}

		final double shapeWidth = newShape.getBounds2D().getWidth();
		final double shapeHeight = newShape.getBounds2D().getHeight()*2;

		final double originalXYRatio = shapeWidth / shapeHeight;

		final double xRatio = (width / 3) / shapeWidth;
		final double yRatio = height / shapeHeight;
		af.setToScale(xRatio * originalXYRatio, yRatio);
		newShape = af.createTransformedShape(newShape);

		af.setToTranslation((width * 0.8) - newShape.getBounds2D().getCenterX(),
		                    ((height + 20) / 2) - newShape.getBounds2D().getCenterY());
		newShape = af.createTransformedShape(newShape);

		g2d.fill(newShape);
		g2d.setStroke(EDGE_STROKE);
		g2d.drawLine(DEF_L_PAD, (height + 20) / 2, (int) (newShape.getBounds2D().getCenterX()) - 2, (height + 20) / 2);
		g2d.translate(-leftPad, -bottomPad);

	}
}
