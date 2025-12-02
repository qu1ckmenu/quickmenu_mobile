import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.QuickMenu.mobile.R

import com.QuickMenu.mobile.databinding.ItemProdutoHomeBinding
import com.QuickMenu.mobile.main.home.ItemProdutoHome
import com.bumptech.glide.Glide

class ItemProdutoHomeAdapter(
    private var items: List<ItemProdutoHome>
) : RecyclerView.Adapter<ItemProdutoHomeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProdutoHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProdutoHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvNome.text = item.nome // Supondo que você tenha esse Textview
        holder.binding.tvPrice.text = item.preco

        // Carregamento da Imagem com Glide
        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.pao) // Imagem padrão enquanto carrega
                .error(R.drawable.pao)       // Imagem caso falhe
                .into(holder.binding.imgItem) // ID da ImageView no XML item_produto
        } else {
            holder.binding.imgItem.setImageResource(R.drawable.pao)
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<ItemProdutoHome>) {
        items = newItems
        notifyDataSetChanged()
    }
}