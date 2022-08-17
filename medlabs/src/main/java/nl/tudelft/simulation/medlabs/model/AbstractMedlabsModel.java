package nl.tudelft.simulation.medlabs.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.model.AbstractDSOLModel;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterBoolean;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterException;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterInteger;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterLong;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterMap;
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterString;
import nl.tudelft.simulation.dsol.simtime.SimTimeDouble;
import nl.tudelft.simulation.jstats.distributions.DistUniform;
import nl.tudelft.simulation.jstats.streams.MersenneTwister;
import nl.tudelft.simulation.jstats.streams.StreamInterface;
import nl.tudelft.simulation.medlabs.activity.pattern.WeekPattern;
import nl.tudelft.simulation.medlabs.common.ReproducibleRandomGenerator;
import nl.tudelft.simulation.medlabs.disease.DiseaseMonitor;
import nl.tudelft.simulation.medlabs.disease.DiseaseProgression;
import nl.tudelft.simulation.medlabs.disease.DiseaseTransmission;
import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.LocationType;
import nl.tudelft.simulation.medlabs.location.animation.defaults.HouseAnimation;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.person.PersonMonitor;
import nl.tudelft.simulation.medlabs.policy.Policy;
import nl.tudelft.simulation.medlabs.simulation.SimpleDEVSSimulatorInterface;

