package se306.team7.Algorithm;

import se306.team7.CostEstimatedSchedule;
import se306.team7.Metrics;
import se306.team7.Digraph.Digraph;
import se306.team7.Schedule;

import java.util.*;
import java.util.PriorityQueue;
import java.util.List;

public class AStarAlgorithm implements IAlgorithm {

    private PriorityQueue <CostEstimatedSchedule> _schedules;
    private Digraph _digraph;
    private Set<ICostEstimator> _costEstimators;
    private IScheduleGenerator _scheduleGenerator;

    public AStarAlgorithm (Set<ICostEstimator> costEstimators, IScheduleGenerator scheduleGenerator) {
        _schedules = new PriorityQueue<CostEstimatedSchedule>();
        _costEstimators = costEstimators;
        _scheduleGenerator = scheduleGenerator;
    }

    public Schedule getOptimalSchedule(Digraph digraph, int numOfProcessors, Schedule schedule) {
        _schedules.clear();
        _digraph = digraph;

        ValidScheduleGenerator v = new ValidScheduleGenerator();
        int knownScheduleFinishTime = v.generateValidSchedule(digraph, numOfProcessors).endTime();

        CostEstimatedSchedule emptySchedule = new CostEstimatedSchedule(schedule, Integer.MAX_VALUE);

        _schedules.add(emptySchedule);
        
        while(true){
            Schedule mostPromisingSchedule =  _schedules.poll().getSchedule();
            List<Schedule> possibleSchedules = _scheduleGenerator.generateSchedules(mostPromisingSchedule, digraph);

        	Metrics.setCurrentBestCost(mostPromisingSchedule.endTime()); //bogus code
            if(possibleSchedules.isEmpty()) {
                return mostPromisingSchedule;
            }

            for(Schedule _schedule : possibleSchedules){

                int cost = Math.max(getCostEstimate(_schedule), mostPromisingSchedule.endTime());
                if (cost <= knownScheduleFinishTime) {
                    CostEstimatedSchedule costEstimatedSchedule = new CostEstimatedSchedule(_schedule, cost);
                    Metrics.doneSchedule(costEstimatedSchedule, costEstimatedSchedule.getSchedule().getNumberOfProcessors()); // bogus code
                    _schedules.add(costEstimatedSchedule);
                }
            }
        }
    }

    /**
     * Get the optimal schedule for a given digraph containing tasks and task dependencies
     * @param digraph Represents tasks and task dependencies
     * @param numOfProcessors Processors available to concurrently complete tasks
     * @return Optimal complete schedule
     */
    public Schedule getOptimalSchedule(Digraph digraph, int numOfProcessors) {
        Schedule schedule = new Schedule(numOfProcessors);

        return getOptimalSchedule(digraph, numOfProcessors, schedule);
    }

    /**
     * Cost estimate of a schedule is given by the maximum out of (the latest task end time) or
     * (newestAddedTask's start time plus its bottom level)
     * @param schedule
     * @return
     */
    public int getCostEstimate(Schedule schedule) {

        int currentMax = schedule.getLastTaskScheduled().getEndTime();

        for (ICostEstimator costEstimator : _costEstimators) {
            currentMax = Math.max(currentMax, costEstimator.estimateCost(schedule, _digraph));
        }

        return currentMax;
    }
}