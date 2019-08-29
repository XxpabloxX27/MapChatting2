package com.example.mapchatting2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.chat_window.view.*

class AdaptadorMensajes(val context:Context, val lista:ArrayList<Mensaje>): RecyclerView.Adapter<ViewHolderMensaje>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMensaje {
        return ViewHolderMensaje(
            LayoutInflater.from(context)
                .inflate(R.layout.chat_window,parent,false),context)
    }

    override fun getItemCount(): Int {
        return lista.size
    }

    override fun onBindViewHolder(holder: ViewHolderMensaje, position: Int) {
        holder.nombre.text = lista[position].nombre
        holder.mensaje.text = lista[position].mensaje
    }
}

class ViewHolderMensaje(view: View,var context: Context) : RecyclerView.ViewHolder(view),
    View.OnClickListener{
    override fun onClick(p0: View?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val nombre = view.txtNombre
    val mensaje = view.txtMensaje

}