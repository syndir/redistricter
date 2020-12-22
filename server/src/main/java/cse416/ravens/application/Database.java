package cse416.ravens.application;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.persistence.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.eclipse.persistence.config.*;

public class Database {
    private static final ReentrantLock lock = new ReentrantLock();
    private static Database db = null;
    private EntityManagerFactory emf;
    private EntityManager em;

    private Database() {
        emf = Persistence.createEntityManagerFactory("ravensPU");
        em = emf.createEntityManager();
    }

    public static Database getInstance() {
        if(db == null) {
            db = new Database();
        }
        return db;
    }

    private EntityManager getEntityManager() {
        return em;
    }

    private BigInteger getLastInsertedID() {
        BigInteger lastID = null;
        lastID = (BigInteger)em.createNativeQuery("SELECT LAST_INSERT_ID();").getSingleResult();
        DebugHelper.log("Last inserted ID=" + lastID);
        return lastID;
    }

    /* FOR ONE-OFFS */
    public Object performSingleStatementSelect(String query) {
        Object result = null;

        lock.lock();
        DebugHelper.log("SELECT QUERY IS: " + query);
        try {
            db.getEntityManager().getTransaction().begin();
            result = db.getEntityManager().createNativeQuery(query)
                .getResultList();
        }
        finally {
            db.getEntityManager().getTransaction().commit();
            lock.unlock();
        }
        return result;
    }

    public Integer performSingleStatementUpdate(String query) {
        Integer numRowsUpdated = 0;

        lock.lock();
        DebugHelper.log("UPDATE QUERY IS: " + query);
        try {
            db.getEntityManager().getTransaction().begin();
            numRowsUpdated = db.getEntityManager().createNativeQuery(query)
                .executeUpdate();
        }
        finally {
            db.getEntityManager().getTransaction().commit();
            lock.unlock();
        }
        return numRowsUpdated;
    }

