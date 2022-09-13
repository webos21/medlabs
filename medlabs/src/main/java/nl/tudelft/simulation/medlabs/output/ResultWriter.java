package nl.tudelft.simulation.medlabs.output;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Map;

import org.djutils.event.EventInterface;
import org.djutils.event.EventListenerInterface;

import nl.tudelft.simulation.medlabs.common.MedlabsRuntimeException;
import nl.tudelft.simulation.medlabs.disease.DiseasePhase;
import nl.tudelft.simulation.medlabs.disease.DiseaseProgression;
import nl.tudelft.simulation.medlabs.location.Location;
import nl.tudelft.simulation.medlabs.location.LocationType;
import nl.tudelft.simulation.medlabs.model.MedlabsModelInterface;
import nl.tudelft.simulation.medlabs.person.Person;
import nl.tudelft.simulation.medlabs.person.PersonMonitor;
import nl.tudelft.simulation.medlabs.person.PersonType;
import nl.tudelft.simulation.medlabs.person.Student;
import nl.tudelft.simulation.medlabs.person.Worker;

/**
 * ResultWriter writes simulation results to output files periodically.
 * <p>
 * Copyright (c) 2014-2022 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. The
 * MEDLABS project (Modeling Epidemic Disease with Large-scale Agent-Based Simulation) is aimed at providing policy analysis
 * tools to predict and help contain the spread of epidemics. It makes use of the DSOL simulation engine and the agent-based
 * modeling formalism. See for project information <a href="http://www.simulation.tudelft.nl/"> www.simulation.tudelft.nl</a>.
 * The original MEDLABS Java library was developed as part of the PhD research of Mingxin Zhang at TU Delft and is described in
 * the PhD thesis "Large-Scale Agent-Based Social Simulation" (2016). This software is licensed under the BSD license. See
 * license.txt in the main project.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class ResultWriter implements EventListenerInterface
{
    /** */
    private static final long serialVersionUID = 20201005L;

    /** the model. */
    private final MedlabsModelInterface model;

    /** The locationtype file. */
    private PrintWriter locationTypeWriter;

    /** The diseasephase file. */
    private PrintWriter diseasePhaseWriter;

    /** The infections per location file. */
    private PrintWriter infectionLocationWriter;

    /** The infections per age bracket file. */
    private PrintWriter infectionAgeWriter;

    /** The deaths per age bracket file. */
    private PrintWriter deathsAgeWriter;

    /** The person dump file. */
    private PrintWriter personDumpWriter;

    /** The file with detailed information about an infection. */
    private PrintWriter infectedPersonWriter;

    /** The file with detailed information about a dead person. */
    private PrintWriter deadPersonWriter;

    /** The file with the number of infections per person type per day. */
    private PrintWriter dayInfPersonWriter;

    /** The file with the total number of infections per person type. */
    private PrintWriter totInfPersonWriter;

    /** The file with the number of infections from a person type to a person type per day. */
    private PrintWriter dayInfPersonToPersonWriter;

    /** The file with the total number of infections from a person type to a person type. */
    private PrintWriter totInfPersonToPersonWriter;

    /** The file with the number of infections per location type from a person type to a person type per day. */
    private PrintWriter dayInfLocPersonToPersonWriter;

    /** The file with the total number of infections per location type from a person type to a person type. */
    private PrintWriter totInfLocPersonToPersonWriter;

    /**
     * Create a writer of results to file.
     * @param model the model
     * @param outputPath the output path to which the filenames will be appended
     */
    public ResultWriter(final MedlabsModelInterface model, final String outputPath)
    {
        this.model = model;
        makeOutputDirectory(outputPath);

        try
        {
            this.locationTypeWriter = new PrintWriter(outputPath + "/locationTypeNrs.csv");
            writeLocationTypeHeader();
            writeLocationTypeLine();

            DiseaseProgression disease = model.getDiseaseProgression();
            this.diseasePhaseWriter = new PrintWriter(outputPath + "/diseasePhaseNrs_" + disease.getName() + ".csv");
            writeDiseasePhaseHeader(disease);
            writeDiseasePhaseLine(disease);

            this.infectionLocationWriter = new PrintWriter(outputPath + "/infectionsPerLocation.csv");
            writeInfectionLocationHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.INFECT_ALL_LOCATIONTYPES_PER_HOUR_EVENT);

            this.infectionAgeWriter = new PrintWriter(outputPath + "/infectionsPerAge.csv");
            writeInfectionAgeHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.INFECT_AGE_PER_HOUR_EVENT);

            this.deathsAgeWriter = new PrintWriter(outputPath + "/deathsPerAge.csv");
            writeDeathsAgeHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.DEATHS_AGE_PER_DAY_EVENT);

            int personDumpInterval = model.getParameterValueInt("generic.PersonDumpIntervalDays");
            if (personDumpInterval > 0)
            {
                this.personDumpWriter = new PrintWriter(outputPath + "/personDump.csv");
                writePersonDumpHeader();
                writePersonDump(personDumpInterval);
            }

            this.infectedPersonWriter = new PrintWriter(outputPath + "/infectedPersons.csv");
            writeInfectedPersonHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.INFECTED_PERSON_EVENT);

            this.deadPersonWriter = new PrintWriter(outputPath + "/deadPersons.csv");
            writeDeadPersonHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.DEAD_PERSON_EVENT);

            this.dayInfPersonWriter = new PrintWriter(outputPath + "/dayInfPersonType.csv");
            writeDayInfPersonTypeHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.DAY_INFECTIONS_PERSON_TYPE);

            this.totInfPersonWriter = new PrintWriter(outputPath + "/totInfPersonType.csv");
            writeTotInfPersonTypeHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.TOT_INFECTIONS_PERSON_TYPE);

            this.dayInfPersonToPersonWriter = new PrintWriter(outputPath + "/dayInfPersonTypeToPersonType.csv");
            writeDayInfPersonToPersonTypeHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.DAY_INFECTIONS_PERSON_TO_PERSON_TYPE);

            this.totInfPersonToPersonWriter = new PrintWriter(outputPath + "/totInfPersonTypeToPersonType.csv");
            writeTotInfPersonToPersonTypeHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.TOT_INFECTIONS_PERSON_TO_PERSON_TYPE);

            this.dayInfLocPersonToPersonWriter = new PrintWriter(outputPath + "/dayInfLocPersonTypeToPersonType.csv");
            writeDayInfLocPersonToPersonTypeHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.DAY_INFECTIONS_LOC_PERSON_TO_PERSON_TYPE);

            this.totInfLocPersonToPersonWriter = new PrintWriter(outputPath + "/totInfLocPersonTypeToPersonType.csv");
            writeTotInfLocPersonToPersonTypeHeader();
            model.getPersonMonitor().addListener(this, PersonMonitor.TOT_INFECTIONS_LOC_PERSON_TO_PERSON_TYPE);
        }
        catch (IOException ioe)
        {
            throw new MedlabsRuntimeException(ioe);
        }
    }

    /**
     * make the output path + directory.
     * @param directory string; the full path to the output directory to create
     */
    private void makeOutputDirectory(final String directory)
    {
        // try to create directory
        File f = new File(directory);
        if (!f.exists())
        {
            try
            {
                if (!f.mkdirs())
                {
                    throw new Exception("Could not create directory for output: " + directory);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }
        }
    }

    private void writeLocationTypeHeader()
    {
        this.locationTypeWriter.print("\"Time(h)\"");
        for (LocationType locationType : this.model.getLocationTypeIndexMap().values())
        {
            this.locationTypeWriter.print(",\"" + locationType.getName() + "\"");
        }
        this.locationTypeWriter.write("\n");
        this.locationTypeWriter.flush();
    }

    private void writeLocationTypeLine()
    {
        this.locationTypeWriter.print(this.model.getSimulator().getSimulatorTime());
        for (LocationType locationType : this.model.getLocationTypeIndexMap().values())
        {
            int nr = 0;
            for (Location location : locationType.getLocationMap().valueCollection())
            {
                nr += location.getAllPersonIds().size();
            }
            this.locationTypeWriter.print("," + nr);
        }
        this.locationTypeWriter.write("\n");
        this.locationTypeWriter.flush();
        this.model.getSimulator().scheduleEventRel(0.5, this, this, "writeLocationTypeLine", null);
    }

    private void writeDiseasePhaseHeader(final DiseaseProgression disease)
    {
        this.diseasePhaseWriter.print("\"Time(h)\"");
        for (DiseasePhase diseasePhase : disease.getDiseasePhases())
        {
            this.diseasePhaseWriter.print(",\"" + diseasePhase.getName() + "\"");
        }
        this.diseasePhaseWriter.write("\n");
        this.diseasePhaseWriter.flush();
    }

    private void writeDiseasePhaseLine(final DiseaseProgression disease)
    {
        this.diseasePhaseWriter.print(this.model.getSimulator().getSimulatorTime());
        for (DiseasePhase diseasePhase : disease.getDiseasePhases())
        {
            this.diseasePhaseWriter.print("," + diseasePhase.getNumberOfPersons());
        }
        this.diseasePhaseWriter.write("\n");
        this.diseasePhaseWriter.flush();
        this.model.getSimulator().scheduleEventRel(0.5, this, this, "writeDiseasePhaseLine", new Object[] {disease});
    }

    private void writeInfectionLocationHeader()
    {
        this.infectionLocationWriter.print("\"Time(h)\"");
        for (LocationType locationType : this.model.getLocationTypeIndexMap().values())
        {
            this.infectionLocationWriter.print(",\"" + locationType.getName() + "\"");
        }
        this.infectionLocationWriter.write("\n");
        this.infectionLocationWriter.flush();
    }

    private void writeInfectionLocationLine(final Map<LocationType, Integer> infections)
    {
        this.infectionLocationWriter.print(this.model.getSimulator().getSimulatorTime());
        for (LocationType locationType : this.model.getLocationTypeIndexMap().values())
        {
            this.infectionLocationWriter.print("," + infections.get(locationType));
        }
        this.infectionLocationWriter.write("\n");
        this.infectionLocationWriter.flush();
    }

    private void writeInfectionAgeHeader()
    {
        this.infectionAgeWriter.print("\"Time(h)\"");
        for (int ageBracket = 0; ageBracket < 11; ageBracket++)
        {
            String bracket = (10 * ageBracket) + "-" + (10 * (ageBracket + 1) - 1);
            this.infectionAgeWriter.print(",\"" + bracket + "\"");
        }
        this.infectionAgeWriter.write("\n");
        this.infectionAgeWriter.flush();
    }

    private void writeInfectionAgeLine(final int[] infectionsPerAgeBracket)
    {
        this.infectionAgeWriter.print(this.model.getSimulator().getSimulatorTime());
        for (int i = 0; i < infectionsPerAgeBracket.length; i++)
        {
            this.infectionAgeWriter.print("," + infectionsPerAgeBracket[i]);
        }
        this.infectionAgeWriter.write("\n");
        this.infectionAgeWriter.flush();
    }

    private void writeDeathsAgeHeader()
    {
        this.deathsAgeWriter.print("\"Time(h)\"");
        for (int ageBracket = 0; ageBracket < 11; ageBracket++)
        {
            String bracket = (10 * ageBracket) + "-" + (10 * (ageBracket + 1) - 1);
            this.deathsAgeWriter.print(",\"" + bracket + "\"");
        }
        this.deathsAgeWriter.write("\n");
        this.deathsAgeWriter.flush();
    }

    private void writeDeathsAgeLine(final int[] deathsPerAgeBracket)
    {
        this.deathsAgeWriter.print(this.model.getSimulator().getSimulatorTime());
        for (int i = 0; i < deathsPerAgeBracket.length; i++)
        {
            this.deathsAgeWriter.print("," + deathsPerAgeBracket[i]);
        }
        this.deathsAgeWriter.write("\n");
        this.deathsAgeWriter.flush();
    }

    private void writePersonDumpHeader()
    {
        // this.personDumpWriter.println("\"Time(h)\",\"personId\",\"personType\",\"Age\",\"Gender\",\"homeId\",\"homeSubId\","
        // + "\"homeLat\",\"homeLon\",\"diseasePhase\",\"workId\",\"schoolId\"");

        this.personDumpWriter.println("\"Time(h)\",\"personId\",\"personType\",\"Age\",\"Gender\",\"homeId\",\"homeSubId\","
                + "\"currentActivity\"," + "\"currentLat\",\"currentLon\","
                + "\"homeLat\",\"homeLon\",\"diseasePhase\",\"workId\",\"schoolId\"");

        this.personDumpWriter.flush();
    }

    private void writePersonDump(final int personDumpInterval)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        for (Person person : this.model.getPersonMap().valueCollection())
        {
            this.personDumpWriter.print(time + "," + person.getId());
            this.personDumpWriter.print(",\"" + person.getClass().getSimpleName() + "\"");
            this.personDumpWriter.print("," + person.getAge());
            this.personDumpWriter.print("," + (person.getGenderFemale() ? "\"F\"" : "\"M\""));
            this.personDumpWriter.print("," + person.getHomeLocation().getId());
            this.personDumpWriter.print("," + person.getHomeSubLocationIndex());

            this.personDumpWriter.print("," + person.getCurrentActivity());

            Location currentLocation = this.model.getLocationMap().get(person.getCurrentLocation().getId());
            this.personDumpWriter.print("," + currentLocation.getLatitude());
            this.personDumpWriter.print("," + currentLocation.getLongitude());

            Location homeLocation = this.model.getLocationMap().get(person.getHomeLocation().getId());
            this.personDumpWriter.print("," + homeLocation.getLatitude());
            this.personDumpWriter.print("," + homeLocation.getLongitude());
            this.personDumpWriter.print(",\"" + person.getDiseasePhase().getName() + "\"");
            this.personDumpWriter.print("," + (person instanceof Worker ? ((Worker) person).getWorkLocation().getId() : -1));
            this.personDumpWriter
                    .print("," + (person instanceof Student ? ((Student) person).getSchoolLocation().getId() : -1));
            this.personDumpWriter.println();
        }
        this.personDumpWriter.flush();

        // this.model.getSimulator().scheduleEventRel(24.0 * personDumpInterval, this, this, "writePersonDump",
        // new Object[] { personDumpInterval });
        this.model.getSimulator().scheduleEventRel(1.0 * personDumpInterval, this, this, "writePersonDump",
                new Object[] {personDumpInterval});
    }

    private void writeInfectedPersonHeader()
    {
        this.infectedPersonWriter.println("\"Time(h)\",\"personId\",\"personType\",\"Age\",\"Gender\",\"homeId\",\"homeSubId\","
                + "\"homeLat\",\"homeLon\",\"diseasePhase\",\"workId\",\"schoolId\",\"infectLocationType\","
                + "\"infectLocationId\",\"infectLocationLat\",\"infectLocationLon\"");
        this.infectedPersonWriter.flush();
    }

    private void writeInfectedPersonLine(final Person person)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        this.infectedPersonWriter.print(time + "," + person.getId());
        this.infectedPersonWriter.print(",\"" + person.getClass().getSimpleName() + "\"");
        this.infectedPersonWriter.print("," + person.getAge());
        this.infectedPersonWriter.print("," + (person.getGenderFemale() ? "\"F\"" : "\"M\""));
        this.infectedPersonWriter.print("," + person.getHomeLocation().getId());
        this.infectedPersonWriter.print("," + person.getHomeSubLocationIndex());
        Location homeLocation = this.model.getLocationMap().get(person.getHomeLocation().getId());
        this.infectedPersonWriter.print("," + homeLocation.getLatitude());
        this.infectedPersonWriter.print("," + homeLocation.getLongitude());
        this.infectedPersonWriter.print(",\"" + person.getDiseasePhase().getName() + "\"");
        this.infectedPersonWriter.print("," + (person instanceof Worker ? ((Worker) person).getWorkLocation().getId() : -1));
        this.infectedPersonWriter
                .print("," + (person instanceof Student ? ((Student) person).getSchoolLocation().getId() : -1));
        Location infectLocation = person.getCurrentLocation();
        this.infectedPersonWriter.print(",\"" + infectLocation.getLocationType().getName() + "\"");
        this.infectedPersonWriter.print("," + infectLocation.getId());
        this.infectedPersonWriter.print("," + infectLocation.getLatitude());
        this.infectedPersonWriter.print("," + infectLocation.getLongitude());
        this.infectedPersonWriter.println();
        this.infectedPersonWriter.flush();
    }

    private void writeDeadPersonHeader()
    {
        this.deadPersonWriter.println("\"Time(h)\",\"personId\",\"personType\",\"Age\",\"Gender\",\"homeId\",\"homeSubId\","
                + "\"homeLat\",\"homeLon\",\"diseasePhase\",\"workId\",\"schoolId\"");
        this.deadPersonWriter.flush();
    }

    private void writeDeadPersonLine(final Person person)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        this.deadPersonWriter.print(time + "," + person.getId());
        this.deadPersonWriter.print(",\"" + person.getClass().getSimpleName() + "\"");
        this.deadPersonWriter.print("," + person.getAge());
        this.deadPersonWriter.print("," + (person.getGenderFemale() ? "\"F\"" : "\"M\""));
        this.deadPersonWriter.print("," + person.getHomeLocation().getId());
        this.deadPersonWriter.print("," + person.getHomeSubLocationIndex());
        Location homeLocation = this.model.getLocationMap().get(person.getHomeLocation().getId());
        this.deadPersonWriter.print("," + homeLocation.getLatitude());
        this.deadPersonWriter.print("," + homeLocation.getLongitude());
        this.deadPersonWriter.print(",\"" + person.getDiseasePhase().getName() + "\"");
        this.deadPersonWriter.print("," + (person instanceof Worker ? ((Worker) person).getWorkLocation().getId() : -1));
        this.deadPersonWriter.print("," + (person instanceof Student ? ((Student) person).getSchoolLocation().getId() : -1));
        this.deadPersonWriter.println();
        this.deadPersonWriter.flush();
    }

    /* ****************************** INFECTIONS PER PERSON TYPE **************************************** */

    private void writeDayInfPersonTypeHeader()
    {
        this.dayInfPersonWriter.print("\"time\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            PersonType pt = this.model.getPersonTypeList().get(i);
            this.dayInfPersonWriter.print(",\"" + pt.getName() + "\"");
        }
        this.dayInfPersonWriter.println();
        this.dayInfPersonWriter.flush();
    }

    private void writeTotInfPersonTypeHeader()
    {
        this.totInfPersonWriter.print("\"time\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            PersonType pt = this.model.getPersonTypeList().get(i);
            this.totInfPersonWriter.print(",\"" + pt.getName() + "\"");
        }
        this.totInfPersonWriter.println();
        this.totInfPersonWriter.flush();
    }

    private void writeDayInfPersonTypeLine(final int[] nrs)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        this.dayInfPersonWriter.print(Math.round(time));
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            this.dayInfPersonWriter.print("," + nrs[i]);
        }
        this.dayInfPersonWriter.println();
        this.dayInfPersonWriter.flush();
    }

    private void writeTotInfPersonTypeLine(final int[] nrs)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        this.totInfPersonWriter.print(Math.round(time));
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            this.totInfPersonWriter.print("," + nrs[i]);
        }
        this.totInfPersonWriter.println();
        this.totInfPersonWriter.flush();
    }

    /* ****************************** INFECTIONS FROM PERSON TYPE TO PERSON TYPE ********************************* */

    private void writeDayInfPersonToPersonTypeHeader()
    {
        this.dayInfPersonToPersonWriter.print("\"time\",\"infecting_person_type\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            PersonType pt = this.model.getPersonTypeList().get(i);
            this.dayInfPersonToPersonWriter.print(",\"" + pt.getName() + "\"");
        }
        this.dayInfPersonToPersonWriter.println();
        this.dayInfPersonToPersonWriter.flush();
    }

    private void writeTotInfPersonToPersonTypeHeader()
    {
        this.totInfPersonToPersonWriter.print("\"time\",\"infecting_person_type\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            PersonType pt = this.model.getPersonTypeList().get(i);
            this.totInfPersonToPersonWriter.print(",\"" + pt.getName() + "\"");
        }
        this.totInfPersonToPersonWriter.println();
        this.totInfPersonToPersonWriter.flush();
    }

    private void writeDayInfPersonToPersonTypeLine(final int[] nrs)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        String infectingPersonType = this.model.getPersonTypeList().get(nrs[0]).getName();
        this.dayInfPersonToPersonWriter.print(Math.round(time) + ",\"" + infectingPersonType + "\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            this.dayInfPersonToPersonWriter.print("," + nrs[i + 1]);
        }
        this.dayInfPersonToPersonWriter.println();
        this.dayInfPersonToPersonWriter.flush();
    }

    private void writeTotInfPersonToPersonTypeLine(final int[] nrs)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        String infectingPersonType = this.model.getPersonTypeList().get(nrs[0]).getName();
        this.totInfPersonToPersonWriter.print(Math.round(time) + ",\"" + infectingPersonType + "\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            this.totInfPersonToPersonWriter.print("," + nrs[i + 1]);
        }
        this.totInfPersonToPersonWriter.println();
        this.totInfPersonToPersonWriter.flush();
    }

    /* ****************************** INFECTIONS FROM PERSON TYPE TO PERSON TYPE ********************************* */

    private void writeDayInfLocPersonToPersonTypeHeader()
    {
        this.dayInfLocPersonToPersonWriter.print("\"time\",\"location_type\",\"infecting_person_type\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            PersonType pt = this.model.getPersonTypeList().get(i);
            this.dayInfLocPersonToPersonWriter.print(",\"" + pt.getName() + "\"");
        }
        this.dayInfLocPersonToPersonWriter.println();
        this.dayInfLocPersonToPersonWriter.flush();
    }

    private void writeTotInfLocPersonToPersonTypeHeader()
    {
        this.totInfLocPersonToPersonWriter.print("\"time\",\"location_type\",\"infecting_person_type\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            PersonType pt = this.model.getPersonTypeList().get(i);
            this.totInfLocPersonToPersonWriter.print(",\"" + pt.getName() + "\"");
        }
        this.totInfLocPersonToPersonWriter.println();
        this.totInfLocPersonToPersonWriter.flush();
    }

    private void writeDayInfLocPersonToPersonTypeLine(final int[] nrs)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        String locationType = this.model.getLocationTypeList().get(nrs[0]).getName();
        String infectingPersonType = this.model.getPersonTypeList().get(nrs[1]).getName();
        this.dayInfLocPersonToPersonWriter
                .print(Math.round(time) + ",\"" + locationType + "\",\"" + infectingPersonType + "\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            this.dayInfLocPersonToPersonWriter.print("," + nrs[i + 1]);
        }
        this.dayInfLocPersonToPersonWriter.println();
        this.dayInfLocPersonToPersonWriter.flush();
    }

    private void writeTotInfLocPersonToPersonTypeLine(final int[] nrs)
    {
        double time = this.model.getSimulator().getSimulatorTime();
        String locationType = this.model.getLocationTypeList().get(nrs[0]).getName();
        String infectingPersonType = this.model.getPersonTypeList().get(nrs[1]).getName();
        this.totInfLocPersonToPersonWriter
                .print(Math.round(time) + ",\"" + locationType + "\",\"" + infectingPersonType + "\"");
        int ptSize = this.model.getPersonTypeList().size();
        for (int i = 0; i < ptSize; i++)
        {
            this.totInfLocPersonToPersonWriter.print("," + nrs[i + 1]);
        }
        this.totInfLocPersonToPersonWriter.println();
        this.totInfLocPersonToPersonWriter.flush();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void notify(final EventInterface event) throws RemoteException
    {
        if (event.getType().equals(PersonMonitor.INFECT_ALL_LOCATIONTYPES_PER_HOUR_EVENT))
        {
            writeInfectionLocationLine((Map<LocationType, Integer>) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.INFECT_AGE_PER_HOUR_EVENT))
        {
            writeInfectionAgeLine((int[]) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.DEATHS_AGE_PER_DAY_EVENT))
        {
            writeDeathsAgeLine((int[]) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.INFECTED_PERSON_EVENT))
        {
            writeInfectedPersonLine((Person) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.DEAD_PERSON_EVENT))
        {
            writeDeadPersonLine((Person) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.DAY_INFECTIONS_PERSON_TYPE))
        {
            writeDayInfPersonTypeLine((int[]) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.TOT_INFECTIONS_PERSON_TYPE))
        {
            writeTotInfPersonTypeLine((int[]) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.DAY_INFECTIONS_PERSON_TO_PERSON_TYPE))
        {
            writeDayInfPersonToPersonTypeLine((int[]) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.TOT_INFECTIONS_PERSON_TO_PERSON_TYPE))
        {
            writeTotInfPersonToPersonTypeLine((int[]) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.DAY_INFECTIONS_LOC_PERSON_TO_PERSON_TYPE))
        {
            writeDayInfLocPersonToPersonTypeLine((int[]) event.getContent());
        }
        else if (event.getType().equals(PersonMonitor.TOT_INFECTIONS_LOC_PERSON_TO_PERSON_TYPE))
        {
            writeTotInfLocPersonToPersonTypeLine((int[]) event.getContent());
        }
    }

}
