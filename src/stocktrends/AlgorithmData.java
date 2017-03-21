package stocktrends;

import java.util.Date;

/**
 *
 * @author rghodke
 */
public class AlgorithmData {

    private String dataPt;
    
    private Date date;
    
    public AlgorithmData(String dataPt, Date date){
        this.dataPt = dataPt;
        this.date = date;
    }
    
    public String getDataPt(){
        return dataPt;
    }
    
    public Date getDate(){
        return date;
    }
    
    
}
