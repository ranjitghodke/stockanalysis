/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stocktrends;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author rghodke
 */
public class StockTrends extends Application {
    
    private Stage currentStage;
    private Scene scene1, scene2;
    private GridPane grid1, grid2;
    private String companySelected;
    
    
    @Override
    public void start(Stage primaryStage) {
        
        TextField textField = new TextField();
        Button btn = new Button();
        
        grid1 = new GridPane();
        grid1.setAlignment(Pos.CENTER);
        grid1.setHgap(10);
        grid1.setVgap(10);
        grid1.setPadding(new Insets(25, 25, 25, 25));
        
        grid2 = new GridPane();
        grid2.setAlignment(Pos.CENTER);
        grid2.setHgap(10);
        grid2.setVgap(10);
        grid2.setPadding(new Insets(25, 25, 25, 25));
        
        currentStage = primaryStage;
        scene1 = new Scene(grid1, 300, 250);
        scene2 = new Scene(grid2, 500, 500);
        
        
        btn.setText("Analyze Stock History");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                companySelected = textField.getText();
                currentStage.setScene(scene2);
                
        }
        });
        
        
        grid1.add(textField, 0, 0);
        grid1.add(btn, 0, 1);
        
        currentStage.setTitle("Stock Market Analysis");
        currentStage.setScene(scene1);
        currentStage.show();
    }

    private void getStockData(String companyName){
        String yahooUrl = "http://real-chart.finance.yahoo.com/table.csv?s=";
        String csvUrl = yahooUrl + companyName;
        
         
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
