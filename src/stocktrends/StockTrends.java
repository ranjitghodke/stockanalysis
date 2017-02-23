/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stocktrends;

import com.opencsv.CSVReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
        scene2 = new Scene(grid2, 1000, 1000);
        
        
        btn.setText("Analyze Stock History");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                companySelected = textField.getText();
                currentStage.setScene(scene2);
                try {
                    Stock[] companyStockData = getStockData(companySelected);
                    Stock[] yearlyStockData = averageStockByYear(companyStockData);
                    createGraph(yearlyStockData);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(StockTrends.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(StockTrends.class.getName()).log(Level.SEVERE, null, ex);
                }
                
        }
        });
        
        
        grid1.add(textField, 0, 0);
        grid1.add(btn, 0, 1);
        
        currentStage.setTitle("Stock Market Analysis");
        currentStage.setScene(scene1);
        currentStage.show();
    }

    private Stock[] getStockData(String companyName) throws FileNotFoundException, IOException{
        
        List<Stock> stockData = new ArrayList<>();      
                
        String yahooUrl = "http://real-chart.finance.yahoo.com/table.csv?s=";
        String completedYahooUrl = yahooUrl + companyName;
        URL csvUrl = new URL(completedYahooUrl);      
        
        CSVReader reader = new CSVReader(new InputStreamReader(csvUrl.openStream()));
        
        String [] nextLine = reader.readNext(); //Discard column headers 
        while ((nextLine = reader.readNext()) != null) {
            // nextLine[] is an array of values from the line
            Stock stock = new Stock((nextLine[0]),
                    new BigDecimal(nextLine[1]), new BigDecimal(nextLine[2]),
                    new BigDecimal(nextLine[3]), new BigDecimal(nextLine[4]),
                    new BigDecimal(nextLine[5]), new BigDecimal(nextLine[6]));
            stockData.add(stock);

        }
        
        Stock[] answer = stockData.toArray(new Stock[stockData.size()]);
        
        return answer;
    }
    
    public void createGraph(Stock[] stockData){
        final NumberAxis xAxis = new NumberAxis(stockData[stockData.length - 1].getYear(), stockData[0].getYear(), 1);
        System.out.println(xAxis.getTickLabelFormatter());
        final DecimalFormat format = new DecimalFormat("####");
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Number of Years");
        //creating the chart
        final LineChart<Number,Number> lineChart = 
                new LineChart<Number,Number>(xAxis,yAxis);
                
        lineChart.setTitle("Stock Monitoring");
        //defining a series
        XYChart.Series series = new XYChart.Series();
        series.setName("Portfolio");
    
        for(Stock s:stockData){
            series.getData().add(new XYChart.Data(s.getYear(), s.getOpen()));
        }
        
        //populating the series with data
        Scene scene  = new Scene(lineChart,1000,1000);
        lineChart.getData().add(series);
        
        currentStage.setScene(scene);
    }
    
    private Stock[] averageStockByYear(Stock[] stockData){
        List<Stock> stockDataByYear = new ArrayList<>();
        int count = 0; //Incremented for every entry that has the same year as the previous
        BigDecimal average = new BigDecimal(0); //Variable used to compute the average from value/count
        BigDecimal value = new BigDecimal(0); //Variable used to add the value of the daily stocks in order to compute the yearly average
        for(int i = 0; i<stockData.length; i++){
            int currentYear = stockData[i].getYear();
            for(int j = i; j<stockData.length; j++){
                //If the entry is the same year, add to value and increment count
                if(stockData[j].getYear() == currentYear){
                    value = value.add(stockData[j].getOpen());
                    count++;
                }
                //If it is a new year, compute the yearly stock average
                if(stockData[j].getYear() != currentYear) {
                    average = value.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                    Stock yearStock = new Stock(currentYear, average);
                    stockDataByYear.add(yearStock);
                    value = new BigDecimal(0);
                    count = 0;
                    break;
                }
                i=j;
                //For the last section of years
                if(i == stockData.length - 1){
                    average = value.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                    Stock yearStock = new Stock(currentYear, average);
                    stockDataByYear.add(yearStock);
                    count = 0;
                    break;
                }
            }
        }
        return stockDataByYear.toArray(new Stock[stockDataByYear.size()]);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
