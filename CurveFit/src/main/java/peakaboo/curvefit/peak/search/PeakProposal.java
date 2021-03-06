package peakaboo.curvefit.peak.search;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import peakaboo.curvefit.curve.fitting.EnergyCalibration;
import peakaboo.curvefit.curve.fitting.FittingResultSet;
import peakaboo.curvefit.curve.fitting.FittingSet;
import peakaboo.curvefit.curve.fitting.fitter.CurveFitter;
import peakaboo.curvefit.curve.fitting.solver.FittingSolver;
import peakaboo.curvefit.peak.search.scoring.CompoundFittingScorer;
import peakaboo.curvefit.peak.search.scoring.EnergyProximityScorer;
import peakaboo.curvefit.peak.search.scoring.FastFittingScorer;
import peakaboo.curvefit.peak.search.scoring.NoComplexPileupScorer;
import peakaboo.curvefit.peak.search.scoring.ProportionalPileupScorer;
import peakaboo.curvefit.peak.table.PeakTable;
import peakaboo.curvefit.peak.transition.Transition;
import peakaboo.curvefit.peak.transition.TransitionSeries;
import scitypes.Pair;
import scitypes.ReadOnlySpectrum;


/**
 * This class contains functions to do things like score, suggest, or sort TransitionSeries based on provided data.
 * @author NAS, 2010-2018
 *
 */

