package com.QuickMenu.mobile.main.pedidos

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.QuickMenu.mobile.R
import com.QuickMenu.mobile.databinding.ItemPedidoBinding
import com.bumptech.glide.Glide // Importe o Glide aqui

class PedidosAdapter(
    private val pedidos: List<Pedido>
) : RecyclerView.Adapter<PedidosAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(val binding: ItemPedidoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidos[position]

        with(holder.binding) {

            // 1. Define o NOME do restaurante ao invés do ID
            restaurante.text = pedido.nomeRestaurante

            // 2. Define a IMAGEM do restaurante usando Glide
            if (pedido.fotoRestaurante.isNotEmpty()) {
                Glide.with(root.context)
                    .load(pedido.fotoRestaurante)
                    .centerCrop() // Ou .fitCenter(), dependendo do seu gosto
                    .placeholder(R.drawable.restaurante_default) // Imagem enquanto carrega
                    .into(imageButton2)
            } else {
                imageButton2.setImageResource(R.drawable.restaurante_default)
            }

            // --- LÓGICA DE CORES E STATUS ---
            val corFundoId = if (pedido.status == Status.Ativo) {
                R.color.pedido_ativo_background
            } else {
                R.color.pedido_encerrado_background
            }

            val corFundo = ContextCompat.getColor(root.context, corFundoId)
            cardPedido.setCardBackgroundColor(corFundo)

            // Define a cor de fundo do botão (borda/fundo redondo)
            imageButton2.backgroundTintList = ColorStateList.valueOf(corFundo)

            // --- LÓGICA DE TEXTO DO HORÁRIO ---
            if (pedido.status == Status.Ativo) {
                textHorario.text = "Horário de compra :"
                textTime.text = pedido.horarioCompraFormatado
            } else {
                textHorario.text = "Horário de retirada :"
                textTime.text = pedido.horarioRetiradaFormatado ?: pedido.horarioCompraFormatado
            }

            // --- CONFIGURAÇÃO DA LISTA DE PRODUTOS ---
            if (recyclerProdutos.layoutManager == null) {
                recyclerProdutos.layoutManager = LinearLayoutManager(root.context)
                recyclerProdutos.setHasFixedSize(true)
                recyclerProdutos.isNestedScrollingEnabled = false
            }
            recyclerProdutos.adapter = ProdutosAdapter(pedido.produtoPedidos)
        }
    }

    override fun getItemCount(): Int = pedidos.size
}