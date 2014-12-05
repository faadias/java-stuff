/* ExtendedDateFormat.java -- A class for formatting dates
 * Copyright (C) 2014  Felipe Augusto Araujo Dias (@faadias1)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 * 
 */

import java.text.DateFormatSymbols;
import java.util.*;

/**
 * 
 * @author Felipe Augusto Araujo Dias (@faadias1)
 * @version 1.0.0
 *
 * ExtendedDateFormat is a Gregorian calendar-based date formatting 
 * tool. Its behaviour is the same of java.text.SimpleDateFormat 
 * (JDK7), except for the new parameters 'o' and 'O', which map 
 * respectively to 'day of month ordinal' and 'day of year ordinal'; 
 * thus, a pattern such as "'Today is' MMM do, yyyy" will result in 
 * "Today is Dec 5th, 2014".
 * 
 */
public class ExtendedDateFormat {
	
	protected static final char SINGLE_QUOTE = '\'';
	protected static final Set<Character> formatters = new HashSet<Character>(
		Arrays.asList(
			/*  'o' is the ordinal for day of month
			 *  'O' is the ordinal for day of year
			 *  all other chars are documented within JAVA's SimpleDateFormat class
			 */
			new Character[] {'G','y','Y','M','w','W','D','d','F','E','a','h','H','k','K','m','s','S','z','Z','o','O'}
		)
	);
	
	protected String pattern = null;
	protected Locale locale = null;
	protected List<FormatToken> tokens = null;
	protected GregorianCalendar cal = null;
	protected DateFormatSymbols dfs = null;
	
	protected enum FormatType {TEXT, FORMATTER, SEPARATOR}
	
	protected class FormatToken {
		String content = "";
		FormatType type = null;
		
		@Override
		public String toString() {
			return "{content : \"" + content.replace("\"", "\\\"") + "\", " + "type : " + (type == null ? "null" : "\"" + type + "\"") + "}";
		}
	}
	
	
	public ExtendedDateFormat(String pattern, Locale locale) {
		this.pattern = pattern == null ? "" : pattern;
		this.locale = locale == null ? Locale.getDefault() : locale;
		this.tokens = new ArrayList<FormatToken>();
		this.cal = (GregorianCalendar) Calendar.getInstance();
		this.dfs = DateFormatSymbols.getInstance(locale);
		
		lexAnalyzer();
	}
	
	public ExtendedDateFormat(String pattern) {
		this(pattern, Locale.getDefault());
	}

	
	public final String format(Date date) {
		StringBuffer buffer = new StringBuffer();
		cal.setTime(date);
		
		for (FormatToken token : tokens) {
			if (token.content.length() > 0 && token.type != null) {
				buffer.append(getFormattedField(token));
			}
		}
		
		return buffer.toString();
	}
	
