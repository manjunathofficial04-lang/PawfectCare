package com.example.pawfectcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.pawfectcare.ui.theme.PawfectCareTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // 🔸 TEMPORARY SAMPLE DATA – REMOVE LATER TO AVOID DUPLICATION
        seedSampleDataOnce(db, auth)

        enableEdgeToEdge()
        setContent {
            PawfectCareTheme {
                HomeScreen(db = db, auth = auth)
            }
        }
    }

    /**
     * Seeds sample Pets and Owner Contacts into Firestore
     *  You can remove this later once you have real data.
     */
    private fun seedSampleDataOnce(db: FirebaseFirestore, auth: FirebaseAuth) {
        val uid = auth.currentUser?.uid ?: return

        // --- Pets collection ---
        val petsCollection = db.collection("users")
            .document(uid)
            .collection("pets")

        petsCollection.get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) return@addOnSuccessListener

            val samplePets = listOf(
                Pet(name = "Buddy", type = "Dog", breed = "Labrador", age = 2),
                Pet(name = "Milo", type = "Cat", breed = "Siamese", age = 1),
                Pet(name = "Rocky", type = "Dog", breed = "German Shepherd", age = 4),
                Pet(name = "Bella", type = "Cat", breed = "Persian", age = 3),
                Pet(name = "Max", type = "Dog", breed = "Beagle", age = 5)
            )

            samplePets.forEach { pet ->
                val docRef = petsCollection.document()
                val petWithId = pet.copy(id = docRef.id)
                docRef.set(petWithId)
            }
        }

        // --- Owner Contacts collection ---
        val ownersCollection = db.collection("users")
            .document(uid)
            .collection("owners")

        ownersCollection.get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) return@addOnSuccessListener

            val sampleOwners = listOf(
                OwnerContact(
                    id = "",
                    name = "John Smith",
                    phone = "+44 7123 456789",
                    email = "john.smith@example.com",
                    address = "12 Bark Street, London"
                ),
                OwnerContact(
                    id = "",
                    name = "Emma Johnson",
                    phone = "+44 7011 223344",
                    email = "emma.johnson@example.com",
                    address = "45 Paw Lane, Manchester"
                ),
                OwnerContact(
                    id = "",
                    name = "Liam Brown",
                    phone = "+44 7312 998877",
                    email = "liam.brown@example.com",
                    address = "78 Whisker Road, Birmingham"
                )
            )

            sampleOwners.forEach { owner ->
                val docRef = ownersCollection.document()
                val ownerWithId = owner.copy(id = docRef.id)
                docRef.set(ownerWithId)
            }
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

@Composable
fun HomeScreen(db: FirebaseFirestore, auth: FirebaseAuth) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2),
            Color(0xFFD7CCC8),
            Color(0xFF8D6E63)
        )
    )

    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.PETS) }

    Scaffold(
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

    // Load pets
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
                        modifier = Modifier
                            .fillMaxSize(),
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

    // Load owner contacts
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Owner Contacts ",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4E342E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
}
