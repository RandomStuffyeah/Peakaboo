package peakaboo.display.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import peakaboo.common.PeakabooLog;
import peakaboo.curvefit.curve.fitting.FittingResult;
import peakaboo.curvefit.peak.transition.TransitionSeries;
import peakaboo.display.plot.painters.FittingMarkersPainter;
import peakaboo.display.plot.painters.FittingPainter;
import peakaboo.display.plot.painters.FittingSumPainter;
import peakaboo.display.plot.painters.FittingTitlePainter;
import peakaboo.filter.model.Filter;
import scidraw.drawing.DrawingRequest;
import scidraw.drawing.ViewTransform;
import scidraw.drawing.backends.DrawingSurfaceFactory;
import scidraw.drawing.backends.SaveableSurface;
import scidraw.drawing.backends.Surface;
import scidraw.drawing.backends.SurfaceType;
import scidraw.drawing.painters.axis.AxisPainter;
import scidraw.drawing.painters.axis.LineAxisPainter;
import scidraw.drawing.painters.axis.TitleAxisPainter;
import scidraw.drawing.plot.PlotDrawing;
import scidraw.drawing.plot.painters.PlotPainter;
import scidraw.drawing.plot.painters.axis.GridlinePainter;
import scidraw.drawing.plot.painters.axis.TickMarkAxisPainter;
import scidraw.drawing.plot.painters.plot.OriginalDataPainter;
import scidraw.drawing.plot.painters.plot.PrimaryPlotPainter;
import scitypes.Bounds;
import scitypes.ReadOnlySpectrum;
import scitypes.SpectrumCalculations;

public class Plotter {


