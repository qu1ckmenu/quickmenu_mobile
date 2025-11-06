package com.example.login.main.carrinho

import android.content.Context // Import necessário para onAttach
import android.icu.text.NumberFormat
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.login.databinding.FragmentCarrinhoBinding
import java.util.Locale

// Interface para o Fragment avisar o Adapter sobre remoção
interface CarrinhoActionsListener {
    fun onRemoverItem(position: Int)
}

class CarrinhoFragment : Fragment(), CarrinhoActionsListener {

    private var _binding: FragmentCarrinhoBinding? = null
    private val binding get() = _binding!!

    private lateinit var carrinhoAdapter: CarrinhoAdapter
    // Torna a lista de itens uma propriedade da classe
    private val listaItens = mutableListOf<ItemCarrinho>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarrinhoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializa a lista de dados (populando, já que agora é uma propriedade)
        listaItens.add(ItemCarrinho("1", "Bolo Red Velvet", 50.00, 1, 3))
        listaItens.add(ItemCarrinho("2", "Brigadeiro de morango", 9.00, 1, 3))

        // 2. Cria e INICIALIZA a instância do seu adapter
        carrinhoAdapter = CarrinhoAdapter(listaItens, this)

        // 3. Configura a RecyclerView
        binding.rvCarrinhoItens.layoutManager = LinearLayoutManager(context)
        binding.rvCarrinhoItens.adapter = carrinhoAdapter

        // 4. Lógica de clique de adição (na Activity)
        binding.btnAddItemTeste.setOnClickListener {
            val novoItem = ItemCarrinho(
                id = "ID_${System.currentTimeMillis()}",
                nome = "Item Adicionado (TESTE)",
                preco = 10.00,
                quantidade = 1,
                imagem = 3
            )

            carrinhoAdapter.adicionarItem(novoItem)

            // NOTIFICA A ACTIVITY SOBRE A MUDANÇA
            onTotalChanged(calcularTotal(), listaItens.size)

            // 1. Encontra a Toolbar dentro do layout deste Fragment
            val toolbar = binding.toolbar // **VERIFIQUE O ID DA SUA TOOLBAR NO XML DO CARRINHO**

            // 2. Define a Toolbar do Fragment como a ActionBar da Activity
            (activity as AppCompatActivity).setSupportActionBar(toolbar)

            // 3. Vincula a Toolbar ao NavController do Fragment
            // Isso faz com que:
            // a) O título (label) do nav_graph seja exibido.
            // b) O botão de voltar (seta) apareça.
            // c) O clique no botão de voltar navegue para trás (popBackStack).
            val navController = findNavController()
            toolbar.setupWithNavController(navController)
            (activity as AppCompatActivity).supportInvalidateOptionsMenu()
        }

        // CÁLCULO INICIAL: Envia o total na primeira vez que o Fragment é carregado
        onTotalChanged(calcularTotal(), listaItens.size)
    }

    fun onTotalChanged(novoTotal: Double, totalItens: Int) {

        // 1. Configuração do formatador de moeda (Exemplo: Real Brasileiro)
        val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        // 2. ATUALIZAÇÃO DO TOTAL USANDO BINDING
        // Acessa as TextViews diretamente pelo objeto 'binding'
        binding.total.text = formatador.format(novoTotal)

        // 3. ATUALIZAÇÃO DA CONTAGEM DE ITENS USANDO BINDING
        binding.totalTitle.text = "Total ($totalItens)"
    }

    // Implementação da função da Interface CarrinhoActionsListener
    override fun onRemoverItem(position: Int) {
        // 1. Executa a remoção no Adapter
        carrinhoAdapter.removerItem(position)

        onTotalChanged(calcularTotal(), listaItens.size)
    }

    // Função para calcular o total
    private fun calcularTotal(): Double {
        return listaItens.sumOf { it.preco * it.quantidade }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
        _binding = null
    }
}