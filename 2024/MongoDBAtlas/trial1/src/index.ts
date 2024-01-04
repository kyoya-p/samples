import * as Realm from "realm-web";

async function main() {
	const app = new Realm.App({ id: "application-0-yvxdl" });
	const credentials = Realm.Credentials.anonymous();
	try {
	  const user = await app.logIn(credentials);
	} catch(err) {
	  console.error("Failed to log in", err);
	}
}

main()

