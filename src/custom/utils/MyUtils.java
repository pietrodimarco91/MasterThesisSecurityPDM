package custom.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import custom.entities.RelationView;
import custom.enums.Keywords;

/**
 * Class that simplifies many operations with dates
 * @author paolobruzzo
 */
public class MyUtils {
	
	/**
	 * Converts a string formatted as a date into a Calendar date
	 * @param date in the form <b>yyyy-mm-dd hh:mm:ss</b>
	 * @return a Calendar instance date
	 * @throws ParseException when <i>date</i> is not correctly formatted
	 */
	public static Calendar getDateFromString(String date) throws ParseException{
		DateFormat dateFormat = new SimpleDateFormat(Keywords.DATE_FORMAT.toString());
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateFormat.parse(date));
		return cal;
	}
	
	/**
	 * Returns the string corresponding to the input Calendar <i>date</i>
	 * @param date is the date in Calendar format
	 * @return the corresponding String
	 */
	public static String getStringFromDate(Calendar date){
		DateFormat dateFormat = new SimpleDateFormat(Keywords.DATE_FORMAT.toString());
		return dateFormat.format(date.getTime());
	}
	
	/**
	 * Get the current time in Calendar format
	 * @return the current time
	 */
	public static Calendar getCurrentDateTime(){
		return Calendar.getInstance();
	}
	
	/**
	 * Get the maximum time that the application has to wait until refreshing
	 * @return the max time in milliseconds
	 */
	public static long getMaxTime(){
		try {
			return getDateFromString("2100-01-01 00:00:00").getTimeInMillis();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Should never get here
		return 0;
	}
	
	/**
	 * Computes the oldest date
	 * @param rViews is the list of views
	 * @return the oldest date in string format
	 */
	public static String getOldestBeginDateToString(List<RelationView> rViews){
		Calendar mostRecent = rViews.get(0).getBeginDate();
		for(int i = 1 ; i < rViews.size() ; i++){
			if(rViews.get(i).getBeginDate().before(mostRecent))
				mostRecent = rViews.get(i).getBeginDate();
		}
		return MyUtils.getStringFromDate(mostRecent);
	}
	
	
	/**
	 * Computes the oldest date
	 * @param rViews is the list of views
	 * @return the oldest date in string format
	 */
	public static String getOldestExpirationDateToString(List<RelationView> rViews){
		Calendar mostRecent = rViews.get(0).getExpirationDate();
		for(int i = 1 ; i < rViews.size() ; i++){
			if(rViews.get(i).getExpirationDate().before(mostRecent))
				mostRecent = rViews.get(i).getExpirationDate();
		}
		return MyUtils.getStringFromDate(mostRecent);
	}
	
	
}
