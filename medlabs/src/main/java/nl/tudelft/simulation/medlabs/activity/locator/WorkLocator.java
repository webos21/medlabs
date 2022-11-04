package nl.tudelft.simulation.medlabs.activity.locator;

import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.LocationType;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.person.index.IdxWorker;

/**
 * The WorkLocator can locate the work location of a Worker.
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
public class WorkLocator implements LocatorInterface
{
    /** {@inheritDoc} */
    @Override
    public Location getLocation(final Person person)
    {
        Location workLocation = ((IdxWorker) person).getWorkLocation();
        LocationType wlt = workLocation.getLocationType();
        if (wlt.getFractionActivities() < 1.0 || wlt.getFractionOpen() < 1.0)
        {
            // person might be forced to work somewhere else
            LocationType alt = wlt.getAlternativeLocationType();
            if (person.getModel().getLocationTypeHouse().getLocationTypeId() == alt.getLocationTypeId())
                return person.getHomeLocation();
            return new NearestLocator(new CurrentLocator(), alt).getLocation(person);
        }
        return workLocation;
    }

}