/**
 * Abstract class from which a disease model for a certain city or region can be extended.
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
@SuppressWarnings("checkstyle:visibilitymodifier")
public abstract class AbstractMedlabsModel
        extends AbstractDSOLModel<Double, Double, SimTimeDouble, SimpleDEVSSimulatorInterface> implements MedlabsModelInterface
{
    /** */
    private static final long serialVersionUID = 1L;

    /** The reproducible Random generator. */
    protected ReproducibleRandomGenerator reproducibleJava2Random;

    /** the standard random stream of the model. */
    protected StreamInterface randomStream;

    /** the standard uniform distribution based on the standard random stream of the model. */
    protected DistUniform u01;

    /** the persons in the model. */
    protected TIntObjectMap<Person> personMap = new TIntObjectHashMap<>();

    /** the family compositions in the model (array of person ids), indexed by home location. */
    protected TIntObjectMap<TIntSet> familyMembersByHomeLocation = new TIntObjectHashMap<>();

    /** the simulator. */
    protected boolean interactive = true;

    /** the map of all location types by name. */
    protected Map<String, LocationType> locationTypeNameMap = new LinkedHashMap<>();

    /** the List of all location types sorted by index. */
    protected Map<Byte, LocationType> locationTypeIdMap = new LinkedHashMap<>();

    /** the map of all locations, by original id. */
    private TIntObjectMap<Location> locationMap = new TIntObjectHashMap<>();

    /** the map of week patterns to use elsewhere in the model. */
    protected Map<String, WeekPattern> weekPatternMap = new LinkedHashMap<>();

    /** the list of week patterns. */
    protected List<WeekPattern> weekPatternList = new ArrayList<>();

    // /** the graph for this network of gridstops and public transport stops. */
    // protected DefaultDirectedWeightedGraph<Stop, PublicTransportEdge> directedGraph =
    // new DefaultDirectedWeightedGraph<Stop, PublicTransportEdge>(PublicTransportEdge.class);

    // /** the map of ActivityGroups. */
    // protected TIntObjectMap<ActivityGroup> activityGroupMap = new TIntObjectHashMap<>();

    /** the location type of a house or residence. */
    public LocationType locationTypeHouse;

    // /** the location type of the a grid stop in the public transport network. */
    // protected LocationType locationTypeGridStop;

    /** the location of the infinitely large walk area. */
    protected Location locationWalk;

    /** the location of the infinitely large bike area. */
    protected Location locationBike;

    /** the location of the infinitely large car area. */
    protected Location locationCar;

    /** the person monitor to report changes for statistics. */
    private PersonMonitor personMonitor;

    /** the properties file to use. */
    private final String propertyFilename;

    /** the disease progression state machine. */
    protected DiseaseProgression diseaseProgression;

    /** the disease Transmission model. */
    protected DiseaseTransmission diseaseTransmission;

    /** the disease monitor to report changes for statistics. */
    private DiseaseMonitor diseaseMonitor;

    /** the policies. */
    private Map<String, Policy> policyMap = new HashMap<>();

    /** the active policies. */
    private Map<String, Policy> activePolicyMap = new HashMap<>();

    /**
     * Construct the model and set the simulator.
     * @param simulator SimpleDEVSSimulatorInterface; the simulator for this model
     * @param propertyFilename String; the name of the property file
     */
    public AbstractMedlabsModel(final SimpleDEVSSimulatorInterface simulator, final String propertyFilename)
    {
        super(simulator);
        this.propertyFilename = propertyFilename;
        makeInputParameterMap();

        // this.locationTypeGridStop = new LocationType(this, "gridstop", GridStop.class, null);
        this.locationTypeHouse =
                new LocationType(this, (byte) 0, "house", Location.class, HouseAnimation.class, true, true, 1.0);
        LocationType walkLT = new LocationType(this, (byte) -1, "walk", Location.class, null, false, false, 0.0);
        this.locationWalk = new Location(this, -1, walkLT.getLocationTypeId(), 0.0f, 0.0f, (short) 1, 1E6f);
        LocationType bikeLT = new LocationType(this, (byte) -2, "bike", Location.class, null, false, false, 0.0);
        this.locationBike = new Location(this, -2, bikeLT.getLocationTypeId(), 0.0f, 0.0f, (short) 1, 1E6f);
        LocationType carLT = new LocationType(this, (byte) -3, "car", Location.class, null, false, false, 0.0);
        this.locationCar = new Location(this, -3, carLT.getLocationTypeId(), 0.0f, 0.0f, (short) 1, 1E6f);
    }

    /** {@inheritDoc} */
    @Override
    public void constructModel() throws SimRuntimeException
    {
        try
        {
            this.randomStream = new MersenneTwister(getParameterValueLong("generic.Seed") + 1L);
            this.reproducibleJava2Random = new ReproducibleRandomGenerator(getParameterValueLong("generic.Seed") + 2L);
            this.u01 = new DistUniform(this.randomStream, 0.0, 1.0);

            constructModelFromSource();

            // initialize persons
            for (TIntObjectIterator<Person> it = getPersonMap().iterator(); it.hasNext();)
            {
                it.advance();
                it.value().init();
            }

            // schedule the week pattern changes just before midnight every day
            getSimulator().scheduleEventRel(23.999, this, this, "checkChangeWeekPattern", null);
            System.out.println("Model constructed");
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    /**
     * Make the initial input parameter ma with a few parameters that are used by the generic MedlabsModel such as the name for
     * the experiment, the output path for the results (or write no results at all), and the need for animation. The method
     * extendInputParameterMap() is called at the end, enabling extensions of this abstract model to easily add parameters.
     */
    public void makeInputParameterMap()
    {
        try
        {
            InputParameterMap root = this.inputParameterMap;
            InputParameterMap genericMap = new InputParameterMap("generic", "Generic", "Generic parameters", 1.0);
            root.add(genericMap);
            String inputPath = "/";
            genericMap.add(new InputParameterString("InputPath", "Input path", "Input path", inputPath, 1.0));
            genericMap.add(new InputParameterBoolean("WriteOutput", "Write output?", "Output writing on or off", true, 2.0));
            String outputPath = getExecutionPath();
            genericMap.add(new InputParameterString("OutputPath", "Output path", "Output path", outputPath, 3.0));
            genericMap.add(
                    new InputParameterInteger("RunLength", "Run length in days", "Run length in days", 60, 1, 1000, "%d", 4.0));
            genericMap.add(new InputParameterLong("Seed", "Seed for the RNG", "Seed for the Random Number Generator", 111L, 1,
                    Long.MAX_VALUE, "%d", 5.0));
            genericMap.add(new InputParameterInteger("PersonDumpIntervalDays", "Person dump interval in days",
                    "0 means no dumping of person data", 60, 0, 365, "%d", 6.0));

            InputParameterMap policyMap = new InputParameterMap("policies", "Policies", "Policies", 2.0);
            root.add(policyMap);

            extendInputParameterMap();
        }
        catch (InputParameterException exception)
        {
            exception.printStackTrace();
        }
    }

    /**
     * Extend the input parameters with extra tabs and parameters. The base parameters are already there.
     * @throws InputParameterException on not being able to find a key or submap
     */
    protected abstract void extendInputParameterMap() throws InputParameterException;

    /**
     * @return Execution Path
     */
    private String getExecutionPath()
    {
        String absolutePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
        absolutePath = absolutePath.replaceAll("%20", " ");
        return absolutePath;
    }

    /**
     * This method should be implemented in the final model to construct the actual model.
     */
    protected abstract void constructModelFromSource();

    /**
     * Check for all persons whether they need to change their day pattern or not. This method can be overridden to specify a
     * custom method. In that case, don't forget to schedule the method again after 24 hours!
     */
    protected void checkChangeWeekPattern()
    {
        // TODO: implement loop over all persons and see if they need to change
        getSimulator().scheduleEventRel(24.0, this, this, "checkChangeWeekPattern", null);
    }

    /** {@inheritDoc} */
    @Override
    public TIntObjectMap<Person> getPersonMap()
    {
        return this.personMap;
    }

    /** {@inheritDoc} */
    @Override
    public TIntObjectMap<TIntSet> getFamilyMembersByHomeLocation()
    {
        return this.familyMembersByHomeLocation;
    }

    /** {@inheritDoc} */
    @Override
    public void setInteractive(final boolean interactive)
    {
        this.interactive = interactive;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInteractive()
    {
        return this.interactive;
    }

    /** {@inheritDoc} */
    @Override
    public StreamInterface getRandomStream()
    {
        return this.randomStream;
    }

    /** {@inheritDoc} */
    @Override
    public DistUniform getU01()
    {
        return this.u01;
    }

    /** {@inheritDoc} */
    @Override
    public ReproducibleRandomGenerator getReproducibleJava2Random()
    {
        return this.reproducibleJava2Random;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, LocationType> getLocationTypeNameMap()
    {
        return this.locationTypeNameMap;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Byte, LocationType> getLocationTypeIndexMap()
    {
        return this.locationTypeIdMap;
    }

    /** {@inheritDoc} */
    @Override
    public TIntObjectMap<Location> getLocationMap()
    {
        return this.locationMap;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, WeekPattern> getWeekPatternMap()
    {
        return this.weekPatternMap;
    }

    /** {@inheritDoc} */
    @Override
    public List<WeekPattern> getWeekPatternList()
    {
        return this.weekPatternList;
    }

    // /** {@inheritDoc} */
    // @Override
    // public DefaultDirectedWeightedGraph<Stop, PublicTransportEdge> getDirectedGraph()
    // {
    // return this.directedGraph;
    // }

    // /** {@inheritDoc} */
    // @Override
    // public TIntObjectMap<ActivityGroup> getActivityGroupMap()
    // {
    // return this.activityGroupMap;
    // }

    /** {@inheritDoc} */
    @Override
    public LocationType getLocationTypeHouse()
    {
        return this.locationTypeHouse;
    }

    // /** {@inheritDoc} */
    // @Override
    // public LocationType getLocationTypeGridStop()
    // {
    // return this.locationTypeGridStop;
    // }

    /** {@inheritDoc} */
    @Override
    public Location getLocationWalk()
    {
        return this.locationWalk;
    }

    /** {@inheritDoc} */
    @Override
    public Location getLocationBike()
    {
        return this.locationBike;
    }

    /** {@inheritDoc} */
    @Override
    public Location getLocationCar()
    {
        return this.locationCar;
    }

    /** {@inheritDoc} */
    @Override
    public void setPersonMonitor(final PersonMonitor personMonitor)
    {
        this.personMonitor = personMonitor;
    }

    @Override
    public PersonMonitor getPersonMonitor()
    {
        return this.personMonitor;
    }

    /** {@inheritDoc} */
    @Override
    public void setDiseaseProgression(final DiseaseProgression diseaseProgression)
    {
        this.diseaseProgression = diseaseProgression;
    }

    /** {@inheritDoc} */
    @Override
    public DiseaseProgression getDiseaseProgression()
    {
        return this.diseaseProgression;
    }

    /** {@inheritDoc} */
    @Override
    public void setDiseaseTransmission(final DiseaseTransmission diseaseTransmission)
    {
        this.diseaseTransmission = diseaseTransmission;
    }

    /** {@inheritDoc} */
    @Override
    public DiseaseTransmission getDiseaseTransmission()
    {
        return this.diseaseTransmission;
    }

    /** {@inheritDoc} */
    @Override
    public void setDiseaseMonitor(final DiseaseMonitor diseaseMonitor)
    {
        this.diseaseMonitor = diseaseMonitor;
    }

    /** {@inheritDoc} */
    @Override
    public DiseaseMonitor getDiseaseMonitor()
    {
        return this.diseaseMonitor;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Policy> getAllPolicies()
    {
        return this.policyMap;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Policy> getActivePolicies()
    {
        return this.activePolicyMap;
    }

    /** {@inheritDoc} */
    @Override
    public String getPropertyFilename()
    {
        return this.propertyFilename;
    }

    /** {@inheritDoc} */
    @Override
    public Serializable getSourceId()
    {
        return "MedlabsModel";
    }

}