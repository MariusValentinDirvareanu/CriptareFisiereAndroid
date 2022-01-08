package com.example.finger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var txtAuth: TextView;
    private lateinit var executor: Executor;
    private lateinit var biometricPrompt: BiometricPrompt;
    private lateinit var promptInfo: BiometricPrompt.PromptInfo;


    private var readyToEncrypt: Boolean = false
    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var secretKeyName: String
    private lateinit var cipherByteArray: ByteArray
    private lateinit var initializationVector: ByteArray
    private lateinit var decipherByteArray: ByteArray
    private var fileByteArray: ByteArray? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cryptographyManager = CryptographyManager()

        secretKeyName = "SafeFiles"

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
                val data1: Intent? = result.data
                val uri: Uri? = data1?.data
                fileByteArray = getBytesFromUri(applicationContext, uri)
            }
        }


    private var resultLauncherCreate =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data1: Intent? = result.data
                val uri: Uri? = data1?.data
                val outputStream = uri?.let { contentResolver.openOutputStream(it) }
                if (readyToEncrypt) {
                    outputStream?.write(initializationVector + cipherByteArray)
                } else {
                    outputStream?.write(decipherByteArray)
                }
                outputStream?.close()
                findViewById<Button>(R.id.btnAlegeFisier).visibility = View.VISIBLE
                findViewById<Button>(R.id.btnSalveaza).visibility = View.INVISIBLE
            }
        }

    fun openFileDialog(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*";
        val intent2: Intent = Intent.createChooser(intent, "Alege fisier")
        resultLauncher.launch(intent2)
    }


    fun createFileDialog(view: View) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "*/*";
        if (readyToEncrypt) {
            intent.putExtra(Intent.EXTRA_TITLE, "fisierCriptat.bin");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        resultLauncherCreate.launch(intent)
        txtAuth.text = "Buna ziua!"
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
                val eroare = "$errorCode :: $errString"
                txtAuth.text = eroare
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                txtAuth.text = "Autentificare esuata din motiv necunoscut"
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                txtAuth.text = "Autentificare cu succes"
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
        var iv = ByteArray(12)
        if (fileByteArray != null) {
            iv = fileByteArray?.copyOfRange(0, 12)!!
        }
        if (BiometricManager.from(applicationContext)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager
                .BIOMETRIC_SUCCESS
        ) {
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName,
                iv
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }


    private fun processData(cryptoObject: BiometricPrompt.CryptoObject?) {
        if (readyToEncrypt) {
            val encryptedData =
                fileByteArray?.let { cryptographyManager.encryptData(it, cryptoObject?.cipher!!) }
            if (encryptedData != null) {
                cipherByteArray = encryptedData.ciphertext
            }
            if (encryptedData != null) {
                initializationVector = encryptedData.initializationVector
            }
        } else {
            var cipherByteArrayFile = ByteArray((fileByteArray?.size ?: 12) - 12)
            if (fileByteArray != null) {
                cipherByteArrayFile = fileByteArray?.copyOfRange(12, fileByteArray!!.size)!!
            }
            decipherByteArray =
                cryptographyManager.decryptData(cipherByteArrayFile, cryptoObject?.cipher!!)
        }
        findViewById<Button>(R.id.btnAlegeFisier).visibility = View.INVISIBLE
        findViewById<Button>(R.id.btnSalveaza).visibility = View.VISIBLE
    }
}