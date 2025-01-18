package com.example.googlesignintest

object PostRepository {
    private val posts = mutableListOf(
        Post(
            id = 1,
            title = "Is \$AAPL a Buy?",
            content = "Apple's stock might be undervalued after the recent dip. Thoughts?",
            stockSymbol = "AAPL",
            author = "user1",
            timestamp = System.currentTimeMillis()
        ),
        Post(
            id = 2,
            title = "Tesla Rally Analysis",
            content = "Tesla had a massive rally today. What are your thoughts on this momentum?",
            stockSymbol = "TSLA",
            author = "user2",
            timestamp = System.currentTimeMillis()
        )
    )

    fun getPosts() = posts

    fun addPost(post: Post) {
        posts.add(0, post)
    }
}
