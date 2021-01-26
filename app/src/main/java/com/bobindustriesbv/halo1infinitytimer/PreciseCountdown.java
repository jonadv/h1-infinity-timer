package com.bobindustriesbv.halo1infinitytimer;
import androidx.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("ALL")
public class PreciseCountdown extends Timer {
    //https://stackoverflow.com/questions/23323823/android-countdowntimer-tick-is-not-accurate
    private long totalTime;
    private long interval;
    private long delay;
    private TimerTask task;
    private long startTime = -1;
    private boolean restart = false;
    private boolean wasCancelled = false;
    private boolean wasStarted = false;

    public PreciseCountdown(long totalTime, long interval) {
        this(totalTime, interval, 0);
    }

    private PreciseCountdown(long totalTime, long interval, long delay) {
        super("PreciseCountdown", true);
        this.delay = delay;
        this.interval = interval;
        this.totalTime = totalTime;
        this.task = getTask(totalTime);
    }

    public void start() {
        wasStarted = true;
        this.scheduleAtFixedRate(task, delay, interval);
    }

    public void restart() {
        if(!wasStarted) {
            start();
        }
        else if(wasCancelled) {
            wasCancelled = false;
            this.task = getTask(totalTime);
            start();
        }
        else{
            this.restart = true;
        }
    }

    public void stop() {
        this.wasCancelled = true;
        this.task.cancel();
    }

    // Call this when there's no further use for this timer
    public void dispose(){
        cancel();
        purge();
    }

    @NonNull
    private TimerTask getTask(final long totalTime) {
        return new TimerTask() {

            @Override
            public void run() {
                long timeLeft;
                if (startTime < 0 || restart) {
                    startTime = scheduledExecutionTime();
                    timeLeft = totalTime;
                    restart = false;
                } else {
                    timeLeft = totalTime - (scheduledExecutionTime() - startTime);

                    if (timeLeft <= 0) {
                        this.cancel();
                        startTime = -1;
                        onFinished();
                        return;
                    }
                }

                onTick(timeLeft);
            }
        };
    }

    public void onTick(long timeLeft){}
    public void onFinished(){}
}