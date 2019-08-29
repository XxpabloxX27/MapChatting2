package com.example.mapchatting2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    lateinit var button: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.main_btn_ingresar)
        button.setOnClickListener {
            var nombre = findViewById<EditText>(R.id.main_et_nombre).text.toString()
            checkNombre(nombre)
        }
    }

    fun login(nombre: String){
        val intent = Intent(this,Map::class.java)
        intent.putExtra("nombre",nombre)
        startActivity(intent)
    }

    fun checkNombre(nombre: String){
        var db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                for(d in snapshot.documents){
                    if(d.data?.get("nombre").toString() == nombre){
                        Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_LONG).show()
                    }
                    else{
                        login(nombre)
                    }
                }

            }
    }
}
