import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.Operation.Variables
import com.apollographql.apollo.api.Operation.Data


// Apollo SDK使用
fun main() {
    // Deviceの初期化の処理
    // 認証略

    val apolloClient = ApolloClient.builder()
        .serverUrl("https://your.domain/graphql/endpoint").build()

    apolloClient.query<Data, Any, Variables>(LaunchDetailsQuery("83"))
        .enqueue(object : ApolloCall.Callback<LaunchDetailsQuery.Data?>() {
            override fun onResponse(response: Response<LaunchDetailsQuery.Data?>) {
                // ここでDevice初期化処理
            }

            override fun onFailure(e: ApolloException) {
                //  ここでエラー処理
            }
        })
}