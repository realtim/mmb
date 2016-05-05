package ru.mmb.loggermanager.activity.dataload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoggerReplyParser {
    public static final Pattern REGEXP_LOG_DATA = Pattern.compile("(\\d{2}), (\\d{2}), (\\d{8}), (\\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2}), CRC8=(\\d+)");
    public static final Pattern REGEXP_TO_CHECK_CRC = Pattern.compile("(\\d{2}, \\d{2}, \\d{8}, \\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2}), CRC8=\\d+");

    private final LoggerDataLoadBluetoothClient owner;

    public LoggerReplyParser(LoggerDataLoadBluetoothClient owner) {
        this.owner = owner;
    }

    public void parseLogData(String loggerReply) {
        InternalParser parser = new InternalParser();
        parser.processLogData(loggerReply);
        owner.writeToConsole("data check finished");
    }

    private class InternalParser extends LoggerReplyProcessor {
        private int linesProcessed = 0;

        @Override
        protected void processLogLine(String logLine) {
            linesProcessed++;
            if (linesProcessed % 50 == 0) {
                owner.writeToConsole("lines processed: " + linesProcessed);
            }

            LogStringParsingResult parsingResult = parseReplyString(logLine);
            if (parsingResult.isError()) {
                owner.writeToConsole(parsingResult.getErrorMessage());
            }
        }

        private LogStringParsingResult parseReplyString(String replyString) {
            LogStringParsingResult result = new LogStringParsingResult(replyString);
            Matcher matcher = REGEXP_LOG_DATA.matcher(replyString);
            if (matcher.find()) {
                result.setRegexpMatches(true);
                int crc = Integer.parseInt(matcher.group(5));
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
}
