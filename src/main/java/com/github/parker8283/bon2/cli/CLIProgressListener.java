package com.github.parker8283.bon2.cli;

import com.github.parker8283.bon2.BON2;
import com.github.parker8283.bon2.data.IProgressListener;

public class CLIProgressListener implements IProgressListener {

    @Override
    public void start(int max, String label) {
        BON2.log(label);
    }

    @Override
    public void startWithoutProgress(String label) {
        BON2.log(label);
    }

    @Override
    public void setProgress(int value) {
        //NO-OP
    }

    @Override
    public void setMax(int max) {
        //NO-OP
    }
    
    @Override
    public void setLabel(String label) {
        BON2.log(label);
    }
}
