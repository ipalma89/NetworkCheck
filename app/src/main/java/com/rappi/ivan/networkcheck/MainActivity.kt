package com.rappi.ivan.networkcheck

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.GsonBuilder
import com.rappi.ivan.networkcheck.MainActivity.Companion.PATH
import com.rappi.ivan.networkcheck.databinding.ActivityMainBinding
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }

    private val retrofit: Retrofit by lazy {
        val clientBuilder = OkHttpClient.Builder()
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)

        Retrofit.Builder()
                .client(clientBuilder.build())
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
    }

    private val rappiApi: RappiApi by lazy { retrofit.create(RappiApi::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        binding.buttonRetrofit.setOnClickListener {
            rappiApi.check()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { binding.progress.visibility = View.VISIBLE }
                    .doFinally { binding.progress.visibility = View.GONE }
                    .subscribe({ binding.textRetrofit.plus("Result OK: $it") },
                            { binding.textRetrofit.plus("Error: ${it.message}\n${it.cause}") })
        }

        binding.buttonVolley.setOnClickListener {
            val queue = Volley.newRequestQueue(this)
            val stringRequest = StringRequest(Request.Method.GET, BASE_URL + PATH,
                    Response.Listener<String> { response ->
                        binding.textVolley.plus("Result OK: $response")
                        binding.progress.visibility = View.GONE
                    },
                    Response.ErrorListener {
                        binding.textVolley.plus("Error: ${it.message}")
                        binding.progress.visibility = View.GONE
                    })

            queue.add(stringRequest)
            binding.progress.visibility = View.VISIBLE
        }

        binding.buttonVolleyOkHttp.setOnClickListener {
            val queue = Volley.newRequestQueue(this, VolleyOkHttp3StackInterceptors())
            val stringRequest = StringRequest(Request.Method.GET, BASE_URL + PATH,
                    Response.Listener<String> { response ->
                        binding.textVolleyOkHtto.plus("Result OK: $response")
                        binding.progress.visibility = View.GONE
                    },
                    Response.ErrorListener {
                        binding.textVolleyOkHtto.plus("Error: ${it.message}")
                        binding.progress.visibility = View.GONE
                    })

            queue.add(stringRequest)
            binding.progress.visibility = View.VISIBLE
        }

        binding.buttonHttp.setOnClickListener {
            Thread {
                try {
                    val url = URL(BASE_URL + PATH).openConnection() as HttpURLConnection
                    if (url.responseCode == 200) {
                        binding.textHttp.plus("Result OK: ${url.inputStream.bufferedReader().readText()}")
                    } else {
                        binding.textHttp.plus("Error: ${url.responseCode}")
                    }
                    url.disconnect()
                } catch (exception: Exception) {
                    binding.textHttp.plus("Error: $exception")
                }
            }.start()
        }
    }

    companion object {
        private const val TIMEOUT = 40L
        private const val BASE_URL = "http://v2.grability.rappi.com/"
        const val PATH = "api/application-versions/android/check/83/storekeeper_restaurant"
    }
}

interface RappiApi {
    @GET(PATH)
    fun check(): Single<Any>
}

fun TextView.plus(newText: String) {
    val old = text.toString()
    text =  "$old\n$newText"
}