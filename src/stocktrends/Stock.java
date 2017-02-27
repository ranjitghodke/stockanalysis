/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stocktrends;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * @author rghodke
 */
public class Stock {
    
    private Date date;
    
    private int year;
    private int month;
    private int day;
        
    private BigDecimal open;
    
    private BigDecimal high;
    
    private BigDecimal low;
    
    private BigDecimal close;
    
    private BigDecimal volume;
    
    private BigDecimal adjClose;
    
    public Stock(String date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume, BigDecimal adjClose){
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.adjClose = adjClose;
        
        String[] dateInfo = date.split("-");
        this.year = Integer.parseInt(dateInfo[0]);
        this.month = Integer.parseInt(dateInfo[1]);
        this.day = Integer.parseInt(dateInfo[2]);    
    
        StringBuilder x = new StringBuilder();
        
        for(String s:dateInfo){
            x.append(s);
        }
        
        this.date = new Date(year, month, day);
        
    }
    
    public Stock(int year, BigDecimal open){
        this.open = open;
        this.year = year;
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
    
    public Date getDate(){
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
