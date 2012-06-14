/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gnychis.coexisyst.Interfaces;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst;

/**
 * Average temperature demo chart.
 */
public class GraphWispy extends AbstractDemoChart {
  /**
   * Returns the chart name.
   * 
   * @return the chart name
   */
  public String getName() {
    return "Average temperature";
  }
  int maxresults[];

  /**
   * Returns the chart description.
   * 
   * @return the chart description
   */
  public String getDesc() {
    return "The average temperature in 4 Greek islands (line chart)";
  }

  /**
   * Executes the chart demo.
   * 
   * @param context the context
   * @return the built intent
   */
  public Intent execute(Context context) {
	CoexiSyst coexisyst = (CoexiSyst)context;
	Log.d("GraphWispy", "Inside execute() of GraphWispy()");
    String[] titles = new String[] { "2.4GHz" };
    List<double[]> x = new ArrayList<double[]>();
    for (int i = 0; i < titles.length; i++) {
      double d[] = new double[256];
      for(int j=0; j<256; j++)
    	  d[j]=j;
      x.add(d);
    }
    List<double[]> values = new ArrayList<double[]>();
    double d2[] = new double[256];
    for(int j=0; j<256; j++)
    	d2[j] = coexisyst.wispy._maxresults[j];
    values.add(d2);
    int[] colors = new int[] { Color.BLUE};
    PointStyle[] styles = new PointStyle[] { PointStyle.POINT, PointStyle.POINT,
        PointStyle.POINT, PointStyle.POINT };
    XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
    int length = renderer.getSeriesRendererCount();
    for (int i = 0; i < length; i++) {
      ((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
    }
    setChartSettings(renderer, "2.4GHz Spectrum", "bin", "RSSI", 0, 256, -120, -20,
        Color.LTGRAY, Color.LTGRAY);
    renderer.setXLabels(12);
    renderer.setYLabels(10);
    renderer.setShowGrid(true);
    renderer.setXLabelsAlign(Align.RIGHT);
    renderer.setYLabelsAlign(Align.RIGHT);
    renderer.setPanLimits(new double[] { -50, 300, -150, 0 });
    renderer.setZoomLimits(new double[] { -50, 300, -150, 0 });
    Intent intent = ChartFactory.getLineChartIntent(context, buildDataset(titles, x, values),
        renderer, "2.4GHz Spectrum");
    return intent;
  }

}
