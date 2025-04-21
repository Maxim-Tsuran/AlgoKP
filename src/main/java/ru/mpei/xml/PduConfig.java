package ru.mpei.xml;

import lombok.Data;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Data
public class  PduConfig {
    private String gocbRef;
    private String datSet;
    private String goID;
    private String t;
    private int timeAllowedToLive;
    private int confRev;
    private int stNum;
    private int sqNum;
    private boolean simulation;
    private boolean ndsCom;
    private int numDatSetEntries;
    private List<DataEntry> allData;

    public String updateTimestamp() {
        Instant now = Instant.now();
        ZonedDateTime utc = now.atZone(ZoneOffset.UTC);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM  d, yyyy HH:mm:ss.SSSSSSSSS 'UTC'", Locale.ENGLISH);
        this.t = formatter.format(utc);
        System.out.println(t);
        return t;
    }

}

