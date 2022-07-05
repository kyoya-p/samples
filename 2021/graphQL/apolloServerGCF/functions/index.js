const functions = require('firebase-functions');

const { ApolloServer, gql } = require('apollo-server-cloud-functions');

// Construct a schema, using GraphQL schema language
const typeDefs = gql`
  type Query {
    hello: String
  }
`;

// Provide resolver functions for your schema fields
const resolvers = {
  Query: {
    hello: () => 'Hello ',
  },
};

const server = new ApolloServer({
  typeDefs,
  resolvers,
  playground: true,
  introspection: true,
});

//exports.handler = server.createHandler();


// -----
exports.handler = functions.https.onRequest((req, res) => {
  var f=server.createHandler();
  return f(req,res);
})



// -----
const admin = require('firebase-admin');
admin.initializeApp({serviceAccountId: 'firebase-adminsdk-rc191@road-to-iot.iam.gserviceaccount.com'});
var firestore = admin.firestore()

exports.requestToken = functions.https.onRequest((request, response) => {
  var id=request.query.id; if(id==null) response.send("1");
  var pw=request.query.pw; if(pw==null) response.send("2");
  firestore.collection('device').doc(id).get().then(doc=>{
      if(doc==null) response.send("3");
      if(doc.data()==null) response.send("4");
      if(doc.data().dev==null) response.send("5");
      if(doc.data().dev.password==null) response.send("6");
      if(pw!=doc.data().dev.password) response.send("7");
      var additionalClaims={id: id, cluster: doc.data().dev.cluster};
      admin.auth().createCustomToken(id,additionalClaims)
          .then(customToken => {
              response.send(customToken);
          }).catch(error => {
              console.log('Error creating custom token:', error);
              response.send("8");
          });
  });
})
