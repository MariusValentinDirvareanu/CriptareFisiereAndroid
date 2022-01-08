package com.example.finger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.Exception
import java.nio.charset.Charset
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var txtAuth: TextView;
    private lateinit var executor: Executor;
    private lateinit var biometricPrompt: BiometricPrompt;
    private lateinit var promptInfo: BiometricPrompt.PromptInfo;


    private var readyToEncrypt: Boolean = false
    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var secretKeyName: String
    private lateinit var ciphertext: ByteArray
    private lateinit var initializationVector: ByteArray


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cryptographyManager = CryptographyManager()

        secretKeyName = "cheieBoss"


        setContentView(R.layout.activity_main)

        txtAuth = findViewById(R.id.txtAuth);

        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = createBiometricPrompt();

        promptInfo = createPromptInfo();

        findViewById<Button>(R.id.btnEncrypt).setOnClickListener { authenticateToEncrypt() }
        findViewById<Button>(R.id.btnDecrypt).setOnClickListener { authenticateToDecrypt() }
    }


    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data1: Intent? = result.data
                val uri: Uri? = data1?.data
                val bytes = getBytesFromUri(applicationContext, uri);
                txtAuth.text = bytes?.get(3).toString()
            }
        }

    fun openFileDialog(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*";
        val intent2 = Intent.createChooser(intent, "Alege fisier")
        resultLauncher.launch(intent2)
    }


    private fun getBytesFromUri(context: Context, uri: Uri?): ByteArray? {
        try {
            val iStream = uri?.let { context.contentResolver.openInputStream(it) }
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var len = iStream?.read(buffer)
            while (len != -1) {
                if (len != null) {
                    byteBuffer.write(buffer, 0, len)
                }
                len = iStream?.read(buffer)
            }
            return byteBuffer.toByteArray()
        } catch (ex: Exception) {
        }
        return null
    }


    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                txtAuth.text = "$errorCode :: $errString"
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                txtAuth.text = "Authentication failed for an unknown reason"
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                txtAuth.text = "Authentication was successful"
                processData(result.cryptoObject)
            }
        }

        //The API requires the client/Activity context for displaying the prompt view
        return BiometricPrompt(this, executor, callback)
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autentificare")
            .setSubtitle("Scaneaza amprenta")
            .setNegativeButtonText("Cancel")
            .build()
    }


    private fun authenticateToEncrypt() {
        readyToEncrypt = true
        if (BiometricManager.from(applicationContext)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager
                .BIOMETRIC_SUCCESS
        ) {
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun authenticateToDecrypt() {
        readyToEncrypt = false
        if (BiometricManager.from(applicationContext)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager
                .BIOMETRIC_SUCCESS
        ) {
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName,
                initializationVector
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
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