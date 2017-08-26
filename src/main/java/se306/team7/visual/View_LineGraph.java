package se306.team7.visual;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import se306.team7.Metrics;

public class View_LineGraph implements ITaskSchedulerView {

    public LineChart<Number,Number> _lineChart;
    private static View_LineGraph _view_lineGraph;
    private static int currentTimeUnit = 0;
    private NumberAxis _xAxis;
    private static double currentTime;

    private View_LineGraph() {
        //defining the axes
        _xAxis = new NumberAxis(0, 5, 0.5);
        _xAxis.setLabel("Time (seconds)");
        _xAxis.setAutoRanging(false);
        NumberAxis yAxis = new NumberAxis(1, Metrics.getLevels(), 1);
        yAxis.setLabel("Current level");

        //creating the chart
        _lineChart = new LineChart<Number,Number>(_xAxis, yAxis);
        _lineChart.setTitle("Current level explored for each core");
        _lineChart.setAnimated(true);
        _lineChart.setLegendVisible(true);
        _lineChart.setCreateSymbols(false);

        //Preparing and adding the initial data
       for (int i = 1; i <= Metrics.getNumOfCores(); i++) {
            XYChart.Series<Number, Number> newCoreSeries = new XYChart.Series<Number, Number>();
            newCoreSeries.setName("Core " + i);
            newCoreSeries.getData().add(new XYChart.Data<Number, Number>(0, 0));
            _lineChart.getData().add(newCoreSeries);
        }
    }
    
    public static View_LineGraph getInstance(){
    	if (_view_lineGraph == null){
    		_view_lineGraph = new View_LineGraph();
    	}
    	
    	return _view_lineGraph;
    }

    /**
     * Updates the line graph by adding a new data point for each series.
     * Each series corresponds to a core.
     * Each data point in the series represents the level of the solution tree at which the core was operating on at that moment in time
     * The level of the solution tree corresponds to the number of tasks that have been scheduled in the (partial) solution.
     *
     * If the new X value is closely approaching the X axis' upper bound, then recalibrate the X axis to twice its current size.
     * @param currentBestCost
     * @param histogram
     * @param coreCurrentLevel
     */
    public void update( int currentBestCost, ConcurrentHashMap<Integer, Integer> histogram, ConcurrentHashMap<Integer, Integer> coreCurrentLevel) {

        currentTime = (System.currentTimeMillis() - TaskSchedulerGUI._startTime)/1000.00;
        System.out.println(currentTime);
        double currentXAxisUpperBound = _xAxis.getUpperBound();
        if (currentTime > (currentXAxisUpperBound * 0.8)) {
            _xAxis.setUpperBound(Math.ceil(currentXAxisUpperBound * 1.5));
        }

        for (Map.Entry<Integer, Integer> entry : coreCurrentLevel.entrySet()) {
            _lineChart.getData().get(entry.getKey()-1) //get the series for the corresponding core
                    .getData().add(new XYChart.Data<Number, Number>(currentTime, entry.getValue())); //add new Y value for the current time unit
        }

    }
}
