package com.QuickMenu.mobile.main.cardapio
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.QuickMenu.mobile.databinding.ItemCategoriaBinding




class CategoriaAdapter(
    private val categorias: List<Categoria>,
    private val onAddProdutoClick: (ProdutoCardapio) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    inner class CategoriaViewHolder(val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val binding = ItemCategoriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val categoria = categorias[position]
        holder.binding.txtTituloCategoria.text = categoria.nome

        val produtoAdapter = ProdutoAdapter(categoria.produtoCardapios, onAddProdutoClick)

        holder.binding.rvProdutos.apply {
            layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.VERTICAL, false)
            adapter = produtoAdapter
            setRecycledViewPool(RecyclerView.RecycledViewPool())
        }
    }

    override fun getItemCount() = categorias.size
}