package stocktrends;

import com.opencsv.CSVReader;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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
    private TableView<AlgorithmResult> table;

    private String companySelected;
    private Stock[] companyStockData;
    private boolean graphDisplayed;
    private File csvFile;
    private BigDecimal movingAverage50, movingAverage100, movingAverage200;
    private BigDecimal profit;
    private ObservableList<XYChart.Data<Date, Number>> profitPoints, neutralPoints, lossPoints;
    private ObservableList<XYChart.Series<Date, Number>> series;
    private LineChart<Number, Number> lineChart;

    //The selected time frame to display analysis for
    private static enum TIMEFRAME {
        Daily, Monthly, Yearly
    }

    /**
     * The method called to start the program
     *
     * @param primaryStage
     */
    @Override
    public void start(Stage primaryStage) {

        //Different view children for inputting company data
        TextField textField = new TextField();
        Button btn = new Button();
        Label label = new Label();
        
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
        scene2 = new Scene(grid2, 1250, 1000);

        btn.setText("Analyze Stock History");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                companySelected = textField.getText();
                try {
                    companyStockData = getStockData(companySelected);
                    createDailyGraph(companyStockData); //Daily graph by default
                    runSimpleAlgo(companyStockData); //Calculate the moving averages
                } catch (FileNotFoundException ex) {
                    label.setText("Please re-enter the company name");
                    grid1.add(label, 0, 2);
                    return; //Wrong company
                } catch (IOException ex) {
                    Logger.getLogger(StockTrends.class.getName()).log(Level.SEVERE, null, ex);
                }
                currentStage.setScene(scene2);
                currentStage.centerOnScreen();

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
        cb1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String selectedOption = cb1.getSelectionModel().getSelectedItem().toString();
                //Switch statement to change the graph appropriately
                switch (TIMEFRAME.valueOf(selectedOption)) {
                    case Daily:
                        createDailyGraph(companyStockData);
                        break;
                    case Monthly:
                        Stock[] monthlyStockData = averageStockByMonth(companyStockData);
                        createMonthlyGraph(monthlyStockData);
                        break;
                    case Yearly:
                        Stock[] yearlyStockData = averageStockByYear(companyStockData);
                        createYearGraph(yearlyStockData);
                        break;
                }
                runSimpleAlgo(companyStockData); //Calculate the moving averages
            }
        });

        Button btnOpenNewWindow = new Button();
        btnOpenNewWindow.setText("See raw CSV Data");
        btnOpenNewWindow.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                try {
                    Desktop.getDesktop().open(csvFile);
                } catch (IOException ex) {
                    Logger.getLogger(StockTrends.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        //populating the series with data
        grid2.add(cb1, 0, 0);
        grid2.add(btnOpenNewWindow, 0, 1);

        currentStage.setTitle("Stock Market Analysis");
        currentStage.setScene(scene1);
        currentStage.show();
    }

    /**
     * Method: getStockData Description: Retrieves the csv from yahoo and parses
     * the data into Stock objects
     *
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

        InputStream csvInputStream;
        
        csvInputStream = csvUrl.openStream();
              
        InputStreamReader initalStream = new InputStreamReader(csvInputStream);

        saveCSVFile(csvInputStream);

        CSVReader reader = new CSVReader(initalStream);

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

    private void saveCSVFile(InputStream csvInputStream) throws IOException, FileNotFoundException {
        byte[] buffer = new byte[csvInputStream.available()];
        csvInputStream.read(buffer);
        csvFile = new File("csvFile.csv");
        OutputStream outStream = new FileOutputStream(csvFile);
        outStream.write(buffer);
    }

    /**
     * Method: createDailyGraph Description: Creates a graph with daily stock
     * data
     *
     * @param stockData
     */
    public void createDailyGraph(Stock[] stockData) {
        series = FXCollections.observableArrayList();

        ObservableList<XYChart.Data<Date, Number>> series1Data = FXCollections.observableArrayList();
        for (Stock s : stockData) {
            series1Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
        }

        series.add(new XYChart.Series<>("Daily Stock", series1Data));

        NumberAxis numberAxis = new NumberAxis();
        DateAxis dateAxis = new DateAxis();
        LineChart<Date, Number> lineChart = new LineChart<>(dateAxis, numberAxis, series);
        lineChart.setMinSize(900, 900);

        //TODO: Redo this logic 
        if (graphDisplayed) {
            grid2.getChildren().remove(2); //Remove the previous graph
        }

        //Add the chart as the third node child since the first,second child is the button
        grid2.add(lineChart, 0, 2);

        graphDisplayed = true;
        currentStage.centerOnScreen();

    }

    /**
     * Method: createMonthlyGraph Description: Creates a graph with monthly
     * stock data
     *
     * @param stockData
     */
    public void createMonthlyGraph(Stock[] stockData) {
        series = FXCollections.observableArrayList();

        ObservableList<XYChart.Data<Date, Number>> series1Data = FXCollections.observableArrayList();
        for (Stock s : stockData) {
            //TODO: Add logic to handle monthly stock data - meaning that s.getDay() on the monthly stock data shouldn't use 01 hardcoded.
            //See Stock(int year, int month, BigDecimal close) for more info
            series1Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
        }

        series.add(new XYChart.Series<>("Daily Stock", series1Data));

        NumberAxis numberAxis = new NumberAxis();
        DateAxis dateAxis = new DateAxis();
        LineChart<Date, Number> lineChart = new LineChart<>(dateAxis, numberAxis, series);
        lineChart.setMinSize(900, 900);

        //TODO: Redo this logic 
        if (graphDisplayed) {
            grid2.getChildren().remove(2); //Remove the previous graph
        }

        //Add the chart as the third node child since the first,second child is the button
        grid2.add(lineChart, 0, 2);

        graphDisplayed = true;
        currentStage.centerOnScreen();
    }

    /**
     * Method: createYearGraph Description: Gets the stock data and analyses the
     * data averaging by year
     *
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
        lineChart = new LineChart<Number, Number>(xAxis, yAxis);

        lineChart.setTitle("Yearly Stock");
        //defining a series
        XYChart.Series series = new XYChart.Series();
        series.setName("Average Stock Value - Year");

        //Add the data
        for (Stock s : stockData) {
            series.getData().add(new XYChart.Data(s.getYear(), s.getClose()));
        }

        lineChart.setMinSize(900, 900);
        lineChart.getData().add(series);

        //TODO: Redo this logic
        if (graphDisplayed) {
            grid2.getChildren().remove(2); //Remove the previous graph
        }
        //Add the chart as the third node child since the first,second child is the button
        grid2.add(lineChart, 0, 2);

        graphDisplayed = true;
        currentStage.centerOnScreen();
    }

    /**
     * Class: AlgorithmResult Description: This class is used as entries for the
     * tableview
     */
    public static class AlgorithmResult {

        private final SimpleStringProperty dataPointAnalysis;
        private final SimpleStringProperty dateOfAnalysis; 

        private AlgorithmResult(String dataPointAnalysis, Date date) {
            this.dataPointAnalysis = new SimpleStringProperty(dataPointAnalysis);
            this.dateOfAnalysis = new SimpleStringProperty(date.toString());
        }

        public String getDataPointAnalysis() {
            return dataPointAnalysis.get();
        }

        public void setDataPointAnalysis(String dataPointAnalysis) {
            this.dataPointAnalysis.set(dataPointAnalysis);
        }
        
        public String getDateOfAnalysis() {
            return dateOfAnalysis.get();
        }

        public void setDateOfAnalysis(String date) {
            this.dateOfAnalysis.set(date);
        }
    }

    /**
     * Method: drawTable()
     * Description: Draws a table with the buy and sell data
     */
    private void drawTable(List<AlgorithmData> stringData) {
        table = new TableView<>();

        ObservableList<AlgorithmResult> data
                = FXCollections.observableArrayList();

        //Add the data to the data list
        stringData.forEach((s) -> {
            data.add(new AlgorithmResult(s.getDataPt(), s.getDate()));
        });

        table.setEditable(true);

        //Column for the analysis
        TableColumn<AlgorithmResult, String> dataPointCol
                = new TableColumn<>("Algorithm Analysis");
        dataPointCol.setMinWidth(table.getWidth());
        dataPointCol.setCellValueFactory(
                new PropertyValueFactory<>("dataPointAnalysis"));

        dataPointCol.setCellFactory(TextFieldTableCell.<AlgorithmResult>forTableColumn());
        dataPointCol.setOnEditCommit(
                (CellEditEvent<AlgorithmResult, String> t) -> {
                    ((AlgorithmResult) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setDataPointAnalysis(t.getNewValue());
                });

        //Column for the date
        TableColumn<AlgorithmResult, String> dateCol
                = new TableColumn<>("Date of Occurance");
        dateCol.setMinWidth(table.getWidth());
        dateCol.setCellValueFactory(
                new PropertyValueFactory<>("dateOfAnalysis"));

        dateCol.setCellFactory(TextFieldTableCell.<AlgorithmResult>forTableColumn());
        dateCol.setOnEditCommit(
                (CellEditEvent<AlgorithmResult, String> t) -> {
                    ((AlgorithmResult) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setDateOfAnalysis(t.getNewValue());
                });

        
        table.setItems(data);
        table.getColumns().addAll(dataPointCol);
        table.getColumns().addAll(dateCol);


        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 0, 0, 10));
        vbox.getChildren().addAll(table);

        grid2.add(vbox, 1, 2);
    }

    /**
     * Method: averageStockByMonth Description: Create a list of stocks that
     * represent the average for the months throughout the years
     *
     * @param stockData
     * @return
     */
    private Stock[] averageStockByMonth(Stock[] stockData) {
        List<Stock> stockDataByMonth = new ArrayList<>();
        int count = 0; //Incremented for every entry that has the same month as the previous
        BigDecimal average = new BigDecimal(0); //Variable used to compute the average from value/count
        BigDecimal value = new BigDecimal(0); //Variable used to add the value of the daily stocks in order to compute the monthly average
        for (int i = 0; i < stockData.length; i++) {
            int currentMonth = stockData[i].getMonth();
            int currentYear = stockData[i].getYear();
            for (int j = i; j < stockData.length; j++) {
                //If the entry is the same month, add to value and increment count
                if (stockData[j].getMonth() == currentMonth) {
                    value = value.add(stockData[j].getClose());
                    count++;
                }
                //If it is a new month, compute the previous monthly stock average
                if (stockData[j].getMonth() != currentMonth) {
                    average = value.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                    Stock monthStock = new Stock(currentYear, currentMonth, average);
                    stockDataByMonth.add(monthStock);
                    value = new BigDecimal(0);
                    count = 0;
                    break;
                }
                i = j;
                //For the last section of years
                if (i == stockData.length - 1) {
                    average = value.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                    Stock monthStock = new Stock(currentYear, currentMonth, average);
                    stockDataByMonth.add(monthStock);
                    count = 0;
                    break;
                }
            }
        }

        return stockDataByMonth.toArray(new Stock[stockDataByMonth.size()]);
    }

    /**
     * Method: averageStockByYear Description: Create a list of stocks that
     * represent the average for the year
     *
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
                    value = value.add(stockData[j].getClose());
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
     * Method: calculatMovingAverage Description: This method is used to
     * calculate the moving averages for the different sample sizes(50, 100, and
     * 200).
     *
     * @param stockData
     */
    private void calculateMovingAverages(Stock[] stockData) {
        //Condition that not even 50 days exist - Use what you have
        if (stockData.length < 50) {
            BigDecimal value = new BigDecimal(0);
            for (Stock stock : stockData) {
                value = value.add(stock.getClose());
            }
            movingAverage50 = value.divide(new BigDecimal(stockData.length), 2, RoundingMode.HALF_UP);
        } //If >50 days exist. If corner case like 99 entries exist; only assign 
        //the 50 day moving average
        else {
            //Variable used to add the value of the daily stocks in order to compute the yearly average
            BigDecimal value = new BigDecimal(0);
            for (int i = 0; i < 200; i++) {
                value = value.add(stockData[i].getClose());
                if (i == 49) {
                    movingAverage50 = value.divide(new BigDecimal(50), 2, RoundingMode.HALF_UP); //the moving average for the top 50 entries
                }
                if (i == 99) {
                    movingAverage100 = value.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP); //the moving average for the top 100 entries
                }
                if (i == 199) {
                    movingAverage200 = value.divide(new BigDecimal(200), 2, RoundingMode.HALF_UP); //the moving average for the top 200 entries
                }
            }
        }
        System.out.println(" 50: " + movingAverage50 + " 100: " + movingAverage100 + " 200: " + movingAverage200);
    }

    private List<AlgorithmData> profitPointsList;

    /**
     * Method: simpleAlgo Description: Simple stock buying and selling
     * algorithm.
     *
     * Buy 50 shares of a stock when its 50-day moving average goes above the
     * 200-day moving average Sell shares of the stock when its 50-day moving
     * average goes below the 200-day moving average
     *
     * @param stockData
     */
    private void simpleAlgo(Stock s) {
        switch (movingAverage50.compareTo(movingAverage200)) {
            case 1:
                profit = profit.add(s.getClose().multiply(new BigDecimal(-50)));
                profitPointsList.add(new AlgorithmData(("You should buy @ " + s.getClose()), s.getDate()));
                profitPoints.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
                break;
            case 0:
                profitPointsList.add(new AlgorithmData("You should do nothing", s.getDate()));
                neutralPoints.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
                break;
            case -1:
                profitPointsList.add(new AlgorithmData("You should sell @ " + s.getClose(), s.getDate()));
                profit = profit.add(s.getClose().multiply(new BigDecimal(50)));
                lossPoints.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
                break;
            default:
                profitPointsList.add(new AlgorithmData("Something has gone wrong; I recommend looking at the data yourself.", s.getDate()));
                break;
        }
    }

    /**
     * Method: runSimpleAlgo Description: Method used to run the simpleAlgo
     * method using 200 day periods and simulating from the beginning of the
     * company
     *
     * @param stockData
     */
    private void runSimpleAlgo(Stock[] stockData) {
        profitPoints = FXCollections.observableArrayList();
        neutralPoints = FXCollections.observableArrayList();
        lossPoints = FXCollections.observableArrayList();
        profit = new BigDecimal(0);
        profitPointsList = new ArrayList<>();

        for (int i = stockData.length - 1; i > 200; i -= 50) {
            Stock[] tempPeriod = Arrays.copyOfRange(stockData, (i - 200), i);
            calculateMovingAverages(tempPeriod);
            simpleAlgo(tempPeriod[0]);
        }

        /*
        Add the three profit, neutral, and loss results to the graph 
         */
        XYChart.Series profitPointsSeries = new XYChart.Series<>("Profit Points", profitPoints);
        XYChart.Series neutralPointsSeries = new XYChart.Series<>("Neutral Points", neutralPoints);
        XYChart.Series lossPointsSeries = new XYChart.Series<>("Loss Points", lossPoints);

        series.add(profitPointsSeries);
        series.add(neutralPointsSeries);
        series.add(lossPointsSeries);

        //TODO: Implement profit, neutral, and loss for the yearly graph
//        lineChart.getData().add(profitPointsSeries);
//        lineChart.getData().add(profitPointsSeries);
//        lineChart.getData().add(profitPointsSeries);
//
//        XYChart.Series profitPointsSeriesYearly = new XYChart.Series();
//        XYChart.Series profitPointsSeriesMonthly = new XYChart.Series();
//        XYChart.Series profitPointsSeriesDaily = new XYChart.Series();
//
//        profitPointsSeriesYearly.setName("Profit Points - Yearly");
//
//        //Add the data
//        for (Stock s : stockData) {
//            series.getData().add(new XYChart.Data(s.getYear(), s.getClose()));
//        }
//
//        lineChart.setMinSize(900, 900);
//        lineChart.getData().add(series);

        profitPointsList.add(new AlgorithmData(("Net Profit with simple algo is: " + profit), new Date()));
        drawTable(profitPointsList);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
