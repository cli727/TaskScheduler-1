package se306.team7.Algorithm;

import pt.runtime.TaskID;
import pt.runtime.TaskIDGroup;
import pt.runtime.TaskInfo;
import pt.runtime.TaskpoolFactory;
import se306.team7.CostEstimatedSchedule;
import se306.team7.Digraph.Digraph;
import se306.team7.Schedule;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AStarDfsHybrid {
    private static PriorityBlockingQueue<CostEstimatedSchedule> _schedules;
    private static Digraph _digraph;
    private static Set<ICostEstimator> _costEstimators;
    private static IScheduleGenerator _scheduleGenerator;

    private static AtomicReference<Schedule> _bestSchedule;
    private static AtomicInteger _bestCost;
    private int _numOfCores = 4;

    public AStarDfsHybrid (Set<ICostEstimator> costEstimators, IScheduleGenerator scheduleGenerator) {
        _schedules = new PriorityBlockingQueue<CostEstimatedSchedule>();
        _costEstimators = costEstimators;
        _scheduleGenerator = scheduleGenerator;
        _bestSchedule = new AtomicReference<Schedule>();
        _bestCost = new AtomicInteger(Integer.MAX_VALUE);
    }

    public Schedule getOptimalSchedule(Digraph digraph, int numOfProcessors) {
        Schedule schedule = new Schedule(numOfProcessors);
        return getOptimalSchedule(digraph, numOfProcessors, schedule);
    }

    public Schedule getOptimalSchedule(Digraph digraph, int numOfProcessors, Schedule schedule) {
        _digraph = digraph;
        ValidScheduleGenerator v = new ValidScheduleGenerator();
        Schedule knownSchedule = v.generateValidSchedule(digraph, numOfProcessors);
        _bestSchedule.set(knownSchedule);
        _bestCost.set(knownSchedule.endTime());
        CostEstimatedSchedule emptySchedule = new CostEstimatedSchedule(schedule, Integer.MAX_VALUE);
        _schedules.add(emptySchedule);

        pollAndGenerateSchedules(10);

        try {
            Method aStarAlgorithmMethod = AStarDfsHybrid.class.getMethod("pollAndGenerateSchedules", int.class);

            TaskIDGroup<Void> id = new TaskIDGroup<Void>(_numOfCores);

            for (int i = 0; i < _numOfCores; i++) {

                TaskInfo taskInfo = new TaskInfo();
                taskInfo.setMethod(aStarAlgorithmMethod);
                taskInfo.setParameters(-1);

                TaskID<Void> task = TaskpoolFactory.getTaskpool().enqueue(taskInfo);

                id.add(task);
            }

            id.waitTillFinished();

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException ex) {
            System.exit(1); // Something has gone horribly wrong
        }
        return _bestSchedule.get();
    }

    /**
     * Cost estimate of a schedule is given by the maximum out of (the latest task end time) or
     * (newestAddedTask's start time plus its bottom level)
     * @param schedule
     * @return
     */
    public static int getCostEstimate(Schedule schedule) {

        int currentMax = schedule.getLastTaskScheduled().getEndTime();

        for (ICostEstimator costEstimator : _costEstimators) {
            currentMax = Math.max(currentMax, costEstimator.estimateCost(schedule, _digraph));
        }

        return currentMax;
    }

    public static Schedule getOptimalScheduleDfs(Digraph digraph, int numOfProcessors, Schedule schedule) {
        _digraph = digraph;

        List<Schedule> nextSchedules = _scheduleGenerator.generateSchedules(schedule, digraph);

        if (nextSchedules.isEmpty()) {
            if (schedule.endTime() < _bestCost.get()) {
                _bestCost.set(schedule.endTime());
                _bestSchedule.set(schedule);
            }

            return schedule;
        }

        List<CostEstimatedSchedule> costEstimatedSchedules = new ArrayList<CostEstimatedSchedule>();

        for (Schedule nextSchedule : nextSchedules) {
            int costEstimate = getCostEstimate(nextSchedule);
            if (costEstimate < _bestCost.get())
                costEstimatedSchedules.add(new CostEstimatedSchedule(nextSchedule, costEstimate));
        }

        if (costEstimatedSchedules.isEmpty()) {
            return null;
        }

        Collections.sort(costEstimatedSchedules);

        Schedule bestSchedule = null;

        for (CostEstimatedSchedule nextSchedule : costEstimatedSchedules) {
            Schedule s = getOptimalScheduleDfs(digraph, numOfProcessors, nextSchedule.getSchedule());

            if (s == null)
                continue;

            if (bestSchedule == null || s.endTime() < bestSchedule.endTime()) {
                bestSchedule = s;
            }
        }

        return bestSchedule;
    }

    /**
     * Polls the priority queue for a best schedule so far and generates its children
     * @param loopThreshold
     */
    public static void pollAndGenerateSchedules (int loopThreshold) {
        while (true) {



            CostEstimatedSchedule polledSchedule = _schedules.poll();



            if (polledSchedule == null) {
                return;
            }

            Schedule mostPromisingSchedule =  polledSchedule.getSchedule();

            System.out.println(mostPromisingSchedule.getTasks().size());

            if (mostPromisingSchedule.endTime() >= _bestCost.get()) {
                return;
            }

            if (mostPromisingSchedule.getTasks().size() >= 9) {

                getOptimalScheduleDfs(_digraph, polledSchedule.getSchedule()._numOfProcessors, polledSchedule.getSchedule());

            } else {

                List<Schedule> possibleSchedules = _scheduleGenerator.generateSchedules(mostPromisingSchedule, _digraph);

                if (possibleSchedules.isEmpty()) {
                    if (mostPromisingSchedule.endTime() < _bestCost.get()) {
                        _bestCost.set(mostPromisingSchedule.endTime());
                        _bestSchedule.set(mostPromisingSchedule);
                    }
                    return;
                }

                for (Schedule _schedule : possibleSchedules) {
                    int cost = Math.max(getCostEstimate(_schedule), mostPromisingSchedule.endTime());
                    if (cost < _bestCost.get()) {
                        CostEstimatedSchedule costEstimatedSchedule = new CostEstimatedSchedule(_schedule, cost);
                        _schedules.add(costEstimatedSchedule);
                    }
                }
                if (loopThreshold == 0) {
                    return;
                }
                loopThreshold--;
            }
        }
    }

    public Schedule run (Digraph digraph, int numOfProcessors, int numOfCores)  {
        _numOfCores = numOfCores;
        return getOptimalSchedule(digraph, numOfProcessors);
    }
}
