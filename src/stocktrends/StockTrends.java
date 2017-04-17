package stocktrends;

import com.opencsv.CSVReader;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
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
    private BigDecimal profit;
    private ObservableList<XYChart.Data<Date, Number>> profitPoints, neutralPoints, lossPoints;
    private ObservableList<XYChart.Series<Date, Number>> series;
    private LineChart<?, ?> lineChart;
    private String selectedGraph;
    private List<AlgorithmData> profitPointsList;

    XYChart.Series profitPointsSeries;
    XYChart.Series neutralPointsSeries;
    XYChart.Series lossPointsSeries;

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
                    //runSimpleAlgo(companyStockData); //Calculate the moving averages
                    runBuySellOnce(companyStockData);
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
                selectedGraph = cb1.getSelectionModel().getSelectedItem().toString();
                //Switch statement to change the graph appropriately
                switch (TIMEFRAME.valueOf(selectedGraph)) {
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
            }
        });

        //The different algorithms you can select
        ObservableList<String> algoOptions = FXCollections.observableArrayList();
        algoOptions.add("Buy and Sell Once");
        algoOptions.add("Buy and Sell Twice");
        algoOptions.add("50 Day Moving Average");
        algoOptions.add("Find best average");

        //Use combobox to select the different algorithms
        final ComboBox cb2 = new ComboBox(algoOptions);
        cb2.getSelectionModel().selectFirst(); //Select "Daily" as the default option
        cb2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String selectedOption = cb2.getSelectionModel().getSelectedItem().toString();
                //Switch statement to change the graph appropriately
                switch (selectedOption) {
                    case "Buy and Sell Once":
                        runBuySellOnce(companyStockData);
                        resetGraph();
                        break;
                    case "Buy and Sell Twice":
                        runBuySellTwice(companyStockData);
                        resetGraph();
                        break;
                    case "50 Day Moving Average":
                        runMovingAverageAlgo(companyStockData, 50, 200);
                        break;
                    case "Find best average":
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runCustomAlgo(companyStockData);
                            }
                        });
                        t.start();
                        break;
                }
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
        grid2.add(cb2, 1, 0);
        grid2.add(btnOpenNewWindow, 0, 1);

        currentStage.setTitle("Stock Market Analysis");
        currentStage.setScene(scene1);
        currentStage.show();
    }

    /**
     * Method: resetGraph() Description: removes the different profit,neutral,
     * and loss lines
     */
    private void resetGraph() {
        series.remove(profitPointsSeries);
        series.remove(neutralPointsSeries);
        series.remove(lossPointsSeries);
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

        //http://www.google.com/finance/historical?q=NASDAQ%3AGOOG&ei=C_jrWMHVIoKrjAG5pLa4CA&startdate=Jan+1%2C+1970&enddate=Apr+12%2C+2017&output=csv
        String completedGoogUrl = "http://www.google.com/finance/historical?q=NASDAQ%3A" + companyName + "&ei=C_jrWMHVIoKrjAG5pLa4CA&startdate=Jan+1%2C+1970&enddate=Apr+12%2C+2017&output=csv";

        URL website = new URL(completedGoogUrl);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream("csvData.csv");
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

        File initialFile = new File("csvData.csv");

        InputStream csvInputStream;

        csvInputStream = new FileInputStream(initialFile);

        InputStreamReader initalStream = new InputStreamReader(csvInputStream);

        //saveCSVFile(csvInputStream);
        CSVReader reader = new CSVReader(initalStream);

        String[] nextLine = reader.readNext(); //Discard column headers 
        while ((nextLine = reader.readNext()) != null) {
            // nextLine[] is an array of values from the line

            //(String date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume, BigDecimal adjClose)
            Stock stock = new Stock.Builder(nextLine[0], new BigDecimal(nextLine[4]))
                    .open(nextLine[1])
                    .high(nextLine[2])
                    .low(nextLine[3])
                    //                    .volume(nextLine[5])
                    //                    .adjClose(nextLine[6])
                    .build();

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
        //TODO: Redo this logic 
        if (graphDisplayed) {
            grid2.getChildren().remove(lineChart); //Remove the previous graph
        }

        series = FXCollections.observableArrayList();

        ObservableList<XYChart.Data<Date, Number>> series1Data = FXCollections.observableArrayList();
        for (Stock s : stockData) {
            series1Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
        }

        series.add(new XYChart.Series<>("Daily Stock", series1Data));

        NumberAxis numberAxis = new NumberAxis();
        DateAxis dateAxis = new DateAxis();
        lineChart = new LineChart<Date, Number>(dateAxis, numberAxis, series);
        lineChart.setMinSize(900, 900);

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
        //TODO: Redo this logic 
        if (graphDisplayed) {
            grid2.getChildren().remove(lineChart); //Remove the previous graph
        }

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
        lineChart = new LineChart<Date, Number>(dateAxis, numberAxis, series);
        lineChart.setMinSize(900, 900);

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

        //TODO: Redo this logic
        if (graphDisplayed) {
            grid2.getChildren().remove(lineChart); //Remove the previous graph
        }

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

        private AlgorithmResult(String dataPointAnalysis, String date) {
            this.dataPointAnalysis = new SimpleStringProperty(dataPointAnalysis);
            this.dateOfAnalysis = new SimpleStringProperty(date);
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
     * Method: drawTable() Description: Draws a table with the buy and sell data
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
                //    public Stock(int year, int month, BigDecimal close){
                //If the entry is the same month, add to value and increment count
                if (stockData[j].getMonth() == currentMonth) {
                    value = value.add(stockData[j].getClose());
                    count++;
                }
                //If it is a new month, compute the previous monthly stock average
                if (stockData[j].getMonth() != currentMonth) {
                    average = value.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                    Stock monthStock = new Stock.Builder(currentYear + "-" + currentMonth + "-" + "1", average)
                            .build();
                    stockDataByMonth.add(monthStock);
                    value = new BigDecimal(0);
                    count = 0;
                    break;
                }
                i = j;
                //For the last section of years
                if (i == stockData.length - 1) {
                    average = value.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                    Stock monthStock = new Stock.Builder(currentYear + "-" + currentMonth + "-" + "1", average)
                            .build();
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
                    Stock yearStock = new Stock.Builder(currentYear + "-" + 1 + "-" + "1", average)
                            .build();
                    stockDataByYear.add(yearStock);
                    value = new BigDecimal(0);
                    count = 0;
                    break;
                }
                i = j;
                //For the last section of years
                if (i == stockData.length - 1) {
                    average = value.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                    Stock yearStock = new Stock.Builder(currentYear + "-" + 1 + "-" + "1", average)
                            .build();
                    stockDataByYear.add(yearStock);
                    count = 0;
                    break;
                }
            }
        }
        return stockDataByYear.toArray(new Stock[stockDataByYear.size()]);
    }

    /**
     * Method: calculateMovingAverages Description: This method is used to
     * calculate the moving averages for the different sample sizes(50, 100, and
     * 200).
     *
     * @param stockData
     */
    private BigDecimal[] calculateMovingAverages(Stock[] stockData, int movingAverageSmall, int movingAverageLarge) {
        BigDecimal[] movingAverageStocks = new BigDecimal[2];
        BigDecimal movingAverageSmallStock = new BigDecimal(0);
        BigDecimal movingAverageLargeStock = new BigDecimal(0);
        //Condition that not even 50 days exist - Use what you have
        if (stockData.length < movingAverageSmall) {
            BigDecimal value = new BigDecimal(0);
            for (Stock stock : stockData) {
                value = value.add(stock.getClose());
            }
            movingAverageSmallStock = value.divide(new BigDecimal(stockData.length), 2, RoundingMode.HALF_UP);
        } //If >50 days exist. If corner case like 99 entries exist; only assign 
        //the 50 day moving average
        else {
            //Variable used to add the value of the daily stocks in order to compute the yearly average
            BigDecimal value = new BigDecimal(0);
            for (int i = 0; i < movingAverageLarge; i++) {
                value = value.add(stockData[i].getClose());
                if (i == movingAverageSmall - 1) {
                    movingAverageSmallStock = value.divide(new BigDecimal(movingAverageSmall), 2, RoundingMode.HALF_UP); //the moving average for the top 50 entries
                }
                if (i == movingAverageLarge - 1) {
                    movingAverageLargeStock = value.divide(new BigDecimal(movingAverageLarge), 2, RoundingMode.HALF_UP); //the moving average for the top 200 entries
                }
            }
        }
        movingAverageStocks[0] = movingAverageSmallStock;
        movingAverageStocks[1] = movingAverageLargeStock;
        //System.out.println(movingAverageSmall + " : " + movingAverageSmallStock + " " + movingAverageLarge + " : " + movingAverageLargeStock);
        return movingAverageStocks;
    }

    /**
     * Method: simpleAlgo Description: Simple stock buying and selling
     * algorithm.
     *
     * Buy/Sell 50 shares of a stock when its 50-day moving average goes above
     * the 200-day moving average Sell shares of the stock when its 50-day
     * moving average goes below the 200-day moving average
     *
     * @param stockData
     */
    private BigDecimal simpleAlgo(Stock s, BigDecimal movingAverageSmall, BigDecimal movingAverageLarge, BigDecimal curProfit) {
        switch (movingAverageSmall.compareTo(movingAverageLarge)) {
            case 1:
                curProfit = curProfit.add(s.getClose().multiply(new BigDecimal(50)));
                profitPoints.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
                profitPointsList.add(new AlgorithmData("You should buy @ " + s.getClose(), s.getDate()));
                break;
            case 0:
                neutralPoints.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
                profitPointsList.add(new AlgorithmData("You should do nothing", s.getDate()));
                break;
            case -1:
                curProfit = curProfit.add(s.getClose().multiply(new BigDecimal(-50)));
                lossPoints.add(new XYChart.Data<Date, Number>(new GregorianCalendar(s.getYear(), s.getMonth(), s.getDay()).getTime(), s.getClose()));
                profitPointsList.add(new AlgorithmData("You should sell @ " + s.getClose(), s.getDate()));
                break;
            default:
                profitPointsList.add(new AlgorithmData("Something has gone wrong; I recommend looking at the data yourself.", s.getDate()));
                break;
        }

        return curProfit;
    }

    /**
     * Method: runMovingAverageAlgo Description: Method used to run the
     * movingAverageAlgo method using specified periods and simulating from the
     * beginning of the company
     *
     * @param stockData
     */
    private void runMovingAverageAlgo(Stock[] stockData, int movingAvgSmall, int movingAvgLarge) {
        profitPoints = FXCollections.observableArrayList();
        neutralPoints = FXCollections.observableArrayList();
        lossPoints = FXCollections.observableArrayList();
        profit = new BigDecimal(0);
        profitPointsList = new ArrayList<>();

        for (int i = stockData.length - 1; i > movingAvgLarge; i -= movingAvgSmall) {
            Stock[] tempPeriod = Arrays.copyOfRange(stockData, (i - movingAvgLarge), i);
            BigDecimal[] movingAverageStocks = calculateMovingAverages(tempPeriod, movingAvgSmall, movingAvgLarge);
            profit = simpleAlgo(tempPeriod[0], movingAverageStocks[0], movingAverageStocks[1], profit);
        }

        /*
        Add the three profit, neutral, and loss results to the graph 
         */
        profitPointsSeries = new XYChart.Series<>("Profit Points", profitPoints);
        neutralPointsSeries = new XYChart.Series<>("Neutral Points", neutralPoints);
        lossPointsSeries = new XYChart.Series<>("Loss Points", lossPoints);

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
        profitPointsList.add(new AlgorithmData(("Net Profit with simple algo is: " + profit), ""));
        drawTable(profitPointsList);
    }

    private class ProfitPoint {

        int valueSmall;
        int valueBig;
        BigDecimal profit;

        public ProfitPoint(int valueSmall, int valueBig, BigDecimal profit) {
            this.valueSmall = valueSmall;
            this.valueBig = valueBig;
            this.profit = profit;
        }
    }

    /**
     * Method: runCustomAlgo Description: Method used to run the simpleAlgo
     * method using ? day periods and simulating from the beginning of the
     * company
     *
     * @param stockData
     */
    private void runCustomAlgo(final Stock[] stockData) {

        List<ProfitPoint> profitList = new ArrayList<>();
        PriorityQueue<ProfitPoint> profitHeap = new PriorityQueue<>(10, new Comparator<ProfitPoint>(){
            @Override
            public int compare(ProfitPoint o1, ProfitPoint o2) {
                return o1.profit.compareTo(o2.profit);
            }
        });
        
        
        int i = 1;
        int j = 1;
        int k = stockData.length - 1;
        try {

            for (i = 1; i < stockData.length - 1; i++) { //smallAvg

                for (j = 1; j < stockData.length - 1; j++) { //largeAvg

                    //Calculation Operation
                    profitPoints = FXCollections.observableArrayList();
                    neutralPoints = FXCollections.observableArrayList();
                    lossPoints = FXCollections.observableArrayList();
                    profit = new BigDecimal(0);
                    profitPointsList = new ArrayList<>();

                    for (k = stockData.length - 1; k > j; k -= i) {
                        Stock[] tempPeriod = Arrays.copyOfRange(stockData, (k - j), k);
                        BigDecimal[] movingAverageStocks = calculateMovingAverages(tempPeriod, i, j);
                        profit = simpleAlgo(tempPeriod[0], movingAverageStocks[0], movingAverageStocks[1], profit);
                    }

                    //At the end of the movingAvg Calcs, add the data. Use 200 and 50 as a checkpoint since I know the value is -767121.9889
                    //It worked, the checkpoint reported: -767121.988900
                    ProfitPoint point = new ProfitPoint(i, j, profit);
                    profitList.add(point);
                    profitHeap.add(point);
                    if (j == 200 && i == 50) {
                        System.out.println("Checkpoint: " + profit);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(" i: " + i + " j: " + j + " k: " + k);
        }

//        BigDecimal curMax = new BigDecimal(0);
//        int highI = 0, highJ = 0;
//        ProfitPoint tempHighProfitPoint = null;
//        for (int x = 0; x < profitList.size(); x++) {
//            if (profitList.get(x).profit.compareTo(curMax) == 1) {
//                curMax = profitList.get(x).profit;
//                highI = profitList.get(x).valueSmall;
//                highJ = profitList.get(x).valueBig;
//                tempHighProfitPoint = profitList.get(x);
//            }
//        }
//
//        System.out.println("curMAX: " + curMax + " highI: " + highI + " highJ: " + highJ);

        final ProfitPoint highProfitPoint = profitHeap.peek();

        while(!profitHeap.isEmpty()){
            ProfitPoint tempHighProfitPoint = profitHeap.poll();
            System.out.println("curMAX: " + tempHighProfitPoint.profit + " highI: " + tempHighProfitPoint.valueSmall + " highJ: " + tempHighProfitPoint.valueBig);
        }

//        final ProfitPoint highProfitPoint = tempHighProfitPoint;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                runMovingAverageAlgo(stockData, highProfitPoint.valueSmall, highProfitPoint.valueBig);
            }
        });

    }

    /**
     * runBuySellOnce(Stock[] data) Computes the best time to buy and sell a
     * share a stock over the course of the company's history.
     *
     * @param data
     */
    private double runBuySellOnce(Stock[] data) {
        double profit = 0.0;
        double minStockClose = Double.MAX_VALUE;
        Stock minStock = null;
        Stock maxStock = null;
        for (Stock s : data) {
            if (profit < s.getClose().doubleValue() - minStockClose) {
                maxStock = s;
                profit = s.getClose().doubleValue() - minStockClose;
            }
            if (s.getClose().doubleValue() < minStockClose) {
                minStock = s;
                minStockClose = s.getClose().doubleValue();
            }
        }

        /*
        Add data points to the data table 
         */
        profitPointsList = new ArrayList<>();
        profitPointsList.add(new AlgorithmData("You should buy @ " + minStock.getClose(), minStock.getDate()));
        profitPointsList.add(new AlgorithmData("You should sell @ " + maxStock.getClose(), maxStock.getDate()));
        profit = maxStock.getClose().doubleValue() - minStock.getClose().doubleValue();
        profitPointsList.add(new AlgorithmData(("Net Profit with simple algo is: " + profit), ""));

        drawTable(profitPointsList);

        return profit;
    }

    /**
     * runBuySellTwice(Stock[] data) Computes the best time to buy and sell a
     * share a stock twice over the course of the company's history.
     *
     * @param data
     */
    private double runBuySellTwice(Stock[] data) {
        double profit = 0.0;
        List<Double> firstProfit = new ArrayList<>();
        double minStockClose = Double.MAX_VALUE;

        Stock firstStockMin = null, firstStockMax = null, secondStockMin = null, secondStockMax = null;

        //Getting max profit first pass
        for (int i = 0; i < data.length; i++) {
            //Get min stock
            //minStockClose = Math.min(minStockClose, data[i].getClose().doubleValue());
            if (minStockClose > data[i].getClose().doubleValue()) {
                minStockClose = data[i].getClose().doubleValue();
                firstStockMin = data[i];
            }

            //Get max stock
            //profit = Math.max(profit, data[i].getClose().doubleValue() - minStockClose);
            if (profit < (data[i].getClose().doubleValue() - minStockClose)) {
                profit = (data[i].getClose().doubleValue() - minStockClose);
                firstStockMax = data[i];
            }

            firstProfit.add(profit);
        }

        //Second pass profits
        double maxPriceClose = Double.MIN_VALUE;
        for (int i = data.length - 1; i > 0; i--) {
            //Get max close
            //maxPriceClose = Math.max(maxPriceClose, data[i].getClose().doubleValue());
            if (maxPriceClose < (data[i].getClose().doubleValue())) {
                maxPriceClose = (data[i].getClose().doubleValue());
                secondStockMax = data[i];
            }

            //Get min close
            //profit = Math.max(profit, (maxPriceClose - data[i].getClose().doubleValue() + firstProfit.get(i - 1)));
            if (profit < (maxPriceClose - data[i].getClose().doubleValue() + firstProfit.get(i - 1))) {
                profit = (maxPriceClose - data[i].getClose().doubleValue() + firstProfit.get(i - 1));
                secondStockMin = data[i];
            }
        }

        /*
        Add data points to the data table 
0         */
        profitPointsList = new ArrayList<>();
        profitPointsList.add(new AlgorithmData("You should buy @ " + firstStockMin.getClose(), firstStockMin.getDate()));
        profitPointsList.add(new AlgorithmData("You should sell @ " + firstStockMax.getClose(), firstStockMax.getDate()));
        profitPointsList.add(new AlgorithmData("You should buy @ " + secondStockMin.getClose(), secondStockMin.getDate()));
        profitPointsList.add(new AlgorithmData("You should sell @ " + secondStockMax.getClose(), secondStockMax.getDate()));
        profitPointsList.add(new AlgorithmData(("Net Profit with simple algo is: " + profit), ""));

        drawTable(profitPointsList);

        return profit;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
