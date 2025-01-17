package nl.tudelft.simulation.medlabs.activity;

import org.djutils.exceptions.Throw;

import nl.tudelft.simulation.jstats.distributions.DistContinuous;
import nl.tudelft.simulation.medlabs.activity.locator.LocatorInterface;
import nl.tudelft.simulation.medlabs.model.MedlabsModelInterface;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.simulation.TimeUnit;

/**
 * The StochasticDurationActivity is an activity for a person where the flexible
 * duration is provided as a distribution function.
 * <p>
 * Copyright (c) 2014-2024 Delft University of Technology, Jaffalaan 5, 2628 BX
 * Delft, the Netherlands. All rights reserved. The MEDLABS project (Modeling
 * Epidemic Disease with Large-scale Agent-Based Simulation) is aimed at
 * providing policy analysis tools to predict and help contain the spread of
 * epidemics. It makes use of the DSOL simulation engine and the agent-based
 * modeling formalism. See for project information
 * <a href="http://www.simulation.tudelft.nl/"> www.simulation.tudelft.nl</a>.
 * The original MEDLABS Java library was developed as part of the PhD research
 * of Mingxin Zhang at TU Delft and is described in the PhD thesis "Large-Scale
 * Agent-Based Social Simulation" (2016). This software is licensed under the
 * BSD license. See license.txt in the main project.
 * </p>
 * 
 * @author Mingxin Zhang
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class StochasticDurationActivity extends FlexibleDurationActivity {
	/** */
	private static final long serialVersionUID = 20140505L;

	/** the duration distribution in hours. */
	private final DistContinuous durationDistribution;

	/** the unit. */
	private final TimeUnit unit;

	/**
	 * Create an activity with a fixed duration. Duration can be NaN which will have
	 * the person skip this activity.
	 * 
	 * @param model                MedlabsModelInterface; pointer to the model for
	 *                             retrieving simulator and other relevant
	 *                             information
	 * @param name                 String; the name of the activity
	 * @param locator              LocatorInterface&lt;T&gt;; the locator that
	 *                             returns where the activity takes place
	 * @param estimatedDuration    double; the estimated duration of the activity
	 *                             (in hours), e.g. for planning purposes
	 * @param durationDistribution DistContinuous; the duration distribution in the
	 *                             time unit
	 * @param unit                 TimeUnit; the time unit of the duration
	 *                             distribution
	 */
	public StochasticDurationActivity(final MedlabsModelInterface model, final String name,
			final LocatorInterface locator, final double estimatedDuration, final DistContinuous durationDistribution,
			final TimeUnit unit) {
		super(model, name, locator, estimatedDuration);
		Throw.whenNull(durationDistribution, "durationDistribution cannot be null");
		Throw.whenNull(unit, "unit cannot be null");
		this.durationDistribution = durationDistribution;
		this.unit = unit;
	}

	/** {@inheritDoc} */
	@Override
	public double getDuration(final Person person) {
		return TimeUnit.convert(this.durationDistribution.draw(), this.unit);
	}
}
