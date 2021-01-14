package com.chplalex.shaman.mvp.presenter

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import com.chplalex.shaman.utils.*
import com.chplalex.shaman.mvp.model.db.Location
import com.chplalex.shaman.mvp.model.db.Request
import com.chplalex.shaman.R
import com.chplalex.shaman.mvp.model.api.LocationData
import com.chplalex.shaman.mvp.model.api.WeatherData
import com.chplalex.shaman.service.location.LocationService
import com.chplalex.shaman.mvp.view.IViewStart
import com.chplalex.shaman.service.api.APP_ID
import com.chplalex.shaman.service.api.OpenWeatherRetrofit
import com.chplalex.shaman.service.db.ShamanDao
import com.chplalex.shaman.ui.App.Companion.instance
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import moxy.MvpPresenter
import javax.inject.Inject
import javax.inject.Named

class PresenterStart(private val fragment: Fragment, private val arguments: Bundle?) : MvpPresenter<IViewStart>() {

//    private val retrofit = instance.appComponent.getRetrofit()
//    private val uiScheduler = instance.appComponent.getUiScheduler()
//    private val ioScheduler = instance.appComponent.getIoScheduler()
//    private val dao = instance.appComponent.getDao()


    @Inject
    lateinit var retrofit : OpenWeatherRetrofit
    @Inject
    lateinit var dao : ShamanDao
    @Inject
    @Named("UI")
    lateinit var uiScheduler : Scheduler
    @Inject
    @Named("IO")
    lateinit var ioScheduler : Scheduler

    init {
        instance.appComponent.inject(this)
    }

    private val context = fragment.requireContext()
    private val sp = context.getSharedPreferences(SP_NAME, MODE_PRIVATE)
    private val resources = context.resources

    private val disposable = CompositeDisposable()

    private var weatherData: WeatherData? = null
    private var favoriteState: Boolean = false

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        initRowsVisibility()
        initLocationData(arguments)
    }

    private fun initRowsVisibility() {
        viewState.setPressureVisibility(if (sp.getBoolean("pref_pressure", true)) VISIBLE else GONE)
        viewState.setWindVisibility(if (sp.getBoolean("pref_wind", true)) VISIBLE else GONE)
        viewState.setSunMovingVisibility(if (sp.getBoolean("pref_sun_moving", true)) VISIBLE else GONE)
        viewState.setHumidityVisibility(if (sp.getBoolean("pref_humidity", true)) VISIBLE else GONE)
    }

    @SuppressLint("CheckResult")
    fun initLocationData(arguments: Bundle?) {
        var locationData = LocationService.getFromBundle(arguments)
        if (locationData == null) locationData = LocationService.getFromSharedPreferences(sp)
        if (locationData == null) {
            disposable.add(
                LocationService.getFromCurrentLocation(context)
                    .observeOn(uiScheduler)
                    .subscribeOn(ioScheduler)
                    .subscribe({
                        initWeatherData(it)
                    }, {
                        setNoWeatherData()
                        viewState.showErrorLocation(it)

                    })
            )
        } else {
            initWeatherData(locationData)
        }
    }

    private fun initWeatherData(locationData: LocationData) {
        val lang = sp.getString("pref_lang", "RU")
        val units = sp.getString("pref_units", "metric")

        disposable.add(
            retrofit.loadWeather(locationData.fullName(), APP_ID, lang, units)
                .observeOn(uiScheduler)
                .subscribeOn(ioScheduler)
                .subscribe({
                    setWeatherData(it)
                    dbDataExchange(it)
                }, {
                    viewState.showErrorRetrofit(it)
                })
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }

    private fun dbDataExchange(wd: WeatherData) {
        disposable.add(
            dao.getLocations(wd.name, wd.country)
                .subscribeOn(ioScheduler)
                .observeOn(uiScheduler)
                .subscribe({
                    if (it.isEmpty()) {
                        val location = Location(wd, favoriteState)
                        disposable.add(
                            dao.insertLocation(location)
                                .subscribeOn(ioScheduler)
                                .observeOn(uiScheduler)
                                .subscribe({ }, {
                                    viewState.showErrorDB(it)
                                })
                        )
                    } else {
                        viewState.setFavoriteState(it[0].favorite)
                    }
                }, {
                    viewState.showErrorDB(it)
                })
        )

        val request = Request(wd.id.toLong(), wd.main.temp)
        disposable.add(
            dao.insertRequest(request)
                .subscribeOn(ioScheduler)
                .observeOn(uiScheduler)
                .subscribe({ }, {
                    viewState.showErrorDB(it)
                })
        )
    }

    private fun setNoWeatherData() {
        this.weatherData = null

        viewState.setLocationName(resources.getString(R.string.not_found_location_name))
        viewState.setLocationCountry("--")
        viewState.setUncertainTemp()
        viewState.setWeatherDescription("--")
        viewState.setPressure("--")
        viewState.setWind("--")
        viewState.setSunMoving("--")
        viewState.setHumidity("--")

        sp.edit().apply {
            remove(LOCATION_ARG_NAME)
            remove(LOCATION_ARG_COUNTRY)
            remove(LOCATION_ARG_LONGITUDE)
            remove(LOCATION_ARG_LATITUDE)
            apply()
        }
    }

    private fun setWeatherData(weatherData: WeatherData) {
        this.weatherData = weatherData

        viewState.setLocationName(weatherData.name)
        viewState.setLocationCountry(weatherData.country)
        viewState.setTemp(weatherData.temp)
        viewState.setWeatherDescription(weatherData.description)
        viewState.setPressure(weatherData.pressure(resources))
        viewState.setWind(weatherData.windString(resources))
        viewState.setSunMoving(weatherData.sunMoving(resources))
        viewState.setHumidity(weatherData.humidity)

        sp.edit().apply {
            putString(LOCATION_ARG_NAME, weatherData.name)
            putString(LOCATION_ARG_COUNTRY, weatherData.country)
            putFloat(LOCATION_ARG_LONGITUDE, weatherData.coord.lon)
            putFloat(LOCATION_ARG_LATITUDE, weatherData.coord.lat)
            apply()
        }
    }

    fun actionLocationByQuerySelected(query: String) = LocationData(query.trim { it <= ' ' }, "", 0.0f, 0.0f).also {
        if (!it.isEmpty) {
            initWeatherData(it)
        }
    }

    fun actionMyLocationSelected() {
        disposable.add(
            LocationService.getFromCurrentLocation(context)
                .observeOn(uiScheduler)
                .subscribeOn(ioScheduler)
                .subscribe({
                    initWeatherData(it)
                }, {
                    viewState.showErrorLocation(it)
                })
        )
    }

    fun actionFavoriteSelected() = weatherData?.let {
        favoriteState = !favoriteState
        disposable.add(
            dao.updateLocationFavorite(it.name, it.country, favoriteState)
                .observeOn(uiScheduler)
                .subscribeOn(ioScheduler)
                .subscribe({
                    viewState.setFavoriteState(favoriteState)
                }, {
                    favoriteState = !favoriteState
                    viewState.showErrorDB(it)
                })
        )
    }
}