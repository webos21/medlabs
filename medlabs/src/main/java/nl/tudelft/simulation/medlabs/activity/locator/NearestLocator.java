package nl.tudelft.simulation.medlabs.activity.locator;

import org.djunits.Throw;

import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.LocationType;
import nl.tudelft.simulation.medlabs.person.Person;

/**
 * The NearestLocator returns the closest location of a certain type relative to the current location of the Person.
 * <p>
 * Copyright (c) 2014-2022 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. The
 * MEDLABS project (Modeling Epidemic Disease with Large-scale Agent-Based Simulation) is aimed at providing policy analysis
 * tools to predict and help contain the spread of epidemics. It makes use of the DSOL simulation engine and the agent-based
 * modeling formalism. See for project information <a href="http://www.simulation.tudelft.nl/"> www.simulation.tudelft.nl</a>.
 * The original MEDLABS Java library was developed as part of the PhD research of Mingxin Zhang at TU Delft and is described in
 * the PhD thesis "Large-Scale Agent-Based Social Simulation" (2016). This software is licensed under the BSD license. See
 * license.txt in the main project.
 * </p>
 * @author Mingxin Zhang
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class NearestLocator implements LocatorInterface
{
    /** the locator where we start, e.g. work, school or home */
    private final LocatorInterface startLocator;

    /** the type of location to return, e.g. "playground" */
    private final LocationType activityLocationType;

    /**
     * @param startLocator LocationInterface&lt;T&gt; the stating position to which the other location needs to be found
     * @param activityLocationType LocationType; the location that needs to be found
     */
    public NearestLocator(final LocatorInterface startLocator, final LocationType activityLocationType)
    {
        Throw.whenNull(startLocator, "startLocator cannot be null");
        Throw.whenNull(activityLocationType, "activityLocationType cannot be null");
        this.startLocator = startLocator;
        this.activityLocationType = activityLocationType;
    }

    /** {@inheritDoc} */
    @Override
    public Location getLocation(final Person person)
    {
        Location startLocation = this.startLocator.getLocation(person);
        if (this.activityLocationType.getLocationTypeId() == person.getModel().getLocationTypeHouse().getLocationTypeId())
        {
            return person.getHomeLocation();
        }
        return this.activityLocationType.getNearestLocation(startLocation);
    }

}
