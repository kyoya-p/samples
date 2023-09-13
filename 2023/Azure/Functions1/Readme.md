## Build
```
npm i
npx tsc
```

## URL

### リダイレクト要求(URLクエリにtgOriginをセット)
Request:
URL: `http://host/api/func1?tgOrigin=<target-origin>&tgPath=<path-for-target>`

Response:
Statue: 302(Redirect)
Location: `http://host/api/func1?tgPath=<path-for-target>`
SetCookie: {tgOrigin:target-origin}

### リバースプロキシ要求(cookies[tgOrigin]をセット)
Request:
URL: `http://host/api/func1?tgPath=<path-for-target>`
Cookie: {tgOrigin:target-origin}

Response:
`${tgOrigin}/${tgPath}`をURLとしてHTTP要求し、そのHTTP応答を応答する。

## Note

https://qiita.com/shokkaa/items/3e46e1b9ae24e05d818e