    /* JOB DB METHODS */
    @SuppressWarnings("unchecked")
    public Map<Integer, Map<Status, Parameters>> findJobsByState(String state) {
        Map<Integer, Map<Status, Parameters>> jobResults = new HashMap<>();
        List<Object[]> results;

        DebugHelper.log("findJobsByState() for state=" + state);
        lock.lock();
        try {
            em.getTransaction().begin();
            results  = em.createNativeQuery("SELECT * FROM jobs WHERE job_status !='ABORTED' AND state='" + state + "';")
                .getResultList();
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return null;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        
        for(Object[] res : results) {
            Map<Status, Parameters> par = new HashMap<>();
            par.put(Status.valueOf(res[2].toString()), Database.getInstance().findParametersByID((Integer)res[7]));
            jobResults.put((Integer)res[0], par);
        }

        return jobResults;
    }

    @SuppressWarnings("unchecked")
    public Job findJobByID(Integer jobID) {
        Job job = null;
        Map<String, Object> result  = null;
        ObjectMapper objMapper = new ObjectMapper();

        DebugHelper.log("findJobByID() for JobID=" + jobID);
        lock.lock();
        try {
            em.getTransaction().begin();
            result = (Map<String, Object>)em.createNativeQuery("SELECT * FROM jobs WHERE job_id=?;")
                .setParameter(1, jobID)
                .setHint(QueryHints.RESULT_TYPE, ResultType.Map)
                .getSingleResult();
            DebugHelper.log("FOUND RESULT: " + result.toString());
        }
        catch (NoResultException e) {
            DebugHelper.log("NO MATCHING RESULT FOUND");
            return null;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }

        try {
            job = new Job();
            job.setJobID((Integer)result.get("job_id"));
            job.setStatus(Status.valueOf(result.get("job_status").toString()));
            job.setGeneratedDistrictingPlans(result.get("generated_plans") != null ? Arrays.asList(objMapper.readValue(result.get("generated_plans").toString(), Integer[].class)) : null);
            job.setAverageDistrictingPlan((Integer)result.get("average_plan"));
            job.setMaxDistrictingPlan((Integer)result.get("max_plan"));
            job.setMinDistrictingPlan((Integer)result.get("min_plan"));
            job.setRandomDistrictingPlan((Integer)result.get("random_plan"));
            job.setParametersID(Long.valueOf(result.get("parameters").toString()));
            job.setParameters(Database.getInstance().findParametersByID(job.getParametersID().intValue()));
            job.setSlurmJobNumber((Integer)result.get("slurm_job"));
            job.setLocalPID(result.get("local_pid") != null ? Long.valueOf(result.get("local_pid").toString()) : null);
            job.setBoxAndWhiskerData(result.get("baw_data") != null ? objMapper.readValue(result.get("baw_data").toString(), BoxAndWhiskerData.class) : null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return job;
    }

    public BigInteger insertJobIntoDB(Job job) {
        DebugHelper.log("inserting job into DB");
        BigInteger lastID = null;

        if(job == null) {
            return lastID;
        }
        lock.lock();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("INSERT INTO jobs (job_status, parameters, state) VALUES (?, ?, ?);")
                .setParameter(1, job.getStatus().name())
                .setParameter(2, job.getParametersID())
                .setParameter(3, job.getParameters().getState())
                .executeUpdate();
            lastID = getLastInsertedID();
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return lastID;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return lastID;
    }

    public Boolean removeJobFromDB(Job job) {
        Database db = Database.getInstance();
        Integer numRowsDeleted;

        if(job == null) {
            DebugHelper.log("removeJobFromDB() called for null job!");
            return false;
        }
        DebugHelper.log("removeJobFromDB() for JobID=" + job.getJobID());
        if(job.getParameters() != null) {
            removeParametersFromDB(job.getParameters());
        }
        if(job.getGeneratedDistrictingPlans() != null) {
            for(Integer planNumberToDelete : job.getGeneratedDistrictingPlans()) {
                DistrictingPlan planToDelete = db.findDistrictingPlanByID(planNumberToDelete);
                if(planToDelete != null) {
                    db.removeDistrictingPlanFromDB(planToDelete);
                }
            }
        }
        lock.lock();
        try {
            em.getTransaction().begin();
            DebugHelper.log("Deleting job_id=" + job.getJobID() + " from jobs table");
            numRowsDeleted = em.createNativeQuery("DELETE FROM jobs WHERE job_id=?;")
                .setParameter(1, job.getJobID())
                .executeUpdate();
            DebugHelper.log(numRowsDeleted + " rows deleted");
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return false;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return true;
    }

    /* PARAMETERS DB METHODS */
    @SuppressWarnings("unchecked")
    public Parameters findParametersByID(Integer id) {
        Parameters parameters = null;
        ObjectMapper objMapper = new ObjectMapper();
        Map<String, Boolean> demosAsStrings = null;
        Map<Demographic, Boolean> demos = null;
        Map<String, Object> paramResult = null;

        DebugHelper.log("findParametersByID() for id=" + id);
        lock.lock();
        try {
            em.getTransaction().begin();
            paramResult = (Map<String, Object>)em.createNativeQuery("SELECT * FROM parameters WHERE id=?;")
                .setParameter(1, id)
                .setHint(QueryHints.RESULT_TYPE, ResultType.Map)
                .getSingleResult();
        }
        catch (NoResultException e) {
            DebugHelper.log("NO MATCHING RESULT FOUND");
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }

        try {
            /* since jackson can't deserialize a map to an enum-typed key, we have to read it originally as a String and convert it. so dumb. */
            demosAsStrings = objMapper.readValue(paramResult.get("selected_demos").toString(), Map.class);
            demos = new HashMap<>();
            for(Entry<String, Boolean> entry : demosAsStrings.entrySet()) {
                demos.put(Demographic.valueOf(entry.getKey()), entry.getValue());
            }
            parameters = new Parameters(id,
                    paramResult.get("state").toString(),
                    demos,
                    (Integer)paramResult.get("num_seeds"),
                    (Integer)paramResult.get("num_iters"),
                    CompactnessMeasure.valueOf(paramResult.get("compactness_measure").toString()),
                    ((Float)paramResult.get("pop_variance")).doubleValue());
            Float var = (Float)paramResult.get("pop_variance");
            Integer varI = Float.valueOf((float)(var * 1000.0)).intValue();
            parameters.setPopulationVariance(varI.doubleValue() / 1000.0);
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
        }
        return parameters;
    }

    public BigInteger insertParametersIntoDB(Parameters parameters) {
        DebugHelper.log("inserting parameters into DB");
        BigInteger lastID = null;

        if(parameters == null) {
            return lastID;
        }

        lock.lock();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                    "INSERT INTO parameters (state, compactness_measure, pop_variance, selected_demos, num_seeds, num_iters) VALUES (?, ?, ?, ?, ?, ?);")
                .setParameter(1, parameters.getState())
                .setParameter(2, parameters.getCompactnessMeasure().name())
                .setParameter(3, parameters.getPopulationVariance())
                .setParameter(4, new ObjectMapper().writeValueAsString(parameters.getSelectedDemographics()))
                .setParameter(5, parameters.getNumSeedPlans())
                .setParameter(6, parameters.getNumIterations())
                .executeUpdate();
            lastID = getLastInsertedID();
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return lastID;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return lastID;
    }

    public Boolean removeParametersFromDB(Parameters parameters) {
        if(parameters == null) {
            DebugHelper.log("removeParametersFromDB() for null plan!");
            return false;
        }
        DebugHelper.log("removeParametersFromDB() for parametersID=" + parameters.getId());
        lock.lock();
        try {
            Integer numRowsDeleted = null;

            em.getTransaction().begin();
            numRowsDeleted = em.createNativeQuery("DELETE FROM parameters WHERE id=?;")
                .setParameter(1, parameters.getId())
                .executeUpdate();
            DebugHelper.log(numRowsDeleted + " rows deleted");
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return false;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return true;
    }


    /* DISTRICTINGPLAN DB METHODS */
    @SuppressWarnings("unchecked")
    public DistrictingPlan findDistrictingPlanByID(Integer districtingPlanID) {
        DistrictingPlan dp = null;
        Map<String, Object> result = null;
        ObjectMapper objMapper = new ObjectMapper();

        DebugHelper.log("findDistrictingPlanByID() for ID=" + districtingPlanID);
        lock.lock();
        try {
            em.getTransaction().begin();
            result = (Map<String, Object>)em.createNativeQuery("SELECT * FROM districting_plans WHERE id=?;")
                .setParameter(1, districtingPlanID)
                .setHint(QueryHints.RESULT_TYPE, ResultType.Map)
                .getSingleResult();
            DebugHelper.log("FOUND RESULT: " + result.toString());
            dp = new DistrictingPlan();
            dp.setId(districtingPlanID);
            dp.setVariance(result.get("variance") != null ? Float.valueOf(result.get("variance").toString()).doubleValue() : null);
            dp.setDistricts(result.get("districts") != null ? Arrays.asList(objMapper.readValue(result.get("districts").toString(), Integer[].class)) : null);
            dp.setSortedDistricts(result.get("sorted_districts") != null ? Arrays.asList(objMapper.readValue(result.get("sorted_districts").toString(), Integer[].class)) : null);
            dp.setGeoJSON(result.get("geojson") != null ? result.get("geojson").toString() : null);
        }
        catch (NoResultException e) {
            DebugHelper.log("NO MATCHING RESULT FOUND");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return dp;
    }

    public BigInteger insertDistrictingPlanIntoDB(DistrictingPlan dp) {
        ObjectMapper objectMapper = new ObjectMapper();
        BigInteger lastID = null;

        if(dp == null) {
            DebugHelper.log("insertDistrictingPlanIntoDB() for null plan!");
            return lastID;
        }

        lock.lock();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                    "INSERT INTO districting_plans (districts, sorted_districts) VALUES (?, ?);")
                .setParameter(1, objectMapper.writeValueAsString(dp.getDistricts()))
                .setParameter(2, objectMapper.writeValueAsString(dp.getSortedDistricts()))
                .executeUpdate();
            lastID = getLastInsertedID();
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return lastID;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return lastID;
    }

    public Boolean removeDistrictingPlanFromDB(DistrictingPlan dp) {
        Database db = Database.getInstance();

        if(dp == null) {
            DebugHelper.log("removeDistrictingPlanFromDB() for null plan!");
            return false;
        }
        DebugHelper.log("removeDistrictingPlanFromDB() for districtPlanID=" + dp.getId());
        if(dp.getDistricts() != null) {
            for(Integer districtNumberToRemove : dp.getDistricts()) {
                District districtToRemove = db.findDistrictByID(districtNumberToRemove);
                if(districtToRemove != null) {
                    db.removeDistrictFromDB(districtToRemove);
                }
            }
        }
        lock.lock();
        try {
            Integer numRowsDeleted = null;

            em.getTransaction().begin();
            numRowsDeleted = em.createNativeQuery("DELETE FROM districting_plans WHERE id=?;")
                .setParameter(1, dp.getId())
                .executeUpdate();
            DebugHelper.log(numRowsDeleted + " rows deleted");

        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return false;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return true;
    }

    /* DISTRICT DB METHODS */
    @SuppressWarnings("unchecked")
    private District findDistrictByField(Integer identifier, String fieldName) {
        District district = null;
        Map<String, Object> result = null;
        Map<String, Integer> demosAsStrings = null;
        Map<Demographic, Integer> demos = null;
        ObjectMapper objMapper = new ObjectMapper();
        String query = "SELECT * FROM districts WHERE " + fieldName + "=?;";

        lock.lock();
        try {
            em.getTransaction().begin();
            result = (Map<String, Object>)em.createNativeQuery(query)
                .setParameter(1, identifier)
                .setHint(QueryHints.RESULT_TYPE, ResultType.Map)
                .getSingleResult();
            DebugHelper.log("FOUND RESULT: " + result.toString());
            district = new District();
            district.setDistrictID(Integer.valueOf(result.get("id").toString()));
            // district.setName(result.get("name") != null ? Integer.valueOf(result.get("name").toString()) : null);
            district.setPrecincts(result.get("precincts") != null ? Arrays.asList(objMapper.readValue(result.get("precincts").toString(), String[].class)) : null);
            district.setNeighboringDistricts(result.get("neighboring_districts") != null ? Arrays.asList(objMapper.readValue(result.get("neighboring_districts").toString(), Integer[].class)) : null);
            district.setTotalPopulation((Integer)result.get("total_pop"));
            district.setCompactness(result.get("pop_variance") != null ? ((Float)result.get("pop_variance")).doubleValue() : null);
            district.setNumberOfCounties(result.get("num_counties") != null ? (Integer)result.get("num_counties") : null);
            district.setRealGeoJSON(result.get("real_geojson") != null ? result.get("real_geojson").toString() : null);
            district.setClientGeoJSON(result.get("client_geojson") != null ? result.get("client_geojson").toString() : null);

            demosAsStrings = result.get("total_pop_by_demo") != null ? objMapper.readValue(result.get("total_pop_by_demo").toString(), Map.class) : new HashMap<>();
            demos = new HashMap<>();
            for(Entry<String, Integer> entry : demosAsStrings.entrySet()) {
                demos.put(Demographic.valueOf(entry.getKey()), entry.getValue());
            }
            district.setTotalPopulationByDemographic(demos);
            demosAsStrings = result.get("vap_pop_by_demo") != null ? objMapper.readValue(result.get("vap_pop_by_demo").toString(), Map.class) : new HashMap<>();
            demos = new HashMap<>();
            for(Entry<String, Integer> entry : demosAsStrings.entrySet()) {
                demos.put(Demographic.valueOf(entry.getKey()), entry.getValue());
            }
            district.setVotingAgePopulationByDemographic(demos);
        }
        catch (NoResultException e) {
            DebugHelper.log("NO MATCHING RESULT FOUND");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return district;
    }

    // public District findDistrictByName(Integer districtName) {
    //     DebugHelper.log("findDistrictByName() for name=" + districtName);
    //     return Database.getInstance().findDistrictByField(districtName, "name");
    // }

    public District findDistrictByID(Integer districtID) {
        DebugHelper.log("findDistrictByID() for ID=" + districtID);
        return Database.getInstance().findDistrictByField(districtID, "id");
    }

    public Boolean removeDistrictFromDB(District district) {
        if(district == null) {
            DebugHelper.log("removeDistrictFromDB() for null district!");
            return false;
        }

        DebugHelper.log("removeDistrictFromDB() for districtID=" + district.getDistrictID());
        lock.lock();
        try {
            Integer numRowsDeleted = null;

            em.getTransaction().begin();
            numRowsDeleted = em.createNativeQuery("DELETE FROM districts WHERE id=?;")
                .setParameter(1, district.getDistrictID())
                .executeUpdate();
            DebugHelper.log(numRowsDeleted + " rows deleted");
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return false;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return true;
    }

    public BigInteger insertDistrictIntoDB(District district) {
        ObjectMapper objectMapper = new ObjectMapper();
        BigInteger lastID = null;

        if(district == null) {
            DebugHelper.log("insertDistrictIntoDB() for null plan!");
            return null;
        }
        DebugHelper.log("insertDistrictIntoDB() for districtID=" + district.getDistrictID());
        lock.lock();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                    "INSERT INTO districts (precincts, total_pop, num_counties, total_pop_by_demo, vap_pop_by_demo) VALUES (?, ?, ?, ?, ?);")
                .setParameter(1, objectMapper.writeValueAsString(district.getPrecincts()))
                .setParameter(2, district.getTotalPopulation())
                .setParameter(3, district.getNumberOfCounties())
                .setParameter(4, objectMapper.writeValueAsString(district.getTotalPopulationByDemographic()))
                .setParameter(5, objectMapper.writeValueAsString(district.getVotingAgePopulationByDemographic()))
                .executeUpdate();
            lastID = getLastInsertedID();
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            return lastID;
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return lastID;
    }

    /* PRECINCT DB METHODS */
    @SuppressWarnings("unchecked")
    public Precinct findPrecinctByID(String precinctID) {
        Precinct precinct = null;
        Map<String, Integer> demosAsStrings = null;
        Map<Demographic, Integer> demos = null;
        Map<String, Object> result  = null;
        ObjectMapper objMapper = new ObjectMapper();

        // DebugHelper.log("findPrecinctByID() for precinctID=" + precinctID);
        lock.lock();
        try {
            em.getTransaction().begin();
            result = (Map<String, Object>)em.createNativeQuery("SELECT * FROM precincts WHERE precinct_id=?;")
                .setParameter(1, precinctID)
                .setHint(QueryHints.RESULT_TYPE, ResultType.Map)
                .getSingleResult();
            // DebugHelper.log("FOUND RESULT: " + result.toString());
            precinct = new Precinct();
            precinct.setPrecinctID(result.get("precinct_id").toString());
            precinct.setPrecinctName(result.get("precinct_name").toString());
            precinct.setCountyName(result.get("county_name").toString());
            precinct.setCountyID(Integer.valueOf(result.get("county_id").toString()));
            precinct.setBoundary(result.get("boundary").toString());
            demosAsStrings = objMapper.readValue(result.get("total_pop_by_demo").toString(), Map.class);
            demos = new HashMap<>();
            for(Entry<String, Integer> entry : demosAsStrings.entrySet()) {
                demos.put(Demographic.valueOf(entry.getKey()), entry.getValue());
            }
            precinct.setTotalPopulations(demos);
            demosAsStrings = objMapper.readValue(result.get("vap_pop_by_demo").toString(), Map.class);
            demos = new HashMap<>();
            for(Entry<String, Integer> entry : demosAsStrings.entrySet()) {
                demos.put(Demographic.valueOf(entry.getKey()), entry.getValue());
            }
            precinct.setVotingAgePopulations(demos);
            precinct.setPrecinctNeighbors(objMapper.readValue(result.get("precinct_neighbors").toString(), List.class));
        }
        catch (NoResultException e) {
            DebugHelper.log("NO MATCHING RESULT FOUND");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return precinct;
    }

    /* STATE DB METHODS */
    @SuppressWarnings("unchecked")
    public State findStateByID(String stateName) {
        State state = null;
        Map<String, Object> result = null;

        DebugHelper.log("findStateByID() for state name=" + stateName);
        lock.lock();
        try {
            if(!em.getTransaction().isActive())
                em.getTransaction().begin();
            result = (Map<String, Object>)em.createNativeQuery("SELECT * FROM states WHERE state_selection=?;")
                .setHint(QueryHints.RESULT_TYPE, ResultType.Map)
                .setParameter(1, stateName.toUpperCase())
                .getSingleResult();
            DebugHelper.log("DB returned " + result.toString());
            state = new State();
            state.setState(StateSelection.valueOf(result.get("state_selection").toString()));
            state.setCurrentlyEnactedPlan((Integer)result.get("enacted_plan"));
            state.setPrecincts(Arrays.asList(new ObjectMapper().readValue(result.get("precincts").toString(), String[].class)));
        }
        catch (NoResultException e) {
            DebugHelper.log("STATE NOT FOUND IN DB!");
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        finally {
            em.getTransaction().commit();
            lock.unlock();
        }
        return state;
    }
}
