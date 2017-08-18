package se306.team7.visual;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TaskSchedulerGUI extends Application {

	@Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Task Scheduler");

        BorderPane root = new BorderPane();

        View_Histogram hist = new View_Histogram();
        View_LineGraph lineGraph = new View_LineGraph();
        View_CurrentBest currentBest = new View_CurrentBest();

        VBox leftVBox = new VBox(hist._barChart);
        VBox rightVBox = new VBox(currentBest._text, lineGraph._lineChart);
        root.setLeft(leftVBox);
        root.setRight(rightVBox);
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);

        primaryStage.show();
    }
	
	public static void main(String[] args) {
        launch(args);
    }

}