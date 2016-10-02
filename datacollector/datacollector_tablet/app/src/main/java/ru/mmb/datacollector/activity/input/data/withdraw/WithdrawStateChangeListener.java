package ru.mmb.datacollector.activity.input.data.withdraw;

import ru.mmb.datacollector.activity.StateChangeListener;

public interface WithdrawStateChangeListener extends StateChangeListener {
    void onStateReload();
}
