package com.speakerband.utils;

import android.os.CountDownTimer;


public class MyCountDownTimer extends CountDownTimer {

    public MyCountDownTimer(long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
    }

    @Override
    public void onTick(long millisUntilFinished) {

        int progress = (int) (millisUntilFinished/1000);

    }

    @Override
    public void onFinish() {
        //finish();
    }
}
