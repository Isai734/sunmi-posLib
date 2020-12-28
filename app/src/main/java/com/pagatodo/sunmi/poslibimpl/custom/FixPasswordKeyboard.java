package com.pagatodo.sunmi.poslibimpl.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pagatodo.sunmi.poslibimpl.R;

public class FixPasswordKeyboard extends LinearLayout {

    public FixPasswordKeyboard(Context context) {
        this(context, null);
    }

    public FixPasswordKeyboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixPasswordKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private TextView key0;
    private TextView key1;
    private TextView key2;
    private TextView key3;
    private TextView key4;
    private TextView key5;
    private TextView key6;
    private TextView key7;
    private TextView key8;
    private TextView key9;

    private void initView(Context context) {
        inflate(context, R.layout.view_fix_password_keyboard, this);
        key0 = findViewById(R.id.text_0);
        key1 = findViewById(R.id.text_1);
        key2 = findViewById(R.id.text_2);
        key3 = findViewById(R.id.text_3);
        key4 = findViewById(R.id.text_4);
        key5 = findViewById(R.id.text_5);
        key6 = findViewById(R.id.text_6);
        key7 = findViewById(R.id.text_7);
        key8 = findViewById(R.id.text_8);
        key9 = findViewById(R.id.text_9);
    }

    public void setKeyBoard(String keys) {
        if (keys == null || keys.length() != 10) return;

        String temp = keys.substring(0, 1);
        key0.setText(temp);

        temp = keys.substring(1, 2);
        key1.setText(temp);

        temp = keys.substring(2, 3);
        key2.setText(temp);

        temp = keys.substring(3, 4);
        key3.setText(temp);

        temp = keys.substring(4, 5);
        key4.setText(temp);

        temp = keys.substring(5, 6);
        key5.setText(temp);

        temp = keys.substring(6, 7);
        key6.setText(temp);

        temp = keys.substring(7, 8);
        key7.setText(temp);

        temp = keys.substring(8, 9);
        key8.setText(temp);

        temp = keys.substring(9, 10);
        key9.setText(temp);
    }

    public TextView getKey0() {
        return key0;
    }

}
