package com.QuickMenu.mobile.main.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.QuickMenu.mobile.R // Import para usar os ícones de favorito
import com.QuickMenu.mobile.databinding.ItemRestauranteBinding
import com.bumptech.glide.Glide

class ItemRestauranteAdapter(
    private var restaurants: MutableList<ItemRestaurante>,
    private val onFavoriteClick: (ItemRestaurante) -> Unit,
    private val onItemClick: (ItemRestaurante) -> Unit
) : RecyclerView.Adapter<ItemRestauranteAdapter.ItemRestauranteViewHolder>() {

    class ItemRestauranteViewHolder(val binding: ItemRestauranteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemRestauranteViewHolder {
        val binding = ItemRestauranteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemRestauranteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemRestauranteViewHolder, position: Int) {
        val item = restaurants[position]
        val context = holder.binding.root.context

        holder.binding.tvRestaurantName.text = item.nome
        holder.binding.tvRestaurantType.text = item.descricao

        Glide.with(context)
            .load(item.imageUrl)
            .placeholder(R.drawable.sweetcake)
            .error(R.drawable.bolo)
            .into(holder.binding.imgRestaurant)

        val favoriteIcon = if (item.isFavorito) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_border
        }
        holder.binding.ivFavorite.setImageResource(favoriteIcon)

        holder.binding.ivFavorite.setOnClickListener {
            onFavoriteClick(item)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = restaurants.size

    fun updateList(newRestaurants: List<ItemRestaurante>, favoriteIds: Set<String>) {
        newRestaurants.forEach { restaurante ->
            restaurante.isFavorito = favoriteIds.contains(restaurante.id)
        }


        restaurants.clear()
        restaurants.addAll(newRestaurants)
        notifyDataSetChanged()
    }
}
