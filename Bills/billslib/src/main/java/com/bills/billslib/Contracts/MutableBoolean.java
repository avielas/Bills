package com.bills.billslib.Contracts;

/**
 * Created by mvalersh on 12/4/2016.
 */

public class MutableBoolean {
    private boolean _state;

    public MutableBoolean(boolean state){
        _state = state;
    }

    public boolean Get(){
        return _state;
    }

    public void Set(boolean newState){
        _state = newState;
    }
}
