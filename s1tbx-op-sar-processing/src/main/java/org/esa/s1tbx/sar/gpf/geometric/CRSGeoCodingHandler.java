/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.sar.gpf.geometric;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.Rectangle2D;

/**
 * help create a CRS GeoCoding
 */
public class CRSGeoCodingHandler {

    private final CoordinateReferenceSystem targetCRS;
    private final CrsGeoCoding geoCoding;
    private final int targetWidth;
    private final int targetHeight;

    public CRSGeoCodingHandler(final Product sourceProduct, final String mapProjection,
                               final double pixelSpacingInDegree, final double pixelSpacingInMeter) throws Exception {

        targetCRS = getCRS(sourceProduct, mapProjection);

        final OperatorUtils.ImageGeoBoundary srcImageBoundary = OperatorUtils.computeImageGeoBoundary(sourceProduct);

        double pixelSizeX = pixelSpacingInMeter;
        double pixelSizeY = pixelSpacingInMeter;
        if (targetCRS.getName().getCode().equals("WGS84(DD)")) {
            pixelSizeX = pixelSpacingInDegree;
            pixelSizeY = pixelSpacingInDegree;
        }

        final Rectangle2D bounds = new Rectangle2D.Double();
        double lonMin = srcImageBoundary.lonMin;
        double lonMax = srcImageBoundary.lonMax;
        /*
        if(lonMin > 180)
            lonMin -= 360;
        if(lonMax > 180)
            lonMax -= 360;
        */
        bounds.setFrameFromDiagonal(lonMin, srcImageBoundary.latMin, lonMax, srcImageBoundary.latMax);
        final ReferencedEnvelope boundsEnvelope = new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84);
        final ReferencedEnvelope targetEnvelope = boundsEnvelope.transform(targetCRS, true, 200);
        targetWidth = (int) Math.floor(targetEnvelope.getSpan(0) / pixelSizeX);
        targetHeight = (int) Math.floor(targetEnvelope.getSpan(1) / pixelSizeY);
        geoCoding = new CrsGeoCoding(targetCRS,
                targetWidth,
                targetHeight,
                targetEnvelope.getMinimum(0),
                targetEnvelope.getMaximum(1),
                pixelSizeX, pixelSizeY);
    }

    public static CoordinateReferenceSystem getCRS(final Product sourceProduct, String crs) throws Exception {
        try {
            if (crs == null || crs.isEmpty() || crs.equals("WGS84(DD)")) {
                return DefaultGeographicCRS.WGS84;
            }
            return CRS.parseWKT(crs);
        } catch (Exception e) {
            // prefix with EPSG, if there are only numbers
            if (crs.matches("[0-9]*")) {
                crs = "EPSG:" + crs;
            }
            // append center coordinates for AUTO code
            if (crs.matches("AUTO:[0-9]*")) {
                final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                crs = String.format("%s,%s,%s", crs, centerGeoPos.lon, centerGeoPos.lat);
            }
            // force longitude==x-axis and latitude==y-axis
            return CRS.decode(crs, true);
        }
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    public CrsGeoCoding getCrsGeoCoding() {
        return geoCoding;
    }
}
