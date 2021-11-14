package com.example.finger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    lateinit var btnAuth:Button;
    lateinit var txtAuth:TextView;
    lateinit var executor: Executor;
    lateinit var biometricPrompt: androidx.biometric.BiometricPrompt;
    lateinit var promptInfo: androidx.biometric.BiometricPrompt.PromptInfo;



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnAuth=findViewById(R.id.BtnAuth);
        txtAuth=findViewById(R.id.txtAuth);

        executor=ContextCompat.getMainExecutor(this);

        biometricPrompt=androidx.biometric.BiometricPrompt(this@MainActivity,executor,object:androidx.biometric.BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                txtAuth.text = "Eroare"
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                txtAuth.text = "Autentificare cu succes"
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                txtAuth.text = "Autentificare nereusita"
            }

        })

        promptInfo=androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autentificare")
            .setSubtitle("Scaneaza amprenta")
            .setNegativeButtonText("Cancel")
            .build()

        btnAuth.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }



    }
}