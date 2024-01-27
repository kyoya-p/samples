import * as Realm from "realm-web"
import { ApolloClient, ApolloProvider, HttpLink, InMemoryCache, } from "@apollo/client";

async function main() {
	const appId="application-0-yvxdl"

	// Add your Realm App ID
	const graphqlUri = `https://realm.mongodb.com/api/client/v2.0/app/${appId}/graphql`;
	// Local apps should use a local URI!
	// const graphqlUri = `https://us-east-1.aws.stitch.mongodb.com/api/client/v2.0/app/${APP_ID}/graphql`
	// const graphqlUri = `https://eu-west-1.aws.stitch.mongodb.com/api/client/v2.0/app/${APP_ID}/graphql`
	// const graphqlUri = `https://ap-southeast-1.aws.stitch.mongodb.com/api/client/v2.0/app/${APP_ID}/graphql`
	const client = new ApolloClient({
		link: new HttpLink({
			uri: graphqlUri,
		}),
		cache: new InMemoryCache(),
	});

	const app = new Realm.App({ id: appId });
	const credentials = Realm.Credentials.anonymous();
	try {
		const user = await app.logIn(credentials);
	} catch (err) {
		console.error("Failed to log in", err);
	}
}

main()

