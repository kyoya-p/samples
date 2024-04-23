"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
require('dotenv').config();
// クライアント認証
const random = Math.floor(Math.random() * 100);
// const random = 1;
const { MongoClient, ObjectId } = require('mongodb');
const url = process.env.COSMOS_CONNECTION_STRING; // 環境変数にプライマリ接続文字列を格納しておく
const client = new MongoClient(url);
function main() {
    return __awaiter(this, void 0, void 0, function* () {
        yield client.connect(); // データベースに接続
        const db = client.db(`adventureworks`); // データベースインスタンス取得
        console.log(`New database:\t${db.databaseName}`);
        const collection = db.collection('products'); // コレクションインスタンス取得
        console.log(`New collection:\t${collection.collectionName}`);
        const indexResult = yield collection.createIndex({ name: 1 }); // インデックス作成
        console.log(`indexResult: ${JSON.stringify(indexResult)}`);
        // ドキュメント作成
        const product = {
            category: "gear-surf-surfboards",
            name: `Yamba Surfboard-${random}`,
            quantity: 12,
            sale: false
        };
        const query = { name: product.name };
        const update = { $set: product };
        const options = { upsert: true, new: true };
        // ドキュメントupsert(insert or update)
        const upsertResult1 = yield collection.updateOne(query, update, options);
        console.log(`upsertResult1: ${JSON.stringify(upsertResult1)}`);
        // const query2 = { _id: ObjectId(upsertResult1.upsertedId) };
        // const update2 = { $set: { quantity: 20 } };
        // const upsertResult2 = await client.db(`adventureworks`).collection('products').updateOne(query2, update2, options);
        // console.log(`upsertResult2: ${JSON.stringify(upsertResult2)}`);
        // ドキュメント取得_id
        const foundProduct = yield collection.findOne({
            _id: ObjectId(upsertResult1.upsertedId),
            // category: "gear-surf-surfboards"
        });
        console.log(`foundProduct_id(${upsertResult1.upsertedId}): ${JSON.stringify(foundProduct)}`);
        // ドキュメント取得 - name
        const foundProduct2 = yield collection.findOne({
            name: product.name,
            // category: "gear-surf-surfboards"
        });
        console.log(`foundProduct_name(${product.name}): ${JSON.stringify(foundProduct2)}`);
    });
}
main()
    .then(console.log)
    .catch(console.error)
    .finally(() => client.close());
//# sourceMappingURL=index.js.map