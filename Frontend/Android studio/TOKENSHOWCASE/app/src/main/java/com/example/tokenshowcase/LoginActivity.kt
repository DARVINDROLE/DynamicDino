package com.example.tokenshowcase

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleDeepLink(intent?.data)

        val loginButton: Button = findViewById(R.id.loginButton)
        loginButton.setOnClickListener {
            startLogin()
        }
    }

    private fun handleDeepLink(data: Uri?) {
        if (data != null && data.scheme == "emotionwellbeing" && data.host == "auth-success") {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrEmpty()) {
                Log.d("LoginActivity", "JWT Token received: $token")

                // ✅ Save token in SharedPreferences
                val sharedPref = getSharedPreferences("auth", MODE_PRIVATE)
                sharedPref.edit().putString("jwt_token", "Bearer $token").apply()

                Toast.makeText(this, "Login successful!", Toast.LENGTH_LONG).show()

                // ✅ Navigate to main app screen (actual MainActivity)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Token missing in redirect", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startLogin() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://emotion-730u.onrender.com/auth/authorize?platform=mobile&provider=google")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val authUrl = json.getString("auth_url")

                    withContext(Dispatchers.Main) {
                        val customTabsIntent = CustomTabsIntent.Builder().build()
                        customTabsIntent.launchUrl(this@LoginActivity, Uri.parse(authUrl))
                    }
                } else {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("Login", "Error ($responseCode): $error")
                    showToast("Server error: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("Login", "HTTP Error", e)
                showToast("Connection failed: ${e.message}")
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}
