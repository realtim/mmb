package ru.mmb.datacollector.activity.transport.http.receive;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.transport.http.ThreadMessageTypes;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.widget.ConsoleMessagesAppender;

import static ru.mmb.datacollector.activity.transport.http.receive.TransportHttpReceiveActivityState.STATE_HTTP_RECEIVING;
import static ru.mmb.datacollector.activity.transport.http.receive.TransportHttpReceiveActivityState.STATE_IDLE;

public class TransportHttpReceiveActivity extends Activity {
    private TransportHttpReceiveActivityState currentState;

    private Button btnReceiveData;
    private Button btnClearConsole;
    private TextView areaConsole;

    private ConsoleMessagesAppender consoleAppender;
    private Handler httpHandler;
    private TransportHttpReceiveClient httpClient;
    private Thread httpThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new TransportHttpReceiveActivityState();

        setContentView(R.layout.transport_http_receive);

        btnReceiveData = (Button) findViewById(R.id.transportHttpReceive_receiveDataButton);
        btnClearConsole = (Button) findViewById(R.id.transportHttpReceive_clearConsoleButton);
        areaConsole = (TextView) findViewById(R.id.transportHttpReceive_consoleTextView);

        btnReceiveData.setOnClickListener(new ReceiveDataClickListener());
        btnClearConsole.setOnClickListener(new ClearConsoleClickListener());

        setTitle(getResources().getString(R.string.transport_http_receive_title));

        consoleAppender = new ConsoleMessagesAppender(areaConsole);
        httpHandler = new HttpHandler(this, consoleAppender);

        refreshState();
    }

    private void refreshState() {
        switch (currentState.getState()) {
            case STATE_IDLE:
                btnReceiveData.setEnabled(true);
                btnClearConsole.setEnabled(true);
                break;
            case STATE_HTTP_RECEIVING:
            default:
                btnReceiveData.setEnabled(false);
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

    private class ReceiveDataClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            currentState.setState(STATE_HTTP_RECEIVING);
            refreshState();
            httpClient = new TransportHttpReceiveClient(httpHandler);
            httpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    httpClient.transportData();
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
        private final TransportHttpReceiveActivity owner;
        private final ConsoleMessagesAppender consoleAppender;

        private HttpHandler(TransportHttpReceiveActivity owner, ConsoleMessagesAppender consoleAppender) {
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
