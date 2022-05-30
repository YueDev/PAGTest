package com.example.pagtest

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.example.pagtest.databinding.ItemPagBinding

class PagAdapter(
    private val uris: MutableList<Uri>,
    private val click:(position: Int) -> Unit,
    private val longClick:(position: Int) -> Unit
) : RecyclerView.Adapter<PagAdapter.PagHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPagBinding.inflate(inflater, parent, false)
        return PagHolder(binding).also {
            it.itemView.setOnClickListener { _ ->
                val position = it.adapterPosition
                if (position < 0 || position >= uris.size) return@setOnClickListener
                click(position)
            }
            it.itemView.setOnLongClickListener { _ ->
                val position = it.adapterPosition
                if (position < 0 || position >= uris.size) return@setOnLongClickListener false
                longClick(position)
                true
            }
        }
    }

    override fun onBindViewHolder(holder: PagHolder, position: Int) {
        uris[position].let { holder.bind(it, position) }
    }

    override fun getItemCount() = uris.size

    fun setUri(index:Int, uri:Uri) {
        if (index < 0 || index >= uris.size) return
        uris[index] = uri
        notifyItemChanged(index)
     }


    class PagHolder(private val binding: ItemPagBinding) : ViewHolder(binding.root) {

        fun bind(uri: Uri, position: Int) {
            binding.imageView.load(uri)
            binding.textView.text = position.toString()
        }

    }

}