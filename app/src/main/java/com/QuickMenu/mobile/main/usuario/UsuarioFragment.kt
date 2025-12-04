package com.QuickMenu.mobile.main.usuario

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.QuickMenu.mobile.databinding.FragmentUsuarioBinding
import com.QuickMenu.mobile.main.MainActivity
import com.QuickMenu.mobile.main.home.ItemRestaurante
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class UsuarioFragment : Fragment() {

    private var _binding: FragmentUsuarioBinding? = null
    private val binding get() = _binding!!
    private lateinit var banco: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var allRestaurants = mutableListOf<ItemRestaurante>()
    private var favoriteRestaurantIds = setOf<String>()
    private var lastSelectedRestaurantIds = listOf<String>()
    private var currentPhotoUrl: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uploadImageToImageBB(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        banco = Firebase.firestore
        auth = Firebase.auth

        initListener()
        loadUserData()
        loadAndDisplayPriorityRestaurants()
    }

    private fun initListener() {
        binding.voltar.setOnClickListener { logout() }

        binding.fotoPerfil.setOnClickListener {
            showOptionsDialog()
        }

        binding.editButton.setOnClickListener {
            showEditUsernameDialog()
        }
    }

    private fun showEditUsernameDialog() {
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50

        val editText = android.widget.EditText(requireContext())
        editText.layoutParams = params
        editText.setText(binding.nome.text)
        editText.setSelection(binding.nome.text.length)

        container.addView(editText)

        AlertDialog.Builder(requireContext())
            .setTitle("Alterar nome de usuário")
            .setView(container)
            .setPositiveButton("Salvar") { dialog, _ ->
                val newUsername = editText.text.toString().trim()

                if (newUsername.isNotEmpty()) {
                    updateUsernameInFirestore(newUsername)
                } else {
                    Toast.makeText(requireContext(), "O nome não pode ser vazio", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateUsernameInFirestore(newUsername: String) {
        val uid = auth.currentUser?.uid ?: return

        Toast.makeText(requireContext(), "Salvando...", Toast.LENGTH_SHORT).show()

        banco.collection("Usuario").document(uid)
            .update("username", newUsername)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Nome atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                binding.nome.text = newUsername
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao atualizar o nome: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showOptionsDialog() {
        val options = arrayOf("Visualizar foto", "Alterar foto")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Foto de Perfil")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showFullImage()
                1 -> pickImageLauncher.launch("image/*")
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun showFullImage() {
        if (currentPhotoUrl.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Nenhuma foto para visualizar", Toast.LENGTH_SHORT).show()
            return
        }

        val container = android.widget.LinearLayout(requireContext())
        container.orientation = android.widget.LinearLayout.VERTICAL
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 50, 50, 50) // Margens laterais
        container.layoutParams = params

        val imageView = ImageView(requireContext())

        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            1000
        )

        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.adjustViewBounds = true

        container.addView(imageView)

        Glide.with(this)
            .load(currentPhotoUrl)
            .placeholder(com.QuickMenu.mobile.R.drawable.default_profile_picture)
            .into(imageView)

        AlertDialog.Builder(requireContext())
            .setTitle("Foto de Perfil")
            .setView(container) // Passa o container, não a imagem direta
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun uploadImageToImageBB(imageUri: Uri) {
        Toast.makeText(requireContext(), "Fazendo upload...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiKey = "3b1fc0436f09d45aab3d2388edf3099e"
                val client = OkHttpClient()

                val inputStream = requireContext().contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null) throw Exception("Erro ao ler arquivo")

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", apiKey)
                    .addFormDataPart("image", "profile.jpg", bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                    .build()

                val request = Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string()

                if (response.isSuccessful && responseString != null) {
                    val json = JSONObject(responseString)
                    val newUrl = json.getJSONObject("data").getString("url")

                    withContext(Dispatchers.Main) {
                        updateFirestore(newUrl)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Erro no servidor de imagem", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFirestore(url: String) {
        val uid = auth.currentUser?.uid ?: return

        banco.collection("Usuario").document(uid)
            .update("profileImageUrl", url)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Foto atualizada com sucesso!", Toast.LENGTH_SHORT).show()

                loadProfileImage(url)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao salvar no banco", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        binding.email.text = auth.currentUser?.email

        banco.collection("Usuario").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    val photoUrl = document.getString("profileImageUrl")

                    binding.nome.text = username ?: "Sem nome"

                    if (!photoUrl.isNullOrEmpty()) {
                        loadProfileImage(photoUrl)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao buscar dados", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileImage(url: String) {
        currentPhotoUrl = url


        Glide.with(this)
            .load(url)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(com.QuickMenu.mobile.R.drawable.default_profile_picture)
            .error(com.QuickMenu.mobile.R.drawable.default_profile_picture)
            .into(binding.fotoPerfil)

    }

    private fun logout() {
        auth.signOut()
        parentFragmentManager.popBackStack()
        (requireActivity() as MainActivity).navigateToAuth()
    }

    private fun loadAndDisplayPriorityRestaurants() {
        loadUserPreferences()

        banco.collectionGroup("restaurantes").get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) return@addOnSuccessListener

                allRestaurants.clear()
                for (document in result) {
                    val restaurante = document.toObject(ItemRestaurante::class.java).copy(id = document.id)
                    allRestaurants.add(restaurante)
                }

                displayPriorityRestaurants()
            }

    }

    private fun loadUserPreferences() {
        val prefs = activity?.getSharedPreferences("RestaurantPreferences", Context.MODE_PRIVATE) ?: return
        favoriteRestaurantIds = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        lastSelectedRestaurantIds = prefs.getString("last_selected_ids", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun displayPriorityRestaurants() {
        val scoredRestaurants = allRestaurants.map { restaurant ->
            val isFavorite = favoriteRestaurantIds.contains(restaurant.id)
            val lastSelectedIndex = lastSelectedRestaurantIds.indexOf(restaurant.id)
            val isLastSelected = lastSelectedIndex != -1

            var score = 0
            if (isFavorite) {
                score = 1000

                if (isLastSelected) {
                    score += (100 - lastSelectedIndex)
                }
            } else if (isLastSelected) {
                score = 100 - lastSelectedIndex
            }

            Pair(restaurant, score)
        }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        val topRestaurants = scoredRestaurants.take(3)

        val imageViews = listOf(binding.ivRestaurante1, binding.ivRestaurante2, binding.ivRestaurante3)


        topRestaurants.forEachIndexed { index, restaurant ->
            if (index < imageViews.size) {
                val imageView = imageViews[index]
                imageView.visibility = View.VISIBLE
                Glide.with(this)
                    .load(restaurant.imageUrl)
                    .placeholder(com.QuickMenu.mobile.R.drawable.default_profile_picture)
                    .error(com.QuickMenu.mobile.R.drawable.bolo)
                    .into(imageView)

                imageView.setOnClickListener {
                    Toast.makeText(context, "Clicou em ${restaurant.nome}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}