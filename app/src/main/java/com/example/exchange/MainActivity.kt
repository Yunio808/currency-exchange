package com.example.exchange

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class ExchangeRateResponse(
    val rates: Map<String, Double>?,
    val base: String?,
    val date: String?,
    val result: String?,
    val documentation: String?,
    val termsOfUse: String?,
    val timeLastUpdateUnix: Long?,
    val timeLastUpdateUtc: String?,
    val timeNextUpdateUnix: Long?,
    val timeNextUpdateUtc: String?
)


class MainActivity : AppCompatActivity() {

    private lateinit var editTextAmount: EditText
    private lateinit var spinnerFromCurrency: Spinner
    private lateinit var spinnerToCurrency: Spinner
    private lateinit var buttonConvert: Button
    private lateinit var textViewResult: TextView

    private val exchangeApiService: ExchangeApiService by lazy {
        createExchangeApiService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.exchange2)

        editTextAmount = findViewById(R.id.editTextAmount)
        spinnerFromCurrency = findViewById(R.id.spinnerFromCurrency)
        spinnerToCurrency = findViewById(R.id.spinnerToCurrency)
        buttonConvert = findViewById(R.id.buttonConvert)
        textViewResult = findViewById(R.id.textViewResult)

        // Set up the Spinners with the currency adapter
        val currencyAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.currency_array,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerFromCurrency.adapter = currencyAdapter
        spinnerToCurrency.adapter = currencyAdapter

        buttonConvert.setOnClickListener { convertCurrency() }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun convertCurrency() {
        val amount = editTextAmount.text.toString().toDoubleOrNull()
        if (amount == null) {
            textViewResult.text = getString(R.string.invalid_amount)
            return
        }

        val fromCurrency = spinnerFromCurrency.selectedItem.toString()
        val toCurrency = spinnerToCurrency.selectedItem.toString()

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val fromCurrencyCode = getCurrencyCode(fromCurrency)
                val toCurrencyCode = getCurrencyCode(toCurrency)

                Log.d("ExchangeApp", "Making API call for $fromCurrency to get latest rates.")
                val fromResponse = exchangeApiService.getLatestRates(baseCurrency = fromCurrencyCode)
                Log.d("ExchangeApp", "Response for $fromCurrency: ${fromResponse.body()}")
                Log.d("ExchangeApp", "Result for $fromCurrency: ${fromResponse.body()?.result}")

                if (!fromResponse.isSuccessful) {
                    handleApiError(fromResponse)
                    return@launch
                }

                Log.d("ExchangeApp", "Making API call for $toCurrency to get latest rates.")
                val toResponse = exchangeApiService.getLatestRates(baseCurrency = toCurrencyCode)
                Log.d("ExchangeApp", "Response for $toCurrency: ${toResponse.body()}")
                Log.d("ExchangeApp", "Result for $toCurrency: ${toResponse.body()?.result}")

                if (!toResponse.isSuccessful) {
                    handleApiError(toResponse)
                    return@launch
                }

                val fromRates = fromResponse.body()?.rates
                val toRates = toResponse.body()?.rates

                Log.d("ExchangeApp", "fromRates: $fromRates")
                Log.d("ExchangeApp", "toRates: $toRates")

                if (fromRates != null && toRates != null && fromResponse.body()?.result == "success" && toResponse.body()?.result == "success") {
                    val fromRate = fromRates[fromCurrencyCode] ?: 1.0
                    val toRate = toRates[toCurrencyCode] ?: 1.0

                    Log.d("ExchangeApp", "fromRate: $fromRate")
                    Log.d("ExchangeApp", "toRate: $toRate")

                    val convertedAmount = amount * (toRate / fromRate)

                    val resultText = getString(R.string.result_format, amount, fromCurrency, convertedAmount, toCurrency)
                    textViewResult.text = resultText
                } else {
                    textViewResult.text = getString(R.string.error_fetching_rates_generic)
                }
            } catch (e: Exception) {
                Log.e("ExchangeApp", "Error during currency conversion: ${e.message}", e)
                textViewResult.text = getString(R.string.error_occurred) + " " + e.message
            } finally {
                // TODO: Hide loading indicator or enable UI
            }
        }
    }




    private fun handleApiError(response: Response<ExchangeRateResponse>) {
        val errorMessage = getString(R.string.error_fetching_rates, response.message(), response.errorBody()?.string())
        textViewResult.text = errorMessage
        Log.e("ExchangeApp", "API Error: $errorMessage")
    }

    private fun getCurrencyCode(currencyName: String): String {
        return when (currencyName) {
            "Euro (EUR)" -> "EUR"
            "Pound (GBP)" -> "GBP"
            "Yen (JPY)" -> "JPY"
            "US Dollar (USD)" -> "USD"
            "Ruble (RUB)" -> "RUB"
            "Yuan (CNY)" -> "CNY"
            else -> currencyName
        }
    }

    private fun createExchangeApiService(): ExchangeApiService {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://v6.exchangerate-api.com/v6/982bb107887200c0f503d970/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(ExchangeApiService::class.java)
    }
}
