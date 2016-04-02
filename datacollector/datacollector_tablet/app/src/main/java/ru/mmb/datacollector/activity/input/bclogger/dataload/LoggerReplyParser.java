package ru.mmb.datacollector.activity.input.bclogger.dataload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoggerReplyParser {
    public static final Pattern REGEXP_LOG_DATA = Pattern.compile("(\\d{2}), (\\d{2}), (\\d{8}), (\\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2}), Line#=(\\d+), CRC8=(\\d+)");
    public static final Pattern REGEXP_TO_CHECK_CRC = Pattern.compile("(\\d{2}, \\d{2}, \\d{8}, \\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2}), Line#=\\d+, CRC8=\\d+");

    private final LoggerDataProcessor owner;
    private final String confLoggerId;

    public LoggerReplyParser(LoggerDataProcessor owner, String confLoggerId) {
        this.owner = owner;
        this.confLoggerId = confLoggerId;
    }

    public void parseAndSaveLogData(String loggerReply) {
        LoggerDataSaver dataSaver = new LoggerDataSaver(owner);
        dataSaver.init();
        try {
            String[] replyStrings = loggerReply.split("\\n");
            boolean inDataLines = false;
            int linesProcessed = 0;
            for (String replyString : replyStrings) {
                // stop parsing if thread is terminated
                if (owner.isTerminated()) {
                    break;
                }
                if ("====".equals(replyString.trim())) {
                    if (!inDataLines) {
                        // got start of data lines marker
                        inDataLines = true;
                        continue;
                    } else {
                        // got end of data lines marker
                        break;
                    }
                }
                if (!inDataLines) continue;

                linesProcessed++;
                if (linesProcessed % 50 == 0) {
                    owner.writeToConsole("lines processed: " + linesProcessed);
                }

                LogStringParsingResult parsingResult = parseReplyString(replyString);
                if (parsingResult.isRegexpMatches() && parsingResult.isCrcFailed()) {
                    if (owner.needRepeatLineRequest()) {
                        parsingResult = owner.repeatLineRequest(parsingResult.getLineNumber());
                        if (parsingResult == null) {
                            owner.writeError("ERROR repeating on bad CRC was not successful");
                            continue;
                        }
                    } else {
                        owner.writeError("ERROR repeating on bad CRC was not successful");
                        continue;
                    }
                }

                parsingResult.checkConsistencyErrors(confLoggerId);

                if (parsingResult.isFatalError()) {
                    owner.writeError(parsingResult.getErrorMessage());
                } else {
                    // dont ignore data from other scan points
                    if (parsingResult.isWrongRecordDateTime()) {
                        owner.writeError(parsingResult.getErrorMessage());
                    } else {
                        dataSaver.saveToDB(parsingResult);
                    }
                }
            }
            dataSaver.flushData();
            owner.writeToConsole("log import finished");
        } finally {
            dataSaver.releaseResources();
        }
    }

    public LogStringParsingResult parseReplyString(String replyString) {
        LogStringParsingResult result = new LogStringParsingResult(replyString);
        Matcher matcher = REGEXP_LOG_DATA.matcher(replyString);
        if (matcher.find()) {
            result.setRegexpMatches(true);
            result.setLoggerId(matcher.group(1));
            result.setScanpointOrder(matcher.group(2));
            result.setTeamInfo(matcher.group(3));
            result.setRecordDateTime(matcher.group(4));
            result.setLineNumber(matcher.group(5));
            int crc = Integer.parseInt(matcher.group(6));
            if (!checkCRC(replyString, crc)) {
                result.setCrcFailed(true);
            }
        } else {
            result.setRegexpMatches(false);
        }
        return result;
    }

    private boolean checkCRC(String replyString, int crcToCompare) {
        Matcher matcher = REGEXP_TO_CHECK_CRC.matcher(replyString);
        matcher.find();
        int crcCalculated = crc8(matcher.group(1));
        return crcToCompare == crcCalculated;
    }

    /*
     * Tom Meyers
     * Tuesday January 14th, 2014 at 10:55 pm	 (UTC 1) Link to this comment
     * I searched all over for a Java version to interface with my Arduino messages, I developed this (below) and it seems to work. Maybe some else can use it.
     */
    private int crc8(String stringData) {
        byte crc = 0x00;
        for (int i = 0; i < stringData.length(); i++) {
            byte extract = (byte) stringData.charAt(i);
            for (byte tempI = 8; tempI > 0; tempI--) {
                byte sum = (byte) ((crc & 0xFF) ^ (extract & 0xFF));
                sum = (byte) ((sum & 0xFF) &
                        0x01); // I had Problems writing this as one line with previous
                crc = (byte) ((crc & 0xFF) >>> 1);
                if (sum != 0) {
                    crc = (byte) ((crc & 0xFF) ^ 0x8C);
                }
                extract = (byte) ((extract & 0xFF) >>> 1);
            }
        }
        return (int) (crc & 0xFF);
    }
}
