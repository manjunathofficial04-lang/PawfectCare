package com.example.pawfectcare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.pawfectcare.ui.theme.PawfectCareTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()



        enableEdgeToEdge()
        setContent {
            PawfectCareTheme {
                HomeScreen(db = db, auth = auth)
            }
        }
    }



// Data models
data class Pet(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val breed: String = "",
    val age: Int = 0
)

data class OwnerContact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = ""
)

enum class HomeTab {
    PETS,
    OWNERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(db: FirebaseFirestore, auth: FirebaseAuth) {
    val context = LocalContext.current

    // ✅ Correct Compose permission launcher
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "Camera permission granted ✅", Toast.LENGTH_SHORT).show()
            // Later: open camera / capture photo
        } else {
            Toast.makeText(context, "Camera permission denied ❌", Toast.LENGTH_SHORT).show()
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2),
            Color(0xFFD7CCC8),
            Color(0xFF8D6E63)
        )
    )

    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.PETS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PawfectCare",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    // ✅ Camera icon TOP RIGHT
                    IconButton(onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (granted) {
                            Toast.makeText(context, "Camera already allowed 📸", Toast.LENGTH_SHORT).show()
                        } else {
                            // ✅ Ask permission when clicked
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6D4C41)
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradientBrush)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (selectedTab) {
                HomeTab.PETS -> PetsScreen(db = db, auth = auth)
                HomeTab.OWNERS -> OwnersScreen(db = db, auth = auth)
            }
        }
    }
}


@Composable
fun BottomNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF6D4C41),
        contentColor = Color.White
    ) {
        NavigationBarItem(
            selected = selectedTab == HomeTab.PETS,
            onClick = { onTabSelected(HomeTab.PETS) },
            icon = { Icon(Icons.Default.Pets, contentDescription = "Pets") },
            label = { Text("Pets") }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.OWNERS,
            onClick = { onTabSelected(HomeTab.OWNERS) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Owners") },
            label = { Text("Owners") }
        )
    }
}

@Composable
fun PetsScreen(db: FirebaseFirestore, auth: FirebaseAuth) {
    var isLoading by remember { mutableStateOf(true) }
    var pets by remember { mutableStateOf(listOf<Pet>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect

        db.collection("users")
            .document(uid)
            .collection("pets")
            .get()
            .addOnSuccessListener { snapshot ->
                pets = snapshot.documents.mapNotNull { it.toObject(Pet::class.java) }
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = e.localizedMessage ?: "Failed to load pets"
                isLoading = false
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Your Pawfect Pets 🐾",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4E342E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF6D4C41))
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                pets.isEmpty() -> {
                    Text(
                        text = "No pets found.\nAdd some later!",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF4E342E),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pets) { pet ->
                            PetCard(pet = pet)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnersScreen(db: FirebaseFirestore, auth: FirebaseAuth) {
    var isLoading by remember { mutableStateOf(true) }
    var owners by remember { mutableStateOf(listOf<OwnerContact>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect

        db.collection("users")
            .document(uid)
            .collection("owners")
            .get()
            .addOnSuccessListener { snapshot ->
                owners = snapshot.documents.mapNotNull { it.toObject(OwnerContact::class.java) }
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = e.localizedMessage ?: "Failed to load contacts"
                isLoading = false
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Owner Contacts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4E342E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF6D4C41))
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                owners.isEmpty() -> {
                    Text(
                        text = "No contacts found.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF4E342E),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(owners) { owner ->
                            OwnerCard(owner = owner)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PetCard(pet: Pet) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = pet.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${pet.type} • ${pet.breed}",
                fontSize = 16.sp,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Age: ${pet.age} years",
                fontSize = 14.sp,
                color = Color(0xFF795548)
            )
        }
    }
}

@Composable
fun OwnerCard(owner: OwnerContact) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = owner.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Phone: ${owner.phone}",
                fontSize = 16.sp,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Email: ${owner.email}",
                fontSize = 14.sp,
                color = Color(0xFF795548)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Address: ${owner.address}",
                fontSize = 14.sp,
                color = Color(0xFF795548)
            )
        }
    }
}}
