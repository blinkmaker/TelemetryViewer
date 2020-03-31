import java.awt.Color;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class ChartUtils {

	/**
	 * Determines the best y values to use for vertical divisions. The 1/2/5 pattern is used (1,2,5,10,20,50,100,200,500...)
	 * 
	 * @param plotHeight    Number of pixels for the y-axis
	 * @param minY          Y value at the bottom of the plot
	 * @param maxY          Y value at the top of the plot
	 * @return              A Map of the y values for each division, keys are Floats and values are formatted Strings
	 */
	public static Map<Float, String> getYdivisions125(float plotHeight, float minY, float maxY) {
		
		Map<Float, String> yValues = new HashMap<Float, String>();
		
		// sanity check
		if(plotHeight < 1)
			return yValues;
		
		// calculate the best vertical division size
		float minSpacingBetweenText = 2.0f * Theme.tickTextHeight;
		float maxDivisionsCount = plotHeight / (Theme.tickTextHeight + minSpacingBetweenText) + 1.0f;
		float divisionSize = (maxY - minY) / maxDivisionsCount;
		float closestDivSize1 = (float) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/1.0))) * 1.0f; // closest (10^n)*1 that is >= divisionSize, such as 1,10,100,1000
		float closestDivSize2 = (float) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/2.0))) * 2.0f; // closest (10^n)*2 that is >= divisionSize, such as 2,20,200,2000
		float closestDivSize5 = (float) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/5.0))) * 5.0f; // closest (10^n)*5 that is >= divisionSize, such as 5,50,500,5000
		float error1 = closestDivSize1 - divisionSize;
		float error2 = closestDivSize2 - divisionSize;
		float error5 = closestDivSize5 - divisionSize;
		if(error1 < error2 && error1 < error5)
			divisionSize = closestDivSize1;
		else if(error2 < error1 && error2 < error5)
			divisionSize = closestDivSize2;
		else
			divisionSize = closestDivSize5;
		
		// decide if the numbers should be displayed as integers, or as floats to one significant decimal place
		int precision = 0;
		String format = "";
		if(divisionSize < 0.99) {
			precision = 1;
			float size = divisionSize;
			while(size * (float) Math.pow(10, precision) < 1.0f)
				precision++;
			format = "%." + precision + "f";
		}
		
		// calculate the values for each vertical division
		float firstDivision = maxY - (maxY % divisionSize);
		float lastDivision  = minY - (minY % divisionSize);
		if(firstDivision > maxY)
			firstDivision -= divisionSize;
		if(lastDivision < minY)
			lastDivision += divisionSize;
		int divisionCount = (int) Math.round((firstDivision - lastDivision) / divisionSize) + 1;
		
		for(int i = 0; i < divisionCount; i++) {
			float number = firstDivision - (i * divisionSize);
			String text;
			if(precision == 0) {
				text = Integer.toString((int) number);
			} else {
				text = String.format(format, number);
			}
			yValues.put(number, text);
		}
		
		return yValues;
		
	}
	
	/**
	 * Determines the best Log10 y values to use for vertical divisions. Division size will be either 1e1, 1e3 or 1e9.
	 * 
	 * @param plotHeight    Number of pixels for the y-axis
	 * @param minY          Y value at the bottom of the plot
	 * @param maxY          Y value at the top of the plot
	 * @return              A Map of the y values for each division, keys are Floats and values are formatted Strings
	 */
	public static Map<Float, String> getLogYdivisions(float plotHeight, float minY, float maxY) {
		
		Map<Float, String> yValues = new HashMap<Float, String>();
		
		// sanity check
		if(plotHeight < 1)
			return yValues;
		
		// calculate the best vertical division size
		float minSpacingBetweenText = 2.0f * Theme.tickTextHeight;
		float maxDivisionsCount = plotHeight / (Theme.tickTextHeight + minSpacingBetweenText) + 1.0f;
		float divisionSize = (maxY - minY) / maxDivisionsCount;
		float divSize1 = 1.0f; // 1W, 100mW, 10mW, 1mW, 100uW, ...
		float divSize3 = 3.0f; // 1W, 1mW, 1uW, ...
		float divSize9 = 9.0f; // 1W, 1nW, ...
		float error1 = divSize1 - divisionSize;
		float error3 = divSize3 - divisionSize;
		float error9 = divSize9 - divisionSize;
		if(error1 > 0 && error1 < error3 && error1 < error9)
			divisionSize = divSize1;
		else if(error3 > 0 && error3 < error9)
			divisionSize = divSize3;
		else if(error9 > 0)
			divisionSize = divSize9;
		else
			return new HashMap<Float, String>();
		
		// calculate the values for each vertical division
		float firstDivision = maxY - (maxY % divisionSize);
		float lastDivision  = minY - (minY % divisionSize);
		if(firstDivision > maxY)
			firstDivision -= divisionSize;
		if(lastDivision < minY)
			lastDivision += divisionSize;
		int divisionCount = (int) Math.round((firstDivision - lastDivision) / divisionSize) + 1;
//		if(divisionCount > Math.floor(maxDivisionsCount))
//			divisionCount = (int) Math.floor(maxDivisionsCount);
		
		for(int i = 0; i < divisionCount; i++) {
			float number = firstDivision - (i * divisionSize);
			String text = "1e" + Integer.toString((int) number);
			yValues.put(number, text);
		}
		
		return yValues;
		
	}
	
	/**
	 * Determines the best integer x values to use for horizontal divisions. The 1/2/5 pattern is used (1,2,5,10,20,50,100,200,500...)
	 * 
	 * @param plotWidth    Number of pixels for the x-axis
	 * @param minX         X value at the left of the plot
	 * @param maxX         X value at the right of the plot
	 * @return             A Map of the x values for each division, keys are Integers and values are formatted Strings
	 */
	public static Map<Integer, String> getXdivisions125(float plotWidth, int minX, int maxX) {
		
		Map<Integer, String> xValues = new HashMap<Integer, String>();
		
		// sanity check
		if(plotWidth < 1)
			return xValues;
		
		// calculate the best horizontal division size
		int textWidth = (int) Float.max(Theme.tickTextWidth(Integer.toString(maxX)), Theme.tickTextWidth(Integer.toString(minX)));
		int minSpacingBetweenText = textWidth;
		float maxDivisionsCount = plotWidth / (textWidth + minSpacingBetweenText);
		int divisionSize = (int) Math.ceil((maxX - minX) / maxDivisionsCount);
		if(divisionSize == 0) divisionSize = 1;
		int closestDivSize1 = (int) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/1.0))) * 1; // closest (10^n)*1 that is >= divisionSize, such as 1,10,100,1000
		int closestDivSize2 = (int) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/2.0))) * 2; // closest (10^n)*2 that is >= divisionSize, such as 2,20,200,2000
		int closestDivSize5 = (int) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/5.0))) * 5; // closest (10^n)*5 that is >= divisionSize, such as 5,50,500,5000
		int error1 = closestDivSize1 - divisionSize;
		int error2 = closestDivSize2 - divisionSize;
		int error5 = closestDivSize5 - divisionSize;
		if(error1 < error2 && error1 < error5)
			divisionSize = closestDivSize1;
		else if(error2 < error1 && error2 < error5)
			divisionSize = closestDivSize2;
		else
			divisionSize= closestDivSize5;
		
		// calculate the values for each horizontal division
		int firstDivision = maxX - (maxX % divisionSize);
		int lastDivision  = minX - (minX % divisionSize);
		if(firstDivision > maxX)
			firstDivision -= divisionSize;
		if(lastDivision < minX)
			lastDivision += divisionSize;
		int divisionCount = ((firstDivision - lastDivision) / divisionSize + 1);
		
		for(int i = 0; i < divisionCount; i++) {
			int number = lastDivision + (i * divisionSize);
			String text = Integer.toString(number);
			xValues.put(number, text);
		}
		
		return xValues;
		
	}
	
	/**
	 * Determines the best floating point x values to use for horizontal divisions. The 1/2/5 pattern is used (.1,.2,.5,1,2,5,10,20,50...)
	 * 
	 * @param plotWidth    Number of pixels for the x-axis
	 * @param minX         X value at the left of the plot
	 * @param maxX         X value at the right of the plot
	 * @return             A Map of the x values for each division, keys are Floats and values are formatted Strings
	 */
	public static Map<Float, String> getFloatXdivisions125(float plotWidth, float minX, float maxX) {
		
		Map<Float, String> xValues = new HashMap<Float, String>();
		
		// sanity check
		if(plotWidth < 1)
			return xValues;
		
		for(int maxDivisionsCount = 1; maxDivisionsCount < 100; maxDivisionsCount++) {
			
			float divisionSize = (maxX - minX) / (float) maxDivisionsCount;
			float closestDivSize1 = (float) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/1.0))) * 1; // closest (10^n)*1 that is >= divisionSize, such as 1,10,100,1000
			float closestDivSize2 = (float) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/2.0))) * 2; // closest (10^n)*2 that is >= divisionSize, such as 2,20,200,2000
			float closestDivSize5 = (float) Math.pow(10.0, Math.ceil(Math.log10(divisionSize/5.0))) * 5; // closest (10^n)*5 that is >= divisionSize, such as 5,50,500,5000
			float error1 = closestDivSize1 - divisionSize;
			float error2 = closestDivSize2 - divisionSize;
			float error5 = closestDivSize5 - divisionSize;
			if(error1 < error2 && error1 < error5)
				divisionSize = closestDivSize1;
			else if(error2 < error1 && error2 < error5)
				divisionSize = closestDivSize2;
			else
				divisionSize= closestDivSize5;
			
			// decide if the numbers should be displayed as integers, or as floats to one significant decimal place
			int precision = 0;
			String format = "";
			if(divisionSize < 0.99) {
				precision = 1;
				float size = divisionSize;
				while(size * (float) Math.pow(10, precision) < 1.0f)
					precision++;
				format = "%." + precision + "f";
			}
			
			// calculate the values for each vertical division
			float firstDivision = maxX - (maxX % divisionSize);
			float lastDivision  = minX - (minX % divisionSize);
			firstDivision += divisionSize; // compensating for floating point error that may skip the end points
			lastDivision -= divisionSize;
			while(firstDivision > maxX)
				firstDivision -= divisionSize;
			while(lastDivision < minX)
				lastDivision += divisionSize;
			int divisionCount = (int) Math.round((firstDivision - lastDivision) / divisionSize) + 1;
			
			Map<Float, String> proposedXvalues = new HashMap<Float, String>();
			for(int i = 0; i < divisionCount; i++) {
				float number = firstDivision - (i * divisionSize);
				String text;
				if(precision == 0) {
					text = Integer.toString((int) number);
				} else {
					text = String.format(format, number);
				}
				proposedXvalues.put(number, text);
			}
			
			// calculate how much width is taken up by the text
			float width = 0;
			for(String s : proposedXvalues.values())
				width += Theme.tickTextWidth(s);
			
			// stop and don't use this iteration if we're using more than half of the width
			if(width > plotWidth / 2.0f)
				break;
			
			xValues = proposedXvalues;
			
		}
		
		return xValues;
		
	}
	
	/**
	 * Determines the best timestamp values to use for horizontal divisions.
	 * 
	 * @param width           Number of horizontal pixels available for displaying divisions.
	 * @param minTimestamp    Timestamp at the left edge (milliseconds since 1970-01-01).
	 * @param maxTimestamp    Timestamp at the right edge (milliseconds since 1970-01-01).
	 * @return                A Map of divisions: keys are Float pixelX locations, and values are formatted Strings.
	 */
	@SuppressWarnings("deprecation")
	public static Map<Float, String> getTimestampDivisions(float width, long minTimestamp, long maxTimestamp) {
		
		Map<Float, String> divisions = new HashMap<Float, String>();
		
		// sanity check
		if(width < 1 || minTimestamp > maxTimestamp)
			return divisions;
		
		// determine how many divisions can fit on screen
		String leftLabel  = Theme.timestampFormatter.format(new Date(minTimestamp));
		String rightLabel = Theme.timestampFormatter.format(new Date(maxTimestamp));
		float maxLabelWidth = 0;
		if(leftLabel.contains("\n")) {
			String[] leftLine = leftLabel.split("\n");
			String[] rightLine = rightLabel.split("\n");
			float leftMax  = Float.max(Theme.tickTextWidth(leftLine[0]),  Theme.tickTextWidth(leftLine[1]));
			float rightMax = Float.max(Theme.tickTextWidth(rightLine[0]), Theme.tickTextWidth(rightLine[1]));
			maxLabelWidth = Float.max(leftMax, rightMax);
		} else {
			maxLabelWidth = Float.max(Theme.tickTextWidth(leftLabel), Theme.tickTextWidth(rightLabel));
		}
		float padding = maxLabelWidth / 2f;
		int divisionCount = (int) (width / (maxLabelWidth + padding));
		
		// determine how many milliseconds between divisions
		long millisecondsOnScreen = maxTimestamp - minTimestamp;
		long millisecondsPerDivision = (long) Math.ceil((double) millisecondsOnScreen / (double) divisionCount);
		if(millisecondsPerDivision < 1000 && !Theme.timestampFormatter.toPattern().contains("SSS"))
			millisecondsPerDivision = 1000; // can't display milliseconds resolution
		if(millisecondsPerDivision < 60000 && !Theme.timestampFormatter.toPattern().contains("ss"))
			millisecondsPerDivision = 60000; // can't display seconds resolution
		
		Date minDate = new Date(minTimestamp);
		long firstDivisionTimestamp = minTimestamp;
		if(millisecondsPerDivision < 1000) {
			// <1s per div, so use 1/2/5/10/20/50/100/200/250/500/1000ms per div, relative to the nearest second
			millisecondsPerDivision = (millisecondsPerDivision <= 1)   ? 1 :
			                          (millisecondsPerDivision <= 2)   ? 2 :
			                          (millisecondsPerDivision <= 5)   ? 5 :
			                          (millisecondsPerDivision <= 10)  ? 10 :
			                          (millisecondsPerDivision <= 20)  ? 20 :
			                          (millisecondsPerDivision <= 50)  ? 50 :
			                          (millisecondsPerDivision <= 100) ? 100 :
			                          (millisecondsPerDivision <= 200) ? 200 :
			                          (millisecondsPerDivision <= 250) ? 250 :
			                          (millisecondsPerDivision <= 500) ? 500 :
			                                                             1000;
			firstDivisionTimestamp = new Date(minDate.getYear(), minDate.getMonth(), minDate.getDate(), minDate.getHours(), minDate.getMinutes(), minDate.getSeconds()).getTime() - 1000;
		} else if(millisecondsPerDivision < 60000) {
			// <1m per div, so use 1/2/5/10/15/20/30/60s per div, relative to the nearest minute
			millisecondsPerDivision = (millisecondsPerDivision <= 1000)  ? 1000 :
			                          (millisecondsPerDivision <= 2000)  ? 2000 :
			                          (millisecondsPerDivision <= 5000)  ? 5000 :
			                          (millisecondsPerDivision <= 10000) ? 10000 :
			                          (millisecondsPerDivision <= 15000) ? 15000 :
			                          (millisecondsPerDivision <= 20000) ? 20000 :
			                          (millisecondsPerDivision <= 30000) ? 30000 :
			                                                               60000;
			firstDivisionTimestamp = new Date(minDate.getYear(), minDate.getMonth(), minDate.getDate(), minDate.getHours(), minDate.getMinutes(), 0).getTime() - 60000;
		} else if(millisecondsPerDivision < 3600000) {
			// <1h per div, so use 1/2/5/10/15/20/30/60m per div, relative to the nearest hour
			millisecondsPerDivision = (millisecondsPerDivision <= 60000)   ? 60000 :
			                          (millisecondsPerDivision <= 120000)  ? 120000 :
			                          (millisecondsPerDivision <= 300000)  ? 300000 :
			                          (millisecondsPerDivision <= 600000)  ? 600000 :
			                          (millisecondsPerDivision <= 900000)  ? 900000 :
			                          (millisecondsPerDivision <= 1200000) ? 1200000 :
			                          (millisecondsPerDivision <= 1800000) ? 1800000 :
			                                                                 3600000;
			firstDivisionTimestamp = new Date(minDate.getYear(), minDate.getMonth(), minDate.getDate(), minDate.getHours(), 0, 0).getTime() - 3600000;
		} else if(millisecondsPerDivision < 86400000) {
			// <1d per div, so use 1/2/3/4/6/8/12/24 hours per div, relative to the nearest day
			millisecondsPerDivision = (millisecondsPerDivision <= 3600000)  ? 3600000 :
			                          (millisecondsPerDivision <= 7200000)  ? 7200000 :
			                          (millisecondsPerDivision <= 10800000) ? 10800000 :
			                          (millisecondsPerDivision <= 14400000) ? 14400000 :
			                          (millisecondsPerDivision <= 21600000) ? 21600000 :
			                          (millisecondsPerDivision <= 28800000) ? 28800000 :
			                          (millisecondsPerDivision <= 43200000) ? 43200000 :
			                                                                  86400000;
			firstDivisionTimestamp = new Date(minDate.getYear(), minDate.getMonth(), minDate.getDate(), 0, 0, 0).getTime() - 86400000;
		} else {
			// >=1d per div, so use an integer number of days, relative to the nearest day
			if(millisecondsPerDivision != 86400000)
				millisecondsPerDivision += 86400000 - (millisecondsPerDivision % 86400000);
			firstDivisionTimestamp = new Date(minDate.getYear(), minDate.getMonth(), 1, 0, 0, 0).getTime() - 86400000;
		}
		while(firstDivisionTimestamp < minTimestamp)
			firstDivisionTimestamp += millisecondsPerDivision;
		
		// populate the Map
		for(int divisionN = 0; divisionN < divisionCount; divisionN++) {
			long timestampN = firstDivisionTimestamp + (divisionN * millisecondsPerDivision);
			float pixelX = (float) (timestampN - minTimestamp) / (float) millisecondsOnScreen * width;
			String label = Theme.timestampFormatter.format(new Date(timestampN));
			if(pixelX <= width)
				divisions.put(pixelX, label);
			else
				break;
		}
		
		return divisions;
		
	}
	
	/**
	 * Formats a double as a string, limiting the total number of digits to a specific length, but never truncating the integer part.
	 * 
	 * For example, with a digitCount of 4:
	 * 1.234567 -> "1.234"
	 * 12.34567 -> "12.34"
	 * 123456.7 -> "123456"
	 * -1.23456 -> "-1.234"
	 * 
	 * @param number        The double to format.
	 * @param digitCount    How many digits to clip to.
	 * @return              The double formatted as a String.
	 */
	public static String formattedNumber(double number, int digitCount) {
		
		String text = String.format("%.9f", number);
		int pointLocation = text.indexOf('.');
		int stringLength = text.charAt(0) == '-' ? digitCount + 2 : digitCount + 1;
		return text.substring(0, pointLocation < stringLength ? stringLength : pointLocation);
		
	}
	
	/**
	 * Takes a string of text and checks that it exactly matches a format string.
	 * Throws an AssertionException if the text does not match the format string.
	 * 
	 * @param text            Line of text to examine.
	 * @param formatString    Expected line of text, for example: "GUI Settings:"
	 */
	public static void parseExact(String text, String formatString) {
		
		if(!text.equals(formatString))
			throw new AssertionError("Text does not match the expected value.");
		
	}
	
	/**
	 * Takes a string of text and attempts to extract a boolean from the end of it.
	 * Throws an AssertionException if the text does not match the format string or does not end with a boolean value.
	 * 
	 * @param text            Line of text to examine.
	 * @param formatString    Expected line of text, for example: "show x-axis title = %b"
	 * @return                The boolean value extracted from the text.
	 */
	public static boolean parseBoolean(String text, String formatString) {
		
		if(!formatString.endsWith("%b"))
			throw new AssertionError("Source code contains an invalid format string.");
		
		try {
			String expectedText = formatString.substring(0, formatString.length() - 2);
			String actualText = text.substring(0, expectedText.length());
			String token = text.substring(expectedText.length()); 
			if(actualText.equals(expectedText))
				if(token.toLowerCase().equals("true"))
					return true;
				else if(token.toLowerCase().equals("false"))
					return false;
				else
					throw new Exception();
			else
				throw new AssertionError("Text does not match the expected value.");
		} catch(Exception e) {
			throw new AssertionError("Text does not end with a boolean.");
		}
		
	}
	
	/**
	 * Takes a string of text and attempts to extract an integer from the beginning or end of it.
	 * Throws an AssertionException if the text does not match the format string or does not start/end with an integer value.
	 * 
	 * @param text            Line of text to examine.
	 * @param formatString    Expected line of text, for example: "sample count = %d"
	 * @return                The integer value extracted from the text.
	 */
	public static int parseInteger(String text, String formatString) {
		
		if(!formatString.startsWith("%d") && !formatString.endsWith("%d"))
			throw new AssertionError("Source code contains an invalid format string.");
		
		if(formatString.startsWith("%d")) {
			
			// starting with %d, so an integer should be at the start of the text
			try {
				String[] tokens = text.split(" ");
				int number = Integer.parseInt(tokens[0]);
				String expectedText = formatString.substring(2);
				String remainingText = "";
				for(int i = 1; i < tokens.length; i++)
					remainingText += " " + tokens[i];
				if(remainingText.equals(expectedText))
					return number;
				else
					throw new AssertionError("Text does not match the expected value.");
			} catch(Exception e) {
				throw new AssertionError("Text does not start with an integer.");
			}
			
		} else  {
			
			// ending with %d, so an integer should be at the end of the text
			try {
				String[] tokens = text.split(" ");
				int number = Integer.parseInt(tokens[tokens.length - 1]);
				String expectedText = formatString.substring(0, formatString.length() - 2);
				String remainingText = "";
				for(int i = 0; i < tokens.length - 1; i++)
					remainingText += tokens[i] + " ";
				if(remainingText.equals(expectedText))
					return number;
				else
					throw new AssertionError("Text does not match the expected value.");
			} catch(Exception e) {
				throw new AssertionError("Text does not end with an integer.");
			}
			
		}
		
	}
	
	/**
	 * Takes a string of text and attempts to extract an float from the beginning or end of it.
	 * Throws an AssertionException if the text does not match the format string or does not start/end with a float value.
	 * 
	 * @param text            Line of text to examine.
	 * @param formatString    Expected line of text, for example: "manual y-axis maximum = %f"
	 * @return                The float value extracted from the text.
	 */
	public static float parseFloat(String text, String formatString) {
		
		if(!formatString.startsWith("%f") && !formatString.endsWith("%f"))
			throw new AssertionError("Source code contains an invalid format string.");
		
		if(formatString.startsWith("%f")) {
			
			// starting with %f, so a float should be at the start of the text
			try {
				String[] tokens = text.split(" ");
				float number = Float.parseFloat(tokens[0]);
				String expectedText = formatString.substring(2);
				String remainingText = "";
				for(int i = 1; i < tokens.length; i++)
					remainingText += " " + tokens[i];
				if(remainingText.equals(expectedText))
					return number;
				else
					throw new AssertionError("Text does not match the expected value.");
			} catch(Exception e) {
				throw new AssertionError("Text does not start with a floating point number.");
			}
			
		} else  {
			
			// ending with %f, so a float should be at the end of the text
			try {
				String[] tokens = text.split(" ");
				float number = Float.parseFloat(tokens[tokens.length - 1]);
				String expectedText = formatString.substring(0, formatString.length() - 2);
				String remainingText = "";
				for(int i = 0; i < tokens.length - 1; i++)
					remainingText += tokens[i] + " ";
				if(remainingText.equals(expectedText))
					return number;
				else
					throw new AssertionError("Text does not match the expected value.");
			} catch(Exception e) {
				throw new AssertionError("Text does not end with a floating point number.");
			}
			
		}
		
	}
	
	/**
	 * Takes a string of text and attempts to extract a string from the end of it.
	 * Throws an AssertionException if the text does not match the format string.
	 * 
	 * @param text            Line of text to examine.
	 * @param formatString    Expected line of text, for example: "packet type = %s"
	 * @return                The String value extracted from the text.
	 */
	public static String parseString(String text, String formatString) {
		
		if(!formatString.endsWith("%s"))
			throw new AssertionError("Source code contains an invalid format string.");
		
		try {
			String expectedText = formatString.substring(0, formatString.length() - 2);
			String actualText = text.substring(0, expectedText.length());
			String token = text.substring(expectedText.length()); 
			if(actualText.equals(expectedText))
				return token;
			else
				throw new AssertionError("Text does not match the expected value.");
		} catch(Exception e) {
			throw new AssertionError("Text does not match the expected value.");
		}
		
	}
	
	/**
	 * Parses an ASCII STL file to extract it's vertices and normal vectors.
	 * 
	 * Blender users: when exporting the STL file (File > Export > Stl) ensure the "Ascii" checkbox is selected,
	 * and ensure your model fits in a bounding box from -1 to +1 Blender units, centered at the origin.
	 * 
	 * @param fileStream    InputStream of an ASCII STL file.
	 * @returns             A FloatBuffer with a layout of x1,y1,z1,u1,v1,w1... or null if the InputStream could not be parsed.
	 */
	public static FloatBuffer getShapeFromAsciiStl(InputStream fileStream) {
		
		try {
			
			// get the lines of text
			List<String> lines = new ArrayList<String>();
			Scanner s = new Scanner(fileStream);
			while(s.hasNextLine())
				lines.add(s.nextLine());
			s.close();
			
			// count the vertices
			int vertexCount = 0;
			for(String line : lines)
				if(line.startsWith("vertex"))
					vertexCount++;
			
			
			// write the vertices into the FloatBuffer
			FloatBuffer buffer = Buffers.newDirectFloatBuffer(vertexCount * 6);
			float u = 0;
			float v = 0;
			float w = 0;
			for(String line : lines) {
				if(line.startsWith("facet normal")) {
					String[] token = line.split(" ");
					u = Float.parseFloat(token[2]);
					v = Float.parseFloat(token[3]);
					w = Float.parseFloat(token[4]);
				} else if(line.startsWith("vertex")) {
					String[] token = line.split(" ");
					buffer.put(Float.parseFloat(token[1]));
					buffer.put(Float.parseFloat(token[2]));
					buffer.put(Float.parseFloat(token[3]));
					buffer.put(u);
					buffer.put(v);
					buffer.put(w);
				}
			}
			
			return buffer;
			
		} catch (Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
		
	}
	
	/**
	 * Draws a collection of markers over a plot.
	 * 
	 * @param gl              The OpenGL context.
	 * @param markers         A List of events in time and their corresponding names/colors.
	 * @param topLeftX        Allowed bounding box's top-left x coordinate.
	 * @param topLeftY        Allowed bounding box's top-left y coordinate.
	 * @param bottomRightX    Allowed bounding box's bottom-right x coordinate.
	 * @param bottomRightY    Allowed bounding box's bottom-right y coordinate.
	 */
	public static void drawMarkers(GL2 gl, List<BitfieldEvents.EventsAtSampleNumber> markers, float topLeftX, float topLeftY, float bottomRightX, float bottomRightY) {
			
		final int NORTH      = 0;
		final int NORTH_WEST = 1;
		final int NORTH_EAST = 2;
		final int UNDEFINED  = 3;
		
		float padding = 6f * Controller.getDisplayScalingFactor();
		
		List<float[]> occupiedRegions = new ArrayList<float[]>(); // [0] = minX, [1] = maxX, [2] = minY, [3] = maxY
		
		// draw each event
		boolean insufficientSpace = false;
		for(BitfieldEvents.EventsAtSampleNumber marker : markers) {
			
			// calculate the box size
			float maxTextWidth = Theme.tickTextWidth("Sample " + marker.sampleNumber);
			for(int i = 0; i < marker.names.size(); i++)
				if(Theme.tickTextWidth(marker.names.get(i)) > maxTextWidth)
					maxTextWidth = Theme.tickTextWidth(marker.names.get(i));
			float textHeight = Theme.tickTextHeight;
			
			float boxWidth = textHeight + Theme.tooltipTextPadding + maxTextWidth + (2 * padding);
			float boxHeight = (marker.names.size() + 1) * (textHeight + padding) + padding;
			
			// calculate the box anchor position
			float anchorX = marker.pixelX + topLeftX;
			float anchorY = topLeftY - boxHeight - padding;
			
			// decide which orientation to use
			int orientation = UNDEFINED;
			
			while(orientation == UNDEFINED) {
				if(anchorX - boxWidth >= topLeftX && anchorX <= bottomRightX && regionAvailable(occupiedRegions, anchorX - boxWidth, anchorX, anchorY, anchorY + boxHeight + padding))
					orientation = NORTH_WEST;
				else if(anchorX - (boxWidth / 2f) >= topLeftX && anchorX + (boxWidth / 2f) <= bottomRightX && regionAvailable(occupiedRegions, anchorX - (boxWidth / 2f), anchorX + (boxWidth / 2f), anchorY, anchorY + boxHeight + padding))
					orientation = NORTH;
				else if(anchorX >= topLeftX && anchorX + boxWidth <= bottomRightX && regionAvailable(occupiedRegions, anchorX, anchorX + boxWidth, anchorY, anchorY + boxHeight + padding))
					orientation = NORTH_EAST;
				else
					anchorY = anchorY - 1;
				
				if(anchorY < bottomRightY) {
					// not enough room to draw this marker
					insufficientSpace = true;
					break;
				}
			}
			
			// draw the marker
			if(orientation == NORTH) {
				
				OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                  anchorY,
				                                                        anchorX + (padding / 2f), anchorY + padding,
				                                                        anchorX - (padding / 2f), anchorY + padding);
				OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX - (boxWidth / 2f), anchorY + padding,
				                                                    anchorX + (boxWidth / 2f), anchorY + padding + boxHeight);
				
				OpenGL.buffer.rewind();
				OpenGL.buffer.put(anchorX - (boxWidth / 2f)); OpenGL.buffer.put(anchorY + padding + boxHeight);
				OpenGL.buffer.put(anchorX - (boxWidth / 2f)); OpenGL.buffer.put(anchorY + padding);
				OpenGL.buffer.put(anchorX - (padding / 2f));  OpenGL.buffer.put(anchorY + padding);
				OpenGL.buffer.put(anchorX);                   OpenGL.buffer.put(anchorY);
				OpenGL.buffer.put(anchorX + (padding / 2f));  OpenGL.buffer.put(anchorY + padding);
				OpenGL.buffer.put(anchorX + (boxWidth / 2f)); OpenGL.buffer.put(anchorY + padding);
				OpenGL.buffer.put(anchorX + (boxWidth / 2f)); OpenGL.buffer.put(anchorY + padding + boxHeight);
				OpenGL.buffer.rewind();
				OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 7);
				OpenGL.buffer.rewind();
				OpenGL.buffer.put(anchorX); OpenGL.buffer.put(anchorY);               OpenGL.buffer.put(Theme.tooltipBorderColor);
				OpenGL.buffer.put(anchorX); OpenGL.buffer.put(anchorY - padding * 6); OpenGL.buffer.put(Theme.tooltipBorderColor, 0, 3); OpenGL.buffer.put(0);
				OpenGL.buffer.rewind();
				OpenGL.drawColoredLines2D(gl, OpenGL.buffer, 2);
				
				occupiedRegions.add(new float[] {anchorX - (boxWidth / 2f),
				                                 anchorX + (boxWidth / 2f),
				                                 anchorY,
				                                 anchorY + padding + boxHeight});
				
				// draw the text and color boxes
				float textX = anchorX - (boxWidth / 2f) + padding + textHeight + Theme.tooltipTextPadding;
				float textY = anchorY + padding + boxHeight - (padding + textHeight);
				Theme.drawTickText("Sample " + marker.sampleNumber, (int) textX, (int) textY);
				for(int i = 0; i < marker.names.size(); i++) {
					textY = anchorY + padding + boxHeight - ((i + 2) * (padding + textHeight));
					Theme.drawTickText(marker.names.get(i), (int) textX, (int) textY);
					OpenGL.drawQuad2D(gl, marker.glColors.get(i), textX - Theme.tooltipTextPadding - textHeight, textY,
					                                              textX - Theme.tooltipTextPadding,              textY + textHeight);
				}
				
			} else if(orientation == NORTH_WEST) {
				
				OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                     anchorY,
				                                                        anchorX,                     anchorY + padding,
				                                                        anchorX - (0.85f * padding), anchorY + padding);
				OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX - boxWidth, anchorY + padding,
				                                                    anchorX,            anchorY + padding + boxHeight);
				
				OpenGL.buffer.rewind();
				OpenGL.buffer.put(anchorX - boxWidth);          OpenGL.buffer.put(anchorY + padding + boxHeight);
				OpenGL.buffer.put(anchorX - boxWidth);          OpenGL.buffer.put(anchorY + padding);
				OpenGL.buffer.put(anchorX - (0.85f * padding)); OpenGL.buffer.put(anchorY + padding);
				OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY);
				OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY + padding + boxHeight);
				OpenGL.buffer.rewind();
				OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 5);
				OpenGL.buffer.rewind();
				OpenGL.buffer.put(anchorX); OpenGL.buffer.put(anchorY);               OpenGL.buffer.put(Theme.tooltipBorderColor);
				OpenGL.buffer.put(anchorX); OpenGL.buffer.put(anchorY - padding * 6); OpenGL.buffer.put(Theme.tooltipBorderColor, 0, 3); OpenGL.buffer.put(0);
				OpenGL.buffer.rewind();
				OpenGL.drawColoredLines2D(gl, OpenGL.buffer, 2);
				
				occupiedRegions.add(new float[] {anchorX - boxWidth,
				                                 anchorX,
				                                 anchorY,
				                                 anchorY + padding + boxHeight});
				
				// draw the text and color boxes
				float textX = anchorX - boxWidth + padding + textHeight + Theme.tooltipTextPadding;
				float textY = anchorY + padding + boxHeight - (padding + textHeight);
				Theme.drawTickText("Sample " + marker.sampleNumber, (int) textX, (int) textY);
				for(int i = 0; i < marker.names.size(); i++) {
					textY = anchorY + padding + boxHeight - ((i + 2) * (padding + textHeight));
					Theme.drawTickText(marker.names.get(i), (int) textX, (int) textY);
					OpenGL.drawQuad2D(gl, marker.glColors.get(i), textX - Theme.tooltipTextPadding - textHeight, textY,
					                                              textX - Theme.tooltipTextPadding,              textY + textHeight);
				}
				
			} else if(orientation == NORTH_EAST) {
				
				OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                     anchorY + padding,
				                                                        anchorX,                     anchorY,
				                                                        anchorX + (0.85f * padding), anchorY + padding);
				OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX,            anchorY + padding,
				                                                    anchorX + boxWidth, anchorY + padding + boxHeight);
				
				OpenGL.buffer.rewind();
				OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY + padding + boxHeight);
				OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY);
				OpenGL.buffer.put(anchorX + (0.85f * padding)); OpenGL.buffer.put(anchorY + padding);
				OpenGL.buffer.put(anchorX + boxWidth);          OpenGL.buffer.put(anchorY + padding);
				OpenGL.buffer.put(anchorX + boxWidth);          OpenGL.buffer.put(anchorY + padding + boxHeight);
				OpenGL.buffer.rewind();
				OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 5);
				OpenGL.buffer.rewind();
				OpenGL.buffer.put(anchorX); OpenGL.buffer.put(anchorY);               OpenGL.buffer.put(Theme.tooltipBorderColor);
				OpenGL.buffer.put(anchorX); OpenGL.buffer.put(anchorY - padding * 6); OpenGL.buffer.put(Theme.tooltipBorderColor, 0, 3); OpenGL.buffer.put(0);
				OpenGL.buffer.rewind();
				OpenGL.drawColoredLines2D(gl, OpenGL.buffer, 2);
				
				occupiedRegions.add(new float[] {anchorX,
				                                 anchorX + boxWidth,
				                                 anchorY,
				                                 anchorY + padding + boxHeight});
				
				// draw the text and color boxes
				float textX = anchorX + padding + textHeight + Theme.tooltipTextPadding;
				float textY = anchorY + padding + boxHeight - (padding + textHeight);
				Theme.drawTickText("Sample " + marker.sampleNumber, (int) textX, (int) textY);
				for(int i = 0; i < marker.names.size(); i++) {
					textY = anchorY + padding + boxHeight - ((i + 2) * (padding + textHeight));
					Theme.drawTickText(marker.names.get(i), (int) textX, (int) textY);
					OpenGL.drawQuad2D(gl, marker.glColors.get(i), textX - Theme.tooltipTextPadding - textHeight, textY,
					                                              textX - Theme.tooltipTextPadding,              textY + textHeight);
				}
				
			}
			
		}
		
		// notify the user if not all markers could be drawn
		if(insufficientSpace) {
			
			float gradientLength = 10 * Controller.getDisplayScalingFactor();
			float[] red            = new float[] {1, 0, 0, 1};
			float[] transparentRed = new float[] {1, 0, 0, 0};
			
			// top gradient
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(topLeftX);     OpenGL.buffer.put(topLeftY);                  OpenGL.buffer.put(red);
			OpenGL.buffer.put(topLeftX);     OpenGL.buffer.put(topLeftY - gradientLength); OpenGL.buffer.put(transparentRed);
			OpenGL.buffer.put(bottomRightX); OpenGL.buffer.put(topLeftY);                  OpenGL.buffer.put(red);
			OpenGL.buffer.put(bottomRightX); OpenGL.buffer.put(topLeftY - gradientLength); OpenGL.buffer.put(transparentRed);
			OpenGL.buffer.rewind();
			OpenGL.drawColoredTriangleStrip2D(gl, OpenGL.buffer, 4);
			
			// bottom gradient
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(topLeftX);     OpenGL.buffer.put(bottomRightY + gradientLength); OpenGL.buffer.put(transparentRed);
			OpenGL.buffer.put(topLeftX);     OpenGL.buffer.put(bottomRightY);                  OpenGL.buffer.put(red);
			OpenGL.buffer.put(bottomRightX); OpenGL.buffer.put(bottomRightY + gradientLength); OpenGL.buffer.put(transparentRed);
			OpenGL.buffer.put(bottomRightX); OpenGL.buffer.put(bottomRightY);                  OpenGL.buffer.put(red);
			OpenGL.buffer.rewind();
			OpenGL.drawColoredTriangleStrip2D(gl, OpenGL.buffer, 4);
			
			// left gradient
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(topLeftX);                  OpenGL.buffer.put(topLeftY);     OpenGL.buffer.put(red);
			OpenGL.buffer.put(topLeftX);                  OpenGL.buffer.put(bottomRightY); OpenGL.buffer.put(red);
			OpenGL.buffer.put(topLeftX + gradientLength); OpenGL.buffer.put(topLeftY);     OpenGL.buffer.put(transparentRed);
			OpenGL.buffer.put(topLeftX + gradientLength); OpenGL.buffer.put(bottomRightY); OpenGL.buffer.put(transparentRed);
			OpenGL.buffer.rewind();
			OpenGL.drawColoredTriangleStrip2D(gl, OpenGL.buffer, 4);
			
			// right gradient
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(bottomRightX - gradientLength); OpenGL.buffer.put(topLeftY);     OpenGL.buffer.put(transparentRed);
			OpenGL.buffer.put(bottomRightX - gradientLength); OpenGL.buffer.put(bottomRightY); OpenGL.buffer.put(transparentRed);
			OpenGL.buffer.put(bottomRightX);                  OpenGL.buffer.put(topLeftY);     OpenGL.buffer.put(red);
			OpenGL.buffer.put(bottomRightX);                  OpenGL.buffer.put(bottomRightY); OpenGL.buffer.put(red);
			OpenGL.buffer.rewind();
			OpenGL.drawColoredTriangleStrip2D(gl, OpenGL.buffer, 4);
			
			String text = "Insufficent Room for All Markers";
			float textWidth = Theme.tickTextWidth(text);
			float textLeftX = (bottomRightX - topLeftX) / 2f + topLeftX - (textWidth / 2f);
			float textRightX = (bottomRightX - topLeftX) / 2f + topLeftX + (textWidth / 2f);
			float textBottomY = bottomRightY + padding;
			float textTopY = textBottomY + Theme.tickTextHeight;
			
			// text background
			OpenGL.drawQuad2D(gl, red, textLeftX - padding,  textBottomY - padding,
			                           textRightX + padding, textTopY + padding);
			
			Theme.drawTickText(text, (int) textLeftX, (int) textBottomY); 
						
		}
		
	}
	
	/**
	 * Checks if a region overlaps with any occupied regions.
	 * 
	 * @param occupiedRegions    A List of float[]'s describing any currently occupied regions. [0] = minX, [1] = maxX, [2] = minY, [3] = maxY
	 * @param minX               Proposed region's left-most x value.
	 * @param maxX               Proposed region's right-most x value.
	 * @param minY               Proposed region's bottom-most y value.
	 * @param maxY               Proposed region's top-most y value.
	 * @return                   True if there is no overlap (touching is allowed), false otherwise.
	 */
	private static boolean regionAvailable(List<float[]> occupiedRegions, float minX, float maxX, float minY, float maxY) {
		
		for(float[] region : occupiedRegions) {
			float regionMinX = region[0];
			float regionMaxX = region[1];
			float regionMinY = region[2];
			float regionMaxY = region[3];
			
			if(minX >= regionMinX && minX < regionMaxX) { // x starts inside region
				if(minY >= regionMinY && minY < regionMaxY) // y starts inside region
					return false;
				else if(maxY > regionMinY && maxY <= regionMaxY) // y ends inside region
					return false;
				else if(minY < regionMinY && maxY > regionMaxY) // y surrounds region
					return false;
			} else if(maxX > regionMinX && maxX <= regionMaxX) { // x ends inside region
				if(minY >= regionMinY && minY < regionMaxY) // y starts inside region
					return false;
				else if(maxY > regionMinY && maxY <= regionMaxY) // y ends inside region
					return false;
				else if(minY < regionMinY && maxY > regionMaxY) // y surrounds region
					return false;
			} else if(minX <= regionMinX && maxX >= regionMaxX) { // x surrounds region
				if(minY >= regionMinY && minY < regionMaxY) // y starts inside region
					return false;
				else if(maxY > regionMinY && maxY <= regionMaxY) // y ends inside region
					return false;
				else if(minY < regionMinY && maxY > regionMaxY) // y surrounds region
					return false;
			}
		}
		
		// not occupied
		return true;
		
	}
	
	/**
	 * Draws a tooltip. An anchor point specifies where the tooltip should point to.
	 * 
	 * @param gl              The OpenGL context.
	 * @param text            Array of text to show.
	 * @param colors          Corresponding array of colors to show at the left of each line of text, or null to not show any colors.
	 * @param anchorX         X location to point to.
	 * @param anchorY         Y location to point to.
	 * @param topLeftX        Allowed bounding box's top-left x coordinate.
	 * @param topLeftY        Allowed bounding box's top-left y coordinate.
	 * @param bottomRightX    Allowed bounding box's bottom-right x coordinate.
	 * @param bottomRightY    Allowed bounding box's bottom-right y coordinate.
	 */
	public static void drawTooltip(GL2 gl, String[] text, Color[] colors, float anchorX, float anchorY, float topLeftX, float topLeftY, float bottomRightX, float bottomRightY) {
			
		final int NORTH      = 0;
		final int SOUTH      = 1;
		final int WEST       = 2;
		final int EAST       = 3;
		final int NORTH_WEST = 4;
		final int NORTH_EAST = 5;
		final int SOUTH_WEST = 6;
		final int SOUTH_EAST = 7;
		
		float padding = 6f * Controller.getDisplayScalingFactor();
		
		float maxTextWidth = Theme.tickTextWidth(text[0]);
		for(int i = 1; i < text.length; i++)
			if(Theme.tickTextWidth(text[i]) > maxTextWidth)
				maxTextWidth = Theme.tickTextWidth(text[i]);
		float textHeight = Theme.tickTextHeight;
		
		float boxWidth = textHeight + Theme.tooltipTextPadding + maxTextWidth + (2 * padding);
		if(colors == null)
			boxWidth -= textHeight + Theme.tooltipTextPadding;
		float boxHeight = text.length * (textHeight + padding) + padding;
		
		// decide which orientation to draw the tooltip in, or return if there is not enough space
		int orientation = NORTH;
		if(anchorY + padding + boxHeight <= topLeftY) {
			// there is space above the anchor, so use NORTH or NORTH_WEST or NORTH_EAST if there is enough horizontal space
			if(anchorX - (boxWidth / 2f) >= topLeftX && anchorX + (boxWidth / 2f) <= bottomRightX)
				orientation = NORTH;
			else if(anchorX - boxWidth >= topLeftX && anchorX <= bottomRightX)
				orientation = NORTH_WEST;
			else if(anchorX >= topLeftX && anchorX + boxWidth <= bottomRightX)
				orientation = NORTH_EAST;
			else
				return;
		} else if(anchorY + (boxHeight / 2f) <= topLeftY && anchorY - (boxHeight / 2f) >= bottomRightY) {
			// there is some space above and below the anchor, so use WEST or EAST if there is enough horizontal space
			if(anchorX - padding - boxWidth >= topLeftX)
				orientation = WEST;
			else if(anchorX + padding + boxWidth <= bottomRightX)
				orientation = EAST;
			else
				return;
		} else if(anchorY - padding - boxHeight >= bottomRightY) {
			// there is space below the anchor, so use SOUTH or SOUTH_WEST or SOUTH_EAST if there is enough horizontal space
			if(anchorX - (boxWidth / 2f) >= topLeftX && anchorX + (boxWidth / 2f) <= bottomRightX)
				orientation = SOUTH;
			else if(anchorX - boxWidth >= topLeftX && anchorX <= bottomRightX)
				orientation = SOUTH_WEST;
			else if(anchorX >= topLeftX && anchorX + boxWidth <= bottomRightX)
				orientation = SOUTH_EAST;
			else
				return;
		} else {
			// there is not enough space anywhere
			return;
		}
		
		float[][] glColors = new float[colors == null ? 0 : colors.length][];
		if(colors != null)
			for(int i = 0; i < colors.length; i++)
				glColors[i] = new float[] {colors[i].getRed() / 255f, colors[i].getGreen() / 255f, colors[i].getBlue() / 255f, 1};
		
		// draw the tooltip
		if(orientation == NORTH) {
			
			OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                  anchorY,
			                                                        anchorX + (padding / 2f), anchorY + padding,
			                                                        anchorX - (padding / 2f), anchorY + padding);
			OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX - (boxWidth / 2f), anchorY + padding,
			                                                    anchorX + (boxWidth / 2f), anchorY + padding + boxHeight);
			
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(anchorX - (boxWidth / 2f)); OpenGL.buffer.put(anchorY + padding + boxHeight);
			OpenGL.buffer.put(anchorX - (boxWidth / 2f)); OpenGL.buffer.put(anchorY + padding);
			OpenGL.buffer.put(anchorX - (padding / 2f));  OpenGL.buffer.put(anchorY + padding);
			OpenGL.buffer.put(anchorX);                   OpenGL.buffer.put(anchorY);
			OpenGL.buffer.put(anchorX + (padding / 2f));  OpenGL.buffer.put(anchorY + padding);
			OpenGL.buffer.put(anchorX + (boxWidth / 2f)); OpenGL.buffer.put(anchorY + padding);
			OpenGL.buffer.put(anchorX + (boxWidth / 2f)); OpenGL.buffer.put(anchorY + padding + boxHeight);
			OpenGL.buffer.rewind();
			OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 7);
			
			// draw the text and color boxes
			float textX = anchorX - (boxWidth / 2f) + padding + textHeight + Theme.tooltipTextPadding;
			for(int i = 0; i < text.length; i++) {
				if(colors == null)
					textX = anchorX - (boxWidth / 2f) + (boxWidth - Theme.tickTextWidth(text[i])) / 2;
				float textY = anchorY + padding + boxHeight - ((i + 1) * (padding + textHeight));
				Theme.drawTickText(text[i], (int) textX, (int) textY);
				if(colors != null)
					OpenGL.drawQuad2D(gl, glColors[i], textX - Theme.tooltipTextPadding - textHeight, textY,
					                                   textX - Theme.tooltipTextPadding,              textY + textHeight);
			}
			
		} else if(orientation == SOUTH) {
			
			OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                  anchorY, 
			                                                        anchorX - (padding / 2f), anchorY - padding,
			                                                        anchorX + (padding / 2f), anchorY - padding);
			OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX - (boxWidth / 2f), anchorY - padding - boxHeight,
			                                                    anchorX + (boxWidth / 2f), anchorY - padding);
			
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(anchorX - (boxWidth / 2f)); OpenGL.buffer.put(anchorY - padding);
			OpenGL.buffer.put(anchorX - (boxWidth / 2f)); OpenGL.buffer.put(anchorY - padding - boxHeight);
			OpenGL.buffer.put(anchorX + (boxWidth / 2f)); OpenGL.buffer.put(anchorY - padding - boxHeight);
			OpenGL.buffer.put(anchorX + (boxWidth / 2f)); OpenGL.buffer.put(anchorY - padding);
			OpenGL.buffer.put(anchorX + (padding / 2f));  OpenGL.buffer.put(anchorY - padding);
			OpenGL.buffer.put(anchorX);                   OpenGL.buffer.put(anchorY);
			OpenGL.buffer.put(anchorX - (padding / 2f));  OpenGL.buffer.put(anchorY - padding);
			OpenGL.buffer.rewind();
			OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 7);
			
			// draw the text and color boxes
			float textX = anchorX - (boxWidth / 2f) + padding + textHeight + Theme.tooltipTextPadding;
			for(int i = 0; i < text.length; i++) {
				if(colors == null)
					textX = anchorX - (boxWidth / 2f) + (boxWidth - Theme.tickTextWidth(text[i])) / 2;
				float textY = anchorY - padding - ((i + 1) * (padding + textHeight));
				Theme.drawTickText(text[i], (int) textX, (int) textY);
				if(colors != null)
					OpenGL.drawQuad2D(gl, glColors[i], textX - Theme.tooltipTextPadding - textHeight, textY,
					                                   textX - Theme.tooltipTextPadding,              textY + textHeight);
			}
			
		} else if(orientation == WEST) {
			
			OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX, anchorY,
			                                                        anchorX - padding, anchorY + (padding / 2f),
			                                                        anchorX - padding, anchorY - (padding / 2f));
			OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX - padding - boxWidth, anchorY - (boxHeight / 2f),
			                                                    anchorX - padding,            anchorY + (boxHeight / 2f));
			
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(anchorX - padding - boxWidth); OpenGL.buffer.put(anchorY + (boxHeight / 2f));
			OpenGL.buffer.put(anchorX - padding - boxWidth); OpenGL.buffer.put(anchorY - (boxHeight / 2f));
			OpenGL.buffer.put(anchorX - padding);            OpenGL.buffer.put(anchorY - (boxHeight / 2f));
			OpenGL.buffer.put(anchorX - padding);            OpenGL.buffer.put(anchorY - (padding / 2f));
			OpenGL.buffer.put(anchorX);                      OpenGL.buffer.put(anchorY);
			OpenGL.buffer.put(anchorX - padding);            OpenGL.buffer.put(anchorY + (padding / 2f));
			OpenGL.buffer.put(anchorX - padding);            OpenGL.buffer.put(anchorY + (boxHeight / 2f));
			OpenGL.buffer.rewind();
			OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 7);
			
			// draw the text and color boxes
			float textX = anchorX - boxWidth + textHeight + Theme.tooltipTextPadding;
			for(int i = 0; i < text.length; i++) {
				if(colors == null)
					textX = anchorX - padding - boxWidth + (boxWidth - Theme.tickTextWidth(text[i])) / 2;
				float textY = anchorY + (boxHeight / 2f) - ((i + 1) * (padding + textHeight));
				Theme.drawTickText(text[i], (int) textX, (int) textY);
				if(colors != null)
					OpenGL.drawQuad2D(gl, glColors[i], textX - Theme.tooltipTextPadding - textHeight, textY,
					                                   textX - Theme.tooltipTextPadding,              textY + textHeight);
			}
			
		} else if(orientation == EAST) {
			
			OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,           anchorY,
			                                                        anchorX + padding, anchorY - (padding / 2f),
			                                                        anchorX + padding, anchorY + (padding / 2f));
			OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX + padding,            anchorY - (boxHeight / 2f),
			                                                    anchorX + padding + boxWidth, anchorY + (boxHeight / 2f));
			
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(anchorX + padding);            OpenGL.buffer.put(anchorY + (boxHeight / 2f));
			OpenGL.buffer.put(anchorX + padding);            OpenGL.buffer.put(anchorY + (padding / 2f));
			OpenGL.buffer.put(anchorX);                      OpenGL.buffer.put(anchorY);
			OpenGL.buffer.put(anchorX + padding);            OpenGL.buffer.put(anchorY - (padding / 2f));
			OpenGL.buffer.put(anchorX + padding);            OpenGL.buffer.put(anchorY - (boxHeight / 2f));
			OpenGL.buffer.put(anchorX + padding + boxWidth); OpenGL.buffer.put(anchorY - (boxHeight / 2f));
			OpenGL.buffer.put(anchorX + padding + boxWidth); OpenGL.buffer.put(anchorY + (boxHeight / 2f));
			OpenGL.buffer.rewind();
			OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 7);
			
			// draw the text and color boxes
			float textX = anchorX + (2f * padding) + textHeight + Theme.tooltipTextPadding;
			for(int i = 0; i < text.length; i++) {
				if(colors == null)
					textX = anchorX + padding + (boxWidth - Theme.tickTextWidth(text[i])) / 2;
				float textY = anchorY + (boxHeight / 2f) - ((i + 1) * (padding + textHeight));
				Theme.drawTickText(text[i], (int) textX, (int) textY);
				if(colors != null)
					OpenGL.drawQuad2D(gl, glColors[i], textX - Theme.tooltipTextPadding - textHeight, textY,
					                                   textX - Theme.tooltipTextPadding,              textY + textHeight);
			}
			
		} else if(orientation == NORTH_WEST) {
			
			OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                     anchorY,
			                                                        anchorX,                     anchorY + padding,
			                                                        anchorX - (0.85f * padding), anchorY + padding);
			OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX - boxWidth, anchorY + padding,
			                                                    anchorX,            anchorY + padding + boxHeight);
			
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(anchorX - boxWidth);          OpenGL.buffer.put(anchorY + padding + boxHeight);
			OpenGL.buffer.put(anchorX - boxWidth);          OpenGL.buffer.put(anchorY + padding);
			OpenGL.buffer.put(anchorX - (0.85f * padding)); OpenGL.buffer.put(anchorY + padding);
			OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY);
			OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY + padding + boxHeight);
			OpenGL.buffer.rewind();
			OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 5);
			
			// draw the text and color boxes
			float textX = anchorX - boxWidth + padding + textHeight + Theme.tooltipTextPadding;
			for(int i = 0; i < text.length; i++) {
				if(colors == null)
					textX = anchorX - boxWidth + (boxWidth - Theme.tickTextWidth(text[i])) / 2;
				float textY = anchorY + padding + boxHeight - ((i + 1) * (padding + textHeight));
				Theme.drawTickText(text[i], (int) textX, (int) textY);
				if(colors != null)
					OpenGL.drawQuad2D(gl, glColors[i], textX - Theme.tooltipTextPadding - textHeight, textY,
					                                   textX - Theme.tooltipTextPadding,              textY + textHeight);
			}
			
		} else if(orientation == NORTH_EAST) {
			
			OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                     anchorY + padding,
			                                                        anchorX,                     anchorY,
			                                                        anchorX + (0.85f * padding), anchorY + padding);
			OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX,            anchorY + padding,
			                                                    anchorX + boxWidth, anchorY + padding + boxHeight);
			
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY + padding + boxHeight);
			OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY);
			OpenGL.buffer.put(anchorX + (0.85f * padding)); OpenGL.buffer.put(anchorY + padding);
			OpenGL.buffer.put(anchorX + boxWidth);          OpenGL.buffer.put(anchorY + padding);
			OpenGL.buffer.put(anchorX + boxWidth);          OpenGL.buffer.put(anchorY + padding + boxHeight);
			OpenGL.buffer.rewind();
			OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 5);
			
			// draw the text and color boxes
			float textX = anchorX + padding + textHeight + Theme.tooltipTextPadding;
			for(int i = 0; i < text.length; i++) {
				if(colors == null)
					textX = anchorX + (boxWidth - Theme.tickTextWidth(text[i])) / 2;
				float textY = anchorY + padding + boxHeight - ((i + 1) * (padding + textHeight));
				Theme.drawTickText(text[i], (int) textX, (int) textY);
				if(colors != null)
					OpenGL.drawQuad2D(gl, glColors[i], textX - Theme.tooltipTextPadding - textHeight, textY,
					                                   textX - Theme.tooltipTextPadding,              textY + textHeight);
			}
			
		} else if(orientation == SOUTH_WEST) {
			
			OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                     anchorY,
			                                                        anchorX - (0.85f * padding), anchorY - padding,
			                                                        anchorX,                     anchorY - padding);
			OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX - boxWidth, anchorY - padding - boxHeight,
			                                                    anchorX,            anchorY - padding);
			
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(anchorX - boxWidth);          OpenGL.buffer.put(anchorY - padding);
			OpenGL.buffer.put(anchorX - boxWidth);          OpenGL.buffer.put(anchorY - padding - boxHeight);
			OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY - padding - boxHeight);
			OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY);
			OpenGL.buffer.put(anchorX - (0.85f * padding)); OpenGL.buffer.put(anchorY - padding);
			OpenGL.buffer.rewind();
			OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 5);
			
			// draw the text and color boxes
			float textX = anchorX - boxWidth + padding + textHeight + Theme.tooltipTextPadding;
			for(int i = 0; i < text.length; i++) {
				if(colors == null)
					textX = anchorX - boxWidth + (boxWidth - Theme.tickTextWidth(text[i])) / 2;
				float textY = anchorY - padding - ((i + 1) * (padding + textHeight));
				Theme.drawTickText(text[i], (int) textX, (int) textY);
				if(colors != null)
					OpenGL.drawQuad2D(gl, glColors[i], textX - Theme.tooltipTextPadding - textHeight, textY,
					                                   textX - Theme.tooltipTextPadding,              textY + textHeight);
			}
			
		} else if(orientation == SOUTH_EAST) {
			
			OpenGL.drawTriangle2D(gl, Theme.tooltipBackgroundColor, anchorX,                     anchorY,
			                                                        anchorX,                     anchorY - padding,
			                                                        anchorX + (0.85f * padding), anchorY - padding);
			OpenGL.drawQuad2D(gl, Theme.tooltipBackgroundColor, anchorX,            anchorY - padding - boxHeight,
			                                                    anchorX + boxWidth, anchorY - padding);
			
			OpenGL.buffer.rewind();
			OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY);
			OpenGL.buffer.put(anchorX);                     OpenGL.buffer.put(anchorY - padding - boxHeight);
			OpenGL.buffer.put(anchorX + boxWidth);          OpenGL.buffer.put(anchorY - padding - boxHeight);
			OpenGL.buffer.put(anchorX + boxWidth);          OpenGL.buffer.put(anchorY - padding);
			OpenGL.buffer.put(anchorX + (0.85f * padding)); OpenGL.buffer.put(anchorY - padding);
			OpenGL.buffer.rewind();
			OpenGL.drawLineLoop2D(gl, Theme.tooltipBorderColor, OpenGL.buffer, 5);
			
			// draw the text and color boxes
			float textX = anchorX + padding + textHeight + Theme.tooltipTextPadding;
			for(int i = 0; i < text.length; i++) {
				if(colors == null)
					textX = anchorX + (boxWidth - Theme.tickTextWidth(text[i])) / 2;
				float textY = anchorY - padding - ((i + 1) * (padding + textHeight));
				Theme.drawTickText(text[i], (int) textX, (int) textY);
				if(colors != null)
					OpenGL.drawQuad2D(gl, glColors[i], textX - Theme.tooltipTextPadding - textHeight, textY,
					                                   textX - Theme.tooltipTextPadding,              textY + textHeight);
			}
			
		}
		
	}
	
}
