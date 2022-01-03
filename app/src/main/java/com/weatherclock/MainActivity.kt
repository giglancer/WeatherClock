package com.weatherclock

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private var lat : Double = 0.0
    private var lon : Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpPermission()
    }

    override fun onResume() {
        super.onResume()
        getCurrentPosition()
    }
    private fun setUpPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                } else -> {
                // No location access granted.
                Toast.makeText(this, R.string.cancelledPermission, Toast.LENGTH_SHORT).show()
                finish()
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun getCurrentPosition() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).let { client ->
                client.lastLocation.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful && task.result != null) {
                        lat = task.result.latitude
                        lon = task.result.longitude
                        GlobalScope.launch {
                            getCurrentWeatherInfo()
                        }
                    } else {
                        val intent = Intent()
                        intent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        startActivity(intent)
                        Toast.makeText(this, R.string.failedGetPosition, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    private fun chooseWeather(weatherId: Int, currentDegree: Double, maxDegree: Double, minDegree: Double) {
        var currentWeather = findViewById<TextView>(R.id.currentWeather)
        var weatherMovieContents = 0

        when (weatherId) {
            in 200..230 -> {
                weatherMovieContents = R.raw.thunderstorm
                currentWeather.setText(R.string.thunderstorm)
            }
            in 300..321 -> {
                weatherMovieContents = R.raw.rain
                currentWeather.setText(R.string.rain)
            }
            in 600..622 ->  {
                weatherMovieContents = R.raw.snow
                currentWeather.setText(R.string.snow)
            }
            701 ->  {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.mist)
            }
            711 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.smoke)
            }
            721 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.haze)
            }
            731 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.dust)
            }
            741 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.fog)
            }
            751 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.sand)
            }
            761 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.dust)
            }
            762 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.volcanic_ash)
            }
            771 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.squalls)
            }
            781 -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.tornado)
            }
            800 -> {
                weatherMovieContents = R.raw.sunny
                currentWeather.setText(R.string.clear)
            }
            in 801..804 -> {
                weatherMovieContents = R.raw.cloud
                currentWeather.setText(R.string.clouds)
            }
            else -> {
                weatherMovieContents = R.raw.another
                currentWeather.setText(R.string.unknown)
            }
        }
        val weatherMovie = findViewById<VideoView>(R.id.videoView)
        weatherMovie.setVideoURI(Uri.parse("android.resource://" + this.packageName + "/" + weatherMovieContents))
        weatherMovie.setOnPreparedListener { it.isLooping = true }
        weatherMovie.start()

        // 気温
        val currentDegreeTxt = findViewById<TextView>(R.id.currentDegree)
        val maxDegreeTxt = findViewById<TextView>(R.id.maxDegree)
        val minDegreeTxt = findViewById<TextView>(R.id.minDegree)

        val roundCurrentDegree = ((currentDegree - 273.15).roundToInt()).toString()
        val roundMaxDegree = ((maxDegree - 273.15).roundToInt()).toString()
        val roundMinDegree = ((minDegree - 273.15).roundToInt()).toString()

        currentDegreeTxt.text = "$roundCurrentDegree ℃"
        maxDegreeTxt.text = "$roundMaxDegree ℃"
        minDegreeTxt.text = "$roundMinDegree ℃"
    }
    private suspend fun getCurrentWeatherInfo() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            withContext(Dispatchers.IO) {
                val service = retrofit.create(WeatherService::class.java)
                val weatherApiResponse = service.fetchWeather(
                    lat.toString(),
                    lon.toString(),
                    BuildConfig.OWM_API_KEY
                ).execute().body()?: throw IllegalStateException("body is null")
                withContext(Dispatchers.Main) {
                    chooseWeather(
                        weatherApiResponse.weather[0].id,
                        weatherApiResponse.main.temp,
                        weatherApiResponse.main.temp_max,
                        weatherApiResponse.main.temp_min)
                    Log.d("天気ID", weatherApiResponse.weather[0].id.toString())
                    Log.d("天気", weatherApiResponse.toString())
                }
            }
        }
    }