/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stocktrends;

import javafx.geometry.Side;
import javafx.scene.chart.NumberAxis;

/**
 *
 * @author rghodke
 */
public class CustomAxis extends NumberAxis{
    
    /**
     * Called to set the upper and lower bound and anything else that needs to be auto-ranged
     *
     * @param minValue The min data value that needs to be plotted on this axis
     * @param maxValue The max data value that needs to be plotted on this axis
     * @param length The length of the axis in display coordinates
     * @param labelSize The approximate average size a label takes along the axis
     * @return The calculated range
     */
    @Override protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
        final Side side = getEffectiveSide();
        // check if we need to force zero into range
        if (isForceZeroInRange()) {
            if (maxValue < 0) {
                maxValue = 0;
            } else if (minValue > 0) {
                minValue = 0;
            }
        }
        final double range = maxValue-minValue;
        // pad min and max by 2%, checking if the range is zero
        final double paddedRange = (range==0) ? 2 : Math.abs(range)*1.02;
        final double padding = (paddedRange - range) / 2;
        // if min and max are not zero then add padding to them
        double paddedMin = minValue - padding;
        double paddedMax = maxValue + padding;
        // check padding has not pushed min or max over zero line
        if ((paddedMin < 0 && minValue >= 0) || (paddedMin > 0 && minValue <= 0)) {
            // padding pushed min above or below zero so clamp to 0
            paddedMin = 0;
        }
        if ((paddedMax < 0 && maxValue >= 0) || (paddedMax > 0 && maxValue <= 0)) {
            // padding pushed min above or below zero so clamp to 0
            paddedMax = 0;
        }
        // calculate the number of tick-marks we can fit in the given length
        int numOfTickMarks = (int)Math.floor(length/labelSize);
        // can never have less than 2 tick marks one for each end
        numOfTickMarks = Math.max(numOfTickMarks, 2);
        // calculate tick unit for the number of ticks can have in the given data range
        double tickUnit = paddedRange/(double)numOfTickMarks;
        // search for the best tick unit that fits
        double tickUnitRounded = 0;
        double minRounded = 0;
        double maxRounded = 0;
        int count = 0;
        double reqLength = Double.MAX_VALUE;
        String formatter = "0.00000000";
        // loop till we find a set of ticks that fit length and result in a total of less than 20 tick marks
        while (reqLength > length || count > 20) {
            int exp = (int)Math.floor(Math.log10(tickUnit));
            final double mant = tickUnit / Math.pow(10, exp);
            double ratio = mant;
            if (mant > 5d) {
                exp++;
                ratio = 1;
            } else if (mant > 1d) {
                ratio = mant > 2.5 ? 5 : 2.5;
            }
            if (exp > 1) {
                formatter = "#,##0";
            } else if (exp == 1) {
                formatter = "0";
            } else {
                final boolean ratioHasFrac = Math.rint(ratio) != ratio;
                final StringBuilder formatterB = new StringBuilder("0");
                int n = ratioHasFrac ? Math.abs(exp) + 1 : Math.abs(exp);
                if (n > 0) formatterB.append(".");
                for (int i = 0; i < n; ++i) {
                    formatterB.append("0");
                }
                formatter = formatterB.toString();

            }
            tickUnitRounded = ratio * Math.pow(10, exp);
            // move min and max to nearest tick mark
            minRounded = Math.floor(paddedMin / tickUnitRounded) * tickUnitRounded;
            maxRounded = Math.ceil(paddedMax / tickUnitRounded) * tickUnitRounded;
            // calculate the required length to display the chosen tick marks for real, this will handle if there are
            // huge numbers involved etc or special formatting of the tick mark label text
            double maxReqTickGap = 0;
            double last = 0;
            count = 0;
            for (double major = minRounded; major <= maxRounded; major += tickUnitRounded, count ++)  {
                double size = side.isVertical() ? measureTickMarkSize(major, getTickLabelRotation(), formatter).getHeight() :
                                            measureTickMarkSize(major, getTickLabelRotation(), formatter).getWidth();
                if (major == minRounded) { // first
                    last = size/2;
                } else {
                    maxReqTickGap = Math.max(maxReqTickGap, last + 6 + (size/2) );
                }
            }
            reqLength = (count-1) * maxReqTickGap;
            tickUnit = tickUnitRounded;

            // fix for RT-35600 where a massive tick unit was being selected
            // unnecessarily. There is probably a better solution, but this works
            // well enough for now.
            if (numOfTickMarks == 2 && reqLength > length) {
                break;
            }
            if (reqLength > length || count > 20) tickUnit *= 2; // This is just for the while loop, if there are still too many ticks
        }
        // calculate new scale
        final double newScale = calculateNewScale(length, minRounded, maxRounded);
        // return new range
        return new Object[]{minRounded, maxRounded, tickUnitRounded, newScale, formatter};
    }
    
}
