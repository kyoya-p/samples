"use strict";
// Read .env file and set environment variables
require('dotenv').config();
const random = Math.floor(Math.random() * 100);
// Use official mongodb driver to connect to the server
const { MongoClient, ObjectId } = require('mongodb');
function hello(name) {
    return `Hello, ${name}!`;
}
console.log(hello("World"));
