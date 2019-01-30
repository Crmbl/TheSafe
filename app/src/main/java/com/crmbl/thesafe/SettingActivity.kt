package com.crmbl.thesafe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.crmbl.thesafe.databinding.ActivitySettingBinding
import com.google.android.material.button.MaterialButton

class SettingActivity : AppCompatActivity() {

    var prefs : Prefs? = null
    var binding : ActivitySettingBinding? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.title = resources.getString(R.string.setting_field_title)

        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this@SettingActivity, R.layout.activity_setting)
        if (prefs?.firstLogin!!) {
            binding?.viewModel = SettingViewModel()
        }
        else {
            binding?.viewModel = SettingViewModel(
                prefs?.rememberUsername!!,
                prefs?.useFingerprint!!,
                prefs?.username!!,
                prefs?.saltDecryptHash!!,
                prefs?.passwordDecryptHash!!,
                prefs?.firstLogin!!)
        }

        var saveButton = findViewById<MaterialButton>(R.id.setting_button_save)
        saveButton.setOnClickListener{this.save()}
        var cancelButton = findViewById<MaterialButton>(R.id.setting_button_cancel)
        cancelButton.setOnClickListener{this.cancel()}
    }

    private fun save() {
        // TODO check if every fields are set
        // update values in Prefs
    }

    private fun cancel() {
        // TODO Just cancel and get back on MainActivity
    }
}