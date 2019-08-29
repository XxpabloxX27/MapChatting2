package com.example.mapchatting2

import android.app.*
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import kotlin.collections.Map
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent



class Chat : AppCompatActivity() {

    private var mensajes = ArrayList<Mensaje>()
    private lateinit var btn:Button
    private lateinit var rv:RecyclerView
    private lateinit var et:EditText
    private lateinit var NAME: String
    private var REQUEST_CODE = 8
    private var background = false
    private val CANAL = "Mi Canal"
    var i : Int = 0
    val FILTRO = "FILTRO"
    val REPLY = "REPLY"
    private lateinit var notificationManager: NotificationManager
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        registerReceiver(MyReciever(), IntentFilter(FILTRO))
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        NAME = intent.getStringExtra("nombre")
        btn = findViewById(R.id.chat_btn_button)
        rv = findViewById(R.id.chat_rv_recycler)
        et = findViewById(R.id.chat_et_mensaje)
        setupFirebase()
        btn.setOnClickListener {
            sendMessage()
        }

    }

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    private fun createNotification(nombre:String, mensaje:String) : NotificationCompat.Builder{
        var i = Intent(this,Chat::class.java)
        var p = PendingIntent.getActivity(this,REQUEST_CODE,i,PendingIntent.FLAG_ONE_SHOT)
        var pUpdata = PendingIntent.getBroadcast(this,10,Intent(this,MyReciever::class.java),PendingIntent.FLAG_UPDATE_CURRENT)
        var remoteInput = RemoteInput.Builder(REPLY)
            .setLabel("Responde Aqui")
            .build()
        var action = NotificationCompat.Action.Builder(R.drawable.ic_mensaje,"Reply",pUpdata)
            .addRemoteInput(remoteInput).build()
        return NotificationCompat.Builder(this,CANAL)
            .setContentIntent(p)
            .setContentTitle(nombre)
            .setSmallIcon(R.drawable.ic_mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_mensaje,mensaje,p)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel =
            NotificationChannel(CANAL,CANAL,NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.description = "Mi APP"
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override fun onPause() {
        super.onPause()
        background = true
    }

    override fun onResume() {
        super.onResume()
        background = false
    }


    private fun sendMessage(){
        if(et.text.toString() != null && et.text.toString() != " "){
            var nombre = NAME
            var texto = et.text.toString()
            var mensaje = hashMapOf<String,String>(
                "nombre" to nombre,
                "mensaje" to texto
            )
            var db = FirebaseFirestore.getInstance()
            db.collection("chats")
                .add(mensaje as Map<String, Any>)
        }
        et.text.clear()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupFirebase(){
        var db: FirebaseFirestore = FirebaseFirestore.getInstance()
        var mensajeNot = ""
        var nombreNot = ""
        var ref = db.collection("chats")
        ref.addSnapshotListener {snapshot, e ->
            if(e != null){
                Log.v("FBERROR",e.message)
            }
            if(snapshot != null){
                for(dc in snapshot.documentChanges){
                    when(dc.type){
                        DocumentChange.Type.ADDED -> {
                            var d = dc.document.data
                            var nombre = d?.get("nombre").toString()
                            nombreNot = nombre
                            var texto = d?.get("mensaje").toString()
                            mensajeNot = texto
                            var mensaje = Mensaje(nombre,texto)
                            mensajes.add(mensaje)
                        }
                    }
                }
                var adapter = AdaptadorMensajes(this.baseContext,mensajes)
                var l = LinearLayoutManager(this.baseContext)
                rv.layoutManager = l
                rv.adapter = adapter
                if(background) {
                    createNotificationChannel()
                    val notifyBuilder = createNotification(nombreNot, mensajeNot)
                    notificationManager.notify(3, notifyBuilder.build())
                }
            }

        }


    }
}
