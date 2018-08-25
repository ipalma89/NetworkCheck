package com.rappi.ivan.networkcheck

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
                .client(okHttpClient)
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

        binding.buttonRetrofitCall.setOnClickListener {
            val retro = Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                    .build()

            binding.progress.visibility = View.VISIBLE

            retro.create(RappiApi2::class.java).check().enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>?, response: retrofit2.Response<Any>) {
                    binding.textRetrofitCall.plus("Result OK: ${response.body()}")
                    binding.progress.visibility = View.GONE
                }

                override fun onFailure(call: Call<Any>?, t: Throwable) {
                    binding.textRetrofitCall.plus("Error: ${t.message}\n${t.cause}")
                    binding.progress.visibility = View.GONE
                }
            })
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
            val queue = Volley.newRequestQueue(this, VolleyOkHttp3StackInterceptors(okHttpClient))
            val stringRequest = StringRequest(Request.Method.GET, BASE_URL + PATH,
                    Response.Listener<String> { response ->
                        binding.textVolleyOkHttp.plus("Result OK: $response")
                        binding.progress.visibility = View.GONE
                    },
                    Response.ErrorListener {
                        binding.textVolleyOkHttp.plus("Error: ${it.message}")
                        binding.progress.visibility = View.GONE
                    })

            queue.add(stringRequest)
            binding.progress.visibility = View.VISIBLE
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

interface RappiApi2 {
    @GET(PATH)
    fun check(): Call<Any>
}

fun TextView.plus(newText: String) {
    val old = text.toString()
    text = "$old\n$newText"
}