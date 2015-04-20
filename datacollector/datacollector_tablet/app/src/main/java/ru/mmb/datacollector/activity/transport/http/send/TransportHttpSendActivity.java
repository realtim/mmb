package ru.mmb.datacollector.activity.transport.http.send;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.widget.ConsoleMessagesAppender;

import static ru.mmb.datacollector.activity.transport.http.send.TransportHttpSendActivityState.STATE_IDLE;
import static ru.mmb.datacollector.activity.transport.http.send.TransportHttpSendActivityState.STATE_HTTP_SENDING;

public class TransportHttpSendActivity extends Activity {
    private TransportHttpSendActivityState currentState;

    private Button btnSendData;
    private Button btnClearConsole;
    private TextView areaConsole;

    private ConsoleMessagesAppender consoleAppender;
    private Handler httpHandler;
    private TransportHttpClient httpClient;
    private Thread httpThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new TransportHttpSendActivityState();

        setContentView(R.layout.transport_http_send);

        btnSendData = (Button) findViewById(R.id.transportHttpSend_sendDataButton);
        btnClearConsole = (Button) findViewById(R.id.transportHttpSend_clearConsoleButton);
        areaConsole = (TextView) findViewById(R.id.transportHttpSend_consoleTextView);

        btnSendData.setOnClickListener(new SendDataClickListener());
        btnClearConsole.setOnClickListener(new ClearConsoleClickListener());

        setTitle(getResources().getString(R.string.transport_http_send_title));

        consoleAppender = new ConsoleMessagesAppender(areaConsole);
        httpHandler = new HttpHandler(this, consoleAppender);

        refreshState();
    }

    private void refreshState() {
        switch (currentState.getState()) {
            case STATE_IDLE:
                btnSendData.setEnabled(true);
                btnClearConsole.setEnabled(true);
                break;
            case STATE_HTTP_SENDING:
            default:
                btnSendData.setEnabled(false);
                btnClearConsole.setEnabled(false);
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (httpThread != null) {
            httpClient.terminate();
            httpThread.interrupt();
            httpThread = null;
        }
    }

    private class SendDataClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            currentState.setState(STATE_HTTP_SENDING);
            refreshState();
            httpClient = new TransportHttpClient(httpHandler);
            httpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    httpClient.sendRawData();
                }
            });
            httpThread.start();
        }
    }

    private class ClearConsoleClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            consoleAppender.clear();
        }
    }

    private static class HttpHandler extends Handler {
        private final TransportHttpSendActivity owner;
        private final ConsoleMessagesAppender consoleAppender;

        private HttpHandler(TransportHttpSendActivity owner, ConsoleMessagesAppender consoleAppender) {
            super();
            this.owner = owner;
            this.consoleAppender = consoleAppender;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS ||
                       msg.what == ThreadMessageTypes.MSG_FINISHED_ERROR) {
                owner.httpThread = null;
                owner.currentState.setState(STATE_IDLE);
                owner.refreshState();
            }
        }
    }
}
