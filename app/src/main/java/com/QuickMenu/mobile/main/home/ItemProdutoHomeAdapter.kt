import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.QuickMenu.mobile.R

import com.QuickMenu.mobile.databinding.ItemProdutoHomeBinding
import com.QuickMenu.mobile.main.home.ItemProdutoHome
import com.bumptech.glide.Glide

class ItemProdutoHomeAdapter(
    private var items: List<ItemProdutoHome>,

    private val onItemClick: (ItemProdutoHome) -> Unit
) : RecyclerView.Adapter<ItemProdutoHomeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProdutoHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProdutoHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.cardProdutoHome.setOnClickListener {
            onItemClick(item)
        }
        holder.binding.imgItem.setOnClickListener {
            onItemClick(item)
        }

        holder.binding.tvNome.text = item.nome
        holder.binding.tvPrice.text = String.format("R$ %.2f", item.precoUnitario)

        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.pao)
                .error(R.drawable.pao)
                .into(holder.binding.imgItem)
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
