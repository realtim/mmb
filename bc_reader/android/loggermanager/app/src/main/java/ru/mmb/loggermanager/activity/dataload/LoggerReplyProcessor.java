package ru.mmb.loggermanager.activity.dataload;

public abstract class LoggerReplyProcessor {
    protected abstract void processLogLine(String logLine);

    public void processLogData(String loggerReply) {
        String[] replyStrings = loggerReply.split("\\n");
        boolean inDataLines = false;
        for (String replyString : replyStrings) {
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
            processLogLine(replyString);
        }
    }
}
