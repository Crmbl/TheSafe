package com.crmbl.thesafe

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val userNameTxt = findViewById<EditText>(R.id.userNameTxt)
//        val passwordTxt = findViewById<EditText>(R.id.passwordTxt)
//        val resetBtn = findViewById<Button>(R.id.resetBtn)
//        val submitBtn = findViewById<Button>(R.id.submitBtn)
//
//        resetBtn.setOnClickListener {
//            userNameTxt.setText("")
//            passwordTxt.setText("")
//        }
//        submitBtn.setOnClickListener {
//            val user_name = userNameTxt.text;
//            val password = passwordTxt.text;
//            Toast.makeText(this@MainActivity, user_name, Toast.LENGTH_LONG).show()
//        }
    }
}
