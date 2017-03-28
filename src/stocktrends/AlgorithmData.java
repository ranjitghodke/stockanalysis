package stocktrends;

import java.util.Date;

/**
 *
 * @author rghodke
 */
public class AlgorithmData {

    private String dataPt;
    
    private String date;
    
    public AlgorithmData(String dataPt, String date){
        this.dataPt = dataPt;
        this.date = date;
    }
    
    public String getDataPt(){
        return dataPt;
    }
    
    public String getDate(){
        return date;
    }
    
    
}
