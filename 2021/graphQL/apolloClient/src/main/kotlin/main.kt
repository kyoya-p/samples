import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun main() {


// First, create an `ApolloClient`
// Replace the serverUrl with your GraphQL endpoint
    val apolloClient = ApolloClient.builder()
        .serverUrl("https://your.domain/graphql/endpoint")
        .build()

// in your coroutine scope, call `ApolloClient.query(...).toDeferred().await()`
    GlobalScope.launch {
        val response = try {
            apolloClient.query(LaunchDetailsQuery(id = "83")).toDeferred().await()
        } catch (e: ApolloException) {
            // handle protocol errors
            return@launch
        }

        val launch = response.data?.launch
        if (launch == null || response.hasErrors()) {
            // handle application errors
            return@launch
        }

        // launch now contains a typesafe model of your data
        println("Launch site: ${launch.site}")
    }


}
