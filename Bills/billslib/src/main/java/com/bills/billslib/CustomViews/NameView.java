package com.bills.billslib.CustomViews;

import android.content.Context;
import android.widget.TextView;

/**
 * Created by mvalersh on 10/7/2016.
 */

public class NameView extends TextView {
    private String _name;
    private Integer _tip;
    private Double _bill;

    public NameView(Context context, String name, Integer tip) {
        super(context);
        _name = name;
        _tip = tip;
        _bill = 0.0;
        UpdateText();
    }

    public boolean SetTip(Integer tip){
        if(tip < 0){
            return false;
        }
        _tip = tip;
        UpdateText();
        return true;
    }

    public boolean RemvoeFromBill(Double itemPrice){
        if(_bill < itemPrice) {
            return false;
        }

        _bill -= itemPrice;
        UpdateText();
        return true;
    }

    public void AddToBill(Double itemPrice){
        _bill += itemPrice;
        UpdateText();
    }

    private void UpdateText() {
        if(_tip == 0){
            this.setText(String.format("%1$s %2$.2f", _name, _bill));
        }
        else {
            this.setText(String.format("%1$s %2$.2f(%3$.3f)", _name, _bill, _bill + _tip * _bill / 100));
        }
    }

}
