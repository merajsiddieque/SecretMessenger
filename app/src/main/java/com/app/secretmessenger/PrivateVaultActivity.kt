package com.app.secretmessenger

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class PrivateVaultActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var vaultAdapter: VaultAdapter
    private val vaultItems = mutableListOf<VaultData>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isMultiSelect = false
    val selectedItems = mutableSetOf<VaultData>()
    private var lastVisibleItem: String? = null
    private val PAGE_SIZE = 10

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val currentUserEmail = auth.currentUser?.email ?: return@let
                getUsername(currentUserEmail) { username ->
                    if (username != null) {
                        val clipData = data.clipData
                        if (clipData != null) {
                            for (i in 0 until clipData.itemCount) {
                                val uri = clipData.getItemAt(i).uri
                                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                                val base64Image = bitmapToBase64(bitmap)
                                addNewItem(username, base64Image)
                            }
                        } else {
                            data.data?.let { uri ->
                                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                                val base64Image = bitmapToBase64(bitmap)
                                addNewItem(username, base64Image)
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed to get username", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_private_vault)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerViewVault)
        vaultAdapter = VaultAdapter(vaultItems, ::onItemLongClick, ::onItemClick, ::downloadSingleImage)
        recyclerView.layoutManager = GridLayoutManager(this, 1)
        recyclerView.adapter = vaultAdapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                if (totalItemCount <= (lastVisiblePosition + 3) && vaultItems.size >= PAGE_SIZE) {
                    loadMoreItems()
                }
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadInitialItems()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.vault_menu, menu)
        menu?.findItem(R.id.action_download)?.isVisible = isMultiSelect && selectedItems.isNotEmpty()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                openImageSelector()
                true
            }
            R.id.action_delete -> {
                if (isMultiSelect && selectedItems.isNotEmpty()) {
                    deleteSelectedItems()
                } else {
                    Toast.makeText(this, "Select items to delete", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.selectAll -> {
                toggleSelectAll()
                true
            }
            R.id.action_download -> {
                if (isMultiSelect && selectedItems.isNotEmpty()) {
                    downloadSelectedImages()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private var isAllSelected = false

    private fun toggleSelectAll() {
        if (isAllSelected && selectedItems.size == vaultItems.size) {
            selectedItems.clear()
            isMultiSelect = false
            isAllSelected = false
            supportActionBar?.title = "Private Vault"
            Toast.makeText(this, "All items deselected", Toast.LENGTH_SHORT).show()
        } else {
            isMultiSelect = true
            selectedItems.clear()
            selectedItems.addAll(vaultItems)
            isAllSelected = true
            supportActionBar?.title = "${selectedItems.size} selected"
            Toast.makeText(this, "All items selected", Toast.LENGTH_SHORT).show()
        }
        vaultAdapter.notifyDataSetChanged()
        invalidateOptionsMenu()
    }

    private fun onItemClick(item: VaultData) {
        if (isMultiSelect) {
            toggleSelection(item)
        }
    }

    private fun onItemLongClick(item: VaultData): Boolean {
        if (!isMultiSelect) {
            isMultiSelect = true
            selectedItems.add(item)
            supportActionBar?.title = "${selectedItems.size} selected"
        } else {
            toggleSelection(item)
        }
        vaultAdapter.notifyDataSetChanged()
        invalidateOptionsMenu()
        return true
    }

    private fun toggleSelection(item: VaultData) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }

        if (selectedItems.isEmpty()) {
            isMultiSelect = false
            supportActionBar?.title = "Private Vault"
        } else {
            supportActionBar?.title = "${selectedItems.size} selected"
        }

        vaultAdapter.notifyDataSetChanged() // Refresh entire list
        invalidateOptionsMenu()
    }


    private fun deleteSelectedItems() {
        val currentUserEmail = auth.currentUser?.email ?: return
        getUsername(currentUserEmail) { username ->
            if (username != null) {
                selectedItems.forEach { item ->
                    item.documentId?.let { docId ->
                        db.collection("Private_Vault").document(username)
                            .collection("Images").document(docId).delete()
                    }
                }
                vaultItems.removeAll(selectedItems)
                vaultAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Deleted ${selectedItems.size} items", Toast.LENGTH_SHORT).show()
                isMultiSelect = false
                selectedItems.clear()
                supportActionBar?.title = "Private Vault"
                invalidateOptionsMenu()
            }
        }
    }

    private fun openImageSelector() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        imagePicker.launch(intent)
    }

    private fun getUsername(email: String, callback: (String?) -> Unit) {
        db.collection("Users")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    val docId = doc.id
                    if (docId.endsWith("-$email")) {
                        val username = docId.substringBefore("-$email")
                        callback(username)
                        return@addOnSuccessListener
                    }
                }
                callback(null)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun addNewItem(username: String, base64Image: String) {
        val newItem = VaultData(username = username, image = base64Image)
        db.collection("Private_Vault").document(username)
            .collection("Images")
            .add(newItem)
            .addOnSuccessListener { docRef ->
                newItem.documentId = docRef.id
                vaultItems.add(newItem)
                vaultAdapter.notifyItemInserted(vaultItems.size - 1)
                recyclerView.scrollToPosition(vaultItems.size - 1)
                Toast.makeText(this, "Image added", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun loadInitialItems() {
        val currentUserEmail = auth.currentUser?.email ?: return
        getUsername(currentUserEmail) { username ->
            if (username != null) {
                db.collection("Private_Vault").document(username)
                    .collection("Images")
                    .limit(PAGE_SIZE.toLong())
                    .get()
                    .addOnSuccessListener { snapshot ->
                        vaultItems.clear()
                        for (doc in snapshot) {
                            val item = doc.toObject(VaultData::class.java).apply {
                                documentId = doc.id
                                this.username = username
                            }
                            vaultItems.add(item)
                        }
                        lastVisibleItem = snapshot.documents.lastOrNull()?.id
                        vaultAdapter.notifyDataSetChanged()
                    }
            }
        }
    }

    private fun loadMoreItems() {
        if (lastVisibleItem == null) return
        val currentUserEmail = auth.currentUser?.email ?: return
        getUsername(currentUserEmail) { username ->
            if (username != null) {
                db.collection("Private_Vault").document(username)
                    .collection("Images")
                    .startAfter(lastVisibleItem)
                    .limit(PAGE_SIZE.toLong())
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val newItems = snapshot.documents.map { doc ->
                            doc.toObject(VaultData::class.java)!!.apply {
                                documentId = doc.id
                                this.username = username
                            }
                        }
                        lastVisibleItem = snapshot.documents.lastOrNull()?.id
                        val startPosition = vaultItems.size
                        vaultItems.addAll(newItems)
                        vaultAdapter.notifyItemRangeInserted(startPosition, newItems.size)
                    }
            }
        }
    }

    private fun downloadSingleImage(item: VaultData) {
        val bitmap = base64ToBitmap(item.image)
        saveImageToStorage(bitmap, "${item.documentId}.jpg")
        Toast.makeText(this, "Image downloaded", Toast.LENGTH_SHORT).show()
    }

    private fun downloadSelectedImages() {
        selectedItems.forEach { item ->
            val bitmap = base64ToBitmap(item.image)
            saveImageToStorage(bitmap, "${item.documentId}.jpg")
        }
        Toast.makeText(this, "${selectedItems.size} images downloaded", Toast.LENGTH_SHORT).show()
        vaultAdapter.notifyDataSetChanged()
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun saveImageToStorage(bitmap: Bitmap, fileName: String) {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "SecretMessenger/Private Vault"
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }
}