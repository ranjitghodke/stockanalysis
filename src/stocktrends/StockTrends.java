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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 *
 * @author rghodke
 */
public class StockTrends extends Application {

    //Private variables that are used throughout the program
    private Stage currentStage;
    private Scene scene1, scene2;
    private GridPane grid1, grid2;
    private String companySelected;

    private Stock[] companyStockData;
    
    //The selected time frame to display analysis for
    private static enum TIMEFRAME{
        Daily, Monthly, Yearly
    }
    
    /**
     * The method called to start the program
     * @param primaryStage 
     */
    @Override
    public void start(Stage primaryStage) {

        //Different view children for inputting company data
        TextField textField = new TextField();
        Button btn = new Button();

        //The first Grid to retrieve user input
        grid1 = new GridPane();
        grid1.setAlignment(Pos.CENTER);
        grid1.setHgap(10);
        grid1.setVgap(10);
        grid1.setPadding(new Insets(25, 25, 25, 25));

        //The second Grid to display stock analysis
        grid2 = new GridPane();
        grid2.setHgap(10);
        grid2.setVgap(10);
        grid2.setPadding(new Insets(25, 25, 25, 25));

        //Store the stage and the scenes
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
                    companyStockData = getStockData(companySelected);
                    createDailyGraph(companyStockData); //Daily graph by default
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(StockTrends.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(StockTrends.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });

        //Add the node children to the gridpane
        grid1.add(textField, 0, 0);
        grid1.add(btn, 0, 1);

        //The different options you can select
        ObservableList<TIMEFRAME> options = FXCollections.observableArrayList();
        options.add(TIMEFRAME.Daily);
        options.add(TIMEFRAME.Monthly);
        options.add(TIMEFRAME.Yearly);
        
        //Use combobox to select the different timeframe options
        final ComboBox cb1 = new ComboBox(options);
        cb1.getSelectionModel().selectFirst(); //Select "Daily" as the default option
        cb1.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent event){            
                String selectedOption = cb1.getSelectionModel().getSelectedItem().toString();

                //Switch statement to change the graph appropriately
                switch(TIMEFRAME.valueOf(selectedOption)){
                    case Daily:
                        createDailyGraph(companyStockData);
                        break;
                    case Monthly:
                        break;
                    case Yearly:
                        Stock[] yearlyStockData = averageStockByYear(companyStockData);
                        createYearGraph(yearlyStockData);
                        break;
                }
                
            }
        });

        
        //populating the series with data
        grid2.add(cb1, 0, 0);
    
        currentStage.setTitle("Stock Market Analysis");
        currentStage.setScene(scene1);
        currentStage.show();
    }

    /**
     * Method: getStockData
     * Description: Retrieves the csv from yahoo and parses the data into Stock
     * objects
     * @param companyName
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private Stock[] getStockData(String companyName) throws FileNotFoundException, IOException {

        List<Stock> stockData = new ArrayList<>();

        String yahooUrl = "http://real-chart.finance.yahoo.com/table.csv?s=";
        String completedYahooUrl = yahooUrl + companyName;
        URL csvUrl = new URL(completedYahooUrl);

        CSVReader reader = new CSVReader(new InputStreamReader(csvUrl.openStream()));

        String[] nextLine = reader.readNext(); //Discard column headers 
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

    /**
     * Method: createDailyGraph
     * Description: Creates a graph with daily stock data
     * @param stockData 
     */
    public void createDailyGraph(Stock[] stockData) {
        ObservableList<XYChart.Series<Date, Number>> series = FXCollections.observableArrayList();

        ObservableList<XYChart.Data<Date, Number>> series1Data = FXCollections.observableArrayList();
        for(Stock s: stockData){
                series1Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getOpen()));
        }

        series.add(new XYChart.Series<>("Daily Stock", series1Data));

        NumberAxis numberAxis = new NumberAxis();
        DateAxis dateAxis = new DateAxis();
        LineChart<Date, Number> lineChart = new LineChart<>(dateAxis, numberAxis, series);

        lineChart.setMinSize(900, 900);
        
        
        //TODO: Redo this logic 
        try{
            grid2.getChildren().remove(1); //Remove the previous graph
        }catch(IndexOutOfBoundsException e){
            e.printStackTrace();
        }
        
        //Add the chart as the second node child since the first child is the button
        grid2.add(lineChart, 0, 1);
        
        Scene scene = new Scene(grid2, 1000, 1000);
        currentStage.setScene(scene);
    }

    /**
     * Method: createYearGraph
     * Description: Gets the stock data and analyses the data averaging by year
     * @param stockData 
     */
    public void createYearGraph(Stock[] stockData) {      
       
        final NumberAxis xAxis = new NumberAxis(stockData[stockData.length - 1].getYear(), stockData[0].getYear(), 1);
        final DecimalFormat format = new DecimalFormat("####"); //Remove commas in thousand place ex) 1,000 -> 1000
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number number) {
                return format.format(number.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                try {
                    return format.parse(string);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Number of Years");
        //creating the chart
        final LineChart<Number, Number> lineChart
                = new LineChart<Number, Number>(xAxis, yAxis);

        lineChart.setTitle("Yearly Stock");
        //defining a series
        XYChart.Series series = new XYChart.Series();
        series.setName("Average Stock Value - Year");

        //Add the data
        for (Stock s : stockData) {
            series.getData().add(new XYChart.Data(s.getYear(), s.getOpen()));
        }

        
        lineChart.setMinSize(900, 900);
        lineChart.getData().add(series);
        
        
        //TODO: Redo this logic
        try{
            grid2.getChildren().remove(1); //Remove the previous graph
        }catch(IndexOutOfBoundsException e){
            e.printStackTrace();
        }
        
        grid2.add(lineChart, 0, 1);
        
        Scene scene = new Scene(grid2, 1000, 1000);
        currentStage.setScene(scene);
    }

    /**
     * Method: averageStockByYear
     * Description: Create a list of stocks that represent the average for the year
     * @param stockData
     * @return 
     */
    private Stock[] averageStockByYear(Stock[] stockData) {
        List<Stock> stockDataByYear = new ArrayList<>();
        int count = 0; //Incremented for every entry that has the same year as the previous
        BigDecimal average = new BigDecimal(0); //Variable used to compute the average from value/count
        BigDecimal value = new BigDecimal(0); //Variable used to add the value of the daily stocks in order to compute the yearly average
        for (int i = 0; i < stockData.length; i++) {
            int currentYear = stockData[i].getYear();
            for (int j = i; j < stockData.length; j++) {
                //If the entry is the same year, add to value and increment count
                if (stockData[j].getYear() == currentYear) {
                    value = value.add(stockData[j].getOpen());
                    count++;
                }
                //If it is a new year, compute the yearly stock average
                if (stockData[j].getYear() != currentYear) {
                    average = value.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                    Stock yearStock = new Stock(currentYear, average);
                    stockDataByYear.add(yearStock);
                    value = new BigDecimal(0);
                    count = 0;
                    break;
                }
                i = j;
                //For the last section of years
                if (i == stockData.length - 1) {
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
