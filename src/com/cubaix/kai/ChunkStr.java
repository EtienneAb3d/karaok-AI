package com.cubaix.kai;

import java.util.concurrent.TimeUnit;

public class ChunkStr {
	
	private String text;
	private Long startTime;
	private Long EndTime;
	public int[] editorTimestampLine;
	
	public ChunkStr() {
		editorTimestampLine = new int[2];
	}
	
	
	public void setEditorTimestampLine(int charIdxBeggin, int charIdxEnd) {
		editorTimestampLine[0] = charIdxBeggin;
		editorTimestampLine[1] = charIdxEnd;
	}


	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Long getStartTime() {
		return startTime;
	}
	public String getStartTimeToFormatString() {
		return getTimeFormatFromMs(startTime);
	}
	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}
	public Long getEndTime() {
		return EndTime;
	}
	public String getEndTimeToFormatString() {
		return getTimeFormatFromMs(EndTime);
	}
	public void setEndTime(Long endTime) {
		EndTime = endTime;
	}
	
	public static String getTimeFormatFromMs(Long TimeMs) {
		return String.format("%02d:%02d.%03d", 
			    TimeUnit.MILLISECONDS.toMinutes(TimeMs),
			    TimeUnit.MILLISECONDS.toSeconds(TimeMs) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TimeMs)),
			    TimeMs - TimeUnit.MINUTES.toMillis(TimeUnit.MILLISECONDS.toMinutes(TimeMs)) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(TimeMs) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TimeMs)))
		);
	}
	
	public String getStartTimeToFormatTimestamp() {
		return "00:"+getTimeFormatFromMs(startTime);
	}
	
	public String getEndTimeToFormatTimestamp() {
		return "00:"+getTimeFormatFromMs(EndTime);
	}
	
	public static String getTimeMSToFormatTimestamp(Long time) {
		return "00:"+getTimeFormatFromMs(time);
	}
	
	

}
