package com.QuickMenu.mobile.main.carrinho

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.QuickMenu.mobile.R
import com.QuickMenu.mobile.databinding.ItemCarrinhoBinding
import com.bumptech.glide.Glide
import kotlin.inc
import kotlin.toString

class CarrinhoAdapter (private val itens: MutableList<ItemCarrinho>,
                       private val listener: CarrinhoActionsListener)
    : RecyclerView.Adapter<CarrinhoAdapter.CarrinhoViewHolder>() {
    inner class CarrinhoViewHolder(private val binding: ItemCarrinhoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemCarrinho) {
            binding.Nome.text = item.nome
            binding.quantidade.text = "${item.quantidade}"
            binding.Preco.text = "R$ ${item.preco}"


            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.bolo)
                    .error(R.drawable.bolo)
                    .centerCrop()
                    .into(binding.imagemProduto)
            } else {
                binding.imagemProduto.setImageResource(R.drawable.bolo)
            }


            binding.btnLixo.setOnClickListener {
                listener.onRemoverItem(adapterPosition)
            }

            binding.btnAddQtd.setOnClickListener {
                item.quantidade++

                binding.quantidade.text = item.quantidade.toString()

                listener.onUpdateItem(item)
            }

            binding.btnSubQtd.setOnClickListener {
                if (item.quantidade > 1) {
                    item.quantidade--
                    binding.quantidade.text = item.quantidade.toString()
                    listener.onUpdateItem(item)
                } else {
                    listener.onRemoverItem(adapterPosition)
                }
            }
        }
    }

    fun adicionarItem(item: ItemCarrinho) {
        itens.add(item)
        notifyItemInserted(itens.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarrinhoViewHolder {
        val binding = ItemCarrinhoBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return CarrinhoViewHolder(binding)
    }


    override fun onBindViewHolder(holder: CarrinhoViewHolder, position: Int) {
        val itemAtual = itens[position]
        holder.bind(itemAtual)

    }

    override fun getItemCount(): Int {
        return itens.size
    }

    fun removerItem(position: Int) {
        itens.removeAt(position)

        notifyItemRemoved(position)
    }


}