	public PlotDrawing draw(PlotData data, PlotSettings settings, Surface context, Dimension size) {
		
		if (settings == null) {
			settings = new PlotSettings();
		}
		
		DrawingRequest dr = new DrawingRequest();
		

		if (data.filtered == null) {
			PeakabooLog.get().log(Level.WARNING, "Could not draw plot, dataForPlot (filtered) was null");
			return null;
		};
		
		
		
		
		
		//white background
		context.rectangle(0, 0, (float)size.getWidth(), (float)size.getHeight());
		context.setSource(Color.white);
		context.fill();

		

		////////////////////////////////////////////////////////////////////
		// Colour Selections
		////////////////////////////////////////////////////////////////////
		Color fitting, fittingStroke, fittingSum;
		Color proposed, proposedStroke, proposedSum;
		Color selected, selectedStroke;

		fitting = new Color(0.0f, 0.0f, 0.0f, 0.3f);
		fittingStroke = new Color(0.0f, 0.0f, 0.0f, 0.5f);
		fittingSum = new Color(0.0f, 0.0f, 0.0f, 0.8f);

		// Colour/Monochrome colours for curve fittings
		if (settings.monochrome)
		{
			proposed = new Color(0x50ffffff, true);
			proposedStroke = new Color(0x80ffffff, true);
			proposedSum = new Color(0xD0ffffff, true);
		}
		else
		{
			proposed = new Color(0x80D32F2F, true);
			proposedStroke = new Color(0x80B71C1C, true);
			proposedSum = new Color(0xD0B71C1C, true);
		}
		
		// Colour/Monochrome colours for highlighted/selected fittings
		if (settings.monochrome)
		{
			selected = new Color(0x50ffffff, true);
			selectedStroke = new Color(0x80ffffff, true);
		}
		else
		{
			selected = new Color(0x800288D1, true);
			selectedStroke = new Color(0xff01579B, true);
		}

		
		
		
		
		////////////////////////////////////////////////////////////////////
		// Plot Painters
		////////////////////////////////////////////////////////////////////

		//if the filtered data somehow becomes taller than the maximum value from the raw data, we don't want to clip it.
		//but if the fitlered data gets weaker, we still want to scale it to the original data, so that its shrinking is obvious
		float maxIntensity = Math.max(data.dataset.getAnalysis().maximumIntensity(), data.filtered.max());
		
		
		
		dr.imageHeight = (float) size.getHeight();
		dr.imageWidth = (float) size.getWidth();
		dr.viewTransform = settings.viewTransform;
		dr.unitSize = (data.calibration.getMaxEnergy() - data.calibration.getMinEnergy()) / (float)data.calibration.getDataWidth();
		dr.drawToVectorSurface = context.isVectorSurface();
		

		// if axes are shown, also draw horizontal grid lines
		List<PlotPainter> plotPainters = new ArrayList<PlotPainter>();
		if (settings.showAxes) plotPainters.add(new GridlinePainter(new Bounds<Float>(
			0.0f,
			maxIntensity)));


		// draw the filtered data
		plotPainters.add(new PrimaryPlotPainter(data.filtered, settings.monochrome));

		
		// draw the original data
		if (data.raw != null && settings.backgroundShowOriginal) {
			ReadOnlySpectrum originalData = data.raw;
			plotPainters.add(new OriginalDataPainter(originalData, settings.monochrome));
		}
		
		
		// get any painters that the filters might want to add to the mix
		PlotPainter filterPainter;
		for (Filter f : data.filters)
		{
			filterPainter = f.getPainter();
			
			if (filterPainter != null && f.isEnabled()) {
				filterPainter.setSourceName(f.getFilterName());
				plotPainters.add(filterPainter);
			}
		}

		
		// draw curve fitting
		if (data.selectionResults != null) {
			if (settings.showIndividualFittings)
			{
				plotPainters.add(new FittingPainter(data.selectionResults, fittingStroke, fitting));
				plotPainters.add(new FittingSumPainter(data.selectionResults.getTotalFit(), fittingSum));
			}
			else
			{			
				plotPainters.add(new FittingSumPainter(data.selectionResults.getTotalFit(), fittingSum, fitting));
			}
		}
		
		//draw curve fitting for proposed fittings
		if (data.proposedTransitionSeries != null && data.proposedTransitionSeries.size() > 0)
		{
			if (settings.showIndividualFittings) {
				plotPainters.add(new FittingPainter(data.proposedResults, proposedStroke, proposed));
			} else {
				plotPainters.add(new FittingSumPainter(data.proposedResults.getTotalFit(), proposedStroke, proposed));
			}

			plotPainters.add(

				new FittingSumPainter(SpectrumCalculations.addLists(
						data.proposedResults.getTotalFit(),
						data.selectionResults.getTotalFit()), proposedSum)

			);
		}
		

		//highlighted fittings
		List<TransitionSeries> selectedFits = data.highlightedTransitionSeries;
		if (!selectedFits.isEmpty()) {
			List<FittingResult> selectedFitResults = data.selectionResults.getFits()
					.stream()
					.filter(r -> selectedFits.contains(r.getTransitionSeries()))
					.collect(Collectors.toList());
			plotPainters.add(new FittingPainter(selectedFitResults, selectedStroke, selected));
		}
		
		
		if (data.selectionResults != null) {
			plotPainters.add(new FittingTitlePainter(
					data.selectionResults,
					settings.showElementFitTitles,
					settings.showElementFitIntensities,
					fittingStroke
				)
			);
		}
		
		if (data.proposedResults != null) {
			plotPainters.add(new FittingTitlePainter(
					data.proposedResults,
					settings.showElementFitTitles,
					settings.showElementFitIntensities,
					proposedStroke
				)
			);
		}
		
		
		if (settings.showElementFitMarkers) {
			if (data.selectionResults != null) {
				plotPainters.add(new FittingMarkersPainter(data.selectionResults, data.escape, fittingStroke));
			}
			if (data.proposedResults != null) {
				plotPainters.add(new FittingMarkersPainter(data.proposedResults, data.escape, proposedStroke));
			}
		}
		
		
		
		

		
		////////////////////////////////////////////////////////////////////
		// Axis Painters
		////////////////////////////////////////////////////////////////////

		//if (axisPainters == null)
		//{
		List<AxisPainter> axisPainters = new ArrayList<AxisPainter>();

		if (settings.showPlotTitle)
		{
			axisPainters.add(new TitleAxisPainter(1.0f, null, null, data.dataset.getScanData().datasetName(), null));
		}

		if (settings.showAxes)
		{

			axisPainters.add(new TitleAxisPainter(1.0f, "Relative Intensity", null, null, "Energy (keV)"));
			axisPainters.add(new TickMarkAxisPainter(
				new Bounds<Float>(0.0f, maxIntensity),
				new Bounds<Float>(data.calibration.getMinEnergy(), data.calibration.getMaxEnergy()),
				null,
				new Bounds<Float>(0.0f, maxIntensity),
				dr.viewTransform == ViewTransform.LOG,
				dr.viewTransform == ViewTransform.LOG));
			axisPainters.add(new LineAxisPainter(true, true, settings.showPlotTitle, true));

		}


		
		dr.maxYIntensity = maxIntensity;
		dr.dataWidth = data.calibration.getDataWidth();
		
		
		PlotDrawing plot = new PlotDrawing(context, dr, plotPainters, axisPainters);
		plot.draw();
				
		return plot;

		
	}
	
	public void write(PlotData data, PlotSettings settings, SurfaceType type, Dimension size, OutputStream out) throws IOException {
		
		SaveableSurface s = DrawingSurfaceFactory.createSaveableSurface(type, (int)size.getWidth(), (int)size.getHeight());
		this.draw(data, settings, s, size);
		s.write(out);
		
	}
	
	public void write(PlotData data, PlotSettings settings, SurfaceType type, Dimension size, Path destination) throws IOException {
		
		OutputStream stream = Files.newOutputStream(destination, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		this.write(data, settings, type, size, stream);
		stream.close();
		
	}
	
}
