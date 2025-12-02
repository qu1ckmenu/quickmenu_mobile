package com.QuickMenu.mobile.main.home

import ItemProdutoHomeAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.QuickMenu.mobile.R
import com.QuickMenu.mobile.databinding.FragmentHomeBinding
import com.QuickMenu.mobile.main.pedidos.ProdutoPedido
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recentAdapter: ItemProdutoHomeAdapter
    private lateinit var restaurantAdapter: ItemRestauranteAdapter
    private lateinit var db: FirebaseFirestore
    private val auth = Firebase.auth

    private var allRestaurants = mutableListOf<ItemRestaurante>()
    private var lastSearchedRestaurants = mutableListOf<ItemRestaurante>()
    private var favoriteRestaurants = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore

        setupRecyclerViews() // Configura os adapters vazios

        loadFavorites()
        loadRestaurantsFromFirestore()
        fetchRecentOrderedProducts() // <--- NOVA FUNÇÃO AQUI

        setupSearchBar()
        setupFilterButtons()
        voltar()
    }

    // --- SETUP INICIAL ---
    private fun setupRecyclerViews() {
        // Setup Recentes (Horizontal)
        recentAdapter = ItemProdutoHomeAdapter(emptyList())
        binding.recyclerItemProduto.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }

        // Setup Restaurantes (Vertical)
        restaurantAdapter = ItemRestauranteAdapter(mutableListOf(),
            onFavoriteClick = { toggleFavorite(it) },
            onItemClick = { restaurante ->
                updateLastSearched(restaurante)
                val bundle = Bundle().apply {
                    putString("restauranteId", restaurante.id)
                    putString("donoId", restaurante.userId)
                }
                try {
                    findNavController().navigate(R.id.cardapioFragment, bundle)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Erro ao navegar: ${e.message}")
                }
            }
        )
        binding.recyclerRestaurantList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = restaurantAdapter
        }
    }

    // --- NOVA LÓGICA: BUSCAR PRODUTOS RECENTES ---
    private fun fetchRecentOrderedProducts() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                // 1. Busca os últimos 5 pedidos (para garantir que teremos pelo menos 3 itens)
                val pedidosSnapshot = db.collection("Usuario")
                    .document(userId)
                    .collection("Pedidos")
                    .orderBy("dataPedido", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()

                val todosProdutosRecentes = mutableListOf<ItemProdutoHome>()
                val nomesAdicionados = mutableSetOf<String>() // Para evitar duplicatas visuais (ex: 3 cocas seguidas)

                // 2. Itera sobre os pedidos para buscar a subcoleção "Itens"
                for (doc in pedidosSnapshot.documents) {
                    // Se já temos 3 produtos únicos, paramos de buscar para economizar leitura
                    if (todosProdutosRecentes.size >= 3) break

                    val itensSnapshot = doc.reference.collection("Itens").get().await()

                    for (itemDoc in itensSnapshot.documents) {
                        val produtoBD = itemDoc.toObject(ProdutoPedido::class.java)

                        if (produtoBD != null && !nomesAdicionados.contains(produtoBD.nome)) {

                            // Formata o preço
                            val precoFormatado = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                                .format(produtoBD.preco)

                            // Cria o objeto de UI
                            val itemHome = ItemProdutoHome(
                                nome = produtoBD.nome,
                                preco = precoFormatado,
                                imageUrl = produtoBD.imageUrl
                            )

                            todosProdutosRecentes.add(itemHome)
                            nomesAdicionados.add(produtoBD.nome)

                            if (todosProdutosRecentes.size >= 3) break
                        }
                    }
                }

                // 3. Atualiza o Adapter na Thread principal
                if (todosProdutosRecentes.isNotEmpty()) {
                    recentAdapter.updateList(todosProdutosRecentes)
                    // binding.textRecentes.visibility = View.VISIBLE
                } else {
                    // Opcional: Esconder o título "Recentes" se não houver nada
                    // binding.textRecentes.visibility = View.GONE
                    Log.d("HomeFragment", "Nenhum produto recente encontrado.")
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Erro ao buscar produtos recentes", e)
            }
        }
    }

    // ... (Mantenha suas outras funções: loadRestaurants, searchBar, favorites, etc.) ...

    private fun loadRestaurantsFromFirestore() {
        // ... (seu código existente)
        db.collectionGroup("restaurantes").get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) return@addOnSuccessListener
                allRestaurants.clear()
                for (document in result) {
                    val restaurante = document.toObject(ItemRestaurante::class.java)
                    restaurante.id = document.id
                    val operadorId = document.reference.parent.parent?.id ?: ""
                    restaurante.userId = operadorId
                    allRestaurants.add(restaurante)
                }
                filterAndDisplayRestaurants("")
            }
    }

    // ... (Resto do código igual ao que você enviou)

    private fun loadFavorites() {
        val prefs = activity?.getSharedPreferences("RestaurantPreferences", Context.MODE_PRIVATE) ?: return
        favoriteRestaurants = prefs.getStringSet("favorite_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveFavorites() {
        val prefs = activity?.getSharedPreferences("RestaurantPreferences", Context.MODE_PRIVATE) ?: return
        with(prefs.edit()) {
            putStringSet("favorite_ids", favoriteRestaurants)
            apply()
        }
    }

    private fun setupSearchBar() {
        binding.searchBar.doOnTextChanged { text, _, _, _ ->
            filterAndDisplayRestaurants(text.toString().trim())
        }
    }

    private fun setupFilterButtons() {
        // ... (seu código existente)
        binding.btnFavoritos.setOnClickListener {
            // ...
            val favoritedList = allRestaurants.filter { favoriteRestaurants.contains(it.id) }
            restaurantAdapter.updateList(favoritedList, favoriteRestaurants)
        }
        binding.root.setOnClickListener {
            filterAndDisplayRestaurants("")
        }
    }

    private fun filterAndDisplayRestaurants(query: String) {
        val filteredList = if (query.isEmpty()) {
            (lastSearchedRestaurants + allRestaurants).distinctBy { it.id }
        } else {
            allRestaurants.filter { it.nome.contains(query, ignoreCase = true) }
        }
        val sortedList = filteredList.sortedByDescending { favoriteRestaurants.contains(it.id) }
        restaurantAdapter.updateList(sortedList, favoriteRestaurants)
    }

    private fun toggleFavorite(restaurante: ItemRestaurante) {
        if (favoriteRestaurants.contains(restaurante.id)) {
            favoriteRestaurants.remove(restaurante.id)
        } else {
            favoriteRestaurants.add(restaurante.id)
        }
        saveFavorites()
        filterAndDisplayRestaurants(binding.searchBar.text.toString().trim())
    }

    private fun updateLastSearched(restaurante: ItemRestaurante) {
        lastSearchedRestaurants.removeIf { it.id == restaurante.id }
        lastSearchedRestaurants.add(0, restaurante)
        if (lastSearchedRestaurants.size > 5) {
            lastSearchedRestaurants = lastSearchedRestaurants.take(5).toMutableList()
        }
        // ... Salvar em SharedPreferences (código existente)
    }

    private fun voltar(){
        // ... (seu código existente)
        val navController = findNavController()
        val isCardapio = navController.previousBackStackEntry?.destination?.id == R.id.cardapioFragment
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isCardapio) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}