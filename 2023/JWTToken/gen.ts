import * as jwt from 'jsonwebtoken';

// HS256アルゴリズムで署名を生成するための共有シークレット
const secret = 'your-secret-phrase';

// 検証対象のJWTトークン
const jwtToken = 'your-jwt-token';

// JWTトークンの検証
try {
    const decodedToken = jwt.verify(jwtToken, secret, { algorithms: ['HS256'] });
    console.log(decodedToken);
} catch (err) {
    console.log(err);