	protected String getFormattedField(FormatToken token) {
		if (token.type == FormatType.SEPARATOR) {
			return token.content;
		}
		
		if (token.type == FormatType.TEXT) {
			return token.content.replaceAll("^'|'$", "").replaceAll("''","'");
		}
		
		//token.type == FormatType.FORMATTER
		char c = token.content.charAt(0);
		int length = token.content.length();
		String format = c == 'Z' ? "%04d" : "%0" + length + "d";
		
		switch (c) {
			case 'G':
				return dfs.getEras()[cal.get(Calendar.ERA)];
			case 'y':
			case 'Y':
				if (length == 2) return String.valueOf(cal.get(Calendar.YEAR) % 100);
				return String.format(format, cal.get(Calendar.YEAR));
			case 'M':
				if (length < 3) return String.valueOf(cal.get(Calendar.MONTH)+1);
				if (length > 3) return dfs.getMonths()[cal.get(Calendar.MONTH)];
				return dfs.getShortMonths()[cal.get(Calendar.MONTH)];
			case 'w':
				return String.format(format, cal.get(Calendar.WEEK_OF_YEAR));
			case 'W':
				return String.format(format, cal.get(Calendar.WEEK_OF_MONTH));
			case 'D':
				return String.format(format, cal.get(Calendar.DAY_OF_YEAR));
			case 'd':
				return String.format(format, cal.get(Calendar.DAY_OF_MONTH));
			case 'F':
				return String.format(format, cal.get(Calendar.DAY_OF_WEEK_IN_MONTH));
			case 'E':
				if (length > 3) return dfs.getWeekdays()[cal.get(Calendar.DAY_OF_WEEK)];
				return dfs.getShortWeekdays()[cal.get(Calendar.DAY_OF_WEEK)];
			case 'a':
				return dfs.getAmPmStrings()[cal.get(Calendar.AM_PM)];
			case 'h':
				return String.format(format, cal.get(Calendar.HOUR) == 0 ? 12 : cal.get(Calendar.HOUR));
			case 'H':
				return String.format(format, cal.get(Calendar.HOUR_OF_DAY));
			case 'k':
				return String.format(format, cal.get(Calendar.HOUR_OF_DAY) == 0 ? 24 : cal.get(Calendar.HOUR_OF_DAY));
			case 'K':
				return String.format(format, cal.get(Calendar.HOUR));
			case 'm':
				return String.format(format, cal.get(Calendar.MINUTE));
			case 's':
				return String.format(format, cal.get(Calendar.SECOND));
			case 'S':
				return String.format(format, cal.get(Calendar.MILLISECOND));
			case 'z':
				TimeZone tz = cal.getTimeZone();
				boolean isDST = cal.get(Calendar.DST_OFFSET) != 0;
				return tz.getDisplayName(isDST, length < 4 ? TimeZone.SHORT : TimeZone.LONG, locale);
			case 'Z':
				int pureMinutes = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
				pureMinutes = Math.abs(pureMinutes);
				
				char sign = pureMinutes < 0 ? '-' : '+';
				int hours = pureMinutes / 60;
				int minutes = pureMinutes % 60;
				String text = sign + String.format(format, hours * 100 + minutes);
				return text;
			case 'o':
				return getOrderString(cal.get(Calendar.DAY_OF_MONTH));
			case 'O':
				return getOrderString(cal.get(Calendar.DAY_OF_YEAR));
			default:
				throw new IllegalArgumentException ("Illegal pattern character " + c);
		}
	}
	
	protected String getOrderString(int day) {
		switch(day % 10) {
		case 1:
			return (day % 100) != 1 ? "st" : "th";
		case 2:
			return (day % 100) != 1 ? "nd" : "th";
		case 3:
			return (day % 100) != 1 ? "rd" : "th";
		default:
			return "th";
		}
	}
	
	protected void lexAnalyzer() {
		boolean quoteOpened = false;
		Character lastFormatterChar = null;
		FormatToken token = new FormatToken();
		
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			
			if (quoteOpened) {
				if (c == SINGLE_QUOTE) {
					token.content += c;
					if (i+1 < pattern.length() && pattern.charAt(i+1) == SINGLE_QUOTE) {
						token.content += SINGLE_QUOTE;
						i++;
					}
					else {
						quoteOpened = false;
					}
				}
				else {
					token.content += c;
				}
			}
			else {
				if (formatters.contains(c)) {
					if (token.type == null) {
						token.type = FormatType.FORMATTER;
					}
					
					if (token.type != FormatType.FORMATTER || ( !"".equals(token.content) && c != lastFormatterChar )) {
						tokens.add(token);
						token = new FormatToken();
						token.type = FormatType.FORMATTER;
					}

					token.content += c;
					lastFormatterChar = c;
				}
				else if (c == SINGLE_QUOTE) {
					quoteOpened = true;
					
					if (token.type != FormatType.TEXT) {
						tokens.add(token);
						token = new FormatToken();
						token.type = FormatType.TEXT;
					}
					token.content += c;
					lastFormatterChar = null;
				}
				else {
					if (token.type == null) {
						token.type = FormatType.SEPARATOR;
					}
					
					if (token.type != FormatType.SEPARATOR) {
						tokens.add(token);
						token = new FormatToken();
						token.type = FormatType.SEPARATOR;
					}
					
					token.content += c;
					lastFormatterChar = null;
				}
			}
		}
		
		if (token.type != null) {
			tokens.add(token);
		}
	}
}
