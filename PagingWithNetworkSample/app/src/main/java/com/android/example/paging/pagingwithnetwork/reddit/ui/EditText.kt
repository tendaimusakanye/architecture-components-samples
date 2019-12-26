package com.android.example.paging.pagingwithnetwork.reddit.ui

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText

fun EditText.setAsSearch(onChange : (String) -> Unit) {
    this.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_GO) {
            onChange(text.trim().toString())
            true
        } else {
            false
        }
    }
    this.setOnKeyListener { _, keyCode, event ->
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            onChange(text.trim().toString())
            true
        } else {
            false
        }
    }
}