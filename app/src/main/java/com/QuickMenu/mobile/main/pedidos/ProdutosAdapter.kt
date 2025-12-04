package com.QuickMenu.mobile.main.pedidos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.QuickMenu.mobile.R
import com.QuickMenu.mobile.databinding.ItemProdutoPedidoBinding
import com.bumptech.glide.Glide

class ProdutosAdapter(
    private val produtoPedidos: List<ProdutoPedido>
) : RecyclerView.Adapter<ProdutosAdapter.ProdutoViewHolder>() {

    class ProdutoViewHolder(val binding: ItemProdutoPedidoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val binding = ItemProdutoPedidoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProdutoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        val produto = produtoPedidos[position]

        with(holder.binding) {
            nomeProduto.text = produto.nome
            quantidadeProduto.text = "${produto.quantidade}x"

            if (produto.imageUrl.isNotEmpty()) {
                Glide.with(root.context)
                    .load(produto.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.bolo)
                    .error(R.drawable.bolo)
                    .into(imageProduto)
            } else {
                imageProduto.setImageResource(R.drawable.bolo)
            }
        }
    }

    override fun getItemCount(): Int = produtoPedidos.size
}