const path = require('path');

module.exports = {
  entry: './build/peer-server.js',
  output: {
    path: path.resolve(__dirname, 'public'),
    filename: 'peer-server.js',
  },
};

