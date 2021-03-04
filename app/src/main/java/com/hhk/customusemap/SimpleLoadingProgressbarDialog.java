package com.hhk.customusemap;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

public class SimpleLoadingProgressbarDialog extends Dialog {

    SimpleLoadingProgressbarDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progressbar_dialog);
        setCancelable(false);
    }
}
