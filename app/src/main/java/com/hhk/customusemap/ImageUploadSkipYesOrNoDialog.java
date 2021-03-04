package com.hhk.customusemap;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ImageUploadSkipYesOrNoDialog extends Dialog {

    private ButtonsClickListener buttonsClickListener;
    private String title;

    interface ButtonsClickListener {
        void onYesBtnClick();
        void onNoBtnClick();
    }

    ImageUploadSkipYesOrNoDialog(String title, Context context, ButtonsClickListener listener) {
        super(context);
        buttonsClickListener = listener;
        this.title = title;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_yn_dialog_layout);
        setCancelable(false);

        Button btn_simpledialog_yes, btn_simpledialog_no;
        btn_simpledialog_yes = findViewById(R.id.btn_simpledialog_yes);
        btn_simpledialog_no = findViewById(R.id.btn_simpledialog_no);

        TextView tv_simple_yn_dialog_title = findViewById(R.id.tv_simple_yn_dialog_title);
        tv_simple_yn_dialog_title.setText(title);

        btn_simpledialog_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonsClickListener.onYesBtnClick();
                dismiss();
            }
        });

        btn_simpledialog_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonsClickListener.onNoBtnClick();
                dismiss();
            }
        });
    }
}
