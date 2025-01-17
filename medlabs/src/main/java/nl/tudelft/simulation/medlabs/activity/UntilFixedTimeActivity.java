package nl.tudelft.simulation.medlabs.activity;

import org.djutils.exceptions.Throw;

import nl.tudelft.simulation.medlabs.activity.locator.LocatorInterface;
import nl.tudelft.simulation.medlabs.common.MedlabsRuntimeException;
import nl.tudelft.simulation.medlabs.model.MedlabsModelInterface;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.simulation.TimeUnit;

/**
 * The UntilFixedTimeActivity describes an activity that has a duration until a
 * time of day. E.g., when the until-time is 17:00 and the current time at the
 * start of the activity is 15:30, the activity will take 90 minutes.
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
public class UntilFixedTimeActivity extends AbstractDurationActivity {
	/** */
	private static final long serialVersionUID = 20140505L;

	/** the duration of the activity. */
	private final double untilHour;

	/**
	 * Create an Activity type for a single person walking from A to B.
	 * 
	 * @param model     MedlabsModelInterface; pointer to the model for retrieving
	 *                  simulator and other relevant information
	 * @param name      String; the name of the activity
	 * @param locator   LocatorInterface&lt;T&gt;; the locator that returns where
	 *                  the activity takes place
	 * @param untilHour double; the hour of the day between 0 and 24 till which the
	 *                  activity should take place
	 */
	public UntilFixedTimeActivity(final MedlabsModelInterface model, final String name, final LocatorInterface locator,
			final double untilHour) {
		super(model, name, locator);
		Throw.when(Double.isNaN(untilHour), MedlabsRuntimeException.class, "untilHour cannot be NaN");
		this.untilHour = untilHour;
	}

	/** {@inheritDoc} */
	@Override
	public double getDuration(final Person person) {
		// get the simulation time, and derive the hour of the day, with a fraction
		double hours = this.model.getSimulator().getSimulatorTime();
		int days = (int) Math.floor(hours / 24.0);
		double currentHour = hours - 24.0 * days;
		double currentHour1 = this.model.getSimulator().getSimulatorTime() % 24.0;

		if (currentHour != currentHour1) {
			System.err.println("sth is wrong");
		}

		double duration = this.untilHour - currentHour;
		return (duration > 0) ? TimeUnit.convert(duration, TimeUnit.HOUR) : Double.NaN;
	}

}
