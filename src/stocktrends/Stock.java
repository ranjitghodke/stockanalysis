/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stocktrends;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author rghodke
 */
public class Stock {
    private final String date;
    private final int year;
    private final int month;
    private final int day;
    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final BigDecimal volume;
    private final BigDecimal adjClose;
    
    public static class Builder {
        //Required parameters
        private final String date;
        private final BigDecimal close;
        private final int year;
        private final int month;
        private final int day;

        
        //Optional Parameters
        private BigDecimal open = new BigDecimal(0);
        private BigDecimal high = new BigDecimal(0);
        private BigDecimal low = new BigDecimal(0);
        private BigDecimal volume = new BigDecimal(0);
        private BigDecimal adjClose = new BigDecimal(0);
        
        public Builder(String date, BigDecimal close){
            this.date = date;
            this.close = close;
            String[] dateInfo = date.split("-");
            this.year = Integer.parseInt(dateInfo[0]);
            this.month = Integer.parseInt(dateInfo[1]);
            this.day = Integer.parseInt(dateInfo[2]);    
        }
        
        public Builder open(String open){
            this.open = new BigDecimal(open);
            return this;
        }
        
        public Builder high(String high){
            this.high = new BigDecimal(high);
            return this;
        }
        
        public Builder low(String low){
            this.low = new BigDecimal(low);
            return this;
        }
        
        public Builder volume(String volume){
            this.volume = new BigDecimal(volume);
            return this;
        }
        
        public Builder adjClose(String adjClose){
            this.adjClose = new BigDecimal(adjClose);
            return this;
        }   
        
        public Stock build(){
           return new Stock(this);
        }
    }
    
    private Stock(Builder builder){
        this.date = builder.date;
        this.year = builder.year;
        this.month = builder.month;
        this.day = builder.day;
        this.open = builder.open;
        this.high = builder.high;
        this.low = builder.low;
        this.close = builder.close;
        this.volume = builder.volume;
        this.adjClose = builder.adjClose;
    }
    
    public int getYear(){
        return year;
    }
    
    public int getMonth(){
        return month;
    }
    
    public int getDay(){
        return day;
    }
    
    public String getDate(){
        return date;
    }
    
    public BigDecimal getOpen(){
        return open;
    }
    
    public BigDecimal getHigh(){
        return high;
    }
    
    public BigDecimal getLow(){
        return low;
    }
    
    public BigDecimal getClose(){
        return close;
    }
    
    public BigDecimal getVolume(){
        return volume;
    }
    
    public BigDecimal getAdjClose(){
        return adjClose;
    }

    
}
//
//public class Stock {
//    
//    private Date date;
//    
//    private int year;
//    private int month;
//    private int day;
//        
//    private BigDecimal open;
//    
//    private BigDecimal high;
//    
//    private BigDecimal low;
//    
//    private BigDecimal close;
//    
//    private BigDecimal volume;
//    
//    private BigDecimal adjClose;
//    
//    public Stock(String date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume, BigDecimal adjClose){
//        this.open = open;
//        this.high = high;
//        this.low = low;
//        this.close = close;
//        this.volume = volume;
//        this.adjClose = adjClose;
//        
//        String[] dateInfo = date.split("-");
//        this.year = Integer.parseInt(dateInfo[0]);
//        this.month = Integer.parseInt(dateInfo[1]);
//        this.day = Integer.parseInt(dateInfo[2]);    
//    
//        StringBuilder x = new StringBuilder();
//        
//        for(String s:dateInfo){
//            x.append(s);
//        }
//        
//        this.date = new Date(year, month, day);
//        
//    }
//    
//    public Stock(int year, BigDecimal close){
//        this.close = close;
//        this.year = year;
//    }
//    
//    public Stock(int year, int month, BigDecimal close){
//        this.close = close;
//        this.year = year;
//        this.month = month;
//        this.day = 1;
//    }
//    
//    public int getYear(){
//        return year;
//    }
//    
//    public int getMonth(){
//        return month;
//    }
//    
//    public int getDay(){
//        return day;
//    }
//    
//    public Date getDate(){
//        return date;
//    }
//    
//    public BigDecimal getOpen(){
//        return open;
//    }
//    
//    public BigDecimal getHigh(){
//        return high;
//    }
//    
//    public BigDecimal getLow(){
//        return low;
//    }
//    
//    public BigDecimal getClose(){
//        return close;
//    }
//    
//    public BigDecimal getVolume(){
//        return volume;
//    }
//    
//    public BigDecimal getAdjClose(){
//        return adjClose;
//    }
//
//}
