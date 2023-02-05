require('dotenv').config();

// クライアント認証
const random = Math.floor(Math.random() * 100);
// const random = 1;
const { MongoClient, ObjectId } = require('mongodb');
const url = process.env.COSMOS_CONNECTION_STRING; // 環境変数にプライマリ接続文字列を格納しておく
const client = new MongoClient(url);

async function main() {
    await client.connect(); // データベースに接続
    const db = client.db(`adventureworks`); // データベースインスタンス取得
    console.log(`New database:\t${db.databaseName}`);
    const collection = db.collection('products'); // コレクションインスタンス取得
    console.log(`New collection:\t${collection.collectionName}`);

    const indexResult = await collection.createIndex({ name: 1 }); // インデックス作成
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
    const upsertResult1 = await collection.updateOne(query, update, options);
    console.log(`upsertResult1: ${JSON.stringify(upsertResult1)}`);

    // const query2 = { _id: ObjectId(upsertResult1.upsertedId) };
    // const update2 = { $set: { quantity: 20 } };
    // const upsertResult2 = await client.db(`adventureworks`).collection('products').updateOne(query2, update2, options);
    // console.log(`upsertResult2: ${JSON.stringify(upsertResult2)}`);

    // ドキュメント取得_id
    const foundProduct = await collection.findOne({
        _id: ObjectId(upsertResult1.upsertedId),
        // category: "gear-surf-surfboards"
    });
    console.log(`foundProduct_id(${upsertResult1.upsertedId}): ${JSON.stringify(foundProduct)}`);

    // ドキュメント取得 - name
    const foundProduct2 = await collection.findOne({
        name: product.name,
        // category: "gear-surf-surfboards"
    });
    console.log(`foundProduct_name(${product.name}): ${JSON.stringify(foundProduct2)}`);

}

main()
    .then(console.log)
    .catch(console.error)
    .finally(() => client.close());

