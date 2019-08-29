package com.example.mapchatting2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.Map
import kotlin.concurrent.thread
import kotlin.random.Random

class Map : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mlastLocation: Location
    private val INTERVAL: Long = 2000
    private val FASTEST_INTERVAL: Long = 1000
    internal lateinit var mLocationRequest: LocationRequest
    private val REQUEST_PERMISSION_LOCATION = 10
    private lateinit var NAME: String
    private lateinit var COLOR: String
    private var marcadores = arrayListOf<Marker>()
    private lateinit var btn:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        NAME = intent.getStringExtra("nombre")
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mLocationRequest = LocationRequest()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(!locationManager.isProviderEnabled((LocationManager.GPS_PROVIDER))){
            buildAlertMessageNoGps()
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        btn = findViewById(R.id.map_btn_chat)
        btn.setOnClickListener {
            startActivity(Intent(this,Chat::class.java).putExtra("nombre",intent.getStringExtra("nombre")))
        }
    }

    override fun onDestroy() {
        stoplocationUpdates()
        deleteFirebaseUser()
        super.onDestroy()

    }

    private fun deleteFirebaseUser() {
        var db = FirebaseFirestore.getInstance()
        Log.v("DELETED", "USUARIO BORRADO ")
        db.collection("users").document(NAME).delete()
    }

    private fun buildAlertMessageNoGps() {

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Su ubicacion por GPS esta deshabilitada, desea habilitarla?")
            .setCancelable(false)
            .setPositiveButton("Si") { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    , 11)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                finish()
            }
        val alert: AlertDialog = builder.create()
        alert.show()


    }

    private fun startLocationUpdates(){
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest!!.interval = INTERVAL
        mLocationRequest!!.fastestInterval = FASTEST_INTERVAL

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return
        }
        fusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest,mLocationCallback,
            Looper.myLooper())

    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
            Log.v("UBICACION","CAMBIE DE UBICACION")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == REQUEST_PERMISSION_LOCATION){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startLocationUpdates()
            }
            else{
                Toast.makeText(this@Map,"Permiso denegado", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun checkPermissionForLocation(context:Context): Boolean{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                true
            }
            else{
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_PERMISSION_LOCATION)
                false
            }
        }
        else{
            true
        }
    }

    private fun stoplocationUpdates() {
        fusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
    }

    override fun onStop() {
        super.onStop()
        stoplocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    fun onLocationChanged(location: Location) {
        mlastLocation = location
        var ubicacion : LatLng
        var lat = location.latitude
        var lng = location.longitude
        ubicacion = LatLng(lat,lng)
        setMarker(ubicacion,NAME,COLOR)
        updateLocationFirebase(ubicacion)
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        setUpMap()
        if(checkPermissionForLocation(this)){
            startLocationUpdates()
        }
    }

    fun updateLocationFirebase(ubicacion: LatLng){
        var db = FirebaseFirestore.getInstance()
        var now : Timestamp = Timestamp.now()
        var user = hashMapOf<String,Any>(
            "lat" to ubicacion.latitude.toString(),
            "lng" to ubicacion.longitude.toString(),
            "nombre" to NAME,
            "color" to COLOR,
            "lastSeen" to now

        )
        db.collection("users").document(NAME)
            .set(user as Map<String, Any>)
            .addOnSuccessListener { Log.v("FIREBASEMAPS", "Ubicacion del usuario actualizada con exito en firebase") }
            .addOnFailureListener { e -> Log.v("FIREBASEMAPS",e.message) }
    }

    fun setupFirebase(lat: String, lng: String){
        var db: FirebaseFirestore = FirebaseFirestore.getInstance()
        var miNombre = intent.getStringExtra("nombre")
        NAME = miNombre
        var now = Timestamp.now()
        val user = hashMapOf<String,Any>(
            "lat" to lat,
            "lng" to lng,
            "nombre" to miNombre,
            "color" to COLOR,
            "lastSeen" to now

        )


        db.collection("users").document(miNombre)
            .set(user as Map<String, Any>)
            .addOnSuccessListener { Log.v("SUCCESS","Usuarios conseguidos con exito") }
            .addOnFailureListener { e -> Log.v("FALIURE",e.message) }

        val ref = db.collection("users")
        ref.addSnapshotListener{
            snapshot,e ->
            if(e != null){
                Log.v("ERROR",e.message)
            }
            if(snapshot != null){
                for(dc in snapshot.documentChanges){
                    when(dc.type){
                        DocumentChange.Type.ADDED -> {
                            var d = dc.document.data
                            var nombre = d.get("nombre").toString()
                            var lat = d.get("lat").toString().toDouble()
                            var lng = d.get("lng").toString().toDouble()
                            var color = d.get("color").toString()
                            setMarker(LatLng(lat,lng),nombre,color)
                        }
                        DocumentChange.Type.REMOVED ->{
                            var d = dc.document.data
                            var nombre = d.get("nombre").toString()
                            for(m in marcadores){
                                if(m.title == nombre){
                                    m.remove()
                                    break
                                }
                            }

                        }
                        DocumentChange.Type.MODIFIED ->{
                            var d = dc.document.data
                            var nombre = d.get("nombre").toString()
                            var lat = d.get("lat").toString().toDouble()
                            var lng = d.get("lng").toString().toDouble()
                            for(m in marcadores){
                                if(m.title == nombre){
                                    m.position = LatLng(lat,lng)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


    private fun setUpMap(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true
        fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) {location ->
            if(location != null){
                mlastLocation = location
                val currentLatLong = LatLng(location.latitude,location.longitude)
                setMarker(currentLatLong)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 13f))
            }
            setupFirebase(location.latitude.toString(),location.longitude.toString())
        }
    }

    private fun setMarker(location: LatLng){
        var markerOptions = MarkerOptions().position(location)
        var color = randomColor()
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color))
        markerOptions.title(NAME)
        mMap.addMarker(markerOptions)
        var m = mMap.addMarker(markerOptions)
        marcadores.add(m)
    }


    private fun setMarker(location: LatLng, nombre:String, color: String){
        if(nombre == NAME){
            for(m in marcadores){
                if(m.title == nombre){
                    m.position = location
                    break
                }
            }
        }
        else {
            var markerOptions = MarkerOptions().position(location)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(toMarkerColor(color)))
            markerOptions.title(nombre)
            mMap.addMarker(markerOptions)
            var m = mMap.addMarker(markerOptions)
            marcadores.add(m)
        }
    }

    private fun randomColor() : Float{
        var option = Random.nextInt(1,10)
        var bdf = BitmapDescriptorFactory.HUE_AZURE
        when(option){
            1 -> {
                bdf = BitmapDescriptorFactory.HUE_AZURE
                COLOR = "AZURE"
            }
            2 -> {
                bdf = BitmapDescriptorFactory.HUE_BLUE
                COLOR = "BLUE"
            }
            3 -> {
                bdf = BitmapDescriptorFactory.HUE_CYAN
                COLOR = "CYAN"
            }
            4 -> {
                bdf = BitmapDescriptorFactory.HUE_GREEN
                COLOR = "GREEN"
            }
            5 -> {
                bdf = BitmapDescriptorFactory.HUE_MAGENTA
                COLOR = "MAGENTA"
            }
            6 -> {
                bdf = BitmapDescriptorFactory.HUE_ORANGE
                COLOR = "ORANGE"
            }
            7 -> {
                bdf = BitmapDescriptorFactory.HUE_RED
                COLOR = "RED"
            }
            8 -> {
                bdf = BitmapDescriptorFactory.HUE_ROSE
                COLOR = "ROSE"
            }
            9 -> {
                bdf = BitmapDescriptorFactory.HUE_VIOLET
                COLOR = "VIOLET"
            }
            10 -> {
                bdf = BitmapDescriptorFactory.HUE_YELLOW
                COLOR = "YELLOW"
            }
        }
        return bdf
    }

    private fun toMarkerColor(color:String): Float{
        var bdf = BitmapDescriptorFactory.HUE_AZURE
        when(color){
            "AZURE" -> bdf = BitmapDescriptorFactory.HUE_AZURE
            "BLUE" -> bdf = BitmapDescriptorFactory.HUE_BLUE
            "CYAN" -> bdf = BitmapDescriptorFactory.HUE_CYAN
            "GREEN" -> bdf = BitmapDescriptorFactory.HUE_GREEN
            "MAGENTA" -> bdf = BitmapDescriptorFactory.HUE_MAGENTA
            "ORANGE" -> bdf = BitmapDescriptorFactory.HUE_ORANGE
            "RED" -> bdf = BitmapDescriptorFactory.HUE_RED
            "ROSE" -> bdf = BitmapDescriptorFactory.HUE_ROSE
            "VIOLET" -> bdf = BitmapDescriptorFactory.HUE_VIOLET
            "YELLOW" -> bdf = BitmapDescriptorFactory.HUE_YELLOW
        }
        return bdf
    }
}
