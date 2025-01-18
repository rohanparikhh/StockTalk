package com.example.googlesignintest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import java.text.DateFormat
import kotlin.math.roundToInt
import com.example.googlesignintest.Stock

/**
 * MainActivity sets up Compose content and provides Firebase and Google Sign-In
 * functionality along with all the screens: login, post list, post detail, and swipe.
 */
class MainActivity : ComponentActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth.
        mAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In.
        // Replace "YOUR_WEB_CLIENT_ID" with your actual web client ID or use a string resource.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            StockTalkApp(
                mAuth = mAuth,
                googleSignInClient = mGoogleSignInClient
            )
        }
    }
}

val likedTags = mutableSetOf<String>()


/**
 * StockTalkApp sets up a NavHost and provides navigation between different screens.
 */
@Composable
fun StockTalkApp(
    mAuth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                mAuth = mAuth,
                googleSignInClient = googleSignInClient,
                onLoginSuccess = { navController.navigate("postList") }
            )
        }
        composable("postList") {
            // Remember a list of posts from our dummy repository.
            val posts = remember { mutableStateListOf<Post>().apply { addAll(PostRepository.getPosts()) } }
            PostListScreen(
                posts = posts,
                onPostClick = { selectedPost ->
                    navController.navigate("postDetail/${selectedPost.id}")
                },
                onSwipeClick = { navController.navigate("stockSwipe") },
                mAuth = mAuth,
                googleSignInClient = googleSignInClient,
                onLoggedOut = {
                    // After logout, navigate back to the login screen.
                    navController.navigate("login") {
                        popUpTo("postList") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "postDetail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.IntType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getInt("postId")
            val post = PostRepository.getPosts().find { it.id == postId }
            if (post != null) {
                PostDetailScreen(post = post, onBack = { navController.popBackStack() })
            }
        }
        composable("stockSwipe") {
            SwipeStockScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * LoginScreen displays a Google Sign-In button.
 * On successful sign in, it navigates to the Post List screen.
 */
@Composable
fun LoginScreen(
    mAuth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    // Launcher to handle the result of the sign-in intent.
    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account!!, mAuth, onLoginSuccess, context)
        } catch (e: ApiException) {
            Toast.makeText(context, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to StockTalk!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        GoogleSignInButton(onClick = { signInLauncher.launch(googleSignInClient.signInIntent) })
    }
}

/**
 * Authenticates with Firebase using a Google account.
 */
fun firebaseAuthWithGoogle(
    account: GoogleSignInAccount,
    mAuth: FirebaseAuth,
    onLoginSuccess: () -> Unit,
    context: Context
) {
    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
    mAuth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onLoginSuccess()
            } else {
                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
}

/**
 * A simple Google Sign-In button.
 */
@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Google Sign-In Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Sign in with Google", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * PostListScreen displays a list of posts and includes a Logout button in the TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListScreen(
    posts: List<Post>,
    onPostClick: (Post) -> Unit,
    onSwipeClick: () -> Unit,
    mAuth: FirebaseAuth, // passed from MainActivity
    googleSignInClient: GoogleSignInClient,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Get the current user's name.
    val currentUser = mAuth.currentUser
    val userName = currentUser?.displayName ?: "Anonymous"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hello, $userName!") },
                actions = {
                    // Navigate to the swipe screen.
                    IconButton(onClick = onSwipeClick) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Swipe Stocks"
                        )
                    }
                    // Logout button.
                    IconButton(onClick = {
                        coroutineScope.launch {
                            firebaseSignOut(
                                mAuth = mAuth,
                                googleSignInClient = googleSignInClient,
                                onSuccess = {
                                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                    onLoggedOut()
                                },
                                onFailure = {
                                    Toast.makeText(context, "Logout failed", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = "Liked Tags: ${if(likedTags.isEmpty()) "None" else likedTags.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { likedTags.clear() },
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text("Reset Liked Tags")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            items(posts) { post ->
                PostListItem(post = post, onClick = { onPostClick(post) })
            }
        }
    }
}


/**
 * Signs out of Firebase and Google.
 */
suspend fun firebaseSignOut(
    mAuth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    try {
        mAuth.signOut()
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    } catch (e: Exception) {
        onFailure()
    }
}

/**
 * Displays a single post in the list.
 */
@Composable
fun PostListItem(post: Post, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${post.stockSymbol} - ${post.title}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "by ${post.author}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Posted at ${DateFormat.getDateTimeInstance().format(post.timestamp)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * PostDetailScreen shows the full details of a post.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(post: Post, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(post.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Go back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Stock: ${post.stockSymbol}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = post.content, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Author: ${post.author}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * SwipeStockScreen displays a card that can be swiped left/right to like or dislike a stock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeStockScreen(onBack: () -> Unit) {
    // Dummy stock list.
    val dummyStocks = listOf(
         Stock("AAPL", "Apple Inc.", 150.0, listOf("tech", "consumer electronics")),
         Stock("TSLA", "Tesla Inc.", 700.0, listOf("tech", "automotive")),
         Stock("GOOGL", "Alphabet Inc.", 2800.0, listOf("tech", "internet")),
         Stock("AMZN", "Amazon.com Inc.", 3500.0, listOf("tech", "e-commerce")),
         Stock("MSFT", "Microsoft Corp.", 300.0, listOf("tech", "software"))
    )
    var currentIndex by remember { mutableStateOf(0) }
    val stock = dummyStocks.getOrNull(currentIndex)

    // Animation offset.
    val offsetX = remember { Animatable(0f) }
    val swipeThreshold = 200f
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swipe Stocks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Go back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (stock != null) {
                Card(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .pointerInput(stock) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (offsetX.value > swipeThreshold || offsetX.value < -swipeThreshold) {
                                            currentIndex++
                                        }
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                    }
                                }
                            )
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stock.name, style = MaterialTheme.typography.headlineLarge)
                    }
                }
                if (offsetX.value > swipeThreshold / 2) {
                    Text(
                        text = "Liked!",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    likedTags.addAll(stock.tags)

                } else if (offsetX.value < -swipeThreshold / 2) {
                    Text(
                        text = "Disliked!",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            } else {
                Text(text = "No more stocks!", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
