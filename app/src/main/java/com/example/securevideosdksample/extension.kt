package com.example.securevideosdksample

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: kotlin.String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