public class PeakProposal
{


	
	public static List<TransitionSeries> search(
			final ReadOnlySpectrum data,
			PeakSearcher searcher,
			FittingSet fits,
			CurveFitter fitter,
			FittingSolver solver
		) {
		
		
		EnergyCalibration calibration = fits.getFittingParameters().getCalibration();
		
		//Proposals fitting set to store proposals in with same parameters
		FittingSet proposals = new FittingSet(fits);
		proposals.clear();
		
		
		//Generate list of peaks
		List<Integer> peaks = searcher.search(data);
		
		
		//remove any peaks within the FWHM of an existing Transitions in fits
		for (int peak : new ArrayList<>(peaks)) {
			for (TransitionSeries ts : fits.getFittedTransitionSeries()) {
				for (Transition t : ts) {
					float hwhm = fits.getFittingParameters().getFWHM(t)/2f;
					float min = t.energyValue - hwhm;
					float max = t.energyValue + hwhm;
					float energy = calibration.energyFromChannel(peak);
					if (min < energy && energy < max) {
						peaks.remove(new Integer(peak));
					}
				}
			}
		}
		
		
		
		//Generate lists of guesses for all peaks
		Map<Integer, List<TransitionSeries>> guesses = makeGuesses(data, peaks, fits, proposals, fitter, solver);
		
		
		/*
		 * Go peak by peak from strongest to weakest.
		 * Take the best guess for that peak.
		 * Find other peaks which also have that guess as part of their list of guesses
		 * Remove those other peaks from future consideration 
		 */
		List<TransitionSeries> newFits = new ArrayList<>();
		for (int channel : peaks) {
			if (!guesses.containsKey(channel)) { continue; }
			

			//Get the best guess from the list
			TransitionSeries guess = guesses.get(channel).get(0);
			
			
			//If the existing fits doesn't contain this, add it
			if (!fits.getFittedTransitionSeries().contains(guess)) {
				newFits.add(guess);
				proposals.addTransitionSeries(guess);
			}
			
			
			//remove all peaks which contain this guess
			for (int match : new ArrayList<>(guesses.keySet())) {
				if (guesses.get(match).contains(guess)) { 
					guesses.remove(match);
				}
			}
			
			
			//Regenerate new guesses for remaining peaks based on combined 
			//fittingset so that pileup is considered in future iterations
			guesses = makeGuesses(data, guesses.keySet(), fits, proposals, fitter, solver);
			
			
		}
		
		return newFits;
		

		
	}

	
	private static Map<Integer, List<TransitionSeries>> makeGuesses(
			ReadOnlySpectrum data, 
			Collection<Integer> peaks, 
			FittingSet fits,
			FittingSet proposals,
			CurveFitter fitter, 
			FittingSolver solver
		) {
		Map<Integer, List<TransitionSeries>> guesses = new LinkedHashMap<>();
		for (int channel : peaks) {
			guesses.put(channel, fromChannel(data, fits, proposals, fitter, solver, channel, null));
		}
		return guesses;
	}
	
	
	/**
	 * Generates a list of {@link TransitionSeries} which are good fits for the given data at the given channel index
	 * @return an ordered list of {@link TransitionSeries} which are good fits for the given data at the given channel
	 */
	public static List<TransitionSeries> fromChannel(
			final ReadOnlySpectrum data, 
			FittingSet fits,
			FittingSet proposed,
			CurveFitter fitter,
			FittingSolver solver,
			final int channel, 
			TransitionSeries currentTS
		) {
		
		
		/*
		 * 
		 * Method description
		 * ------------------
		 * 
		 * We try to figure out which Transition Series are the best fit for the given channel.
		 * This is done in the following steps
		 * 
		 * 1. If we have suggested a TS previously, it should be passed in in currentTS
		 * 2. If currentTS isn't null, we remove it from the proposals, refit, and then readd it
		 * 		* we do this so that we can still suggest that same TS this time, otherwise, there would be no signal for it to fit
		 * 3. We get all TSs from the peak table, and add all summations of all fitted & proposed TSs
		 * 4. We remove all TSs which are already fitted or proposed.
		 * 5. We add currentTS to the list, since the last step will have removed it
		 * 6. We unique the list, don't want duplicates showing up
		 * 7. We sort by proximity, and take the top 15
		 * 8. We sort by a more detailed scoring function which involves fitting each TS and seeing how well it fits
		 * 9. We return the top 5 from the list in the last step
		 * 
		 */
	
		EnergyCalibration calibration = fits.getFittingParameters().getCalibration();
		
		//remove the current transitionseries from the list of proposed trantision series so we can re-suggest it.
		//otherwise, the copy getting fitted eats all the signal from the one we would suggest during scoring
		boolean currentTSisUsed = currentTS != null && proposed.getFittedTransitionSeries().contains(currentTS);
		if (currentTSisUsed) proposed.remove(currentTS);
		
		//recalculate
		FittingResultSet fitResults = solver.solve(data, fits, fitter);
		FittingResultSet proposedResults = solver.solve(fitResults.getResidual(), proposed, fitter);
		
		
		final ReadOnlySpectrum s = proposedResults.getResidual();
		
		if (currentTSisUsed) proposed.addTransitionSeries(currentTS);
		

		final float energy = calibration.energyFromChannel(channel);	
		

		//get a list of all transition series to start with
		List<TransitionSeries> tss = new ArrayList<>(PeakTable.SYSTEM.getAll());

		
		//add in any 2x summations from the list of previously fitted AND proposed peaks.
		//we exclude any that the caller requests so that if a UI component is *replacing* a TS with
		//these suggestions, it doesn't get summations for the now-removed TS
		List<TransitionSeries> summationCandidates = fits.getFittedTransitionSeries();
		summationCandidates.addAll(proposed.getFittedTransitionSeries());
		if (currentTSisUsed) summationCandidates.remove(currentTS);
		
		for (TransitionSeries ts1 : summationCandidates)
		{
			for (TransitionSeries ts2 : summationCandidates)
			{
				tss.add(ts1.summation(ts2));
			}
		}
		

		//remove the transition series we have already fit, including any summations
		tss.removeAll(fits.getFittedTransitionSeries());
		tss.removeAll(proposed.getFittedTransitionSeries());
		
		
		//We then re-add the TS passed to us so that we can still suggest the 
		//TS that is currently selected, if it fits
		if (currentTSisUsed) {
			tss.add(currentTS);
		}
		
		
		//remove any duplicates we might have created while adding the summations
		tss = new ArrayList<>(new HashSet<>(tss));
		
	
		
		CompoundFittingScorer compoundScorer = new CompoundFittingScorer();
		compoundScorer.add(new EnergyProximityScorer(energy, fits.getFittingParameters()), 10f);
		compoundScorer.add(new FastFittingScorer(s, fits.getFittingParameters()), 10f);
		compoundScorer.add(new NoComplexPileupScorer(), 2f);
		compoundScorer.add(new ProportionalPileupScorer(data, calibration), 1f);
		

		
		
		//now sort by score
		tss = tss.stream()
			.map(ts -> new Pair<>(ts, -compoundScorer.score(ts)))
			.sorted((p1, p2) -> p1.second.compareTo(p2.second))
			.limit(15)
			.map(p -> p.first)
			.collect(Collectors.toList());

		
		//take the best in sorted order based on score
		return tss;
	}


	
	
}
