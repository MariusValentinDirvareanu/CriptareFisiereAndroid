package com.example.finger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.nio.charset.Charset
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var btnAuth:Button;
    private lateinit var txtAuth:TextView;
    private lateinit var executor: Executor;
    private lateinit var biometricPrompt: androidx.biometric.BiometricPrompt;
    private lateinit var promptInfo: androidx.biometric.BiometricPrompt.PromptInfo;


    private var readyToEncrypt: Boolean = false
    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var secretKeyName: String
    private lateinit var ciphertext:ByteArray
    private lateinit var initializationVector: ByteArray



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cryptographyManager = CryptographyManager()
        // e.g. secretKeyName = "biometric_sample_encryption_key"
        secretKeyName = "cheieBoss"


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
                processData(result.cryptoObject)
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


        findViewById<Button>(R.id.btnEncrypt).setOnClickListener { authenticateToEncrypt() }
        findViewById<Button>(R.id.btnDecrypt).setOnClickListener { authenticateToDecrypt() }
    }


    private fun authenticateToEncrypt() {
        readyToEncrypt = true
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun authenticateToDecrypt() {
        readyToEncrypt = false
            val cipher = cryptographyManager.getInitializedCipherForDecryption(secretKeyName,initializationVector)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }


    private fun processData(cryptoObject: BiometricPrompt.CryptoObject?) {
        val data = if (readyToEncrypt) {
            val text = "Test";
            val encryptedData = cryptographyManager.encryptData(text, cryptoObject?.cipher!!)
            ciphertext = encryptedData.ciphertext
            initializationVector = encryptedData.initializationVector

            String(ciphertext, Charset.forName("UTF-8"))
        } else {
            cryptographyManager.decryptData(ciphertext, cryptoObject?.cipher!!)
        }
        txtAuth.text = data;
    }
}