import { ApolloServer } from '@apollo/server';
import { PubSub } from 'graphql-subscriptions';

const SOMETHING_CHANGED_TOPIC = 'something_changed';
const pubsub = new PubSub();

const typeDefs = `
  type Query {
    hello: String
  }

  type Subscription {
    somethingChanged: String
  }
`;

const resolvers = {
  Query: {
    hello: () => 'Hello world!',
  },
  Subscription: {
    somethingChanged: {
      subscribe: () => pubsub.asyncIterator(SOMETHING_CHANGED_TOPIC),
    },
  },
};

const server = new ApolloServer({ typeDefs, resolvers });

server.listen().then(({ url }) => {
  console.log(`🚀 Server ready at ${url}`);
});
