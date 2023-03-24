
# Syntax
```
npx ts-node exp.ts <original-token> [<signature> [<new-date>]]
```

# Usage
```
npm i

SIGN=XXXXX
ORGTKN=eyJ...vucFbUCRg8
npx ts-node exp.ts $ORGTKN                                  # Token Content Confirmation / トークン内容確認
npx ts-node exp.ts $ORGTKN $SIGN                            # Sugnature Check / 署名チェック
npx ts-node exp.ts $ORGTKN $SIGN 2023-03-23T23:59:59+09:00  # Token generation with modified due date / 期日を変更したトークン生成
```
